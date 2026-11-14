package app.songling.ui.den

import android.content.Intent
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.songling.R
import app.songling.container
import app.songling.data.MIN
import app.songling.domain.Studio
import app.songling.domain.Words
import app.songling.listen.NowPlaying
import app.songling.ui.LocalRu
import app.songling.ui.Nav
import app.songling.ui.Overlay
import app.songling.ui.components.Bar
import app.songling.ui.components.Cover
import app.songling.ui.components.Glyph
import app.songling.ui.components.Sheet
import app.songling.ui.components.Sign
import app.songling.ui.components.t
import app.songling.ui.theme.C
import app.songling.ui.theme.R8
import app.songling.ui.theme.T
import app.songling.ui.theme.paper
import app.songling.ui.title

private fun Modifier.card() = paper(22.dp).clip(R8.Lg).background(C.surface)

/** The pet in one card: who it is, today's music, how far to the next stage, and what it says. */
@Composable
fun PetCard(st: Studio, said: String, nav: Nav) {
    val ru = LocalRu.current
    val p = st.pet ?: return
    val today = st.byDay[st.today] ?: 0L
    Column(Modifier.fillMaxWidth().card().clickable { nav.go(Overlay.PetInfo) }.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Text(p.pet.title(ru), style = T.Name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                // Who it is, and the streak once there is one to speak of.
                Text(listOfNotNull(Words.species(p.species, ru), Words.stage(p.stage, ru),
                    if (st.streak >= 2) (if (ru) "${st.streak} ${Words.plural(st.streak, "день", "дня", "дней")} подряд" else "${st.streak} days in a row") else null).joinToString(" · "),
                    style = T.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(Words.dur(today, ru), style = T.Strong.copy(color = C.amber), maxLines = 1)
                Text(t("сегодня", "today"), style = T.Small, maxLines = 1)
            }
        }
        Bar(p.progress, Modifier.fillMaxWidth(), color = C.amber)
        if (!st.s.quiet && said.isNotEmpty()) Text("«$said»", style = T.Muted.copy(fontStyle = FontStyle.Italic), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** One line under the pet: what plays now, else the song stuck today, else a word about silence. */
@Composable
fun NowLine(st: Studio, np: NowPlaying?, enabled: Boolean, nav: Nav) {
    val ctx = LocalContext.current
    val ru = LocalRu.current
    val co = ctx.container
    if (!enabled && !app.songling.BuildConfig.DEMO) {
        Row(Modifier.fillMaxWidth().paper(22.dp).clip(R8.Lg).background(C.coralSoft)
            .clickable { ctx.startActivity(Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(t("Питомец пока не слышит музыку", "Your pet can't hear the music yet"), style = T.Strong)
                Text(t("Нажми и разреши доступ к уведомлениям – Songling возьмёт оттуда только песню и артиста.", "Tap to allow notification access – Songling takes just the song and the artist."), style = T.Small)
            }
            Sign(Glyph.NEXT, color = C.coral, size = 14.dp)
        }
        return
    }
    // Access is on, yet the system hasn't bound the listener (again): nothing would be counted (#7).
    val connected by co.tracker.connected.collectAsState()
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(5_000); settled = true }
    if (!connected && settled && !app.songling.BuildConfig.DEMO) {
        var tries by remember { mutableStateOf(0) }
        Row(Modifier.fillMaxWidth().paper(22.dp).clip(R8.Lg).background(C.coralSoft)
            .clickable {
                tries++
                if (tries == 1) app.songling.listen.Listener.rebind(ctx)
                else ctx.startActivity(Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            .padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(t("Питомец перестал слышать музыку", "Your pet stopped hearing the music"), style = T.Strong)
                Text(if (tries == 0) t("Система отключила Songling от плееров. Нажми – попробуем подключиться снова.", "The system cut Songling off from the players. Tap to reconnect.")
                    else t("Не вышло? Нажми ещё раз и в настройках выключи и снова включи доступ для Songling.", "Didn't help? Tap again, then turn Songling's access off and on in settings."), style = T.Small)
            }
            Sign(Glyph.NEXT, color = C.coral, size = 14.dp)
        }
        return
    }
    if (np != null) {
        Row(Modifier.fillMaxWidth().card().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            app.songling.ui.components.Sleeve(np.cover, np.album.ifEmpty { np.title }, np.artist, null, 48.dp)
            Column(Modifier.weight(1f)) {
                Text(np.title, style = T.Strong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(np.artist, style = T.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (np.length > 0) Bar(np.ms / np.length.toFloat(), Modifier.fillMaxWidth().padding(top = 4.dp), color = C.mint)
            }
            Text(if (np.playing) t("играет", "playing") else t("пауза", "paused"), style = T.Small.copy(color = if (np.playing) C.mint else C.faint))
        }
        return
    }
    val unstuck = st.s.unstuck > 0 && st.day(st.s.unstuck) == st.today
    val e = if (st.s.earworm && !unstuck) st.earworm else null
    val f = if (st.s.earworm && unstuck) st.forgotten else null
    Row(Modifier.fillMaxWidth().card().clickable(enabled = e != null || f != null) { (e ?: f)?.let { nav.go(Overlay.TrackOf(it.key)) } }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            when {
                e != null -> {
                    Text(t("ПРИЛИПЛО", "EARWORM"), style = T.Eyebrow.copy(color = C.amber))
                    Text("«${e.name}» · ${e.artistName}", style = T.Strong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                f != null -> {
                    Text(t("ДАВНО НЕ СЛЫШАЛИ", "LONG UNHEARD"), style = T.Eyebrow.copy(color = C.lilac))
                    Text("«${f.name}» · ${f.artistName}", style = T.Strong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                else -> Text(if (st.s.only) t("Тишина. Включи музыку в отмеченном плеере – питомец услышит.", "Silence. Play music in a ticked player – your pet will hear it.")
                    else t("Тишина. Включи музыку в любом плеере – питомец услышит.", "Silence. Play music in any player – your pet will hear it."), style = T.Muted)
            }
        }
        if (e != null) Text(t("отлепить", "unstick"), style = T.Small.copy(color = C.coral), modifier = Modifier.clip(CircleShape).clickable {
            co.write { settings.update { it.copy(unstuck = System.currentTimeMillis()) } }
        }.padding(horizontal = 8.dp, vertical = 6.dp))
    }
}

/** The week's three tasks in one line; the list opens in a sheet. */
@Composable
fun QuestLine(st: Studio, nav: Nav) {
    val ru = LocalRu.current
    val done = st.quests.count { it.p.done }
    val ready = st.quests.filter { it.p.done && "${st.weekId}:${it.q.name}" !in st.s.claimed }.sumOf { it.q.reward }
    Row(Modifier.fillMaxWidth().card().clickable { nav.go(Overlay.Quests) }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(t("Задания недели", "This week's tasks"), style = T.Strong)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (q in st.quests) Box(Modifier.weight(1f)) { Bar(q.p.part, Modifier.fillMaxWidth(), if (q.p.done) C.mint else C.amber) }
            }
        }
        if (ready > 0) Text("+$ready", style = T.Small.copy(color = C.onAmber), modifier = Modifier.clip(CircleShape).background(C.amber).padding(horizontal = 10.dp, vertical = 5.dp))
        else Text(if (ru) "$done из ${st.quests.size}" else "$done of ${st.quests.size}", style = T.Small)
        Sign(Glyph.NEXT, color = C.faint, size = 14.dp)
    }
}

/** The week's tasks, with plectrums to take. */
@Composable
fun QuestsSheet(st: Studio, nav: Nav) {
    Sheet(onDismiss = nav::close) {
        Text(t("Задания недели", "This week's tasks"), style = T.Title)
        Text(t("Новые – каждый понедельник. За каждое – медиаторы на логово.", "New ones every Monday. Each pays plectrums for the den."), style = T.Muted)
        QuestList(st, nav)
    }
}

/**
 * Three things to do from the den. Putting something on the shelf is the dock's record, and a concert is one of the
 * ways to log a listen, so neither needs a button of its own here.
 */
@Composable
fun Actions(st: Studio, nav: Nav) {
    Row(Modifier.fillMaxWidth().card().padding(vertical = 10.dp, horizontal = 4.dp)) {
        Action(R.drawable.act_log, t("Отметить", "Log a listen")) { nav.go(Overlay.Log()) }
        Action(R.drawable.act_egg, t("Питомцы", "Pets"), badge = "${st.hints.count { it.open }}") { nav.go(Overlay.Bestiary) }
        Action(R.drawable.act_lamp, t("Обустроить", "Furnish")) { nav.go(Overlay.Shop) }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Action(icon: Int, label: String, badge: String? = null, onClick: () -> Unit) {
    Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(C.surface2), contentAlignment = Alignment.Center) {
            Image(ImageBitmap.imageResource(icon), null, Modifier.size(24.dp), filterQuality = FilterQuality.Low, colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(C.muted))
        }
        Text(label + (badge?.let { " $it" } ?: ""), style = T.Small.copy(color = C.muted), maxLines = 1, textAlign = TextAlign.Center)
    }
}
