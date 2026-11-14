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

    @Query("SELECT * FROM albums WHERE checked = 0 LIMIT :n")
    suspend fun uncheckedAlbums(n: Int): List<Album>

    // --- artists
    @Query("SELECT * FROM artists")
    fun artists(): Flow<List<Artist>>

    @Query("SELECT * FROM artists WHERE `key` = :key")
    suspend fun artist(key: String): Artist?

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertArtist(a: Artist): Long
    @Upsert suspend fun upsert(a: Artist)

    /** Artists never looked up, or looked up before the app knew to ask whether they're still alive. */
    @Query("SELECT * FROM artists WHERE checked = 0 OR gone = 0 LIMIT :n")
    suspend fun uncheckedArtists(n: Int): List<Artist>

    // --- tracks
    @Query("SELECT * FROM tracks")
    fun tracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE `key` = :key")
    suspend fun track(key: String): Track?

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertTrack(t: Track): Long
    @Upsert suspend fun upsert(t: Track)
    @Delete suspend fun delete(t: Track)

    @Query("SELECT * FROM tracks WHERE checked = 0 LIMIT :n")
    suspend fun uncheckedTracks(n: Int): List<Track>

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

@Database(entities = [Play::class, Album::class, Artist::class, Track::class, Pet::class, Goal::class], version = 6, exportSchema = true,
    autoMigrations = [androidx.room.AutoMigration(from = 1, to = 2), androidx.room.AutoMigration(from = 2, to = 3), androidx.room.AutoMigration(from = 3, to = 4),
        androidx.room.AutoMigration(from = 4, to = 5), androidx.room.AutoMigration(from = 5, to = 6)])
@TypeConverters(Converters::class)
abstract class MusicDb : RoomDatabase() {
    abstract fun dao(): MusicDao

    companion object {
        fun create(context: Context): MusicDb = Room.databaseBuilder(context, MusicDb::class.java, "songling.db").build()
    }
}
