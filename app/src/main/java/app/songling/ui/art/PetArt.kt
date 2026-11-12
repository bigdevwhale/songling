package app.songling.ui.art

import android.graphics.BitmapFactory
import app.songling.domain.Form
import app.songling.domain.Mood
import app.songling.domain.Species
import app.songling.domain.Stage
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** How a pet looks right now. */
data class Look(
    val species: Species,
    val stage: Stage,
    val shiny: Boolean = false,
    val form: Form? = null,
    val mood: Mood = Mood.NONE,
    val sleeping: Boolean = false,
    /** 0 awake .. 1 wound down to a stop. */
    val windup: Float = 0f,
    /** Being stroked. */
    val happy: Boolean = false,
    /** Music is playing right now. */
    val dancing: Boolean = false,
    /** Egg: 0 just laid .. 1 about to hatch. */
    val hatch: Float = 0f,
)

/**
 * The pets, painted ahead of time (one 80×80 picture per species and stage, [FINE] times finer: tools/cut.py
 * from the drawn sheets, tools/pets.py for the eggs, with the eyes marked) and brought to life here: blinking, dancing to what plays, sleeping, winding down,
 * a rare colouring and the colours of a grown form.
 */
object PetArt {
    const val SIZE = 80
    /** Fine pixels to an art pixel: the frame a pet is drawn into must be as fine. */
    const val FINE = Buf.FINE
    private const val FS = SIZE * FINE
    /** The frame around the sprite, for notes, hearts and the aura. */
    const val W = 100
    const val H = 96
    private const val OX = 10
    private const val OY = 14

    /**
     * [buf] is [SIZE] art pixels a side, [FINE] times finer; [eyes] are boxes in fine pixels; [lids]: the exact eye
     * pixels of a drawn sheet (tools/cut.py), painted over when the eyes close.
     */
    class Sprite(val buf: Buf, val eyes: List<IntArray>, val lids: BooleanArray? = null) {
        private fun any(x: Int, y: Int) = buf.getF(x, y) ushr 24 != 0
        /** The first fine row with anything in it. */
        val fineTop: Int = (0 until FS).firstOrNull { y -> (0 until FS).any { any(it, y) } } ?: 0
        val top: Int get() = fineTop / FINE
        val left: Int = ((0 until FS).firstOrNull { x -> (0 until FS).any { any(x, it) } } ?: 0) / FINE
        val right: Int = ((FS - 1 downTo 0).firstOrNull { x -> (0 until FS).any { any(x, it) } } ?: (FS - 1)) / FINE
    }

    private val cache = HashMap<String, Sprite?>()

    /** Eye boxes in fine pixels: the drawn sheets' (faces.txt, tools/cut.py, fine) over the painted ones' (eyes.txt, tools/pets.py, art pixels). */
    private val eyeTable: Map<String, List<IntArray>> by lazy {
        fun read(name: String, k: Int) = (PetArt::class.java.getResourceAsStream("/pets/$name")?.bufferedReader()?.use { it.readText() }.orEmpty())
            .lines().filter { it.isNotBlank() && !it.startsWith("#") }.associate { line ->
                val p = line.trim().split(Regex("\\s+"))
                p[0] to p.drop(1).map { it.toInt() * k }.chunked(4).filter { it.size == 4 }.map { it.toIntArray() }
            }
        read("eyes.txt", FINE) + read("faces.txt", 1)
    }

    /** A picture's pixels at [FS]×[FS]: taken as they are, or blown up from an older 80×80 one. */
    private fun fine(bmp: android.graphics.Bitmap): IntArray {
        val n = bmp.width
        val src = IntArray(n * bmp.height).also { bmp.getPixels(it, 0, n, 0, 0, n, bmp.height) }
        if (n == FS && bmp.height == FS) return src
        return IntArray(FS * FS) { i -> val x = (i % FS) * n / FS; val y = (i / FS) * bmp.height / FS; src[y * n + x] }
    }

    fun key(sp: Species, st: Stage) = sp.file + "_" + st.name.lowercase()

    fun sprite(sp: Species, st: Stage): Sprite? = synchronized(cache) {
        cache.getOrPut(key(sp, st)) {
            runCatching {
                val bmp = PetArt::class.java.getResourceAsStream("/pets/${key(sp, st)}.png")?.use { BitmapFactory.decodeStream(it) } ?: return@runCatching null
                val b = Buf(SIZE, SIZE, FINE)
                fine(bmp).copyInto(b.d)
                bmp.recycle()
                val lids = PetArt::class.java.getResourceAsStream("/pets/${key(sp, st)}_eyes.png")?.use { BitmapFactory.decodeStream(it) }?.let { m ->
                    val e = fine(m)
                    m.recycle()
                    BooleanArray(FS * FS) { e[it] ushr 24 != 0 }
                }
                Sprite(b, eyeTable[key(sp, st)].orEmpty(), lids)
            }.getOrNull()
        }
    }

    // ---------------------------------------------------------------- colour

    private fun hsv(c: Int): FloatArray { val f = FloatArray(3); android.graphics.Color.colorToHSV(c, f); return f }
    private fun fromHsv(a: Int, f: FloatArray): Int = (android.graphics.Color.HSVToColor(f) and 0xFFFFFF) or (a shl 24)

    /** The rare colouring: every hue turned, the same way for each species. */
    private fun shine(b: Buf, sp: Species) {
        val turn = 120f + sp.ordinal * 37f % 160f
        for (i in b.d.indices) {
            val c = b.d[i]; val a = c ushr 24
            if (a == 0) continue
            val f = hsv(c)
            if (f[1] < 0.12f) continue
            f[0] = (f[0] + turn) % 360f
            b.d[i] = fromHsv(a, f)
        }
    }

    /** A grown form: the body leans to its colour. */
    private fun tint(b: Buf, form: Form) {
        val t = (form.argb and 0xFFFFFF).toInt() or Buf.OPAQUE
        for (i in b.d.indices) {
            val c = b.d[i]; val a = c ushr 24
            if (a == 0) continue
            val l = (Buf.ch(c, 16) * 0.3f + Buf.ch(c, 8) * 0.59f + Buf.ch(c, 0) * 0.11f) / 255f
            if (l < 0.12f || l > 0.92f) continue
            b.d[i] = (mix(c, t, 0.22f) and 0xFFFFFF) or (a shl 24)
        }
    }

    /** Winding down: colour drains away. */
    private fun drain(b: Buf, k: Float) {
        for (i in b.d.indices) {
            val c = b.d[i]; val a = c ushr 24
            if (a == 0) continue
            val g = (Buf.ch(c, 16) * 0.3f + Buf.ch(c, 8) * 0.59f + Buf.ch(c, 0) * 0.11f).roundToInt()
            val grey = rgb(g * 9 / 10 + 14, g * 9 / 10 + 12, g * 9 / 10 + 22)
            b.d[i] = (mix(c, grey, k * 0.85f) and 0xFFFFFF) or (a shl 24)
        }
    }

    // ---------------------------------------------------------------- frame

    private val OUT = rgb(0x1A0E22)
    private val NOTE = rgb(0xFFF2A0)
    private val PINK = rgb(0xF07AA0)

    /** One frame of the pet at [tick], in a [W]×[H] buffer, its feet on the bottom. */
    /**
     * [motion] false leaves the body still in the buffer (eyes, notes and hearts still animate):
     * the caller then moves the whole picture smoothly (see [Motion]) instead of a pixel at a time.
     */
    fun frame(look: Look, tick: Int, into: Buf = Buf(W, H, FINE), motion: Boolean = true): Buf {
        into.clear()
        if (look.stage == Stage.EGG) { egg(into, look, tick); return into }
        val sp = sprite(look.species, look.stage) ?: return into
        val b = Buf(SIZE, SIZE, FINE).also { it.copyFrom(sp.buf) }
        if (look.form != null && look.stage >= Stage.ADULT) tint(b, look.form)
        if (look.shiny) shine(b, look.species)
        // Eyes: blink now and then, shut when asleep, wound down or stroked.
        val still = look.windup >= 1f
        val shut = look.sleeping || still || look.happy || look.mood == Mood.DREAMY && (tick / 30) % 2 == 0
        val blink = !shut && tick % 48 in 44..45
        if (shut || blink) for (e in sp.eyes) closeEye(b, e, look.happy, sp.lids)
        if (look.windup > 0f) drain(b, look.windup)

        val ph = (tick % 24) / 24f
        val s = sin(ph * 2 * PI).toFloat()
        // How it moves: dancing to the music, swaying, floating, or barely breathing.
        var dy = 0f; var shear = 0f; var squash = 0f
        val slow = 1f - look.windup
        when {
            still -> {}
            look.dancing -> when (look.mood) {
                Mood.HEADBANG -> { dy = -kotlin.math.abs(sin(ph * 4 * PI).toFloat()) * 3f; shear = s * 1.5f }
                Mood.SPARKLE -> { dy = -kotlin.math.abs(s) * 4f; squash = if (kotlin.math.abs(s) < 0.2f) 0.06f else 0f }
                Mood.DREAMY -> { shear = s * 2.5f; dy = -1f }
                else -> { dy = -kotlin.math.abs(s) * 3f; shear = sin(ph * PI).toFloat() * 2f * (if ((tick / 24) % 2 == 0) 1 else -1) }
            }
            look.species.floats -> dy = s * 1.5f - 2f
            else -> squash = (s * 0.5f + 0.5f) * 0.025f * slow
        }
        if (look.mood == Mood.SWAY && !look.dancing && !still) shear = s * 1.2f
        if (!motion) { dy = 0f; shear = 0f; squash = 0f }
        // Lay it down fine row by fine row, so it can lean and squash.
        val top = sp.fineTop
        val h = FS - top
        for (y in 0 until FS) {
            val fromBottom = FS - 1 - y
            val lean = (shear * FINE * (1f - y.toFloat() / FS)).roundToInt()
            val sy = (FS - 1 - fromBottom * (1f + squash)).roundToInt()
            val ty = OY * FINE + sy + (dy * FINE).roundToInt()
            if (y < top - FINE) continue
            for (x in 0 until FS) {
                val c = b.d[y * FS + x]
                if (c ushr 24 == 0) continue
                into.pfs(OX * FINE + x + lean, ty, FINE, c)
                if (squash > 0f && h > 0) for (k in 1 until FINE + 1) into.pfs(OX * FINE + x + lean, ty - k, FINE, c)
            }
        }
        // The key on its back while it winds down.
        if (look.windup > 0.05f) key(into, OX + sp.right - 4, OY + SIZE - 30, tick, still)
        if (look.stage == Stage.ANCIENT && !still) aura(into, tick)
        val eyeY = sp.eyes.firstOrNull()?.let { OY + it[1] / FINE } ?: (OY + sp.top + 12)
        val headX = sp.eyes.takeIf { it.size >= 2 }?.let { OX + (it[0][0] + it[1][2]) / 2 / FINE } ?: (OX + SIZE / 2)
        when {
            look.sleeping && !still -> zzz(into, headX + 10, eyeY - 8, tick)
            look.happy || look.mood == Mood.HEARTS && look.dancing -> heart(into, headX + 12, eyeY - 10 - (tick % 12) / 2)
            look.dancing -> notes(into, headX, eyeY - 6, tick)
        }
        if (look.shiny && !still) sparkle(into, tick)
        return into
    }

    /** Shuts an eye (a box in fine pixels): the lid takes the colour just around it, with a dark line along the middle. */
    private fun closeEye(b: Buf, e: IntArray, happy: Boolean, mask: BooleanArray?) {
        val (x0, y0, x1, y1) = e
        // The lid: the commonest colour just around the eye.
        val around = HashMap<Int, Int>()
        for (y in y0 - 2 * FINE..y1 + FINE - 1) for (x in x0 - 2 * FINE..x1 + FINE - 1) {
            if (x in x0 until x1 && y in y0 until y1) continue
            if (x !in 0 until FS || y !in 0 until FS) continue
            if (mask != null && mask[y * FS + x]) continue
            val c = b.getF(x, y)
            if (c ushr 24 == 0) continue
            val l = Buf.ch(c, 16) * 0.3f + Buf.ch(c, 8) * 0.59f + Buf.ch(c, 0) * 0.11f
            if (l < 40) continue
            around[c] = (around[c] ?: 0) + 1
        }
        val lid = around.maxByOrNull { it.value }?.key ?: b.getF((x0 + x1) / 2, max(0, y0 - 1))
        for (y in y0 until y1) for (x in x0 until x1) {
            if (x !in 0 until FS || y !in 0 until FS) continue
            if (mask != null && !mask[y * FS + x]) continue
            if (b.getF(x, y) ushr 24 != 0) b.d[y * FS + x] = lid
        }
        // The line: as thick as an art pixel, curved up in the middle when happy.
        val mid = (y0 + y1) / 2
        for (x in x0 until x1) {
            val edge = x - x0 < FINE || x1 - 1 - x < FINE
            val yy = if (happy) mid - if (edge) 0 else FINE else mid
            for (k in 0 until FINE) if (b.getF(x, yy + k) ushr 24 != 0 || happy) b.pf(x, yy + k, OUT)
        }
    }

    private fun key(b: Buf, x: Int, y: Int, tick: Int, still: Boolean) {
        val brass = rgb(0xE6C068); val dark = rgb(0x6E4422)
        b.rect(x, y + 3, 4, 2, dark)
        val turn = if (still) 0 else (tick / 6) % 2
        if (turn == 0) { b.rect(x + 4, y, 2, 8, brass); b.rect(x + 6, y + 1, 2, 2, brass); b.rect(x + 6, y + 5, 2, 2, brass) }
        else { b.rect(x + 4, y + 2, 4, 4, brass); b.px(x + 5, y + 3, dark) }
    }

    private fun aura(b: Buf, tick: Int) {
        val c = rgb(0xFFE8A0)
        for (i in 0 until 6) {
            val a = (tick * 3 + i * 60) * PI / 180
            if (sin(a) > 0) continue
            val x = (W / 2 + kotlin.math.cos(a) * 40).roundToInt()
            val y = (H * 0.6 + sin(a) * 12).roundToInt()
            b.p(x, y, c); b.p(x - 1, y, c, .5f); b.p(x + 1, y, c, .5f); b.p(x, y - 1, c, .5f)
        }
    }

    private val NOTE_SHAPES = arrayOf(
        intArrayOf(0b0110, 0b0101, 0b0100, 0b0100, 0b1100, 0b1100),
        intArrayOf(0b1111, 0b1001, 0b1001, 0b1001, 0b1011, 0b1011),
    )

    private fun glyph(b: Buf, rows: IntArray, x: Int, y: Int, c: Int) {
        for ((j, r) in rows.withIndex()) for (i in 0 until 4) if (r shr (3 - i) and 1 == 1) { b.p(x + i, y + j, c); b.p(x + i + 1, y + j + 1, OUT, .5f) }
    }

    private fun notes(b: Buf, x: Int, y: Int, tick: Int) {
        for (k in 0 until 2) {
            val t = (tick + k * 16) % 32
            if (t > 26) continue
            val nx = x + (if (k == 0) -22 else 20) + (sin(t / 4.0) * 2).roundToInt()
            glyph(b, NOTE_SHAPES[k], nx, y - t / 2, NOTE)
        }
    }

    private fun heart(b: Buf, x: Int, y: Int) {
        val rows = intArrayOf(0b0110110, 0b1111111, 0b1111111, 0b0111110, 0b0011100, 0b0001000)
        for ((j, r) in rows.withIndex()) for (i in 0 until 7) if (r shr (6 - i) and 1 == 1) b.p(x + i, y + j, PINK)
        b.p(x + 1, y + 1, rgb(0xFFF0F4))
    }

    private fun zzz(b: Buf, x: Int, y: Int, tick: Int) {
        val c = rgb(0xC8C0E8)
        for (k in 0 until 2) {
            val t = (tick + k * 20) % 40
            val zx = x + k * 5 + t / 8; val zy = y - t / 3
            val a = 1f - t / 40f
            for (i in 0 until 4) { b.p(zx + i, zy, c, a); b.p(zx + i, zy + 3, c, a) }
            b.p(zx + 2, zy + 1, c, a); b.p(zx + 1, zy + 2, c, a)
        }
    }

    private fun sparkle(b: Buf, tick: Int) {
        val c = rgb(0xEAF4FF)
        for (i in 0 until 3) {
            if ((tick / 8 + i * 3) % 7 >= 2) continue
            val x = 18 + (hashI("s$i${tick / 56}") % 64)
            val y = 16 + (hashI("t$i${tick / 56}") % 56)
            b.p(x, y, c); b.p(x - 1, y, c, .6f); b.p(x + 1, y, c, .6f); b.p(x, y - 1, c, .6f); b.p(x, y + 1, c, .6f)
        }
    }

    // ---------------------------------------------------------------- the egg

    private fun egg(b: Buf, look: Look, tick: Int) {
        val sp = sprite(look.species, Stage.EGG)
        val img = sp?.buf ?: return
        // It rocks more the nearer it is to hatching.
        val shake = look.hatch.coerceIn(0f, 1f)
        val phase = (tick % 40)
        val rock = if (phase < 8 && shake > 0.3f) ((if (phase % 4 < 2) 1 else -1) * (1 + (shake * 2).toInt())) else 0
        for (y in 0 until FS) {
            val lean = (rock * FINE * (1f - y.toFloat() / FS)).roundToInt()
            for (x in 0 until FS) { val c = img.d[y * FS + x]; if (c ushr 24 != 0) b.pfs(OX * FINE + x + lean, OY * FINE + y, FINE, c) }
        }
        // Cracks.
        val cracks = (shake * 4).toInt()
        val cx = OX + 40; val top = OY + sp.top + 14
        val col = rgb(0x2A1A20)
        for (k in 0 until cracks) {
            var x = cx - 10 + k * 7; var y = top + k * 9
            repeat(6) { i -> b.p(x + rock, y, col); x += if (i % 2 == 0) 1 else -1; y += 1 }
        }
        if (look.dancing) notes(b, cx, top, tick)
    }

    /** A still picture for lists and the bestiary. */
    fun still(sp: Species, st: Stage, shiny: Boolean = false, form: Form? = null, dim: Boolean = false): Buf {
        val b = frame(Look(sp, st, shiny, form), 0)
        if (dim) for (i in b.d.indices) if (b.d[i] ushr 24 != 0) b.d[i] = rgb(0x2A2236)
        return b
    }

    private operator fun IntArray.component1() = this[0]
    private operator fun IntArray.component2() = this[1]
    private operator fun IntArray.component3() = this[2]
    private operator fun IntArray.component4() = this[3]

    @Suppress("unused") private fun clampI(v: Int, lo: Int, hi: Int) = min(hi, max(lo, v))
}

/** Smooth movement for a pet drawn still: where it is at [t] seconds. Applied as a transform. */
object Motion {
    class Pose(val dy: Float, val rot: Float, val sx: Float, val sy: Float)

    private fun wave(t: Float, hz: Float) = sin(2 * PI * hz * t).toFloat()
    /** A soft bounce: 0 on the floor, 1 at the top, easing in and out. */
    private fun bounce(t: Float, hz: Float): Float { val x = kotlin.math.abs(wave(t, hz / 2)); return x * x * (3 - 2 * x) }

    /** [dy] is a share of the picture's height (negative is up), [rot] degrees. */
    fun at(look: Look, t: Float): Pose {
        if (look.windup >= 1f || look.stage == Stage.EGG) return Pose(0f, if (look.stage == Stage.EGG && look.hatch > 0.3f) wave(t, 1.5f) * 2.5f * look.hatch else 0f, 1f, 1f)
        val slow = 1f - look.windup
        val breath = (wave(t, 0.3f) * 0.5f + 0.5f) * 0.022f * slow
        if (look.dancing) {
            return when (look.mood) {
                Mood.HEADBANG -> { val b = bounce(t, 2.2f); Pose(-b * 0.035f, wave(t, 1.1f) * 3f, 1f + (1 - b) * 0.02f, 1f - (1 - b) * 0.03f) }
                Mood.SPARKLE -> { val b = bounce(t, 1.8f); Pose(-b * 0.05f, wave(t, 0.9f) * 2f, 1f + (1 - b) * 0.03f, 1f - (1 - b) * 0.045f) }
                Mood.DREAMY -> Pose(wave(t, 0.25f) * 0.01f - 0.01f, wave(t, 0.5f) * 5f, 1f, 1f + breath)
                else -> { val b = bounce(t, 1.6f); Pose(-b * 0.04f, wave(t, 0.8f) * 4f, 1f + (1 - b) * 0.02f, 1f - (1 - b) * 0.03f) }
            }
        }
        if (look.species.floats) return Pose(wave(t, 0.35f) * 0.02f - 0.015f, wave(t, 0.2f) * 2f, 1f, 1f)
        val sway = if (look.mood == Mood.SWAY) wave(t, 0.4f) * 2.5f else 0f
        return Pose(0f, sway * slow, 1f - breath * 0.4f, 1f + breath)
    }
}
