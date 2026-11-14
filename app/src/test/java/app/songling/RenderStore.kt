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
 * The screens the README, the Stillcraft site and Google Play show, over the demo, in English and Russian (run with
 * `-Prender`): 1080x2229, a 1080x2400 phone between its status bar and its gesture strip, into
 * app/build/art/store/<lang>/<n>-<name>.png. `python tools/store_shots.py` then makes docs/raw and the rest of them.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RenderStore {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private class View(val name: String, val tab: Tab = Tab.DEN, val overlay: Overlay? = null, val tap: Pair<String, String>? = null)

    private fun settle(ms: Int = 2400) {
        repeat(ms / 100) {
            rule.mainClock.advanceTimeBy(100)
            Thread.sleep(30)
            shadowOf(Looper.getMainLooper()).idle()
        }
    }

    private fun shot(dir: File, name: String) {
        val v = rule.activity.window.decorView
        val b = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { v.draw(android.graphics.Canvas(b)) }
        File(dir, "$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun render(lang: String) {
        val dir = File(System.getProperty("render.out") ?: "build/art", "store/$lang").apply { mkdirs() }
        Art.still = true
        Mode.dark = false
        rule.mainClock.autoAdvance = false
        val c = rule.activity.container
        // Built with -Pdemo (no «can't hear the music» card) the app seeds itself: wait for it rather than seed twice.
        if (BuildConfig.DEMO) {
            val until = System.currentTimeMillis() + 180_000
            while (c.seeding.value && System.currentTimeMillis() < until) { Thread.sleep(100); shadowOf(Looper.getMainLooper()).idle() }
            check(!c.seeding.value) { "the demo never finished seeding" }
        } else runBlocking { DemoSeed.seedIfEmpty(rule.activity, c) }
        val s = runBlocking { c.settings.now() }
        val views = listOf(
            View("1-den"), View("2-shelf", Tab.SHELF), View("3-album", Tab.SHELF, Overlay.AlbumOf(Keys.album("Radiohead", "OK Computer"))),
            View("4-saga", Tab.SAGA), View("5-goals", Tab.GOALS), View("6-bestiary", overlay = Overlay.Bestiary),
            View("7-pet", overlay = Overlay.PetInfo), View("8-shop", overlay = Overlay.Shop),
            View("9-awards", Tab.SAGA, tap = "Награды" to "Awards"), View("10-cards", Tab.SAGA, Overlay.Cards),
        )
        val pick = System.getProperty("render.screens")?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }
        var view by mutableStateOf(views.first())
        rule.setContent {
            SonglingTheme { key(view) { SonglingRoot(s, clock = System.currentTimeMillis(), tab0 = view.tab, overlay0 = view.overlay) } }
        }
        // The covers come from iTunes the first time: a while on the shelf lets them land.
        view = views[1]; settle(6000)
        for (v in views) {
            if (pick != null && pick.none { it in v.name }) continue
            view = v
            settle()
            v.tap?.let { rule.onAllNodesWithText(if (lang == "ru") it.first else it.second, substring = true).onFirst().performClick(); settle(800) }
            shot(dir, v.name)
        }
    }

    @Test @Config(sdk = [34], qualifiers = "en-w360dp-h743dp-xxhdpi") fun english() = render("en")

    @Test @Config(sdk = [34], qualifiers = "ru-w360dp-h743dp-xxhdpi") fun russian() = render("ru")
}
