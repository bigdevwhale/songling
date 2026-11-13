package app.songling.domain

import app.songling.data.Goal
import app.songling.data.GoalKind
import app.songling.data.MIN
import app.songling.data.Period
import app.songling.data.Play
import java.time.LocalDate

/** A goal or a quest as it stands. */
data class Progress(val have: Int, val need: Int) {
    val done get() = have >= need
    val part get() = if (need <= 0) 1f else (have.toFloat() / need).coerceIn(0f, 1f)
}

/** Listens inside a window of time, and what can be said about them. */
class Window(val st: Studio, val from: Long, val to: Long) {
    val plays: List<Play> = st.plays.filter { it.at in from until to }
    val ms: Long get() = plays.sumOf { it.ms }
    val minutes: Int get() = (ms / MIN).toInt()
    val whole: List<Pair<String, Long>> get() = st.wholeRuns.filter { it.second in from until to }
    val newArtists: Set<String> get() = plays.flatMap { app.songling.data.Keys.artists(it.artistName) }.filter { (st.byArtist[it]?.first ?: 0) in from until to }.toSet()
    val newTracks: Set<String> get() = plays.map { it.track }.filter { (st.byTrack[it]?.first ?: 0) in from until to }.toSet()
    /** Albums heard for the first time in the window. */
    val newAlbums: Set<String> get() = plays.map { it.album }.filter { it.isNotEmpty() && (st.byAlbum[it]?.first ?: 0) in from until to }.toSet()
    /** Genres (built-in and one's own) first heard in the window. */
    val newGenres: Set<String> get() {
        val first = HashMap<String, Long>()
        for (p in st.plays) { if (p.ms < 60_000) continue; val k = st.ownOf(p)?.let { "own:" + it.id } ?: st.genreOf(p)?.name ?: continue; if (k !in first) first[k] = p.at }
        return first.filterValues { it in from until to }.keys
    }
    /** Countries whose first artist was heard in the window. */
    val newCountries: Set<String> get() {
        val first = HashMap<String, Long>()
        for (p in st.plays) { val c = st.countryOf(p.artist).ifBlank { continue }; if (c !in first) first[c] = p.at }
        return first.filterValues { it in from until to }.keys
    }
    val genres: Set<Genre> get() = plays.filter { it.ms >= MIN }.mapNotNull { st.genreOf(it) }.toSet()
    val countries: Set<String> get() = plays.map { it.artist }.toSet().mapNotNull { st.countryOf(it).ifBlank { null } }.toSet()
    val decades: Set<Int> get() = plays.mapNotNull { st.yearOf(it)?.let { y -> y / 10 * 10 } }.toSet()
    val days: Set<LocalDate> get() = plays.groupBy { st.day(it.at) }.filterValues { l -> l.sumOf { it.ms } >= 10 * MIN }.keys
    fun genreMs(g: Genre) = plays.filter { st.genreOf(it) == g }.sumOf { it.ms }

    /** Things from the «want to hear» list heard in the window. */
    val wishesHeard: Int get() {
        val a = st.albums.values.filter { it.added > 0 }.associateBy { it.key }
        val t = st.tracks.values.filter { it.added > 0 }.associateBy { it.key }
        return plays.filter { p -> (a[p.album]?.let { p.at >= it.added } ?: false) || (t[p.track]?.let { p.at >= it.added } ?: false) }
            .map { if (a.containsKey(it.album)) it.album else it.track }.toSet().size
    }
}

object Goals {
    fun window(st: Studio, g: Goal): Window {
        val today = st.today
        return when (g.period) {
            Period.WEEK -> Window(st, st.weekStart, st.weekStart + 7 * 86_400_000L)
            Period.MONTH -> Window(st, st.startOf(today.withDayOfMonth(1)), st.startOf(today.withDayOfMonth(1).plusMonths(1)))
            Period.YEAR -> Window(st, st.startOf(today.withDayOfYear(1)), st.startOf(today.withDayOfYear(1).plusYears(1)))
            Period.ONCE -> Window(st, g.created - 60_000, if (g.until > 0) g.until else Long.MAX_VALUE)
        }
    }

    fun progress(st: Studio, g: Goal): Progress {
        val w = window(st, g)
        val have = when (g.kind) {
            GoalKind.MINUTES -> w.minutes
            GoalKind.ALBUMS -> w.whole.map { it.first }.toSet().size
            GoalKind.NEW_ARTISTS -> w.newArtists.size
            GoalKind.NEW_TRACKS -> w.newTracks.size
            GoalKind.ARTIST -> (w.plays.filter { g.ref in app.songling.data.Keys.artists(it.artistName) }.sumOf { it.ms } / MIN).toInt()
            GoalKind.ALBUM -> w.whole.count { it.first == g.ref }
            GoalKind.TRACK -> w.plays.count { it.track == g.ref }
            GoalKind.GENRE -> (if (g.ref.startsWith("own:")) w.plays.filter { st.ownOf(it)?.id == g.ref.removePrefix("own:") }.sumOf { it.ms }
                else Genre.entries.firstOrNull { it.name == g.ref }?.let { w.genreMs(it) } ?: 0L).div(MIN).toInt()
            GoalKind.GENRES -> w.genres.size
            GoalKind.COUNTRIES -> w.countries.size
            GoalKind.DECADES -> w.decades.size
            GoalKind.DAYS -> w.days.size
            GoalKind.WISHES -> w.wishesHeard
            GoalKind.PLAYS -> w.plays.size
            GoalKind.NEW_ALBUMS -> w.newAlbums.size
            GoalKind.NEW_GENRES -> w.newGenres.size
            GoalKind.NEW_COUNTRIES -> w.newCountries.size
        }
        return Progress(have, g.target)
    }

    /** Whether the target is counted in minutes (and is entered in hours). */
    fun inMinutes(k: GoalKind) = k == GoalKind.MINUTES || k == GoalKind.ARTIST || k == GoalKind.GENRE
}

/** The week's three small tasks. Each pays plectrums once. */
enum class Quest(val need: Int, val reward: Int) {
    WHOLE(1, 15), WHOLE3(3, 30), NEW_ARTISTS(3, 20), NEW_COUNTRY(1, 25), OLD(1, 20), GENRES(5, 20), MORNINGS(3, 15),
    NO_SKIPS(15, 15), WISH(1, 20), HOURS(300, 15), DECADES(3, 20), FORGOTTEN(1, 20), LONG(1, 15), NIGHT(2, 15);
}

data class QuestState(val q: Quest, val p: Progress)

object Quests {
    fun of(st: Studio): List<QuestState> {
        val w = Window(st, st.weekStart, st.weekStart + 7 * 86_400_000L)
        val seed = st.weekId.hashCode()
        val pool = Quest.entries.toMutableList()
        val rnd = java.util.Random(seed.toLong())
        pool.shuffle(rnd)
        // Not two of the same kind in one week.
        val picked = pool.filter { it != Quest.WHOLE3 || Quest.WHOLE !in pool.take(3) }.take(3)
        return picked.map { QuestState(it, Progress(have(st, w, it), it.need)) }
    }

    private fun have(st: Studio, w: Window, q: Quest): Int = when (q) {
        Quest.WHOLE, Quest.WHOLE3 -> w.whole.size
        Quest.NEW_ARTISTS -> w.newArtists.size
        Quest.NEW_COUNTRY -> {
            val before = st.plays.filter { it.at < w.from }.map { it.artist }.toSet().mapNotNull { st.countryOf(it).ifBlank { null } }.toSet()
            (w.countries - before).size
        }
        Quest.OLD -> w.plays.filter { p -> (st.yearOf(p) ?: 9999) <= st.today.year - 30 }.map { it.album }.toSet().count { it.isNotEmpty() }
        Quest.GENRES -> w.genres.size
        Quest.MORNINGS -> w.plays.filter { st.hour(it.at) in 5..8 }.map { st.day(it.at) }.toSet().size
        Quest.NO_SKIPS -> {
            var best = 0; var run = 0
            for (p in w.plays) { run = if (p.skipped) 0 else run + 1; best = maxOf(best, run) }
            best
        }
        Quest.WISH -> w.wishesHeard
        Quest.HOURS -> w.minutes
        Quest.DECADES -> w.decades.size
        Quest.FORGOTTEN -> w.plays.map { it.artist }.toSet().count { a ->
            val before = st.plays.lastOrNull { it.artist == a && it.at < w.from }?.at ?: return@count false
            w.from - before > 180 * 86_400_000L
        }
        Quest.LONG -> st.sessions.count { it.from >= w.from && it.ms >= 45 * MIN }
        Quest.NIGHT -> w.plays.filter { st.hour(it.at).let { h -> h >= 22 || h < 3 } }.map { st.day(it.at) }.toSet().size
    }
}
