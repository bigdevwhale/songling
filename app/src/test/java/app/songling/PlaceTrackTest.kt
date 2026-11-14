package app.songling

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.songling.data.Album
import app.songling.data.Keys
import app.songling.data.MIN
import app.songling.data.MusicDb
import app.songling.data.Play
import app.songling.data.Track
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A song put in another album by hand (#22): its listens move, and the album remembers it for the next ones. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlaceTrackTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MusicDb::class.java).allowMainThreadQueries().build()
    private val dao = db.dao()

    @After fun close() = db.close()

    @Test fun songMovesWithItsListensAndStays() = runBlocking {
        // The player named the wrong album for the last song.
        val right = Album(Keys.album("Зимовье зверей", "Оба неба"), "Оба неба", "Зимовье зверей")
        val wrong = Album(Keys.album("Зимовье зверей", "Сборник"), "Сборник", "Зимовье зверей")
        dao.upsert(right); dao.upsert(wrong)
        val p = Play(at = 1_000, ms = 3 * MIN, title = "Долгая дорога в Дублин", artistName = "Зимовье зверей", albumName = "Сборник")
        dao.insert(p)
        val song = Track(p.track, p.title, p.artistName, album = wrong.key, albumName = wrong.title, number = 4)
        dao.upsert(song)

        dao.placeTrackByHand(song.key, right, song)

        assertEquals(right.key, dao.playList().single().album)
        assertEquals(right.key, dao.track(song.key)!!.album)
        assertEquals(0, dao.track(song.key)!!.number)
        // The next listen, still named «Сборник» by the player, is found by the song.
        assertEquals(right.key, dao.albumByAlias("t:" + song.key)?.key)

        // Moved again: only the last album keeps it.
        dao.placeTrackByHand(song.key, wrong, song)
        assertEquals(wrong.key, dao.albumByAlias("t:" + song.key)?.key)
        assertFalse(dao.album(right.key)!!.aliases.contains("t:"))
        assertTrue(dao.album(wrong.key)!!.aliases.split(',').contains("t:" + song.key))
    }
}
