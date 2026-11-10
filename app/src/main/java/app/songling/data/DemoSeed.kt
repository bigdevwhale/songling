package app.songling.data

import android.content.Context
import app.songling.AppContainer
import app.songling.domain.Form
import app.songling.domain.Genre
import app.songling.domain.Nature
import app.songling.domain.Species
import app.songling.domain.genres
import kotlin.random.Random

/** A den four months into listening, for the demo build and screenshots. */
object DemoSeed {
    private class A(val artist: String, val title: String, val year: Int, val genre: Genre, val country: String, val songs: List<String>, val weight: Int)

    private val ALBUMS = listOf(
        A("Radiohead", "OK Computer", 1997, Genre.ALT, "GB", listOf("Airbag", "Paranoid Android", "Subterranean Homesick Alien", "Exit Music (For a Film)", "Let Down", "Karma Police", "No Surprises", "Lucky"), 9),
        A("Miles Davis", "Kind of Blue", 1959, Genre.JAZZ, "US", listOf("So What", "Freddie Freeloader", "Blue in Green", "All Blues", "Flamenco Sketches"), 8),
        A("Daft Punk", "Discovery", 2001, Genre.DANCE, "FR", listOf("One More Time", "Aerodynamic", "Digital Love", "Harder, Better, Faster, Stronger", "Something About Us", "Face to Face"), 6),
        A("Кино", "Группа крови", 1988, Genre.ROCK, "RU", listOf("Группа крови", "Закрой за мной дверь, я ухожу", "Война", "Спокойная ночь", "Мама, мы все тяжело больны", "Бошетунмай"), 7),
        A("Massive Attack", "Mezzanine", 1998, Genre.ELECTRONIC, "GB", listOf("Angel", "Risingson", "Teardrop", "Inertia Creeps", "Exchange", "Black Milk"), 6),
        A("Fleetwood Mac", "Rumours", 1977, Genre.ROCK, "GB", listOf("Second Hand News", "Dreams", "Never Going Back Again", "Don't Stop", "Go Your Own Way", "The Chain"), 5),
        A("Brian Eno", "Ambient 1: Music for Airports", 1978, Genre.AMBIENT, "GB", listOf("1/1", "2/1", "1/2", "2/2"), 4),
        A("Kendrick Lamar", "To Pimp a Butterfly", 2015, Genre.HIPHOP, "US", listOf("Wesley's Theory", "King Kunta", "Alright", "u", "These Walls", "The Blacker the Berry"), 4),
        A("Земфира", "Земфира", 1999, Genre.ROCK, "RU", listOf("Снег", "Ариведерчи", "СПИД", "Ракеты", "Почему", "Румба"), 5),
        A("Glenn Gould", "Bach: The Goldberg Variations", 1981, Genre.CLASSICAL, "CA", listOf("Aria", "Variatio 1", "Variatio 2", "Variatio 3", "Variatio 4", "Aria da capo"), 3),
        A("Bon Iver", "For Emma, Forever Ago", 2007, Genre.FOLK, "US", listOf("Flume", "Lump Sum", "Skinny Love", "The Wolves", "Blindsided", "Re: Stacks"), 4),
        A("Metallica", "Master of Puppets", 1986, Genre.METAL, "US", listOf("Battery", "Master of Puppets", "The Thing That Should Not Be", "Welcome Home (Sanitarium)", "Orion"), 3),
        A("Hans Zimmer", "Interstellar", 2014, Genre.SOUNDTRACK, "DE", listOf("Dreaming of the Crash", "Cornfield Chase", "Mountains", "No Time for Caution", "Stay"), 3),
        A("Björk", "Homogenic", 1997, Genre.ELECTRONIC, "IS", listOf("Hunter", "Jóga", "Bachelorette", "All Is Full of Love", "Pluto"), 3),
        A("Buena Vista Social Club", "Buena Vista Social Club", 1997, Genre.LATIN, "CU", listOf("Chan Chan", "De camino a la vereda", "El cuarto de Tula", "Dos gardenias"), 2),
    )

    suspend fun seedIfEmpty(ctx: Context, c: AppContainer) {
        val dao = c.db.dao()
        if (dao.playCount() > 0) { covers(ctx, dao); return }
        val rnd = Random(7)
        val now = System.currentTimeMillis()
        val ru = app.songling.BuildConfig.DEMO_LANG.ifEmpty { ctx.resources.configuration.locales[0].language } == "ru"
        val start = now - 120 * DAY
        val plays = ArrayList<Play>()
        val pool = ALBUMS.flatMap { a -> List(a.weight) { a } }
        var day = start
        while (day < now - 2 * 3_600_000L) {
            val sessions = when (rnd.nextInt(10)) { 0 -> 0; in 1..5 -> 1; in 6..8 -> 2; else -> 3 }
            for (s in 0 until sessions) {
                val hour = listOf(8, 13, 19, 21, 23).random(rnd)
                var t = day - day % DAY + hour * 3_600_000L + rnd.nextLong(0, 3_000_000)
                if (t > now - 600_000) continue
                val a = pool.random(rnd)
                val whole = rnd.nextInt(3) != 0
                val songs = if (whole) a.songs else a.songs.shuffled(rnd).take(rnd.nextInt(1, 3))
                val headphones = rnd.nextBoolean()
                for (song in songs) {
                    val len = rnd.nextLong(160_000, 330_000)
                    val skip = !whole && rnd.nextInt(6) == 0
                    val ms = if (skip) rnd.nextLong(35_000, 70_000) else len
                    plays += Play(at = t, ms = ms, title = song, artistName = a.artist, albumName = a.title, length = len, app = "com.spotify.music", whole = !skip, skipped = skip, headphones = headphones)
                    t += ms + 2_000
                }
            }
            day += DAY
        }
        // Today: an earworm and the song still on.
        repeat(4) { i -> plays += Play(at = now - (5 - i) * 600_000L, ms = 250_000, title = "Teardrop", artistName = "Massive Attack", albumName = "Mezzanine", length = 330_000, app = "com.spotify.music", whole = true) }
        plays += Play(at = now - 70 * DAY, ms = 90 * MIN, title = (if (ru) "Концерт · Клуб «Космонавт»" else "Concert · Electric Ballroom"), artistName = "Kino", source = Source.CONCERT)
        plays.sortBy { it.at }
        dao.insertPlays(plays)
        for (a in ALBUMS) {
            val key = Keys.album(a.artist, a.title)
            dao.upsert(Album(key, a.title, a.artist, year = a.year, genre = a.genre, genreName = a.genre.name, tracks = a.songs.size, checked = now, stars = if (a.weight > 6) 5 else if (a.weight > 3) 4 else 0,
                own = if (a.genre == Genre.AMBIENT || a.genre == Genre.CLASSICAL) "work" else ""))
            dao.upsert(Artist(Keys.artist(a.artist), a.artist, a.country, checked = now))
            a.songs.forEachIndexed { i, s -> dao.upsert(Track(Keys.track(a.artist, s), s, a.artist, album = key, albumName = a.title, number = i + 1, year = a.year, genre = a.genre, checked = now, loved = i == 1)) }
        }
        dao.upsert(Artist(Keys.artist("Kino"), "Kino", "RU", Genre.ROCK, checked = now))
        // On the «to hear» list.
        dao.upsert(Album(Keys.album("Joni Mitchell", "Blue"), "Blue", "Joni Mitchell", year = 1971, genre = Genre.FOLK, tracks = 10, wish = true, added = now - 5 * DAY, checked = now))
        dao.upsert(Album(Keys.album("Talking Heads", "Remain in Light"), "Remain in Light", "Talking Heads", year = 1980, genre = Genre.ROCK, tracks = 8, wish = true, added = now - 9 * DAY, checked = now))
        dao.upsert(Track(Keys.track("Nina Simone", "Feeling Good"), "Feeling Good", "Nina Simone", year = 1965, genre = Genre.JAZZ, wish = true, added = now - 2 * DAY, checked = now))
        // Two pets: a lyrebird raised first, then the jazz cat out now.
        dao.insert(Pet(species = Species.LYREBIRD, name = if (ru) "Лирик" else "Lyric", laid = start, nature = Nature.CURIOUS, form = Form.STORM, spans = "$start-${now - 45 * DAY}"))
        // Every other pet, raised earlier and waiting in its box, at every stage, two in the rare colouring.
        val rest = Species.entries.filter { it != Species.LYREBIRD && it != Species.PURR }
        rest.forEachIndexed { i, sp ->
            val laid = start - (i + 1) * 9 * DAY
            val minutes = listOf(400, 1_800, 5_000, 13_000)[i % 4]
            dao.insert(Pet(species = sp, laid = laid, shiny = i == 3 || i == 11, nature = Nature.entries[i % Nature.entries.size],
                form = if (minutes >= 4_000) sp.genres.firstOrNull()?.form ?: Form.entries[i % Form.entries.size] else null,
                spans = "$laid-${laid + 8 * DAY}", extra = minutes))
        }
        val cat = dao.insert(Pet(species = Species.PURR, name = if (ru) "Майлз" else "Miles", laid = now - 45 * DAY, nature = Nature.DANCER, spans = "${now - 45 * DAY}-0", extra = 4_200))
        dao.insert(Goal(kind = GoalKind.ALBUMS, target = 6, period = Period.MONTH, created = now - 40 * DAY))
        dao.insert(Goal(kind = GoalKind.ARTIST, target = 10 * 60, period = Period.YEAR, ref = Keys.artist("Miles Davis"), label = "Miles Davis", created = now - 60 * DAY))
        dao.insert(Goal(kind = GoalKind.COUNTRIES, target = 10, period = Period.YEAR, created = now - 60 * DAY))
        dao.insert(Goal(kind = GoalKind.WISHES, target = 3, period = Period.MONTH, created = now - 10 * DAY))
        covers(ctx, dao)
        c.settings.update {
            it.copy(onboarded = true, pet = cat, bought = setOf("SHELF", "TURNTABLE", "SPEAKERS", "GARLAND", "PLANT", "POSTER", "LAVA", "RUG", "NEON"), spent = 330,
                tips = setOf("DEN", "SHELF", "SAGA", "GOALS", "BESTIARY", "SHOP", "SETTINGS"),
                own = listOf(OwnGenre("work", if (ru) "Для работы" else "For work", 0xFF4C8AD8, Genre.AMBIENT.name)),
                seen = setOf(Species.LYREBIRD.name, Species.PURR.name))
        }
    }

    /** Real covers for the demo albums, from iTunes, once (they're kept with the rest). */
    private suspend fun covers(ctx: Context, dao: MusicDao) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        for (a in dao.albumList().filter { it.cover == null }) runCatching {
            val hit = Meta.searchAlbums("${a.artistName} ${a.title}", false).firstOrNull { Keys.norm(it.title).startsWith(Keys.norm(a.title).take(6)) }
                ?: Meta.searchAlbums("${a.artistName} ${a.title}", false).firstOrNull()
            val art = hit?.art ?: return@runCatching
            Covers.download(ctx, a.key, art)?.let { name -> dao.upsert(a.copy(cover = name)) }
        }
    }
}
