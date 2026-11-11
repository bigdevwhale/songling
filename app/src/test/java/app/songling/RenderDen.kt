package app.songling

import android.graphics.Bitmap
import app.songling.domain.Item
import app.songling.domain.Wall
import app.songling.ui.art.Buf
import app.songling.ui.art.DenArt
import app.songling.ui.art.DenLook
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** The den drawn to PNGs (run with `-Prender`): every wall, empty and with everything in it, and the shop's pictures. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class RenderDen {
    private val out = File(System.getProperty("render.out") ?: "build/art", "den").apply { mkdirs() }

    private fun save(b: Buf, name: String, k: Int = 4) {
        val bmp = Bitmap.createBitmap(b.w, b.h, Bitmap.Config.ARGB_8888).apply { setPixels(b.d, 0, b.w, 0, 0, b.w, b.h) }
        Bitmap.createScaledBitmap(bmp, b.w * k, b.h * k, false).let { s -> File(out, "$name.png").outputStream().use { s.compress(Bitmap.CompressFormat.PNG, 100, it) } }
    }

    @Test fun render() {
        val spines = List(30) { listOf(0xFFE2584A, 0xFFF0A63A, 0xFF4CB4D8, 0xFF6CC46A, 0xFFB06AD8)[it % 5].toInt() }
        for (w in Wall.entries) {
            val b = Buf(DenArt.W, DenArt.H)
            DenArt.draw(b, DenLook(wall = w, items = Item.entries.toSet(), hour = 14, minute = 40, spines = spines, playing = true, label = 0xFF4CB4D8.toInt()), 0)
            save(b, "full_${w.name.lowercase()}")
            DenArt.draw(b, DenLook(wall = w, hour = 22), 0)
            save(b, "empty_${w.name.lowercase()}")
        }
        val b = Buf(DenArt.W, DenArt.H)
        DenArt.draw(b, DenLook(items = setOf(Item.RUG, Item.SHELF, Item.SPEAKERS, Item.TURNTABLE, Item.PLANT, Item.POSTER, Item.GARLAND, Item.CANDLES), hour = 22, spines = spines, playing = true), 3)
        save(b, "evening")
        for (i in Item.entries) save(DenArt.thumb(i, Wall.PLUM), "thumb_${i.name.lowercase()}", 6)
    }
}
