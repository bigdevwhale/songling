package app.songling.ui.onboarding

import android.content.Intent
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.songling.container
import app.songling.domain.Species
import app.songling.domain.Stage
import app.songling.domain.Words
import app.songling.ui.LocalRu
import app.songling.ui.art.Look
import app.songling.ui.components.Btn
import app.songling.ui.components.Kind
import app.songling.ui.components.PetView
import app.songling.ui.components.t
import app.songling.ui.theme.C
import app.songling.ui.theme.R8
import app.songling.ui.theme.T

/** Three steps: what this is, the three eggs, access to players. */
@Composable
fun Onboarding() {
    val ctx = LocalContext.current
    val ru = LocalRu.current
    val co = ctx.container
    var step by remember { mutableIntStateOf(0) }
    var egg by remember { mutableStateOf<Species?>(null) }
    var look by remember { mutableStateOf(app.songling.ui.components.Medium.VINYL) }
    var theme by remember { mutableStateOf("light") }
    val s by co.settings.settings.collectAsState(initial = app.songling.data.Settings())
    val players = app.songling.ui.settings.rememberPlayers()
    Box(Modifier.fillMaxSize().background(C.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when (step) {
                0 -> {
                    Text("Songling", style = T.Display.copy(color = C.amber))
                    // The three you can start with, grown up, dancing.
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                        for (sp in listOf(Species.LYREBIRD, Species.PURR, Species.AXOLOTL)) PetView(Look(sp, Stage.ADULT, dancing = true), Modifier.weight(1f), gentle = true)
                    }
                    // A record, a tape and a record: what the shelf will look like.
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        val m = app.songling.ui.components.Medium.entries
                        val shelf = listOf(
                            Triple(m.first(), (if (ru) "Ночной эфир" else "Night Air") to (if (ru) "Сверчки" else "The Crickets"), app.songling.domain.Genre.JAZZ),
                            Triple(m.last(), (if (ru) "Летний микс" else "Summer Mix") to (if (ru) "Разные исполнители" else "Various artists"), app.songling.domain.Genre.ROCK),
                            Triple(m.first(), (if (ru) "Рассвет" else "Daybreak") to (if (ru) "Огоньки" else "Fireflies"), app.songling.domain.Genre.POP),
                        )
                        for ((medium, words, genre) in shelf)
                            androidx.compose.runtime.CompositionLocalProvider(app.songling.ui.components.LocalMedium provides medium) {
                                app.songling.ui.components.Sleeve(null, words.first, words.second, genre, 72.dp)
                            }
                    }
                    Text(t("Питомец, который слушает музыку вместе с тобой", "A pet that listens to music with you"), style = T.Title)
                    Text(t("Включай музыку в любом плеере – питомец услышит, будет танцевать и расти. По тому, как ты слушаешь, находятся новые яйца: всего их 18.",
                        "Play music in any player: your pet hears it, dances and grows. How you listen finds new eggs – 18 in all."), style = T.Muted)
                    Text(t("А ещё – полка альбомов, синглов и песен, статистика, календарь, цели и задания недели.",
                        "Plus a shelf of albums, singles and songs, stats, a calendar, goals and weekly tasks."), style = T.Muted)
                    Btn(t("Дальше", "Next"), { step = 1 }, Modifier.fillMaxWidth(), kind = Kind.AMBER)
                }
                1 -> {
                    Text(t("Выбери яйцо", "Pick an egg"), style = T.Title)
                    Text(t("Остальные появятся позже.", "The others come later."), style = T.Muted)
                    for (sp in Species.entries.filter { it.starter }) {
                        val on = egg == sp
                        Row(Modifier.fillMaxWidth().clip(R8.Lg).background(C.surface).border(if (on) 2.dp else 1.dp, if (on) C.amber else C.line, R8.Lg).clickable { egg = sp }.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PetView(Look(sp, Stage.EGG), Modifier.weight(0.26f), gentle = true)
                            // What it grows into.
                            PetView(Look(sp, Stage.ADULT, dancing = on), Modifier.weight(0.3f), gentle = true)
                            Column(Modifier.weight(0.44f)) {
                                Text(Words.species(sp, ru), style = T.Name)
                                Text(t("Любит ", "Loves ") + Words.loves(sp, ru), style = T.Small)
                            }
                        }
                    }
                    Btn(t("Дальше", "Next"), { step = 2 }, Modifier.fillMaxWidth(), kind = Kind.AMBER, enabled = egg != null)
                }
                2 -> {
                    Text(t("Как выглядит твоя музыка?", "How does your music look?"), style = T.Title)
                    Text(t("Альбомы, песни и формы будут выглядеть как пластинки в конвертах или как кассеты с подписанными от руки вкладышами. Поменять можно в настройках.",
                        "Albums, songs and forms look like records in sleeves, or like tapes with hand-written J-cards. You can change it in settings."), style = T.Muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (m in app.songling.ui.components.Medium.entries) {
                            val on = look == m
                            Column(Modifier.weight(1f).clip(R8.Lg).background(C.surface).border(if (on) 2.dp else 1.dp, if (on) C.amber else C.line, R8.Lg).clickable { look = m }.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                androidx.compose.runtime.CompositionLocalProvider(app.songling.ui.components.LocalMedium provides m) {
                                    app.songling.ui.components.Sleeve(null, if (ru) "Ночной эфир" else "Night Air", if (ru) "Сверчки" else "The Crickets", app.songling.domain.Genre.JAZZ, 110.dp)
                                }
                                Text(if (m == app.songling.ui.components.Medium.VINYL) t("Пластинки", "Records") else t("Кассеты", "Tapes"), style = T.Name.copy(color = if (on) C.amber else C.ink))
                            }
                        }
                    }
                    Text(t("Тема", "Theme"), style = T.Name)
                    app.songling.ui.components.Segmented(listOf(t("Светлая", "Light"), t("Тёмная", "Dark"), t("Как в системе", "System")),
                        listOf("light", "dark", "system").indexOf(theme), { i ->
                            theme = listOf("light", "dark", "system")[i]
                            co.write { settings.update { it.copy(theme = theme) } }
                        })
                    Btn(t("Дальше", "Next"), { step = 3; co.write { settings.update { it.copy(look = look.name) } } }, Modifier.fillMaxWidth(), kind = Kind.AMBER)
                }
                3 -> {
                    Text(t("Где ты слушаешь музыку?", "Where do you listen to music?"), style = T.Title)
                    Text(t("Питомец может слушать любой плеер или только те приложения, где у тебя музыка. Тогда видео, соцсети и аудиокниги его не собьют. Поменять можно в настройках.",
                        "The pet can hear any player, or only the apps you play music in, so video, social apps and audiobooks don't throw it off. You can change it in settings."), style = T.Muted)
                    app.songling.ui.settings.AppsChoice(s, players, { change -> co.write { settings.update(change) } }, pad = 0.dp)
                    Btn(t("Дальше", "Next"), { step = 4 }, Modifier.fillMaxWidth(), kind = Kind.AMBER, enabled = !s.only || s.tracked.isNotEmpty())
                }
                else -> {
                    Text(t("Чтобы питомец слышал музыку", "So your pet can hear the music"), style = T.Title)
                    Text(t("Android рассказывает, что играет в плеере, только приложениям с доступом к уведомлениям. Songling берёт оттуда лишь название песни, артиста и альбом – больше ничего.",
                        "Android only tells apps with notification access what a player is playing. Songling takes just the song, the artist and the album – nothing else."), style = T.Muted)
                    Btn(t("Дать доступ", "Give access"), { ctx.startActivity(Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
                        Modifier.fillMaxWidth(), kind = Kind.CORAL)
                    Btn(t("Готово", "Done"), {
                        co.write {
                            egg?.let { layEgg(it) }
                            settings.update { it.copy(onboarded = true) }
                        }
                    }, Modifier.fillMaxWidth(), kind = Kind.AMBER)
                    Text(t("Доступ можно дать и позже, в настройках.", "You can give access later in settings."), style = T.Small, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
