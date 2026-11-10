package app.songling.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/** The player widget's picture: a record with the cover as its label, or a tape with the cover as its sticker. */
object PlayerArt {
    private const val S = 320

    fun record(cover: Bitmap?, accent: Int): Bitmap {
        val b = Bitmap.createBitmap(S, S, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val m = S / 2f
        val r = S / 2f - 4
        p.color = Color.argb(70, 0, 0, 0); c.drawCircle(m + 3, m + 5, r, p)
        p.color = Color.rgb(24, 20, 20); c.drawCircle(m, m, r, p)
        // Grooves, and a soft shine across them.
        p.style = Paint.Style.STROKE; p.strokeWidth = 1.4f
        var g = r - 8
        var i = 0
        while (g > r * .42f) { p.color = if (i % 3 == 0) Color.rgb(46, 40, 40) else Color.rgb(34, 29, 29); c.drawCircle(m, m, g, p); g -= 4.5f; i++ }
        p.style = Paint.Style.FILL
        p.color = Color.argb(16, 255, 240, 220)
        c.drawArc(RectF(m - r, m - r, m + r, m + r), 210f, 24f, true, p)
        c.drawArc(RectF(m - r, m - r, m + r, m + r), 30f, 24f, true, p)
        // The label: the cover cut round, or the accent colour.
        val lr = r * .42f
        if (cover != null) {
            val k = lr * 2 / minOf(cover.width, cover.height)
            c.save()
            c.clipPath(Path().apply { addCircle(m, m, lr, Path.Direction.CW) })
            c.drawBitmap(cover, Matrix().apply { setScale(k, k); postTranslate(m - cover.width * k / 2, m - cover.height * k / 2) }, Paint(Paint.FILTER_BITMAP_FLAG))
            c.restore()
        } else { p.color = accent; c.drawCircle(m, m, lr, p) }
        p.color = Color.rgb(24, 20, 20); c.drawCircle(m, m, 7f, p)
        p.color = Color.rgb(200, 190, 176); c.drawCircle(m, m, 3.5f, p)
        return b
    }

    fun tape(cover: Bitmap?, accent: Int): Bitmap {
        val w = S; val h = (S * .66f).toInt()
        val b = Bitmap.createBitmap(w, S, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val top = (S - h) / 2f
        val body = RectF(6f, top, w - 6f, top + h)
        p.color = Color.argb(70, 0, 0, 0); c.drawRoundRect(RectF(body.left + 3, body.top + 5, body.right + 3, body.bottom + 5), 18f, 18f, p)
        p.color = Color.rgb(52, 44, 40); c.drawRoundRect(body, 18f, 18f, p)
        for ((x, y) in listOf(body.left + 14 to body.top + 14, body.right - 14 to body.top + 14, body.left + 14 to body.bottom - 14, body.right - 14 to body.bottom - 14)) {
            p.color = Color.rgb(96, 86, 78); c.drawCircle(x, y, 5f, p)
        }
        // The sticker: the cover as a strip, with a cream band for the window.
        val label = RectF(body.left + 22, body.top + 16, body.right - 22, body.bottom - 46)
        p.color = Color.rgb(240, 230, 212); c.drawRoundRect(label, 8f, 8f, p)
        if (cover != null) {
            val strip = RectF(label.left, label.top, label.right, label.top + label.height() * .34f)
            c.save(); c.clipRect(strip)
            val k = strip.width() / cover.width
            c.drawBitmap(cover, Matrix().apply { setScale(k, k); postTranslate(strip.left, strip.centerY() - cover.height * k / 2) }, p)
            c.restore()
        } else { p.color = accent; c.drawRect(label.left, label.top, label.right, label.top + label.height() * .34f, p) }
        val win = RectF(label.left + label.width() * .2f, label.top + label.height() * .46f, label.right - label.width() * .2f, label.bottom - 10)
        p.color = Color.rgb(36, 30, 28); c.drawRoundRect(win, win.height() / 2, win.height() / 2, p)
        val rr = win.height() * .36f
        for (x in listOf(win.left + win.height() * .55f, win.right - win.height() * .55f)) {
            p.color = Color.rgb(230, 222, 208); c.drawCircle(x, win.centerY(), rr, p)
            p.color = Color.rgb(36, 30, 28)
            for (a in 0 until 6) {
                val t = Math.toRadians(a * 60.0)
                c.drawCircle(x + (rr * .55f * Math.cos(t)).toFloat(), win.centerY() + (rr * .55f * Math.sin(t)).toFloat(), rr * .14f, p)
            }
        }
        // The trapezoid at the bottom with its two holes.
        val tr = Path().apply {
            moveTo(body.left + w * .2f, body.bottom); lineTo(body.left + w * .25f, body.bottom - 34)
            lineTo(body.right - w * .25f, body.bottom - 34); lineTo(body.right - w * .2f, body.bottom); close()
        }
        p.color = Color.rgb(70, 60, 54); c.drawPath(tr, p)
        p.color = Color.rgb(30, 25, 23)
        c.drawCircle(w * .38f, body.bottom - 16, 6f, p); c.drawCircle(w * .62f, body.bottom - 16, 6f, p)
        return b
    }
}
