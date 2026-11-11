package app.songling.ui.shelf

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import app.songling.container
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import app.songling.ui.components.Flow
import app.songling.ui.components.Section
import app.songling.ui.components.Sheet
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.songling.data.Keys
import app.songling.data.Kind
import app.songling.domain.Genre
import app.songling.domain.Studio
import app.songling.domain.Words
import app.songling.ui.LocalRu
import app.songling.ui.Nav
import app.songling.ui.Overlay
import app.songling.ui.components.Btn
import app.songling.ui.components.Chip
import app.songling.ui.components.Cover
import app.songling.ui.components.Field
import app.songling.ui.components.Stars
import app.songling.ui.components.Tabs
import app.songling.ui.components.t
import app.songling.ui.den.date
import app.songling.ui.theme.C
import app.songling.ui.theme.R8
import app.songling.ui.theme.T
import app.songling.ui.theme.paper

enum class ShelfTab { ALBUMS, SINGLES, SONGS, ARTISTS, WISH, HISTORY }
enum class Sort { MINUTES, RECENT, PLAYS, TITLE, YEAR, STARS, ADDED }

/** One line on the shelf, whatever it is. */
data class Entry(
    val key: String, val kind: ShelfTab, val title: String, val sub: String, val cover: String?, val genre: Genre?, val year: Int?,
    val ms: Long, val plays: Int, val last: Long, val stars: Int, val whole: Int, val wish: Boolean, val added: Long, val country: String = "",
    /** A genre of one's own, its id. */
    val own: String = "",
)

fun entries(st: Studio, tab: ShelfTab): List<Entry> = when (tab) {
    ShelfTab.ALBUMS, ShelfTab.SINGLES -> st.shelfAlbums.toSet().mapNotNull { k ->
        val a = st.albums[k]; val tl = st.byAlbum[k]
        val single = a?.kind == Kind.SINGLE
        if ((tab == ShelfTab.SINGLES) != single) return@mapNotNull null
        Entry(k, tab, a?.title ?: tl?.name ?: k, a?.artistName ?: tl?.artistName.orEmpty(), a?.cover, a?.genre ?: st.artists[a?.artist ?: ""]?.genre, a?.year,
            tl?.ms ?: 0, tl?.plays ?: 0, tl?.last ?: 0, a?.stars ?: 0, tl?.whole ?: 0, a?.wish == true, a?.added ?: tl?.first ?: 0, own = a?.own.orEmpty())
    }
    ShelfTab.SONGS -> st.shelfTracks.toSet().map { k ->
        val tr = st.tracks[k]; val tl = st.byTrack[k]
        val al = st.albums[tr?.album ?: ""]
        Entry(k, tab, tr?.title ?: tl?.name ?: k, tr?.artistName ?: tl?.artistName.orEmpty(), al?.cover, tr?.genre ?: al?.genre, tr?.year ?: al?.year,
            tl?.ms ?: 0, tl?.plays ?: 0, tl?.last ?: 0, tr?.stars ?: 0, 0, tr?.wish == true, tr?.added ?: tl?.first ?: 0, own = tr?.own?.ifEmpty { null } ?: al?.own.orEmpty())
    }
    ShelfTab.ARTISTS -> st.shelfArtists.toSet().map { k ->
        val a = st.artists[k]; val tl = st.byArtist[k]
        val top = tl?.albums?.maxByOrNull { st.byAlbum[it]?.ms ?: 0 }
        Entry(k, tab, st.artistName(k), "", st.albums[top ?: ""]?.cover, a?.genre ?: st.albums[top ?: ""]?.genre, null,
            tl?.ms ?: 0, tl?.plays ?: 0, tl?.last ?: 0, a?.stars ?: 0, 0, a?.wish == true, a?.added ?: tl?.first ?: 0, a?.country.orEmpty(), own = a?.own.orEmpty())
    }
    ShelfTab.WISH -> (entries(st, ShelfTab.ALBUMS) + entries(st, ShelfTab.SINGLES) + entries(st, ShelfTab.SONGS) + entries(st, ShelfTab.ARTISTS)).filter { it.wish }
    ShelfTab.HISTORY -> emptyList()
}

@Composable
fun ShelfScreen(st: Studio, nav: Nav) {
    val ru = LocalRu.current
    var tab by rememberSaveable { mutableStateOf(ShelfTab.ALBUMS) }
    var q by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(Sort.MINUTES) }
    var genres by remember { mutableStateOf(emptySet<Genre>()) }
    var owns by remember { mutableStateOf(emptySet<String>()) }
    var decades by remember { mutableStateOf(emptySet<Int>()) }
    var rated by rememberSaveable { mutableStateOf(false) }
    var wholeOnly by rememberSaveable { mutableStateOf(false) }
    var wishOnly by rememberSaveable { mutableStateOf(false) }
    var sheet by remember { mutableStateOf(0) }

    val all = remember(st, tab) { entries(st, tab) }
    val nq = Keys.norm(q)
    fun pass(e: Entry, gs: Set<Genre>, ds: Set<Int>, r: Boolean, w: Boolean, wi: Boolean) =
        (nq.isEmpty() || Keys.norm(e.title).contains(nq) || Keys.norm(e.sub).contains(nq)) &&
            (gs.isEmpty() || e.genre in gs) && (owns.isEmpty() || e.own in owns) && (ds.isEmpty() || e.year?.let { it / 10 * 10 } in ds) &&
            (!r || e.stars > 0) && (!w || e.whole > 0) && (!wi || e.wish)
    val list = remember(all, nq, sort, genres, owns, decades, rated, wholeOnly, wishOnly) {
        all.filter { pass(it, genres, decades, rated, wholeOnly, wishOnly) }.let { l ->
            when (sort) {
                Sort.MINUTES -> l.sortedByDescending { it.ms }
                Sort.RECENT -> l.sortedByDescending { it.last }
                Sort.PLAYS -> l.sortedByDescending { it.plays }
                Sort.TITLE -> l.sortedBy { it.title.lowercase() }
                Sort.YEAR -> l.sortedByDescending { it.year ?: 0 }
                Sort.STARS -> l.sortedByDescending { it.stars * 1_000_000_000L + it.ms / 1000 }
                Sort.ADDED -> l.sortedByDescending { it.added }
            }
        }
    }
    val counts = remember(st) { ShelfTab.entries.map { if (it == ShelfTab.HISTORY) null else entries(st, it).size } }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        val names = listOf(t("Альбомы", "Albums"), t("Синглы", "Singles"), t("Песни", "Songs"), t("Артисты", "Artists"), t("Хочу послушать", "To hear"), t("История", "History"))
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (tb in ShelfTab.entries) {
                val n = counts[tb.ordinal]
                Chip(names[tb.ordinal] + if (n != null && n > 0) " $n" else "", tb == tab, { tab = tb }, color = if (tb == tab) C.lilac else null)
            }
        }
        app.songling.ui.components.FirstVisit(app.songling.ui.components.Tip.SHELF, st.s, Modifier.padding(top = 8.dp))
        if (tab == ShelfTab.HISTORY) { History(st, nav); return@Column }
        val active = genres.size + owns.size + decades.size + listOf(rated, wholeOnly, wishOnly).count { it }
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field(q, { q = it }, t("Найти на полке", "Search the shelf"), Modifier.weight(1f))
            ToolButton("⇅", sortShort(sort, ru)) { sheet = 1 }
            ToolButton("⚙", if (active > 0) "$active" else t("Фильтры", "Filters"), on = active > 0) { sheet = 2 }
        }
        // What's on, each with a cross.
        if (active > 0) Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (g in genres) Removable(Words.genre(g, ru), C.genre(g)) { genres = genres - g }
            for (o in owns) st.owns[o]?.let { og -> Removable(og.name, androidx.compose.ui.graphics.Color(og.color)) { owns = owns - o } }
            for (d in decades) Removable(if (ru) "${d}-е" else "${d}s") { decades = decades - d }
            if (rated) Removable(t("со звёздами", "rated")) { rated = false }
            if (wholeOnly) Removable(t("слушал целиком", "heard whole")) { wholeOnly = false }
            if (wishOnly) Removable(t("хочу послушать", "to hear")) { wishOnly = false }
            Text(t("сбросить", "clear"), style = T.Small.copy(color = C.coral), modifier = Modifier.clip(CircleShape).clickable {
                genres = emptySet(); owns = emptySet(); decades = emptySet(); rated = false; wholeOnly = false; wishOnly = false
            }.padding(horizontal = 10.dp, vertical = 8.dp))
        }
        Text(if (ru) "${list.size} из ${all.size}" else "${list.size} of ${all.size}", style = T.Small, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
        if (sheet == 1) app.songling.ui.Floating { Sheet(onDismiss = { sheet = 0 }) {
            Text(t("Сортировка", "Sort"), style = T.Title)
            for (so in Sort.entries) Row(Modifier.fillMaxWidth().clip(R8.Md).clickable { sort = so; sheet = 0 }.padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(20.dp).clip(CircleShape).background(if (so == sort) C.amber else C.surface2), contentAlignment = Alignment.Center) {
                    if (so == sort) Box(Modifier.size(8.dp).clip(CircleShape).background(C.surface))
                }
                Text(sortName(so, ru).replaceFirstChar { it.uppercase() }, style = T.Body)
            }
        } }
        if (sheet == 2) app.songling.ui.Floating { Sheet(onDismiss = { sheet = 0 }) {
            Text(t("Фильтры", "Filters"), style = T.Title)
            val gs = remember(all) { all.mapNotNull { it.genre }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key } }
            val mine = remember(all) { all.map { it.own }.filter { it.isNotEmpty() }.toSet() }
            if (mine.isNotEmpty()) {
                Section(t("Свои жанры", "Your genres"))
                Flow { for (o in mine) st.owns[o]?.let { og -> Chip(og.name, o in owns, { owns = if (o in owns) owns - o else owns + o }, color = androidx.compose.ui.graphics.Color(og.color)) } }
            }
            if (gs.isNotEmpty()) {
                Section(t("Жанр", "Genre"), t("можно несколько", "pick several"))
                Flow { for (g in gs) Chip(Words.genre(g, ru), g in genres, { genres = if (g in genres) genres - g else genres + g }, color = C.genre(g)) }
            }
            val ds = remember(all) { all.mapNotNull { it.year?.let { y -> y / 10 * 10 } }.toSet().sorted() }
            if (ds.isNotEmpty()) {
                Section(t("Десятилетие", "Decade"))
                Flow { for (d in ds) Chip(if (ru) "${d}-е" else "${d}s", d in decades, { decades = if (d in decades) decades - d else decades + d }) }
            }
            Section(t("Ещё", "More"))
            Flow {
                Chip(t("со звёздами", "rated"), rated, { rated = !rated })
                if (tab == ShelfTab.ALBUMS) Chip(t("слушал целиком", "heard whole"), wholeOnly, { wholeOnly = !wholeOnly })
                if (tab != ShelfTab.WISH) Chip(t("хочу послушать", "to hear"), wishOnly, { wishOnly = !wishOnly })
            }
            val n = all.count { pass(it, genres, decades, rated, wholeOnly, wishOnly) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                Btn(t("Сбросить", "Clear"), { genres = emptySet(); owns = emptySet(); decades = emptySet(); rated = false; wholeOnly = false; wishOnly = false }, Modifier.weight(1f), kind = app.songling.ui.components.Kind.GHOST)
                Btn(if (ru) "Показать $n" else "Show $n", { sheet = 0 }, Modifier.weight(1f), kind = app.songling.ui.components.Kind.AMBER)
            }
        } }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 8.dp, bottom = 104.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                Btn(when (tab) {
                    ShelfTab.WISH -> t("+ Хочу послушать", "+ Want to hear")
                    else -> t("+ Добавить альбом, сингл или песню", "+ Add an album, single or song")
                }, { nav.go(Overlay.Add(wish = tab == ShelfTab.WISH)) }, Modifier.fillMaxWidth(), kind = app.songling.ui.components.Kind.GHOST, small = true)
            }
            if (list.isEmpty()) item {
                Text(when {
                    all.isEmpty() && tab == ShelfTab.WISH -> t("Список «послушать» пуст. Добавь то, до чего давно не доходили руки: послушаешь – получишь медиаторы.",
                        "The «to hear» list is empty. Add what you never got round to: hearing it pays plectrums.")
                    all.isEmpty() -> t("Здесь появится всё, что ты слушаешь.", "Everything you listen to shows up here.")
                    else -> t("Ничего не нашлось.", "Nothing found.")
                }, style = T.Muted, modifier = Modifier.padding(vertical = 20.dp))
            }
            items(list, key = { it.kind.name + it.key }) { e -> Line(st, e, nav) }
        }
    }
}

/** A small square-ish button with a symbol and a word. */
@Composable
fun ToolButton(symbol: String, label: String, on: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.paper(99.dp, 2.dp).clip(CircleShape).background(if (on) C.amber else C.surface).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(symbol, style = T.Strong.copy(color = if (on) C.onAmber else C.muted))
        Text(label, style = T.Small.copy(color = if (on) C.onAmber else C.ink), maxLines = 1)
    }
}

/** A filter that's on: tap to take it off. */
@Composable
fun Removable(label: String, color: androidx.compose.ui.graphics.Color = C.amber, onRemove: () -> Unit) {
    Row(Modifier.clip(CircleShape).background(color.copy(alpha = .15f)).clickable(onClick = onRemove).padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = T.Small.copy(color = C.ink))
        Text("×", style = T.Strong.copy(color = C.muted))
    }
}

private fun sortShort(s: Sort, ru: Boolean) = when (s) {
    Sort.MINUTES -> if (ru) "больше" else "most"
    Sort.RECENT -> if (ru) "недавние" else "recent"
    Sort.PLAYS -> if (ru) "чаще" else "listens"
    Sort.TITLE -> if (ru) "А–Я" else "A–Z"
    Sort.YEAR -> if (ru) "год" else "year"
    Sort.STARS -> if (ru) "звёзды" else "stars"
    Sort.ADDED -> if (ru) "новые" else "added"
}

private fun sortName(s: Sort, ru: Boolean) = when (s) {
    Sort.MINUTES -> if (ru) "больше всего" else "most"
    Sort.RECENT -> if (ru) "недавние" else "recent"
    Sort.PLAYS -> if (ru) "по прослушиваниям" else "by listens"
    Sort.TITLE -> if (ru) "по названию" else "by title"
    Sort.YEAR -> if (ru) "по году" else "by year"
    Sort.STARS -> if (ru) "по звёздам" else "by stars"
    Sort.ADDED -> if (ru) "по добавлению" else "by date added"
}

@Composable
private fun Line(st: Studio, e: Entry, nav: Nav) {
    val ru = LocalRu.current
    val open: () -> Unit = {
        nav.go(when (e.kind) {
            ShelfTab.SONGS -> Overlay.TrackOf(e.key)
            ShelfTab.ARTISTS -> Overlay.ArtistOf(e.key)
            else -> if (st.tracks.containsKey(e.key) && !st.albums.containsKey(e.key)) Overlay.TrackOf(e.key) else Overlay.AlbumOf(e.key)
        })
    }
    Row(Modifier.fillMaxWidth().paper(16.dp).clip(R8.Md).background(C.surface).clickable(onClick = open).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box {
            if (e.kind == ShelfTab.ARTISTS || e.kind == ShelfTab.SONGS) Cover(e.cover, e.title, e.genre, 52.dp, round = if (e.kind == ShelfTab.ARTISTS) 26.dp else 8.dp)
            else app.songling.ui.components.Sleeve(e.cover, e.title, e.sub, e.genre, 56.dp)
        }
        Column(Modifier.weight(1f)) {
            Text(e.title, style = T.Strong, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val bits = listOfNotNull(e.sub.ifEmpty { null }, e.year?.toString(), app.songling.ui.sheets.genreName(st, e.own, e.genre, ru),
                e.country.ifEmpty { null }?.let { Words.flag(it) + " " + Words.country(it, ru) })
            if (bits.isNotEmpty()) Text(bits.joinToString(" · "), style = T.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                if (e.ms > 0) Text(Words.dur(e.ms, ru), style = T.Small.copy(color = C.amber))
                if (e.whole > 0) Text(t("целиком ×${e.whole}", "whole ×${e.whole}"), style = T.Small.copy(color = C.mint))
                if (e.wish && e.plays == 0) Text(t("хочу послушать", "to hear"), style = T.Small.copy(color = C.lilac))
                if (e.stars > 0) Stars(e.stars, size = 11.dp)
            }
        }
    }
}

/** Takes one listen out of the history, and with it out of every count. */
@Composable
private fun ForgetSheet(st: Studio, p: app.songling.data.Play, close: () -> Unit) {
    val co = androidx.compose.ui.platform.LocalContext.current.container
    val ru = LocalRu.current
    app.songling.ui.Floating { Sheet(onDismiss = close) {
        Text(t("Убрать из истории?", "Remove from the history?"), style = T.Title)
        Text("«${p.title}» · ${p.artistName}", style = T.Body, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(date(p.at, ru) + String.format(java.util.Locale.ROOT, ", %02d:%02d · ", st.hour(p.at), java.time.Instant.ofEpochMilli(p.at).atZone(st.zone).minute) + Words.dur(p.ms.coerceAtLeast(60_000), ru), style = T.Small)
        Text(t("Это прослушивание пропадёт из статистики, времени питомца и заданий.", "This listen leaves the statistics, the pet's time and the goals."), style = T.Muted)
        Btn(t("Убрать", "Remove"), { co.write { db.dao().delete(p) }; close() }, Modifier.fillMaxWidth())
        Btn(t("Оставить", "Keep it"), close, Modifier.fillMaxWidth(), kind = app.songling.ui.components.Kind.GHOST)
    } }
}

@Composable
private fun History(st: Studio, nav: Nav) {
    val ru = LocalRu.current
    var shown by remember { mutableIntStateOf(150) }
    val rev = remember(st) { st.plays.asReversed() }
    // A listen held down: asked whether to take it out of the history (a stray video, a misheard song).
    var gone by remember { mutableStateOf<app.songling.data.Play?>(null) }
    gone?.let { p -> ForgetSheet(st, p) { gone = null } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 10.dp, bottom = 104.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (rev.isEmpty()) item { Text(t("Пока ни одного прослушивания.", "No listens yet."), style = T.Muted) }
        var lastDay: java.time.LocalDate? = null
        val slice = rev.take(shown)
        for (p in slice) {
            val d = st.day(p.at)
            if (d != lastDay) {
                lastDay = d
                val dayMs = st.byDay[d] ?: 0
                item(key = "d$d") {
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp)) {
                        Text(date(st.startOf(d), ru).uppercase(), style = T.Eyebrow.copy(color = C.amber), modifier = Modifier.weight(1f))
                        Text(Words.dur(dayMs, ru), style = T.Small)
                    }
                }
            }
            item(key = "p${p.id}") {
                val al = st.albums[p.album]
                Row(Modifier.fillMaxWidth().clip(R8.Sm).combinedClickable(onLongClick = { gone = p }) { nav.go(Overlay.TrackOf(p.track)) }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Cover(al?.cover, p.albumName.ifEmpty { p.title }, al?.genre, 36.dp, round = 6.dp)
                    Column(Modifier.weight(1f)) {
                        Text(p.title, style = T.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(p.artistName + if (p.source != app.songling.data.Source.PLAYER) " · " + Words.source(p.source, ru) else "", style = T.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(String.format("%02d:%02d", st.hour(p.at), java.time.Instant.ofEpochMilli(p.at).atZone(st.zone).minute), style = T.Small)
                        Text(if (p.skipped) t("пропуск", "skipped") else Words.dur(p.ms.coerceAtLeast(60_000), ru), style = T.Small.copy(color = if (p.skipped) C.coral else C.faint))
                    }
                }
            }
        }
        if (rev.size > shown) item { Btn(t("Показать ещё", "Show more"), { shown += 300 }, Modifier.fillMaxWidth(), kind = app.songling.ui.components.Kind.SOFT, small = true) }
    }
}
