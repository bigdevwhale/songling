package app.songling.ui.sheets

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.songling.container
import app.songling.data.Album
import app.songling.data.Covers
import app.songling.data.Keys
import app.songling.data.Kind
import app.songling.data.Track
import app.songling.domain.Studio
import app.songling.domain.Words
import app.songling.ui.LocalRu
import app.songling.ui.Nav
import app.songling.ui.Overlay
import app.songling.ui.components.Btn
import app.songling.ui.components.Chip
import app.songling.ui.components.Field
import app.songling.ui.components.Flow
import app.songling.ui.components.Kind as BtnKind
import app.songling.ui.components.Section
import app.songling.ui.components.Sheet
import app.songling.ui.components.t
import app.songling.ui.theme.C
import app.songling.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A song in an album being made: its title, and its key when it's one already heard. */
private data class Song(val title: String, val key: String?)

/**
 * An album made or put right by hand: name, artist, year, cover from the gallery, and its songs in order – typed
 * in or picked from what was heard. For albums the catalogue doesn't know (or knows wrong), and to fold a player's
 * «Unnamed» into the real one. [key] null makes a new one.
 */
@Composable
fun AlbumEditSheet(st: Studio, key: String?, nav: Nav) {
    val ru = LocalRu.current
    val ctx = LocalContext.current
    val co = ctx.container
    val scope = rememberCoroutineScope()
    val old = key?.let { st.albums[it] }
    val heardTitle = key?.let { st.byAlbum[it]?.name }
    var title by remember { mutableStateOf(old?.title ?: heardTitle ?: "") }
    var artist by remember { mutableStateOf(old?.artistName ?: key?.let { st.byAlbum[it]?.artistName } ?: "") }
    var year by remember { mutableStateOf(old?.year?.toString().orEmpty()) }
    var kind by remember { mutableStateOf(old?.kind ?: Kind.ALBUM) }
    var cover by remember { mutableStateOf(old?.cover) }
    var typed by remember { mutableStateOf("") }
    var keepAlias by remember { mutableStateOf(true) }
    val songs = remember {
        mutableStateListOf<Song>().apply {
            if (key != null) {
                val heard = st.plays.filter { it.album == key }.map { it.track to it.title }
                (st.tracks.values.filter { it.album == key }.sortedBy { if (it.number > 0) it.number else 999 }.map { it.key to it.title } + heard)
                    .distinctBy { it.first }.forEach { (k, n) -> add(Song(n, k)) }
            }
        }
    }
    // The artist's songs heard anywhere, not yet on the list: one tap puts them on.
    val artistKey = Keys.artist(artist)
    val heardSongs = remember(artist, songs.size, st) {
        if (artist.isBlank()) emptyList() else st.byTrack.values
            .filter { t -> Keys.artists(t.artistName).contains(artistKey) && songs.none { it.key == t.key } }
            .sortedByDescending { it.last }.take(40)
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            cover = withContext(Dispatchers.IO) {
                runCatching { ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } }.getOrNull()
                    ?.let { Covers.keep(ctx, "own:" + System.currentTimeMillis(), it) }
            } ?: cover
        }
    }

    Sheet(onDismiss = nav::close) {
        Text(if (key == null) t("Свой альбом", "Your own album") else t("Альбом: песни и обложка", "Album: songs and cover"), style = T.Title)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).clickable { pick.launch("image/*") }) {
                app.songling.ui.components.Cover(cover, title.ifBlank { "?" }, old?.genre, 96.dp)
            }
            Column(Modifier.weight(1f)) {
                Text(t("Обложка", "Cover"), style = T.Strong)
                Text(t("Нажми, чтобы выбрать картинку из галереи.", "Tap to pick a picture from the gallery."), style = T.Small)
            }
        }
        Label(t("Название", "Title"))
        Field(title, { title = it.take(80) }, t("Название альбома", "Album title"))
        Label(t("Артист", "Artist"))
        Field(artist, { artist = it.take(80) }, t("Артист", "Artist"))
        Label(t("Год", "Year"))
        Field(year, { year = it.filter(Char::isDigit).take(4) }, t("Год", "Year"), number = true)
        Flow { for (k in Kind.entries) Chip(Words.kind(k, ru), k == kind, { kind = k }) }

        Section(t("Песни", "Songs"), if (songs.isEmpty()) null else "${songs.size}")
        songs.forEachIndexed { i, s ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}.", style = T.Small, modifier = Modifier.width(28.dp))
                Text(s.title, style = T.Body, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Mini("↑", i > 0) { songs.add(i - 1, songs.removeAt(i)) }
                Mini("↓", i < songs.lastIndex) { songs.add(i + 1, songs.removeAt(i)) }
                Mini("✕", true) { songs.removeAt(i) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            fun addTyped() { val n = typed.trim(); if (n.isNotEmpty()) { songs.add(Song(n, null)); typed = "" } }
            Field(typed, { typed = it.take(80) }, t("Добавить песню", "Add a song"), Modifier.weight(1f), onDone = ::addTyped)
            Btn("+", ::addTyped, kind = BtnKind.AMBER, small = true, enabled = typed.isNotBlank())
        }
        if (heardSongs.isNotEmpty()) {
            Section(t("Из прослушанного", "From what you heard"), t("нажми, чтобы добавить", "tap to add"))
            for (h in heardSongs) Row(Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .clickable { songs.add(Song(h.name, h.key)) }.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("+", style = T.Strong.copy(color = C.amber), modifier = Modifier.width(28.dp))
                Column(Modifier.weight(1f)) {
                    Text(h.name, style = T.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(h.albumName.ifBlank { t("без альбома", "no album") }, style = T.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        val newKey = Keys.album(artist, title)
        val renamed = key != null && title.isNotBlank() && artist.isNotBlank() && newKey != key
        if (renamed) Row(Modifier.fillMaxWidth().clickable { keepAlias = !keepAlias }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (keepAlias) "☑" else "☐", style = T.Title.copy(color = C.amber), modifier = Modifier.width(32.dp))
            Text(t("Новые прослушивания «${heardTitle ?: old?.title}» тоже класть сюда", "Put new listens of «${heardTitle ?: old?.title}» here too"), style = T.Body)
        }
        Btn(t("Сохранить", "Save"), {
            val name = title.trim(); val who = artist.trim(); val list = songs.toList(); val y = year.toIntOrNull(); val k = kind; val pic = cover; val alias = keepAlias
            co.write {
                val dao = db.dao()
                val now = System.currentTimeMillis()
                val base = (key?.let { dao.album(it) }) ?: Album(newKey, name, who)
                var aliases = base.aliases.split(',').filter { it.isNotBlank() }.toSet()
                if (key != null && key != newKey) {
                    // Renamed, or «Unnamed» made into the real album: its listens and songs move along.
                    dao.moveAlbum(key, newKey, name); dao.moveTracks(key, newKey, name)
                    if (alias) aliases = aliases + key
                    dao.album(key)?.let { dao.delete(it) }
                }
                // Songs taken off the list leave the album.
                st.tracks.values.filter { it.album == newKey || it.album == key }.filter { tr -> list.none { it.key == tr.key || Keys.track(who, it.title) == tr.key } }
                    .forEach { dao.upsert(it.copy(album = "", albumName = "", number = 0)) }
                var length = 0L
                list.forEachIndexed { i, s ->
                    val tk = s.key ?: Keys.track(who, s.title)
                    val tr = dao.track(tk) ?: Track(tk, s.title, who, checked = now)
                    dao.upsert(tr.copy(album = newKey, albumName = name, number = i + 1))
                    dao.moveTrack(tk, newKey, name)
                    length += tr.length
                }
                dao.upsert(base.copy(key = newKey, title = name, artistName = who, artist = Keys.artist(who), kind = k, year = y ?: base.year, cover = pic,
                    tracks = list.size, length = if (length > 0) length else base.length, checked = maxOf(base.checked, now), pinned = true,
                    added = if (base.added == 0L) now else base.added, aliases = aliases.joinToString(",")))
                kick()
            }
            nav.toast(if (ru) "Альбом сохранён" else "Album saved")
            nav.go(Overlay.AlbumOf(newKey))
        }, Modifier.fillMaxWidth(), kind = BtnKind.AMBER, enabled = title.isNotBlank() && artist.isNotBlank())
    }
}

@Composable
private fun Label(text: String) = Text(text, style = T.Small, modifier = Modifier.padding(start = 6.dp, top = 2.dp))

@Composable
private fun Mini(text: String, on: Boolean, onClick: () -> Unit) {
    Text(text, style = T.Strong.copy(color = if (on) C.ink else C.faint), modifier = Modifier.padding(start = 4.dp).clip(CircleShape)
        .background(C.surface2).then(if (on) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 10.dp, vertical = 2.dp))
}
