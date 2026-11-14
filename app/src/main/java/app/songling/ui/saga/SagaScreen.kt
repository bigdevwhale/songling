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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
        Tabs(listOf(t("Статистика", "Stats"), t("Календарь", "Calendar"), t("История", "History"), t("Награды", "Awards")), page, { page = it }, Modifier.padding(horizontal = 16.dp),
            counts = listOf(null, null, null, fresh.size.takeIf { it > 0 }))
        // The history is a long list of its own, so it isn't put in the page's scroll.
        if (page == 2) { app.songling.ui.shelf.History(st, nav, Modifier.padding(horizontal = 16.dp)); return@Column }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 104.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            app.songling.ui.components.FirstVisit(app.songling.ui.components.Tip.SAGA, st.s)
            // Postcards of all this, for a social feed.
            when (page) {
                0 -> StatsPage(st, nav)
                1 -> CalendarPage(st, nav)
                3 -> FeatsPage.Page(st)
                else -> {}
            }
            // Postcards of all this, for a social feed: at the foot, out of the numbers' way.
            app.songling.ui.components.LinkRow(t("Открытки для соцсетей", "Postcards to share"), t("Логово, питомец, итоги, награды – одной картинкой", "Your den, pet, stats and awards as a picture"),
                { nav.go(Overlay.Cards) }, Modifier.padding(top = 6.dp))
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
    val short = listOf(t("Неделя", "Week"), t("Месяц", "Month"), t("Год", "Year"), "${st.today.year - 1}", t("Всё", "All"))
    val active = listOfNotNull(f.genre, f.own, f.app, f.source).size + if (f.headphones) 1 else 0
    // The span and the filters in one line; what's filtered shows under it only when something is.
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        app.songling.ui.components.Segmented(short, spans.indexOf(f.span), { f = f.copy(span = spans[it]) }, Modifier.weight(1f))
        app.songling.ui.shelf.ToolButton(app.songling.ui.components.Glyph.FILTER, if (active > 0) "$active" else null, on = active > 0, describe = t("Фильтры", "Filters")) { more = true }
    }
    if (active > 0) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        run {
            f.genre?.let { g -> app.songling.ui.shelf.Removable(Words.genre(g, ru), C.genre(g)) { f = f.copy(genre = null) } }
            f.own?.let { o -> st.owns[o]?.let { og -> app.songling.ui.shelf.Removable(og.name, androidx.compose.ui.graphics.Color(og.color)) { f = f.copy(own = null) } } }
            f.app?.let { a -> app.songling.ui.shelf.Removable(appName(a)) { f = f.copy(app = null) } }
            f.source?.let { src -> app.songling.ui.shelf.Removable(Words.source(src, ru)) { f = f.copy(source = null) } }
            if (f.headphones) app.songling.ui.shelf.Removable(t("в наушниках", "on headphones")) { f = f.copy(headphones = false) }
        }
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
    // Tiles of one row stand as tall as the tallest.
    Row(Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Stat(Words.hours(s.ms, ru), t("музыки", "of music"), Modifier.weight(1f).fillMaxHeight())
        Stat(Words.dur(s.perDay * MIN, ru), t("в среднем в день", "a day on average"), Modifier.weight(1f).fillMaxHeight())
        Stat("${s.days}", t("дней с музыкой", "days with music"), Modifier.weight(1f).fillMaxHeight())
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
            val rows = remember(s, topTab) { when (topTab) { 0 -> s.topArtists(50); 1 -> s.albums.let { s.top({ it.album }, st::albumTitle, { k -> st.albums[k]?.artistName ?: st.byAlbum[k]?.artistName.orEmpty() }, n = 50) }; else -> s.top({ it.track }, st::trackTitle, { k -> st.byTrack[k]?.artistName.orEmpty() }, n = 50) } }
            val top = rows.firstOrNull()?.ms?.coerceAtLeast(1) ?: 1
            if (rows.isEmpty()) app.songling.ui.components.Empty(t("За это время – тишина", "Nothing in this time"), t("Выбери срок подольше или сними фильтры.", "Pick a longer span, or clear the filters."))
            rows.take(if (all) 50 else 5).forEachIndexed { i, r ->
                val al = when (topTab) { 1 -> st.albums[r.key]; 2 -> st.albums[st.tracks[r.key]?.album ?: ""]; else -> null }
                TopRow(i + 1, r.name, listOf(r.sub, Words.plays(r.plays, ru)).filter { it.isNotEmpty() }.joinToString(" · "), Words.dur(r.ms, ru), r.ms / top.toFloat(),
                    cover = al?.cover, genre = al?.genre, showCover = topTab != 0, onClick = {
                        nav.go(when (topTab) { 0 -> Overlay.ArtistOf(r.key); 1 -> Overlay.AlbumOf(r.key); else -> Overlay.TrackOf(r.key) })
                    })
            }
            if (rows.size > 5) Btn(if (all) t("Свернуть", "Less") else t("Показать ещё", "Show more"), { all = !all }, Modifier.fillMaxWidth(), kind = Kind.GHOST, small = true)
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
    // One card, two views of it, instead of two charts in a row.
    var whenTab by rememberSaveable { mutableIntStateOf(0) }
    Card {
        app.songling.ui.components.Segmented(listOf(t("По часам", "By hour"), t("По дням недели", "By weekday")), whenTab, { whenTab = it })
        if (whenTab == 0) {
        val h = s.hours
        Bars(h.toList(), (0 until 24).map { "$it" }, Modifier.padding(top = 8.dp), color = C.lilac, every = 3, highlight = s.peakHour ?: -1,
            describe = { i -> "$i:00–${(i + 1) % 24}:00 – " + Words.dur(h[i], ru) })
        } else {
        val w = s.weekdays
        Bars(w.toList(), (1..7).map { DayOfWeek.of(it).getDisplayName(TextStyle.SHORT, if (ru) Locale("ru") else Locale.ENGLISH) }, Modifier.padding(top = 8.dp), color = C.mint,
            highlight = s.favouriteWeekday?.let { it.value - 1 } ?: -1,
            describe = { i -> DayOfWeek.of(i + 1).getDisplayName(TextStyle.FULL_STANDALONE, if (ru) Locale("ru") else Locale.ENGLISH).replaceFirstChar { it.uppercase() } + " – " + Words.dur(w[i], ru) })
        }
    }
    val countries = s.countries
    if (countries.isNotEmpty()) {
        Section(t("Страны", "Countries"), "${countries.size}")
        val top = countries.first().second
        countries.take(5).forEachIndexed { i, (c, ms) -> TopRow(i + 1, Words.flag(c) + " " + Words.country(c, ru), "", Words.dur(ms, ru), ms / top.toFloat()) }
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
        apps.take(4).forEachIndexed { i, (a, ms) -> TopRow(i + 1, Source.entries.firstOrNull { it.name == a }?.let { Words.source(it, ru) } ?: appName(a), "", Words.dur(ms, ru), ms / top.toFloat()) }
    }
    Section(t("В цифрах", "In numbers"))
    Card(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 10.dp)) {
        val fact = @Composable { label: String, value: String -> app.songling.ui.components.Fact(label, value) }
        fact(t("Прослушиваний", "Listens"), Words.num(s.plays.size)); app.songling.ui.components.Rule()
        fact(t("Альбомов целиком", "Whole albums"), Words.num(s.whole)); app.songling.ui.components.Rule()
        fact(t("Новых артистов", "New artists"), Words.num(s.newArtists)); app.songling.ui.components.Rule()
        s.longest?.let { fact(t("Самая долгая сессия", "Longest sitting"), "${Words.dur(it.ms, ru)} · ${date(it.from, ru)}"); app.songling.ui.components.Rule() }
        s.busiestDay?.let { fact(t("Самый музыкальный день", "Busiest day"), "${Words.dur(it.second, ru)} · ${date(st.startOf(it.first), ru)}"); app.songling.ui.components.Rule() }
        fact(t("Лучшая серия дней", "Best streak of days"), t("${st.bestStreak}, сейчас ${st.streak}", "${st.bestStreak}, now ${st.streak}")); app.songling.ui.components.Rule()
        s.peakHour?.let { fact(t("Чаще всего слушаешь", "You listen most"), t("около $it:00", "around $it:00")); app.songling.ui.components.Rule() }
        fact(t("Пропущено песен", "Songs skipped"), Words.num(s.skips)); app.songling.ui.components.Rule()
        fact(t("За всё время", "All time"), t("${Words.hours(st.totalMs, true)} · артистов: ${st.byArtist.size}", "${Words.hours(st.totalMs, false)} · ${st.byArtist.size} artists"))
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
        val live = st.goals.filter { !it.archived }
        // The title, how it stands, and the one thing to do here.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Text(t("Цели", "Goals"), style = T.Title)
                if (live.isNotEmpty()) Text(t("готово ${live.count { Goals.progress(st, it).done }} из ${live.size}", "${live.count { Goals.progress(st, it).done }} of ${live.size} done"), style = T.Small)
            }
            Btn(t("+ Новая", "+ New"), { nav.go(Overlay.GoalEdit()) }, kind = Kind.AMBER, small = true)
        }
        app.songling.ui.components.FirstVisit(app.songling.ui.components.Tip.GOALS, st.s)
        GoalsPage(st, nav)
    }
}

@Composable
private fun GoalsPage(st: Studio, nav: Nav) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    val goals = st.goals.filter { !it.archived }
    if (goals.isEmpty()) {
        Card {
            Text(t("Цели – по альбомам, артистам, песням и не только", "Goals – albums, artists, songs and more"), style = T.Name)
            Text(t("Например: «пять альбомов целиком за месяц», «10 часов Radiohead в этом году», «20 новых артистов», «музыка трёх десятилетий». Разовая цель приносит 15 медиаторов.",
                "For example: «five whole albums a month», «10 hours of Radiohead this year», «20 new artists», «music from three decades». A one-off goal pays 15 plectrums."), style = T.Muted)
        }
        Section(t("Быстро", "Quick"))
        for (d in Presets.all(ru).take(4)) Row(Modifier.fillMaxWidth().paper(16.dp).clip(R8.Md).background(C.surface).clickable { co.write { db.dao().insert(d) } }.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Text(GoalText.title(st, d, ru), style = T.Strong)
                Text(Words.period(d.period, ru), style = T.Small)
            }
            androidx.compose.foundation.layout.Box(Modifier.clip(androidx.compose.foundation.shape.CircleShape).background(C.amberSoft).padding(8.dp)) {
                app.songling.ui.components.Sign(app.songling.ui.components.Glyph.PLUS, color = C.amber, size = 12.dp)
            }
        }
    }
    for (g in goals) {
        val p = Goals.progress(st, g)
        Column(Modifier.fillMaxWidth().paper(16.dp).clip(R8.Md).background(C.surface).then(if (p.done) Modifier.border(1.5.dp, C.mint, R8.Md) else Modifier)
            .clickable { nav.go(Overlay.GoalEdit(g.id)) }.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(GoalText.title(st, g, ru), style = T.Strong, modifier = Modifier.weight(1f))
                // Done, or how far along.
                if (p.done) Text(t("готово", "done"), style = T.Small.copy(color = androidx.compose.ui.graphics.Color.White),
                    modifier = Modifier.clip(androidx.compose.foundation.shape.CircleShape).background(C.mint).padding(horizontal = 9.dp, vertical = 2.dp))
                else Text("${(p.part * 100).toInt()}%", style = T.Small.copy(color = C.amber))
            }
            Bar(p.part, Modifier.fillMaxWidth(), if (p.done) C.mint else C.amber)
            Text(GoalText.progress(g, p, ru) + " · " + Words.period(g.period, ru), style = T.Small)
        }
    }
    val archived = st.goals.filter { it.archived }
    if (archived.isNotEmpty()) {
        Section(t("В архиве", "Archived"), "${archived.size}")
        for (g in archived) Text(GoalText.title(st, g, ru), style = T.Muted, modifier = Modifier.fillMaxWidth().clip(R8.Sm).clickable { nav.go(Overlay.GoalEdit(g.id)) }.padding(vertical = 10.dp, horizontal = 4.dp))
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
            Text(start.month.getDisplayName(TextStyle.SHORT_STANDALONE, if (ru) Locale("ru") else Locale.ENGLISH).trimEnd('.'), style = T.Small, maxLines = 1, modifier = Modifier.padding(end = 6.dp).fillMaxWidth(0.12f))
            Canvas(Modifier.weight(1f).aspectRatio(31 / 1.25f).pointerInput(year, m) {
                detectTapGestures { o -> val d = (o.x / (size.width / 31f)).toInt() + 1; if (d <= start.lengthOfMonth()) picked = start.withDayOfMonth(d) }
            }) {
                val w = size.width / 31f
                for (d in 1..start.lengthOfMonth()) {
                    val day = start.withDayOfMonth(d)
                    val v = st.byDay[day] ?: 0L
                    val k = (v.toFloat() / max).coerceIn(0f, 1f)
                    val col = if (v == 0L) C.track else C.honey.copy(alpha = .28f + .72f * k)
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
                Row(Modifier.fillMaxWidth().clip(R8.Sm).clickable { nav.go(Overlay.ArtistOf(k)) }.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(st.artistName(k), style = T.Body, modifier = Modifier.weight(1f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Text(Words.dur(l.sumOf { it.ms }, ru), style = T.Small)
                }
        }
    }
}
