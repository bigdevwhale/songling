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
    /** A song by two artists counts for both (#20); names with «ft» or a slash inside stay whole. */
    @Test fun everyBilledArtistGetsTheListen() {
        assertEquals(listOf("Баста", "Скриптонит"), Keys.allArtists("Баста, Скриптонит"))
        assertEquals(listOf("Daft Punk", "Pharrell Williams"), Keys.allArtists("Daft Punk feat. Pharrell Williams"))
        assertEquals(listOf("AC/DC"), Keys.allArtists("AC/DC"))
        val p = Play(at = t0, ms = 3 * MIN, title = "Song", artistName = "Баста, Скриптонит")
        val st = studio(listOf(p))
        assertEquals(3 * MIN, st.byArtist[Keys.norm("Баста")]?.ms)
        assertEquals(3 * MIN, st.byArtist[Keys.norm("Скриптонит")]?.ms)
        assertEquals("Скриптонит", st.artistName(Keys.norm("Скриптонит")))
    }

    /** Several genres at once (#23): the listen counts in full for each, the song's list outranks the album's. */
    @Test fun aListenCountsForEveryGenre() {
        val p = play("One", t0)
        val album = duo.copy(genre = Genre.ROCK, pinned = true, more = "POP")
        val st = studio(listOf(p), listOf(album))
        assertEquals(listOf(Genre.ROCK, Genre.POP), st.genresOf(p))
        assertEquals(4 * MIN, st.byGenre[Genre.ROCK])
        assertEquals(4 * MIN, st.byGenre[Genre.POP])
        val song = Track(p.track, "One", "A", album = duo.key, genre = Genre.JAZZ, pinned = true, more = "FOLK")
        assertEquals(listOf(Genre.JAZZ, Genre.FOLK), studio(listOf(p), listOf(album), tracks = listOf(song)).genresOf(p))
        // A song told by hand with one genre doesn't take the album's extra ones.
        assertEquals(listOf(Genre.JAZZ), studio(listOf(p), listOf(album), tracks = listOf(song.copy(more = ""))).genresOf(p))
    }

    /** A pet's genre as the main one is a whole favourite; as an extra one, half (#23). */
    @Test fun anExtraGenreIsHalfAFavourite() {
        val p = play("One", t0)
        val sp = app.songling.domain.Species.BASILISK
        val st = studio(listOf(p), listOf(duo.copy(genre = Genre.ROCK, pinned = true, more = "POP")))
        assertEquals(1.0, st.favourite(sp, p, st.genresOf(p)), 0.0)
        val st2 = studio(listOf(p), listOf(duo.copy(genre = Genre.POP, pinned = true, more = "ROCK")))
        assertEquals(0.5, st2.favourite(sp, p, st2.genresOf(p)), 0.0)
        assertEquals(0.0, st2.favourite(app.songling.domain.Species.WHALE, p, st2.genresOf(p)), 0.0)
    }

    /** A listen's length to the nearest half minute (#24). */
    @Test fun historyRoundsToHalfMinutes() {
        fun d(m: Int, s: Int) = app.songling.domain.Words.songDur((m * 60L + s) * 1000, true)
        assertEquals("1 мин", d(1, 15))
        assertEquals("1,5 мин", d(1, 16))
        assertEquals("1,5 мин", d(1, 44))
        assertEquals("2 мин", d(1, 50))
        assertEquals("2 мин", d(2, 15))
        assertEquals("0,5 мин", d(0, 5))
    }

    /** An imported history feeds no pet, and doesn't make an artist the lyrebird already ate as new «not new» (#import). */
    @Test fun importsDontFeedPets() {
        val pet = app.songling.data.Pet(1, app.songling.domain.Species.LYREBIRD, laid = t0 - 1000)
        val live = play("One", t0, artist = "Fresh")
        val old = play("Old", t0 - 400 * 24 * 3_600_000L, artist = "Fresh").copy(source = app.songling.data.Source.IMPORT)
        val recent = play("Two", t0 + 5 * MIN, artist = "Other").copy(source = app.songling.data.Source.IMPORT)
        val st = Studio(listOf(old, live, recent), emptyList(), emptyList(), emptyList(), listOf(pet), emptyList(), Settings(), t0 + 10 * 3_600_000L)
        val ps = st.pets.single()
        assertEquals(4 * MIN, ps.ms)
        assertEquals(4 * MIN, ps.favMs)
        assertEquals(1, ps.found)
    }

    /** An imported listen of a song already written about the same time is the same listen; a song on repeat keeps every play. */
    @Test fun importSkipsListensAlreadyThere() {
        val heard = play("One", t0)
        val dup = play("One", t0 + 40_000).copy(source = app.songling.data.Source.IMPORT, ref = "lf:1")
        val again = play("One", t0 + 4 * MIN).copy(source = app.songling.data.Source.IMPORT, ref = "lf:2")
        val other = play("Two", t0 + 30_000).copy(source = app.songling.data.Source.IMPORT, ref = "lf:3")
        assertEquals(listOf(other, again), app.songling.data.Import.fresh(listOf(dup, again, other), listOf(heard)))
        assertEquals(listOf(dup, again), app.songling.data.Import.fresh(listOf(again, dup), emptyList()))
    }

    /** Each pet has its own room (#26): bringing out another puts this one away; a new pet starts empty; the shared den of before goes to every pet. */
    @Test fun eachPetHasItsOwnRoom() {
        val shared = Settings(pet = 1, bought = setOf("RUG", "wall:ROSE"), wall = "ROSE")
        val split = shared.splitRooms(listOf(1, 2))
        assertEquals(setOf("RUG", "wall:ROSE"), split.rooms["2"]?.bought)
        assertEquals(split, split.splitRooms(listOf(1, 2, 3)))
        val two = split.switchRoom(2).copy(bought = setOf("RUG", "wall:ROSE", "DISCO"))
        assertEquals(2L, two.pet)
        val back = two.switchRoom(1)
        assertEquals(setOf("RUG", "wall:ROSE"), back.bought)
        assertEquals("ROSE", back.wall)
        assertEquals(setOf("RUG", "wall:ROSE", "DISCO"), back.rooms["2"]?.bought)
        val fresh = back.switchRoom(3)
        assertEquals(emptySet<String>(), fresh.bought)
        assertEquals("PLUM", fresh.wall)
        assertEquals(setOf("1", "2"), fresh.rooms.keys)
    }
}
