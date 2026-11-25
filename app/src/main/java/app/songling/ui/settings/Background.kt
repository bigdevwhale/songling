package app.songling.ui.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.delay
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.songling.ui.components.Btn
import app.songling.ui.components.Kind
import app.songling.ui.components.t
import app.songling.ui.theme.C
import app.songling.ui.theme.R8
import app.songling.ui.theme.T
import app.songling.ui.theme.paper

/**
 * Listening with the screen off (#16): the system puts an app to sleep when the battery is saved on it, and on Xiaomi,
 * Huawei, Oppo, Vivo and their kin also when it may not start on its own. The two switches are deep in the phone's
 * settings; these open them.
 */
object Background {
    fun free(ctx: Context): Boolean = ctx.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(ctx.packageName) ?: true

    /** The phone's own autostart list, where there is one. */
    private val autostart: List<ComponentName> get() = when (Build.MANUFACTURER.lowercase()) {
        "xiaomi", "redmi", "poco" -> listOf(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"))
        "huawei", "honor" -> listOf(
            ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
            ComponentName("com.hihonor.systemmanager", "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"))
        "oppo", "realme", "oneplus" -> listOf(
            ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
            ComponentName("com.oplus.battery", "com.oplus.startupapp.view.StartupAppListActivity"))
        "vivo", "iqoo" -> listOf(
            ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
            ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"))
        else -> emptyList()
    }

    val hasAutostart: Boolean get() = autostart.isNotEmpty()

    private fun go(ctx: Context, i: Intent): Boolean = runCatching { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess

    private fun details(ctx: Context) = go(ctx, Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + ctx.packageName)))

    /**
     * Where the battery is saved on the app. On Xiaomi that's on the app's own page («Контроль активности»); elsewhere
     * the system's list of apps without limits, and the app's page if that isn't there.
     */
    fun battery(ctx: Context) {
        val miui = Build.MANUFACTURER.lowercase() in setOf("xiaomi", "redmi", "poco")
        if (miui && go(ctx, Intent("miui.intent.action.HIDDEN_APPS_CONFIG_ACTIVITY").putExtra("package_name", ctx.packageName)
                .putExtra("package_label", ctx.applicationInfo.loadLabel(ctx.packageManager)))) return
        if (!miui && go(ctx, Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))) return
        details(ctx)
    }

    fun autostart(ctx: Context) {
        for (c in autostart) if (go(ctx, Intent().setComponent(c))) return
        details(ctx)
    }
}

/**
 * The card under «music can be heard»: [always] shows it even when the battery is already free (in «Apps»),
 * otherwise it's there only while the system may put the app to sleep.
 */
@Composable
fun BackgroundCard(always: Boolean = false) {
    val ctx = LocalContext.current
    var free by remember { mutableStateOf(Background.free(ctx)) }
    var resumed by remember { mutableIntStateOf(0) }
    // Back from the phone's settings: see whether it was done.
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val o = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) resumed++ }
        owner.lifecycle.addObserver(o)
        onDispose { owner.lifecycle.removeObserver(o) }
    }
    // MIUI saves the choice only after its screen has closed, a moment after we're back: look again for a few seconds.
    LaunchedEffect(resumed) {
        for (wait in listOf(0L, 400L, 1_000L, 1_600L)) { delay(wait); free = Background.free(ctx); if (free) break }
    }
    if (free && !always) return
    Column(Modifier.fillMaxWidth().paper(22.dp).clip(R8.Lg).background(if (free) C.surface else C.coralSoft).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(t("Слушать с выключенным экраном", "Listening with the screen off"), style = T.Strong)
        Text(
            if (free && Background.hasAutostart) t("Батарея не ограничена. Если музыка с выключенным экраном всё равно теряется – включи Songling автозапуск.",
                "The battery isn't limited. If music with the screen off still goes missing, allow Songling to autostart.")
            else if (free) t("Батарея не ограничена – система не усыпит Songling.", "The battery isn't limited – the system won't put Songling to sleep.")
            else if (Background.hasAutostart) t("Сними ограничения батареи и включи автозапуск – иначе система усыпляет Songling и музыка не считается.",
                "Lift the battery limits and allow autostart – otherwise the system puts Songling to sleep and music isn't counted.")
            else t("Сними ограничения батареи – иначе система усыпляет Songling и музыка не считается.",
                "Lift the battery limits – otherwise the system puts Songling to sleep and music isn't counted."),
            style = T.Small)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(t("Батарея", "Battery"), { Background.battery(ctx) }, kind = if (free) Kind.GHOST else Kind.AMBER, small = true)
            if (Background.hasAutostart) Btn(t("Автозапуск", "Autostart"), { Background.autostart(ctx) }, kind = Kind.GHOST, small = true)
        }
    }
}
