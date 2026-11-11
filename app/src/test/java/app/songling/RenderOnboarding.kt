package app.songling

import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.songling.ui.onboarding.Onboarding
import app.songling.ui.theme.SonglingTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** The first two onboarding steps, drawn to PNGs (run with `-Prender`). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "ru-w393dp-h851dp-xxhdpi")
class RenderOnboarding {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val out = File(System.getProperty("render.out") ?: "build/art", "onboarding").apply { mkdirs() }

    private fun shot(name: String) {
        rule.mainClock.advanceTimeBy(500)
        val v = rule.activity.window.decorView
        val b = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { v.draw(android.graphics.Canvas(b)) }
        File(out, "$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun render() {
        // The pets dance every frame: the clock is moved by hand.
        rule.mainClock.autoAdvance = false
        rule.setContent { SonglingTheme { Onboarding() } }
        shot("1-hello")
        rule.onNodeWithText("Дальше").performClick()
        shot("2-egg")
    }
}
