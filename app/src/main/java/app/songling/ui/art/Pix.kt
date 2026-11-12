package app.songling.ui.art

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * A small ARGB pixel buffer every picture is painted into. Drawing goes by art pixels ([w]×[h]); each art pixel
 * is [s]×[s] fine pixels underneath ([fw]×[fh], held in [d]), so pictures drawn ahead of time (pets, the den)
 * keep their finer detail while what the code draws stays on the art grid. Screens scale it up with
 * nearest-neighbour filtering. Colours are 0xAARRGGBB ints; drawing past the edges is ignored.
 */
class Buf(val w: Int, val h: Int, val s: Int = 1) {
    val fw = w * s
    val fh = h * s
    val d = IntArray(fw * fh)

    fun copyFrom(o: Buf) = System.arraycopy(o.d, 0, d, 0, d.size)

    fun get(x: Int, y: Int): Int = if (x < 0 || y < 0 || x >= w || y >= h) 0 else d[y * s * fw + x * s]
    fun filled(x: Int, y: Int): Boolean = get(x, y) ushr 24 != 0
    fun getF(x: Int, y: Int): Int = if (x < 0 || y < 0 || x >= fw || y >= fh) 0 else d[y * fw + x]

    /** Paints an art pixel, blending by [a] (0..1) over what is there. */
    fun p(x: Int, y: Int, c: Int, a: Float = 1f) {
        if (x < 0 || y < 0 || x >= w || y >= h || a <= 0f) return
        if (s == 1) { pf(x, y, c, a); return }
        for (j in 0 until s) for (i in 0 until s) pf(x * s + i, y * s + j, c, a)
    }

    /** Paints a fine pixel of a picture [from] fine pixels to the art pixel, whatever this buffer's own grain. */
    fun pfs(x: Int, y: Int, from: Int, c: Int, a: Float = 1f) = if (from == s) pf(x, y, c, a) else pf(x * s / from, y * s / from, c, a)

    /** Paints a fine pixel. */
    fun pf(x: Int, y: Int, c: Int, a: Float = 1f) {
        if (x < 0 || y < 0 || x >= fw || y >= fh || a <= 0f) return
        val i = y * fw + x
        if (a >= 1f || d[i] ushr 24 == 0) {
            d[i] = if (a >= 1f) c or OPAQUE else (c and 0xFFFFFF) or ((a * 255).toInt() shl 24)
            return
        }
        val o = d[i]
        val r = ch(o, 16) + ((ch(c, 16) - ch(o, 16)) * a).toInt()
        val g = ch(o, 8) + ((ch(c, 8) - ch(o, 8)) * a).toInt()
        val b = ch(o, 0) + ((ch(c, 0) - ch(o, 0)) * a).toInt()
        d[i] = (max(o ushr 24, (a * 255).toInt()) shl 24) or (r shl 16) or (g shl 8) or b
    }

    fun clearAt(x: Int, y: Int, w: Int = 1, h: Int = 1) {
        for (j in y * s until (y + h) * s) for (i in x * s until (x + w) * s) if (i in 0 until fw && j in 0 until fh) d[j * fw + i] = 0
    }

    fun clear() = d.fill(0)

    /** Draws [o] at art pixel (x, y), skipping its empty pixels; [alpha] fades the whole of it. Keeps as much of [o]'s detail as this one holds. */
    fun blit(o: Buf, x: Int, y: Int, alpha: Float = 1f) {
        if (o.s == 1 && s > 1) {
            for (j in 0 until o.h) for (i in 0 until o.w) {
                val c = o.d[j * o.w + i]
                val a = (c ushr 24) / 255f * alpha
                if (a > 0f) p(x + i, y + j, c, a)
            }
            return
        }
        // Each fine pixel here takes the one of [o] under it.
        val tw = o.w * s; val th = o.h * s
        for (j in 0 until th) for (i in 0 until tw) {
            val c = o.d[(j * o.s / s) * o.fw + i * o.s / s]
            val a = (c ushr 24) / 255f * alpha
            if (a > 0f) pf(x * s + i, y * s + j, c, a)
        }
    }

    /** Draws [o] squeezed or stretched into w×h art pixels, nearest neighbour. */
    fun blitScaled(o: Buf, x: Int, y: Int, w: Int, h: Int) {
        val tw = w * s; val th = h * s
        for (j in 0 until th) for (i in 0 until tw) {
            val c = o.d[(j * o.fh / th) * o.fw + i * o.fw / tw]
            if (c ushr 24 != 0) pf(x * s + i, y * s + j, c)
        }
    }

    companion object {
        const val OPAQUE = 0xFF000000.toInt()
        /** Fine pixels to an art pixel in the pets and the den. */
        const val FINE = 2
        fun ch(c: Int, sh: Int) = (c shr sh) and 0xFF
    }
}

fun rgb(hex: Long): Int = (hex or 0xFF000000).toInt()
fun rgb(r: Int, g: Int, b: Int): Int = Buf.OPAQUE or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)

/** Mixes two colours. */
fun mix(a: Int, b: Int, t: Float): Int {
    fun one(sh: Int) = (Buf.ch(a, sh) + (Buf.ch(b, sh) - Buf.ch(a, sh)) * t).roundToInt().coerceIn(0, 255)
    return Buf.OPAQUE or (one(16) shl 16) or (one(8) shl 8) or one(0)
}

/** Four tones of one colour: light, base, dark, deepest. */
class Ramp(val l: Int, val b: Int, val d: Int, val dd: Int)

private val LIGHT = rgb(0xFFF4D8)
private val DEEP = rgb(0x1C0C10)
fun ramp(c: Int) = Ramp(mix(c, LIGHT, .38f), c, mix(c, DEEP, .36f), mix(c, DEEP, .64f))

private val B4 = intArrayOf(0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5)

/** True where the 4×4 ordered dither lets a pixel through at density [t] (0..1). */
fun dith(x: Int, y: Int, t: Double): Boolean = B4[(y and 3) * 4 + (x and 3)] / 16.0 < t

/** A stable unsigned hash of a string, the same on every phone. */
fun hash(s: String): Long {
    var h = 2166136261L
    for (c in s) { h = h xor c.code.toLong(); h = (h * 16777619L) and 0xFFFFFFFFL }
    return h
}
fun hashI(s: String): Int = (hash(s) and 0x7FFFFFFF).toInt()

fun Buf.rect(x: Int, y: Int, w: Int, h: Int, c: Int) {
    for (j in y until y + h) for (i in x until x + w) p(i, j, c)
}
fun Buf.px(x: Int, y: Int, c: Int) = p(x, y, c)
fun Buf.px(x: Double, y: Double, c: Int) = p(floor(x).toInt(), floor(y).toInt(), c)

/** A filled ellipse, row by row, the way the prototype draws it. */
fun Buf.ell(cx: Double, cy: Double, rx: Int, ry: Int, c: Int, a: Float = 1f) {
    for (y in -ry..ry) {
        val w = (rx * sqrt(max(0.0, 1 - (y * y) / ((ry + .5) * (ry + .5))))).roundToInt()
        val x0 = (cx - w).roundToInt()
        val yy = (cy + y).roundToInt()
        for (i in 0..2 * w) p(x0 + i, yy, c, a)
    }
}
fun Buf.ell(cx: Int, cy: Int, rx: Int, ry: Int, c: Int, a: Float = 1f) = ell(cx.toDouble(), cy.toDouble(), rx, ry, c, a)

fun Buf.line(x0: Int, y0: Int, x1: Int, y1: Int, c: Int) {
    var x = x0; var y = y0
    val dx = abs(x1 - x0); val dy = -abs(y1 - y0)
    val sx = if (x0 < x1) 1 else -1; val sy = if (y0 < y1) 1 else -1
    var e = dx + dy
    while (true) {
        p(x, y, c)
        if (x == x1 && y == y1) break
        val e2 = 2 * e
        if (e2 >= dy) { e += dy; x += sx }
        if (e2 <= dx) { e += dx; y += sy }
    }
}

/** Rings everything drawn with a one-pixel line of [col]. */
fun Buf.outline(col: Int) {
    val src = BooleanArray(fw * fh) { d[it] ushr 24 != 0 }
    for (y in 0 until fh) for (x in 0 until fw) {
        val i = y * fw + x
        if (src[i]) continue
        if ((x > 0 && src[i - 1]) || (x < fw - 1 && src[i + 1]) || (y > 0 && src[i - fw]) || (y < fh - 1 && src[i + fw])) d[i] = col or Buf.OPAQUE
    }
}

/** Recolours a one-colour mask into a lit shape: light on top and left, shade on the bottom and right. */
fun Buf.shade(r: Ramp) {
    val m = BooleanArray(fw * fh) { d[it] ushr 24 != 0 }
    fun f(x: Int, y: Int) = x in 0 until fw && y in 0 until fh && m[y * fw + x]
    for (y in 0 until fh) for (x in 0 until fw) {
        if (!f(x, y)) continue
        val dk = !f(x + 1, y) || !f(x, y + 1) || !f(x + 1, y + 1)
        val lt = !f(x - 1, y) || !f(x, y - 1) || !f(x - 1, y - 1)
        d[y * fw + x] = if (dk) r.d else if (lt) r.l else r.b
    }
}

/** Draws a mask with [fn] on a scratch buffer, lights it with [r] and lays it on this one. */
inline fun Buf.lit(r: Ramp, fn: Buf.(Int) -> Unit) {
    val t = Buf(w, h)
    t.fn(Buf.OPAQUE)
    t.shade(r)
    blit(t, 0, 0)
}

fun Buf.silhouette(col: Int) {
    for (i in d.indices) if (d[i] ushr 24 != 0) d[i] = col or Buf.OPAQUE
}

fun Buf.trim(): Buf = this

fun minOf3(a: Int, b: Int, c: Int) = min(a, min(b, c))
