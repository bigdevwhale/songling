package app.songling.ui.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import app.songling.ui.components.SideHeader
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.songling.container
import app.songling.data.Album
import app.songling.data.Covers
import app.songling.data.Artist
import app.songling.data.Goal
import app.songling.data.GoalKind
import app.songling.data.Keys
import app.songling.data.Kind
import app.songling.data.MIN
import app.songling.data.Period
import app.songling.data.Track
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
import app.songling.ui.components.Flow
import app.songling.ui.components.Kind as BtnKind
import app.songling.ui.components.Section
import app.songling.ui.components.Sheet
import app.songling.ui.components.Stars
import app.songling.ui.components.Stat
import app.songling.ui.components.t
import app.songling.ui.den.date
import app.songling.ui.theme.C
import app.songling.ui.theme.T

@Composable
private fun StatRow(vararg s: Pair<String, String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { for ((v, l) in s) Stat(v, l, Modifier.weight(1f)) }
}


// ---------------------------------------------------------------- album

@Composable
fun AlbumSheet(st: Studio, key: String, nav: Nav) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    val tl = st.byAlbum[key]
    val a = st.albums[key] ?: Album(key, tl?.name ?: key.substringAfter('|'), tl?.artistName ?: st.artistName(key.substringBefore('|')))
    var edit by remember { mutableStateOf(false) }
    var year by remember(key) { mutableStateOf(a.year?.toString().orEmpty()) }
    var note by remember(key) { mutableStateOf(a.note) }
    fun save(x: Album) = co.write { db.dao().upsert(x) }
    Sheet(onDismiss = { if (note != a.note) save(a.copy(note = note)); nav.close() }) {
        // The album as a thing: the sleeve with the record sliding out, or the cassette's J-card.
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { app.songling.ui.components.Sleeve(a.cover, a.title, a.artistName, a.genre, 200.dp, slide = true) }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(a.title, style = T.Title, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text(a.artistName, style = T.Body.copy(color = C.lilac), modifier = Modifier.clickable { nav.go(Overlay.ArtistOf(a.artist)) })
            Text(listOfNotNull(Words.kind(a.kind, ru), a.year?.toString(), genreName(st, a.own, a.genre, ru)).joinToString(" · "), style = T.Small)
        }
        Stars(a.stars, size = 28.dp) { save(a.copy(stars = it)) }
        val heard = tl?.tracks?.size ?: 0
        StatRow(Words.dur(tl?.ms ?: 0, ru) to t("послушано", "listened"), "${tl?.whole ?: 0}" to t("раз целиком", "times whole"),
            (if (a.tracks > 0) "$heard / ${a.tracks}" else "$heard") to t("песен услышано", "songs heard"))
        if (tl != null) Text(t("Впервые ${date(tl.first, true)}, последний раз ${date(tl.last, true)}", "First ${date(tl.first, false)}, last ${date(tl.last, false)}"), style = T.Small)
        Flow {
            Chip(if (a.wish) t("в списке «послушать»", "on the «to hear» list") else t("хочу послушать", "want to hear"), a.wish, {
                save(a.copy(wish = !a.wish, added = if (a.added == 0L) System.currentTimeMillis() else a.added))
            }, color = C.lilac)
            Chip(t("✓ послушал целиком", "✓ heard it whole"), false, { nav.go(Overlay.Mark(key, null)) }, color = C.mint)
            Chip(t("повесить в логово", "hang in the den"), false, { nav.go(Overlay.Poster("album:$key")) })
            Chip(t("цель: послушать целиком", "goal: hear it whole"), false, {
                nav.go(Overlay.GoalEdit(draft = Goal(kind = GoalKind.ALBUM, target = 1, period = Period.ONCE, ref = key, label = a.title)))
            })
            Chip(t("изменить", "edit"), edit, { edit = !edit })
        }
        if (edit) {
            Section(t("Жанр", "Genre"))
            GenrePick(st, a.genre, a.own, { save(a.copy(genre = it, own = "", pinned = true)) }, { save(a.copy(own = it)) })
            Section(t("Что это", "What it is"))
            Flow { for (k in Kind.entries) Chip(Words.kind(k, ru), k == a.kind, { save(a.copy(kind = k, pinned = true)) }) }
            Field(year, { year = it.filter(Char::isDigit).take(4) }, t("Год", "Year"), number = true)
            Btn(t("Сохранить год", "Save the year"), { save(a.copy(year = year.toIntOrNull(), pinned = true)) }, kind = BtnKind.SOFT, small = true)
        }
        Field(note, { note = it }, t("Заметка: где услышал, с чем связан…", "A note: where you heard it, what it reminds you of…"), single = false)
        val heardCount = st.plays.filter { it.album == key }.groupingBy { it.track }.eachCount()
        val songs = (st.tracks.values.filter { it.album == key }.map { it.key to it.title } + st.plays.filter { it.album == key }.map { it.track to it.title })
            .distinctBy { it.first }.map { (k, title) -> Triple(k, title, heardCount[k] ?: 0) }
            .sortedBy { st.tracks[it.first]?.number?.takeIf { n -> n > 0 } ?: 999 }
        if (songs.isNotEmpty()) {
            // Two sides, as on the record or the tape.
            val half = (songs.size + 1) / 2
            for ((side, part) in listOf(songs.take(half), songs.drop(half)).withIndex()) {
                if (part.isEmpty()) continue
                val len = part.sumOf { st.tracks[it.first]?.length ?: 0L }
                SideHeader(t("Сторона ", "Side ") + (if (side == 0) t("А", "A") else t("Б", "B")),
                    if (len > 0) "%d:%02d".format(len / 60_000, len / 1000 % 60) else null)
                for ((k, title, n) in part) Row(Modifier.fillMaxWidth().clickable { nav.go(Overlay.TrackOf(k)) }.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    val num = st.tracks[k]?.number ?: 0
                    Text(if (num > 0) "$num." else "·", style = T.Small, modifier = Modifier.width(26.dp))
                    Text(title, style = T.Body.copy(color = if (n == 0) C.faint else C.ink), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (n == 0) t("не слышал", "unheard") else Words.times(n, ru), style = T.Small)
                    Text("+", style = T.Strong.copy(color = C.amber), modifier = Modifier.padding(start = 6.dp).clip(androidx.compose.foundation.shape.CircleShape)
                        .background(C.amberSoft).clickable { nav.go(Overlay.Mark(key, k)) }.padding(horizontal = 10.dp, vertical = 2.dp))
                }
            }
        }
        if (tl == null) Btn(t("Убрать с полки", "Remove from the shelf"), { co.write { db.dao().delete(a) }; nav.close() }, Modifier.fillMaxWidth(), kind = BtnKind.GHOST)
    }
}

// ---------------------------------------------------------------- artist

@Composable
fun ArtistSheet(st: Studio, key: String, nav: Nav) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    val tl = st.byArtist[key]
    val a = st.artists[key] ?: Artist(key, st.artistName(key))
    var country by remember(key) { mutableStateOf(a.country) }
    var edit by remember { mutableStateOf(false) }
    fun save(x: Artist) = co.write { db.dao().upsert(x) }
    Sheet(onDismiss = nav::close) {
        Text(t("АРТИСТ", "ARTIST") + (a.country.takeIf { it.isNotBlank() }?.let { " · " + Words.flag(it) + " " + Words.country(it, ru).uppercase() } ?: ""), style = T.Eyebrow)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            app.songling.ui.den.ArtistFace(st, key, 72.dp)
            Text(a.name, style = T.Display, modifier = Modifier.weight(1f))
        }
        Stars(a.stars, size = 26.dp) { save(a.copy(stars = it)) }
        StatRow(Words.dur(tl?.ms ?: 0, ru) to t("послушано", "listened"), "${tl?.plays ?: 0}" to t("раз слушал", "listens"), "${tl?.tracks?.size ?: 0}" to t("разных песен", "songs"))
        if (tl != null) Text(t("Впервые ${date(tl.first, true)}", "First heard ${date(tl.first, false)}"), style = T.Small)
        Flow {
            Chip(if (a.wish) t("в списке «послушать»", "on the «to hear» list") else t("хочу послушать", "want to hear"), a.wish, {
                save(a.copy(wish = !a.wish, added = if (a.added == 0L) System.currentTimeMillis() else a.added))
            }, color = C.lilac)
            Chip(t("постер в логово", "poster in the den"), false, { nav.go(Overlay.Poster("artist:$key")) })
            Chip(t("цель: часы с ним", "goal: hours of them"), false, {
                nav.go(Overlay.GoalEdit(draft = Goal(kind = GoalKind.ARTIST, target = 300, period = Period.MONTH, ref = key, label = a.name)))
            })
            Chip(t("изменить", "edit"), edit, { edit = !edit })
        }
        if (edit) {
            Section(t("Жанр артиста", "The artist's genre"))
            GenrePick(st, a.genre, a.own, { save(a.copy(genre = it, own = "")) }, { save(a.copy(own = it)) })
            Field(country, { country = it.uppercase().filter(Char::isLetter).take(2) }, t("Страна, код из двух букв (RU, GB, US…)", "Country, two-letter code (GB, US, JP…)"))
            Btn(t("Сохранить страну", "Save the country"), { save(a.copy(country = country, checked = System.currentTimeMillis())) }, kind = BtnKind.SOFT, small = true)
        }
        val albums = tl?.albums?.map { it to (st.byAlbum[it]?.ms ?: 0L) }?.sortedByDescending { it.second }.orEmpty() +
            st.albums.values.filter { it.artist == key && tl?.albums?.contains(it.key) != true }.map { it.key to 0L }
        if (albums.isNotEmpty()) {
            Section(t("Альбомы и синглы", "Albums and singles"), "${albums.size}")
            for ((k, ms) in albums) {
                val al = st.albums[k]
                Row(Modifier.fillMaxWidth().clickable { nav.go(Overlay.AlbumOf(k)) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Cover(al?.cover, st.albumTitle(k), al?.genre, 40.dp)
                    Column(Modifier.weight(1f)) {
                        Text(st.albumTitle(k), style = T.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(listOfNotNull(al?.kind?.let { Words.kind(it, ru) }, al?.year?.toString()).joinToString(" · "), style = T.Small)
                    }
                    if (ms > 0) Text(Words.dur(ms, ru), style = T.Small)
                }
            }
        }
        val songs = st.byTrack.values.filter { it.key.substringBefore('|') == key }.sortedByDescending { it.plays }.take(10)
        if (songs.isNotEmpty()) {
            Section(t("Чаще всего", "Most played"))
            for (s in songs) Row(Modifier.fillMaxWidth().clickable { nav.go(Overlay.TrackOf(s.key)) }.padding(vertical = 5.dp)) {
                Text(s.name, style = T.Body, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(Words.times(s.plays, ru), style = T.Small)
            }
        }
    }
}

// ---------------------------------------------------------------- song

@Composable
fun TrackSheet(st: Studio, key: String, nav: Nav) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    val tl = st.byTrack[key]
    val tr = st.tracks[key] ?: Track(key, tl?.name ?: key.substringAfter('|'), tl?.artistName ?: st.artistName(key.substringBefore('|')))
    val al = st.albums[tr.album]
    fun save(x: Track) = co.write { db.dao().upsert(x) }
    Sheet(onDismiss = nav::close) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            app.songling.ui.components.Sleeve(al?.cover, al?.title ?: tr.albumName.ifEmpty { tr.title }, tr.artistName, tr.genre ?: al?.genre, 96.dp)
            Column(Modifier.weight(1f)) {
                Text(t("ПЕСНЯ", "SONG") + ((tr.year ?: al?.year)?.let { " · $it" } ?: ""), style = T.Eyebrow)
                Text(tr.title, style = T.Title)
                Text(tr.artistName, style = T.Body.copy(color = C.lilac), modifier = Modifier.clickable { nav.go(Overlay.ArtistOf(tr.artist)) })
                if (tr.albumName.isNotEmpty() || al != null) Text(al?.title ?: tr.albumName, style = T.Small, modifier = Modifier.clickable { if (tr.album.isNotEmpty()) nav.go(Overlay.AlbumOf(tr.album)) })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stars(tr.stars, size = 26.dp) { save(tr.copy(stars = it)) }
            Chip(if (tr.loved) "♥" else "♡", tr.loved, { save(tr.copy(loved = !tr.loved)) }, color = C.coral)
        }
        StatRow("${tl?.plays ?: 0}" to t("раз слушал", "listens"), Words.dur(tl?.ms ?: 0, ru) to t("всего", "in all"), "${tl?.skips ?: 0}" to t("пропусков", "skips"))
        if (tl != null) Text(t("Впервые ${date(tl.first, true)}, последний раз ${date(tl.last, true)}", "First ${date(tl.first, false)}, last ${date(tl.last, false)}"), style = T.Small)
        Flow {
            Chip(if (tr.wish) t("в списке «послушать»", "on the «to hear» list") else t("хочу послушать", "want to hear"), tr.wish, {
                save(tr.copy(wish = !tr.wish, added = if (tr.added == 0L) System.currentTimeMillis() else tr.added))
            }, color = C.lilac)
            Chip(t("✓ послушал", "✓ heard it"), false, { nav.go(Overlay.Mark(tr.album.ifEmpty { null }, key)) }, color = C.mint)
            Chip(t("цель: прослушивания", "goal: listens"), false, {
                nav.go(Overlay.GoalEdit(draft = Goal(kind = GoalKind.TRACK, target = 10, period = Period.MONTH, ref = key, label = tr.title)))
            })
        }
        Section(t("Жанр", "Genre"))
        GenrePick(st, tr.genre ?: al?.genre, tr.own, { save(tr.copy(genre = it, own = "", pinned = it != null)) }, { save(tr.copy(own = it)) })
        if (tl == null) Btn(t("Убрать с полки", "Remove from the shelf"), { co.write { db.dao().delete(tr) }; nav.close() }, Modifier.fillMaxWidth(), kind = BtnKind.GHOST)
    }
}

/** Adds a found or typed album, single or song to the shelf (and the «to hear» list). */
fun addAlbum(co: app.songling.AppContainer, title: String, artist: String, kind: Kind, year: Int?, genreName: String, tracks: Int, art: String?, wish: Boolean) = co.write {
    val key = Keys.album(artist, title)
    val now = System.currentTimeMillis()
    val old = db.dao().album(key)
    val cover = old?.cover ?: art?.let { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { Covers.download(app, key, it) } }
    db.dao().upsert((old ?: Album(key, title, artist)).copy(kind = kind, year = old?.year ?: year, genre = old?.genre ?: Genre.of(genreName), genreName = genreName,
        tracks = if ((old?.tracks ?: 0) > 0) old!!.tracks else tracks, cover = cover, wish = wish || old?.wish == true, added = old?.added?.takeIf { it > 0 } ?: now, checked = 0))
    if (db.dao().artist(Keys.artist(artist)) == null) db.dao().insertArtist(Artist(Keys.artist(artist), Keys.mainArtist(artist)))
    kick()
}

fun addTrack(co: app.songling.AppContainer, title: String, artist: String, album: String, year: Int?, genreName: String, length: Long, art: String?, albumTracks: Int, wish: Boolean) = co.write {
    val key = Keys.track(artist, title)
    val now = System.currentTimeMillis()
    val albumKey = if (album.isBlank()) "" else Keys.album(artist, album)
    val old = db.dao().track(key)
    db.dao().upsert((old ?: Track(key, title, artist)).copy(album = old?.album?.ifEmpty { null } ?: albumKey, albumName = old?.albumName?.ifEmpty { null } ?: album,
        year = old?.year ?: year, genre = old?.genre ?: Genre.of(genreName), length = if ((old?.length ?: 0) > 0) old!!.length else length,
        wish = wish || old?.wish == true, added = old?.added?.takeIf { it > 0 } ?: now, checked = if (art != null) now else 0))
    if (albumKey.isNotEmpty() && db.dao().album(albumKey) == null) {
        val cover = art?.let { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { Covers.download(app, albumKey, it) } }
        db.dao().insertAlbum(Album(albumKey, album, artist, year = year, genre = Genre.of(genreName), genreName = genreName, tracks = albumTracks, cover = cover,
            kind = if (albumTracks in 1..3) Kind.SINGLE else Kind.ALBUM, checked = if (art != null) now else 0))
    }
    if (db.dao().artist(Keys.artist(artist)) == null) db.dao().insertArtist(Artist(Keys.artist(artist), Keys.mainArtist(artist)))
    kick()
}
