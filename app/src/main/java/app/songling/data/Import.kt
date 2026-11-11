package app.songling.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipInputStream

/**
 * Brings a listening history over from elsewhere:
 * - Spotify «Extended streaming history» (Streaming_History_Audio_*.json, with albums),
 * - Spotify «Account data» (StreamingHistory_music_*.json),
 * - ListenBrainz export (JSON array or one listen per line),
 * - Last.fm as CSV (artist, album, track, date), as the usual exporters write it.
 * A zip of any of these works too.
 */
object Import {
    data class Result(val plays: List<Play>, val skipped: Int, val format: String)

    private const val MIN_MS = 30_000L
    private const val GUESS_MS = 210_000L

    fun read(name: String, input: InputStream): Result {
        if (name.endsWith(".zip", true)) {
            val all = ArrayList<Play>(); var skipped = 0; val formats = HashSet<String>()
            ZipInputStream(input).use { z ->
                while (true) {
                    val e = z.nextEntry ?: break
                    if (e.isDirectory) continue
                    val n = e.name.substringAfterLast('/')
                    if (!(n.endsWith(".json", true) || n.endsWith(".csv", true) || n.endsWith(".jsonl", true))) continue
                    if (n.contains("Video", true) || n.contains("podcast", true)) continue
                    val text = z.readBytes().toString(Charsets.UTF_8)
                    val r = parse(n, text)
                    all += r.plays; skipped += r.skipped; if (r.plays.isNotEmpty()) formats += r.format
                }
            }
            return Result(all, skipped, formats.joinToString(", "))
        }
        return parse(name, input.readBytes().toString(Charsets.UTF_8))
    }

    fun parse(name: String, text: String): Result {
        val t = text.trimStart('\uFEFF', ' ', '\n', '\r', '\t')
        return when {
            t.startsWith("[") -> json(JSONArray(t))
            t.startsWith("{") -> {
                // One ListenBrainz listen per line, or an object holding a list.
                val lines = t.lines().filter { it.isNotBlank() }
                if (lines.size > 1 && lines.all { it.trim().startsWith("{") }) json(JSONArray(lines.map { JSONObject(it) }))
                else JSONObject(t).let { o -> o.optJSONArray("listens") ?: o.optJSONObject("payload")?.optJSONArray("listens") }?.let(::json) ?: Result(emptyList(), 0, "")
            }
            else -> csv(t)
        }
    }

    private fun json(arr: JSONArray): Result {
        val out = ArrayList<Play>(arr.length()); var skipped = 0; var format = ""
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val p = when {
                o.has("ts") && o.has("ms_played") -> { format = "Spotify"; spotifyExtended(o) }
                o.has("endTime") && o.has("msPlayed") -> { format = "Spotify"; spotifyBasic(o) }
                o.has("listened_at") -> { format = "ListenBrainz"; listenBrainz(o) }
                else -> null
            }
            if (p == null) skipped++ else out += p
        }
        return Result(out, skipped, format)
    }

    private fun spotifyExtended(o: JSONObject): Play? {
        val title = o.optString("master_metadata_track_name").takeIf { it.isNotBlank() && it != "null" } ?: return null
        val artist = o.optString("master_metadata_album_artist_name").takeIf { it.isNotBlank() && it != "null" } ?: return null
        val album = o.optString("master_metadata_album_album_name").takeIf { it != "null" }.orEmpty()
        val ms = o.optLong("ms_played")
        if (ms < MIN_MS) return null
        val end = runCatching { java.time.Instant.parse(o.getString("ts")).toEpochMilli() }.getOrNull() ?: return null
        val skipped = o.optBoolean("skipped") || o.optString("reason_end") == "fwdbtn"
        return Play(at = end - ms, ms = ms, title = title, artistName = artist, albumName = album, source = Source.IMPORT, app = "com.spotify.music",
            whole = o.optString("reason_end") == "trackdone", skipped = skipped, ref = "sp:${o.getString("ts")}:${title.take(40)}")
    }

    private val SPOTIFY_BASIC = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    private fun spotifyBasic(o: JSONObject): Play? {
        val title = o.optString("trackName").takeIf { it.isNotBlank() && it != "Unknown Track" } ?: return null
        val artist = o.optString("artistName").takeIf { it.isNotBlank() && it != "Unknown Artist" } ?: return null
        val ms = o.optLong("msPlayed")
        if (ms < MIN_MS) return null
        val end = runCatching { LocalDateTime.parse(o.getString("endTime"), SPOTIFY_BASIC).toInstant(ZoneOffset.UTC).toEpochMilli() }.getOrNull() ?: return null
        return Play(at = end - ms, ms = ms, title = title, artistName = artist, source = Source.IMPORT, app = "com.spotify.music", ref = "sb:${o.getString("endTime")}:${title.take(40)}")
    }

    private fun listenBrainz(o: JSONObject): Play? {
        val at = o.optLong("listened_at") * 1000
        val m = o.optJSONObject("track_metadata") ?: return null
        val title = m.optString("track_name").ifBlank { return null }
        val artist = m.optString("artist_name").ifBlank { return null }
        val info = m.optJSONObject("additional_info")
        val ms = info?.optLong("duration_ms")?.takeIf { it > 0 } ?: info?.optLong("duration")?.takeIf { it > 0 }?.times(1000) ?: GUESS_MS
        return Play(at = at, ms = ms, title = title, artistName = artist, albumName = m.optString("release_name"), length = ms, source = Source.IMPORT,
            whole = true, ref = "lb:${o.optLong("listened_at")}:${title.take(40)}")
    }

    private val LASTFM = listOf(
        DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
    )

    private fun cells(line: String): List<String> {
        val out = ArrayList<String>(); val b = StringBuilder(); var q = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && q && i + 1 < line.length && line[i + 1] == '"' -> { b.append('"'); i++ }
                c == '"' -> q = !q
                c == ',' && !q -> { out += b.toString(); b.clear() }
                else -> b.append(c)
            }
            i++
        }
        out += b.toString()
        return out.map { it.trim() }
    }

    private fun csv(text: String): Result {
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return Result(emptyList(), 0, "")
        val head = cells(lines.first()).map { it.lowercase() }
        val hasHead = head.any { it in setOf("artist", "track", "album", "date", "uts", "utc_time") }
        val ia = if (hasHead) head.indexOfFirst { it == "artist" || it == "artist_name" } else 0
        val ib = if (hasHead) head.indexOfFirst { it == "album" || it == "album_name" } else 1
        val it = if (hasHead) head.indexOfFirst { it == "track" || it == "track_name" || it == "title" } else 2
        val iu = if (hasHead) head.indexOf("uts") else -1
        val id = if (hasHead) head.indexOfFirst { it == "date" || it == "utc_time" || it == "time" } else 3
        val out = ArrayList<Play>(); var skipped = 0
        for (l in lines.drop(if (hasHead) 1 else 0)) {
            val c = cells(l)
            val artist = c.getOrNull(ia).orEmpty(); val title = c.getOrNull(it).orEmpty()
            if (artist.isBlank() || title.isBlank()) { skipped++; continue }
            val at = c.getOrNull(iu)?.toLongOrNull()?.times(1000)
                ?: c.getOrNull(id)?.let { s -> LASTFM.firstNotNullOfOrNull { f -> runCatching { LocalDateTime.parse(s, f).toInstant(ZoneOffset.UTC).toEpochMilli() }.getOrNull() } }
            if (at == null) { skipped++; continue }
            out += Play(at = at, ms = GUESS_MS, title = title, artistName = artist, albumName = c.getOrNull(ib).orEmpty(), source = Source.IMPORT, whole = true,
                ref = "lf:$at:${title.take(40)}")
        }
        return Result(out, skipped, "Last.fm")
    }
}
