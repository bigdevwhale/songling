package app.songling.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import app.songling.domain.Form
import app.songling.domain.Genre
import app.songling.domain.Nature
import app.songling.domain.Species
import java.text.Normalizer

const val MIN = 60_000L
const val HOUR = 3_600_000L
const val DAY = 86_400_000L

/** Where a listen came from. */
enum class Source { PLAYER, MANUAL, VINYL, RADIO, CONCERT, IMPORT }

/** What a release is. */
enum class Kind { ALBUM, EP, SINGLE }

/**
 * One listen: a track played (in part or whole). Players report through their media session; the
 * listener writes the row as soon as the track counts and keeps its [ms] growing while it plays.
 */
@Entity(tableName = "plays", indices = [Index("at"), Index("track"), Index("album"), Index("artist"), Index("ref", unique = true)])
data class Play(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** When it started. */
    val at: Long,
    /** How long it actually played. */
    val ms: Long,
    val title: String,
    val artistName: String,
    val albumName: String = "",
    val track: String = Keys.track(artistName, title),
    val artist: String = Keys.artist(artistName),
    val album: String = if (albumName.isBlank()) "" else Keys.album(artistName, albumName),
    /** The track's length, 0 when unknown. */
    val length: Long = 0,
    val source: Source = Source.PLAYER,
    /** The player's package, for filters and exclusions. */
    val app: String = "",
    /** Played to (nearly) the end. */
    val whole: Boolean = false,
    /** Skipped well before the end. */
    val skipped: Boolean = false,
    val headphones: Boolean = false,
    /** Where an imported listen came from, so importing again doesn't count it twice. */
    val ref: String? = null,
)

/** An artist as the catalogues know them. Filled in by Meta. */
@Entity(tableName = "artists")
data class Artist(
    @PrimaryKey val key: String,
    val name: String,
    /** ISO 3166 code, empty when unknown. */
    val country: String = "",
    /** Our genre, when told by hand. */
    val genre: Genre? = null,
    @ColumnInfo(defaultValue = "0") val checked: Long = 0,
    @ColumnInfo(defaultValue = "0") val stars: Int = 0,
    @ColumnInfo(defaultValue = "0") val wish: Boolean = false,
    @ColumnInfo(defaultValue = "0") val added: Long = 0,
    /** A genre of one's own (its id in Settings.own); overrides [genre] for what's shown. */
    @ColumnInfo(defaultValue = "") val own: String = "",
    /** A person who has passed away: the year (1 when it's known but not the year); -1 checked and not; 0 not asked yet. */
    @ColumnInfo(defaultValue = "0") val gone: Int = 0,
)

/** An album, EP or single. Listening fills it in; it can also be put on the shelf by hand. */
@Entity(tableName = "albums", indices = [Index("artist")])
data class Album(
    @PrimaryKey val key: String,
    val title: String,
    val artistName: String,
    val artist: String = Keys.artist(artistName),
    val kind: Kind = Kind.ALBUM,
    val year: Int? = null,
    val genre: Genre? = null,
    /** The genre as the catalogue named it. */
    val genreName: String = "",
    /** A file in filesDir/covers. */
    val cover: String? = null,
    /** Tracks on it, 0 when unknown. */
    val tracks: Int = 0,
    /** Its length, 0 when unknown. */
    val length: Long = 0,
    @ColumnInfo(defaultValue = "0") val checked: Long = 0,
    @ColumnInfo(defaultValue = "0") val stars: Int = 0,
    /** On the «want to hear» list. */
    @ColumnInfo(defaultValue = "0") val wish: Boolean = false,
    @ColumnInfo(defaultValue = "0") val added: Long = 0,
    /** Year and genre were set by hand: the catalogue doesn't overwrite them. */
    @ColumnInfo(defaultValue = "0") val pinned: Boolean = false,
    @ColumnInfo(defaultValue = "") val note: String = "",
    /** A genre of one's own (its id in Settings.own); overrides [genre] for what's shown. */
    @ColumnInfo(defaultValue = "") val own: String = "",
    /**
     * Other album keys whose listens belong here, comma-separated: a player's «Unnamed» or a misspelt name the user
     * folded into this album. New listens under those keys are put here as they come.
     */
    @ColumnInfo(defaultValue = "") val aliases: String = "",
    /** Genres beside the main one (#23), comma-separated: a [Genre] name or «own:<id>». */
    @ColumnInfo(defaultValue = "") val more: String = "",
)

/** A song: what the catalogue knows, and what you think of it. */
@Entity(tableName = "tracks", indices = [Index("album"), Index("artist")])
data class Track(
    @PrimaryKey val key: String,
    val title: String,
    val artistName: String,
    val artist: String = Keys.artist(artistName),
    val album: String = "",
    val albumName: String = "",
    val number: Int = 0,
    val length: Long = 0,
    val year: Int? = null,
    val genre: Genre? = null,
    @ColumnInfo(defaultValue = "0") val checked: Long = 0,
    @ColumnInfo(defaultValue = "0") val stars: Int = 0,
    @ColumnInfo(defaultValue = "0") val wish: Boolean = false,
    @ColumnInfo(defaultValue = "0") val added: Long = 0,
    @ColumnInfo(defaultValue = "0") val loved: Boolean = false,
    /** A genre of one's own (its id in Settings.own); overrides [genre] for what's shown. */
    @ColumnInfo(defaultValue = "") val own: String = "",
    /** The genre was set by hand: it outranks the album's and the artist's. */
    @ColumnInfo(defaultValue = "0") val pinned: Boolean = false,
    /** Genres beside the main one (#23), comma-separated: a [Genre] name or «own:<id>». */
    @ColumnInfo(defaultValue = "") val more: String = "",
)

/** A pet: hatched from an egg of [species], fed by the music played while it was out of the box. */
@Entity(tableName = "pets")
data class Pet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val species: Species,
    val name: String = "",
    val laid: Long = System.currentTimeMillis(),
    val shiny: Boolean = false,
    val nature: Nature = Nature.CALM,
    /** Set when it grows up, for good. */
    val form: Form? = null,
    /** When it was out, as «from-to» pairs (to = 0 while it's out now). */
    val spans: String = "$laid-0",
    /** Minutes it ate before this install (from a backup of another phone). */
    @ColumnInfo(defaultValue = "0") val extra: Int = 0,
)

enum class GoalKind { MINUTES, ALBUMS, NEW_ARTISTS, NEW_TRACKS, ARTIST, ALBUM, TRACK, GENRE, GENRES, COUNTRIES, DECADES, DAYS, WISHES, PLAYS, NEW_ALBUMS, NEW_GENRES, NEW_COUNTRIES }

enum class Period { WEEK, MONTH, YEAR, ONCE }

/** Something to reach: minutes, whole albums, new artists, hours of one artist… */
@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: GoalKind,
    val target: Int,
    val period: Period = Period.MONTH,
    /** What it's about: an artist, album or track key, or a genre name. */
    val ref: String = "",
    /** Its name as shown. */
    val label: String = "",
    val created: Long = System.currentTimeMillis(),
    /** For ONCE: the last day it counts, 0 for no end. */
    val until: Long = 0,
    @ColumnInfo(defaultValue = "0") val archived: Boolean = false,
)

/** Keys that join a player's spelling of a name to the catalogue's. */
object Keys {
    private val feat = Regex("""\s*[(\[]?\s*(feat\.?|ft\.?|featuring|при уч\.?)\s.*$""", RegexOption.IGNORE_CASE)
    private val tail = Regex("""\s*[(\[](remaster(ed)?|deluxe|expanded|anniversary|bonus|explicit|edition|version)[^)\]]*[)\]]\s*$""", RegexOption.IGNORE_CASE)
    private val junk = Regex("""[^\p{L}\p{N}]+""")

    fun norm(s: String): String {
        var t = s.trim()
        repeat(2) { t = t.replace(tail, "") }
        t = Normalizer.normalize(t.lowercase(), Normalizer.Form.NFKD).replace(Regex("\\p{Mn}+"), "")
        return t.replace('ё', 'е').replace(junk, " ").trim()
    }

    /** The first-billed artist: «A, B & C» and «A feat. B» are A. */
    fun mainArtist(s: String): String = s.replace(feat, "").split(Regex("""\s*(,|;| & | x | и | with )\s*""")).first().trim().ifBlank { s.trim() }

    private val joiners = Regex("""\s*(,|;| / | & | x | и | with |\b(?:feat\.?|ft\.?|featuring)(?=\s)|при уч\.?)\s*""", RegexOption.IGNORE_CASE)
    private val many = java.util.concurrent.ConcurrentHashMap<String, List<String>>()

    /**
     * Everyone billed on a track, first-billed first: «A, B feat. C» is A, B and C (#20: a song by two artists counts
     * for both). Names, as written.
     */
    fun allArtists(s: String): List<String> = many.getOrPut(s) {
        s.replace(Regex("""[()\[\]]"""), " ").split(joiners).map { it.trim() }.filter { it.isNotEmpty() }
            .distinctBy { norm(it) }.ifEmpty { listOf(s.trim()) }
    }

    /** The keys of everyone billed, the first-billed one first. */
    fun artists(name: String): List<String> = listOf(artist(name)) + allArtists(name).map { norm(it) }.filter { it != artist(name) }.distinct()

    fun artist(name: String) = norm(mainArtist(name))
    fun album(artist: String, album: String) = artist(artist) + "|" + norm(album)
    fun track(artist: String, title: String) = artist(artist) + "|" + norm(title.replace(feat, ""))
}
