package app.songling.data

import android.content.Context
import app.songling.domain.Genre
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** One album as the catalogue found it. */
data class Found(
    val title: String,
    val artist: String,
    val year: Int?,
    val genreName: String,
    val tracks: Int,
    val art: String?,
    val kind: Kind,
    val id: Long,
)

/** One song as the catalogue found it. */
data class FoundTrack(val title: String, val artist: String, val album: String, val number: Int, val length: Long, val year: Int?, val genreName: String, val art: String?, val albumTracks: Int)

/**
 * Fills in what players don't say: genre, year, number of tracks and cover (iTunes Search), and an
 * artist's country (MusicBrainz). Asks slowly, one thing at a time, and only for what's unknown.
 */
class Meta(private val ctx: Context, private val dao: MusicDao) {
    companion object {
        const val AGENT = "Songling/1.0 (https://github.com/bigdevwhale/songling)"
        private const val RETRY = 14 * DAY

        fun get(url: String): String? = runCatching {
            val c = URL(url).openConnection() as HttpURLConnection
            c.connectTimeout = 10_000; c.readTimeout = 15_000
            c.setRequestProperty("User-Agent", AGENT)
            c.setRequestProperty("Accept", "application/json")
            if (c.responseCode != 200) return null
            c.inputStream.bufferedReader().use { it.readText() }
        }.getOrNull()

        private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

        private fun art(o: JSONObject): String? = o.optString("artworkUrl100").ifBlank { null }?.replace("100x100bb", "600x600bb")

        private fun year(o: JSONObject): Int? = o.optString("releaseDate").take(4).toIntOrNull()

        private fun kindOf(title: String, tracks: Int): Kind = when {
            title.contains(" - Single", true) || tracks in 1..3 -> Kind.SINGLE
            title.contains(" - EP", true) || title.endsWith(" EP") || tracks in 4..6 -> Kind.EP
            else -> Kind.ALBUM
        }

        fun clean(title: String) = title.replace(Regex(""" - (Single|EP)$"""), "")

        /** Albums for a free search, for adding by hand. */
        fun searchAlbums(q: String, ru: Boolean): List<Found> {
            val body = get("https://itunes.apple.com/search?media=music&entity=album&limit=20&term=${enc(q)}&country=${if (ru) "ru" else "us"}") ?: return emptyList()
            val arr = JSONObject(body).optJSONArray("results") ?: return emptyList()
            return (0 until arr.length()).map { arr.getJSONObject(it) }.map { o ->
                val title = o.optString("collectionName"); val n = o.optInt("trackCount")
                Found(clean(title), o.optString("artistName"), year(o), o.optString("primaryGenreName"), n, art(o), kindOf(title, n), o.optLong("collectionId"))
            }
        }

        /** Songs for a free search, for adding by hand. */
        fun searchTracks(q: String, ru: Boolean): List<FoundTrack> {
            val body = get("https://itunes.apple.com/search?media=music&entity=song&limit=20&term=${enc(q)}&country=${if (ru) "ru" else "us"}") ?: return emptyList()
            val arr = JSONObject(body).optJSONArray("results") ?: return emptyList()
            return (0 until arr.length()).map { arr.getJSONObject(it) }.map { o ->
                FoundTrack(o.optString("trackName"), o.optString("artistName"), clean(o.optString("collectionName")), o.optInt("trackNumber"),
                    o.optLong("trackTimeMillis"), year(o), o.optString("primaryGenreName"), art(o), o.optInt("trackCount"))
            }
        }

        private fun close(a: String, b: String) = Keys.norm(a) == Keys.norm(b) || Keys.norm(a).startsWith(Keys.norm(b)) || Keys.norm(b).startsWith(Keys.norm(a))
    }

    private val lock = Mutex()

    /** Works through what's unknown, a few at a time. Safe to call often. */
    suspend fun drain(max: Int = 30) = withContext(Dispatchers.IO) {
        if (!lock.tryLock()) return@withContext
        try {
            var n = 0
            for (t in dao.uncheckedTracks(max)) { track(t); n++; delay(1_500) }
            for (a in dao.uncheckedAlbums(max)) { album(a); n++; delay(1_500) }
            for (a in dao.uncheckedArtists(max)) { artist(a); n++; delay(1_100) }
        } finally { lock.unlock() }
    }

    /** A song heard: its album (when the player didn't say), genre and year. */
    private suspend fun track(t: Track) {
        val ru = ctx.resources.configuration.locales[0].language == "ru"
        val hits = runCatching { searchTracks("${t.artistName} ${t.title}", ru) }.getOrDefault(emptyList())
        val hit = hits.firstOrNull { close(it.title, t.title) && close(Keys.mainArtist(it.artist), Keys.mainArtist(t.artistName)) }
        if (hit == null) { dao.upsert(t.copy(checked = System.currentTimeMillis())); return }
        val albumKey = t.album.ifEmpty { Keys.album(t.artistName, hit.album) }
        dao.upsert(t.copy(checked = System.currentTimeMillis(), number = hit.number, length = if (t.length > 0) t.length else hit.length,
            year = t.year ?: hit.year, genre = t.genre ?: Genre.of(hit.genreName), album = albumKey, albumName = t.albumName.ifEmpty { hit.album }))
        if (t.album.isEmpty()) dao.placeTrack(t.key, albumKey, hit.album)
        if (dao.album(albumKey) == null) dao.insertAlbum(Album(albumKey, hit.album, t.artistName, tracks = hit.albumTracks, year = hit.year,
            genre = Genre.of(hit.genreName), genreName = hit.genreName, kind = kindOf(hit.album, hit.albumTracks)))
    }

    private suspend fun album(a: Album) {
        val ru = ctx.resources.configuration.locales[0].language == "ru"
        val hits = runCatching { searchAlbums("${Keys.mainArtist(a.artistName)} ${a.title}", ru) }.getOrDefault(emptyList())
        val hit = hits.firstOrNull { close(it.title, a.title) && close(Keys.mainArtist(it.artist), Keys.mainArtist(a.artistName)) }
            ?: hits.firstOrNull { close(it.title, a.title) }
        if (hit == null) { dao.upsert(a.copy(checked = System.currentTimeMillis())); return }
        val cover = a.cover ?: hit.art?.let { Covers.download(ctx, a.key, it) }
        // Its length, from the album's track list.
        val length = runCatching {
            val body = get("https://itunes.apple.com/lookup?id=${hit.id}&entity=song") ?: return@runCatching 0L
            val arr = JSONObject(body).optJSONArray("results") ?: return@runCatching 0L
            var sum = 0L
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                if (o.optString("wrapperType") != "track") continue
                val len = o.optLong("trackTimeMillis"); sum += len
                val title = o.optString("trackName").ifBlank { continue }
                val key = Keys.track(a.artistName, title)
                if (dao.track(key) == null) dao.insertTrack(Track(key, title, a.artistName, album = a.key, albumName = a.title, number = o.optInt("trackNumber"),
                    length = len, year = year(o), genre = Genre.of(o.optString("primaryGenreName")), checked = System.currentTimeMillis()))
            }
            sum
        }.getOrDefault(0L)
        val now = dao.album(a.key) ?: a
        dao.upsert(now.copy(
            checked = System.currentTimeMillis(),
            year = if (now.pinned) now.year else now.year ?: hit.year,
            genre = if (now.pinned) now.genre else now.genre ?: Genre.of(hit.genreName),
            genreName = hit.genreName, tracks = if (now.tracks > 0) now.tracks else hit.tracks, cover = cover,
            length = if (now.length > 0) now.length else length, kind = kindOf(hit.title + if (hit.kind == Kind.SINGLE) " - Single" else "", hit.tracks),
        ))
    }

    private suspend fun artist(a: Artist) {
        val body = get("https://musicbrainz.org/ws/2/artist/?fmt=json&limit=3&query=${enc("artist:\"${a.name}\"")}")
        val arr = body?.let { runCatching { JSONObject(it).optJSONArray("artists") }.getOrNull() }
        val o = arr?.let { (0 until it.length()).map { i -> it.getJSONObject(i) }.firstOrNull { x -> close(x.optString("name"), a.name) } }
        val country = o?.optString("country")?.takeIf { it.length == 2 }
            ?: o?.optJSONObject("area")?.optJSONArray("iso-3166-1-codes")?.optString(0)?.takeIf { it.length == 2 }
            ?: o?.optJSONObject("begin-area")?.optJSONArray("iso-3166-1-codes")?.optString(0)?.takeIf { it.length == 2 }
            ?: ""
        // A person whose life-span has ended (MusicBrainz): the raven listens for them (#15). Bands that broke up don't count.
        val life = o?.optJSONObject("life-span")
        val gone = if (o?.optString("type") == "Person" && life?.optBoolean("ended") == true) (life.optString("end").take(4).toIntOrNull() ?: 1) else -1
        val now = dao.artist(a.key) ?: a
        dao.upsert(now.copy(checked = if (o == null) System.currentTimeMillis() - (System.currentTimeMillis() % RETRY) else System.currentTimeMillis(),
            country = now.country.ifBlank { country }, gone = gone))
    }

    /** Makes sure the catalogue rows exist for a new listen, so they get looked up. */
    suspend fun note(p: Play) {
        if (dao.artist(p.artist) == null) dao.insertArtist(Artist(p.artist, Keys.mainArtist(p.artistName)))
        if (dao.track(p.track) == null) dao.insertTrack(Track(p.track, p.title, p.artistName, album = p.album, albumName = p.albumName, length = p.length))
        if (p.album.isNotEmpty() && dao.album(p.album) == null) dao.insertAlbum(Album(p.album, p.albumName, p.artistName))
    }
}
