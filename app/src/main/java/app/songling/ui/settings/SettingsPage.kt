package app.songling.ui.settings

import android.content.Intent
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import app.songling.ui.theme.paper
import app.songling.ui.theme.R8
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import app.songling.container
import app.songling.data.Backup
import app.songling.data.Import
import app.songling.data.Keys
import app.songling.data.Album
import app.songling.data.Artist
import app.songling.data.Track
import app.songling.domain.Studio
import app.songling.domain.Words
import app.songling.ui.BackdropPicker
import app.songling.ui.Backdrops
import app.songling.ui.LocalRu
import app.songling.ui.Nav
import app.songling.ui.Overlay
import app.songling.ui.components.Btn
import app.songling.ui.components.Card
import app.songling.ui.components.Chip
import app.songling.ui.components.Flow
import app.songling.ui.components.Kind
import app.songling.ui.components.Page
import app.songling.ui.components.Section
import app.songling.ui.components.Stepper
import app.songling.ui.components.t
import app.songling.ui.theme.C
import app.songling.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** A group of settings on one paper card, with a small heading above. */
@Composable
private fun Group(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Text(title.uppercase(), style = T.Eyebrow.copy(color = C.coral), modifier = Modifier.padding(start = 6.dp, top = 6.dp))
    androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth().paper(22.dp).clip(R8.Lg).background(C.surface).padding(vertical = 4.dp), content = content)
}

/** One line of a group: what it is, a word about it, and a control on the right. */
@Composable
private fun Line(title: String, note: String? = null, onClick: (() -> Unit)? = null, divider: Boolean = true, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            Text(title, style = T.Strong)
            if (note != null) Text(note, style = T.Muted.copy(fontSize = T.Muted.fontSize * .95f))
        }
        trailing()
    }
    if (divider) Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(C.surface2))
}

/** A plump toggle. */
@Composable
private fun Toggle(on: Boolean, onChange: (Boolean) -> Unit) {
    val x by androidx.compose.animation.core.animateDpAsState(if (on) 20.dp else 0.dp, label = "knob")
    val fill by androidx.compose.animation.animateColorAsState(if (on) C.amber else C.surface2, label = "fill")
    Box(Modifier.width(50.dp).height(30.dp).clip(CircleShape).background(fill).clickable { onChange(!on) }.padding(3.dp)) {
        Box(Modifier.offset(x = x).size(24.dp).paper(12.dp, 2.dp).clip(CircleShape).background(Color(0xFFFFF8EE)))
    }
}

/** − value + */
@Composable
private fun Small(value: String, onStep: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(C.surface2).clickable { onStep(-1) }, contentAlignment = Alignment.Center) { Text("−", style = T.Strong) }
        Text(value, style = T.Strong.copy(color = C.amber), modifier = Modifier.width(54.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Box(Modifier.size(32.dp).clip(CircleShape).background(C.surface2).clickable { onStep(1) }, contentAlignment = Alignment.Center) { Text("+", style = T.Strong) }
    }
}

@Composable
private fun Chevron() = Text("›", style = T.Title.copy(color = C.faint))

/** The StillCraft bot: deep links file a bug or an idea for this app, or follow its releases. */
object Community {
    private const val BOT = "https://t.me/ArcaneWhisperBot?start="
    const val SLUG = "songling"
    const val BUG = "${BOT}bug_$SLUG"
    const val IDEA = "${BOT}wish_$SLUG"
    const val FOLLOW = "${BOT}f_$SLUG"
    const val CHAT = "https://t.me/+Q4gFEDk8O842OGVi"
    const val SUPPORT = "https://bigdevwhale.github.io/stillcraft/support/"
    const val CODE = "https://github.com/bigdevwhale/songling"
}

@Composable
fun SettingsPage(st: Studio, nav: Nav) {
    val ctx = LocalContext.current
    val ru = LocalRu.current
    val co = ctx.container
    val s = st.s
    val scope = rememberCoroutineScope()
    fun set(change: (app.songling.data.Settings) -> app.songling.data.Settings) = co.write { settings.update(change) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch { if (Backdrops.importPicked(ctx, uri)) co.settings.update { it.copy(backdrop = Backdrops.OWN, backdropStamp = System.currentTimeMillis()) } }
    }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) { runCatching { ctx.contentResolver.openOutputStream(uri)!!.use { Backup.write(ctx, co.db.dao(), co.settings, it) } }.isSuccess }
            nav.toast(if (ok) (if (ru) "Копия сохранена" else "Backup saved") else (if (ru) "Не получилось сохранить" else "Couldn't save"))
        }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val n = withContext(Dispatchers.IO) { runCatching { ctx.contentResolver.openInputStream(uri)!!.use { Backup.read(ctx, co.db.dao(), co.settings, it) } }.getOrNull() }
            nav.toast(if (n == null) (if (ru) "Это не копия Songling" else "That isn't a Songling backup") else (if (ru) "Восстановлено: ${Words.plays(n, true)}" else "Restored: ${Words.plays(n, false)}"))
        }
    }
    var apps by remember { mutableStateOf<List<app.songling.data.Players.App>>(emptyList()) }
    LaunchedEffect(Unit) {
        val used = co.db.dao().apps()
        apps = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { app.songling.data.Players.list(ctx, used) }
    }
    var showApps by remember { mutableStateOf(false) }
    var askRestore by remember { mutableStateOf(false) }
    var pickTown by remember { mutableStateOf(false) }
    fun open(url: String) = runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    var newGenre by remember { mutableStateOf(false) }
    var editingGenre by remember { mutableStateOf<app.songling.data.OwnGenre?>(null) }
    Page(t("Настройки", "Settings"), nav::close) {
        app.songling.ui.components.FirstVisit(app.songling.ui.components.Tip.SETTINGS, st.s)
        // Access first: without it nothing works.
        val on = co.tracker.enabled() || app.songling.BuildConfig.DEMO
        Row(Modifier.fillMaxWidth().paper(22.dp).clip(R8.Lg).background(if (on) C.surface else C.coralSoft).clickable {
            ctx.startActivity(Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(if (on) C.mint else C.coral))
            androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                Text(if (on) t("Музыка слышна", "Music can be heard") else t("Музыка не слышна", "Music can't be heard"), style = T.Name)
                Text(if (on) t("Songling видит, что играет в плеерах. Сами уведомления не читаются.", "Songling sees what players play. Notifications themselves aren't read.")
                    else t("Нажми и разреши Songling доступ к уведомлениям – иначе питомец не услышит музыку.", "Tap and allow Songling notification access, or the pet can't hear the music."), style = T.Muted)
            }
            Chevron()
        }

        Group(t("Как считать", "Counting")) {
            Line(t("Песня засчитывается", "A song counts"), t("после стольких секунд", "after this many seconds")) {
                Small(if (ru) "${s.minSeconds} с" else "${s.minSeconds} s") { d -> set { it.copy(minSeconds = (it.minSeconds + d * 10).coerceIn(10, 120)) } }
            }
            Line(t("Без подкастов", "No podcasts"), t("не считать треки длиннее ${s.longMinutes} минут", "skip tracks over ${s.longMinutes} minutes")) { Toggle(s.skipLong) { v -> set { it.copy(skipLong = v) } } }
            Line(t("Новый день", "A new day"), t("музыка до этого часа идёт в вечер накануне", "music before this hour counts to the evening before")) {
                Small("${s.dayStart}:00") { d -> set { it.copy(dayStart = (it.dayStart + d).coerceIn(0, 8)) } }
            }
            val summary = when {
                s.only && s.tracked.isEmpty() -> t("ни одного – отметь плееры", "none yet – tick the players")
                s.only -> t("только отмеченные: ${s.tracked.size}", "only the ticked: ${s.tracked.size}")
                s.excluded.isEmpty() -> t("все плееры", "every player")
                else -> t("все, кроме отмеченных: ${s.excluded.size}", "all but the ticked: ${s.excluded.size}")
            }
            Line(t("Какие приложения слушать", "Which apps to hear"), summary,
                onClick = { showApps = !showApps }, divider = showApps) { Text(if (showApps) "⌃" else "⌄", style = T.Strong.copy(color = C.faint)) }
            if (showApps) {
                app.songling.ui.components.Segmented(listOf(t("Все, кроме…", "All but…"), t("Только…", "Only…")), if (s.only) 1 else 0,
                    { i -> set { it.copy(only = i == 1) } }, Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp))
                Text(if (s.only) t("Питомец слышит только отмеченные приложения. Остальные – соцсети, видео, аудиокниги – он не замечает.",
                        "The pet hears only the apps ticked here. Everything else – social apps, video, audiobooks – goes unnoticed.")
                    else t("Питомец слышит всё, что играет, кроме отмеченных приложений.", "The pet hears whatever plays, except the apps ticked here."),
                    style = T.Small, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp))
                Flow(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
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
        }

        Group(t("Вид", "Look")) {
            Line(t("Музыка", "Music"), t("как выглядят альбомы, песни и формы", "how albums, songs and forms look"), divider = false)
            app.songling.ui.components.Segmented(listOf(t("Пластинки", "Records"), t("Кассеты", "Tapes")), if (s.look == "TAPE") 1 else 0,
                { i -> set { it.copy(look = if (i == 1) "TAPE" else "VINYL") } }, Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp))
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(C.surface2))
            Line(t("Тема", "Theme"), null, divider = false)
            app.songling.ui.components.Segmented(listOf(t("Светлая", "Light"), t("Тёмная", "Dark"), t("Как в системе", "System")),
                when (s.theme) { "dark" -> 1; "system" -> 2; else -> 0 }, { i -> set { it.copy(theme = listOf("light", "dark", "system")[i]) } },
                Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp))
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(C.surface2))
            Line(t("Фон", "Background"), Backdrops.name(s.backdrop, ru), divider = false)
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                BackdropPicker(s.backdrop, s.backdropStamp, { k -> set { it.copy(backdrop = k) } }, { pick.launch("image/*") })
            }
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(C.surface2))
            Line(t("Питомец говорит", "The pet talks"), t("реплики над логовом", "lines above the den")) { Toggle(!s.quiet) { v -> set { it.copy(quiet = !v) } } }
            Line(t("Песня дня", "Earworm"), t("что прилипло сегодня", "what's stuck today")) { Toggle(s.earworm) { v -> set { it.copy(earworm = v) } } }
            Line(t("Обложки пикселями", "Covers in pixels"), t("как в логове", "like in the den"), divider = false) { Toggle(s.pixelCovers) { v -> set { it.copy(pixelCovers = v) } } }
        }

        Group(t("За окном", "Outside the window")) {
            val o = app.songling.data.Outside.of(s.outside)
            Line(t("Настоящая погода и время суток", "Real weather and time of day"), t("как сейчас в вашем городе", "as it is in your town now")) {
                Toggle(s.realWeather) { v -> set { it.copy(realWeather = v) }; if (v) co.refreshOutside(force = true) }
            }
            if (s.realWeather) Line(t("Город", "Town"), s.town.ifBlank { t("определится сам", "found by itself") } +
                (o?.let { " · " + String.format("%+.0f°", it.temp) + " · " + weatherName(it.weather, ru) } ?: ""), onClick = { pickTown = true }, divider = false) { Chevron() }
        }
        if (pickTown) app.songling.ui.Floating { TownSheet(onClose = { pickTown = false }) }

        Group(t("Свои жанры", "Your genres")) {
            for (o in s.own) Line(o.name, t("похож на ", "close to ") + Words.genre(app.songling.domain.Genre.entries.firstOrNull { it.name == o.base } ?: app.songling.domain.Genre.OTHER, ru),
                onClick = { editingGenre = o }) { Box(Modifier.size(18.dp).clip(CircleShape).background(Color(o.color))) }
            Line(t("+ Новый жанр", "+ New genre"), if (s.own.isEmpty()) t("свои названия для музыки: «для пробежки», «пост-рок для работы»…", "your own names: «running», «post-rock for work»…") else null,
                onClick = { newGenre = true }, divider = false)
        }
        if (newGenre || editingGenre != null) app.songling.ui.Floating {
            app.songling.ui.sheets.OwnGenreSheet(st, editingGenre, onSaved = { newGenre = false; editingGenre = null }, onClose = { newGenre = false; editingGenre = null })
        }

        Group(t("Язык", "Language")) {
            val cur = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            Row(Modifier.fillMaxWidth().padding(10.dp).clip(CircleShape).background(C.surface2).padding(4.dp)) {
                for ((label, tag) in listOf(t("Как в телефоне", "Phone") to "", "Русский" to "ru", "English" to "en")) {
                    val sel = if (tag.isEmpty()) cur.isEmpty() else cur.startsWith(tag)
                    Text(label, style = T.Small.copy(color = if (sel) C.ink else C.muted, fontSize = T.Small.fontSize * 1.05f), maxLines = 1,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.weight(1f).then(if (sel) Modifier.paper(99.dp, 2.dp) else Modifier).clip(CircleShape).background(if (sel) C.surface else C.surface2)
                            .clickable { AppCompatDelegate.setApplicationLocales(if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)) }
                            .padding(vertical = 9.dp))
                }
            }
        }

        Group(t("История и копии", "History and backups")) {
            Line(t("Перенести историю", "Bring history over"), "Spotify, Last.fm, ListenBrainz", onClick = { nav.go(Overlay.Import) }) { Chevron() }
            Line(t("Сохранить копию", "Save a backup"), t("всё в один zip: прослушивания, полка, питомцы, цели", "everything in one zip: listens, shelf, pets, goals"),
                onClick = { save.launch("songling-${LocalDate.now()}.zip") }) { Chevron() }
            Line(t("Восстановить из копии", "Restore a backup"), t("заменит всё, что есть сейчас", "replaces everything there is now"),
                onClick = { askRestore = true }, divider = false) { Chevron() }
        }

        Group("StillCraft") {
            Line(t("Сообщество в Telegram", "Telegram community"), t("новости, обсуждения, другие приложения", "news, talk and the other apps"), onClick = { open(Community.CHAT) }) { Chevron() }
            Line(t("Сообщить об ошибке", "Report a bug"), t("через бота StillCraft", "through the StillCraft bot"), onClick = { open(Community.BUG) }) { Chevron() }
            Line(t("Предложить идею", "Suggest an idea"), null, onClick = { open(Community.IDEA) }) { Chevron() }
            Line(t("Следить за обновлениями", "Follow updates"), t("бот напишет, когда выйдет новая версия", "the bot writes when a new version is out"), onClick = { open(Community.FOLLOW) }) { Chevron() }
            Line(t("Поддержать Songling", "Support Songling"), t("приложение бесплатное и без рекламы", "the app is free and has no ads"), onClick = { open(Community.SUPPORT) }) { Chevron() }
            Line(t("Исходный код", "Source code"), "GitHub", onClick = { open(Community.CODE) }, divider = false) { Chevron() }
        }

        Group(t("О приложении", "About")) {
            Line(t("Как это устроено", "How it works"), t("стадии, яйца, медиаторы, цели", "stages, eggs, plectrums, goals"), onClick = { nav.go(Overlay.How) }) { Chevron() }
            Line("Songling ${app.songling.BuildConfig.VERSION_NAME}", t("Из серии Stillcraft. Без рекламы и аккаунта, всё хранится на телефоне. В сеть – только за обложками, жанрами и странами.",
                "Part of Stillcraft. No ads, no account; everything stays on the phone. Online only for covers, genres and countries."), divider = false)
        }
    }
    if (askRestore) app.songling.ui.components.Sheet(onDismiss = { askRestore = false }) {
        Text(t("Восстановить из копии?", "Restore from a backup?"), style = T.Title)
        Text(t("Всё, что сейчас в Songling, – прослушивания, полка, питомцы, цели – заменится тем, что в копии. Сначала можно сохранить копию нынешнего.",
            "Everything in Songling now – listens, shelf, pets, goals – is replaced with what's in the backup. You can save a backup of the current state first."), style = T.Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            app.songling.ui.components.Btn(t("Сначала сохранить", "Save first"), { askRestore = false; save.launch("songling-${LocalDate.now()}.zip") }, Modifier.weight(1f), kind = Kind.SOFT)
            app.songling.ui.components.Btn(t("Выбрать копию", "Pick the backup"), { askRestore = false; restore.launch(arrayOf("application/zip", "application/octet-stream")) }, Modifier.weight(1f), kind = Kind.CORAL)
        }
    }
}

@Composable
fun ImportPage(st: Studio, nav: Nav) {
    val ctx = LocalContext.current
    val ru = LocalRu.current
    val co = ctx.container
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var said by remember { mutableStateOf("") }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            var added = 0; var seen = 0; var formats = HashSet<String>()
            withContext(Dispatchers.IO) {
                for (u in uris) runCatching {
                    val name = ctx.contentResolver.query(u, null, null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(c.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME)) else null } ?: u.lastPathSegment.orEmpty()
                    val r = ctx.contentResolver.openInputStream(u)!!.use { Import.read(name, it) }
                    if (r.format.isNotEmpty()) formats += r.format
                    seen += r.plays.size
                    for (chunk in r.plays.chunked(2000)) {
                        added += co.db.dao().insertPlays(chunk).count { it > 0 }
                        val dao = co.db.dao()
                        for (p in chunk.distinctBy { it.artist }) dao.insertArtist(Artist(p.artist, Keys.mainArtist(p.artistName)))
                        for (p in chunk.distinctBy { it.track }) dao.insertTrack(Track(p.track, p.title, p.artistName, album = p.album, albumName = p.albumName))
                        for (p in chunk.filter { it.album.isNotEmpty() }.distinctBy { it.album }) dao.insertAlbum(Album(p.album, p.albumName, p.artistName))
                    }
                }
            }
            busy = false
            said = if (ru) "Найдено ${Words.plays(seen, true)} (${formats.joinToString()}), новых: $added." else "Found ${Words.plays(seen, false)} (${formats.joinToString()}), new: $added."
            co.kick()
        }
    }
    Page(t("Перенести историю", "Bring history over"), nav::close) {
        Text(t("Прошлые прослушивания попадут в статистику, на полку и в календарь. Питомцу они не достанутся – он ест только то, что слушает вместе с тобой. Зато яйца, которые открываются по истории, найдутся сразу.",
            "Past listens go into the stats, the shelf and the calendar. The pet doesn't eat them – only what it hears with you – but eggs that open by your history are found at once."), style = T.Muted)
        Card {
            Text("Spotify", style = T.Name)
            Text(t("На сайте Spotify: Аккаунт → Конфиденциальность → «Расширенная история потоковой передачи». Через несколько дней придёт архив – выбери его целиком (zip) или файлы Streaming_History_Audio.",
                "On spotify.com: Account → Privacy → «Extended streaming history». An archive arrives within days: pick the zip or the Streaming_History_Audio files."), style = T.Small)
        }
        Card {
            Text("Last.fm", style = T.Name)
            Text(t("Выгрузи скробблы в CSV (например, на lastfm.ghan.nl/export) и выбери файл.", "Export your scrobbles to CSV (e.g. at lastfm.ghan.nl/export) and pick the file."), style = T.Small)
        }
        Card {
            Text("ListenBrainz", style = T.Name)
            Text(t("Настройки профиля → «Экспорт прослушиваний» – выбери полученный JSON или zip.", "Profile settings → «Export listens» – pick the JSON or zip."), style = T.Small)
        }
        Btn(if (busy) t("Читаю…", "Reading…") else t("Выбрать файлы", "Pick files"), { open.launch(arrayOf("*/*")) }, Modifier.fillMaxWidth(), kind = Kind.AMBER, enabled = !busy)
        if (said.isNotEmpty()) Text(said, style = T.Body.copy(color = C.mint))
        val imported = st.plays.count { it.source == app.songling.data.Source.IMPORT }
        if (imported > 0) Btn(t("Убрать всё импортированное ($imported)", "Remove everything imported ($imported)"), { co.write { db.dao().forgetImported() } }, Modifier.fillMaxWidth(), kind = Kind.GHOST, small = true)
    }
}

@Composable
fun HowPage(nav: Nav) {
    Page(t("Как это устроено", "How it works"), nav::close) {
        val rows = listOf(
            t("Питомец ест музыку", "The pet eats music") to t("Слушай в любом плеере – Spotify, Яндекс Музыка, YouTube Music, VK, свой плеер. Каждая минута идёт в корм. Песня засчитывается после 30 секунд.",
                "Listen in any player – Spotify, YouTube Music, Apple Music, your own. Every minute feeds the pet. A song counts after 30 seconds."),
            t("Стадии", "Stages") to t("Яйцо вылупляется через 3 часа музыки. Дальше – подросток (20 ч), взрослый (≈67 ч) и древний (200 ч). Любимое питомца идёт в зачёт в полтора раза.",
                "An egg hatches after 3 hours of music. Then youngster (20 h), grown (≈67 h) and ancient (200 h). A pet's favourite counts one and a half times."),
            t("18 питомцев", "18 pets") to t("Три яйца есть сразу, остальные находятся по тому, как ты слушаешь: часы жанра, ночи, утра, страны, долгие сессии, песня на повторе, концерт.",
                "Three eggs are there from the start; the rest are found by how you listen: hours of a genre, nights, mornings, countries, long sittings, a song on repeat, a concert."),
            t("Форма", "Form") to t("Выросший питомец навсегда принимает одну из шести форм – по музыке, которой питался: Гроза, Неон, Бархат, Хор, Улица, Корни.",
                "A grown pet takes one of six forms for good by the music it ate: Storm, Neon, Velvet, Choir, Street, Roots."),
            t("Завод кончается", "Winding down") to t("Без музыки несколько дней питомец двигается всё медленнее и в конце концов останавливается, как заводная игрушка. Пара песен – и он снова танцует.",
                "Days without music slow the pet down until it stops like a wind-up toy. A couple of songs and it dances again."),
            t("Альбом целиком", "A whole album") to t("Большая часть песен альбома за один присест. За это – пять медиаторов и корешок на полке пластинок.",
                "Most of an album's songs in one sitting. That's five plectrums and a spine on the record shelf."),
            t("Медиаторы", "Plectrums") to t("Один за 10 минут музыки, пять за альбом целиком, ещё – за задания недели и разовые цели. Тратятся на логово.",
                "One per 10 minutes, five per whole album, more for the week's tasks and one-off goals. Spent on the den."),
            t("Полка и цели", "Shelf and goals") to t("Альбомы, синглы и песни можно добавлять сами, ставить звёзды и класть в список «хочу послушать». Цели – по альбомам, артистам, песням, жанрам, странам и времени.",
                "Add albums, singles and songs yourself, rate them and keep a «to hear» list. Goals can be about albums, artists, songs, genres, countries and time."),
        )
        for ((h, b) in rows) Card { Text(h, style = T.Name); Text(b, style = T.Muted) }
    }
}


fun weatherName(w: app.songling.data.Weather, ru: Boolean) = when (w) {
    app.songling.data.Weather.CLEAR -> if (ru) "ясно" else "clear"
    app.songling.data.Weather.CLOUDY -> if (ru) "облачно" else "cloudy"
    app.songling.data.Weather.OVERCAST -> if (ru) "пасмурно" else "overcast"
    app.songling.data.Weather.RAIN -> if (ru) "дождь" else "rain"
    app.songling.data.Weather.SNOW -> if (ru) "снег" else "snow"
    app.songling.data.Weather.FOG -> if (ru) "туман" else "fog"
    app.songling.data.Weather.STORM -> if (ru) "гроза" else "storm"
}

/** Finds a town by name for the window's weather. */
@Composable
private fun TownSheet(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val ru = LocalRu.current
    val co = ctx.container
    val scope = rememberCoroutineScope()
    var q by remember { mutableStateOf("") }
    var found by remember { mutableStateOf<List<app.songling.data.Town>?>(null) }
    fun search() { if (q.isNotBlank()) scope.launch { found = withContext(Dispatchers.IO) { runCatching { app.songling.data.OutsideSource.towns(q.trim(), ru) }.getOrDefault(emptyList()) } } }
    app.songling.ui.components.Sheet(onDismiss = onClose) {
        Text(t("Город за окном", "The town outside"), style = T.Title)
        Text(t("Погода и восход с закатом будут как в этом городе. Наружу уходят только его координаты.", "The weather, sunrise and sunset will be this town's. Only its coordinates are sent."), style = T.Muted)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            app.songling.ui.components.Field(q, { q = it }, t("Название города", "Town name"), Modifier.weight(1f), onDone = ::search)
            app.songling.ui.components.Btn(t("Найти", "Find"), ::search, kind = Kind.AMBER, small = true)
        }
        found?.let { list ->
            if (list.isEmpty()) Text(t("Не нашлось.", "Nothing found."), style = T.Muted)
            for (tn in list) androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth().clip(R8.Md).clickable {
                co.write { settings.update { it.copy(town = tn.name, lat = tn.lat, lon = tn.lon, outside = "") } }
                co.refreshOutside(force = true)
                onClose()
            }.padding(vertical = 8.dp)) {
                Text(tn.name, style = T.Strong)
                if (tn.region.isNotBlank()) Text(tn.region, style = T.Small)
            }
        }
    }
}
