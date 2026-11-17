package app.songling

import app.songling.data.Artist
import app.songling.ui.sheets.lifeSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LifeSpanTest {
    private fun a(type: String, born: Int, ended: Int, gender: String = "") = Artist("k", "n", type = type, born = born, ended = ended, gender = gender)

    @Test fun people() {
        assertEquals("Жив · род. 1985", lifeSpan(a("Person", 1985, -1, "Male"), true))
        assertEquals("Жива · род. 1985", lifeSpan(a("Person", 1985, -1, "Female"), true))
        assertEquals("Умер · 1962–1990", lifeSpan(a("Person", 1962, 1990, "Male"), true))
        assertEquals("Умерла · 1942–2016", lifeSpan(a("Person", 1942, 2016, "Female"), true))
        assertEquals("Умер в 1990", lifeSpan(a("Person", 0, 1990), true))
        assertEquals("Умер", lifeSpan(a("Person", 1962, 1), true))
        assertEquals("Died · 1962–1990", lifeSpan(a("Person", 1962, 1990), false))
    }

    @Test fun bands() {
        assertEquals("Вместе · с 1988", lifeSpan(a("Group", 1988, -1), true))
        assertEquals("Распались · 1988–2014", lifeSpan(a("Group", 1988, 2014), true))
        assertEquals("Broke up in 2014", lifeSpan(a("Group", 0, 2014), false))
    }

    @Test fun unknown() {
        assertNull(lifeSpan(a("", 0, 0), true))
        assertNull(lifeSpan(a("Person", 1985, 0), true))
    }
}
