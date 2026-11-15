package app.songling

import app.songling.data.Settings
import app.songling.domain.Records
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecordsTest {
    /** Each record costs a little more than the last, up to a ceiling; an album heard whole costs half. */
    @Test fun pricesClimbToACeiling() {
        assertEquals(listOf(20, 25, 30), (0..2).map { Records.price(it, false) })
        assertEquals(10, Records.price(0, true))
        assertEquals(Records.MAX, Records.price(1_000, false))
        assertEquals(Records.MAX / 2, Records.price(1_000, true))
    }

    /** The collection keeps its order through the settings file (and so through a backup). */
    @Test fun collectionSurvivesSaving() {
        val s = Settings(records = listOf("radiohead|ok computer", "кино|группа крови"))
        assertEquals(s.records, Settings.of(s.json()).records)
        assertEquals(emptyList<String>(), Settings.of(Settings().json()).records)
    }
}
