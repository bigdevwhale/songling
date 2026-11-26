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
        fun searchAlbums(q: String, ru: Boolean): List<Found> = albumsOrNull(q, ru).orEmpty()

        /** The same, null when the store didn't answer (offline, or asked too often): not the same as nothing found. */
        private fun albumsOrNull(q: String, ru: Boolean): List<Found>? {
            val body = get("https://itunes.apple.com/search?media=music&entity=album&limit=20&term=${enc(q)}&country=${if (ru) "ru" else "us"}") ?: return null
            val arr = runCatching { JSONObject(body).optJSONArray("results") }.getOrNull() ?: return null
            return (0 until arr.length()).map { arr.getJSONObject(it) }.map { o ->
                val title = o.optString("collectionName"); val n = o.optInt("trackCount")
                Found(clean(title), o.optString("artistName"), year(o), o.optString("primaryGenreName"), n, art(o), kindOf(title, n), o.optLong("collectionId"))
            }
        }

        /** Songs for a free search, for adding by hand. */
        fun searchTracks(q: String, ru: Boolean): List<FoundTrack> = tracksOrNull(q, ru).orEmpty()

        private fun tracksOrNull(q: String, ru: Boolean): List<FoundTrack>? {
            val body = get("https://itunes.apple.com/search?media=music&entity=song&limit=20&term=${enc(q)}&country=${if (ru) "ru" else "us"}") ?: return null
            val arr = runCatching { JSONObject(body).optJSONArray("results") }.getOrNull() ?: return null
            return (0 until arr.length()).map { arr.getJSONObject(it) }.map { o ->
                FoundTrack(o.optString("trackName"), o.optString("artistName"), clean(o.optString("collectionName")), o.optInt("trackNumber"),
                    o.optLong("trackTimeMillis"), year(o), o.optString("primaryGenreName"), art(o), o.optInt("trackCount"))
            }
        }

        private fun close(a: String, b: String): Boolean {
            val x = Keys.norm(a); val y = Keys.norm(b)
            return x.isNotEmpty() && y.isNotEmpty() && (x == y || x.startsWith(y) || y.startsWith(x))
        }

        private val cyrillic = Regex("""\p{IsCyrillic}""")

        /**
         * The stores to ask, likeliest first. By the name's own letters, not the phone's language: the US store spells
         * «Король и Шут» as «Korol I Shut», which doesn't match, and a Russian song on an English phone went unfound.
         */
        fun stores(vararg names: String): List<Boolean> = if (names.any { cyrillic.containsMatchIn(it) }) listOf(true, false) else listOf(false, true)

        /** An artist's MusicBrainz tags that name a style of music (not «british» or «seen live»), with their votes, most first. */
        private fun musicTags(o: JSONObject?): List<Pair<String, Int>> {
            val tags = o?.optJSONArray("tags") ?: return emptyList()
            return (0 until tags.length()).mapNotNull { i ->
                val t = tags.optJSONObject(i) ?: return@mapNotNull null
                val name = t.optString("name").trim().lowercase()
                if (Genre.of(name).let { it == null || it == Genre.OTHER }) null else name to t.optInt("count", 1).coerceAtLeast(1)
            }.sortedByDescending { it.second }
        }

        /** The genre most of an artist's tags point to (weighted by votes), or null. */
        fun tagGenre(o: JSONObject?): Genre? =
            musicTags(o).groupBy({ Genre.of(it.first)!! }, { it.second }).maxByOrNull { it.value.sum() }?.key

        /** The artist's styles as the tags name them, the three most voted. */
        fun styles(o: JSONObject?): List<String> = musicTags(o).map { it.first }.distinct().take(3)

        /** Whether a MusicBrainz artist is the one called [name]: by its name, the name it sorts by, or an alias. */
        fun named(o: JSONObject, name: String): Boolean {
            if (close(o.optString("name"), name) || close(o.optString("sort-name"), name)) return true
            val aliases = o.optJSONArray("aliases") ?: return false
            return (0 until aliases.length()).any { i -> aliases.optJSONObject(i)?.let { close(it.optString("name"), name) } == true }
        }
    }

    private val lock = Mutex()

    /** Works through what's unknown, a few at a time. Safe to call often. */
    suspend fun drain(max: Int = 30) = withContext(Dispatchers.IO) {
        if (!lock.tryLock()) return@withContext
        try {
            var n = 0
            val before = System.currentTimeMillis() - RETRY
            for (t in dao.uncheckedTracks(max, before)) { track(t); n++; delay(1_500) }
            for (a in dao.uncheckedAlbums(max, before)) { album(a); n++; delay(1_500) }
            for (a in dao.uncheckedArtists(max, before)) { artist(a); n++; delay(1_500) }
        } finally { lock.unlock() }
    }

    /**
     * Asks the [stores] in turn for the first [pick] that fits, a pause between them. Null with [failed] set when a store
     * didn't answer, so the thing is asked again in an hour rather than in two weeks.
     */
    private class Asked<T>(val hit: T?, val failed: Boolean)
    private suspend fun <R, T> ask(stores: List<Boolean>, search: (Boolean) -> List<R>?, pick: (List<R>) -> T?): Asked<T> {
        var failed = false
        stores.forEachIndexed { i, ru ->
            if (i > 0) delay(1_500)
            val hits = runCatching { search(ru) }.getOrNull()
            if (hits == null) failed = true else pick(hits)?.let { return Asked(it, false) }
        }
        return Asked(null, failed)
    }
    private val later get() = System.currentTimeMillis() - RETRY + HOUR

    /** A song heard: its album (when the player didn't say), genre and year. */
    private suspend fun track(t: Track) {
        val asked = ask(stores(t.artistName, t.title), { ru -> tracksOrNull("${t.artistName} ${t.title}", ru) }) { hits ->
            hits.firstOrNull { close(it.title, t.title) && close(Keys.mainArtist(it.artist), Keys.mainArtist(t.artistName)) }
        }
        val hit = asked.hit
        if (hit == null) { dao.upsert(t.copy(checked = if (asked.failed) later else System.currentTimeMillis())); return }
        val albumKey = t.album.ifEmpty { Keys.album(t.artistName, hit.album) }
        dao.upsert(t.copy(checked = System.currentTimeMillis(), number = hit.number, length = if (t.length > 0) t.length else hit.length,
            year = t.year ?: hit.year, genre = t.genre?.takeIf { it != Genre.OTHER || t.pinned } ?: Genre.of(hit.genreName), album = albumKey, albumName = t.albumName.ifEmpty { hit.album }))
        if (t.album.isEmpty()) dao.placeTrack(t.key, albumKey, hit.album)
        if (dao.album(albumKey) == null) dao.insertAlbum(Album(albumKey, hit.album, t.artistName, tracks = hit.albumTracks, year = hit.year,
            genre = Genre.of(hit.genreName), genreName = hit.genreName, kind = kindOf(hit.album, hit.albumTracks)))
    }

    private suspend fun album(a: Album) {
        val asked = ask(stores(a.artistName, a.title), { ru -> albumsOrNull("${Keys.mainArtist(a.artistName)} ${a.title}", ru) }) { hits ->
            hits.firstOrNull { close(it.title, a.title) && close(Keys.mainArtist(it.artist), Keys.mainArtist(a.artistName)) }
                ?: hits.firstOrNull { close(it.title, a.title) }
        }
        val hit = asked.hit
        if (hit == null) { dao.upsert(a.copy(checked = if (asked.failed) later else System.currentTimeMillis())); return }
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
            // «Other» is no answer: a name read better now (#64) takes its place.
            genre = if (now.pinned) now.genre else now.genre?.takeIf { it != Genre.OTHER } ?: Genre.of(hit.genreName),
            genreName = hit.genreName, tracks = if (now.tracks > 0) now.tracks else hit.tracks, cover = cover,
            length = if (now.length > 0) now.length else length, kind = kindOf(hit.title + if (hit.kind == Kind.SINGLE) " - Single" else "", hit.tracks),
        ))
    }

    private suspend fun artist(a: Artist) {
        val q = a.name.replace("\"", "")
        val body = get("https://musicbrainz.org/ws/2/artist/?fmt=json&limit=5&query=${enc("artist:\"$q\" OR alias:\"$q\"")}")
        if (body == null) { dao.upsert(a.copy(checked = later)); return }  // offline or busy: again in an hour
        val arr = runCatching { JSONObject(body).optJSONArray("artists") }.getOrNull()
        val o = arr?.let { (0 until it.length()).map { i -> it.getJSONObject(i) }.firstOrNull { x -> x.optInt("score") >= 80 && named(x, a.name) } }
        val country = o?.optString("country")?.takeIf { it.length == 2 }
            ?: o?.optJSONObject("area")?.optJSONArray("iso-3166-1-codes")?.optString(0)?.takeIf { it.length == 2 }
            ?: o?.optJSONObject("begin-area")?.optJSONArray("iso-3166-1-codes")?.optString(0)?.takeIf { it.length == 2 }
            ?: ""
        // A person whose life-span has ended (MusicBrainz): the raven listens for them (#15). Bands that broke up don't count.
        val life = o?.optJSONObject("life-span")
        val type = o?.optString("type").orEmpty()
        val ended = when { o == null -> 0; life?.optBoolean("ended") == true -> life.optString("end").take(4).toIntOrNull() ?: 1; else -> -1 }
        val gone = if (type == "Person" && ended > 0) ended else -1
        val now = dao.artist(a.key) ?: a
        dao.upsert(now.copy(checked = System.currentTimeMillis(), country = now.country.ifBlank { country }, gone = gone, tagGenre = tagGenre(o) ?: now.tagGenre,
            styles = styles(o).joinToString(",").ifEmpty { now.styles }, type = type, gender = if (type == "Person") o?.optString("gender").orEmpty() else "",
            born = life?.optString("begin")?.take(4)?.toIntOrNull() ?: 0, ended = ended))
    }

    /** Makes sure the catalogue rows exist for a new listen, so they get looked up. */
    suspend fun note(p: Play) {
        if (dao.artist(p.artist) == null) dao.insertArtist(Artist(p.artist, Keys.mainArtist(p.artistName)))
        if (dao.track(p.track) == null) dao.insertTrack(Track(p.track, p.title, p.artistName, album = p.album, albumName = p.albumName, length = p.length))
        if (p.album.isNotEmpty() && dao.album(p.album) == null) dao.insertAlbum(Album(p.album, p.albumName, p.artistName))
    }
}
