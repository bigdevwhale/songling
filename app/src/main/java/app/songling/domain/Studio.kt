package app.songling.domain

import app.songling.data.Album
import app.songling.data.Artist
import app.songling.data.DAY
import app.songling.data.Goal
import app.songling.data.Kind
import app.songling.data.MIN
import app.songling.data.Pet
import app.songling.data.Play
import app.songling.data.Settings
import app.songling.data.Source
import app.songling.data.Track
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.IsoFields
import kotlin.math.max

/** Totals for one artist, album or song. */
class Tally(val key: String) {
    var ms = 0L
    var plays = 0
    var first = Long.MAX_VALUE
    var last = 0L
    var whole = 0
    var skips = 0
    val tracks = HashSet<String>()
    val albums = HashSet<String>()
    var name = ""
    var artistName = ""
    var albumName = ""
    val minutes get() = (ms / MIN).toInt()

    fun add(p: Play) {
        ms += p.ms; plays++
        if (p.at < first) first = p.at
        if (p.at > last) last = p.at
        if (p.skipped) skips++
        tracks += p.track
        if (p.album.isNotEmpty()) albums += p.album
    }
}

/** A run of listening with no gap longer than a few minutes. */
data class Session(val from: Long, val to: Long, val ms: Long, val plays: Int)

/** A pet as it stands now. */
/** [found]: artists heard for the first time while it was out (the curious one is paid for them). */
class PetState(val pet: Pet, val fed: Int, val stage: Stage, val byGenre: Map<Genre, Long>, val favMs: Long, val found: Int = 0, val ms: Long = 0) {
    /** Plectrums its gifts brought: favourites pay double, a curious one is paid for every new artist. */
    val bonus: Int get() = (favMs / (10 * MIN)).toInt() + if (pet.nature == Nature.CURIOUS) found else 0
    val species get() = pet.species
    val egg get() = stage == Stage.EGG
    /** The form it leans to by what it ate. */
    val leaning: Form? get() = byGenre.entries.groupBy({ it.key.form }, { it.value }).mapValues { it.value.sum() }.maxByOrNull { it.value }?.key
    val form: Form? get() = pet.form ?: if (stage >= Stage.ADULT) leaning else null
    /** 0..1 through the current stage. */
    val progress: Float get() {
        val next = stage.next ?: return 1f
        return ((fed - stage.from).toFloat() / (next.from - stage.from)).coerceIn(0f, 1f)
    }
    val out get() = pet.spans.endsWith("-0")
}

/** How near an egg is to being found. */
data class Hint(val species: Species, val have: Int, val need: Int, val minutes: Boolean = false) {
    val open get() = have >= need
    /** What the egg asks for in its own words: hours for one counted in [minutes]. */
    val asked get() = if (minutes) need / 60 else need
}

/**
 * Everything the app knows, worked out from the listens, the catalogue, the pets and the settings at
 * one moment [now]. Screens read it; nothing here writes.
 */
class Studio(
    val plays: List<Play>,
    albumList: List<Album>,
    artistList: List<Artist>,
    trackList: List<Track>,
    petList: List<Pet>,
    val goals: List<Goal>,
    val s: Settings,
    val now: Long,
    val zone: ZoneId = ZoneId.systemDefault(),
) {
    val albums: Map<String, Album> = albumList.associateBy { it.key }
    val artists: Map<String, Artist> = artistList.associateBy { it.key }
    val tracks: Map<String, Track> = trackList.associateBy { it.key }

    /** The local day a moment belongs to; listens before [Settings.dayStart] count to the evening before. */
    fun day(t: Long): LocalDate = Instant.ofEpochMilli(t - s.dayStart * 3_600_000L).atZone(zone).toLocalDate()
    fun hour(t: Long): Int = Instant.ofEpochMilli(t).atZone(zone).hour
    fun startOf(d: LocalDate): Long = d.atStartOfDay(zone).toInstant().toEpochMilli() + s.dayStart * 3_600_000L
    val today: LocalDate = day(now)

    /** Genres of one's own, by id. */
    val owns: Map<String, app.songling.data.OwnGenre> = s.own.associateBy { it.id }

    /** The own genre a listen belongs to: the song's, else the album's, else the artist's. */
    fun ownOf(p: Play): app.songling.data.OwnGenre? = owns[tracks[p.track]?.own?.ifEmpty { null } ?: albums[p.album]?.own?.ifEmpty { null } ?: artists[p.artist]?.own.orEmpty()]

    /**
     * A listen's genre. What was told by hand comes first (one's own genre, then the song's, the album's, the
     * artist's); the catalogue's guess only after it, and its «other» last of all.
     */
    fun genreOf(p: Play): Genre? {
        ownOf(p)?.let { o -> Genre.entries.firstOrNull { it.name == o.base }?.let { return it } }
        val t = tracks[p.track]
        val a = albums[p.album.ifEmpty { t?.album.orEmpty() }]
        if (t?.pinned == true && t.genre != null) return t.genre
        if (a?.pinned == true && a.genre != null) return a.genre
        artists[p.artist]?.genre?.let { return it }
        val guess = listOfNotNull(t?.genre, a?.genre)
        // The artist's tags (MusicBrainz) when the song and its album say nothing useful.
        return guess.firstOrNull { it != Genre.OTHER } ?: artists[p.artist]?.tagGenre ?: guess.firstOrNull()
    }
    fun yearOf(p: Play): Int? = tracks[p.track]?.year ?: albums[p.album]?.year
    /**
     * Genres told beside the main one (#23), as tokens: a [Genre] name or «own:<id>». The song's own list when its
     * genre was set by hand, else its album's.
     */
    fun moreOf(p: Play): List<String> {
        val t = tracks[p.track]
        val raw = if (t != null && (t.more.isNotEmpty() || t.pinned || t.own.isNotEmpty())) t.more else albums[p.album.ifEmpty { t?.album.orEmpty() }]?.more.orEmpty()
        return if (raw.isEmpty()) emptyList() else raw.split(',').filter { it.isNotBlank() }
    }

    private val genresSeen = java.util.concurrent.ConcurrentHashMap<Triple<String, String, String>, List<Genre>>()

    /** Every genre a listen belongs to, the main one first; it counts in full for each of them (#23). */
    fun genresOf(p: Play): List<Genre> = genresSeen.getOrPut(Triple(p.track, p.album, p.artist)) {
        val more = moreOf(p).mapNotNull { k ->
            val name = if (k.startsWith("own:")) owns[k.removePrefix("own:")]?.base else k
            Genre.entries.firstOrNull { it.name == name }
        }
        (listOfNotNull(genreOf(p)) + more).distinct()
    }

    /** Every genre of one's own a listen belongs to. */
    fun ownsOf(p: Play): List<app.songling.data.OwnGenre> =
        (listOfNotNull(ownOf(p)) + moreOf(p).mapNotNull { if (it.startsWith("own:")) owns[it.removePrefix("own:")] else null }).distinct()

    /** The genres a listen counts for as goal keys: «own:<id>» for one's own, else the genre's name. */
    fun genreKeys(p: Play): List<String> {
        val main = ownOf(p)?.let { "own:" + it.id } ?: genreOf(p)?.name
        return (listOfNotNull(main) + moreOf(p)).distinct()
    }

    fun countryOf(artist: String): String = artists[artist]?.country.orEmpty()

    // ---------------------------------------------------------------- totals

    val totalMs: Long = plays.sumOf { it.ms }
    val minutes: Int get() = (totalMs / MIN).toInt()

    val byArtist = HashMap<String, Tally>()
    val byAlbum = HashMap<String, Tally>()
    val byTrack = HashMap<String, Tally>()
    val byGenre = HashMap<Genre, Long>()
    val byDay = HashMap<LocalDate, Long>()

    init {
        for (p in plays) {
            // Every artist on the track gets the listen, not only the first-billed one (#20).
            // A listen with no artist isn't an artist: the hound's 20 hours and the stats' top list both skip it.
            if (p.artist.isNotEmpty()) byArtist.getOrPut(p.artist) { Tally(p.artist).also { it.name = artists[p.artist]?.name ?: app.songling.data.Keys.mainArtist(p.artistName) } }.add(p)
            for (n in app.songling.data.Keys.allArtists(p.artistName)) {
                val k = app.songling.data.Keys.norm(n)
                if (k.isEmpty() || k == p.artist) continue
                byArtist.getOrPut(k) { Tally(k).also { it.name = artists[k]?.name ?: n } }.add(p)
            }
            if (p.album.isNotEmpty()) byAlbum.getOrPut(p.album) { Tally(p.album).also { it.name = p.albumName; it.artistName = p.artistName } }.add(p)
            byTrack.getOrPut(p.track) { Tally(p.track).also { it.name = p.title; it.artistName = p.artistName; it.albumName = p.albumName } }.add(p)
            for (g in genresOf(p)) byGenre[g] = (byGenre[g] ?: 0L) + p.ms
            val d = day(p.at)
            byDay[d] = (byDay[d] ?: 0L) + p.ms
        }
    }

    // ---------------------------------------------------------------- sessions and whole albums

    val sessions: List<Session> = buildList {
        var from = 0L; var to = 0L; var ms = 0L; var n = 0
        for (p in plays) {
            val end = p.at + max(p.ms, 1)
            if (n > 0 && p.at - to > 6 * MIN) { add(Session(from, to, ms, n)); n = 0 }
            if (n == 0) { from = p.at; ms = 0 }
            to = max(to, end); ms += p.ms; n++
        }
        if (n > 0) add(Session(from, to, ms, n))
    }

    /**
     * Albums heard whole: most of their songs in one sitting, in any order and with other songs in between,
     * as long as no more than 40 minutes pass between two of its songs. Each as (album key, when).
     */
    val wholeRuns: List<Pair<String, Long>> = buildList {
        class Run(val start: Long) { var last = 0L; var ms = 0L; var marked = false; val seen = HashSet<String>() }
        val open = HashMap<String, Run>()
        fun close(key: String, r: Run) {
            // A record or a whole album logged by hand.
            if (r.marked) { add(key to r.start); return }
            val a = albums[key]
            val need = when {
                a == null || a.tracks <= 0 -> 7
                a.tracks == 1 -> Int.MAX_VALUE
                // A short release counts only with every song on it.
                a.tracks <= 3 -> a.tracks
                else -> max(3, (a.tracks * 0.8f).toInt())
            }
            // Songs the catalogue spells differently still count by time, but never one song alone.
            val longEnough = a != null && a.length > 0 && r.ms >= a.length * 0.8 && a.tracks >= 3 && r.seen.size >= 2
            if (r.seen.size >= need || longEnough) add(key to r.start)
        }
        for (p in plays) {
            if (p.album.isEmpty()) continue
            val was = open[p.album]
            val r = if (was == null || p.at - was.last > 40 * MIN) { was?.let { close(p.album, it) }; Run(p.at).also { open[p.album] = it } } else was
            r.seen += p.track; r.ms += p.ms
            if (p.whole && p.source != Source.PLAYER && p.source != Source.IMPORT && p.source != Source.LASTFM) r.marked = true
            // The album just heard in a player, marked whole by hand without adding any time.
            if (p.ref?.startsWith("whole:") == true) r.marked = true
            r.last = max(r.last, p.at + p.ms)
        }
        for ((k, r) in open) close(k, r)
        sortBy { it.second }
    }

    init { for ((k, _) in wholeRuns) byAlbum[k]?.let { it.whole++ } }

    // ---------------------------------------------------------------- days

    /** Days with at least ten minutes of music. */
    val musicDays: Set<LocalDate> = byDay.filterValues { it >= 10 * MIN }.keys

    val streak: Int get() {
        var d = today
        if (d !in musicDays) d = d.minusDays(1)
        var n = 0
        while (d in musicDays) { n++; d = d.minusDays(1) }
        return n
    }

    val bestStreak: Int by lazy {
        var best = 0; var run = 0; var prev: LocalDate? = null
        for (d in musicDays.sorted()) { run = if (prev != null && d == prev.plusDays(1)) run + 1 else 1; best = max(best, run); prev = d }
        best
    }

    /** Minutes in a window of hours, by day: nights (22–4) or mornings (5–10). */
    private fun daysIn(hours: IntRange, wrap: Boolean = false): Int {
        val m = HashMap<LocalDate, Long>()
        for (p in plays) {
            val h = hour(p.at)
            val inside = if (wrap) h >= hours.first || h <= hours.last else h in hours
            if (inside) { val d = day(p.at); m[d] = (m[d] ?: 0L) + p.ms }
        }
        return m.count { it.value >= 20 * MIN }
    }

    val nights: Int by lazy { daysIn(22..3, wrap = true) }
    val mornings: Int by lazy { daysIn(5..9) }
    val lastPlay: Long get() = plays.lastOrNull()?.let { it.at + it.ms } ?: 0L

    /** Comebacks: a listen after a week and more of silence. */
    val comebacks: Int by lazy { plays.zipWithNext().count { (a, b) -> b.at - (a.at + a.ms) >= 7 * DAY } }

    val countries: Set<String> get() = byArtist.keys.mapNotNull { countryOf(it).ifBlank { null } }.toSet()
    val decades: Set<Int> get() = byAlbum.keys.mapNotNull { albums[it]?.year?.let { y -> y / 10 * 10 } }.toSet()
    val concerts: Int get() = plays.count { it.source == Source.CONCERT }
    val longSessions: Int get() = sessions.count { it.ms >= 45 * MIN }
    val maxRepeats: Int get() = byTrack.values.maxOfOrNull { it.plays } ?: 0

    /** Minutes and hours of music by artists who have passed away (#15). */
    val goneMinutes: Int by lazy { (plays.filter { (artists[it.artist]?.gone ?: 0) > 0 }.sumOf { it.ms } / MIN).toInt() }
    val goneHours: Int get() = goneMinutes / 60

    /** A band that broke up (#56): a group whose life-span has ended in MusicBrainz – Кино, Daft Punk. */
    private fun disbanded(key: String?) = artists[key]?.let { it.type == "Group" && it.ended > 0 } == true
    /** Minutes of music by bands that broke up: the hermit crab keeps their shell. */
    val disbandedMinutes: Int by lazy { (plays.filter { disbanded(it.artist) }.sumOf { it.ms } / MIN).toInt() }

    val oldAlbums: Int get() = byAlbum.values.count { t ->
        val y = albums[t.key]?.year ?: return@count false
        t.ms >= 20 * MIN && day(t.first).year - y >= 30
    }

    /** The most genres heard in one day. */
    val mostGenresInADay: Int get() = genresByDay.values.maxOfOrNull { it } ?: 0

    /** How many genres each day had. */
    private val genresByDay: Map<LocalDate, Int> by lazy {
        val m = HashMap<LocalDate, HashSet<Genre>>()
        for (p in plays) if (p.ms >= MIN) genresOf(p).let { if (it.isNotEmpty()) m.getOrPut(day(p.at)) { HashSet() } += it }
        m.mapValues { it.value.size }
    }

    /** When music came back after three days and more of silence (the echo's favourite is the day after). */
    private val returns: java.util.TreeSet<Long> by lazy {
        java.util.TreeSet(plays.zipWithNext().filter { (a, b) -> b.at - (a.at + a.ms) >= 3 * DAY }.map { it.second.at })
    }

    /** The sitting a moment falls in. */
    private fun sessionAt(t: Long): Session? {
        val i = sessions.binarySearch { it.from.compareTo(t) }.let { if (it >= 0) it else -it - 2 }
        return sessions.getOrNull(i)?.takeIf { t in it.from..it.to }
    }

    /** Different songs by two artists or more. */
    val collabTracks: Int by lazy { plays.filter { it.ms >= MIN && Feats.isCollab(it.artistName, it.title) }.map { it.track }.toSet().size }

    /** Days with music in December, January and February. */
    val winterDays: Int get() = musicDays.count { it.monthValue == 12 || it.monthValue <= 2 }

    /** Listens off a record or the radio. */
    val offPhone: Int get() = plays.count { it.source == Source.VINYL || it.source == Source.RADIO }

    /** Different songs of eight minutes and more, heard to the end. */
    val longSongs: Int by lazy { plays.filter { it.whole && it.length >= LONG_SONG }.map { it.track }.toSet().size }

    /** The artist listened to longest of all, and the hours of them. */
    val topArtist: String? by lazy { byArtist.maxByOrNull { it.value.ms }?.key }
    val topArtistMinutes: Int get() = ((byArtist[topArtist]?.ms ?: 0L) / MIN).toInt()
    val topArtistHours: Int get() = topArtistMinutes / 60

    fun genreMinutes(vararg g: Genre): Int = (g.sumOf { byGenre[it] ?: 0L } / MIN).toInt()
    fun genreHours(vararg g: Genre): Int = genreMinutes(*g) / 60

    // ---------------------------------------------------------------- the ten pets of 1.17

    /** Things given stars: albums, songs and artists (the critic fox). */
    val rated: Int get() = albums.values.count { it.stars > 0 } + tracks.values.count { it.stars > 0 } + artists.values.count { it.stars > 0 }

    /** The best stars a listen has: its song's, its album's or its artist's. */
    fun starsOf(p: Play): Int = maxOf(tracks[p.track]?.stars ?: 0, albums[p.album]?.stars ?: 0, artists[p.artist]?.stars ?: 0)

    /** Whether a listen is of something put on the «to hear» list before it was heard (as Goals' wishesHeard). */
    fun wasWish(p: Play): Boolean =
        (albums[p.album]?.added?.let { it > 0 && p.at >= it } ?: false) || (tracks[p.track]?.added?.let { it > 0 && p.at >= it } ?: false)

    /** Different things from the «to hear» list heard (the penguin). */
    val wishesHeard: Int by lazy { plays.filter { wasWish(it) }.map { if ((albums[it.album]?.added ?: 0L) > 0) it.album else it.track }.toSet().size }

    /** The moon's age in days at a moment: 0 new, about 14.8 full. */
    fun moonAge(t: Long): Double = (((t - NEW_MOON) / DAY.toDouble()) % SYNODIC + SYNODIC) % SYNODIC

    private fun evening(t: Long) = hour(t).let { it >= 20 || it < 4 }

    /** Full moons with music in the evening or at night (the moth), each moon once. */
    val fullMoons: Int by lazy {
        plays.filter { it.ms >= MIN && evening(it.at) && moonAge(it.at) in 13.8..15.8 }
            .map { Math.floorDiv(it.at - NEW_MOON, (SYNODIC * DAY).toLong()) }.toSet().size
    }

    /** Each artist's returns after half a year and more without them (the phoenix): artist to the moments. */
    val risings: Map<String, List<Long>> by lazy {
        val last = HashMap<String, Long>(); val m = HashMap<String, MutableList<Long>>()
        for (p in plays) { last[p.artist]?.let { if (p.at - it >= 180 * DAY) m.getOrPut(p.artist) { mutableListOf() } += p.at }; last[p.artist] = p.at }
        m
    }

    /** Pets grown up, the unicorn itself left out. */
    val grownPets: Int get() = pets.count { it.stage >= Stage.ADULT && it.species != Species.UNICORN }

    /** Different songs under two minutes heard to the end (the squirrel). */
    val shortSongs: Int by lazy { plays.filter { it.whole && it.length in 1 until SHORT_SONG }.map { it.track }.toSet().size }

    /** The days each album was heard on (the sloth). */
    private val albumDays: Map<String, Int> by lazy {
        val m = HashMap<String, HashSet<LocalDate>>()
        for (p in plays) if (p.album.isNotEmpty() && p.ms >= MIN) m.getOrPut(p.album) { HashSet() } += day(p.at)
        m.mapValues { it.value.size }
    }
    val mostAlbumDays: Int get() = albumDays.values.maxOrNull() ?: 0

    /** The decade heard most (the chameleon likes the others). */
    private val topDecade: Int? by lazy {
        val m = HashMap<Int, Long>()
        for (p in plays) yearOf(p)?.let { y -> m[y / 10 * 10] = (m[y / 10 * 10] ?: 0L) + p.ms }
        m.maxByOrNull { it.value }?.key
    }

    /** Sittings (as [sessions]) and how many artists each had (the octopus). */
    private val mixes: List<Pair<LongRange, Int>> by lazy {
        buildList {
            var from = 0L; var to = 0L; val who = HashSet<String>()
            for (p in plays) {
                if (who.isNotEmpty() && p.at - to > 6 * MIN) { add(from..to to who.size); who.clear() }
                if (who.isEmpty()) from = p.at
                to = max(to, p.at + max(p.ms, 1)); who += p.artist
            }
            if (who.isNotEmpty()) add(from..to to who.size)
        }
    }
    val bigMixes: Int get() = mixes.count { it.second >= 8 }

    /** How many artists the sitting a moment falls in had. */
    private fun artistsAround(t: Long): Int {
        val i = mixes.binarySearch { it.first.first.compareTo(t) }.let { if (it >= 0) it else -it - 2 }
        return mixes.getOrNull(i)?.takeIf { t in it.first }?.second ?: 0
    }

    // ---------------------------------------------------------------- eggs

    /** How near each egg is to being found; the three starters are always there. */
    val hints: List<Hint> by lazy {
        Species.known.map { sp ->
            // The demo shows the whole bestiary open.
            if (app.songling.BuildConfig.DEMO) Hint(sp, 1, 1) else when (sp) {
                Species.LYREBIRD, Species.MUSICBOX, Species.LONGEAR -> Hint(sp, 1, 1)
                Species.BASILISK -> inHours(sp, genreMinutes(Genre.ROCK, Genre.METAL, Genre.PUNK, Genre.ALT), 10)
                Species.CONDUCTOR -> inHours(sp, genreMinutes(Genre.CLASSICAL, Genre.SOUNDTRACK), 5)
                Species.PURR -> inHours(sp, genreMinutes(Genre.JAZZ, Genre.BLUES, Genre.RNB), 5)
                Species.AXOLOTL -> inHours(sp, genreMinutes(Genre.ELECTRONIC, Genre.DANCE), 10)
                Species.RACCOON -> inHours(sp, genreMinutes(Genre.HIPHOP), 10)
                Species.WHALE -> inHours(sp, genreMinutes(Genre.AMBIENT), 5)
                Species.WANDERER -> Hint(sp, countries.size, 5)
                Species.CRICKET -> Hint(sp, nights, 5)
                Species.LARK -> Hint(sp, mornings, 5)
                Species.SNAIL -> Hint(sp, longSessions, 3)
                Species.PARROT -> Hint(sp, maxRepeats, 10)
                Species.TORTOISE -> Hint(sp, oldAlbums, 5)
                Species.ECHO -> Hint(sp, comebacks, 1)
                Species.FIREFLY -> Hint(sp, concerts, 1)
                Species.POTPOURRI -> Hint(sp, mostGenresInADay, 4)
                Species.RAVEN -> inHours(sp, goneMinutes, 3)
                Species.HUMMINGBIRD -> inHours(sp, genreMinutes(Genre.POP), 10)
                Species.MAGPIE -> Hint(sp, collabTracks, 20)
                Species.BULLFINCH -> Hint(sp, winterDays, 10)
                Species.BAT -> Hint(sp, offPhone, 5)
                Species.DRAGON -> Hint(sp, longSongs, 10)
                Species.HOUND -> inHours(sp, topArtistMinutes, 20)
                Species.FOX -> Hint(sp, rated, 15)
                Species.PENGUIN -> Hint(sp, wishesHeard, 5)
                Species.MOTH -> Hint(sp, fullMoons, 3)
                Species.PHOENIX -> Hint(sp, risings.size, 5)
                Species.UNICORN -> Hint(sp, grownPets, 5)
                Species.SQUIRREL -> Hint(sp, shortSongs, 30)
                Species.BEE -> Hint(sp, bestStreak, 14)
                Species.SLOTH -> Hint(sp, mostAlbumDays, 7)
                Species.CHAMELEON -> Hint(sp, decades.size, 6)
                Species.OCTOPUS -> Hint(sp, bigMixes, 3)
                Species.HERMIT -> inHours(sp, disbandedMinutes, 5)
            }
        }
    }

    /** An egg asking for [hours] of something, counted in minutes so it fills with every song, not once an hour. */
    private fun inHours(sp: Species, minutes: Int, hours: Int) = Hint(sp, minutes, hours * 60, minutes = true)

    fun open(sp: Species) = hints.firstOrNull { it.species == sp }?.open == true

    // ---------------------------------------------------------------- pets

    private fun spans(p: Pet): List<LongRange> = p.spans.split(',').mapNotNull { r ->
        val a = r.split('-'); if (a.size != 2) return@mapNotNull null
        val from = a[0].toLongOrNull() ?: return@mapNotNull null
        val to = a[1].toLongOrNull()?.takeIf { it > 0 } ?: Long.MAX_VALUE
        from..to
    }

    /**
     * How much of a listen is a pet's favourite: all of it, or half when its genre is only one of the extra ones the
     * listen was given beside the main one (#23).
     */
    fun favourite(sp: Species, p: Play, g: List<Genre>): Double = when {
        sp.genres.isEmpty() -> if (likes(sp, p)) 1.0 else 0.0
        g.firstOrNull()?.let { it in sp.genres } == true -> 1.0
        g.drop(1).any { it in sp.genres } -> 0.5
        else -> 0.0
    }

    private fun likes(sp: Species, p: Play): Boolean = when (sp) {
        Species.LYREBIRD -> liveFirst[p.artist] == p.at
        Species.MUSICBOX -> wholeRuns.any { it.first == p.album && it.second <= p.at && p.at - it.second < 3 * 3_600_000L }
        Species.LONGEAR -> p.headphones
        Species.CRICKET -> hour(p.at).let { it >= 22 || it < 4 }
        Species.LARK -> hour(p.at) in 5..9
        Species.SNAIL -> (sessionAt(p.at)?.ms ?: 0) >= 45 * MIN
        Species.PARROT -> (byTrack[p.track]?.plays ?: 0) >= 5
        Species.TORTOISE -> (yearOf(p) ?: 9999) <= day(p.at).year - 30
        Species.ECHO -> returns.floor(p.at)?.let { p.at - it < DAY } == true
        Species.FIREFLY -> p.source == Source.CONCERT
        Species.POTPOURRI -> (genresByDay[day(p.at)] ?: 0) >= 4
        Species.RAVEN -> (artists[p.artist]?.gone ?: 0) > 0
        Species.MAGPIE -> Feats.isCollab(p.artistName, p.title)
        Species.BULLFINCH -> day(p.at).monthValue.let { it == 12 || it <= 2 } || Feats.isFestive(p.title, p.albumName)
        Species.BAT -> p.source == Source.VINYL || p.source == Source.RADIO
        Species.DRAGON -> p.length >= LONG_SONG
        Species.HOUND -> p.artist == topArtist
        Species.FOX -> starsOf(p) >= 4
        Species.PENGUIN -> wasWish(p)
        Species.MOTH -> evening(p.at) && moonAge(p.at) in 11.5..18.0
        Species.PHOENIX -> risings[p.artist]?.any { p.at >= it && p.at - it < DAY } == true
        Species.UNICORN -> byTrack[p.track]?.first == p.at
        Species.SQUIRREL -> p.length in 1 until SHORT_SONG
        Species.BEE -> day(p.at).let { it.minusDays(1) in musicDays && it.minusDays(2) in musicDays }
        Species.SLOTH -> (albumDays[p.album] ?: 0) >= 3
        Species.CHAMELEON -> yearOf(p)?.let { it / 10 * 10 != topDecade } == true
        Species.OCTOPUS -> artistsAround(p.at) >= 5
        Species.HERMIT -> disbanded(p.artist)
        else -> false
    }

    /**
     * When each artist was first heard live, imports left out: an imported history from the past mustn't make an
     * artist the pet already ate as new «not new» after the fact (the lyrebird, a curious pet).
     */
    private val liveFirst: Map<String, Long> by lazy {
        val m = HashMap<String, Long>()
        for (p in plays) if (p.source != Source.IMPORT && p.artist !in m) m[p.artist] = p.at
        m
    }

    /** Pets eat only what they heard: an imported history feeds nobody, though it stays in the statistics and the awards. */
    val pets: List<PetState> = petList.map { pet ->
        val ranges = spans(pet)
        var ms = 0L; var fav = 0L; var found = 0
        val g = HashMap<Genre, Long>()
        for (p in plays) if (p.source != Source.IMPORT && ranges.any { p.at in it }) {
            ms += p.ms
            if (liveFirst[p.artist] == p.at) found++
            val gg = genresOf(p)
            for (x in gg) g[x] = (g[x] ?: 0L) + p.ms
            fav += (p.ms * favourite(pet.species, p, gg)).toLong()
        }
        // A favourite listen counts half again; a greedy one eats a tenth more of everything.
        val greedy = if (pet.nature == Nature.GREEDY) 1.1 else 1.0
        val fed = (((ms + fav / 2) * greedy) / MIN).toInt() + pet.extra
        val stage = Stage.entries.last { fed >= it.from }
        PetState(pet, fed, stage, g, fav, found, ms)
    }

    val pet: PetState? get() = pets.firstOrNull { it.pet.id == s.pet } ?: pets.lastOrNull { it.out }

    /** 0 wide awake .. 1 wound down: days without music slowly stop the pet. */
    val windup: Float get() {
        val last = lastPlay
        if (last == 0L) return 0f
        val days = (now - last) / DAY.toFloat()
        return ((days - 2f) / 5f).coerceIn(0f, 1f)
    }

    val sleeping: Boolean get() = hour(now).let { h -> h in 1..6 } && now - lastPlay > 30 * MIN

    /** The genre of the last listen, if it was in the last half hour. */
    val recentGenre: Genre? get() = plays.lastOrNull()?.takeIf { now - (it.at + it.ms) < 30 * MIN }?.let { genreOf(it) }

    val mood: Mood get() {
        val g = recentGenre ?: return if (windup > 0.3f) Mood.SLEEPY else Mood.NONE
        val sp = pet?.species
        if (sp != null && g in sp.genres) return Mood.HEARTS
        return when (g.form) {
            Form.STORM -> Mood.HEADBANG
            Form.NEON -> Mood.SPARKLE
            Form.VELVET -> Mood.SWAY
            Form.CHOIR -> Mood.DREAMY
            Form.STREET -> Mood.PROUD
            Form.ROOTS -> Mood.SWAY
        }
    }

    // ---------------------------------------------------------------- earworm

    /** Today's song on repeat: the most played today, three times or more. */
    val earworm: Tally? get() {
        val m = HashMap<String, Int>()
        for (p in plays.asReversed()) { if (day(p.at) != today) break; m[p.track] = (m[p.track] ?: 0) + 1 }
        val best = m.maxByOrNull { it.value } ?: return null
        return if (best.value >= 3) byTrack[best.key] else null
    }

    /** A song once loved and long unheard: offered when the earworm is unstuck. */
    val forgotten: Tally? get() = byTrack.values.filter { it.plays >= 5 && now - it.last > 90 * DAY }.maxByOrNull { it.plays }

    // ---------------------------------------------------------------- week and quests

    val weekStart: Long = startOf(today.minusDays((today.dayOfWeek.value - 1).toLong()))
    val weekId: String = "${today.get(IsoFields.WEEK_BASED_YEAR)}-${today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}"
    val quests: List<QuestState> by lazy { Quests.of(this) }

    // ---------------------------------------------------------------- plectrums

    /** Whole albums (with the time of the run) heard by the player, not imported. */
    private val liveWhole: Int get() = wholeRuns.count { (k, t) -> plays.any { it.album == k && it.at >= t && it.at < t + 3_600_000L && it.source != Source.IMPORT } }

    /** Achievements and how far each one got. */
    val feats: List<FeatState> by lazy { Feats.of(this) }
    val featSteps: Int by lazy { feats.sumOf { it.level } }

    val goalsDone: Int by lazy { goals.count { Goals.progress(this, it).done && it.period == app.songling.data.Period.ONCE } }

    // A Studio is one moment's snapshot, so what's earned is worked out once: the shop asks on every tile.
    val earned: Int by lazy {
        var live = 0L; var imported = 0L
        for (p in plays) if (p.source == Source.IMPORT) imported += p.ms else live += p.ms
        val questPlectrums = quests.filter { "$weekId:${it.q.name}" in s.claimed }.sumOf { it.q.reward } +
            s.claimed.count { !it.startsWith("$weekId:") } * 15
        (live / (10 * MIN)).toInt() + (imported / (100 * MIN)).toInt() + liveWhole * 5 + questPlectrums + goalsDone * 15 + pets.sumOf { it.bonus } + featSteps * Feats.REWARD
    }

    val plectrums: Int by lazy { max(0, earned - s.spent) }

    /** What a record of [album] costs now: dearer with each one in the collection, half for an album heard whole. */
    fun recordPrice(album: String): Int = Records.price(s.records.size, (byAlbum[album]?.whole ?: 0) > 0)

    /** Albums heard and not in the record collection yet, the most listened first. */
    val recordsToBuy: List<String> by lazy { byAlbum.values.filter { it.key !in s.records && it.key.isNotEmpty() }.sortedByDescending { it.ms }.map { it.key } }

    // ---------------------------------------------------------------- shelf helpers

    fun artistName(key: String) = artists[key]?.name ?: byArtist[key]?.name ?: key
    fun albumTitle(key: String) = albums[key]?.title ?: byAlbum[key]?.name ?: key.substringAfter('|')
    fun trackTitle(key: String) = tracks[key]?.title ?: byTrack[key]?.name ?: key.substringAfter('|')

    /** Every album, EP and single on the shelf: heard, or put there by hand. */
    val shelfAlbums: List<String> get() = (byAlbum.keys + albums.keys).toList()
    val shelfTracks: List<String> get() = (byTrack.keys + tracks.keys).toList()
    val shelfArtists: List<String> get() = (byArtist.keys + artists.keys.filter { artists[it]?.wish == true || artists[it]?.added != 0L }).toList()

    companion object {
        /** A long song, for the little dragon. */
        const val LONG_SONG = 8 * MIN
        /** A short song, for the squirrel. */
        const val SHORT_SONG = 2 * MIN
        /** A new moon (6 January 2000, 18:14 UTC) and the moon's month in days, for the moth. */
        const val NEW_MOON = 947_182_440_000L
        const val SYNODIC = 29.530588853
    }

    fun wishes(): Int = albums.values.count { it.wish } + tracks.values.count { it.wish } + artists.values.count { it.wish }
}
