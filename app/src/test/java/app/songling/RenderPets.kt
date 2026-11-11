package app.songling

import android.graphics.Bitmap
import app.songling.domain.Species
import app.songling.domain.Stage
import app.songling.ui.art.Look
import app.songling.ui.art.PetArt
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * The pets as the den draws them (run with `-Prender`, `-Ppets=lyrebird,purr` to pick): for each stage
 * awake, blinking and asleep, 4x, on the den's dark background, to app/build/art/pets/<species>.png.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class RenderPets {
    private val out = File(System.getProperty("render.out") ?: "build/art", "pets").apply { mkdirs() }

    @Test fun render() {
        val pick = System.getProperty("render.pets")?.split(',')?.map { it.trim().lowercase() }?.toSet()
        val stages = Stage.entries.filter { it != Stage.EGG }
        for (sp in Species.entries) {
            if (pick != null && sp.file !in pick) continue
            val k = 4
            val cols = 3
            val img = Bitmap.createBitmap(PetArt.W * k * cols, PetArt.H * k * stages.size, Bitmap.Config.ARGB_8888)
            img.eraseColor(0xFF231C33.toInt())
            for ((row, st) in stages.withIndex()) {
                // Awake and still; blinking (a tick where the eyes are shut); asleep.
                val looks = listOf(Look(sp, st) to 1, Look(sp, st) to blinkTick(sp, st), Look(sp, st, sleeping = true) to 1)
                for ((col, lt) in looks.withIndex()) {
                    val b = PetArt.frame(lt.first, lt.second, motion = false)
                    for (y in 0 until PetArt.H) for (x in 0 until PetArt.W) {
                        val c = b.get(x, y)
                        if (c ushr 24 == 0) continue
                        for (dy in 0 until k) for (dx in 0 until k) img.setPixel((col * PetArt.W + x) * k + dx, (row * PetArt.H + y) * k + dy, c)
                    }
                }
            }
            File(out, "${sp.file}.png").outputStream().use { img.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    /** The first tick at which the pet's eyes are shut by a blink. */
    private fun blinkTick(sp: Species, st: Stage): Int {
        val open = PetArt.frame(Look(sp, st), 1, motion = false).d
        return (2 until 2000).firstOrNull { t -> PetArt.frame(Look(sp, st), t, motion = false).d.let { f -> !f.contentEquals(open) && f.indices.count { f[it] != open[it] } in 1..60 } } ?: 1
    }
}
