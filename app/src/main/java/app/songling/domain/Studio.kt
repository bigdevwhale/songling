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
data class Hint(val species: Species, val have: Int, val need: Int) {
    val open get() = have >= need
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
        return guess.firstOrNull { it != Genre.OTHER } ?: guess.firstOrNull()
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
            byArtist.getOrPut(p.artist) { Tally(p.artist).also { it.name = artists[p.artist]?.name ?: app.songling.data.Keys.mainArtist(p.artistName) } }.add(p)
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
            if (p.whole && p.source != Source.PLAYER && p.source != Source.IMPORT) r.marked = true
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

    /** Hours of music by artists who have passed away (#15). */
    val goneHours: Int by lazy { (plays.filter { (artists[it.artist]?.gone ?: 0) > 0 }.sumOf { it.ms } / 3_600_000L).toInt() }

    val oldAlbums: Int get() = byAlbum.values.count { t ->
        val y = albums[t.key]?.year ?: return@count false
        t.ms >= 20 * MIN && day(t.first).year - y >= 30
    }

    /** The most genres heard in one day. */
    val mostGenresInADay: Int by lazy {
        val m = HashMap<LocalDate, HashSet<Genre>>()
        for (p in plays) if (p.ms >= MIN) genresOf(p).let { if (it.isNotEmpty()) m.getOrPut(day(p.at)) { HashSet() } += it }
        m.values.maxOfOrNull { it.size } ?: 0
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
    val topArtistHours: Int get() = ((byArtist[topArtist]?.ms ?: 0L) / 3_600_000L).toInt()

    fun genreHours(vararg g: Genre): Int = (g.sumOf { byGenre[it] ?: 0L } / 3_600_000L).toInt()

    // ---------------------------------------------------------------- eggs

    /** How near each egg is to being found; the three starters are always there. */
    val hints: List<Hint> by lazy {
        Species.known.map { sp ->
            // The demo shows the whole bestiary open.
            if (app.songling.BuildConfig.DEMO) Hint(sp, 1, 1) else when (sp) {
                Species.LYREBIRD, Species.MUSICBOX, Species.LONGEAR -> Hint(sp, 1, 1)
                Species.BASILISK -> Hint(sp, genreHours(Genre.ROCK, Genre.METAL, Genre.PUNK, Genre.ALT), 10)
                Species.CONDUCTOR -> Hint(sp, genreHours(Genre.CLASSICAL, Genre.SOUNDTRACK), 5)
                Species.PURR -> Hint(sp, genreHours(Genre.JAZZ, Genre.BLUES, Genre.RNB), 5)
                Species.AXOLOTL -> Hint(sp, genreHours(Genre.ELECTRONIC, Genre.DANCE), 10)
                Species.RACCOON -> Hint(sp, genreHours(Genre.HIPHOP), 10)
                Species.WHALE -> Hint(sp, genreHours(Genre.AMBIENT), 5)
                Species.WANDERER -> Hint(sp, countries.size, 5)
                Species.CRICKET -> Hint(sp, nights, 5)
                Species.LARK -> Hint(sp, mornings, 5)
                Species.SNAIL -> Hint(sp, longSessions, 3)
                Species.PARROT -> Hint(sp, maxRepeats, 10)
                Species.TORTOISE -> Hint(sp, oldAlbums, 5)
                Species.ECHO -> Hint(sp, comebacks, 1)
                Species.FIREFLY -> Hint(sp, concerts, 1)
                Species.POTPOURRI -> Hint(sp, mostGenresInADay, 4)
                Species.RAVEN -> Hint(sp, goneHours, 3)
                Species.HUMMINGBIRD -> Hint(sp, genreHours(Genre.POP), 10)
                Species.MAGPIE -> Hint(sp, collabTracks, 20)
                Species.BULLFINCH -> Hint(sp, winterDays, 10)
                Species.BAT -> Hint(sp, offPhone, 5)
                Species.DRAGON -> Hint(sp, longSongs, 10)
                Species.HOUND -> Hint(sp, topArtistHours, 20)
            }
        }
    }

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
        Species.LYREBIRD -> byArtist[p.artist]?.first == p.at
        Species.MUSICBOX -> wholeRuns.any { it.first == p.album && it.second <= p.at && p.at - it.second < 3 * 3_600_000L }
        Species.LONGEAR -> p.headphones
        Species.CRICKET -> hour(p.at).let { it >= 22 || it < 4 }
        Species.LARK -> hour(p.at) in 5..9
        Species.SNAIL -> false
        Species.PARROT -> (byTrack[p.track]?.plays ?: 0) >= 5
        Species.TORTOISE -> (yearOf(p) ?: 9999) <= day(p.at).year - 30
        Species.ECHO -> false
        Species.FIREFLY -> p.source == Source.CONCERT
        Species.POTPOURRI -> false
        Species.RAVEN -> (artists[p.artist]?.gone ?: 0) > 0
        Species.MAGPIE -> Feats.isCollab(p.artistName, p.title)
        Species.BULLFINCH -> day(p.at).monthValue.let { it == 12 || it <= 2 } || Feats.isFestive(p.title, p.albumName)
        Species.BAT -> p.source == Source.VINYL || p.source == Source.RADIO
        Species.DRAGON -> p.length >= LONG_SONG
        Species.HOUND -> p.artist == topArtist
        else -> false
    }

    val pets: List<PetState> = petList.map { pet ->
        val ranges = spans(pet)
        var ms = 0L; var fav = 0L; var found = 0
        val g = HashMap<Genre, Long>()
        for (p in plays) if (ranges.any { p.at in it }) {
            ms += p.ms
            if (byArtist[p.artist]?.first == p.at) found++
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
    val featSteps: Int get() = feats.sumOf { it.level }

    val goalsDone: Int get() = goals.count { Goals.progress(this, it).done && it.period == app.songling.data.Period.ONCE }

    val earned: Int get() {
        var live = 0L; var imported = 0L
        for (p in plays) if (p.source == Source.IMPORT) imported += p.ms else live += p.ms
        val questPlectrums = quests.filter { "$weekId:${it.q.name}" in s.claimed }.sumOf { it.q.reward } +
            s.claimed.count { !it.startsWith("$weekId:") } * 15
        return (live / (10 * MIN)).toInt() + (imported / (100 * MIN)).toInt() + liveWhole * 5 + questPlectrums + goalsDone * 15 + pets.sumOf { it.bonus } + featSteps * Feats.REWARD
    }

    val plectrums: Int get() = max(0, earned - s.spent)

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
    }

    fun wishes(): Int = albums.values.count { it.wish } + tracks.values.count { it.wish } + artists.values.count { it.wish }
}
