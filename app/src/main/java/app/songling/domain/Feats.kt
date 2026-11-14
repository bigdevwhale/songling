package app.songling.domain

import app.songling.data.HOUR
import app.songling.data.MIN
import app.songling.data.Source
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit

/** The shelves the achievements stand on. */
enum class FeatGroup { TIME, FINDS, HABITS, SOUND, HOLIDAYS, DEN }

/**
 * Achievements: each one counts something and has steps ([tiers]) to climb. Every step reached pays
 * [Feats.REWARD] plectrums. Worked out from the listens each time, so nothing has to be stored but the
 * steps already shown.
 */
enum class Feat(val group: FeatGroup, val tiers: IntArray) {
    // Each one's picture is resources/feats/<name in lower case>.png.
    // Time
    HOURS(FeatGroup.TIME, intArrayOf(10, 100, 500, 1000, 2500)),
    LISTENS(FeatGroup.TIME, intArrayOf(100, 1000, 10_000, 50_000)),
    DAYS(FeatGroup.TIME, intArrayOf(30, 100, 365, 1000)),
    STREAK(FeatGroup.TIME, intArrayOf(7, 30, 100, 365)),
    BIG_DAY(FeatGroup.TIME, intArrayOf(3, 6, 10)),
    SITTING(FeatGroup.TIME, intArrayOf(1, 3, 5)),
    MARATHON(FeatGroup.TIME, intArrayOf(50, 100, 200)),
    VETERAN(FeatGroup.TIME, intArrayOf(30, 182, 365, 730)),
    SEASONS(FeatGroup.TIME, intArrayOf(4)),
    WEEKENDS(FeatGroup.TIME, intArrayOf(10, 50, 150)),
    MONTH_HOURS(FeatGroup.TIME, intArrayOf(20, 50, 100, 200)),

    // Finds
    ARTISTS(FeatGroup.FINDS, intArrayOf(25, 100, 500, 1000)),
    TRACKS(FeatGroup.FINDS, intArrayOf(100, 1000, 5000)),
    WHOLE(FeatGroup.FINDS, intArrayOf(1, 10, 50, 100)),
    DISCOGRAPHY(FeatGroup.FINDS, intArrayOf(3, 5, 10)),
    COUNTRIES(FeatGroup.FINDS, intArrayOf(3, 10, 25, 50)),
    DECADES(FeatGroup.FINDS, intArrayOf(3, 5, 7)),
    GENRES(FeatGroup.FINDS, intArrayOf(5, 10, 15)),
    OMNIVORE(FeatGroup.FINDS, intArrayOf(4, 6, 8)),
    COLLABS(FeatGroup.FINDS, intArrayOf(10, 50, 200)),
    FRESH(FeatGroup.FINDS, intArrayOf(5, 25, 100)),
    VINTAGE(FeatGroup.FINDS, intArrayOf(1, 10, 30)),
    ANTIQUE(FeatGroup.FINDS, intArrayOf(1, 5, 20)),
    ALBUMS(FeatGroup.FINDS, intArrayOf(25, 100, 500, 1000)),
    NEW_DAY(FeatGroup.FINDS, intArrayOf(5, 10, 20)),
    GONE(FeatGroup.FINDS, intArrayOf(3, 25, 100)),

    // Habits
    NIGHT(FeatGroup.HABITS, intArrayOf(5, 25, 100)),
    MORNING(FeatGroup.HABITS, intArrayOf(5, 25, 100)),
    REPEAT(FeatGroup.HABITS, intArrayOf(10, 50, 100)),
    STUCK(FeatGroup.HABITS, intArrayOf(5, 10, 20)),
    ALBUM_AGAIN(FeatGroup.HABITS, intArrayOf(2, 5, 10)),
    LOYAL(FeatGroup.HABITS, intArrayOf(5, 24, 100)),
    NO_SKIPS(FeatGroup.HABITS, intArrayOf(25, 50, 100)),
    HEADPHONES(FeatGroup.HABITS, intArrayOf(10, 100, 500)),
    EPIC(FeatGroup.HABITS, intArrayOf(1, 10, 30)),
    COMEBACK(FeatGroup.HABITS, intArrayOf(1, 5)),
    PLAYERS(FeatGroup.HABITS, intArrayOf(2, 3, 5)),
    SHORT(FeatGroup.HABITS, intArrayOf(10, 50, 200)),
    ARTIST_DAY(FeatGroup.HABITS, intArrayOf(10, 25, 50)),
    ALBUM_DAY(FeatGroup.HABITS, intArrayOf(2, 4, 6)),
    TIMELESS(FeatGroup.HABITS, intArrayOf(3, 6, 12)),
    RATED(FeatGroup.HABITS, intArrayOf(10, 50, 200)),
    LOVED(FeatGroup.HABITS, intArrayOf(10, 50, 200)),
    NOTES(FeatGroup.HABITS, intArrayOf(1, 10, 30)),

    // Sound: hours in each of the forms a pet can take
    STORM(FeatGroup.SOUND, intArrayOf(5, 25, 100)),
    NEON(FeatGroup.SOUND, intArrayOf(5, 25, 100)),
    VELVET(FeatGroup.SOUND, intArrayOf(5, 25, 100)),
    CHOIR(FeatGroup.SOUND, intArrayOf(5, 25, 100)),
    STREET(FeatGroup.SOUND, intArrayOf(5, 25, 100)),
    ROOTS(FeatGroup.SOUND, intArrayOf(5, 25, 100)),
    OWN_GENRE(FeatGroup.SOUND, intArrayOf(1, 3, 6)),

    // Holidays and special listens
    NEW_YEAR(FeatGroup.HOLIDAYS, intArrayOf(5, 15, 40)),
    MIDNIGHT(FeatGroup.HOLIDAYS, intArrayOf(1, 3)),
    LIVE(FeatGroup.HOLIDAYS, intArrayOf(1, 5, 15)),
    VINYL(FeatGroup.HOLIDAYS, intArrayOf(1, 25, 100)),
    RADIO(FeatGroup.HOLIDAYS, intArrayOf(1, 25)),
    MUSIC_DAY(FeatGroup.HOLIDAYS, intArrayOf(1, 3)),
    FRIDAY13(FeatGroup.HOLIDAYS, intArrayOf(1, 3)),

    // The den and its pets
    PETS(FeatGroup.DEN, intArrayOf(3, 6, 12, 18, 24)),
    GROWN(FeatGroup.DEN, intArrayOf(1, 3, 6)),
    ANCIENT(FeatGroup.DEN, intArrayOf(1, 3)),
    SHINY(FeatGroup.DEN, intArrayOf(1)),
    COSY(FeatGroup.DEN, intArrayOf(3, 8, 16)),
    QUESTS(FeatGroup.DEN, intArrayOf(5, 25, 100)),
    GOALS(FeatGroup.DEN, intArrayOf(1, 5, 20)),
    FORMS(FeatGroup.DEN, intArrayOf(2, 4, 6)),
    GOURMET(FeatGroup.DEN, intArrayOf(5, 25, 100)),
    STROKES(FeatGroup.DEN, intArrayOf(10, 100, 1000)),
    WALLS(FeatGroup.DEN, intArrayOf(1, 3, 6)),
    SPENT(FeatGroup.DEN, intArrayOf(100, 500, 1500)),
}

/** How far one achievement got: [level] steps of its tiers reached. */
data class FeatState(val f: Feat, val have: Int) {
    val level: Int = f.tiers.count { have >= it }
    val max get() = f.tiers.size
    val done get() = level == max
    /** The next step, or the last one when all are reached. */
    val need: Int get() = f.tiers.getOrElse(level) { f.tiers.last() }
    val from: Int get() = if (level == 0) 0 else f.tiers[level - 1]
    val part: Float get() = if (done) 1f else ((have - from).toFloat() / (need - from)).coerceIn(0f, 1f)
}

object Feats {
    const val REWARD = 5

    /** Artists listed together: «A, B», «A feat. B», «A x B», «A with B», «A vs B». Duos with «&» are mostly bands. */
    private val together = Regex("""(,\s)|(\s(x|×|with|vs\.?|при участии)\s)|(\b(feat\.?|ft\.?|featuring)\s)""", RegexOption.IGNORE_CASE)
    private val featInTitle = Regex("""[(\[]\s*(feat\.?|ft\.?|featuring|при уч\.?|with)\s""", RegexOption.IGNORE_CASE)

    /** Words that mark a New Year or Christmas song (#9). */
    private val festive = listOf(
        "christmas", "xmas", "x-mas", "new year", "jingle", "santa", "let it snow", "noel", "noël", "navidad", "feliz navidad", "silent night",
        "winter wonderland", "holly jolly", "sleigh", "mistletoe", "rudolph", "snowman", "auld lang syne", "weihnacht",
        "новый год", "новогод", "рождеств", "ёлк", "елк", "ёлочк", "елочк", "дед мороз", "снегурочк", "с новым годом", "jingle bells", "бубенц",
    )

    fun isCollab(artistName: String, title: String) = together.containsMatchIn(artistName) || featInTitle.containsMatchIn(title)
    fun isFestive(title: String, album: String): Boolean {
        val t = (title + " " + album).lowercase()
        return festive.any { it in t }
    }

    fun of(st: Studio): List<FeatState> = Calc(st).let { c -> Feat.entries.map { FeatState(it, c.value(it)) } }

    private class Calc(val st: Studio) {
        val plays = st.plays

        // One pass for what needs to look at every listen.
        var maxDayPlays = 0
        var stuck = 0
        var noSkips = 0
        var headphonesMs = 0L
        var vinyl = 0; var radio = 0
        val epic = HashSet<String>()
        val collabs = HashSet<String>()
        val festiveTracks = HashSet<String>()
        val midnights = HashSet<Int>()
        val fresh = HashSet<String>()
        val antique = HashSet<String>()
        val players = HashSet<String>()
        val short = HashSet<String>()
        var artistDay = 0
        var timeless = 0

        init {
            val perDay = HashMap<LocalDate, Int>()
            val perDayTrack = HashMap<Pair<LocalDate, String>, Int>()
            val perDayArtist = HashMap<Pair<LocalDate, String>, Int>()
            val months = HashMap<String, HashSet<Int>>()
            var run = 0
            for (p in plays) {
                val d = st.day(p.at)
                val da = d to p.artist
                val na = (perDayArtist[da] ?: 0) + 1
                perDayArtist[da] = na
                if (na > artistDay) artistDay = na
                if (p.ms >= MIN) months.getOrPut(p.track) { HashSet() } += d.year * 12 + d.monthValue
                if (p.whole && p.length in 1 until 2 * MIN) short += p.track
                perDay[d] = (perDay[d] ?: 0) + 1
                val k = d to p.track
                val n = (perDayTrack[k] ?: 0) + 1
                perDayTrack[k] = n
                if (n > stuck) stuck = n
                run = if (p.skipped) 0 else run + 1
                if (run > noSkips) noSkips = run
                if (p.headphones) headphonesMs += p.ms
                when (p.source) {
                    Source.VINYL -> vinyl++
                    Source.RADIO -> radio++
                    else -> {}
                }
                if (p.whole && p.length >= 10 * MIN) epic += p.track
                if (p.ms >= MIN && isCollab(p.artistName, p.title)) collabs += p.track
                if (p.ms >= MIN && isFestive(p.title, p.albumName)) festiveTracks += p.track
                val t = Instant.ofEpochMilli(p.at).atZone(st.zone)
                if ((t.month == Month.DECEMBER && t.dayOfMonth == 31 && t.hour == 23) || (t.month == Month.JANUARY && t.dayOfMonth == 1 && t.hour == 0))
                    midnights += if (t.month == Month.JANUARY) t.year else t.year + 1
                if (p.album.isNotEmpty()) st.yearOf(p)?.let { y ->
                    if (y == d.year) fresh += p.album
                    if (y < 1970) antique += p.album
                }
                when {
                    p.source == Source.PLAYER && p.app.isNotEmpty() -> players += p.app
                    p.source != Source.PLAYER && p.source != Source.IMPORT -> players += p.source.name
                }
            }
            maxDayPlays = perDay.values.maxOrNull() ?: 0
            timeless = months.values.maxOfOrNull { it.size } ?: 0
        }

        fun formHours(f: Form): Int = (st.byGenre.filterKeys { it.form == f }.values.sum() / HOUR).toInt()

        fun value(f: Feat): Int = when (f) {
            Feat.HOURS -> (st.totalMs / HOUR).toInt()
            Feat.LISTENS -> plays.size
            Feat.DAYS -> st.musicDays.size
            Feat.STREAK -> st.bestStreak
            Feat.BIG_DAY -> ((st.byDay.values.maxOrNull() ?: 0L) / HOUR).toInt()
            Feat.SITTING -> ((st.sessions.maxOfOrNull { it.ms } ?: 0L) / HOUR).toInt()
            Feat.MARATHON -> maxDayPlays
            Feat.VETERAN -> plays.firstOrNull()?.let { ChronoUnit.DAYS.between(st.day(it.at), st.today).toInt() } ?: 0
            Feat.SEASONS -> st.musicDays.map { (it.monthValue % 12) / 3 }.toSet().size
            Feat.WEEKENDS -> st.musicDays.count { it.dayOfWeek.value >= 6 }
            Feat.MONTH_HOURS -> ((st.byDay.entries.groupBy({ it.key.year * 12 + it.key.monthValue }, { it.value }).values.maxOfOrNull { it.sum() } ?: 0L) / HOUR).toInt()
            Feat.ARTISTS -> st.byArtist.size
            Feat.TRACKS -> st.byTrack.size
            Feat.WHOLE -> st.wholeRuns.size
            Feat.DISCOGRAPHY -> st.wholeRuns.map { it.first }.toSet().groupBy { it.substringBefore('|') }.values.maxOfOrNull { it.size } ?: 0
            Feat.COUNTRIES -> st.countries.size
            Feat.DECADES -> st.decades.size
            Feat.GENRES -> st.byGenre.count { it.key != Genre.OTHER && it.value >= 10 * MIN }
            Feat.OMNIVORE -> st.mostGenresInADay
            Feat.COLLABS -> collabs.size
            Feat.FRESH -> fresh.size
            Feat.VINTAGE -> st.oldAlbums
            Feat.ANTIQUE -> antique.size
            Feat.ALBUMS -> st.byAlbum.size
            Feat.NEW_DAY -> st.byArtist.values.groupingBy { st.day(it.first) }.eachCount().values.maxOrNull() ?: 0
            Feat.GONE -> st.goneHours
            Feat.NIGHT -> st.nights
            Feat.MORNING -> st.mornings
            Feat.REPEAT -> st.maxRepeats
            Feat.STUCK -> stuck
            Feat.ALBUM_AGAIN -> st.byAlbum.values.maxOfOrNull { it.whole } ?: 0
            Feat.LOYAL -> ((st.byArtist.values.maxOfOrNull { it.ms } ?: 0L) / HOUR).toInt()
            Feat.NO_SKIPS -> noSkips
            Feat.HEADPHONES -> (headphonesMs / HOUR).toInt()
            Feat.EPIC -> epic.size
            Feat.COMEBACK -> st.comebacks
            Feat.PLAYERS -> players.size
            Feat.SHORT -> short.size
            Feat.ARTIST_DAY -> artistDay
            Feat.ALBUM_DAY -> st.wholeRuns.groupingBy { st.day(it.second) }.eachCount().values.maxOrNull() ?: 0
            Feat.TIMELESS -> timeless
            Feat.RATED -> st.albums.values.count { it.stars > 0 } + st.tracks.values.count { it.stars > 0 } + st.artists.values.count { it.stars > 0 }
            Feat.LOVED -> st.tracks.values.count { it.loved }
            Feat.NOTES -> st.albums.values.count { it.note.isNotBlank() }
            Feat.STORM -> formHours(Form.STORM)
            Feat.NEON -> formHours(Form.NEON)
            Feat.VELVET -> formHours(Form.VELVET)
            Feat.CHOIR -> formHours(Form.CHOIR)
            Feat.STREET -> formHours(Form.STREET)
            Feat.ROOTS -> formHours(Form.ROOTS)
            Feat.OWN_GENRE -> st.s.own.size
            Feat.NEW_YEAR -> festiveTracks.size
            Feat.MIDNIGHT -> midnights.size
            Feat.LIVE -> st.concerts
            Feat.VINYL -> vinyl
            Feat.RADIO -> radio
            Feat.MUSIC_DAY -> st.musicDays.count { it.month == Month.JUNE && it.dayOfMonth == 21 }
            Feat.FRIDAY13 -> st.musicDays.count { it.dayOfMonth == 13 && it.dayOfWeek == java.time.DayOfWeek.FRIDAY }
            Feat.PETS -> st.pets.size
            Feat.GROWN -> st.pets.count { it.stage >= Stage.ADULT }
            Feat.ANCIENT -> st.pets.count { it.stage == Stage.ANCIENT }
            Feat.SHINY -> st.pets.count { it.pet.shiny }
            Feat.COSY -> st.s.bought.count { b -> Item.entries.any { it.name == b } }
            Feat.QUESTS -> st.s.claimed.size
            Feat.GOALS -> st.goalsDone
            Feat.FORMS -> st.pets.mapNotNull { it.form }.toSet().size
            Feat.GOURMET -> (st.pets.sumOf { it.favMs } / HOUR).toInt()
            Feat.STROKES -> st.s.strokes
            Feat.WALLS -> st.s.bought.count { it.startsWith("wall:") }
            Feat.SPENT -> st.s.spent
        }
    }

    // ---------------------------------------------------------------- words

    private fun t(ru: Boolean, r: String, e: String) = if (ru) r else e
    private fun n(x: Int) = Words.num(x)

    fun group(g: FeatGroup, ru: Boolean) = when (g) {
        FeatGroup.TIME -> t(ru, "Время", "Time")
        FeatGroup.FINDS -> t(ru, "Открытия", "Finds")
        FeatGroup.HABITS -> t(ru, "Привычки", "Habits")
        FeatGroup.SOUND -> t(ru, "Звучание", "Sound")
        FeatGroup.HOLIDAYS -> t(ru, "Праздники и живой звук", "Holidays and live sound")
        FeatGroup.DEN -> t(ru, "Логово", "The den")
    }

    fun name(f: Feat, ru: Boolean) = when (f) {
        Feat.HOURS -> t(ru, "Наслушано", "Well listened")
        Feat.LISTENS -> t(ru, "Прослушивания", "Listens")
        Feat.DAYS -> t(ru, "Дни с музыкой", "Days of music")
        Feat.STREAK -> t(ru, "Без перерыва", "Unbroken")
        Feat.BIG_DAY -> t(ru, "Музыкальный день", "A day of music")
        Feat.SITTING -> t(ru, "Не отрываясь", "In one sitting")
        Feat.MARATHON -> t(ru, "Марафон", "Marathon")
        Feat.VETERAN -> t(ru, "Старожил", "Old-timer")
        Feat.SEASONS -> t(ru, "Круглый год", "All year round")
        Feat.WEEKENDS -> t(ru, "Выходные", "Weekends")
        Feat.MONTH_HOURS -> t(ru, "Месяц музыки", "A month of music")
        Feat.ARTISTS -> t(ru, "Знакомства", "Acquaintances")
        Feat.TRACKS -> t(ru, "Песенник", "Songbook")
        Feat.WHOLE -> t(ru, "От начала до конца", "Start to finish")
        Feat.DISCOGRAPHY -> t(ru, "Вся дискография", "The whole discography")
        Feat.COUNTRIES -> t(ru, "Путешественник", "Traveller")
        Feat.DECADES -> t(ru, "Машина времени", "Time machine")
        Feat.GENRES -> t(ru, "Палитра", "Palette")
        Feat.OMNIVORE -> t(ru, "Всеядный", "Omnivore")
        Feat.COLLABS -> t(ru, "Совместные", "Together")
        Feat.FRESH -> t(ru, "Свежак", "Fresh")
        Feat.VINTAGE -> t(ru, "Классика жанра", "A classic")
        Feat.ANTIQUE -> t(ru, "Патефон", "Phonograph")
        Feat.ALBUMS -> t(ru, "Коллекция", "Collection")
        Feat.NEW_DAY -> t(ru, "День открытий", "A day of finds")
        Feat.GONE -> t(ru, "Память", "In memory")
        Feat.NIGHT -> t(ru, "Сова", "Night owl")
        Feat.MORNING -> t(ru, "Жаворонок", "Early bird")
        Feat.REPEAT -> t(ru, "На повторе", "On repeat")
        Feat.STUCK -> t(ru, "Заело", "Stuck")
        Feat.ALBUM_AGAIN -> t(ru, "Любимая пластинка", "Favourite record")
        Feat.LOYAL -> t(ru, "Верность", "Loyalty")
        Feat.NO_SKIPS -> t(ru, "Ни одного пропуска", "No skips")
        Feat.HEADPHONES -> t(ru, "В своём мире", "In my own world")
        Feat.EPIC -> t(ru, "Эпос", "Epic")
        Feat.COMEBACK -> t(ru, "Возвращение", "Comeback")
        Feat.PLAYERS -> t(ru, "Отовсюду", "From everywhere")
        Feat.SHORT -> t(ru, "Коротко и ясно", "Short and sweet")
        Feat.ARTIST_DAY -> t(ru, "День одного артиста", "One artist's day")
        Feat.ALBUM_DAY -> t(ru, "Альбомный день", "A day of albums")
        Feat.TIMELESS -> t(ru, "Проверено временем", "Stands the test of time")
        Feat.RATED -> t(ru, "Критик", "Critic")
        Feat.LOVED -> t(ru, "От всего сердца", "With all my heart")
        Feat.NOTES -> t(ru, "Дневник", "Diary")
        Feat.STORM -> t(ru, "Буря", "Storm")
        Feat.NEON -> t(ru, "Неон", "Neon")
        Feat.VELVET -> t(ru, "Бархат", "Velvet")
        Feat.CHOIR -> t(ru, "Хор", "Choir")
        Feat.STREET -> t(ru, "Улица", "Street")
        Feat.ROOTS -> t(ru, "Корни", "Roots")
        Feat.OWN_GENRE -> t(ru, "Свой звук", "A sound of my own")
        Feat.NEW_YEAR -> t(ru, "Новогоднее настроение", "Holiday spirit")
        Feat.MIDNIGHT -> t(ru, "Под бой курантов", "At the stroke of midnight")
        Feat.LIVE -> t(ru, "Живой звук", "Live")
        Feat.VINYL -> t(ru, "Винил", "Vinyl")
        Feat.RADIO -> t(ru, "На волне", "On the air")
        Feat.MUSIC_DAY -> t(ru, "День музыки", "Music Day")
        Feat.FRIDAY13 -> t(ru, "Пятница, 13-е", "Friday the 13th")
        Feat.PETS -> t(ru, "Зверинец", "Menagerie")
        Feat.GROWN -> t(ru, "Вырастил", "All grown up")
        Feat.ANCIENT -> t(ru, "Древний", "Ancient")
        Feat.SHINY -> t(ru, "Блестяшка", "Shiny")
        Feat.COSY -> t(ru, "Уютно", "Cosy")
        Feat.QUESTS -> t(ru, "Порученец", "Errand-runner")
        Feat.GOALS -> t(ru, "Цель достигнута", "Goal reached")
        Feat.FORMS -> t(ru, "Все обличья", "Every shape")
        Feat.GOURMET -> t(ru, "Гурман", "Gourmet")
        Feat.STROKES -> t(ru, "Ласка", "A gentle hand")
        Feat.WALLS -> t(ru, "Ремонт", "Redecorating")
        Feat.SPENT -> t(ru, "Транжира", "Big spender")
    }

    private fun formGenres(f: Form, ru: Boolean) = Genre.entries.filter { it.form == f && it != Genre.OTHER && it != Genre.KIDS }.joinToString(", ") { Words.genre(it, ru).lowercase() }

    private fun hrs(x: Int, ru: Boolean) = if (ru) "${n(x)} ${Words.plural(x, "час", "часа", "часов")}" else "${n(x)} ${if (x == 1) "hour" else "hours"}"
    private fun days(x: Int, ru: Boolean) = if (ru) "${n(x)} ${Words.plural(x, "день", "дня", "дней")}" else "${n(x)} ${if (x == 1) "day" else "days"}"
    private fun times(x: Int, ru: Boolean) = Words.times(x, ru)
    private fun ru3(x: Int, one: String, few: String, many: String) = "${n(x)} ${Words.plural(x, one, few, many)}"
    private fun en(x: Int, one: String, many: String) = "${n(x)} ${if (x == 1) one else many}"

    /** What it takes to reach the step [x]. */
    fun goal(f: Feat, x: Int, ru: Boolean): String = when (f) {
        Feat.HOURS -> t(ru, "${hrs(x, true)} музыки", "${hrs(x, false)} of music")
        Feat.LISTENS -> Words.plays(x, ru).replace("$x", n(x))
        Feat.DAYS -> t(ru, "${days(x, true)} с музыкой", "${days(x, false)} with music")
        Feat.STREAK -> t(ru, "Музыка ${days(x, true)} подряд", "Music ${days(x, false)} in a row")
        Feat.BIG_DAY -> t(ru, "${hrs(x, true)} музыки за один день", "${hrs(x, false)} of music in one day")
        Feat.SITTING -> t(ru, (if (x == 1) "Час" else hrs(x, true)) + " музыки без перерыва", (if (x == 1) "An hour" else hrs(x, false)) + " of music without a break")
        Feat.MARATHON -> t(ru, "${ru3(x, "песня", "песни", "песен")} за один день", "${en(x, "song", "songs")} in one day")
        Feat.VETERAN -> t(ru, "${days(x, true)} с первой песни", "${days(x, false)} since the first song")
        Feat.SEASONS -> t(ru, "Музыка зимой, весной, летом и осенью", "Music in winter, spring, summer and autumn")
        Feat.WEEKENDS -> t(ru, "${ru3(x, "выходной", "выходных", "выходных")} с музыкой", "${en(x, "weekend day", "weekend days")} with music")
        Feat.MONTH_HOURS -> t(ru, "${hrs(x, true)} музыки за один месяц", "${hrs(x, false)} of music in one month")
        Feat.ARTISTS -> t(ru, ru3(x, "артист", "артиста", "артистов"), en(x, "artist", "artists"))
        Feat.TRACKS -> t(ru, "${ru3(x, "разная песня", "разные песни", "разных песен")}", "${en(x, "different song", "different songs")}")
        Feat.WHOLE -> t(ru, "${ru3(x, "альбом", "альбома", "альбомов")} целиком", "${en(x, "album", "albums")} heard whole")
        Feat.DISCOGRAPHY -> t(ru, "${ru3(x, "альбом", "альбома", "альбомов")} одного артиста целиком", "${en(x, "album", "albums")} by one artist, whole")
        Feat.COUNTRIES -> t(ru, "Артисты из ${n(x)} ${Words.plural(x, "страны", "стран", "стран")}", "Artists from ${en(x, "country", "countries")}")
        Feat.DECADES -> t(ru, "Музыка ${n(x)} ${Words.plural(x, "десятилетия", "десятилетий", "десятилетий")}", "Music from ${en(x, "decade", "decades")}")
        Feat.GENRES -> t(ru, "${ru3(x, "жанр", "жанра", "жанров")}, хотя бы по 10 минут", "${en(x, "genre", "genres")}, ten minutes each at least")
        Feat.OMNIVORE -> t(ru, "${ru3(x, "жанр", "жанра", "жанров")} за один день", "${en(x, "genre", "genres")} in one day")
        Feat.COLLABS -> t(ru, "${ru3(x, "песня", "песни", "песен")} двух и более артистов (feat., «A, B», «A x B»)", "${en(x, "song", "songs")} by two artists or more (feat., «A, B», «A x B»)")
        Feat.FRESH -> t(ru, "${ru3(x, "альбом", "альбома", "альбомов")} в год их выхода", "${en(x, "album", "albums")} in the year they came out")
        Feat.VINTAGE -> t(ru, "${ru3(x, "альбом", "альбома", "альбомов")} старше 30 лет", "${en(x, "album", "albums")} over thirty years old")
        Feat.ANTIQUE -> t(ru, "${ru3(x, "альбом", "альбома", "альбомов")}, вышедших до 1970 года", "${en(x, "album", "albums")} from before 1970")
        Feat.ALBUMS -> t(ru, ru3(x, "разный альбом", "разных альбома", "разных альбомов"), en(x, "different album", "different albums"))
        Feat.NEW_DAY -> t(ru, "${ru3(x, "новый артист", "новых артиста", "новых артистов")} за один день", "${en(x, "new artist", "new artists")} in one day")
        Feat.GONE -> t(ru, "${hrs(x, true)} музыки тех, кого уже нет", "${hrs(x, false)} of music by those who are gone")
        Feat.NIGHT -> t(ru, "${ru3(x, "ночь", "ночи", "ночей")} с музыкой после 22:00", "${en(x, "night", "nights")} of music after 10 pm")
        Feat.MORNING -> t(ru, "${ru3(x, "утро", "утра", "утр")} с музыкой с 5 до 10 утра", "${en(x, "morning", "mornings")} of music between 5 and 10 am")
        Feat.REPEAT -> t(ru, "Одна песня ${times(x, true)}", "One song ${times(x, false)}")
        Feat.STUCK -> t(ru, "Одна песня ${times(x, true)} за день", "One song ${times(x, false)} in a day")
        Feat.ALBUM_AGAIN -> t(ru, "Один альбом целиком ${times(x, true)}", "One album whole ${times(x, false)}")
        Feat.LOYAL -> t(ru, "${hrs(x, true)} одного артиста", "${hrs(x, false)} of one artist")
        Feat.NO_SKIPS -> t(ru, "${ru3(x, "песня", "песни", "песен")} подряд без пропусков", "${en(x, "song", "songs")} in a row, no skips")
        Feat.HEADPHONES -> t(ru, "${hrs(x, true)} в наушниках", "${hrs(x, false)} on headphones")
        Feat.EPIC -> t(ru, "${ru3(x, "песня", "песни", "песен")} длиннее 10 минут до конца", "${en(x, "song", "songs")} over ten minutes, to the end")
        Feat.COMEBACK -> t(ru, "Вернуться к музыке после недели тишины ${times(x, true)}", "Come back to music after a silent week ${times(x, false)}")
        Feat.PLAYERS -> t(ru, "Музыка из ${n(x)} ${Words.plural(x, "приложения", "приложений", "приложений")} или источников", "Music from ${en(x, "app or source", "apps or sources")}")
        Feat.SHORT -> t(ru, "${ru3(x, "песня", "песни", "песен")} короче двух минут до конца", "${en(x, "song", "songs")} under two minutes, to the end")
        Feat.ARTIST_DAY -> t(ru, "${ru3(x, "песня", "песни", "песен")} одного артиста за день", "${en(x, "song", "songs")} by one artist in a day")
        Feat.ALBUM_DAY -> t(ru, "${ru3(x, "альбом", "альбома", "альбомов")} целиком за один день", "${en(x, "whole album", "whole albums")} in one day")
        Feat.TIMELESS -> t(ru, "Одна песня в ${n(x)} разных месяцах", "One song in ${en(x, "different month", "different months")}")
        Feat.RATED -> t(ru, "Звёзды ${n(x)} альбомам, песням или артистам", "Stars for ${en(x, "album, song or artist", "albums, songs or artists")}")
        Feat.LOVED -> t(ru, "${ru3(x, "песня", "песни", "песен")} с сердечком", "${en(x, "song", "songs")} with a heart")
        Feat.NOTES -> t(ru, "${ru3(x, "заметка", "заметки", "заметок")} к альбомам", "${en(x, "note", "notes")} on albums")
        Feat.STORM, Feat.NEON, Feat.VELVET, Feat.CHOIR, Feat.STREET, Feat.ROOTS -> {
            val form = Form.valueOf(f.name)
            t(ru, "${hrs(x, true)}: ${formGenres(form, true)}", "${hrs(x, false)}: ${formGenres(form, false)}")
        }
        Feat.OWN_GENRE -> t(ru, "Придумать ${ru3(x, "свой жанр", "своих жанра", "своих жанров")}", "Make up ${en(x, "genre", "genres")} of your own")
        Feat.NEW_YEAR -> t(ru, ru3(x, "новогодняя песня", "новогодние песни", "новогодних песен") + " (и рождественских)", en(x, "Christmas or New Year song", "Christmas and New Year songs"))
        Feat.MIDNIGHT -> t(ru, "Музыка в новогоднюю полночь (с 23:00 до 01:00)" + if (x > 1) " – ${n(x)} ${Words.plural(x, "год", "года", "лет")}" else "", "Music at New Year's midnight (11 pm to 1 am)" + if (x > 1) " – ${en(x, "year", "years")}" else "")
        Feat.LIVE -> t(ru, "${ru3(x, "концерт", "концерта", "концертов")}", "${en(x, "concert", "concerts")}")
        Feat.VINYL -> t(ru, "${ru3(x, "песня", "песни", "песен")} с пластинки", "${en(x, "song", "songs")} off a record")
        Feat.RADIO -> t(ru, "${ru3(x, "песня", "песни", "песен")} по радио", "${en(x, "song", "songs")} on the radio")
        Feat.MUSIC_DAY -> t(ru, "Музыка в День музыки, 21 июня" + if (x > 1) " – ${n(x)} ${Words.plural(x, "год", "года", "лет")}" else "", "Music on World Music Day, June 21" + if (x > 1) " – ${en(x, "year", "years")}" else "")
        Feat.FRIDAY13 -> t(ru, "Музыка в пятницу, 13-го" + if (x > 1) " – ${times(x, true)}" else "", "Music on a Friday the 13th" + if (x > 1) " – ${times(x, false)}" else "")
        Feat.PETS -> t(ru, "Завести " + ru3(x, "питомца", "питомцев", "питомцев"), "Raise ${en(x, "pet", "pets")}")
        Feat.GROWN -> t(ru, "Вырастить ${ru3(x, "взрослого питомца", "взрослых питомцев", "взрослых питомцев")}", "Raise ${en(x, "pet", "pets")} to grown-up")
        Feat.ANCIENT -> t(ru, "${ru3(x, "древний питомец", "древних питомца", "древних питомцев")}", "${en(x, "ancient pet", "ancient pets")}")
        Feat.SHINY -> t(ru, "Найти блестящего питомца", "Find a shiny pet")
        Feat.COSY -> t(ru, "${ru3(x, "вещь", "вещи", "вещей")} в логове", "${en(x, "thing", "things")} in the den")
        Feat.QUESTS -> t(ru, "${ru3(x, "задание", "задания", "заданий")} недели", "${en(x, "weekly quest", "weekly quests")}")
        Feat.GOALS -> t(ru, "Достичь " + ru3(x, "цели", "целей", "целей"), "Reach ${en(x, "goal", "goals")}")
        Feat.FORMS -> t(ru, "Питомцы ${n(x)} разных форм", "Pets in ${en(x, "different form", "different forms")}")
        Feat.GOURMET -> t(ru, "${hrs(x, true)} любимой музыки питомцев", "${hrs(x, false)} of your pets' favourite music")
        Feat.STROKES -> t(ru, "Погладить питомца ${times(x, true)}", "Stroke your pet ${times(x, false)}")
        Feat.WALLS -> t(ru, "Купить обои: ${n(x)}", "Buy ${en(x, "wallpaper", "wallpapers")}")
        Feat.SPENT -> t(ru, "Потратить ${ru3(x, "медиатор", "медиатора", "медиаторов")} на логово", "Spend ${en(x, "plectrum", "plectrums")} on the den")
    }
}
