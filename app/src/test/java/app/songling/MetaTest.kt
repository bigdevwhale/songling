package app.songling

import app.songling.data.Meta
import app.songling.domain.Genre
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MetaTest {
    /** A Russian name is asked of the Russian store first, whatever the phone's language; a Latin one of the US store. */
    @Test fun storeByTheNamesLetters() {
        assertEquals(listOf(true, false), Meta.stores("Король и Шут", "Лесник"))
        assertEquals(listOf(true, false), Meta.stores("Zoloto", "Улицы"))
        assertEquals(listOf(false, true), Meta.stores("Royal Blood", "Figure It Out"))
    }

    /** The genre most tag votes point to; tags that mean nothing to us don't count. */
    @Test fun genreFromTags() {
        val o = JSONObject("""{"tags":[{"count":3,"name":"rock"},{"count":2,"name":"alternative rock"},{"count":2,"name":"stoner rock"},{"count":5,"name":"british"}]}""")
        assertEquals(Genre.ROCK, Meta.tagGenre(o))
        assertNull(Meta.tagGenre(JSONObject("""{"tags":[{"count":4,"name":"british"}]}""")))
        assertNull(Meta.tagGenre(JSONObject("{}")))
    }

    /** Electric Callboy: the fine styles are kept as named, and «-core» counts as metal. */
    @Test fun stylesFromTags() {
        val o = JSONObject("""{"tags":[{"count":4,"name":"electronicore"},{"count":3,"name":"Metalcore"},{"count":2,"name":"german"},{"count":1,"name":"trancecore"},{"count":1,"name":"post-hardcore"}]}""")
        assertEquals(listOf("electronicore", "metalcore", "trancecore"), Meta.styles(o))
        assertEquals(Genre.METAL, Meta.tagGenre(o))
        assertEquals(Genre.PUNK, Genre.of("post-hardcore"))
    }

    /** The Russian store's names and folk's tags were «other» (#64); own genres named so go by them (#63). */
    @Test fun russianStoreNamesAndFolkTags() {
        for (n in listOf("Фольклор", "Народная музыка", "Авторская песня", "medieval", "pagan folk", "Фолк-рок")) assertEquals(n, Genre.FOLK, Genre.of(n))
        for (n in listOf("Музыка мира", "Этно", "Кельтская музыка")) assertEquals(n, Genre.WORLD, Genre.of(n))
        assertEquals(Genre.AMBIENT, Genre.of("Нью-эйдж"))
        assertEquals(Genre.DANCE, Genre.of("Танцевальная музыка"))
        assertEquals(Genre.REGGAE, Genre.of("Регги"))
        for (n in listOf("OST", "Музыка из игр", "Игровая", "Кино", "Аниме")) assertEquals(n, Genre.SOUNDTRACK, Genre.of(n))
        for (n in listOf("Оркестровая", "Симфоническая")) assertEquals(n, Genre.CLASSICAL, Genre.of(n))
        // What was read right before stays so.
        assertEquals(Genre.METAL, Genre.of("folk metal"))
        assertEquals(Genre.ROCK, Genre.of("Русский рок"))
        assertEquals(Genre.POP, Genre.of("Поп"))
        assertEquals(Genre.OTHER, Genre.of("Ghost"))
    }

    /** An artist found under a transliterated name is matched by its aliases. */
    @Test fun matchesAliases() {
        val o = JSONObject("""{"name":"Король и Шут","sort-name":"Korol i Shut","aliases":[{"name":"KiSh"},{"name":"Korol I Shut"}]}""")
        assertTrue(Meta.named(o, "Король и Шут"))
        assertTrue(Meta.named(o, "Korol I Shut"))
        assertTrue(Meta.named(o, "KiSh"))
        assertFalse(Meta.named(o, "Кино"))
    }
}
