package app.songling

import app.songling.domain.Item
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DenItemsTest {
    @Test fun everyThingStandsInOneOfTheTwentyPlaces() {
        assertEquals(20, Item.slots.size)
        assertEquals(60, Item.entries.size)
        for (i in Item.entries) {
            assertTrue(i.name, i.slot in Item.slots)
            for (ru in listOf(true, false)) assertTrue(i.name, i.name(ru).isNotBlank())
        }
        // The frames hold your own pictures and the shelf your albums: nothing stands in for them.
        assertEquals(listOf(Item.POSTER), Item.variants(Item.POSTER))
        assertEquals(listOf(Item.SHELF), Item.variants(Item.SHELF))
    }

    @Test fun aPlaceShowsOneThing() {
        val at = Item.placed(setOf(Item.PLANT, Item.RUG_VINYL, Item.CACTUS))
        assertEquals(Item.CACTUS, at[Item.PLANT])
        assertEquals(Item.RUG_VINYL, at[Item.RUG])
        assertEquals(2, at.size)
    }

    @Test fun puttingAThingInItsPlacePutsAwayWhatStoodThere() {
        val bought = setOf("PLANT", "CACTUS", "RUG", "wall:BRICK")
        assertEquals(setOf("PLANT", "CACTUS"), Item.others(Item.SUNFLOWERS, bought))
        assertEquals(setOf("PLANT"), Item.others(Item.CACTUS, bought))
        assertEquals(emptySet<String>(), Item.others(Item.RUG, bought))
    }
}
