package app.songling.ui.art

import app.songling.domain.Item
import app.songling.domain.Wall
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** What the den shows right now. */
data class DenLook(
    val wall: Wall = Wall.PLUM,
    val items: Set<Item> = emptySet(),
    /** Local hour and minute, for the window and the clock. */
    val hour: Int = 20,
    val minute: Int = 0,
    /** Colours (ARGB) of whole albums, for the record shelf. */
    val spines: List<Int> = emptyList(),
    /** The two posters, already small: an album cover (left) and a band (right). Null frames show a print. */
    val posters: List<Buf?> = emptyList(),
    /** The same pictures as files, for showing them as they are when covers aren't pixels. */
    val posterFiles: List<String?> = emptyList(),
    /** The cover of what's playing, for the turntable's label. */
    val label: Int? = null,
    val playing: Boolean = false,
    val pet: Look? = null,
    /** The real weather and the sun, when known; else the clock and a clear sky. */
    val outside: app.songling.data.Outside? = null,
    val now: Long = System.currentTimeMillis(),
    /** Draw the pet into the picture (widgets, shop thumbnails); the den screen lays a moving one on top. */
    val drawPet: Boolean = false,
)

/**
 * The den: a small room at the pet's scale. The window shows the real time of day; everything else
 * is bought with plectrums and drawn here in code.
 */
object DenArt {
    const val W = 200
    const val H = 132
    private const val FLOOR = 96
    private val OUT = rgb(0x1A0E22)
    private val pet = Buf(PetArt.W, PetArt.H)
    /** The pictures in the two frames, in den pixels: x, y, w, h. */
    val POSTER_RECTS = listOf(intArrayOf(14, 18, 30, 30), intArrayOf(134, 16, 20, 20))
    /** Where the pet stands in the den, in den pixels. */
    const val PET_X = W / 2 - PetArt.W / 2
    const val PET_Y = H - PetArt.H - 6

    fun draw(b: Buf, d: DenLook, tick: Int) {
        b.clear()
        room(b, d)
        window(b, d, tick)
        val i = d.items
        if (Item.STARS in i) stars(b, d, tick)
        if (Item.GARLAND in i) garland(b, tick)
        if (Item.POSTER in i) { poster(b, d.posters.getOrNull(0)); band(b, d.posters.getOrNull(1)) }
        if (Item.CLOCK in i) clock(b, d)
        if (Item.NEON in i) neon(b, tick)
        if (Item.HEADPHONES in i) headphones(b)
        if (Item.RUG in i) rug(b)
        if (Item.SHELF in i) shelf(b, d.spines)
        if (Item.CASSETTES in i) cassettes(b)
        if (Item.LAVA in i) lava(b, tick)
        if (Item.SPEAKERS in i) speakers(b, d.playing, tick)
        if (Item.TURNTABLE in i) turntable(b, d, tick)
        if (Item.PLANT in i) plant(b, tick)
        if (Item.GRAMOPHONE in i) gramophone(b)
        if (Item.CANDLES in i) candles(b, tick)
        // The pet, feet on the rug.
        d.pet?.let { look ->
            shadow(b, W / 2, H - 9, if (look.species.floats) 14 else 20)
            // The pet itself is drawn over the den by the screen, so it can move smoothly; renders draw it here.
            if (d.drawPet) { PetArt.frame(look, tick, pet); b.blit(pet, PET_X, PET_Y) }
        }
        if (Item.DISCO in i) disco(b, d.playing, tick)
        // Evening light: the room darkens a little at night unless lamps are on.
        val dark = phase(d) == Phase.NIGHT
        if (dark) for (k in b.d.indices) b.d[k] = mix(b.d[k], rgb(0x0C0818), if (Item.LAVA in i || Item.GARLAND in i || Item.CANDLES in i) 0.12f else 0.25f)
    }

    /** Where each thing stands in the den: x, y, w, h. */
    private fun spot(i: Item): IntArray = when (i) {
        Item.RUG -> intArrayOf(44, 112, 112, 20)
        Item.PLANT -> intArrayOf(166, 80, 32, 40)
        Item.GARLAND -> intArrayOf(40, 0, 56, 14)
        Item.CANDLES -> intArrayOf(74, 50, 52, 16)
        Item.POSTER -> intArrayOf(10, 12, 148, 36)
        Item.CASSETTES -> intArrayOf(6, 38, 24, 24)
        Item.LAVA -> intArrayOf(32, 32, 26, 36)
        Item.STARS -> intArrayOf(0, 2, 64, 16)
        Item.SHELF -> intArrayOf(2, 60, 52, 62)
        Item.SPEAKERS -> intArrayOf(50, 78, 24, 42)
        Item.HEADPHONES -> intArrayOf(178, 22, 22, 24)
        Item.TURNTABLE -> intArrayOf(150, 78, 46, 44)
        Item.NEON -> intArrayOf(156, 14, 36, 34)
        Item.DISCO -> intArrayOf(156, 0, 28, 26)
        Item.GRAMOPHONE -> intArrayOf(18, 82, 34, 38)
        Item.CLOCK -> intArrayOf(19, 55, 23, 23)
    }

    private val thumbs = HashMap<String, Buf>()

    /** One thing alone, cut out of an evening den with that thing in it, for the shop. */
    fun thumb(i: Item, wall: Wall): Buf = synchronized(thumbs) {
        thumbs.getOrPut(i.name + wall.name) {
            val full = Buf(W, H)
            draw(full, DenLook(wall = wall, items = setOf(i), hour = 21, minute = 0, spines = SPINES, playing = true), 3)
            val (x, y, w, h) = spot(i).let { arrayOf(it[0], it[1], it[2], it[3]) }
            Buf(w, h).also { b -> for (j in 0 until h) for (k in 0 until w) b.d[j * w + k] = full.get(x + k, y + j) }
        }
    }

    private val SPINES = listOf(0xFFE2584A, 0xFFF0A63A, 0xFF4CB4D8, 0xFF6CC46A, 0xFFB06AD8, 0xFFF2D84C, 0xFFE86AA8, 0xFF6A7AE0, 0xFF3EA06A).map { it.toInt() }.let { it + it + it }

    private fun shadow(b: Buf, cx: Int, cy: Int, rx: Int) {
        for (y in -2..2) for (x in -rx..rx) {
            val d = (x * x).toFloat() / (rx * rx) + (y * y) / 6.5f
            if (d <= 1f) b.p(cx + x, cy + y, OUT, if (d < .5f) .35f else .2f)
        }
    }

    private fun room(b: Buf, d: DenLook) {
        val a = (d.wall.a and 0xFFFFFF).toInt() or Buf.OPAQUE
        val c = (d.wall.b and 0xFFFFFF).toInt() or Buf.OPAQUE
        for (y in 0 until FLOOR) for (x in 0 until W) {
            val stripe = if (d.wall == Wall.VINYL) ((x / 6) % 2 == 0) else ((x / 12) % 2 == 0)
            var col = if (stripe) a else c
            if (y < 4) col = mix(col, OUT, .35f)
            b.p(x, y, col)
        }
        // Skirting and floorboards.
        b.rect(0, FLOOR - 3, W, 3, mix(a, rgb(0xE8D8C0), .25f))
        b.rect(0, FLOOR, W, 1, OUT)
        val wood = ramp(rgb(0x7A4E34))
        for (y in FLOOR + 1 until H) {
            val row = (y - FLOOR) / 6
            for (x in 0 until W) {
                val seam = (x + row * 37) % 56 == 0 || (y - FLOOR) % 6 == 0
                b.p(x, y, if (seam) wood.d else if ((x + y) % 7 == 0) wood.l else wood.b)
            }
        }
        for (y in FLOOR + 1 until H) for (x in 0 until W) b.p(x, y, rgb(0x120A10), (1f - (y - FLOOR) / (H - FLOOR).toFloat()) * 0.25f)
    }

    /** Where the sun is: by the real sunrise and sunset when known, else by the clock. */
    enum class Phase { NIGHT, DAWN, DAY, DUSK }

    fun phase(d: DenLook): Phase {
        val o = d.outside
        if (o != null && o.sunrise > 0 && o.sunset > o.sunrise) {
            // Today's sunrise and sunset, moved to the day of [now].
            val dayMs = 86_400_000L
            val shift = Math.floorDiv(d.now - o.sunrise + dayMs / 2, dayMs) * dayMs
            val rise = o.sunrise + shift; val set = o.sunset + shift
            val m = 40 * 60_000L
            return when {
                d.now in rise - m until rise + m -> Phase.DAWN
                d.now in set - m until set + m -> Phase.DUSK
                d.now in rise + m until set - m -> Phase.DAY
                else -> Phase.NIGHT
            }
        }
        val h = d.hour + d.minute / 60f
        return when { h < 5.5f || h >= 21f -> Phase.NIGHT; h < 7.5f -> Phase.DAWN; h < 18.5f -> Phase.DAY; else -> Phase.DUSK }
    }

    private fun window(b: Buf, d: DenLook, tick: Int) {
        val x0 = 72; val y0 = 14; val w = 56; val h = 46
        val ph = phase(d)
        val wx = d.outside?.weather ?: app.songling.data.Weather.CLEAR
        val grey = when (wx) { app.songling.data.Weather.OVERCAST, app.songling.data.Weather.RAIN, app.songling.data.Weather.STORM -> 0.6f; app.songling.data.Weather.FOG, app.songling.data.Weather.SNOW -> 0.5f; app.songling.data.Weather.CLOUDY -> 0.25f; else -> 0f }
        var (top, bottom) = when (ph) {
            Phase.NIGHT -> rgb(0x0C1030) to rgb(0x1E2450)
            Phase.DAWN -> rgb(0x3A3070) to rgb(0xF0A070)
            Phase.DAY -> rgb(0x4A8AD8) to rgb(0xA8D8F8)
            Phase.DUSK -> rgb(0x5A4A9A) to rgb(0xF89A60)
        }
        val greyTo = if (ph == Phase.NIGHT) rgb(0x1E2028) else rgb(0x8A9098)
        top = mix(top, greyTo, grey); bottom = mix(bottom, mix(greyTo, rgb(0xFFFFFF), .15f), grey)
        for (y in 0 until h) for (x in 0 until w) b.p(x0 + x, y0 + y, mix(top, bottom, y / h.toFloat()))
        val clearish = grey < 0.4f
        if (ph == Phase.NIGHT) {
            if (clearish) for (k in 0 until 14) {
                val sx = x0 + 2 + hashI("w$k") % (w - 4); val sy = y0 + 2 + hashI("h$k") % (h - 16)
                if ((tick / 10 + k) % 9 != 0) b.p(sx, sy, rgb(0xFFF6D0), if (k % 3 == 0) 1f else .6f)
            }
            if (clearish) { b.ell(x0 + 42, y0 + 11, 5, 5, rgb(0xFFF2C8)); b.ell(x0 + 44, y0 + 10, 4, 4, top) }
        } else {
            val h24 = d.hour + d.minute / 60f
            val sx = x0 + 12 + ((h24 - 6) / 14f * 32).roundToInt().coerceIn(0, 34)
            if (clearish) b.ell(sx, y0 + 12, 5, 5, rgb(0xFFF0A0))
        }
        // Clouds: a few on a clear day, a whole sky of them when it's grey.
        val clouds = when (wx) { app.songling.data.Weather.CLEAR -> 1; app.songling.data.Weather.CLOUDY -> 3; else -> 5 }
        val cloudCol = if (ph == Phase.NIGHT) rgb(0x3A3E50) else if (grey > 0.4f) rgb(0xC8CCD4) else rgb(0xF4F8FF)
        for (k in 0 until clouds) {
            val cx = x0 + ((tick / (5 + k) + k * 23) % (w + 24)) - 12
            val cy = y0 + 8 + (k * 7) % 22
            val rx = 7 + k % 3; val ry = 2 + k % 2
            // Only inside the glass: clouds drift out of sight behind the frame, never into the room.
            for (yy in -ry..ry) for (xx in -rx..rx) {
                if ((xx * xx).toFloat() / (rx * rx) + (yy * yy).toFloat() / (ry * ry) > 1f) continue
                val px = cx + xx; val py = cy + yy
                if (px in x0 until x0 + w && py in y0 until y0 + h) b.p(px, py, cloudCol, .9f)
            }
        }
        when (wx) {
            app.songling.data.Weather.RAIN, app.songling.data.Weather.STORM -> for (k in 0 until 26) {
                val rx = x0 + hashI("r$k") % w; val ry = y0 + (hashI("q$k") + tick * 3) % h
                b.p(rx, ry, rgb(0xB8C8E8), .7f); b.p(rx, ry + 1, rgb(0xB8C8E8), .5f)
            }
            app.songling.data.Weather.SNOW -> for (k in 0 until 22) {
                val fx = x0 + (hashI("s$k") + (kotlin.math.sin((tick + k * 7) / 6.0) * 2).roundToInt()) % w; val fy = y0 + (hashI("t$k") + tick) % h
                b.p(fx, fy, rgb(0xFFFFFF), .9f)
            }
            app.songling.data.Weather.FOG -> for (y in 0 until h) for (x in 0 until w) if ((x + y * 3) % 4 == 0) b.p(x0 + x, y0 + y, rgb(0xD8DCE0), .35f)
            else -> {}
        }
        // A flash now and then in a storm.
        if (wx == app.songling.data.Weather.STORM && tick % 97 < 2) for (y in 0 until h) for (x in 0 until w) b.p(x0 + x, y0 + y, rgb(0xFFFFFF), .5f)
        val night = ph == Phase.NIGHT
        // Rooftops far off.
        for (x in 0 until w) {
            val rh = 6 + (hashI("r${x / 7}") % 9)
            for (y in h - rh until h) b.p(x0 + x, y0 + y, rgb(0x141028))
            if (night && (x % 7 == 3) && hashI("l${x / 7}") % 3 == 0) b.p(x0 + x, y0 + h - rh + 2, rgb(0xFFD878))
        }
        // Frame and sill.
        val fr = ramp(rgb(0xE8DCC8))
        b.rect(x0 - 3, y0 - 3, w + 6, 3, fr.b); b.rect(x0 - 3, y0 + h, w + 6, 3, fr.d)
        b.rect(x0 - 3, y0, 3, h, fr.l); b.rect(x0 + w, y0, 3, h, fr.d)
        b.rect(x0 + w / 2 - 1, y0, 2, h, fr.b); b.rect(x0, y0 + h / 2 - 1, w, 2, fr.b)
        b.rect(x0 - 6, y0 + h + 3, w + 12, 3, fr.l)
    }

    private fun garland(b: Buf, tick: Int) {
        val cols = intArrayOf(rgb(0xFFD25A), rgb(0xFF7A9A), rgb(0x7AE0FF), rgb(0xA8F07A))
        for (x in 0 until W) {
            val y = 6 + (sin(x / 200.0 * PI * 3) * 3).roundToInt()
            b.p(x, y, rgb(0x3A2A1A))
            if (x % 10 == 5) {
                val on = (x / 10 + tick / 5) % 3 != 0
                val c = cols[(x / 10) % cols.size]
                b.p(x, y + 1, c, if (on) 1f else .4f); b.p(x, y + 2, c, if (on) 1f else .4f)
                if (on) { b.p(x - 1, y + 1, c, .3f); b.p(x + 1, y + 1, c, .3f); b.p(x, y + 3, c, .3f) }
            }
        }
    }

    private fun stars(b: Buf, d: DenLook, tick: Int) {
        val dark = d.hour >= 20 || d.hour < 6
        val c = rgb(0xC8FFB0)
        for (k in 0 until 9) {
            val x = 6 + hashI("st$k") % 188; val y = 8 + hashI("sy$k") % 6
            if (x in 66..134) continue
            val a = if (dark) (if ((tick / 7 + k) % 5 == 0) .5f else .95f) else .35f
            b.p(x, y, c, a); b.p(x - 1, y, c, a * .6f); b.p(x + 1, y, c, a * .6f); b.p(x, y - 1, c, a * .6f); b.p(x, y + 1, c, a * .6f)
        }
    }

    /** The right-hand frame: a portrait poster of a band, with a coloured strip for its name. */
    private fun band(b: Buf, art: Buf?) {
        val x0 = 134; val y0 = 16; val w = 20; val h = 26
        b.rect(x0 - 2, y0 - 2, w + 4, h + 4, rgb(0x1E1A22))
        if (art != null) b.blitScaled(art, x0, y0, w, w)
        else for (y in 0 until w) for (x in 0 until w) b.p(x0 + x, y0 + y, mix(rgb(0x3A6A8A), rgb(0x1A1A3A), (y + x * .3f) / w))
        // The strip where the name would be printed.
        b.rect(x0, y0 + w, w, h - w, rgb(0xE2584A))
        for (k in 0 until 4) b.rect(x0 + 2 + k * 4, y0 + w + 2, 3, 1, rgb(0xFFF2E0))
        b.rect(x0 - 2, y0 + h + 2, w + 4, 1, OUT)
        b.p(x0 + w / 2, y0 - 3, rgb(0xC0C0C8))
    }

    private fun poster(b: Buf, art: Buf?) {
        val x0 = 14; val y0 = 18; val s = 30
        b.rect(x0 - 2, y0 - 2, s + 4, s + 4, rgb(0xF4ECDC))
        if (art != null) b.blitScaled(art, x0, y0, s, s)
        else for (y in 0 until s) for (x in 0 until s) b.p(x0 + x, y0 + y, mix(rgb(0xE2584A), rgb(0x6A3A8A), (x + y) / (2f * s)))
        b.rect(x0 - 2, y0 + s + 2, s + 4, 1, OUT)
        b.p(x0 + s / 2, y0 - 3, rgb(0xC0C0C8))
    }

    private fun clock(b: Buf, d: DenLook) {
        val cx = 30; val cy = 66
        b.ell(cx, cy, 8, 8, rgb(0x16141C)); for (r in 3..7 step 2) for (a in 0 until 32) {
            val t = a / 32.0 * 2 * PI; b.p((cx + cos(t) * r).roundToInt(), (cy + sin(t) * r).roundToInt(), rgb(0x2C2A36))
        }
        b.ell(cx, cy, 2, 2, rgb(0xE2584A))
        val hm = ((d.hour % 12) + d.minute / 60.0) / 12 * 2 * PI - PI / 2
        val mm = d.minute / 60.0 * 2 * PI - PI / 2
        b.line(cx, cy, (cx + cos(hm) * 4).roundToInt(), (cy + sin(hm) * 4).roundToInt(), rgb(0xF4ECDC))
        b.line(cx, cy, (cx + cos(mm) * 6).roundToInt(), (cy + sin(mm) * 6).roundToInt(), rgb(0xF4ECDC))
    }

    private fun neon(b: Buf, tick: Int) {
        val x0 = 162; val y0 = 22
        val on = (tick / 3) % 47 != 0
        val c = rgb(0xFF5AC8); val glow = rgb(0xFF9AE0)
        val pts = listOf(8 to 0, 8 to 16, 20 to 13, 20 to -3, 8 to 0)
        if (on) for (y in -6..26) for (x in -6..28) b.p(x0 + x, y0 + y, c, 0.06f)
        for (k in 0 until pts.size - 1) b.line(x0 + pts[k].first, y0 + pts[k].second + 4, x0 + pts[k + 1].first, y0 + pts[k + 1].second + 4, if (on) glow else rgb(0x5A3050))
        b.ell(x0 + 5, y0 + 21, 4, 3, if (on) c else rgb(0x5A3050)); b.ell(x0 + 17, y0 + 18, 4, 3, if (on) c else rgb(0x5A3050))
    }

    private fun headphones(b: Buf) {
        val x0 = 186; val y0 = 30
        b.rect(x0 + 4, y0 - 4, 2, 4, rgb(0x8A8A96))
        val band = rgb(0x2A2836)
        for (a in 0..20) { val t = PI + a / 20.0 * PI; b.p((x0 + 5 + cos(t) * 6).roundToInt(), (y0 + 6 + sin(t) * 6).roundToInt(), band) }
        b.rect(x0 - 3, y0 + 5, 4, 7, rgb(0xE2584A)); b.rect(x0 + 9, y0 + 5, 4, 7, rgb(0xE2584A))
    }

    private fun rug(b: Buf) {
        val cx = W / 2; val cy = H - 9
        for (y in -9..9) for (x in -54..54) {
            val d = (x * x) / (54f * 54f) + (y * y) / 81f
            if (d > 1f) continue
            val ring = (d * 5).toInt()
            b.p(cx + x, cy + y, if (ring % 2 == 0) rgb(0xB0503C) else rgb(0xE8B060))
        }
    }

    private fun shelf(b: Buf, spines: List<Int>) {
        val x0 = 4; val y0 = 62; val w = 48; val h = 58
        val wood = ramp(rgb(0x8A5A3A))
        b.rect(x0, y0, w, h, wood.d)
        b.rect(x0, y0, w, 2, wood.l)
        for (row in 0 until 3) {
            val ry = y0 + 4 + row * 18
            b.rect(x0 + 2, ry, w - 4, 14, rgb(0x241418))
            b.rect(x0, ry + 14, w, 2, wood.b)
            for (k in 0 until 14) {
                val idx = row * 14 + k
                val col = spines.getOrNull(idx) ?: if (idx < 4) rgb(0x3A3040) else continue
                b.rect(x0 + 3 + k * 3, ry + 1 + (k % 3 == 0).compareTo(false), 2, 13 - (k % 3 == 0).compareTo(false), col)
                b.p(x0 + 3 + k * 3, ry + 2, mix(col, rgb(0xFFFFFF), .3f))
            }
        }
    }

    private fun cassettes(b: Buf) {
        val cols = intArrayOf(rgb(0xE2584A), rgb(0x4CB4D8), rgb(0xF2D84C), rgb(0x6CC46A), rgb(0xB06AD8))
        for (k in 0 until 5) {
            val y = 56 - k * 4; val x = 8 + (k % 2)
            b.rect(x, y, 18, 4, cols[k]); b.rect(x + 3, y + 1, 12, 2, rgb(0xF4ECDC)); b.p(x + 6, y + 2, OUT); b.p(x + 11, y + 2, OUT)
        }
    }

    private fun lava(b: Buf, tick: Int) {
        val x0 = 40; val y0 = 38
        b.rect(x0 + 1, y0 + 22, 8, 3, rgb(0x8A8A96))
        for (y in 0 until 22) {
            val half = 2 + y / 7
            for (x in -half..half) b.p(x0 + 5 + x, y0 + y, rgb(0xFF7A3A), .55f)
        }
        for (k in 0 until 3) {
            val t = ((tick + k * 23) % 60) / 60.0
            val yy = y0 + 20 - (sin(t * PI) * 16).roundToInt()
            b.ell(x0 + 5 + (k - 1), yy, 2, 2, rgb(0xFFD040))
        }
        b.rect(x0 + 3, y0 - 2, 4, 2, rgb(0x8A8A96))
        for (y in -8..30) for (x in -10..20) b.p(x0 + x, y0 + y, rgb(0xFF9A50), 0.04f)
    }

    private fun speakers(b: Buf, playing: Boolean, tick: Int) {
        for (x0 in intArrayOf(56, 132)) {
            val y0 = 82
            b.rect(x0, y0, 12, 36, rgb(0x2A2632)); b.rect(x0, y0, 12, 1, rgb(0x4A4656)); b.rect(x0 + 11, y0, 1, 36, rgb(0x16141C))
            val pump = if (playing && (tick / 2) % 2 == 0) 1 else 0
            b.ell(x0 + 6, y0 + 9, 3 + pump, 3 + pump, rgb(0x16141C)); b.p(x0 + 6, y0 + 9, rgb(0x6A6676))
            b.ell(x0 + 6, y0 + 24, 4 + pump, 4 + pump, rgb(0x16141C)); b.ell(x0 + 6, y0 + 24, 1, 1, rgb(0x6A6676))
        }
    }

    private fun turntable(b: Buf, d: DenLook, tick: Int) {
        val x0 = 152; val y0 = 86
        val wood = ramp(rgb(0x9A6A44))
        b.rect(x0, y0 + 6, 42, 6, wood.b); b.rect(x0, y0 + 6, 42, 1, wood.l)
        b.rect(x0 + 2, y0 + 12, 3, 22, wood.d); b.rect(x0 + 37, y0 + 12, 3, 22, wood.d)
        b.rect(x0 + 1, y0, 40, 7, rgb(0x2A2632)); b.rect(x0 + 1, y0, 40, 1, rgb(0x4A4656))
        // The record, seen from the side: a flat disc with a turning glint.
        b.rect(x0 + 5, y0 - 2, 24, 2, rgb(0x16141C))
        val lab = d.label ?: rgb(0xE2584A)
        b.rect(x0 + 14, y0 - 2, 6, 2, lab)
        if (d.playing) { val g = (tick % 8) * 3; b.p(x0 + 5 + g, y0 - 2, rgb(0x8A8A9A)) }
        b.line(x0 + 34, y0 - 4, x0 + 26, y0 - 2, rgb(0xC0C0C8)); b.p(x0 + 34, y0 - 5, rgb(0xC0C0C8))
    }

    private fun plant(b: Buf, tick: Int) {
        val x0 = 180; val y0 = 104
        b.rect(x0, y0, 14, 14, rgb(0xC0643A)); b.rect(x0, y0, 14, 2, rgb(0xE08A5A))
        val leaf = ramp(rgb(0x3E8A4A))
        val sway = if ((tick / 12) % 2 == 0) 0 else 1
        for ((k, a) in listOf(-60, -100, -135, -30, -80).withIndex()) {
            val t = a * PI / 180
            val len = 12 + k * 2
            val ex = (x0 + 7 + cos(t) * len).roundToInt() + sway; val ey = (y0 + sin(t) * len).roundToInt()
            b.line(x0 + 7, y0, ex, ey, leaf.d)
            b.ell(ex, ey, 3, 2, if (k % 2 == 0) leaf.b else leaf.l)
        }
    }

    private fun gramophone(b: Buf) {
        val x0 = 22; val y0 = 106
        val wood = ramp(rgb(0x7A4A2E))
        b.rect(x0, y0, 20, 12, wood.b); b.rect(x0, y0, 20, 1, wood.l); b.rect(x0 + 19, y0, 1, 12, wood.d)
        val brass = ramp(rgb(0xD8A040))
        b.line(x0 + 10, y0, x0 + 18, y0 - 12, brass.d)
        for (r in 0..8) { b.line(x0 + 18 + r / 2, y0 - 12 - r, x0 + 26 - r, y0 - 20 - r / 3, if (r % 3 == 0) brass.l else brass.b) }
    }

    private fun candles(b: Buf, tick: Int) {
        for ((k, x) in intArrayOf(80, 86, 118).withIndex()) {
            val y = 63 - k % 2 * 2
            b.rect(x, y - 6 + k % 2 * 2, 3, 6 - k % 2 * 2, rgb(0xF4ECDC))
            val f = if ((tick / 3 + k) % 3 == 0) 1 else 0
            b.p(x + 1, y - 7 - f + k % 2 * 2, rgb(0xFFD25A)); b.p(x + 1, y - 8 - f + k % 2 * 2, rgb(0xFFF2A0))
            for (yy in -14..2) for (xx in -6..8) b.p(x + xx, y + yy, rgb(0xFFC060), 0.03f)
        }
    }

    private fun disco(b: Buf, playing: Boolean, tick: Int) {
        val cx = 170; val cy = 14
        b.line(cx, 0, cx, cy - 6, rgb(0x8A8A96))
        for (y in -5..5) for (x in -5..5) if (x * x + y * y <= 26) b.p(cx + x, cy + y, if ((x + y + tick / 3) % 3 == 0) rgb(0xF4F8FF) else rgb(0x9AA0B8))
        if (playing) for (k in 0 until 10) {
            val sx = (hashI("d$k") + tick * (2 + k % 3)) % W; val sy = 10 + hashI("e$k") % 80
            b.p(sx, sy, rgb(0xFFFFFF), .7f); b.p(sx + 1, sy, rgb(0xFFFFFF), .4f)
        }
    }
}
