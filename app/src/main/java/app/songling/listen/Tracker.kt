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
import app.songling.data.Source
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
        var art: Bitmap? = null, var cover: String? = null, var writing: Boolean = false,
    )

    private var cur: Cur? = null
    private var active: MediaController? = null

    fun enabled(): Boolean = NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)

    private val sessions = MediaSessionManager.OnActiveSessionsChangedListener { list -> main.post { follow(list.orEmpty()) } }

    fun start(listener: ComponentName) {
        if (manager != null) return
        val m = ctx.getSystemService(MediaSessionManager::class.java) ?: return
        manager = m
        runCatching {
            m.addOnActiveSessionsChangedListener(sessions, listener)
            follow(m.getActiveSessions(listener))
        }
        main.postDelayed(tickRun, 15_000)
    }

    fun stop() {
        runCatching { manager?.removeOnActiveSessionsChangedListener(sessions) }
        manager = null
        callbacks.forEach { (c, cb) -> c.unregisterCallback(cb) }
        callbacks.clear()
        close()
        main.removeCallbacks(tickRun)
    }

    private val tickRun = object : Runnable {
        override fun run() { tick(); main.postDelayed(this, 15_000) }
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
        val restarted = same && c0!!.playedMs > 30_000 && (c.playbackState?.position ?: 0) < 3_000 && c0.lastPos > 10_000
        if (!same || restarted) {
            pause(); close()
            cur = Cur(c.packageName, title, artist, album, length, System.currentTimeMillis())
        }
        active = c
        val k = cur!!
        if (k.art == null) k.art = md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        k.lastPos = c.playbackState?.position ?: k.lastPos
        if (playing && k.since == 0L) k.since = System.currentTimeMillis()
        if (!playing) pause()
        publish()
        tick()
    }

    /** Stops the clock on the current track. */
    private fun pause() {
        val k = cur ?: return
        if (k.since > 0) { k.playedMs += System.currentTimeMillis() - k.since; k.since = 0 }
        publish()
    }

    private fun played(k: Cur) = k.playedMs + if (k.since > 0) System.currentTimeMillis() - k.since else 0

    private fun publish() {
        val k = cur
        now.value = k?.let { NowPlaying(it.title, it.artist, it.album, it.pkg, it.since > 0, played(it), it.length, it.cover, it.started) }
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
            if (k.pkg in s.excluded || (s.skipLong && k.length > s.longMinutes * MIN) || ms < s.minSeconds * 1000L) { k.writing = false; return@launch }
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
