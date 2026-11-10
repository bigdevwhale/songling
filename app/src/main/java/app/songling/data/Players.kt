package app.songling.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/** The players on the phone, for choosing which ones Songling listens to. */
object Players {
    data class App(val pkg: String, val label: String, val used: Boolean)

    /** Well-known music apps; any other player that offers its library to Android is found too. */
    val KNOWN = listOf(
        "com.spotify.music", "ru.yandex.music", "com.google.android.apps.youtube.music", "com.google.android.youtube",
        "com.vkontakte.android", "com.uma.musicvk", "com.apple.android.music", "com.soundcloud.android", "deezer.android.app",
        "com.zvooq.openplay", "ru.mts.music.android", "com.maxmpz.audioplayer", "in.krosbits.musicolet", "com.aimp.player",
        "com.miui.player", "com.sec.android.app.music", "com.amazon.mp3", "com.aspiro.tidal", "com.pandora.android",
        "app.revanced.android.apps.youtube.music", "com.kapp.youtube.final", "org.schabi.newpipe", "com.bandcamp.android",
    )

    fun list(ctx: Context, used: Collection<String>): List<App> {
        val pm = ctx.packageManager
        val found = runCatching { pm.queryIntentServices(Intent("android.media.browse.MediaBrowserService"), 0).map { it.serviceInfo.packageName } }.getOrDefault(emptyList())
        val installed = KNOWN.filter { runCatching { pm.getPackageInfo(it, 0) }.isSuccess }
        return (used + found + installed).distinct().filter { it != ctx.packageName }
            .map { App(it, label(pm, it), it in used) }
            .sortedWith(compareByDescending<App> { it.used }.thenBy { it.label.lowercase() })
    }

    fun label(pm: PackageManager, pkg: String): String =
        runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrNull()?.takeIf { it.isNotBlank() } ?: pkg.substringAfterLast('.')
}
