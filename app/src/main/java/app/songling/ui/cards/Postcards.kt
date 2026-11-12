package app.songling.ui.cards

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import app.songling.R
import app.songling.data.Covers
import app.songling.data.MIN
import app.songling.domain.Feat
import app.songling.domain.Feats
import app.songling.domain.Filter
import app.songling.domain.Genre
import app.songling.domain.Item
import app.songling.domain.Span
import app.songling.domain.Stats
import app.songling.domain.Studio
import app.songling.domain.Wall
import app.songling.domain.Words
import app.songling.ui.art.Buf
import app.songling.ui.art.DenArt
import app.songling.ui.art.DenLook
import app.songling.ui.art.PetArt
import app.songling.ui.art.toBitmap
import app.songling.ui.den.Posters
import app.songling.ui.den.look
import app.songling.ui.title
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/** The postcards to share: each one a picture of something the app knows, laid out for a social feed. */
enum class Postcard(val a: Int, val b: Int, val accent: Int) {
    DEN(0xFF2A1730.toInt(), 0xFF5B2A40.toInt(), 0xFFF5B841.toInt()),
    PET(0xFF1D2340.toInt(), 0xFF4A2F63.toInt(), 0xFFF0A0C8.toInt()),
    SUMMARY(0xFF2B1A14.toInt(), 0xFF6B3324.toInt(), 0xFFF5B841.toInt()),
    ARTISTS(0xFF14202B.toInt(), 0xFF27495A.toInt(), 0xFF7FD6E0.toInt()),
    ALBUMS(0xFF1E1A24.toInt(), 0xFF3E2F4F.toInt(), 0xFFF5B841.toInt()),
    SONGS(0xFF2A1420.toInt(), 0xFF5E2440.toInt(), 0xFFFF8FA8.toInt()),
    REPEAT(0xFF241A10.toInt(), 0xFF5A3A16.toInt(), 0xFFFFC857.toInt()),
    GENRES(0xFF1A1F18.toInt(), 0xFF35472C.toInt(), 0xFFB8E07A.toInt()),
    RHYTHM(0xFF161631.toInt(), 0xFF35306A.toInt(), 0xFFB9A4FF.toInt()),
    YEAR(0xFF1C1712.toInt(), 0xFF4A3420.toInt(), 0xFFF5B841.toInt()),
    MAP(0xFF102326.toInt(), 0xFF1F5157.toInt(), 0xFF8FE3C8.toInt()),
    AWARDS(0xFF261A0E.toInt(), 0xFF5C3B12.toInt(), 0xFFFFD36A.toInt()),
    ZOO(0xFF1B2416.toInt(), 0xFF3F5A2C.toInt(), 0xFFF5D06A.toInt());

    fun title(ru: Boolean) = when (this) {
        DEN -> Words.t(ru, "Логово", "The den")
        PET -> Words.t(ru, "Питомец", "Pet")
        SUMMARY -> Words.t(ru, "Итоги", "Summary")
        ARTISTS -> Words.t(ru, "Артисты", "Artists")
        ALBUMS -> Words.t(ru, "Альбомы", "Albums")
        SONGS -> Words.t(ru, "Песни", "Songs")
        REPEAT -> Words.t(ru, "На повторе", "On repeat")
        GENRES -> Words.t(ru, "Жанры", "Genres")
        RHYTHM -> Words.t(ru, "Ритм", "Rhythm")
        YEAR -> Words.t(ru, "Год", "The year")
        MAP -> Words.t(ru, "Карта", "Map")
        AWARDS -> Words.t(ru, "Награды", "Awards")
        ZOO -> Words.t(ru, "Зверинец", "Menagerie")
    }

    /** Whether the period chosen changes this card. */
    val bySpan get() = this in setOf(SUMMARY, ARTISTS, ALBUMS, SONGS, REPEAT, GENRES, RHYTHM, MAP)
}

/** A post (4:5) or a story (9:16). Everything is laid out on a 1080×1350 page; a story gets more sky above and below. */
enum class Shape(val h: Int) { POST(1350), STORY(1920) }

object Postcards {
    const val W = 1080
    private const val PAGE = 1350

    /** The cards there is something to show on. */
    fun available(st: Studio, span: Span): List<Postcard> {
        val s = Stats(st, Filter(span = span))
        return Postcard.entries.filter { c ->
            when (c) {
                Postcard.DEN -> true
                Postcard.PET -> st.pet != null
                Postcard.ZOO -> st.pets.isNotEmpty()
                Postcard.YEAR -> st.musicDays.isNotEmpty()
                Postcard.AWARDS -> st.feats.any { it.level > 0 }
                Postcard.ALBUMS -> s.plays.any { it.album.isNotEmpty() }
                Postcard.MAP -> s.countries.isNotEmpty()
                Postcard.REPEAT -> (s.plays.groupingBy { it.track }.eachCount().values.maxOrNull() ?: 0) >= 3
                else -> s.plays.isNotEmpty()
            }
        }
    }

    fun draw(ctx: Context, st: Studio, card: Postcard, span: Span, shape: Shape, ru: Boolean): Bitmap {
        val bmp = Bitmap.createBitmap(W, shape.h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Pen(ctx, c, card.accent)
        p.background(card, shape.h)
        c.save()
        c.translate(0f, (shape.h - PAGE) / 2f)
        val s = Stats(st, Filter(span = span))
        val period = periodName(span, st, ru)
        when (card) {
            Postcard.DEN -> den(ctx, p, st, ru)
            Postcard.PET -> pet(p, st, ru)
            Postcard.SUMMARY -> summary(p, st, s, period, ru)
            Postcard.ARTISTS -> artists(p, st, s, period, ru)
            Postcard.ALBUMS -> albums(ctx, p, st, s, period, ru)
            Postcard.SONGS -> songs(ctx, p, st, s, period, ru)
            Postcard.REPEAT -> repeat(ctx, p, st, s, period, ru)
            Postcard.GENRES -> genres(p, s, period, ru)
            Postcard.RHYTHM -> rhythm(p, s, period, ru)
            Postcard.YEAR -> year(p, st, ru)
            Postcard.MAP -> map(p, s, period, ru)
            Postcard.AWARDS -> awards(p, st, ru)
            Postcard.ZOO -> zoo(p, st, ru)
        }
        c.restore()
        p.brand(shape.h, ru)
        return bmp
    }

    private fun periodName(span: Span, st: Studio, ru: Boolean): String = when (span) {
        Span.WEEK -> Words.t(ru, "последние 7 дней", "the last 7 days")
        Span.MONTH -> Words.t(ru, "последние 30 дней", "the last 30 days")
        Span.YEAR -> Words.t(ru, "${st.today.year} год", "${st.today.year}")
        Span.LAST_YEAR -> Words.t(ru, "${st.today.year - 1} год", "${st.today.year - 1}")
        Span.ALL -> Words.t(ru, "всё время", "all time")
    }

    // ---------------------------------------------------------------- cards

    private fun den(ctx: Context, p: Pen, st: Studio, ru: Boolean) {
        p.head(Words.t(ru, "моё уютное место", "my cosy place"), Words.t(ru, "Моё логово", "My den"))
        val zoned = Instant.ofEpochMilli(st.now).atZone(st.zone)
        val spines = st.wholeRuns.map { it.first }.distinct().takeLast(42).map { k -> Covers.colour(ctx, st.albums[k]?.cover) ?: st.albums[k]?.genre?.argb?.toInt() ?: 0xFF8A7A6A.toInt() }
        val posters = if (Item.POSTER.name in st.s.bought) runCatching { Posters.pictures(ctx, st) }.getOrDefault(emptyList()) else emptyList()
        val look = DenLook(
            wall = Wall.entries.firstOrNull { it.name == st.s.wall } ?: Wall.PLUM,
            items = Item.entries.filter { it.name in st.s.bought && it.name !in st.s.hidden }.toSet(),
            hour = zoned.hour, minute = zoned.minute, spines = spines, posters = posters, playing = true,
            pet = st.look(true), outside = if (st.s.realWeather) app.songling.data.Outside.of(st.s.outside) else null, now = st.now, drawPet = true,
        )
        val b = Buf(DenArt.W, DenArt.H, Buf.FINE)
        DenArt.draw(b, look, 24)
        val w = 960; val h = w * DenArt.H / DenArt.W
        p.framed(b.toBitmap(), 60, 400, w, h, 40f)
        val pet = st.pet
        if (pet != null) {
            p.text(pet.pet.title(ru), 60f, 1120f, 64f, p.cream, p.display)
            p.text("${Words.species(pet.species, ru)} · ${Words.stage(pet.stage, ru)}", 60f, 1185f, 36f, p.soft, p.semi)
        } else p.text(Words.t(ru, "Здесь скоро кто-то поселится", "Someone will move in soon"), 60f, 1150f, 48f, p.cream, p.display)
        val items = Item.entries.count { it.name in st.s.bought }
        p.text(Words.t(ru, "${Words.hours(st.totalMs, true)} музыки · вещей: $items", "${Words.hours(st.totalMs, false)} of music · $items things"), 60f, 1240f, 34f, p.accent, p.bold)
    }

    private fun pet(p: Pen, st: Studio, ru: Boolean) {
        val pet = st.pet ?: return
        p.head(Words.t(ru, "мой музыкальный питомец", "my music pet"), pet.pet.title(ru))
        p.glow(540f, 720f, 380f)
        val buf = PetArt.still(pet.species, pet.stage, pet.pet.shiny, pet.form)
        val k = 8
        p.pixels(buf.toBitmap(), 540 - PetArt.W * k / 2, 690 - PetArt.H * k / 2 + 10, PetArt.W * k, PetArt.H * k)
        val chips = listOfNotNull(Words.species(pet.species, ru), Words.stage(pet.stage, ru), Words.nature(pet.pet.nature, ru), if (pet.pet.shiny) Words.t(ru, "блестящий", "shiny") else null)
        p.chips(chips, 1110f)
        val loves = Words.loves(pet.species, ru)
        p.text(Words.t(ru, "Съел ${Words.num(pet.fed)} минут музыки · любит $loves", "Ate ${Words.num(pet.fed)} minutes of music · loves $loves"), 540f, 1230f, 32f, p.soft, p.semi, Paint.Align.CENTER, 960f)
    }

    private fun summary(p: Pen, st: Studio, s: Stats, period: String, ru: Boolean) {
        p.head(period, Words.t(ru, "Моя музыка", "My music"))
        val hours = s.ms / 3_600_000L
        val big = if (hours >= 1) "${Words.num(hours.toInt())}" else "${s.minutes}"
        val unit = if (hours >= 1) Words.t(ru, Words.plural(hours.toInt(), "час", "часа", "часов"), if (hours == 1L) "hour" else "hours") else Words.t(ru, "мин", "min")
        p.text(big, 60f, 620f, 230f, p.accent, p.display)
        val bw = p.width(big, 230f, p.display)
        p.text(unit, 60f + bw + 24f, 620f, 64f, p.cream, p.display)
        p.text(Words.t(ru, "музыки · ${s.days} ${Words.plural(s.days, "день", "дня", "дней")} с музыкой · в среднем ${Words.dur(s.perDay * MIN, true)} в день",
            "of music · ${s.days} days with music · ${Words.dur(s.perDay * MIN, false)} a day"), 60f, 690f, 34f, p.soft, p.semi, max = 960f)
        val tiles = listOf(
            Words.num(s.plays.size) to Words.t(ru, "прослушиваний", "listens"),
            Words.num(s.newArtists) to Words.t(ru, "новых артистов", "new artists"),
            Words.num(s.whole) to Words.t(ru, "альбомов целиком", "whole albums"),
            Words.num(st.bestStreak) to Words.t(ru, "дней подряд – рекорд", "days in a row – best"),
        )
        tiles.forEachIndexed { i, (v, l) ->
            val x = 60f + (i % 2) * 490f; val y = 760f + (i / 2) * 190f
            p.panel(x, y, 470f, 170f)
            p.text(v, x + 32f, y + 88f, 64f, p.cream, p.display)
            p.text(l, x + 32f, y + 136f, 30f, p.soft, p.semi, max = 410f)
        }
        val top = s.artists.firstOrNull()
        val genre = s.genres.firstOrNull { it.first != Genre.OTHER }
        var y = 1180f
        top?.let { p.text(Words.t(ru, "Главный артист: ", "Top artist: ") + it.name, 60f, y, 38f, p.cream, p.bold, max = 960f); y += 56f }
        genre?.let { p.text(Words.t(ru, "Главный жанр: ", "Top genre: ") + Words.genre(it.first, ru).lowercase(), 60f, y, 38f, p.cream, p.bold, max = 960f) }
    }

    private fun artists(p: Pen, st: Studio, s: Stats, period: String, ru: Boolean) {
        p.head(period, Words.t(ru, "Мои артисты", "My artists"))
        val rows = s.artists.take(5)
        val top = rows.firstOrNull()?.ms?.coerceAtLeast(1) ?: 1
        rows.forEachIndexed { i, r ->
            val y = 430f + i * 165f
            p.text("${i + 1}", 60f, y + 70f, 96f, if (i == 0) p.accent else p.faint, p.display)
            p.text(r.name, 170f, y + 40f, 50f, p.cream, p.display, max = 850f)
            p.text("${Words.dur(r.ms, ru)} · ${Words.plays(r.plays, ru)}", 170f, y + 88f, 30f, p.soft, p.semi, max = 850f)
            p.bar(170f, y + 108f, 850f, 14f, r.ms / top.toFloat())
        }
        val total = st.byArtist.size
        p.text(Words.t(ru, "Всего артистов за всё время: ${Words.num(total)}", "Artists of all time: ${Words.num(total)}"), 60f, 1260f, 32f, p.soft, p.semi)
    }

    private fun albums(ctx: Context, p: Pen, st: Studio, s: Stats, period: String, ru: Boolean) {
        p.head(period, Words.t(ru, "Мои альбомы", "My albums"))
        val rows = s.top({ it.album }, st::albumTitle, { k -> st.albums[k]?.artistName ?: st.byAlbum[k]?.artistName.orEmpty() }, n = 9)
        val n = if (rows.size >= 9) 3 else if (rows.size >= 4) 2 else 1
        val gap = 24
        val side = (780 - gap * (n - 1)) / n
        val x0 = (W - (side * n + gap * (n - 1))) / 2
        rows.take(n * n).forEachIndexed { i, r ->
            val x = x0 + (i % n) * (side + gap); val y = 400 + (i / n) * (side + gap)
            p.cover(ctx, st.albums[r.key]?.cover, r.name, st.albums[r.key]?.genre, x, y, side)
            p.badge("${i + 1}", x + 18f, y + 18f, if (i == 0) p.accent else 0xCC1A1014.toInt())
        }
        rows.firstOrNull()?.let {
            p.text("№1 – ${it.name}", 90f, 1240f, 40f, p.cream, p.display, max = 900f)
            p.text("${it.sub} · ${Words.dur(it.ms, ru)}", 90f, 1290f, 30f, p.soft, p.semi, max = 900f)
        }
    }

    private fun songs(ctx: Context, p: Pen, st: Studio, s: Stats, period: String, ru: Boolean) {
        p.head(period, Words.t(ru, "Мои песни", "My songs"))
        s.top({ it.track }, st::trackTitle, { k -> st.byTrack[k]?.artistName.orEmpty() }, n = 50).sortedWith(compareByDescending<app.songling.domain.Row> { it.plays }.thenByDescending { it.ms }).take(6).forEachIndexed { i, r ->
            val y = 410 + i * 140
            val al = st.albums[st.tracks[r.key]?.album ?: ""] ?: st.albums[st.plays.lastOrNull { it.track == r.key }?.album ?: ""]
            p.cover(ctx, al?.cover, r.name, al?.genre, 60, y, 116)
            p.text(r.name, 210f, y + 50f, 42f, p.cream, p.display, max = 640f)
            p.text(r.sub, 210f, y + 96f, 30f, p.soft, p.semi, max = 640f)
            p.text(Words.times(r.plays, ru), 1020f, y + 72f, 34f, if (i == 0) p.accent else p.soft, p.bold, Paint.Align.RIGHT)
        }
    }

    private fun repeat(ctx: Context, p: Pen, st: Studio, s: Stats, period: String, ru: Boolean) {
        val best = s.plays.groupingBy { it.track }.eachCount().maxByOrNull { it.value } ?: return
        p.head(period, Words.t(ru, "Песня на повторе", "On repeat"))
        val last = s.plays.last { it.track == best.key }
        val al = st.albums[last.album]
        p.glow(540f, 700f, 420f)
        p.cover(ctx, al?.cover, last.title, al?.genre, 540 - 290, 420, 580)
        p.text(last.title, 540f, 1110f, 58f, p.cream, p.display, Paint.Align.CENTER, 960f)
        p.text(last.artistName, 540f, 1170f, 36f, p.soft, p.semi, Paint.Align.CENTER, 960f)
        p.text(Words.t(ru, "${Words.times(best.value, true)} – и всё ещё не надоела", "${Words.times(best.value, false)} – and still not tired of it"), 540f, 1250f, 40f, p.accent, p.hand, Paint.Align.CENTER, 960f)
    }

    private fun genres(p: Pen, s: Stats, period: String, ru: Boolean) {
        p.head(period, Words.t(ru, "Мои жанры", "My genres"))
        val g = s.genres.take(8)
        val total = g.sumOf { it.second }.coerceAtLeast(1).toFloat()
        val cx = 540f; val cy = 690f; val r = 230f
        var a = -90f
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 96f }
        for ((genre, ms) in g) {
            val sweep = ms / total * 360f
            ring.color = genre.argb.toInt()
            p.c.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), a, (sweep - 1.5f).coerceAtLeast(.5f), false, ring)
            a += sweep
        }
        g.firstOrNull()?.let { (top, ms) ->
            p.text("${(ms * 100 / total).toInt()}%", cx, cy + 10f, 84f, p.cream, p.display, Paint.Align.CENTER)
            p.text(Words.genre(top, ru).lowercase(), cx, cy + 62f, 34f, p.soft, p.semi, Paint.Align.CENTER, 300f)
        }
        g.take(6).forEachIndexed { i, (genre, ms) ->
            val x = 90f + (i % 2) * 470f; val y = 1030f + (i / 2) * 76f
            p.dot(x + 14f, y - 12f, 14f, genre.argb.toInt())
            p.text(Words.genre(genre, ru), x + 44f, y, 36f, p.cream, p.bold, max = 300f)
            p.text("${(ms * 100 / total).toInt()}%", x + 420f, y, 34f, p.soft, p.semi, Paint.Align.RIGHT)
        }
    }

    private fun rhythm(p: Pen, s: Stats, period: String, ru: Boolean) {
        val hours = s.hours
        val peak = s.peakHour ?: 20
        val (who, what) = when (peak) {
            in 5..10 -> Words.t(ru, "Жаворонок", "Early bird") to Words.t(ru, "Музыка – с первым кофе", "Music comes with the first coffee")
            in 11..16 -> Words.t(ru, "Дневной слушатель", "Daytime listener") to Words.t(ru, "Музыка – чтобы день шёл веселее", "Music to keep the day going")
            in 17..21 -> Words.t(ru, "Вечерний мечтатель", "Evening dreamer") to Words.t(ru, "Лучшая музыка – под вечер", "The best music comes in the evening")
            else -> Words.t(ru, "Ночная сова", "Night owl") to Words.t(ru, "Самое громкое – когда все спят", "Loudest when everyone's asleep")
        }
        p.head(period, who)
        p.text(what, 60f, 400f, 40f, p.soft, p.hand)
        p.text(Words.t(ru, "по часам", "by hour").uppercase(), 60f, 500f, 28f, p.accent, p.bold, spacing = .12f)
        p.bars(hours.toList(), 60f, 530f, 960f, 300f, peak)
        for (h in listOf(0, 6, 12, 18, 23)) p.text("$h", 60f + (h + .5f) * 40f, 870f, 26f, p.faint, p.semi, Paint.Align.CENTER)
        val wd = s.weekdays
        val fav = s.favouriteWeekday
        p.text(Words.t(ru, "по дням недели", "by weekday").uppercase(), 60f, 960f, 28f, p.accent, p.bold, spacing = .12f)
        p.bars(wd.toList(), 60f, 990f, 960f, 190f, fav?.let { it.value - 1 } ?: -1)
        val loc = if (ru) Locale("ru") else Locale.ENGLISH
        for (i in 0 until 7) p.text(DayOfWeek.of(i + 1).getDisplayName(TextStyle.SHORT, loc), 60f + (i + .5f) * 960f / 7, 1222f, 28f, if (fav?.value == i + 1) p.accent else p.faint, p.semi, Paint.Align.CENTER)
        p.text(Words.t(ru, "Пик – около $peak:00", "Peak around $peak:00"), 60f, 1290f, 34f, p.cream, p.bold)
    }

    private fun year(p: Pen, st: Studio, ru: Boolean) {
        val y = st.today.year
        p.head(Words.t(ru, "каждый день – квадратик", "a square for every day"), Words.t(ru, "Мой $y", "My $y"))
        val first = LocalDate.of(y, 1, 1)
        val max = (0 until first.lengthOfYear()).maxOfOrNull { st.byDay[first.plusDays(it.toLong())] ?: 0L }?.coerceAtLeast(1) ?: 1
        val loc = if (ru) Locale("ru") else Locale.ENGLISH
        val cell = 26f; val gap = 4f; val x0 = 130f; val y0 = 420f; val row = 52f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (m in 1..12) {
            val start = LocalDate.of(y, m, 1)
            val yy = y0 + (m - 1) * row
            p.text(start.month.getDisplayName(TextStyle.SHORT_STANDALONE, loc).trimEnd('.'), 60f, yy + 22f, 26f, p.faint, p.semi)
            for (d in 1..start.lengthOfMonth()) {
                val day = start.withDayOfMonth(d)
                val v = st.byDay[day] ?: 0L
                val x = x0 + (d - 1) * (cell + gap)
                paint.color = if (day.isAfter(st.today)) 0x14FFFFFF else if (v == 0L) 0x22FFFFFF else blend(0x55F5B841, p.accent, (v.toFloat() / max).coerceIn(0f, 1f))
                p.c.drawRoundRect(RectF(x, yy, x + cell, yy + cell), 7f, 7f, paint)
            }
        }
        val days = (0 until first.lengthOfYear()).count { first.plusDays(it.toLong()) in st.musicDays }
        val total = (0 until first.lengthOfYear()).sumOf { st.byDay[first.plusDays(it.toLong())] ?: 0L }
        p.text(Words.t(ru, "$days ${Words.plural(days, "день", "дня", "дней")} с музыкой · ${Words.hours(total, true)}", "$days days with music · ${Words.hours(total, false)}"), 60f, 1100f, 40f, p.cream, p.display, max = 960f)
        p.text(Words.t(ru, "Лучшая серия – ${st.bestStreak} ${Words.plural(st.bestStreak, "день", "дня", "дней")} подряд", "Best streak – ${st.bestStreak} days in a row"), 60f, 1160f, 34f, p.soft, p.semi)
        st.byDay.filterKeys { it.year == y }.maxByOrNull { it.value }?.let { (d, ms) ->
            p.text(Words.t(ru, "Самый музыкальный день – ${d.dayOfMonth} ${d.month.getDisplayName(TextStyle.FULL, loc)}, ${Words.dur(ms, true)}",
                "Busiest day – ${d.month.getDisplayName(TextStyle.FULL, loc)} ${d.dayOfMonth}, ${Words.dur(ms, false)}"), 60f, 1215f, 34f, p.soft, p.semi, max = 960f)
        }
    }

    private fun map(p: Pen, s: Stats, period: String, ru: Boolean) {
        p.head(period, Words.t(ru, "Музыка без границ", "Music without borders"))
        val list = s.countries.take(7)
        val top = list.firstOrNull()?.second?.coerceAtLeast(1) ?: 1
        list.forEachIndexed { i, (code, ms) ->
            val y = 430f + i * 105f
            p.text(Words.flag(code), 60f, y + 50f, 58f, p.cream, p.reg)
            p.text(Words.country(code, ru), 150f, y + 30f, 38f, p.cream, p.display, max = 600f)
            p.text(Words.dur(ms, ru), 1020f, y + 30f, 30f, p.soft, p.semi, Paint.Align.RIGHT)
            p.bar(150f, y + 52f, 870f, 12f, ms / top.toFloat())
        }
        val decades = s.decades
        if (decades.isNotEmpty()) {
            p.text(Words.t(ru, "десятилетия", "decades").uppercase(), 60f, 1190f, 28f, p.accent, p.bold, spacing = .12f)
            p.chips(decades.sortedByDescending { it.second }.take(6).map { "’${(it.first % 100).toString().padStart(2, '0')}" }, 1250f, left = true)
        }
    }

    private fun awards(p: Pen, st: Studio, ru: Boolean) {
        val got = st.feats.filter { it.level > 0 }
        val steps = st.feats.sumOf { it.level }; val most = st.feats.sumOf { it.max }
        p.head(Words.t(ru, "${got.size} из ${st.feats.size} наград", "${got.size} of ${st.feats.size} awards"), Words.t(ru, "Мои награды", "My awards"))
        val shown = got.sortedWith(compareByDescending<app.songling.domain.FeatState> { it.level.toFloat() / it.max }.thenByDescending { it.level }).take(12)
        shown.forEachIndexed { i, fs ->
            val x = 60f + (i % 4) * 245f; val y = 410f + (i / 4) * 255f
            val cx = x + 112f; val cy = y + 90f
            p.dot(cx, cy, 86f, medal(fs.level, fs.max))
            featBitmap(fs.f)?.let { p.smooth(it, (cx - 64).toInt(), (cy - 64).toInt(), 128, 128) }
            p.wrap(Feats.name(fs.f, ru), x, y + 186f, 225, 26f, p.cream, p.bold, 2, center = true)
        }
        p.text(Words.t(ru, "$steps из $most ступеней", "$steps of $most steps"), 60f, 1255f, 40f, p.accent, p.display)
    }

    private fun zoo(p: Pen, st: Studio, ru: Boolean) {
        val pets = st.pets.sortedByDescending { it.fed }.take(9)
        p.head(Words.t(ru, "${pets.size} ${Words.plural(pets.size, "питомец", "питомца", "питомцев")}", "${pets.size} pets"), Words.t(ru, "Мой зверинец", "My menagerie"))
        val n = if (pets.size > 4) 3 else if (pets.size > 1) 2 else 1
        val rows = (pets.size + n - 1) / n
        val cell = 960 / n
        // Rows fit between the title and the brand at the bottom.
        val rowH = min(cell + 50, 880 / rows)
        val pic = rowH - 64
        pets.forEachIndexed { i, ps ->
            val x = 60 + (i % n) * cell; val y = 390 + (i / n) * rowH
            p.panel(x + 8f, y + 6f, cell - 16f, rowH - 12f)
            val k = pic / PetArt.H.toFloat()
            val buf = PetArt.still(ps.species, ps.stage, ps.pet.shiny, ps.form)
            val w = (PetArt.W * k).toInt()
            p.pixels(buf.toBitmap(), x + (cell - w) / 2, y + 8, w, pic)
            p.text(ps.pet.title(ru), x + cell / 2f, y + rowH - 24f, if (n == 3) 30f else 40f, p.cream, p.display, Paint.Align.CENTER, cell - 30f)
        }
    }

    // ---------------------------------------------------------------- helpers

    private fun medal(level: Int, max: Int): Int = when {
        level == max -> 0xFFE8B931.toInt()
        level * 3 >= max * 2 -> 0xFFB9C3CC.toInt()
        else -> 0xFFCD8B5A.toInt()
    }

    private val featIcons = HashMap<Feat, Bitmap?>()
    private fun featBitmap(f: Feat): Bitmap? = synchronized(featIcons) {
        featIcons.getOrPut(f) { runCatching { Postcards::class.java.getResourceAsStream("/feats/${f.name.lowercase()}.png")?.use { BitmapFactory.decodeStream(it) } }.getOrNull() }
    }

    fun blend(a: Int, b: Int, t: Float): Int {
        fun ch(c: Int, s: Int) = (c shr s) and 0xFF
        fun one(s: Int) = (ch(a, s) + (ch(b, s) - ch(a, s)) * t).toInt().coerceIn(0, 255)
        return (one(24) shl 24) or (one(16) shl 16) or (one(8) shl 8) or one(0)
    }

    /** What draws on a card: the fonts, the colours and the shapes they share. */
    class Pen(ctx: Context, val c: Canvas, val accent: Int) {
        val display: Typeface = ResourcesCompat.getFont(ctx, R.font.comfortaa) ?: Typeface.DEFAULT_BOLD
        val bold: Typeface = ResourcesCompat.getFont(ctx, R.font.nunito_bold) ?: Typeface.DEFAULT_BOLD
        val semi: Typeface = ResourcesCompat.getFont(ctx, R.font.nunito_semibold) ?: Typeface.DEFAULT
        val reg: Typeface = ResourcesCompat.getFont(ctx, R.font.nunito_regular) ?: Typeface.DEFAULT
        val hand: Typeface = ResourcesCompat.getFont(ctx, R.font.caveat) ?: Typeface.DEFAULT
        val cream = 0xFFFFF3E0.toInt()
        val soft = 0xCCF3E3D0.toInt()
        val faint = 0x80F3E3D0.toInt()
        private val tp = TextPaint(Paint.ANTI_ALIAS_FLAG)
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG)

        fun background(card: Postcard, h: Int) {
            fill.shader = LinearGradient(0f, 0f, W.toFloat() * .4f, h.toFloat(), card.a, card.b, Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, W.toFloat(), h.toFloat(), fill)
            fill.shader = null
            // Soft lights in the accent's colour, like a lamp-lit room.
            val rnd = java.util.Random(card.ordinal * 31L + 7)
            repeat(5) {
                val x = rnd.nextFloat() * W; val y = rnd.nextFloat() * h; val r = 160f + rnd.nextFloat() * 260f
                fill.shader = RadialGradient(x, y, r, intArrayOf((accent and 0xFFFFFF) or 0x26000000, accent and 0xFFFFFF), null, Shader.TileMode.CLAMP)
                c.drawCircle(x, y, r, fill)
            }
            fill.shader = null
            // A scatter of tiny notes and stars.
            fill.color = 0x1FFFFFFF
            repeat(26) {
                val x = rnd.nextFloat() * W; val y = rnd.nextFloat() * h; val s = 4f + rnd.nextFloat() * 6f
                if (rnd.nextBoolean()) note(x, y, s * 3.2f, 0x22FFFFFF) else c.drawCircle(x, y, s / 2, fill)
            }
        }

        private fun note(x: Float, y: Float, s: Float, col: Int) {
            fill.color = col
            c.drawOval(RectF(x - s * .5f, y - s * .32f, x + s * .5f, y + s * .32f), fill)
            c.drawRect(x + s * .36f, y - s * 1.6f, x + s * .5f, y, fill)
            c.drawRect(x + s * .36f, y - s * 1.6f, x + s * 1.0f, y - s * 1.3f, fill)
        }

        fun brand(h: Int, ru: Boolean) {
            val top = (h - PAGE) / 2f
            note(78f, top + 96f, 22f, accent)
            text("Songling", 112f, top + 104f, 40f, cream, display)
            text(Words.t(ru, "музыка и питомец", "music and a pet"), W - 60f, top + 104f, 28f, faint, semi, Paint.Align.RIGHT)
        }

        fun head(eyebrow: String, title: String) {
            text(eyebrow.uppercase(), 60f, 210f, 30f, accent, bold, spacing = .14f, max = 960f)
            text(title, 60f, 300f, 82f, cream, display, max = 960f)
        }

        fun width(s: String, size: Float, font: Typeface): Float { tp.textSize = size; tp.typeface = font; tp.letterSpacing = 0f; return tp.measureText(s) }

        fun text(s: String, x: Float, y: Float, size: Float, color: Int, font: Typeface, align: Paint.Align = Paint.Align.LEFT, max: Float? = null, spacing: Float = 0f) {
            tp.textSize = size; tp.typeface = font; tp.color = color; tp.textAlign = align; tp.letterSpacing = spacing
            val t = if (max != null) TextUtils.ellipsize(s, tp, max, TextUtils.TruncateAt.END).toString() else s
            c.drawText(t, x, y, tp)
            tp.letterSpacing = 0f
        }

        fun wrap(s: String, x: Float, y: Float, width: Int, size: Float, color: Int, font: Typeface, lines: Int = 3, center: Boolean = false): Int {
            tp.textSize = size; tp.typeface = font; tp.color = color; tp.textAlign = Paint.Align.LEFT
            val l = StaticLayout.Builder.obtain(s, 0, s.length, tp, width).setAlignment(if (center) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL)
                .setMaxLines(lines).setEllipsize(TextUtils.TruncateAt.END).setLineSpacing(0f, .95f).build()
            c.save(); c.translate(x, y); l.draw(c); c.restore()
            return l.height
        }

        fun panel(x: Float, y: Float, w: Float, h: Float) {
            fill.color = 0x1AFFFFFF
            c.drawRoundRect(RectF(x, y, x + w, y + h), 32f, 32f, fill)
        }

        fun dot(x: Float, y: Float, r: Float, col: Int) { fill.color = col; c.drawCircle(x, y, r, fill) }

        fun bar(x: Float, y: Float, w: Float, h: Float, part: Float) {
            fill.color = 0x1FFFFFFF
            c.drawRoundRect(RectF(x, y, x + w, y + h), h / 2, h / 2, fill)
            fill.color = accent
            c.drawRoundRect(RectF(x, y, x + max(h, w * part.coerceIn(0f, 1f)), y + h), h / 2, h / 2, fill)
        }

        fun bars(values: List<Long>, x: Float, y: Float, w: Float, h: Float, hi: Int) {
            val top = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
            val bw = w / values.size
            values.forEachIndexed { i, v ->
                val bh = max(6f, v.toFloat() / top * h)
                fill.color = if (i == hi) accent else blend(0x40FFFFFF, accent, .35f)
                c.drawRoundRect(RectF(x + i * bw + bw * .14f, y + h - bh, x + (i + 1) * bw - bw * .14f, y + h), bw * .2f, bw * .2f, fill)
            }
        }

        fun chips(list: List<String>, y: Float, left: Boolean = false) {
            tp.textSize = 34f; tp.typeface = bold; tp.letterSpacing = 0f
            val widths = list.map { tp.measureText(it) + 56f }
            val total = widths.sum() + (list.size - 1) * 16f
            var x = if (left) 60f else (W - total) / 2
            list.forEachIndexed { i, s ->
                fill.color = if (i == 0) accent else 0x26FFFFFF
                c.drawRoundRect(RectF(x, y - 50f, x + widths[i], y + 14f), 32f, 32f, fill)
                text(s, x + widths[i] / 2, y - 6f, 34f, if (i == 0) 0xFF231A14.toInt() else cream, bold, Paint.Align.CENTER)
                x += widths[i] + 16f
            }
        }

        fun badge(s: String, x: Float, y: Float, col: Int) {
            fill.color = col
            c.drawCircle(x + 28f, y + 28f, 30f, fill)
            text(s, x + 28f, y + 40f, 32f, if (col == accent) 0xFF231A14.toInt() else cream, display, Paint.Align.CENTER)
        }

        fun glow(x: Float, y: Float, r: Float) {
            fill.shader = RadialGradient(x, y, r, intArrayOf((accent and 0xFFFFFF) or 0x44000000, accent and 0xFFFFFF), null, Shader.TileMode.CLAMP)
            c.drawCircle(x, y, r, fill)
            fill.shader = null
        }

        /** Pixel art, blown up with hard edges. */
        fun pixels(b: Bitmap, x: Int, y: Int, w: Int, h: Int) {
            c.drawBitmap(b, null, Rect(x, y, x + w, y + h), Paint().apply { isFilterBitmap = false; isAntiAlias = false })
        }

        fun smooth(b: Bitmap, x: Int, y: Int, w: Int, h: Int) {
            c.drawBitmap(b, null, Rect(x, y, x + w, y + h), Paint(Paint.FILTER_BITMAP_FLAG))
        }

        /** A picture in a rounded frame with a soft shadow. */
        fun framed(b: Bitmap, x: Int, y: Int, w: Int, h: Int, r: Float, hard: Boolean = true) {
            val box = RectF(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat())
            fill.color = 0x66000000
            fill.maskFilter = BlurMaskFilter(28f, BlurMaskFilter.Blur.NORMAL)
            c.drawRoundRect(RectF(box.left + 6, box.top + 18, box.right - 6, box.bottom + 18), r, r, fill)
            fill.maskFilter = null
            c.save()
            c.clipPath(Path().apply { addRoundRect(box, r, r, Path.Direction.CW) })
            c.drawBitmap(b, null, Rect(x, y, x + w, y + h), Paint().apply { isFilterBitmap = !hard })
            c.restore()
        }

        /** An album cover, or its genre's colour with the title when there's no picture. */
        fun cover(ctx: Context, file: String?, name: String, genre: Genre?, x: Int, y: Int, side: Int) {
            val bmp = Covers.bitmap(ctx, file, side)
            if (bmp != null) { framed(bmp, x, y, side, side, side * .06f, hard = false); return }
            val box = RectF(x.toFloat(), y.toFloat(), (x + side).toFloat(), (y + side).toFloat())
            fill.color = genre?.argb?.toInt() ?: 0xFF6A5A7A.toInt()
            c.drawRoundRect(box, side * .06f, side * .06f, fill)
            // A record peeking out, and the title.
            fill.color = 0x33000000
            c.drawCircle(x + side * .5f, y + side * .5f, side * .34f, fill)
            fill.color = 0x55FFFFFF
            c.drawCircle(x + side * .5f, y + side * .5f, side * .08f, fill)
            if (side >= 200) text(name, x + side / 2f, y + side - side * .1f, side * .09f, cream, display, Paint.Align.CENTER, side * .86f)
        }
    }
}
