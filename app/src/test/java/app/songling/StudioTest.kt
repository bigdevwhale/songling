package app.songling

import app.songling.data.Album
import app.songling.data.Artist
import app.songling.data.Keys
import app.songling.data.MIN
import app.songling.data.Play
import app.songling.data.Settings
import app.songling.data.Track
import app.songling.domain.Genre
import app.songling.domain.Studio
import org.junit.Assert.assertEquals
import org.junit.Test

class StudioTest {
    private val t0 = 1_760_000_000_000L
    private fun play(title: String, at: Long, artist: String = "A", album: String = "Duo", ms: Long = 4 * MIN) =
        Play(at = at, ms = ms, title = title, artistName = artist, albumName = album, length = 4 * MIN)

    private fun studio(plays: List<Play>, albums: List<Album> = emptyList(), artists: List<Artist> = emptyList(), tracks: List<Track> = emptyList()) =
        Studio(plays, albums, artists, tracks, emptyList(), emptyList(), Settings(), t0 + 10 * 3_600_000L)

    private val duo = Album(Keys.album("A", "Duo"), "Duo", "A", tracks = 2, length = 8 * MIN)

    @Test fun twoSongAlbumHeardWholeInAnyOrderWithOthersBetween() {
        val st = studio(listOf(play("Two", t0), play("Other", t0 + 5 * MIN, album = "Else"), play("One", t0 + 10 * MIN)), listOf(duo))
        assertEquals(listOf(duo.key to t0), st.wholeRuns)
    }

    @Test fun oneSongIsNotTheWholeAlbum() {
        val big = Album(Keys.album("A", "Big"), "Big", "A", tracks = 10, length = 40 * MIN)
        val st = studio(listOf(play("One", t0, album = "Big", ms = 35 * MIN)), listOf(big))
        assertEquals(emptyList<Pair<String, Long>>(), st.wholeRuns)
    }

    @Test fun genreSetByHandOutranksTheCatalogue() {
        val p = play("One", t0)
        val song = Track(p.track, "One", "A", album = duo.key, genre = Genre.OTHER)
        assertEquals(Genre.JAZZ, studio(listOf(p), listOf(duo.copy(genre = Genre.JAZZ, pinned = true)), tracks = listOf(song)).genreOf(p))
        assertEquals(Genre.ROCK, studio(listOf(p), listOf(duo), listOf(Artist(p.artist, "A", genre = Genre.ROCK)), listOf(song)).genreOf(p))
        assertEquals(Genre.FOLK, studio(listOf(p), listOf(duo.copy(genre = Genre.JAZZ, pinned = true)), tracks = listOf(song.copy(genre = Genre.FOLK, pinned = true))).genreOf(p))
    }
}
