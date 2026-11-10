package app.songling.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import app.songling.ui.art.Buf
import app.songling.ui.art.mix
import app.songling.ui.art.rgb
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Album covers, kept in filesDir/covers and read back small. */
object Covers {
    fun dir(ctx: Context) = File(ctx.filesDir, "covers").apply { mkdirs() }
    fun file(ctx: Context, name: String) = File(dir(ctx), name)

    fun nameFor(key: String): String = MessageDigest.getInstance("MD5").digest(key.toByteArray()).joinToString("") { "%02x".format(it) }.take(16) + ".jpg"

    /** Downloads [url] as the cover for album [key]; returns the file name. */
    fun download(ctx: Context, key: String, url: String): String? = runCatching {
        val name = nameFor(key)
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000; c.readTimeout = 15_000
        c.setRequestProperty("User-Agent", Meta.AGENT)
        val bytes = c.inputStream.use { it.readBytes() }
        if (bytes.size < 500) return null
        File(dir(ctx), "$name.tmp").apply { writeBytes(bytes); renameTo(file(ctx, name)) }
        cache.remove(name)
        name
    }.getOrNull()

    /** Keeps a bitmap from a player's media session as the cover for [key]. */
    fun keep(ctx: Context, key: String, bmp: Bitmap): String? = runCatching {
        val name = nameFor(key)
        val f = file(ctx, name)
        if (f.exists()) return name
        val scaled = if (bmp.width > 600) Bitmap.createScaledBitmap(bmp, 600, 600 * bmp.height / bmp.width, true) else bmp
        File(dir(ctx), "$name.tmp").apply { outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 88, it) }; renameTo(f) }
        name
    }.getOrNull()

    private val cache = object : LinkedHashMap<String, Bitmap>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?) = size > 80
    }

    /** The cover, no larger than [side] pixels; null when there's none. */
    fun bitmap(ctx: Context, name: String?, side: Int = 300): Bitmap? {
        if (name.isNullOrBlank()) return null
        val k = "$name@$side"
        synchronized(cache) { cache[k]?.let { return it } }
        val f = file(ctx, name)
        if (!f.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= side) sample *= 2
        val bmp = BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        synchronized(cache) { cache[k] = bmp }
        return bmp
    }

    /** The cover brought down to n×n art pixels, a few colours, for the den. */
    fun pixels(ctx: Context, name: String?, n: Int): Buf? {
        val bmp = bitmap(ctx, name, 120) ?: return null
        val small = Bitmap.createScaledBitmap(bmp, n, n, true)
        val b = Buf(n, n)
        small.getPixels(b.d, 0, n, 0, 0, n, n)
        // Fewer tones: each channel to 6 steps.
        for (i in b.d.indices) {
            val c = b.d[i]
            fun q(sh: Int) = ((Buf.ch(c, sh) / 51f).let { Math.round(it) } * 51).coerceIn(0, 255)
            b.d[i] = rgb(q(16), q(8), q(0))
        }
        return b
    }

    /**
     * Two colours for the interface from a cover: the most vivid common colour, and a second one far
     * from it in hue. Both are lifted so they read on a dark page. Null when there's no cover.
     */
    fun palette(ctx: Context, name: String?): Pair<Int, Int>? {
        val bmp = bitmap(ctx, name, 120) ?: return null
        val s = Bitmap.createScaledBitmap(bmp, 24, 24, true)
        val px = IntArray(24 * 24).also { s.getPixels(it, 0, 24, 0, 0, 24, 24) }
        // Buckets of hue; each scored by how many pixels and how vivid.
        val score = FloatArray(24); val sum = Array(24) { FloatArray(3) }
        val hsv = FloatArray(3)
        for (c in px) {
            android.graphics.Color.colorToHSV(c, hsv)
            if (hsv[1] < 0.18f || hsv[2] < 0.18f) continue
            val b = (hsv[0] / 15f).toInt().coerceIn(0, 23)
            val w = hsv[1] * hsv[2]
            score[b] += w; sum[b][0] += hsv[0] * w; sum[b][1] += hsv[1] * w; sum[b][2] += hsv[2] * w
        }
        val first = score.indices.maxByOrNull { score[it] } ?: return null
        // A grey cover: quiet warm greys.
        if (score[first] <= 0f) return rgb(0x8A7563) to rgb(0x6E8A80)
        fun hue(b: Int) = sum[b][0] / score[b]
        fun sat(b: Int) = sum[b][1] / score[b]
        val h1 = hue(first)
        val second = score.indices.filter { kotlin.math.abs(it - first).let { d -> minOf(d, 24 - d) } >= 4 }.maxByOrNull { score[it] }?.takeIf { score[it] > 0f }
        return app.songling.ui.theme.warm(h1, sat(first)) to
            (if (second != null) app.songling.ui.theme.warm(hue(second), sat(second), second = true) else app.songling.ui.theme.warm((h1 + 150f) % 360f, .45f, second = true))
    }

    /** The cover's main colour, for spines and labels. */
    fun colour(ctx: Context, name: String?): Int? {
        val bmp = bitmap(ctx, name, 60) ?: return null
        val s = Bitmap.createScaledBitmap(bmp, 1, 1, true)
        return mix(s.getPixel(0, 0) or Buf.OPAQUE, rgb(0x806070), 0.1f)
    }
}
