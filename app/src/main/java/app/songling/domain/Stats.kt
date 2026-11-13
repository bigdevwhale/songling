package app.songling.domain

import app.songling.data.MIN
import app.songling.data.Play
import app.songling.data.Source
import java.time.DayOfWeek
import java.time.LocalDate

/** The span the statistics look at. */
enum class Span { WEEK, MONTH, YEAR, ALL, LAST_YEAR }

/** What the statistics are narrowed to. */
data class Filter(
    val span: Span = Span.MONTH,
    val genre: Genre? = null,
    /** A genre of one's own (its id). */
    val own: String? = null,
    val app: String? = null,
    val source: Source? = null,
    /** Only listens on headphones. */
    val headphones: Boolean = false,
)

data class Row(val key: String, val name: String, val sub: String, val ms: Long, val plays: Int)

/** The numbers for one [Filter]. */
class Stats(val st: Studio, val f: Filter) {
    val from: Long
    val to: Long

    init {
        val t = st.today
        when (f.span) {
            Span.WEEK -> { from = st.startOf(t.minusDays(6)); to = Long.MAX_VALUE }
            Span.MONTH -> { from = st.startOf(t.minusDays(29)); to = Long.MAX_VALUE }
            Span.YEAR -> { from = st.startOf(t.withDayOfYear(1)); to = Long.MAX_VALUE }
            Span.LAST_YEAR -> { from = st.startOf(t.withDayOfYear(1).minusYears(1)); to = st.startOf(t.withDayOfYear(1)) }
            Span.ALL -> { from = 0; to = Long.MAX_VALUE }
        }
    }

    val plays: List<Play> = st.plays.filter { p ->
        p.at in from until to &&
            (f.genre == null || st.genreOf(p) == f.genre) &&
            (f.own == null || st.ownOf(p)?.id == f.own) &&
            (f.app == null || p.app == f.app) &&
            (f.source == null || p.source == f.source) &&
            (!f.headphones || p.headphones)
    }

    val ms: Long = plays.sumOf { it.ms }
    val minutes get() = (ms / MIN).toInt()
    val days: Int get() = plays.map { st.day(it.at) }.toSet().size
    val firstDay: LocalDate? get() = plays.firstOrNull()?.let { st.day(it.at) }
    /** Days the span covers (up to today), for the average. */
    val spanDays: Int get() {
        val start = firstDay ?: return 1
        val begin = if (f.span == Span.ALL) start else maxOf(start, st.day(from))
        val end = if (to == Long.MAX_VALUE) st.today else st.day(to - 1)
        return (java.time.temporal.ChronoUnit.DAYS.between(begin, end) + 1).toInt().coerceAtLeast(1)
    }
    val perDay: Int get() = minutes / spanDays

    fun top(by: (Play) -> String, name: (String) -> String, sub: (String) -> String = { "" }, n: Int = 10): List<Row> =
        plays.groupBy(by).filterKeys { it.isNotEmpty() }.map { (k, l) -> Row(k, name(k), sub(k), l.sumOf { it.ms }, l.size) }
            .sortedByDescending { it.ms }.take(n)

    val artists get() = topArtists()

    /** Artists by time, each one billed on a track counted (#20). */
    fun topArtists(n: Int = 10): List<Row> =
        plays.flatMap { p -> app.songling.data.Keys.artists(p.artistName).map { it to p } }.groupBy({ it.first }, { it.second })
            .filterKeys { it.isNotEmpty() }.map { (k, l) -> Row(k, st.artistName(k), "", l.sumOf { it.ms }, l.size) }
            .sortedByDescending { it.ms }.take(n)
    val albums get() = top({ it.album }, st::albumTitle, { k -> st.albums[k]?.artistName ?: st.byAlbum[k]?.artistName.orEmpty() })
    val tracks get() = top({ it.track }, st::trackTitle, { k -> st.byTrack[k]?.artistName.orEmpty() })

    /** Time per genre of one's own, most first. */
    val ownGenres: List<Pair<app.songling.data.OwnGenre, Long>> get() = plays.mapNotNull { p -> st.ownOf(p)?.let { it to p.ms } }
        .groupBy({ it.first.id }, { it }).map { (_, l) -> l.first().first to l.sumOf { it.second } }.sortedByDescending { it.second }

    val genres: List<Pair<Genre, Long>> get() = plays.groupBy { st.genreOf(it) ?: Genre.OTHER }.map { it.key to it.value.sumOf { p -> p.ms } }.sortedByDescending { it.second }

    val hours: LongArray get() = LongArray(24).also { a -> for (p in plays) a[st.hour(p.at)] += p.ms }
    val weekdays: LongArray get() = LongArray(7).also { a -> for (p in plays) a[st.day(p.at).dayOfWeek.value - 1] += p.ms }

    /** Whether [series] is by month (a year or more) rather than by day. */
    val monthly: Boolean get() = f.span == Span.ALL || f.span == Span.YEAR || f.span == Span.LAST_YEAR

    /** Time per day (or per month for a year and more), oldest first, for the bar chart. */
    val series: List<Pair<LocalDate, Long>> get() {
        if (monthly) {
            val m = plays.groupBy { st.day(it.at).withDayOfMonth(1) }.mapValues { it.value.sumOf { p -> p.ms } }
            val first = (if (f.span == Span.ALL) m.keys.minOrNull() else st.day(from).withDayOfMonth(1)) ?: return emptyList()
            val last = if (to == Long.MAX_VALUE) st.today.withDayOfMonth(1) else st.day(to - 1).withDayOfMonth(1)
            return generateSequence(first) { it.plusMonths(1) }.takeWhile { !it.isAfter(last) }.map { it to (m[it] ?: 0L) }.toList()
        }
        val n = if (f.span == Span.WEEK) 7 else 30
        val m = plays.groupBy { st.day(it.at) }.mapValues { it.value.sumOf { p -> p.ms } }
        return (n - 1 downTo 0).map { st.today.minusDays(it.toLong()) }.map { it to (m[it] ?: 0L) }
    }

    val countries: List<Pair<String, Long>> get() = plays.groupBy { st.countryOf(it.artist) }.filterKeys { it.isNotEmpty() }
        .map { it.key to it.value.sumOf { p -> p.ms } }.sortedByDescending { it.second }
    val decades: List<Pair<Int, Long>> get() = plays.groupBy { st.yearOf(it)?.let { y -> y / 10 * 10 } }.filterKeys { it != null }
        .map { it.key!! to it.value.sumOf { p -> p.ms } }.sortedBy { it.first }
    val apps: List<Pair<String, Long>> get() = plays.groupBy { it.app.ifEmpty { it.source.name } }.map { it.key to it.value.sumOf { p -> p.ms } }.sortedByDescending { it.second }

    val whole: Int get() = st.wholeRuns.count { it.second in from until to }
    val newArtists: Int get() = plays.flatMap { app.songling.data.Keys.artists(it.artistName) }.toSet().count { (st.byArtist[it]?.first ?: 0) in from until to }
    val skips: Int get() = plays.count { it.skipped }
    val longest get() = st.sessions.filter { it.from in from until to }.maxByOrNull { it.ms }
    val busiestDay: Pair<LocalDate, Long>? get() = plays.groupBy { st.day(it.at) }.mapValues { it.value.sumOf { p -> p.ms } }.maxByOrNull { it.value }?.toPair()
    val favouriteWeekday: DayOfWeek? get() = weekdays.withIndex().maxByOrNull { it.value }?.takeIf { it.value > 0 }?.let { DayOfWeek.of(it.index + 1) }
    val peakHour: Int? get() = hours.withIndex().maxByOrNull { it.value }?.takeIf { it.value > 0 }?.index
}
