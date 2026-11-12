package app.songling.listen

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationManagerCompat
import app.songling.container
import app.songling.data.Covers
import app.songling.data.Keys
import app.songling.data.MIN
import app.songling.data.Play
import app.songling.data.Settings
import app.songling.data.Source
import app.songling.data.hears
import app.songling.widget.PlayerWidget
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** What is playing right now, as the den shows it. */
data class NowPlaying(
    val title: String,
    val artist: String,
    val album: String,
    val app: String,
    val playing: Boolean,
    /** Played of this track so far. */
    val ms: Long,
    val length: Long,
    val cover: String?,
    val since: Long,
)

/**
 * Follows every player's media session and turns what it plays into [Play] rows. A listen is written
 * once it has played [Settings.minSeconds] and then grows while it plays; a new track, a long pause
 * or the session going away closes it.
 */
class Tracker(private val ctx: Context) {
    val now = MutableStateFlow<NowPlaying?>(null)

    private val main = Handler(Looper.getMainLooper())
    private var manager: MediaSessionManager? = null
    private val callbacks = HashMap<MediaController, MediaController.Callback>()

    /** The track being followed. */
    private class Cur(
        val pkg: String, val title: String, val artist: String, val album: String, val length: Long,
        val started: Long, var playedMs: Long = 0, var since: Long = 0, var id: Long = 0, var lastPos: Long = 0,
        var art: Bitmap? = null, var cover: String? = null, var writing: Boolean = false, var covering: Boolean = false,
    )

    private var cur: Cur? = null
    /** The settings as last seen, so players left out are ignored right away. */
    @Volatile private var rules = Settings()
    private var watching: Job? = null
    private var active: MediaController? = null
    private var listener: ComponentName? = null

    fun enabled(): Boolean = NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)

    private val sessions = MediaSessionManager.OnActiveSessionsChangedListener { list -> main.post { follow(list.orEmpty()) } }

    /** Whether the players can be heard now: the listener is bound and the sessions came through. */
    val connected = MutableStateFlow(false)

    fun start(listener: ComponentName) {
        if (manager != null) return
        val m = ctx.getSystemService(MediaSessionManager::class.java) ?: return
        // Refused (the system not ready yet, or access just taken away): stay stopped, so the next connect tries again
        // instead of being taken for one already done (#7).
        val ok = runCatching { m.addOnActiveSessionsChangedListener(sessions, listener) }.isSuccess
        if (!ok) return
        manager = m
        this.listener = listener
        connected.value = true
        runCatching { follow(m.getActiveSessions(listener)) }
        main.removeCallbacks(tickRun)
        main.postDelayed(tickRun, 15_000)
        watching = ctx.container.scope.launch {
            ctx.container.settings.settings.collect { s ->
                val was = rules
                rules = s
                if (was.only != s.only || was.tracked != s.tracked || was.excluded != s.excluded) main.post { recheck() }
            }
        }
    }

    /** After the choice of players changed: drops a player no longer heard and picks up one now heard. */
    private fun recheck() {
        if (cur?.let { rules.hears(it.pkg) } == false) { pause(); close() }
        for (c in callbacks.keys.toList()) changed(c)
    }

    fun stop() {
        connected.value = false
        watching?.cancel(); watching = null
        runCatching { manager?.removeOnActiveSessionsChangedListener(sessions) }
        manager = null
        callbacks.forEach { (c, cb) -> c.unregisterCallback(cb) }
        callbacks.clear()
        close()
        main.removeCallbacks(tickRun)
    }

    private val tickRun = object : Runnable {
        override fun run() {
            // A missed callback mustn't cost or add time: ask the players themselves what they play now. Without this
            // a missed track change folds the next songs into one listen, which is then cut to the first song's length.
            val m = manager; val l = listener
            if (m != null && l != null) runCatching { m.getActiveSessions(l) }.onSuccess(::follow).onFailure {
                // The system stopped answering us without saying so: start over and ask to be bound again (#7).
                if (it is SecurityException) { stop(); Listener.rebind(ctx); return }
            }
            // The player followed first; another one only when it has gone quiet, so two playing at once don't take turns.
            active?.let(::changed)
            if (active?.let(::isPlaying) != true) for (c in callbacks.keys.toList()) if (c.sessionToken != active?.sessionToken) changed(c)
            tick(); if (cur == null) PlayerWidget.refresh(ctx); main.postDelayed(this, 15_000)
        }
    }

    private fun follow(list: List<MediaController>) {
        val gone = callbacks.keys.filter { c -> list.none { it.sessionToken == c.sessionToken } }
        for (c in gone) { callbacks.remove(c)?.let(c::unregisterCallback); if (active?.sessionToken == c.sessionToken) { pause(); close() } }
        for (c in list) if (callbacks.keys.none { it.sessionToken == c.sessionToken }) {
            val cb = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) = changed(c)
                override fun onMetadataChanged(metadata: MediaMetadata?) = changed(c)
                override fun onSessionDestroyed() { if (active?.sessionToken == c.sessionToken) { pause(); close() } }
            }
            c.registerCallback(cb, main)
            callbacks[c] = cb
            changed(c)
        }
    }

    private fun isPlaying(c: MediaController) = c.playbackState?.state == PlaybackState.STATE_PLAYING

    private fun changed(c: MediaController) {
        // A player left out isn't followed at all: it neither counts nor makes the pet dance.
        if (!rules.hears(c.packageName)) { if (active?.sessionToken == c.sessionToken) { pause(); close() }; return }
        val playing = isPlaying(c)
        // Another player taking over closes what was playing before.
        if (playing && active != null && active?.sessionToken != c.sessionToken) { pause(); close() }
        if (!playing && active?.sessionToken != c.sessionToken) return
        val md = c.metadata
        val title = md?.getString(MediaMetadata.METADATA_KEY_TITLE)?.trim().orEmpty()
        val artist = (md?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST))?.trim().orEmpty()
        val album = md?.getString(MediaMetadata.METADATA_KEY_ALBUM)?.trim().orEmpty()
        val length = md?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        if (title.isEmpty() || artist.isEmpty()) { if (!playing) pause(); return }
        val c0 = cur
        val same = c0 != null && c0.pkg == c.packageName && c0.title == title && c0.artist == artist
        // The same song started over counts as a new listen.
        val restarted = same && played(c0!!) > 30_000 && pos(c) < 3_000 && c0.lastPos > 10_000
        if (!same || restarted) {
            pause(); close()
            cur = Cur(c.packageName, title, artist, album, length, System.currentTimeMillis())
        }
        active = c
        val k = cur!!
        if (k.art == null) k.art = md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        if (k.cover == null && !k.covering) showCover(k, md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI) ?: md?.getString(MediaMetadata.METADATA_KEY_ART_URI))
        k.lastPos = if (c.playbackState != null) pos(c) else k.lastPos
        if (playing && k.since == 0L) k.since = System.currentTimeMillis()
        if (!playing) pause(stoppedAt(c))
        publish()
        tick()
    }

    /**
     * When the player says it stopped, on our clock. A callback can come late (the app was asleep, the
     * listener restarted), and the time in between wasn't music.
     */
    private fun stoppedAt(c: MediaController): Long {
        val now = System.currentTimeMillis()
        val upd = c.playbackState?.lastPositionUpdateTime ?: return now
        if (upd <= 0) return now
        return now - (android.os.SystemClock.elapsedRealtime() - upd).coerceAtLeast(0)
    }

    /**
     * Shows the player's own cover right away, while the song is still playing: the picture it gave, or the one
     * at the address it gave. It's kept under the album's key, so the shelf gets it too if the catalogue has none.
     */
    private fun showCover(k: Cur, uri: String?) {
        val art = k.art
        if (art == null && uri.isNullOrBlank()) return
        k.covering = true
        val key = Keys.album(k.artist, k.album.ifEmpty { k.title })
        ctx.container.scope.launch {
            val name = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                when {
                    art != null -> Covers.keep(ctx, key, art)
                    uri!!.startsWith("http") -> Covers.download(ctx, key, uri)
                    else -> runCatching {
                        ctx.contentResolver.openInputStream(android.net.Uri.parse(uri))?.use { android.graphics.BitmapFactory.decodeStream(it) }
                    }.getOrNull()?.let { b -> k.art = b; Covers.keep(ctx, key, b) }
                }
            }
            // A failed address isn't tried again for this song; a picture given later still is.
            if (name == null && art != null) k.covering = false
            if (name != null && cur === k) { if (k.cover == null) k.cover = name; publish() }
        }
    }

    /** Stops the clock on the current track, at [at] (now, or when the player said it stopped). */
    private fun pause(at: Long = System.currentTimeMillis()) {
        val k = cur ?: return
        // A stop stamped before our clock started is the player's stale stamp, not a stop: count up to now.
        if (k.since > 0) { val end = if (at < k.since) System.currentTimeMillis() else at; k.playedMs += end - k.since; k.since = 0 }
        publish()
    }

    /** Played of the song so far; one listen is never longer than the song (a repeat is a new listen). */
    private fun played(k: Cur): Long {
        val ms = k.playedMs + if (k.since > 0) System.currentTimeMillis() - k.since else 0
        return if (k.length > 0) ms.coerceAtMost(k.length) else ms
    }

    private fun publish() {
        val k = cur
        now.value = k?.let { NowPlaying(it.title, it.artist, it.album, it.pkg, it.since > 0, played(it), it.length, it.cover, it.started) }
        PlayerWidget.refresh(ctx)
    }

    /** The cover the player itself gave for the song now, if any. */
    fun art(): Bitmap? = cur?.art

    /** Where the player is in the song now. */
    fun position(): Long = active?.let(::pos) ?: 0

    /** Where [c] is in its song now: the position it last gave, moved on by the time it has played since. */
    private fun pos(c: MediaController): Long {
        val st = c.playbackState ?: return 0
        if (st.state != PlaybackState.STATE_PLAYING) return st.position
        return st.position + ((android.os.SystemClock.elapsedRealtime() - st.lastPositionUpdateTime) * st.playbackSpeed).toLong()
    }

    /** The widget's buttons: sent to the player followed now, or else to the first player heard. */
    fun control(action: String) {
        val c = active ?: run {
            val m = ctx.getSystemService(MediaSessionManager::class.java)
            runCatching { m?.getActiveSessions(ComponentName(ctx, Listener::class.java)) }.getOrNull().orEmpty().firstOrNull { rules.hears(it.packageName) }
        } ?: return
        val t = c.transportControls
        when (action) {
            PlayerWidget.PREV -> t.skipToPrevious()
            PlayerWidget.NEXT -> t.skipToNext()
            else -> if (isPlaying(c)) t.pause() else t.play()
        }
    }

    /** Writes or grows the row for the current track. */
    private fun tick() {
        val k = cur ?: return
        val ms = played(k)
        val c = ctx.container
        if (k.id == 0L && k.writing) return
        if (k.id == 0L) k.writing = true
        c.scope.launch {
            val s = c.settings.now()
            if (!s.hears(k.pkg) || (s.skipLong && k.length > s.longMinutes * MIN) || ms < s.minSeconds * 1000L) { k.writing = false; return@launch }
            val whole = k.length > 0 && ms >= k.length * 0.8
            if (k.id == 0L) {
                val p = Play(at = k.started, ms = ms, title = k.title, artistName = k.artist, albumName = k.album, length = k.length,
                    source = Source.PLAYER, app = k.pkg, whole = whole, headphones = headphones())
                k.id = c.db.dao().insert(p)
                // Closed while it was being written: give it its final length now.
                if (cur !== k) { val end = played(k); c.db.dao().grow(k.id, end, k.length > 0 && end >= k.length * 0.8, k.length > 60_000 && end < k.length * 0.5) }
                c.meta.note(p)
                // The player's own cover, when the catalogue has none yet.
                val art = k.art
                if (art != null && p.album.isNotEmpty()) {
                    val a = c.db.dao().album(p.album)
                    if (a != null && a.cover == null) Covers.keep(ctx, p.album, art)?.let { name -> c.db.dao().upsert(a.copy(cover = name)); k.cover = name }
                }
                c.kick()
            } else if (k.id > 0) c.db.dao().grow(k.id, ms, whole, false)
            if (k.cover == null && k.album.isNotEmpty()) k.cover = c.db.dao().album(Keys.album(k.artist, k.album))?.cover
            publish()
        }
    }

    /** Closes the current track: marks a skip if it stopped well before the end. */
    private fun close() {
        val k = cur ?: return
        cur = null
        val ms = played(k)
        val id = k.id
        if (id > 0) {
            val skipped = k.length > 60_000 && ms < k.length * 0.5
            val whole = k.length > 0 && ms >= k.length * 0.8
            ctx.container.scope.launch { ctx.container.db.dao().grow(id, ms, whole, skipped) }
        }
        now.value = null
        active = null
    }

    private fun headphones(): Boolean = runCatching {
        val am = ctx.getSystemService(AudioManager::class.java)
        am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {
            it.type in setOf(AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_USB_HEADSET, 26 /* BLE headset */, 27 /* BLE speaker */)
        }
    }.getOrDefault(false)
}
