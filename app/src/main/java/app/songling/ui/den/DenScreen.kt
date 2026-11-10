package app.songling.ui.den

import android.content.Intent
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.songling.container
import app.songling.data.Covers
import app.songling.data.MIN
import app.songling.domain.Item
import app.songling.domain.Stage
import app.songling.domain.Studio
import app.songling.domain.Wall
import app.songling.domain.Words
import app.songling.listen.NowPlaying
import app.songling.ui.LocalRu
import app.songling.ui.Nav
import app.songling.ui.Overlay
import app.songling.ui.art.Buf
import app.songling.ui.art.DenArt
import app.songling.ui.art.DenLook
import app.songling.ui.art.Look
import app.songling.ui.art.PixelCanvas
import app.songling.ui.components.Bar
import app.songling.ui.components.Btn
import app.songling.ui.components.Card
import app.songling.ui.components.Cover
import app.songling.ui.components.Flow
import app.songling.ui.components.Kind
import app.songling.ui.components.Says
import app.songling.ui.components.Section
import app.songling.ui.components.Stat
import app.songling.ui.components.Tile
import app.songling.ui.components.t
import app.songling.ui.title
import app.songling.ui.theme.C
import app.songling.ui.theme.R8
import app.songling.ui.theme.T
import app.songling.ui.theme.paper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Instant

/** The look of the pet out now, as the den and widget draw it. */
fun Studio.look(playing: Boolean, happy: Boolean = false): Look? {
    val p = pet ?: return null
    return Look(p.species, p.stage, p.pet.shiny, p.form, mood, sleeping && !playing, if (playing) 0f else windup, happy, playing,
        hatch = if (p.egg) p.fed / Stage.BABY.from.toFloat() else 0f)
}

@Composable
fun DenScreen(st: Studio, playing: NowPlaying?, nav: Nav) {
    val ctx = LocalContext.current
    val ru = LocalRu.current
    val co = ctx.container
    val pet = st.pet
    var stroked by remember { mutableLongStateOf(0L) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(stroked) { if (stroked > 0) { delay(2200); nowMs = System.currentTimeMillis() } }
    val happy = stroked > 0 && System.currentTimeMillis() - stroked < 2000 && nowMs <= stroked + 2000
    val isPlaying = playing?.playing == true
    // Colours and covers are read off the main thread.
    val extras by produceState<Triple<List<Int>, List<Buf?>?, Int?>>(Triple(emptyList(), null, null), st.wholeRuns.size, playing?.cover, st.s.bought, st.s.posters, st.albums.values.count { it.cover != null }) {
        value = withContext(Dispatchers.IO) {
            val spines = st.wholeRuns.map { it.first }.distinct().takeLast(42).map { k ->
                val a = st.albums[k]
                Covers.colour(ctx, a?.cover) ?: a?.genre?.argb?.toInt() ?: (0xFF6A5A7A).toInt()
            }
            Triple(spines, if (Item.POSTER.name in st.s.bought) runCatching { Posters.pictures(ctx, st) }.getOrNull() else null, Covers.colour(ctx, playing?.cover))
        }
    }
    val posterFiles by produceState<List<String?>>(emptyList(), st.s.posters, st.s.bought, st.albums.values.count { it.cover != null }) {
        value = withContext(Dispatchers.IO) { if (Item.POSTER.name in st.s.bought) Posters.files(ctx, st) else emptyList() }
    }
    val zoned = Instant.ofEpochMilli(st.now).atZone(st.zone)
    val den = DenLook(
        wall = Wall.entries.firstOrNull { it.name == st.s.wall } ?: Wall.PLUM,
        items = Item.entries.filter { it.name in st.s.bought && it.name !in st.s.hidden }.toSet(),
        hour = zoned.hour, minute = zoned.minute, spines = extras.first, posters = extras.second.orEmpty(), posterFiles = posterFiles, label = extras.third,
        playing = isPlaying, pet = st.look(isPlaying, happy),
        outside = if (st.s.realWeather) app.songling.data.Outside.of(st.s.outside) else null, now = st.now,
    )

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.fillMaxWidth().paper(24.dp, 4.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp)).background(C.surface).padding(6.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
            .pointerInput(pet?.pet?.id) {
                detectTapGestures(onTap = {
                    if (pet == null) nav.go(Overlay.Bestiary) else {
                        stroked = System.currentTimeMillis()
                        co.write { settings.update { it.copy(strokes = it.strokes + 1) } }
                        if (!pet.egg) nav.say(stroke(pet.pet.title(ru), ru, st.s.strokes))
                    }
                }, onLongPress = { if (pet != null) nav.go(Overlay.PetInfo) })
            }) {
            app.songling.ui.components.DenView(den)
        }
        app.songling.ui.components.FirstVisit(app.songling.ui.components.Tip.DEN, st.s)
        if (pet == null) {
            Card {
                Text(t("Здесь будет жить твой питомец", "Your pet will live here"), style = T.Name)
                Text(t("Выбери одно из трёх яиц. Оно будет слушать музыку вместе с тобой и вылупится через три часа музыки.",
                    "Pick one of three eggs. It listens to music with you and hatches after three hours of it."), style = T.Muted)
                Btn(t("Выбрать яйцо", "Pick an egg"), { nav.go(Overlay.Bestiary) }, Modifier.fillMaxWidth().padding(top = 8.dp), kind = Kind.AMBER)
            }
        } else PetCard(st, nav.said.first.ifEmpty { Words.poke(st, ru) }, nav)
        NowLine(st, playing, co.tracker.enabled(), nav)
        QuestLine(st, nav)
        Actions(st, nav)
    }
}

private fun stroke(name: String, ru: Boolean, n: Int): String {
    val r = listOf("$name жмурится и мурлычет в такт.", "$name подставляет макушку.", "$name довольно притопывает.", "$name напевает что-то под нос.")
    val e = listOf("$name squints and purrs in time.", "$name leans into your hand.", "$name taps a happy foot.", "$name hums something under the breath.")
    return (if (ru) r else e)[n % r.size]
}

@Composable
private fun PetLine(st: Studio, nav: Nav) {
    val ru = LocalRu.current
    val p = st.pet ?: return
    Card(onClick = { nav.go(Overlay.PetInfo) }, padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(p.pet.title(ru), style = T.Name)
                Text(Words.species(p.species, ru) + " · " + Words.stage(p.stage, ru) + (p.form?.let { " · " + Words.form(it, ru) } ?: ""), style = T.Small)
            }
            Text(Words.dur(p.fed * MIN, ru), style = T.Small.copy(color = C.amber))
        }
        val next = p.stage.next
        Bar(p.progress, Modifier.fillMaxWidth().padding(top = 8.dp), color = C.lilac)
        Text(if (next == null) t("Древнее не бывает.", "As old as they come.")
            else t("до стадии «${Words.stage(next, true)}» – ${Words.dur((next.from - p.fed).coerceAtLeast(0) * MIN, true)} музыки",
                "${Words.dur((next.from - p.fed).coerceAtLeast(0) * MIN, false)} of music to «${Words.stage(next, false)}»"), style = T.Small, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun NowCard(np: NowPlaying?, enabled: Boolean) {
    val ctx = LocalContext.current
    if (!enabled && !app.songling.BuildConfig.DEMO) {
        Card(fill = C.coralSoft) {
            Text(t("Songling пока не слышит музыку", "Songling can't hear the music yet"), style = T.Name)
            Text(t("Android показывает, что играет в плеере, только приложениям с доступом к уведомлениям. Songling читает лишь название песни, артиста и альбом.",
                "Android only shows what a player is playing to apps with notification access. Songling reads just the song, the artist and the album."), style = T.Muted)
            Btn(t("Дать доступ", "Give access"), { ctx.startActivity(Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
                Modifier.fillMaxWidth().padding(top = 8.dp), kind = Kind.CORAL)
        }
        return
    }
    if (np == null) {
        Card { Text(t("Тишина. Включи музыку в любом плеере – питомец услышит.", "Silence. Play music in any player and your pet will hear it."), style = T.Muted) }
        return
    }
    Card {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Cover(np.cover, np.album.ifEmpty { np.title }, null, 56.dp)
            Column(Modifier.weight(1f)) {
                Text((if (np.playing) t("СЕЙЧАС ИГРАЕТ", "NOW PLAYING") else t("НА ПАУЗЕ", "PAUSED")), style = T.Eyebrow.copy(color = if (np.playing) C.mint else C.faint))
                Text(np.title, style = T.Strong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(np.artist + if (np.album.isNotEmpty()) " · ${np.album}" else "", style = T.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (np.length > 0) Bar(np.ms / np.length.toFloat(), Modifier.fillMaxWidth().padding(top = 10.dp), color = C.mint)
    }
}

@Composable
private fun Earworm(st: Studio, nav: Nav) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    val unstuck = st.s.unstuck > 0 && st.day(st.s.unstuck) == st.today
    val e = st.earworm
    if (unstuck) {
        val f = st.forgotten ?: return
        Card(fill = C.lilacSoft, onClick = { nav.go(Overlay.TrackOf(f.key)) }) {
            Text(t("ДАВНО НЕ СЛЫШАЛИ", "LONG UNHEARD"), style = T.Eyebrow.copy(color = C.lilac))
            Text("«${f.name}» · ${f.artistName}", style = T.Strong)
            Text(t("Ты слушал её ${Words.times(f.plays, true)}, а потом забыл. Питомец помнит.", "You played it ${Words.times(f.plays, false)}, then forgot. Your pet remembers."), style = T.Small)
        }
        return
    }
    if (e == null) return
    Card(fill = C.amberSoft) {
        Text(t("ПРИЛИПЛО", "EARWORM"), style = T.Eyebrow.copy(color = C.amber))
        Text("«${e.name}» · ${e.artistName}", style = T.Strong, modifier = Modifier.clickable { nav.go(Overlay.TrackOf(e.key)) })
        Text(t("Сегодня уже ${Words.times(st.plays.count { it.track == e.key && st.day(it.at) == st.today }, true)}. Питомец напевает её весь день.",
            "${Words.times(st.plays.count { it.track == e.key && st.day(it.at) == st.today }, false)} today already. Your pet hums it all day."), style = T.Small)
        Btn(t("Отлепить", "Unstick it"), { co.write { settings.update { it.copy(unstuck = System.currentTimeMillis()) } } }, Modifier.padding(top = 8.dp), kind = Kind.GHOST, small = true)
    }
}

@Composable
fun QuestList(st: Studio, nav: Nav) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    for (q in st.quests) {
        val id = "${st.weekId}:${q.q.name}"
        val claimed = id in st.s.claimed
        Row(Modifier.fillMaxWidth().clip(R8.Md).background(C.surface).border(1.dp, if (q.p.done && !claimed) C.amber else C.line, R8.Md).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Text(Words.quest(q.q, ru), style = T.Strong.copy(color = if (claimed) C.faint else C.ink))
                Bar(q.p.part, Modifier.fillMaxWidth().padding(top = 6.dp), color = if (claimed) C.line else C.mint)
                Text(if (q.q == app.songling.domain.Quest.HOURS) "${Words.dur(q.p.have * MIN, ru)} / ${Words.dur(q.p.need * MIN, ru)}" else "${q.p.have.coerceAtMost(q.p.need)} / ${q.p.need}", style = T.Small, modifier = Modifier.padding(top = 2.dp))
            }
            when {
                claimed -> Text(t("получено", "taken"), style = T.Small)
                q.p.done -> Btn("+${q.q.reward}", {
                    co.write { settings.update { it.copy(claimed = it.claimed + id) } }
                    nav.toast(if (ru) "+${q.q.reward} медиаторов" else "+${q.q.reward} plectrums")
                }, kind = Kind.AMBER, small = true)
                else -> Text("+${q.q.reward}", style = T.Small.copy(color = C.amber))
            }
        }
    }
}
