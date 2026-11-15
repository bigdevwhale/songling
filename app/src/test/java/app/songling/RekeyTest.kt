package app.songling

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.songling.data.Album
import app.songling.data.Artist
import app.songling.data.Goal
import app.songling.data.GoalKind
import app.songling.data.MIN
import app.songling.data.MusicDb
import app.songling.data.Play
import app.songling.data.Track
import app.songling.domain.Genre
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Keys an older version made by splitting «Король и Шут» at «и» are put right, with everything that points at them. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RekeyTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MusicDb::class.java).allowMainThreadQueries().build()
    private val dao = db.dao()

    @After fun close() = db.close()

    @Test fun oldSplitKeysAreMoved() = runBlocking {
        val name = "Король и Шут"
        // As the old version wrote them: the artist's key was «король».
        dao.insert(Play(at = 1, ms = 3 * MIN, title = "Лесник", artistName = name, albumName = "Акустический альбом",
            track = "король|лесник", artist = "король", album = "король|акустический альбом"))
        // A listen of another spelling folded into their album by hand.
        dao.insert(Play(at = 2, ms = 2 * MIN, title = "Кукла колдуна", artistName = "КиШ", album = "король|акустический альбом", albumName = "Акустический альбом"))
        dao.upsert(Track("король|лесник", "Лесник", name, artist = "король", album = "король|акустический альбом", stars = 5))
        dao.upsert(Album("король|акустический альбом", "Акустический альбом", name, artist = "король", stars = 4, aliases = "король|акустика"))
        dao.upsert(Artist("король", "Король", country = "XX", genre = Genre.PUNK, checked = 5, stars = 3, tagGenre = Genre.POP, styles = "pop"))
        dao.insert(Goal(kind = GoalKind.ARTIST, target = 60, ref = "король"))
        dao.insert(Goal(kind = GoalKind.ALBUM, target = 60, ref = "король|акустический альбом"))
        // Someone else, untouched.
        dao.insert(Play(at = 3, ms = MIN, title = "Ride", artistName = "Royal Blood"))

        dao.rekeyArtists()

        val plays = dao.playList()
        val kish = plays.first { it.title == "Лесник" }
        assertEquals("король и шут", kish.artist)
        assertEquals("король и шут|лесник", kish.track)
        assertEquals("король и шут|акустический альбом", kish.album)
        assertEquals("король и шут|акустический альбом", plays.first { it.title == "Кукла колдуна" }.album)
        assertEquals("royal blood", plays.first { it.title == "Ride" }.artist)

        assertNull(dao.track("король|лесник"))
        val song = dao.track("король и шут|лесник")
        assertNotNull(song); assertEquals(5, song!!.stars); assertEquals("король и шут|акустический альбом", song.album)

        assertNull(dao.album("король|акустический альбом"))
        val album = dao.album("король и шут|акустический альбом")
        assertNotNull(album); assertEquals(4, album!!.stars); assertEquals("король и шут|акустика", album.aliases)

        // The old row goes; the new one keeps what was set by hand and asks the catalogue again.
        assertNull(dao.artist("король"))
        val artist = dao.artist("король и шут")!!
        assertEquals("Король и Шут", artist.name)
        assertEquals(Genre.PUNK, artist.genre); assertEquals(3, artist.stars)
        assertEquals("", artist.country); assertEquals(0L, artist.checked); assertNull(artist.tagGenre); assertEquals("", artist.styles)

        assertEquals(setOf("король и шут", "король и шут|акустический альбом"), dao.goalList().map { it.ref }.toSet())

        // A second run finds nothing to do.
        dao.rekeyArtists()
        assertEquals(plays, dao.playList())
        assertTrue(dao.trackList().none { it.key.startsWith("король|") })
    }
}
