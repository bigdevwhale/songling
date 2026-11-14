package app.songling

import android.graphics.Bitmap
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import app.songling.data.DemoSeed
import app.songling.data.Keys
import app.songling.ui.Overlay
import app.songling.ui.SonglingRoot
import app.songling.ui.Tab
import app.songling.ui.art.Art
import app.songling.ui.theme.Mode
import app.songling.ui.theme.SonglingTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * The app's screens over the demo's four months of music, drawn to PNGs (run with `-Prender`): the four tabs, the
 * pages and the sheets, by day and a few by night – for a look at the whole interface without a phone.
 * `-Pscreens=den,album` picks some. Covers come from iTunes the first time, as in the demo build.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "ru-w393dp-h851dp-xxhdpi")
class RenderScreens {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val out = File(System.getProperty("render.out") ?: "build/art", "screens").apply { mkdirs() }

    private class View(val name: String, val tab: Tab = Tab.DEN, val overlay: Overlay? = null, val dark: Boolean = false, val tap: String? = null)

    /** Lets what loads off the main thread land, and the clock run on. */
    private fun settle(ms: Int = 1600) {
        repeat(ms / 100) {
            rule.mainClock.advanceTimeBy(100)
            Thread.sleep(30)
            shadowOf(Looper.getMainLooper()).idle()
        }
    }

    private fun shot(name: String) {
        val v = rule.activity.window.decorView
        val b = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { v.draw(android.graphics.Canvas(b)) }
        File(out, "$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun render() {
        Art.still = true
        rule.mainClock.autoAdvance = false
        val c = rule.activity.container
        runBlocking { DemoSeed.seedIfEmpty(rule.activity, c) }
        val s = runBlocking { c.settings.now() }
        val album = Keys.album("Radiohead", "OK Computer")
        val views = listOf(
            View("01-den"), View("02-shelf", Tab.SHELF), View("03-saga", Tab.SAGA), View("04-awards", Tab.SAGA, tap = "Награды"),
            View("05-calendar", Tab.SAGA, tap = "Календарь"), View("06-goals", Tab.GOALS),
            View("07-album", Tab.SHELF, Overlay.AlbumOf(album)), View("08-artist", Tab.SHELF, Overlay.ArtistOf(Keys.artist("Radiohead"))),
            View("09-pet", overlay = Overlay.PetInfo), View("10-quests", overlay = Overlay.Quests), View("11-bestiary", overlay = Overlay.Bestiary),
            View("12-shop", overlay = Overlay.Shop), View("13-settings", overlay = Overlay.Settings), View("14-add", Tab.SHELF, Overlay.Add()),
            View("15-goal", Tab.GOALS, Overlay.GoalEdit()), View("16-log", overlay = Overlay.Log()),
            View("17-den-dark", dark = true), View("18-album-dark", Tab.SHELF, Overlay.AlbumOf(album), dark = true), View("19-saga-dark", Tab.SAGA, dark = true),
            View("20-settings-dark", overlay = Overlay.Settings, dark = true),
            View("23-settings-count", overlay = Overlay.Settings, tap = "Что считать"), View("24-settings-look", overlay = Overlay.Settings, tap = "Вид и язык"),
            View("25-history", Tab.SAGA, tap = "История"), View("21-album-edit", Tab.SHELF, Overlay.AlbumEdit(album)), View("22-album-edit-dark", Tab.SHELF, Overlay.AlbumEdit(album), dark = true),
        )
        val pick = System.getProperty("render.screens")?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }
        var view by mutableStateOf(views.first())
        rule.setContent {
            Mode.dark = view.dark
            SonglingTheme { key(view) { SonglingRoot(s, clock = System.currentTimeMillis(), tab0 = view.tab, overlay0 = view.overlay) } }
        }
        for (v in views) {
            if (pick != null && pick.none { it in v.name }) continue
            view = v
            settle()
            v.tap?.let { rule.onAllNodesWithText(it, substring = true).onFirst().performClick(); settle(600) }
            shot(v.name)
        }
    }
}
