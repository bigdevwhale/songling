package app.songling.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.Upsert
import app.songling.domain.Form
import app.songling.domain.Genre
import app.songling.domain.Nature
import app.songling.domain.Species
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {
    // --- plays
    @Query("SELECT * FROM plays ORDER BY at")
    fun plays(): Flow<List<Play>>

    @Query("SELECT * FROM plays ORDER BY at")
    suspend fun playList(): List<Play>

    @Insert suspend fun insert(p: Play): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertPlays(p: List<Play>): List<Long>
    @Update suspend fun update(p: Play)
    @Delete suspend fun delete(p: Play)

    @Query("UPDATE plays SET ms = :ms, whole = :whole, skipped = :skipped WHERE id = :id")
    suspend fun grow(id: Long, ms: Long, whole: Boolean, skipped: Boolean)

    @Query("DELETE FROM plays WHERE source = 'IMPORT'")
    suspend fun forgetImported()

    @Query("SELECT COUNT(*) FROM plays")
    suspend fun playCount(): Int

    /** A listen from a player can't be longer than its song: a longer one was clocked while the player had stopped. */
    @Query("UPDATE plays SET ms = length, whole = 1 WHERE source = 'PLAYER' AND length > 0 AND ms > length")
    suspend fun trimOverlong()

    @Query("SELECT DISTINCT app FROM plays WHERE app != ''")
    suspend fun apps(): List<String>

    /** Every listen of an album named by the player one way, moved to the album the catalogue knows. */
    @Query("UPDATE plays SET album = :to, albumName = :name WHERE track = :track AND album = ''")
    suspend fun placeTrack(track: String, to: String, name: String)

    /** Every listen of one album moved to another (an album renamed or folded into one made by hand). */
    @Query("UPDATE plays SET album = :to, albumName = :name WHERE album = :from")
    suspend fun moveAlbum(from: String, to: String, name: String)

    /** Every listen of a song put on an album, wherever it was before. */
    @Query("UPDATE plays SET album = :to, albumName = :name WHERE track = :track")
    suspend fun moveTrack(track: String, to: String, name: String)

    @Query("DELETE FROM plays WHERE album = :album")
    suspend fun forgetAlbum(album: String)

    @Query("DELETE FROM plays WHERE track = :track")
    suspend fun forgetTrack(track: String)

    /**
     * An artist and everything billed first to them: listens, songs, albums. Songs where they're only featured
     * («X feat. them») stay with the artist who leads them.
     */
    @androidx.room.Transaction
    suspend fun forgetArtist(artist: String) {
        forgetArtistPlays(artist); forgetArtistTracks(artist); forgetArtistAlbums(artist); forgetArtistRow(artist)
    }

    @Query("DELETE FROM plays WHERE artist = :artist")
    suspend fun forgetArtistPlays(artist: String)

    @Query("DELETE FROM tracks WHERE artist = :artist")
    suspend fun forgetArtistTracks(artist: String)

    @Query("DELETE FROM albums WHERE artist = :artist")
    suspend fun forgetArtistAlbums(artist: String)

    @Query("DELETE FROM artists WHERE `key` = :artist")
    suspend fun forgetArtistRow(artist: String)

    @Query("SELECT (SELECT COUNT(*) FROM plays WHERE artist = :artist) + (SELECT COUNT(*) FROM tracks WHERE artist = :artist) + (SELECT COUNT(*) FROM albums WHERE artist = :artist)")
    suspend fun usesOfArtist(artist: String): Int

    @Query("SELECT * FROM tracks")
    suspend fun trackList(): List<Track>

    @Query("SELECT * FROM goals")
    suspend fun goalList(): List<Goal>

    /**
     * Keys an older version read differently from the name (it split «Король и Шут» into «Король» and «Шут»): each
     * listen's, song's and album's artist key worked out again from its name, with the keys that begin with it. Only
     * rows that differ are touched, so it's cheap to run on every start, and after a restore of an old backup.
     */
    @Transaction
    suspend fun rekeyArtists() {
        val memo = HashMap<String, String>()
        fun key(name: String) = memo.getOrPut(name) { Keys.artist(name) }
        fun move(k: String, from: String, to: String) = if (k.startsWith("$from|")) to + k.substring(from.length) else k
        val moved = HashMap<String, MutableMap<String, String>>()  // old key → new key → a name it came from
        fun fresh(old: String, name: String): String? {
            val n = key(name)
            if (n == old || n.isEmpty()) return null
            moved.getOrPut(old) { HashMap() }.putIfAbsent(n, name)
            return n
        }
        // Albums first, then songs: what points at an album or a song (a listen folded into another artist's album,
        // an alias, a goal) follows it by its old key, whoever's name the pointer carries.
        val albumMoves = HashMap<String, String>()
        val albums = albumList()
        for (a in albums) { val n = fresh(a.artist, a.artistName) ?: continue; move(a.key, a.artist, n).let { if (it != a.key) albumMoves[a.key] = it } }
        fun albumOf(k: String, from: String, to: String?) = albumMoves[k] ?: if (to != null) move(k, from, to) else k
        for (a in albums) {
            val n = key(a.artistName).takeIf { it != a.artist && it.isNotEmpty() }
            val aliases = if (a.aliases.isEmpty()) "" else a.aliases.split(',').joinToString(",") { albumMoves[it] ?: if (n != null) move(it, a.artist, n) else it }
            val k = albumMoves[a.key] ?: a.key
            if (n == null && aliases == a.aliases) continue
            if (k != a.key) { delete(a); insertAlbum(a.copy(key = k, artist = n ?: a.artist, aliases = aliases)) } else upsert(a.copy(artist = n ?: a.artist, aliases = aliases))
        }
        val trackMoves = HashMap<String, String>()
        for (t in trackList()) {
            val n = fresh(t.artist, t.artistName)
            val k = if (n != null) move(t.key, t.artist, n) else t.key
            val al = albumOf(t.album, t.artist, n)
            if (n == null && al == t.album) continue
            if (k != t.key) { trackMoves[t.key] = k; delete(t); insertTrack(t.copy(key = k, artist = n ?: t.artist, album = al)) } else upsert(t.copy(artist = n ?: t.artist, album = al))
        }
        for (p in playList()) {
            val n = fresh(p.artist, p.artistName)
            val tr = trackMoves[p.track] ?: if (n != null) move(p.track, p.artist, n) else p.track
            val al = albumOf(p.album, p.artist, n)
            if (n != null || tr != p.track || al != p.album) update(p.copy(artist = n ?: p.artist, track = tr, album = al))
        }
        if (moved.isEmpty()) return
        for (g in goalList()) {
            val ref = albumMoves[g.ref] ?: trackMoves[g.ref] ?: moved[g.ref]?.keys?.singleOrNull() ?: continue
            update(g.copy(ref = ref))
        }
        for ((old, news) in moved) {
            val row = artist(old)
            // What you set by hand stays; what the catalogue said was about the wrong name, so it's asked again.
            for ((n, name) in news) if (artist(n) == null)
                insertArtist((row ?: Artist(n, "")).copy(key = n, name = Keys.mainArtist(name), country = "", checked = 0, gone = 0, tagGenre = null, styles = ""))
            if (usesOfArtist(old) == 0) forgetArtistRow(old)
        }
    }

    @Query("SELECT * FROM plays WHERE album = :album ORDER BY at DESC LIMIT 1")
    suspend fun lastOfAlbum(album: String): Play?

    // --- albums
    @Query("SELECT * FROM albums")
    fun albums(): Flow<List<Album>>

    @Query("SELECT * FROM albums")
    suspend fun albumList(): List<Album>

    @Query("SELECT * FROM albums WHERE `key` = :key")
    suspend fun album(key: String): Album?

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAlbum(a: Album): Long
    @Upsert suspend fun upsert(a: Album)
    @Delete suspend fun delete(a: Album)

    /** The album a key was folded into, if any. */
    @Query("SELECT * FROM albums WHERE ',' || aliases || ',' LIKE '%,' || :key || ',%' LIMIT 1")
    suspend fun albumByAlias(key: String): Album?

    /**
     * Puts a song in another album by hand (#22): its listens and its row move there, and the album keeps
     * «t:<song>» among its aliases, so the song's next listens land there whatever album the player names.
     */
    @androidx.room.Transaction
    suspend fun placeTrackByHand(trackKey: String, to: Album, fallback: Track) {
        val mark = "t:$trackKey"
        albumByAlias(mark)?.let { old -> upsert(old.copy(aliases = old.aliases.split(',').filter { it.isNotBlank() && it != mark }.joinToString(","))) }
        val target = album(to.key) ?: to
        upsert(target.copy(aliases = (target.aliases.split(',').filter { it.isNotBlank() } + mark).distinct().joinToString(",")))
        moveTrack(trackKey, target.key, target.title)
        val t = track(trackKey) ?: fallback
        upsert(t.copy(album = target.key, albumName = target.title, number = if (t.album == target.key) t.number else 0))
    }

    @Query("UPDATE tracks SET album = :to, albumName = :name WHERE album = :from")
    suspend fun moveTracks(from: String, to: String, name: String)

    @Query("DELETE FROM tracks WHERE album = :album")
    suspend fun forgetTracks(album: String)

    /** New albums, then ones with no genre last looked up before [before]: catalogues fill in, and lookups fail. */
    @Query("SELECT * FROM albums WHERE checked = 0 OR (genre IS NULL AND pinned = 0 AND checked < :before) ORDER BY checked LIMIT :n")
    suspend fun uncheckedAlbums(n: Int, before: Long): List<Album>

    // --- artists
    @Query("SELECT * FROM artists")
    fun artists(): Flow<List<Artist>>

    @Query("SELECT * FROM artists WHERE `key` = :key")
    suspend fun artist(key: String): Artist?

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertArtist(a: Artist): Long
    @Upsert suspend fun upsert(a: Artist)

    /** Artists never looked up, or looked up before the app knew to ask whether they're still alive. */
    @Query("SELECT * FROM artists WHERE checked = 0 OR gone = 0 OR ((country = '' OR tagGenre IS NULL) AND checked < :before) ORDER BY checked LIMIT :n")
    suspend fun uncheckedArtists(n: Int, before: Long): List<Artist>

    // --- tracks
    @Query("SELECT * FROM tracks")
    fun tracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE `key` = :key")
    suspend fun track(key: String): Track?

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertTrack(t: Track): Long
    @Upsert suspend fun upsert(t: Track)
    @Delete suspend fun delete(t: Track)

    @Query("SELECT * FROM tracks WHERE checked = 0 OR (genre IS NULL AND pinned = 0 AND checked < :before) ORDER BY checked LIMIT :n")
    suspend fun uncheckedTracks(n: Int, before: Long): List<Track>

    // --- pets
    @Query("SELECT * FROM pets ORDER BY laid")
    fun pets(): Flow<List<Pet>>

    @Query("SELECT * FROM pets ORDER BY laid")
    suspend fun petList(): List<Pet>

    @Insert suspend fun insert(p: Pet): Long
    @Update suspend fun update(p: Pet)

    // --- goals
    @Query("SELECT * FROM goals ORDER BY created")
    fun goals(): Flow<List<Goal>>

    @Insert suspend fun insert(g: Goal): Long
    @Update suspend fun update(g: Goal)
    @Delete suspend fun delete(g: Goal)

    @Transaction
    suspend fun wipe() { clearPlays(); clearAlbums(); clearArtists(); clearTracks(); clearPets(); clearGoals() }
    @Query("DELETE FROM plays") suspend fun clearPlays()
    @Query("DELETE FROM albums") suspend fun clearAlbums()
    @Query("DELETE FROM artists") suspend fun clearArtists()
    @Query("DELETE FROM tracks") suspend fun clearTracks()
    @Query("DELETE FROM pets") suspend fun clearPets()
    @Query("DELETE FROM goals") suspend fun clearGoals()
}

class Converters {
    @TypeConverter fun genre(g: Genre?): String? = g?.name
    @TypeConverter fun genre(s: String?): Genre? = s?.let { n -> Genre.entries.firstOrNull { it.name == n } }
    @TypeConverter fun form(f: Form?): String? = f?.name
    @TypeConverter fun form(s: String?): Form? = s?.let { n -> Form.entries.firstOrNull { it.name == n } }
    @TypeConverter fun species(f: Species): String = f.name
    @TypeConverter fun species(s: String): Species = Species.entries.firstOrNull { it.name == s } ?: Species.LYREBIRD
    @TypeConverter fun nature(f: Nature): String = f.name
    @TypeConverter fun nature(s: String): Nature = Nature.entries.firstOrNull { it.name == s } ?: Nature.CALM
    @TypeConverter fun source(f: Source): String = f.name
    @TypeConverter fun source(s: String): Source = Source.entries.firstOrNull { it.name == s } ?: Source.PLAYER
    @TypeConverter fun kind(f: Kind): String = f.name
    @TypeConverter fun kind(s: String): Kind = Kind.entries.firstOrNull { it.name == s } ?: Kind.ALBUM
    @TypeConverter fun goal(f: GoalKind): String = f.name
    @TypeConverter fun goal(s: String): GoalKind = GoalKind.entries.firstOrNull { it.name == s } ?: GoalKind.MINUTES
    @TypeConverter fun period(f: Period): String = f.name
    @TypeConverter fun period(s: String): Period = Period.entries.firstOrNull { it.name == s } ?: Period.MONTH
}

@Database(entities = [Play::class, Album::class, Artist::class, Track::class, Pet::class, Goal::class], version = 7, exportSchema = true,
    autoMigrations = [androidx.room.AutoMigration(from = 1, to = 2), androidx.room.AutoMigration(from = 2, to = 3), androidx.room.AutoMigration(from = 3, to = 4),
        androidx.room.AutoMigration(from = 4, to = 5), androidx.room.AutoMigration(from = 5, to = 6),
        androidx.room.AutoMigration(from = 6, to = 7, spec = MusicDb.LookAgain::class)])
@TypeConverters(Converters::class)
abstract class MusicDb : RoomDatabase() {
    abstract fun dao(): MusicDao

    /**
     * 7: what an older version couldn't find (it asked the store of the phone's language, so «Король и Шут» came back
     * as «Korol I Shut» on an English phone) is looked up once more, artists for the genre their tags suggest too.
     */
    class LookAgain : androidx.room.migration.AutoMigrationSpec {
        override fun onPostMigrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("UPDATE tracks SET checked = 0 WHERE genre IS NULL AND pinned = 0")
            db.execSQL("UPDATE albums SET checked = 0 WHERE genre IS NULL AND pinned = 0")
            db.execSQL("UPDATE artists SET checked = 0")
        }
    }

    companion object {
        fun create(context: Context): MusicDb = Room.databaseBuilder(context, MusicDb::class.java, "songling.db").build()
    }
}
