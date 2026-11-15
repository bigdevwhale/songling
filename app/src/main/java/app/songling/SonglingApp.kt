package app.songling

import android.app.Application
import android.content.Context
import app.songling.data.DemoSeed
import app.songling.data.LastFm
import app.songling.data.Meta
import app.songling.data.MusicDb
import app.songling.data.Outside
import app.songling.data.OutsideSource
import app.songling.data.Pet
import app.songling.data.SettingsStore
import app.songling.domain.Nature
import app.songling.domain.Species
import app.songling.domain.Studio
import app.songling.listen.Tracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

class AppContainer(context: Context) {
    val app: Context = context.applicationContext
    val db = MusicDb.create(app)
    val settings = SettingsStore(app)
    val meta = Meta(app, db.dao())
    val tracker = Tracker(app)
    /** On the main thread: Room and DataStore are main-safe, and screens' state is written from here. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** True while the demo build fills its den, so the first screen doesn't jump. */
    val seeding = MutableStateFlow(BuildConfig.DEMO)

    /** Fire-and-forget so a write survives the screen being closed right after tapping. */
    fun write(block: suspend AppContainer.() -> Unit) {
        scope.launch { block() }
    }

    private var looking: Job? = null

    /** Keys put right on start ([MusicDao.rekeyArtists]); lookups wait for it, so none writes a song under an old key. */
    var keysFixed: Job? = null

    /** Looks up whatever is new in the catalogues, in the background. */
    fun kick() {
        if (BuildConfig.DEMO || looking?.isActive == true) return
        looking = scope.launch(Dispatchers.IO) { keysFixed?.join(); runCatching { meta.drain() } }
    }

    private var scrobbling: Job? = null

    /** Reads what was scrobbled to Last.fm since last time ([LastFm]), when a username is set. */
    fun syncLastFm() {
        if (BuildConfig.DEMO || scrobbling?.isActive == true) return
        scrobbling = scope.launch(Dispatchers.IO) {
            val s = settings.now()
            if (s.lastfm.isBlank()) return@launch
            val r = runCatching { LastFm.sync(app, db.dao(), s.lastfm, s.lastfmFrom) }
            val now = System.currentTimeMillis()
            r.onSuccess { (added, newest) ->
                settings.update { if (it.lastfm == s.lastfm) it.copy(lastfmFrom = newest, lastfmAt = now, lastfmError = "") else it }
                if (added > 0) kick()
            }.onFailure { e -> settings.update { if (it.lastfm == s.lastfm) it.copy(lastfmAt = now, lastfmError = e.message ?: e.javaClass.simpleName) else it } }
        }
    }

    /** Last.fm every quarter of an hour while the app lives (the listener keeps it alive in the background). */
    fun followLastFm() {
        scope.launch { while (true) { syncLastFm(); kotlinx.coroutines.delay(15 * 60_000L) } }
    }

    private var weathering: Job? = null

    /** Brings the window's weather up to date (every half hour at most), guessing the town on first use. */
    fun refreshOutside(force: Boolean = false) {
        if (weathering?.isActive == true) return
        weathering = scope.launch(Dispatchers.IO) {
            runCatching {
                val s = settings.now()
                if (!s.realWeather) return@runCatching
                val ru = app.resources.configuration.locales[0].language == "ru"
                var lat = s.lat; var lon = s.lon
                if (lat.isNaN() || lon.isNaN()) {
                    val t = OutsideSource.guess(ru) ?: return@runCatching
                    lat = t.lat; lon = t.lon
                    settings.update { it.copy(town = t.name, lat = t.lat, lon = t.lon) }
                }
                val old = Outside.of(s.outside)
                if (!force && old != null && System.currentTimeMillis() - old.at < 30 * 60_000L) return@runCatching
                val now = OutsideSource.fetch(lat, lon)
                settings.update { it.copy(outside = now.json()) }
            }
        }
    }

    suspend fun studio(): Studio {
        val d = db.dao()
        return Studio(d.playList(), d.albums().first(), d.artists().first(), d.tracks().first(), d.petList(), d.goals().first(), settings.now(), System.currentTimeMillis())
    }

    /** Lays a new egg of [sp] and puts the pet that was out back in its box. */
    suspend fun layEgg(sp: Species, rnd: Random = Random) {
        val now = System.currentTimeMillis()
        putAway(now)
        val id = db.dao().insert(Pet(species = sp, laid = now, shiny = rnd.nextInt(16) == 0, nature = Nature.entries[rnd.nextInt(Nature.entries.size)]))
        // A new pet starts with a room of its own, empty (#26).
        settings.update { it.switchRoom(id).copy(seen = it.seen + sp.name) }
    }

    /** Brings a pet out of the box; the one out before goes in. */
    suspend fun bringOut(id: Long) {
        val now = System.currentTimeMillis()
        putAway(now)
        db.dao().petList().firstOrNull { it.id == id }?.let { p -> db.dao().update(p.copy(spans = p.spans + ",$now-0")) }
        settings.update { it.switchRoom(id) }
    }

    private suspend fun putAway(now: Long) {
        for (p in db.dao().petList()) if (p.spans.endsWith("-0")) db.dao().update(p.copy(spans = p.spans.dropLast(1) + now))
    }
}

class SonglingApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        if (BuildConfig.DEMO) container.scope.launch { try { DemoSeed.seedIfEmpty(this@SonglingApp, container) } finally { container.seeding.value = false } }
        else {
            // Listens clocked longer than their song by an older version (#10).
            container.scope.launch { runCatching { container.db.dao().trimOverlong() } }
            // The den shared by all pets before #26 becomes each pet's own room, once.
            container.scope.launch { runCatching { val ids = container.db.dao().petList().map { it.id }; container.settings.update { it.splitRooms(ids) } } }
            // «Король и Шут» read as two artists by an older version: keys put right before anything is looked up.
            container.keysFixed = container.scope.launch(kotlinx.coroutines.Dispatchers.IO) { runCatching { container.db.dao().rekeyArtists() } }
            container.kick()
            container.followLastFm()
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as SonglingApp).container

val Context.ru: Boolean get() = resources.configuration.locales[0].language == "ru"
