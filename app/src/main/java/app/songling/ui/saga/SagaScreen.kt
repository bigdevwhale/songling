package app.songling.ui.saga

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.songling.container
import app.songling.data.MIN
import app.songling.data.Source
import app.songling.domain.Filter
import app.songling.domain.Genre
import app.songling.domain.Goals
import app.songling.domain.Span
import app.songling.domain.Stats
import app.songling.domain.Studio
import app.songling.domain.Words
import app.songling.ui.LocalRu
import app.songling.ui.Nav
import app.songling.ui.Overlay
import app.songling.ui.components.Bar
import app.songling.ui.components.Bars
import app.songling.ui.components.Btn
import app.songling.ui.components.Card
import app.songling.ui.components.Chip
import app.songling.ui.components.Kind
import app.songling.ui.components.Section
import app.songling.ui.components.Stat
import app.songling.ui.components.Tabs
import app.songling.ui.components.TopRow
import app.songling.ui.components.t
import app.songling.ui.den.date
import app.songling.ui.theme.C
import app.songling.ui.theme.R8
import app.songling.ui.theme.T
import app.songling.ui.theme.paper
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun SagaScreen(st: Studio, nav: Nav) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        val fresh = remember(st) { FeatsPage.fresh(st) }
        Tabs(listOf(t("Статистика", "Stats"), t("Календарь", "Calendar"), t("Награды", "Awards")), page, { page = it }, Modifier.padding(horizontal = 16.dp),
            counts = listOf(null, null, fresh.size.takeIf { it > 0 }))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 104.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            app.songling.ui.components.FirstVisit(app.songling.ui.components.Tip.SAGA, st.s)
            // Postcards of all this, for a social feed.
            Row(Modifier.fillMaxWidth().clip(R8.Md).background(C.amberSoft).clickable { nav.go(Overlay.Cards) }.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t("Открытки для соцсетей", "Postcards to share"), style = T.Strong)
                    Text(t("Логово, питомец, итоги, награды – одной картинкой", "Your den, pet, stats and awards as a picture"), style = T.Small)
                }
                Text("›", style = T.Title.copy(color = C.amber))
            }
            when (page) {
                0 -> StatsPage(st, nav)
                1 -> CalendarPage(st, nav)
                else -> FeatsPage.Page(st)
            }
        }
    }
}

// ---------------------------------------------------------------- stats

@Composable
private fun StatsPage(st: Studio, nav: Nav) {
    val ru = LocalRu.current
    var f by remember { mutableStateOf(Filter(span = Span.entries.firstOrNull { it.name == st.s.statsPeriod } ?: Span.MONTH)) }
    var more by remember { mutableStateOf(false) }
    var topTab by rememberSaveable { mutableIntStateOf(0) }
    val s = remember(st, f) { Stats(st, f) }
    val spans = listOf(Span.WEEK, Span.MONTH, Span.YEAR, Span.LAST_YEAR, Span.ALL)
    val short = listOf(t("Неделя", "Week"), t("Месяц", "Month"), t("Год", "Year"), t("Прошлый", "Last yr"), t("Всё", "All"))
    app.songling.ui.components.Segmented(short, spans.indexOf(f.span), { f = f.copy(span = spans[it]) })
    val active = listOfNotNull(f.genre, f.own, f.app, f.source).size + if (f.headphones) 1 else 0
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            f.genre?.let { g -> app.songling.ui.shelf.Removable(Words.genre(g, ru), C.genre(g)) { f = f.copy(genre = null) } }
            f.own?.let { o -> st.owns[o]?.let { og -> app.songling.ui.shelf.Removable(og.name, androidx.compose.ui.graphics.Color(og.color)) { f = f.copy(own = null) } } }
            f.app?.let { a -> app.songling.ui.shelf.Removable(appName(a)) { f = f.copy(app = null) } }
            f.source?.let { src -> app.songling.ui.shelf.Removable(Words.source(src, ru)) { f = f.copy(source = null) } }
            if (f.headphones) app.songling.ui.shelf.Removable(t("в наушниках", "on headphones")) { f = f.copy(headphones = false) }
            if (active == 0) Text(t("вся музыка", "all music"), style = T.Small, modifier = Modifier.padding(start = 4.dp, top = 10.dp))
        }
        app.songling.ui.shelf.ToolButton("⚙", if (active > 0) "$active" else t("Фильтры", "Filters"), on = active > 0) { more = true }
    }
    if (more) app.songling.ui.Floating {
        app.songling.ui.components.Sheet(onDismiss = { more = false }) {
            Text(t("Что считать", "What to count"), style = T.Title)
            val genres = remember(st) { st.byGenre.keys.sortedByDescending { st.byGenre[it] } }
            if (st.s.own.isNotEmpty()) {
                app.songling.ui.components.Section(t("Свои жанры", "Your genres"))
                app.songling.ui.components.Flow { for (o in st.s.own) Chip(o.name, o.id == f.own, { f = f.copy(own = if (f.own == o.id) null else o.id) }, color = androidx.compose.ui.graphics.Color(o.color)) }
            }
            if (genres.isNotEmpty()) {
                app.songling.ui.components.Section(t("Жанр", "Genre"))
                app.songling.ui.components.Flow { for (g in genres) Chip(Words.genre(g, ru), g == f.genre, { f = f.copy(genre = if (f.genre == g) null else g) }, color = C.genre(g)) }
            }
            val apps = remember(st) { st.plays.map { it.app }.filter { it.isNotEmpty() }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key } }
            app.songling.ui.components.Section(t("Откуда", "Where from"))
            app.songling.ui.components.Flow {
                for (a in apps) Chip(appName(a), a == f.app, { f = f.copy(app = if (f.app == a) null else a, source = null) })
                for (src in listOf(Source.VINYL, Source.RADIO, Source.CONCERT, Source.IMPORT)) if (st.plays.any { it.source == src })
                    Chip(Words.source(src, ru), f.source == src, { f = f.copy(source = if (f.source == src) null else src, app = null) })
            }
            app.songling.ui.components.Section(t("Ещё", "More"))
            app.songling.ui.components.Flow { Chip(t("только в наушниках", "headphones only"), f.headphones, { f = f.copy(headphones = !f.headphones) }) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                Btn(t("Сбросить", "Clear"), { f = f.copy(genre = null, own = null, app = null, source = null, headphones = false) }, Modifier.weight(1f), kind = Kind.GHOST)
                Btn(t("Готово", "Done"), { more = false }, Modifier.weight(1f), kind = Kind.AMBER)
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Stat(Words.hours(s.ms, ru), t("музыки", "of music"), Modifier.weight(1f))
        Stat(Words.dur(s.perDay * MIN, ru), t("в среднем в день", "a day on average"), Modifier.weight(1f), C.lilac)
        Stat("${s.days}", t("дней с музыкой", "days with music"), Modifier.weight(1f), C.mint)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Stat("${s.whole}", t("альбомов целиком", "whole albums"), Modifier.weight(1f), C.mint)
        Stat("${s.newArtists}", t("новых артистов", "new artists"), Modifier.weight(1f), C.coral)
        Stat("${s.plays.size}", t("прослушиваний", "listens"), Modifier.weight(1f))
    }
    val series = s.series
    if (series.isNotEmpty()) Card {
        Text(if (f.span == Span.WEEK || f.span == Span.MONTH) t("По дням", "By day") else t("По месяцам", "By month"), style = T.Eyebrow)
        val loc = if (ru) Locale("ru") else Locale.ENGLISH
        val labels = series.mapIndexed { i, (d, _) ->
            when {
                s.monthly -> if (series.size <= 13 || d.monthValue == 1 || i == 0) d.month.getDisplayName(TextStyle.NARROW_STANDALONE, loc).let { if (d.monthValue == 1 || i == 0) "${it}’${d.year % 100}" else it } else ""
                f.span == Span.WEEK -> d.dayOfWeek.getDisplayName(TextStyle.SHORT, loc)
                i == 0 || i == series.size / 2 || i == series.size - 1 -> "${d.dayOfMonth} ${d.month.getDisplayName(TextStyle.SHORT, loc).trimEnd('.')}"
                else -> ""
            }
        }
        Bars(series.map { it.second }, labels, Modifier.padding(top = 8.dp), highlight = if (!s.monthly) series.size - 1 else -1, wide = !s.monthly && f.span != Span.WEEK,
            describe = { i ->
                val (d, ms) = series[i]
                val whenText = if (s.monthly) d.month.getDisplayName(TextStyle.FULL_STANDALONE, loc).replaceFirstChar { it.uppercase() } + " " + d.year
                    else "${d.dayOfMonth} ${d.month.getDisplayName(TextStyle.FULL, loc)}, ${d.dayOfWeek.getDisplayName(TextStyle.SHORT, loc)}"
                "$whenText – " + if (ms == 0L) Words.t(ru, "без музыки", "no music") else Words.dur(ms, ru)
            })
    }
    Section(t("Больше всего", "Top"))
    Tabs(listOf(t("Артисты", "Artists"), t("Альбомы", "Albums"), t("Песни", "Songs"), t("Жанры", "Genres")), topTab, { topTab = it })
    var all by remember(topTab, f) { mutableStateOf(false) }
    when (topTab) {
        0, 1, 2 -> {
            val rows = remember(s, topTab) { when (topTab) { 0 -> s.top({ it.artist }, st::artistName, n = 50); 1 -> s.albums.let { s.top({ it.album }, st::albumTitle, { k -> st.albums[k]?.artistName ?: st.byAlbum[k]?.artistName.orEmpty() }, n = 50) }; else -> s.top({ it.track }, st::trackTitle, { k -> st.byTrack[k]?.artistName.orEmpty() }, n = 50) } }
            val top = rows.firstOrNull()?.ms?.coerceAtLeast(1) ?: 1
            if (rows.isEmpty()) Text(t("Пока пусто.", "Nothing yet."), style = T.Muted)
            rows.take(if (all) 50 else 10).forEachIndexed { i, r ->
                val al = when (topTab) { 1 -> st.albums[r.key]; 2 -> st.albums[st.tracks[r.key]?.album ?: ""]; else -> null }
                TopRow(i + 1, r.name, listOf(r.sub, Words.plays(r.plays, ru)).filter { it.isNotEmpty() }.joinToString(" · "), Words.dur(r.ms, ru), r.ms / top.toFloat(),
                    cover = al?.cover, genre = al?.genre, showCover = topTab != 0, onClick = {
                        nav.go(when (topTab) { 0 -> Overlay.ArtistOf(r.key); 1 -> Overlay.AlbumOf(r.key); else -> Overlay.TrackOf(r.key) })
                    })
            }
            if (rows.size > 10) Btn(if (all) t("Свернуть", "Less") else t("Показать 50", "Show 50"), { all = !all }, Modifier.fillMaxWidth(), kind = Kind.GHOST, small = true)
        }
        else -> {
            val mine = s.ownGenres
            if (mine.isNotEmpty()) {
                val topMine = mine.first().second.coerceAtLeast(1)
                Text(t("Свои", "Your own"), style = T.Eyebrow)
                mine.forEachIndexed { i, (o, ms) -> TopRow(i + 1, o.name, Words.genre(Genre.entries.firstOrNull { it.name == o.base } ?: Genre.OTHER, ru), Words.dur(ms, ru), ms / topMine.toFloat()) }
                Text(t("Все жанры", "All genres"), style = T.Eyebrow)
            }
            val g = s.genres
            val total = g.sumOf { it.second }.coerceAtLeast(1)
            GenreRing(g)
            g.forEachIndexed { i, (genre, ms) -> TopRow(i + 1, Words.genre(genre, ru), "${(ms * 100 / total)}%", Words.dur(ms, ru), ms / g.first().second.toFloat(), genre = genre) }
        }
    }
    Section(t("Когда ты слушаешь", "When you listen"))
    Card {
        Text(t("По часам", "By hour"), style = T.Eyebrow)
        val h = s.hours
        Bars(h.toList(), (0 until 24).map { "$it" }, Modifier.padding(top = 8.dp), color = C.lilac, every = 3, highlight = s.peakHour ?: -1,
            describe = { i -> "$i:00–${(i + 1) % 24}:00 – " + Words.dur(h[i], ru) })
    }
    Card {
        Text(t("По дням недели", "By weekday"), style = T.Eyebrow)
        val w = s.weekdays
        Bars(w.toList(), (1..7).map { DayOfWeek.of(it).getDisplayName(TextStyle.SHORT, if (ru) Locale("ru") else Locale.ENGLISH) }, Modifier.padding(top = 8.dp), color = C.mint,
            highlight = s.favouriteWeekday?.let { it.value - 1 } ?: -1,
            describe = { i -> DayOfWeek.of(i + 1).getDisplayName(TextStyle.FULL_STANDALONE, if (ru) Locale("ru") else Locale.ENGLISH).replaceFirstChar { it.uppercase() } + " – " + Words.dur(w[i], ru) })
    }
    val countries = s.countries
    if (countries.isNotEmpty()) {
        Section(t("Страны", "Countries"), "${countries.size}")
        val top = countries.first().second
        countries.take(10).forEachIndexed { i, (c, ms) -> TopRow(i + 1, Words.flag(c) + " " + Words.country(c, ru), "", Words.dur(ms, ru), ms / top.toFloat()) }
    }
    val decades = s.decades
    if (decades.isNotEmpty()) {
        Section(t("Десятилетия", "Decades"))
        Card { Bars(decades.map { it.second }, decades.map { "’${(it.first % 100).toString().padStart(2, '0')}" }, color = C.coral,
            describe = { i -> Words.t(ru, "${decades[i].first}-е", "${decades[i].first}s") + " – " + Words.dur(decades[i].second, ru) }) }
    }
    val apps = s.apps
    if (apps.size > 1) {
        Section(t("Откуда", "Where from"))
        val top = apps.first().second
        apps.take(6).forEachIndexed { i, (a, ms) -> TopRow(i + 1, Source.entries.firstOrNull { it.name == a }?.let { Words.source(it, ru) } ?: appName(a), "", Words.dur(ms, ru), ms / top.toFloat()) }
    }
    Section(t("Рекорды", "Records"))
    Card {
        s.longest?.let { Text(t("Самая долгая сессия: ${Words.dur(it.ms, true)}, ${date(it.from, true)}", "Longest sitting: ${Words.dur(it.ms, false)}, ${date(it.from, false)}"), style = T.Body) }
        s.busiestDay?.let { Text(t("Самый музыкальный день: ${date(st.startOf(it.first), true)} – ${Words.dur(it.second, true)}", "Busiest day: ${date(st.startOf(it.first), false)} – ${Words.dur(it.second, false)}"), style = T.Body) }
        Text(t("Лучшая серия: ${st.bestStreak} дн. подряд, сейчас – ${st.streak}", "Best streak: ${st.bestStreak} days, now ${st.streak}"), style = T.Body)
        s.peakHour?.let { Text(t("Чаще всего слушаешь около $it:00", "You listen most around $it:00"), style = T.Body) }
        Text(t("Пропущено песен: ${s.skips}", "Songs skipped: ${s.skips}"), style = T.Body)
        Text(t("Всего за всё время: ${Words.hours(st.totalMs, true)}, ${st.byArtist.size} артистов", "All time: ${Words.hours(st.totalMs, false)}, ${st.byArtist.size} artists"), style = T.Small)
    }
}

@Composable
private fun GenreRing(g: List<Pair<Genre, Long>>) {
    val total = g.sumOf { it.second }.toFloat().coerceAtLeast(1f)
    Canvas(Modifier.fillMaxWidth().aspectRatio(3.2f)) {
        val d = size.height * .9f
        var a = -90f
        for ((genre, ms) in g) {
            val sweep = ms / total * 360f
            drawArc(Color(genre.argb), a, sweep, false, Offset((size.width - d) / 2, (size.height - d) / 2), Size(d, d), style = androidx.compose.ui.graphics.drawscope.Stroke(d * .16f))
            a += sweep
        }
    }
}

private fun appName(pkg: String): String = when {
    pkg.contains("spotify") -> "Spotify"
    pkg.contains("yandex.music") -> "Яндекс Музыка"
    pkg.contains("youtube.music") || pkg.contains("apps.youtube.music") -> "YouTube Music"
    pkg.contains("youtube") -> "YouTube"
    pkg.contains("vkontakte") || pkg.contains("vk.") -> "VK"
    pkg.contains("deezer") -> "Deezer"
    pkg.contains("soundcloud") -> "SoundCloud"
    pkg.contains("apple") -> "Apple Music"
    pkg.contains("tidal") -> "Tidal"
    pkg.contains("zvuk") || pkg.contains("sber") -> "Звук"
    pkg.contains("miui.player") || pkg.contains("xiaomi") -> "Mi Music"
    pkg.contains("bandcamp") -> "Bandcamp"
    pkg.contains("poweramp") -> "Poweramp"
    else -> pkg.substringAfterLast('.')
}

// ---------------------------------------------------------------- goals

/** Goals get a tab of their own. */
@Composable
fun GoalsScreen(st: Studio, nav: Nav) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 104.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(t("Цели", "Goals"), style = T.Title)
        app.songling.ui.components.FirstVisit(app.songling.ui.components.Tip.GOALS, st.s)
        GoalsPage(st, nav)
    }
}

@Composable
private fun GoalsPage(st: Studio, nav: Nav) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    Btn(t("+ Новая цель", "+ New goal"), { nav.go(Overlay.GoalEdit()) }, Modifier.fillMaxWidth(), kind = Kind.AMBER)
    val goals = st.goals.filter { !it.archived }
    if (goals.isEmpty()) {
        Card {
            Text(t("Цели – по альбомам, артистам, песням и не только", "Goals – albums, artists, songs and more"), style = T.Name)
            Text(t("Например: «пять альбомов целиком за месяц», «10 часов Radiohead в этом году», «20 новых артистов», «музыка трёх десятилетий». Разовая цель приносит 15 медиаторов.",
                "For example: «five whole albums a month», «10 hours of Radiohead this year», «20 new artists», «music from three decades». A one-off goal pays 15 plectrums."), style = T.Muted)
        }
        Section(t("Быстро", "Quick"))
        for (d in Presets.all(ru)) Row(Modifier.fillMaxWidth().paper(16.dp).clip(R8.Md).background(C.surface).clickable { co.write { db.dao().insert(d) } }.padding(12.dp)) {
            Text(GoalText.title(st, d, ru), style = T.Body, modifier = Modifier.weight(1f))
            Text("+", style = T.Strong.copy(color = C.amber))
        }
    }
    for (g in goals) {
        val p = Goals.progress(st, g)
        Column(Modifier.fillMaxWidth().clip(R8.Md).background(C.surface).border(1.dp, if (p.done) C.mint else C.line, R8.Md).clickable { nav.go(Overlay.GoalEdit(g.id)) }.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(GoalText.title(st, g, ru), style = T.Strong, modifier = Modifier.weight(1f))
                if (p.done) Text(t("готово", "done"), style = T.Small.copy(color = C.mint))
            }
            Bar(p.part, Modifier.fillMaxWidth(), if (p.done) C.mint else C.amber)
            Text(GoalText.progress(g, p, ru) + " · " + Words.period(g.period, ru), style = T.Small)
        }
    }
    val archived = st.goals.filter { it.archived }
    if (archived.isNotEmpty()) {
        Section(t("В архиве", "Archived"), "${archived.size}")
        for (g in archived) Text(GoalText.title(st, g, ru), style = T.Muted, modifier = Modifier.clickable { nav.go(Overlay.GoalEdit(g.id)) }.padding(vertical = 4.dp))
    }
}

// ---------------------------------------------------------------- calendar

@Composable
private fun CalendarPage(st: Studio, nav: Nav) {
    val ru = LocalRu.current
    val years = remember(st) { (st.byDay.keys.map { it.year } + st.today.year).toSet().sortedDescending() }
    var year by rememberSaveable { mutableIntStateOf(st.today.year) }
    var picked by remember { mutableStateOf<LocalDate?>(null) }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (y in years) Chip("$y", y == year, { year = y; picked = null })
    }
    val first = LocalDate.of(year, 1, 1)
    val days = first.lengthOfYear()
    val total = (0 until days).sumOf { st.byDay[first.plusDays(it.toLong())] ?: 0L }
    Text(t("За $year: ${Words.hours(total, true)}, ${(0 until days).count { first.plusDays(it.toLong()) in st.musicDays }} дней с музыкой",
        "In $year: ${Words.hours(total, false)}, ${(0 until days).count { first.plusDays(it.toLong()) in st.musicDays }} days with music"), style = T.Muted)
    // Months as rows of little squares, one per day.
    val max = (0 until days).maxOfOrNull { st.byDay[first.plusDays(it.toLong())] ?: 0L }?.coerceAtLeast(1) ?: 1
    for (m in 1..12) {
        val start = LocalDate.of(year, m, 1)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(start.month.getDisplayName(TextStyle.SHORT_STANDALONE, if (ru) Locale("ru") else Locale.ENGLISH), style = T.Small, modifier = Modifier.padding(end = 6.dp).fillMaxWidth(0.12f))
            Canvas(Modifier.weight(1f).aspectRatio(31 / 1.25f).pointerInput(year, m) {
                detectTapGestures { o -> val d = (o.x / (size.width / 31f)).toInt() + 1; if (d <= start.lengthOfMonth()) picked = start.withDayOfMonth(d) }
            }) {
                val w = size.width / 31f
                for (d in 1..start.lengthOfMonth()) {
                    val day = start.withDayOfMonth(d)
                    val v = st.byDay[day] ?: 0L
                    val k = (v.toFloat() / max).coerceIn(0f, 1f)
                    val col = if (v == 0L) C.surface2 else Color(0xFFF5B841).copy(alpha = .25f + .75f * k)
                    drawRoundRect(col, Offset((d - 1) * w + w * .1f, size.height * .1f), Size(w * .8f, size.height * .8f), CornerRadius(w * .2f))
                    if (day == picked || day == st.today) drawRoundRect(if (day == picked) C.coral else C.ink, Offset((d - 1) * w + w * .1f, size.height * .1f), Size(w * .8f, size.height * .8f), CornerRadius(w * .2f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
                }
            }
        }
    }
    picked?.let { d ->
        val from = st.startOf(d); val to = st.startOf(d.plusDays(1))
        val ps = st.plays.filter { it.at in from until to }
        Card {
            Text(date(from, ru), style = T.Name)
            Text(t("${Words.dur(ps.sumOf { it.ms }, true)}, ${Words.plays(ps.size, true)}", "${Words.dur(ps.sumOf { it.ms }, false)}, ${Words.plays(ps.size, false)}"), style = T.Small)
            val albums = st.wholeRuns.filter { it.second in from until to }.map { it.first }.distinct()
            if (albums.isNotEmpty()) Text(t("Целиком: ", "Whole: ") + albums.joinToString { "«" + st.albumTitle(it) + "»" }, style = T.Body.copy(color = C.mint))
            for ((k, l) in ps.groupBy { it.artist }.entries.sortedByDescending { e -> e.value.sumOf { it.ms } }.take(8))
                Text("${st.artistName(k)} – ${Words.dur(l.sumOf { it.ms }, ru)}", style = T.Body, modifier = Modifier.clickable { nav.go(Overlay.ArtistOf(k)) })
        }
    }
}
