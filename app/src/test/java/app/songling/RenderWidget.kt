package app.songling

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import app.songling.widget.PlayerArt
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** The player widget, inflated and drawn to PNGs (run with `-Prender`): a record and a tape, with and without a cover. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderWidget {
    private val out = File(System.getProperty("render.out") ?: "build/art", "widget").apply { mkdirs() }

    private fun shot(name: String, art: Bitmap, title: String, artist: String) {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val v = LayoutInflater.from(ctx).inflate(R.layout.widget_player, null)
        v.findViewById<ImageView>(R.id.art).setImageBitmap(art)
        v.findViewById<TextView>(R.id.eyebrow).text = "СЕЙЧАС ИГРАЕТ"
        v.findViewById<TextView>(R.id.title).text = title
        v.findViewById<TextView>(R.id.artist).text = artist
        v.findViewById<ProgressBar>(R.id.progress).progress = 420
        v.findViewById<ImageView>(R.id.toggle).setImageResource(R.drawable.wp_pause)
        val d = ctx.resources.displayMetrics.density
        val w = (340 * d).toInt(); val h = (150 * d).toInt()
        v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
        v.layout(0, 0, w, h)
        val b = Bitmap.createBitmap(w + 60, h + 60, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        c.drawColor(0xFF5C7A8A.toInt())
        c.translate(30f, 30f)
        v.draw(c)
        File(out, "$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun render() {
        val cover = System.getProperty("render.cover")?.let { BitmapFactory.decodeFile(it) }
        shot("record", PlayerArt.record(cover, 0xFFC8743C.toInt()), "Так закалялась сталь", "Гражданская оборона · Всё идёт по плану")
        shot("tape", PlayerArt.tape(cover, 0xFFC8743C.toInt()), "Так закалялась сталь", "Гражданская оборона · Всё идёт по плану")
        shot("record-none", PlayerArt.record(null, 0xFFC8743C.toInt()), "Ничего не играет", "Включи музыку – питомец услышит")
        shot("tape-none", PlayerArt.tape(null, 0xFFC8743C.toInt()), "Ничего не играет", "Включи музыку – питомец услышит")
    }
}
