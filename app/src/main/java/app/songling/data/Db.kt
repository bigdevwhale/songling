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

    @Query("SELECT DISTINCT app FROM plays WHERE app != ''")
    suspend fun apps(): List<String>

    /** Every listen of an album named by the player one way, moved to the album the catalogue knows. */
    @Query("UPDATE plays SET album = :to, albumName = :name WHERE track = :track AND album = ''")
    suspend fun placeTrack(track: String, to: String, name: String)

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

    @Query("SELECT * FROM albums WHERE checked = 0 LIMIT :n")
    suspend fun uncheckedAlbums(n: Int): List<Album>

    // --- artists
    @Query("SELECT * FROM artists")
    fun artists(): Flow<List<Artist>>

    @Query("SELECT * FROM artists WHERE `key` = :key")
    suspend fun artist(key: String): Artist?

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertArtist(a: Artist): Long
    @Upsert suspend fun upsert(a: Artist)

    @Query("SELECT * FROM artists WHERE checked = 0 LIMIT :n")
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

@Database(entities = [Play::class, Album::class, Artist::class, Track::class, Pet::class, Goal::class], version = 2, exportSchema = true,
    autoMigrations = [androidx.room.AutoMigration(from = 1, to = 2)])
@TypeConverters(Converters::class)
abstract class MusicDb : RoomDatabase() {
    abstract fun dao(): MusicDao

    companion object {
        fun create(context: Context): MusicDb = Room.databaseBuilder(context, MusicDb::class.java, "songling.db").build()
    }
}
