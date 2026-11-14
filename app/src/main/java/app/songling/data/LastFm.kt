package app.songling.data

import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.first
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Last.fm as it happens: the scrobbles of a username, read every quarter of an hour (user.getRecentTracks, public,
 * no login), so music heard on a computer or a speaker lands here too. Only what came after connecting is taken;
 * the past comes over by the CSV import. A scrobble of a song this phone already heard is the same listen and is
 * left out (see [Import.fresh]).
 */
object LastFm {
    /** Songling's API key: a public app id, not a secret. */
    private const val KEY = "ac70e0dc8bf4a5319489a822d255acbb"
    private const val API = "https://ws.audioscrobbler.com/2.0/"
    private const val GUESS_MS = 210_000L

    class Failure(message: String) : Exception(message)

    data class Page(val plays: List<Play>, val pages: Int)

    /** Calls [method] with [args]; the parsed answer, or a [Failure] with Last.fm's own words. */
    private fun call(method: String, args: Map<String, String>): JSONObject {
        val q = (args + mapOf("method" to method, "api_key" to KEY, "format" to "json")).entries
            .joinToString("&") { (k, v) -> k + "=" + URLEncoder.encode(v, "UTF-8") }
        val c = URL("$API?$q").openConnection() as HttpURLConnection
        c.connectTimeout = 10_000; c.readTimeout = 20_000
        c.setRequestProperty("User-Agent", Meta.AGENT)
        val code = c.responseCode
        val body = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
        val o = runCatching { JSONObject(body) }.getOrNull() ?: throw Failure("HTTP $code")
        if (o.has("error")) throw Failure(o.optString("message", "error ${o.optInt("error")}"))
        return o
    }

    /** The name as Last.fm spells it, or a [Failure] (no such user, no network). */
    fun check(user: String): String = call("user.getinfo", mapOf("user" to user)).optJSONObject("user")?.optString("name")?.ifBlank { null } ?: user

    /** One page of scrobbles after [from] (seconds), newest first; [length] tells a song's length when it's known. */
    fun recent(user: String, from: Long, page: Int, length: (String) -> Long): Page = parse(call("user.getrecenttracks",
        mapOf("user" to user, "from" to (from + 1).toString(), "limit" to "200", "page" to page.toString())), length)

    /** The player's package a Last.fm «now playing» is shown under. */
    const val APP = "fm.last"

    /** What [user] is playing right now on any device, as Last.fm says, or null. */
    fun now(user: String): app.songling.listen.NowPlaying? = playing(call("user.getrecenttracks", mapOf("user" to user, "limit" to "1")))

    fun playing(o: JSONObject): app.songling.listen.NowPlaying? {
        val r = o.optJSONObject("recenttracks") ?: return null
        val t = r.optJSONArray("track")?.optJSONObject(0) ?: r.optJSONObject("track") ?: return null
        if (t.optJSONObject("@attr")?.optString("nowplaying") != "true") return null
        val title = t.optString("name").ifBlank { return null }
        val artist = t.optJSONObject("artist")?.let { it.optString("#text").ifBlank { it.optString("name") } }.orEmpty().ifBlank { return null }
        return app.songling.listen.NowPlaying(title, artist, t.optJSONObject("album")?.optString("#text").orEmpty(), APP, playing = true, ms = 0, length = 0, cover = null,
            since = System.currentTimeMillis())
    }

    fun parse(o: JSONObject, length: (String) -> Long): Page {
        val r = o.optJSONObject("recenttracks") ?: return Page(emptyList(), 0)
        val arr = r.optJSONArray("track") ?: r.optJSONObject("track")?.let { JSONArray().put(it) } ?: JSONArray()
        val out = ArrayList<Play>()
        for (i in 0 until arr.length()) {
            val t = arr.optJSONObject(i) ?: continue
            // What's playing right now has no date yet: it comes once it's scrobbled.
            if (t.optJSONObject("@attr")?.optString("nowplaying") == "true") continue
            val uts = t.optJSONObject("date")?.optString("uts")?.toLongOrNull() ?: continue
            val title = t.optString("name").ifBlank { continue }
            val artist = t.optJSONObject("artist")?.let { it.optString("#text").ifBlank { it.optString("name") } }.orEmpty().ifBlank { continue }
            val album = t.optJSONObject("album")?.optString("#text").orEmpty()
            val known = length(Keys.track(artist, title))
            out += Play(at = uts * 1000, ms = if (known > 0) known else GUESS_MS, title = title, artistName = artist, albumName = album, length = known,
                source = Source.LASTFM, app = APP, whole = true, ref = "fm:$uts:${title.take(40)}")
        }
        val pages = r.optJSONObject("@attr")?.optString("totalPages")?.toIntOrNull() ?: 1
        return Page(out, pages)
    }

    /**
     * Brings in what [user] scrobbled after [from] (seconds): new listens written, the ones already here left out.
     * Returns how many were added and the newest scrobble's time, to start from next time.
     */
    suspend fun sync(dao: MusicDao, user: String, from: Long): Pair<Int, Long> {
        val lengths = HashMap<String, Long>()
        dao.tracks().first().forEach { if (it.length > 0) lengths[it.key] = it.length }
        val all = ArrayList<Play>()
        var page = 1
        while (page <= 10) {
            val p = recent(user, from, page) { lengths[it] ?: 0L }
            all += p.plays
            if (page >= p.pages) break
            page++
        }
        if (all.isEmpty()) return 0 to from
        val newest = all.maxOf { it.at } / 1000
        // Into the album a song or an album name was put by hand, as the player's listens are (#22).
        val fresh = Import.fresh(all, dao.playList()).map { p ->
            val home = dao.albumByAlias("t:" + p.track) ?: if (p.album.isEmpty()) null else dao.albumByAlias(p.album)
            if (home == null) p else p.copy(album = home.key, albumName = home.title)
        }
        val added = dao.insertPlays(fresh).count { it > 0 }
        for (p in fresh.distinctBy { it.artist }) dao.insertArtist(Artist(p.artist, Keys.mainArtist(p.artistName)))
        for (p in fresh.distinctBy { it.track }) dao.insertTrack(Track(p.track, p.title, p.artistName, album = p.album, albumName = p.albumName))
        for (p in fresh.filter { it.album.isNotEmpty() }.distinctBy { it.album }) dao.insertAlbum(Album(p.album, p.albumName, p.artistName))
        return added to maxOf(from, newest)
    }
}
