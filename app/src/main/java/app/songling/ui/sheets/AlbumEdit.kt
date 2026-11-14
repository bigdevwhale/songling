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
import app.songling.ui.components.Glyph
import app.songling.ui.components.Sign
import androidx.compose.foundation.layout.size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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

    var genre by remember { mutableStateOf(old?.genre) }
    var own by remember { mutableStateOf(old?.own.orEmpty()) }
    var genreOpen by remember { mutableStateOf(false) }
    // The song whose title is being put right (only the ones typed in here), and whether all the heard ones are shown.
    var renaming by remember { mutableStateOf(-1) }
    var allHeard by remember { mutableStateOf(false) }
    val newKey = Keys.album(artist, title)
    val renamed = key != null && title.isNotBlank() && artist.isNotBlank() && newKey != key
    val saveAll = {
        val name = title.trim(); val who = artist.trim(); val list = songs.toList(); val y = year.toIntOrNull(); val k = kind; val pic = cover; val alias = keepAlias; val g = genre; val o = own
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
            dao.upsert(base.copy(key = newKey, title = name, artistName = who, artist = Keys.artist(who), kind = k, year = y ?: base.year, cover = pic, genre = g ?: base.genre, own = o,
                tracks = list.size, length = if (length > 0) length else base.length, checked = maxOf(base.checked, now), pinned = true,
                added = if (base.added == 0L) now else base.added, aliases = aliases.joinToString(",")))
            kick()
        }
        nav.toast(if (ru) "Альбом сохранён" else "Album saved")
        nav.go(Overlay.AlbumOf(newKey))
    }

    // «Save» stays under the thumb however long the list of songs grows.
    Sheet(onDismiss = nav::close, footer = {
        Btn(t("Сохранить", "Save"), saveAll, Modifier.fillMaxWidth(), kind = BtnKind.AMBER, enabled = title.isNotBlank() && artist.isNotBlank())
    }) {
        Text(if (key == null) t("Свой альбом", "Your own album") else t("Изменить альбом", "Edit the album"), style = T.Title)
        // The cover with the two things that name the album beside it.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp)).clickable { pick.launch("image/*") }) {
                app.songling.ui.components.Cover(cover, title.ifBlank { "?" }, genre, 108.dp, round = 10.dp)
                Text(t("сменить", "change"), style = T.Small.copy(color = androidx.compose.ui.graphics.Color.White), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.align(Alignment.BottomCenter).width(108.dp).background(androidx.compose.ui.graphics.Color.Black.copy(alpha = .5f)).padding(vertical = 4.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(title, { title = it.take(80) }, t("Название", "Title"))
                Field(artist, { artist = it.take(80) }, t("Артист", "Artist"))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field(year, { year = it.filter(Char::isDigit).take(4) }, t("Год", "Year"), Modifier.width(92.dp), number = true)
            for (k in Kind.entries) Chip(Words.kind(k, ru), k == kind, { kind = k })
        }
        // The genre in one line; the whole list opens under it.
        Row(Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp)).background(C.surface2).clickable { genreOpen = !genreOpen }.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(t("Жанр", "Genre"), style = T.Strong, modifier = Modifier.weight(1f))
            Text(genreName(st, own, genre, ru) ?: t("не выбран", "not set"), style = T.Small.copy(color = genreColour(st, own, genre) ?: C.faint))
            Sign(if (genreOpen) Glyph.UP else Glyph.DOWN, color = C.faint, size = 12.dp)
        }
        if (genreOpen) GenrePick(st, genre, own, { genre = it; own = "" }, { own = it })

        Section(t("Песни", "Songs"), if (songs.isEmpty()) null else "${songs.size}")
        if (songs.isEmpty()) Text(t("Пока ни одной. Впиши название ниже или возьми из прослушанного.", "None yet. Type a title below, or take one from what you heard."), style = T.Muted)
        songs.forEachIndexed { i, s ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}", style = T.Small, modifier = Modifier.width(26.dp))
                if (renaming == i) Field(s.title, { songs[i] = s.copy(title = it.take(80)) }, t("Название песни", "Song title"), Modifier.weight(1f), onDone = { renaming = -1 })
                else Text(s.title, style = T.Body, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .then(if (s.key == null) Modifier.clickable { renaming = i } else Modifier).padding(vertical = 8.dp))
                Mini(Glyph.UP, t("Выше", "Move up"), i > 0) { renaming = -1; songs.add(i - 1, songs.removeAt(i)) }
                Mini(Glyph.DOWN, t("Ниже", "Move down"), i < songs.lastIndex) { renaming = -1; songs.add(i + 1, songs.removeAt(i)) }
                Mini(Glyph.CLOSE, t("Убрать", "Remove"), true) { renaming = -1; songs.removeAt(i) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            fun addTyped() { val n = typed.trim(); if (n.isNotEmpty()) { songs.add(Song(n, null)); typed = "" } }
            Field(typed, { typed = it.take(80) }, t("Новая песня", "A new song"), Modifier.weight(1f), onDone = ::addTyped)
            Btn(t("Добавить", "Add"), ::addTyped, kind = BtnKind.SOFT, small = true, enabled = typed.isNotBlank())
        }
        if (heardSongs.isNotEmpty()) {
            Section(t("Из прослушанного", "From what you heard"), t("нажми, чтобы добавить", "tap to add"))
            for (h in heardSongs.take(if (allHeard) heardSongs.size else 5)) Row(Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                .clickable { songs.add(Song(h.name, h.key)) }.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.clip(CircleShape).background(C.amberSoft).padding(7.dp)) { Sign(Glyph.PLUS, color = C.amber, size = 10.dp) }
                Column(Modifier.weight(1f)) {
                    Text(h.name, style = T.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(h.albumName.ifBlank { t("без альбома", "no album") }, style = T.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (heardSongs.size > 5 && !allHeard) Btn(t("Показать ещё ${heardSongs.size - 5}", "Show ${heardSongs.size - 5} more"), { allHeard = true }, Modifier.fillMaxWidth(), kind = BtnKind.GHOST, small = true)
        }
        if (renamed) Row(Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).clickable { keepAlias = !keepAlias }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // A tick box, drawn like the rest of the signs.
            Box(Modifier.size(22.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(7.dp)).background(if (keepAlias) C.amber else C.track), contentAlignment = Alignment.Center) {
                if (keepAlias) Sign(Glyph.CHECK, color = C.onAmber, size = 11.dp)
            }
            Text(t("Новые прослушивания «${heardTitle ?: old?.title}» тоже класть сюда", "Put new listens of «${heardTitle ?: old?.title}» here too"), style = T.Body, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun Mini(sign: Glyph, label: String, on: Boolean, onClick: () -> Unit) {
    Box(Modifier.padding(start = 6.dp).size(34.dp).clip(CircleShape).background(C.surface2).then(if (on) Modifier.clickable(onClick = onClick) else Modifier)
        .semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Sign(sign, color = if (on) C.ink else C.faint.copy(alpha = .45f), size = 11.dp, stroke = 1.8.dp)
    }
}
