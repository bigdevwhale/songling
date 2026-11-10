package app.songling.ui.sheets

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import app.songling.ui.components.FormField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.songling.container
import app.songling.data.DAY
import app.songling.data.Found
import app.songling.data.FoundTrack
import app.songling.data.Kind
import app.songling.data.MIN
import app.songling.data.Meta
import app.songling.data.Play
import app.songling.data.Source
import app.songling.domain.Studio
import app.songling.domain.Words
import app.songling.ui.LocalRu
import app.songling.ui.Nav
import app.songling.ui.components.Btn
import app.songling.ui.components.Chip
import app.songling.ui.components.Field
import app.songling.ui.components.Flow
import app.songling.ui.components.Kind as BtnKind
import app.songling.ui.components.Section
import app.songling.ui.components.Sheet
import app.songling.ui.components.Stepper
import app.songling.ui.components.t
import app.songling.ui.theme.C
import app.songling.ui.theme.R8
import app.songling.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

private enum class What { ALBUM, SINGLE, SONG }

/** A cover from the catalogue, before it's kept. */
@Composable
fun RemoteCover(url: String?, size: androidx.compose.ui.unit.Dp) {
    val img by produceState<ImageBitmap?>(null, url) {
        value = url?.let { u -> withContext(Dispatchers.IO) { runCatching { URL(u.replace("600x600", "100x100")).openStream().use { BitmapFactory.decodeStream(it) }?.asImageBitmap() }.getOrNull() } }
    }
    Box(Modifier.size(size).clip(R8.Sm).background(C.surface2)) { img?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) } }
}

@Composable
fun AddSheet(st: Studio, wish0: Boolean, nav: Nav) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    val scope = rememberCoroutineScope()
    var what by remember { mutableStateOf(What.ALBUM) }
    var wish by remember { mutableStateOf(wish0) }
    var heard by remember { mutableStateOf(false) }
    var q by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var albums by remember { mutableStateOf<List<Found>?>(null) }
    var songs by remember { mutableStateOf<List<FoundTrack>?>(null) }
    var manual by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var album by remember { mutableStateOf("") }

    fun search() {
        if (q.isBlank()) return
        busy = true
        scope.launch {
            withContext(Dispatchers.IO) {
                if (what == What.SONG) songs = Meta.searchTracks(q, ru) else albums = Meta.searchAlbums(q, ru).filter { (it.kind == Kind.SINGLE || it.kind == Kind.EP) == (what == What.SINGLE) || what == What.ALBUM && it.kind != Kind.SINGLE }
            }
            busy = false
        }
    }

    /** A listen, once it's on the shelf, for «I've heard it». */
    fun logHeard(t: String, a: String, al: String, ms: Long, whole: Boolean) = co.write {
        db.dao().insert(Play(at = System.currentTimeMillis() - ms, ms = ms, title = t, artistName = a, albumName = al, source = Source.MANUAL, whole = whole))
    }

    fun done(name: String) {
        nav.toast(if (wish) (if (ru) "«$name» – в списке «послушать»" else "«$name» is on the «to hear» list") else (if (ru) "«$name» – на полке" else "«$name» is on the shelf"))
        nav.close()
    }

    Sheet(onDismiss = nav::close) {
        Text(t("На полку", "To the shelf"), style = T.Title)
        Flow {
            Chip(t("Альбом", "Album"), what == What.ALBUM, { what = What.ALBUM; albums = null; songs = null })
            Chip(t("Сингл или EP", "Single or EP"), what == What.SINGLE, { what = What.SINGLE; albums = null; songs = null })
            Chip(t("Песня", "Song"), what == What.SONG, { what = What.SONG; albums = null; songs = null })
        }
        Flow {
            Chip(t("хочу послушать", "want to hear"), wish, { wish = !wish; if (wish) heard = false }, color = C.lilac)
            Chip(t("уже слушал(а)", "I've heard it"), heard, { heard = !heard; if (heard) wish = false }, color = C.mint)
        }
        if (!manual) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(q, { q = it }, if (what == What.SONG) t("Песня и артист", "Song and artist") else t("Название и артист", "Title and artist"), Modifier.weight(1f), onDone = ::search)
                Btn(if (busy) "…" else t("Найти", "Find"), ::search, kind = BtnKind.AMBER, small = true, enabled = !busy)
            }
            albums?.let { list ->
                if (list.isEmpty()) Text(t("Ничего не нашлось. Можно ввести вручную.", "Nothing found. You can type it in."), style = T.Muted)
                for (f in list) Row(Modifier.fillMaxWidth().clip(R8.Md).background(C.surface2).clickable {
                    addAlbum(co, f.title, f.artist, f.kind, f.year, f.genreName, f.tracks, f.art, wish)
                    if (heard) logHeard(f.title, f.artist, f.title, (f.tracks.coerceAtLeast(1) * 3.5 * MIN).toLong(), f.kind != Kind.SINGLE)
                    done(f.title)
                }.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RemoteCover(f.art, 48.dp)
                    Column(Modifier.weight(1f)) {
                        Text(f.title, style = T.Strong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(listOfNotNull(f.artist, f.year?.toString(), Words.kind(f.kind, ru), if (f.tracks > 0) t("${f.tracks} песен", "${f.tracks} songs") else null).joinToString(" · "), style = T.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            songs?.let { list ->
                if (list.isEmpty()) Text(t("Ничего не нашлось. Можно ввести вручную.", "Nothing found. You can type it in."), style = T.Muted)
                for (f in list) Row(Modifier.fillMaxWidth().clip(R8.Md).background(C.surface2).clickable {
                    addTrack(co, f.title, f.artist, f.album, f.year, f.genreName, f.length, f.art, f.albumTracks, wish)
                    if (heard) logHeard(f.title, f.artist, f.album, f.length.takeIf { it > 0 } ?: (3.5 * MIN).toLong(), false)
                    done(f.title)
                }.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RemoteCover(f.art, 44.dp)
                    Column(Modifier.weight(1f)) {
                        Text(f.title, style = T.Strong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(listOf(f.artist, f.album).filter { it.isNotBlank() }.joinToString(" · "), style = T.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Btn(t("Ввести вручную", "Type it in"), { manual = true }, Modifier.fillMaxWidth(), kind = BtnKind.GHOST, small = true)
        } else {
            FormField(title, { title = it }, if (what == What.SONG) t("Песня", "Song") else t("Название", "Title"))
            FormField(artist, { artist = it }, t("Артист", "Artist"))
            if (what == What.SONG) FormField(album, { album = it }, t("Альбом, если знаешь", "Album, if you know it"))
            Btn(t("Добавить", "Add"), {
                if (what == What.SONG) {
                    addTrack(co, title.trim(), artist.trim(), album.trim(), null, "", 0, null, 0, wish)
                    if (heard) logHeard(title.trim(), artist.trim(), album.trim(), (3.5 * MIN).toLong(), false)
                } else {
                    addAlbum(co, title.trim(), artist.trim(), if (what == What.SINGLE) Kind.SINGLE else Kind.ALBUM, null, "", 0, null, wish)
                    if (heard) logHeard(title.trim(), artist.trim(), title.trim(), 40 * MIN, what == What.ALBUM)
                }
                done(title.trim())
            }, Modifier.fillMaxWidth(), kind = BtnKind.AMBER, enabled = title.isNotBlank() && artist.isNotBlank())
        }
    }
}

/** A listen the phone didn't hear: a record, the radio, a concert. */
@Composable
fun LogSheet(st: Studio, concert: Boolean, nav: Nav) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    var source by remember { mutableStateOf(if (concert) Source.CONCERT else Source.VINYL) }
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var album by remember { mutableStateOf("") }
    var whole by remember { mutableStateOf(true) }
    var minutes by remember { mutableIntStateOf(if (concert) 90 else 40) }
    var daysAgo by remember { mutableIntStateOf(0) }
    Sheet(onDismiss = nav::close) {
        Text(if (source == Source.CONCERT) t("Концерт", "A concert") else t("Отметить прослушивание", "Log a listen"), style = T.Title)
        Flow {
            for (s in listOf(Source.VINYL, Source.RADIO, Source.MANUAL, Source.CONCERT)) Chip(Words.source(s, ru), s == source, {
                source = s; if (s == Source.CONCERT && minutes < 60) minutes = 90
            }, color = if (s == Source.CONCERT) C.coral else null)
        }
        FormField(artist, { artist = it }, t("Артист", "Artist"))
        if (source == Source.CONCERT) {
            FormField(title, { title = it }, t("Где: клуб, фестиваль, город", "Where: club, festival, city"))
        } else {
            FormField(album, { album = it }, t("Альбом", "Album"))
            FormField(title, { title = it }, t("Песня, можно не писать", "Song, can be left blank"))
            if (album.isNotBlank()) Chip(t("весь альбом целиком", "the whole album"), whole, { whole = !whole }, color = C.mint)
        }
        Stepper(Words.dur(minutes * MIN, ru)) { minutes = (minutes + it * 5).coerceIn(5, 600) }
        Stepper(when (daysAgo) { 0 -> t("сегодня", "today"); 1 -> t("вчера", "yesterday"); else -> t("$daysAgo дн. назад", "$daysAgo days ago") }) { daysAgo = (daysAgo + it).coerceIn(0, 365) }
        if (source == Source.CONCERT) Text(t("Живой концерт – редкое событие: за него появится яйцо Светлячка.", "A live concert is rare: it brings the Firefly egg."), style = T.Small.copy(color = C.coral))
        Btn(t("Записать", "Log it"), {
            val ms = minutes * MIN
            val at = System.currentTimeMillis() - daysAgo * DAY - ms
            val songTitle = when {
                source == Source.CONCERT -> (if (ru) "Концерт" else "Concert") + if (title.isNotBlank()) " · ${title.trim()}" else ""
                title.isNotBlank() -> title.trim()
                else -> album.trim().ifEmpty { if (ru) "Без названия" else "Untitled" }
            }
            co.write {
                val p = Play(at = at, ms = ms, title = songTitle, artistName = artist.trim(), albumName = if (source == Source.CONCERT) "" else album.trim(), source = source,
                    whole = source != Source.CONCERT && album.isNotBlank() && whole)
                db.dao().insert(p)
                meta.note(p)
                kick()
            }
            nav.toast(if (ru) "Записано" else "Logged")
            nav.close()
        }, Modifier.fillMaxWidth(), kind = BtnKind.AMBER, enabled = artist.isNotBlank())
        Section("")
    }
}
