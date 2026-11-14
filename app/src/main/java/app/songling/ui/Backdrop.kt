package app.songling.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.songling.ui.art.Buf
import app.songling.ui.art.hashI
import app.songling.ui.art.ell
import app.songling.ui.art.mix
import app.songling.ui.art.rgb
import app.songling.ui.art.toBitmap
import app.songling.ui.components.Flow
import app.songling.ui.components.t
import app.songling.ui.theme.C
import app.songling.ui.theme.R8
import app.songling.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The picture behind every screen: a pixel landscape drawn here, or a picture of one's own from the
 * gallery. A veil of the page colour keeps text easy to read over it.
 */
object Backdrops {
    const val NONE = "none"
    const val OWN = "own"
    private const val OWN_FILE = "backdrop.jpg"
    const val VEIL = 0.72f

    val drawn = listOf("dusk", "stars", "neon", "sunset", "sea", "forest", "rain", "vinyl", "aurora")

    fun name(key: String, ru: Boolean) = when (key) {
        "dusk" -> if (ru) "сумерки" else "dusk"
        "stars" -> if (ru) "звёзды" else "stars"
        "neon" -> if (ru) "неон" else "neon"
        "sunset" -> if (ru) "закат" else "sunset"
        "sea" -> if (ru) "море" else "sea"
        "forest" -> if (ru) "лес" else "forest"
        "rain" -> if (ru) "дождь" else "rain"
        "vinyl" -> if (ru) "винил" else "vinyl"
        "aurora" -> if (ru) "сияние" else "aurora"
        OWN -> if (ru) "своя" else "my own"
        else -> if (ru) "без фона" else "none"
    }

    fun ownFile(ctx: Context) = File(ctx.filesDir, OWN_FILE)

    fun load(ctx: Context, key: String, maxSide: Int): Bitmap? = when (key) {
        NONE -> null
        OWN -> runCatching {
            val f = ownFile(ctx)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.path, bounds)
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
            BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample })
        }.getOrNull()
        else -> paint(key).toBitmap()
    }

    suspend fun importPicked(ctx: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 2000) sample *= 2
            val bmp = ctx.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }!!
            val tmp = File(ctx.filesDir, "$OWN_FILE.tmp")
            tmp.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            tmp.renameTo(ownFile(ctx))
        }.getOrDefault(false)
    }

    private const val W = 90
    private const val H = 190

    /** Paints a landscape in a small buffer; the screen scales it up with hard pixels. */
    fun paint(key: String): Buf {
        val b = Buf(W, H)
        fun sky(top: Int, bottom: Int, to: Int = H) { for (y in 0 until to) for (x in 0 until W) b.p(x, y, mix(top, bottom, y / to.toFloat())) }
        fun stars(n: Int, maxY: Int, salt: String = "") { for (k in 0 until n) { val x = hashI("x$salt$k") % W; val y = hashI("y$salt$k") % maxY; b.p(x, y, rgb(0xFFF6E0), if (k % 4 == 0) 1f else .5f) } }
        fun hills(base: Int, amp: Double, freq: Double, col: Int, salt: Int) {
            for (x in 0 until W) {
                val h = base + (sin(x * freq + salt) * amp + sin(x * freq * 2.3 + salt * 2) * amp / 3).roundToInt()
                for (y in h until H) b.p(x, y, col)
            }
        }
        when (key) {
            "dusk" -> { sky(rgb(0x1A1430), rgb(0x5A3A6A)); stars(30, 90); b.ell(64, 50, 9, 9, rgb(0xFFE8C0)); hills(130, 6.0, .08, rgb(0x2A1E3E), 1); hills(150, 5.0, .12, rgb(0x1C1430), 4) }
            "stars" -> { sky(rgb(0x080A1C), rgb(0x1A1E40)); stars(90, H, "s"); for (k in 0 until 40) { val x = (k * 2.3).roundToInt() % W; b.p(x, 40 + (sin(k * .3) * 30).roundToInt() + k, rgb(0xC8C0F0), .35f) } ; hills(170, 3.0, .1, rgb(0x0C0C18), 2) }
            "neon" -> {
                sky(rgb(0x14082A), rgb(0x3A0C4A))
                for (k in 0 until 9) { val x0 = k * 11 - 3; val h = 60 + hashI("b$k") % 70; for (y in H - h until H) for (x in x0 until x0 + 9) b.p(x, y, rgb(0x160A26)) ; for (w in 0 until 10) { val wx = x0 + 1 + (hashI("w$k$w") % 7); val wy = H - h + 4 + (hashI("v$k$w") % (h - 8)); b.p(wx, wy, if (w % 3 == 0) rgb(0xFF5AC8) else rgb(0x5AE8FF)) } }
                for (x in 0 until W) b.p(x, 178, rgb(0xFF5AC8), .6f)
            }
            "sunset" -> { sky(rgb(0x3A2060), rgb(0xFF9A5A), 130); b.ell(45, 118, 18, 18, rgb(0xFFD878)); for (y in 130 until H) for (x in 0 until W) b.p(x, y, mix(rgb(0x5A3A6A), rgb(0x1E1430), (y - 130) / 60f)); for (y in 132 until H step 4) for (x in 30..60) if ((x + y) % 5 != 0) b.p(x, y, rgb(0xFFC070), .5f) }
            "sea" -> { sky(rgb(0x2A4A8A), rgb(0xA8D0F0), 110); b.ell(70, 40, 8, 8, rgb(0xFFF6D0)); for (y in 110 until H) for (x in 0 until W) { val w = sin(x * .3 + y * .7) > .85; b.p(x, y, if (w) rgb(0xC8E8FF) else mix(rgb(0x2A6AA8), rgb(0x0E2A50), (y - 110) / 80f)) } }
            "forest" -> { sky(rgb(0x1E3A40), rgb(0x8AB8A0), 120); hills(100, 8.0, .07, rgb(0x2E5A48), 3); for (k in 0 until 12) { val x0 = hashI("t$k") % W; val base = 140 + hashI("u$k") % 40; for (y in 0 until 34) { val half = y / 3; for (x in -half..half) b.p(x0 + x, base - 34 + y, rgb(0x1A3A2A)) } }; hills(176, 3.0, .2, rgb(0x14281E), 5) }
            "rain" -> { sky(rgb(0x2A3040), rgb(0x4A5468)); for (k in 0 until 7) { val x0 = k * 13; val h = 40 + hashI("r$k") % 60; for (y in H - h until H) for (x in x0 until x0 + 11) b.p(x, y, rgb(0x1E2230)); for (w in 0 until 6) b.p(x0 + 2 + (w % 3) * 3, H - h + 5 + (w / 3) * 8, rgb(0xFFD878)) }; for (k in 0 until 120) { val x = hashI("d$k") % W; val y = hashI("e$k") % H; b.p(x, y, rgb(0xA8B8D8), .45f); b.p(x, y + 1, rgb(0xA8B8D8), .3f) } }
            "vinyl" -> { for (y in 0 until H) for (x in 0 until W) { val d = Math.hypot(x - 45.0, y - 95.0); b.p(x, y, if ((d * 1.4).toInt() % 3 == 0) rgb(0x24202E) else rgb(0x16141C)) }; b.ell(45, 95, 14, 14, rgb(0xE2584A)); b.ell(45, 95, 1, 1, rgb(0x16141C)) }
            "aurora" -> { sky(rgb(0x060A1A), rgb(0x10203A)); stars(50, 120, "a"); for (k in 0 until 3) for (x in 0 until W) { val c = (60 + k * 18 + sin(x * .09 + k) * 14).roundToInt(); for (y in c - 26 until c) b.p(x, y, if (k == 1) rgb(0x9A6AE0) else rgb(0x4AE8A0), .18f * (1 - abs(y - c + 13) / 13f)) }; hills(165, 5.0, .09, rgb(0x0A1018), 6) }
        }
        return b
    }
}

val LocalBackdrop = staticCompositionLocalOf<Pair<ImageBitmap?, Boolean>> { null to false }

@Composable
fun rememberBackdrop(key: String, stamp: Long): Pair<ImageBitmap?, Boolean> {
    val ctx = LocalContext.current
    val image by produceState<ImageBitmap?>(null, key, stamp) { value = withContext(Dispatchers.IO) { Backdrops.load(ctx, key, 1800)?.asImageBitmap() } }
    return image to (key != Backdrops.OWN)
}

/** The page colour, or the backdrop under a veil of it. */
@Composable
fun BackdropLayer(modifier: Modifier = Modifier) {
    val (image, pixel) = LocalBackdrop.current
    Box(modifier.fillMaxSize().background(C.bg)) {
        // The colour of the music, washing down from the top and up from the bottom.
        Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(
            0f to C.amber.copy(alpha = .09f), .36f to C.amber.copy(alpha = 0f), .8f to C.lilac.copy(alpha = 0f), 1f to C.lilac.copy(alpha = .06f))))
        if (image != null) {
            Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, filterQuality = if (pixel) FilterQuality.None else FilterQuality.Medium)
            Box(Modifier.fillMaxSize().background(C.bg.copy(alpha = Backdrops.VEIL)))
        }
    }
}

@Composable
fun BackdropPicker(current: String, stamp: Long, onPick: (String) -> Unit, onOwn: () -> Unit) {
    val ctx = LocalContext.current
    val hasOwn = remember(stamp) { Backdrops.ownFile(ctx).exists() }
    Flow(gap = 10.dp) {
        BackTile(Backdrops.NONE, current == Backdrops.NONE, stamp) { onPick(Backdrops.NONE) }
        for (k in Backdrops.drawn) BackTile(k, current == k, stamp) { onPick(k) }
        BackTile(if (hasOwn) Backdrops.OWN else Backdrops.NONE, current == Backdrops.OWN, stamp, label = Backdrops.name(Backdrops.OWN, LocalRu.current), plus = !hasOwn) {
            if (hasOwn && current != Backdrops.OWN) onPick(Backdrops.OWN) else onOwn()
        }
    }
}

@Composable
private fun BackTile(key: String, selected: Boolean, stamp: Long, label: String = Backdrops.name(key, LocalRu.current), plus: Boolean = false, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val thumb by produceState<ImageBitmap?>(null, key, stamp) { value = withContext(Dispatchers.IO) { Backdrops.load(ctx, key, 320)?.asImageBitmap() } }
    Column(Modifier.width(70.dp).clip(R8.Sm).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.width(70.dp).height(118.dp).clip(R8.Sm).background(C.surface2).border(if (selected) 3.dp else 1.dp, if (selected) C.amber else C.line, R8.Sm), contentAlignment = Alignment.Center) {
            thumb?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, filterQuality = if (key == Backdrops.OWN) FilterQuality.Medium else FilterQuality.None) }
            if (plus) Text("+", style = T.Title.copy(color = C.amber))
        }
        Text(label, style = T.Small.copy(color = if (selected) C.amber else C.muted), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(bottom = 2.dp))
    }
}
