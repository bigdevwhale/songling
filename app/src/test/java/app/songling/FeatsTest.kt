package app.songling

import app.songling.data.MIN
import app.songling.data.Play
import app.songling.data.Settings
import app.songling.domain.Feat
import app.songling.domain.Feats
import app.songling.domain.Studio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class FeatsTest {
    private val t0 = 1_760_000_000_000L
    private fun play(title: String, at: Long, artist: String = "A", album: String = "", ms: Long = 4 * MIN) =
        Play(at = at, ms = ms, title = title, artistName = artist, albumName = album, length = 4 * MIN)
    private fun studio(plays: List<Play>) = Studio(plays, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), Settings(), plays.last().at + 3_600_000L, ZoneOffset.UTC)
    private fun level(st: Studio, f: Feat) = st.feats.first { it.f == f }

    @Test fun collabsAreSongsOfTwoArtistsOrMore() {
        assertTrue(Feats.isCollab("Скриптонит, Andy Panda", "Сука любовь"))
        assertTrue(Feats.isCollab("Drake", "Work (feat. Rihanna)"))
        assertTrue(Feats.isCollab("Travis Scott ft. Drake", "Sicko Mode"))
        assertTrue(Feats.isCollab("Kanye West x Lil Pump", "I Love It"))
        assertFalse(Feats.isCollab("Simon & Garfunkel", "The Boxer"))
        assertFalse(Feats.isCollab("Nick Cave and the Bad Seeds", "Red Right Hand"))
        assertFalse(Feats.isCollab("Exxasens", "Beyond The Sun"))
    }

    @Test fun newYearSongsByTitleOrAlbum() {
        assertTrue(Feats.isFestive("Last Christmas", ""))
        assertTrue(Feats.isFestive("Три белых коня", "Новогодние песни"))
        assertTrue(Feats.isFestive("В лесу родилась ёлочка", ""))
        assertFalse(Feats.isFestive("Paranoid Android", "OK Computer"))
    }

    @Test fun stepsClimbWithTheCount() {
        val ps = (0 until 12).map { play("Song", t0 + it * 5 * MIN) }
        val st = studio(ps)
        val rep = level(st, Feat.REPEAT)
        assertEquals(12, rep.have)
        assertEquals(1, rep.level)
        assertEquals(50, rep.need)
        assertEquals(12, level(st, Feat.STUCK).have)
        assertEquals(st.featSteps, st.feats.sumOf { it.level })
    }

    @Test fun midnightCountsOncePerYear() {
        // 31 Dec 2025, 23:30 UTC and 1 Jan 2026, 00:10 UTC: the same New Year's night.
        val eve = java.time.LocalDateTime.of(2025, 12, 31, 23, 30).toInstant(ZoneOffset.UTC).toEpochMilli()
        val st = studio(listOf(play("Jingle Bells", eve), play("Auld Lang Syne", eve + 40 * MIN)))
        assertEquals(1, level(st, Feat.MIDNIGHT).have)
        assertEquals(2, level(st, Feat.NEW_YEAR).have)
    }
}
