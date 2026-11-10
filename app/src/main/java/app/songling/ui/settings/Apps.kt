package app.songling.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import app.songling.container
import app.songling.data.Players
import app.songling.data.Settings
import app.songling.ui.components.Chip
import app.songling.ui.components.Flow
import app.songling.ui.components.Segmented
import app.songling.ui.components.t
import app.songling.ui.theme.C
import app.songling.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The phone's players, those already heard first. */
@Composable
fun rememberPlayers(): List<Players.App> {
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf<List<Players.App>>(emptyList()) }
    LaunchedEffect(Unit) {
        val used = ctx.container.db.dao().apps()
        apps = withContext(Dispatchers.IO) { Players.list(ctx, used) }
    }
    return apps
}

/** Which apps the pet hears: every player but the ticked ones, or only the ticked ones. */
@Composable
fun AppsChoice(s: Settings, apps: List<Players.App>, set: ((Settings) -> Settings) -> Unit, pad: Dp = 16.dp) {
    Segmented(listOf(t("Все, кроме…", "All but…"), t("Только…", "Only…")), if (s.only) 1 else 0,
        { i -> set { it.copy(only = i == 1) } }, Modifier.padding(start = (pad - 4.dp).coerceAtLeast(0.dp), end = (pad - 4.dp).coerceAtLeast(0.dp), top = 4.dp))
    Text(if (s.only) t("Питомец слышит только отмеченные приложения. Остальные – соцсети, видео, аудиокниги – он не замечает.",
            "The pet hears only the apps ticked here. Everything else – social apps, video, audiobooks – goes unnoticed.")
        else t("Питомец слышит всё, что играет, кроме отмеченных приложений.", "The pet hears whatever plays, except the apps ticked here."),
        style = T.Small, modifier = Modifier.padding(start = pad, end = pad, top = 8.dp))
    Flow(Modifier.padding(horizontal = pad, vertical = 10.dp)) {
        if (apps.isEmpty()) Text(t("Здесь появятся плееры с телефона.", "The phone's players show up here."), style = T.Muted)
        for (a in apps) {
            val on = if (s.only) a.pkg in s.tracked else a.pkg in s.excluded
            Chip(a.label, on, {
                set {
                    if (it.only) it.copy(tracked = if (a.pkg in it.tracked) it.tracked - a.pkg else it.tracked + a.pkg)
                    else it.copy(excluded = if (a.pkg in it.excluded) it.excluded - a.pkg else it.excluded + a.pkg)
                }
            }, color = if (s.only) C.mint else C.coral)
        }
    }
}
