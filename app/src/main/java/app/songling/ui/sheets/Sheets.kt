package app.songling.ui.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    Row(Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for ((v, l) in s) Stat(v, l, Modifier.weight(1f).fillMaxHeight())
    }
}


// ---------------------------------------------------------------- album

@Composable
fun AlbumSheet(st: Studio, key: String, nav: Nav) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    val tl = st.byAlbum[key]
    val a = st.albums[key] ?: Album(key, tl?.name ?: key.substringAfter('|'), tl?.artistName ?: st.artistName(key.substringBefore('|')))
    var note by remember(key) { mutableStateOf(a.note) }
    fun save(x: Album) = co.write { db.dao().upsert(x) }
    Sheet(onDismiss = { if (note != a.note) save(a.copy(note = note)); nav.close() }) {
        // The album as a thing: the sleeve with the record sliding out, or the cassette's J-card.
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { app.songling.ui.components.Sleeve(a.cover, a.title, a.artistName, a.genre, 172.dp, slide = true) }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(a.title, style = T.Title, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text(a.artistName, style = T.Body.copy(color = C.lilac), modifier = Modifier.clickable { nav.go(Overlay.ArtistOf(a.artist)) })
            Text(listOfNotNull(Words.kind(a.kind, ru), a.year?.toString(), genresName(st, a.own, a.genre, a.more, ru)).joinToString(" · "), style = T.Small)
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Stars(a.stars, size = 28.dp) { save(a.copy(stars = it)) } }
        val heard = tl?.tracks?.size ?: 0
        StatRow(Words.dur(tl?.ms ?: 0, ru) to t("послушано", "listened"), "${tl?.whole ?: 0}" to t("раз целиком", "times whole"),
            (if (a.tracks > 0) "$heard / ${a.tracks}" else "$heard") to t("песен услышано", "songs heard"))
        // The two things done most, as buttons; everything else waits under «More» at the foot.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(t("Послушал", "Heard it"), { nav.go(Overlay.Mark(key, null)) }, Modifier.weight(1f), kind = BtnKind.AMBER)
            Btn(if (a.wish) t("В списке ✓", "On the list ✓") else t("Хочу послушать", "Want to hear"), {
                save(a.copy(wish = !a.wish, added = if (a.added == 0L) System.currentTimeMillis() else a.added))
            }, Modifier.weight(1f), kind = if (a.wish) BtnKind.SOFT else BtnKind.GHOST)
        }
        // Worked out once for the album, not on every recomposition: it walks every listen.
        val songs = remember(st, key) {
        val heardCount = st.plays.filter { it.album == key }.groupingBy { it.track }.eachCount()
        (st.tracks.values.filter { it.album == key }.map { it.key to it.title } + st.plays.filter { it.album == key }.map { it.track to it.title })
            .distinctBy { it.first }.map { (k, title) -> Triple(k, title, heardCount[k] ?: 0) }
            .sortedBy { st.tracks[it.first]?.number?.takeIf { n -> n > 0 } ?: 999 }
        }
        if (songs.isNotEmpty()) {
            // Two sides, as on the record or the tape.
            val half = (songs.size + 1) / 2
            for ((side, part) in listOf(songs.take(half), songs.drop(half)).withIndex()) {
                if (part.isEmpty()) continue
                val len = part.sumOf { st.tracks[it.first]?.length ?: 0L }
                SideHeader(t("Сторона ", "Side ") + (if (side == 0) t("А", "A") else t("Б", "B")),
                    if (len > 0) "%d:%02d".format(len / 60_000, len / 1000 % 60) else null)
                for ((k, title, n) in part) Row(Modifier.fillMaxWidth().clip(app.songling.ui.theme.R8.Sm).clickable { nav.go(Overlay.TrackOf(k)) }.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    val num = st.tracks[k]?.number ?: 0
                    Text(if (num > 0) "$num." else "·", style = T.Small, modifier = Modifier.width(26.dp))
                    Text(title, style = T.Body.copy(color = if (n == 0) C.faint else C.ink), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (n == 0) t("не слышал", "unheard") else Words.times(n, ru), style = T.Small)
                    val mark = t("Отметить прослушивание", "Log a listen")
                    Box(Modifier.padding(start = 8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(C.amberSoft).clickable { nav.go(Overlay.Mark(key, k)) }
                        .semantics { contentDescription = mark }.padding(horizontal = 12.dp, vertical = 9.dp)) {
                        app.songling.ui.components.Sign(app.songling.ui.components.Glyph.PLUS, color = C.amber, size = 11.dp)
                    }
                }
            }
        }
        Section(t("Ещё", "More"))
        Column(Modifier.fillMaxWidth().clip(app.songling.ui.theme.R8.Md).background(C.surface2)) {
            More(t("Изменить альбом", "Edit the album"), t("название, обложка, год, жанр и песни", "title, cover, year, genre and songs")) { nav.go(Overlay.AlbumEdit(key)) }
            app.songling.ui.components.Rule(Modifier.padding(horizontal = 14.dp))
            More(t("Повесить в логово", "Hang it in the den"), null) { nav.go(Overlay.Poster("album:$key")) }
            app.songling.ui.components.Rule(Modifier.padding(horizontal = 14.dp))
            More(t("Поставить цель", "Set a goal"), t("послушать целиком", "hear it whole")) {
                nav.go(Overlay.GoalEdit(draft = Goal(kind = GoalKind.ALBUM, target = 1, period = Period.ONCE, ref = key, label = a.title)))
            }
        }
        Field(note, { note = it }, t("Заметка: где услышал, с чем связан…", "A note: where you heard it, what it reminds you of…"), single = false)
        if (tl != null) Text(t("Впервые ${date(tl.first, true)}, последний раз ${date(tl.last, true)}", "First ${date(tl.first, false)}, last ${date(tl.last, false)}"), style = T.Small)
        if (tl == null) Btn(t("Убрать с полки", "Remove from the shelf"), { co.write { db.dao().delete(a) }; nav.close() }, Modifier.fillMaxWidth(), kind = BtnKind.GHOST)
        else {
            // Listened to already: it goes with its listens, after asking.
            var sure by remember(key) { mutableStateOf(false) }
            Btn(if (sure) t("Удалить вместе с прослушиваниями (${tl.plays})?", "Delete it with its listens (${tl.plays})?") else t("Удалить альбом", "Delete the album"), {
                if (!sure) sure = true else { co.write { val dao = db.dao(); dao.forgetAlbum(key); dao.forgetTracks(key); dao.album(key)?.let { dao.delete(it) } }; nav.close() }
            }, Modifier.fillMaxWidth(), kind = if (sure) BtnKind.CORAL else BtnKind.GHOST)
            if (sure) Text(t("Прослушивания пропадут из статистики, времени питомца и заданий.", "The listens leave the statistics, the pet's time and the goals."), style = T.Small)
        }
    }
}

/** A line of a sheet's «More»: what it does, a word about it, an arrow. */
@Composable
private fun More(title: String, note: String?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = T.Strong)
            if (note != null) Text(note, style = T.Small)
        }
        app.songling.ui.components.Sign(app.songling.ui.components.Glyph.NEXT, color = C.faint, size = 12.dp)
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
                Row(Modifier.fillMaxWidth().clip(app.songling.ui.theme.R8.Sm).clickable { nav.go(Overlay.AlbumOf(k)) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Cover(al?.cover, st.albumTitle(k), al?.genre, 44.dp)
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
            for (s in songs) Row(Modifier.fillMaxWidth().clip(app.songling.ui.theme.R8.Sm).clickable { nav.go(Overlay.TrackOf(s.key)) }.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(s.name, style = T.Body, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(Words.times(s.plays, ru), style = T.Small)
            }
        }
        // Gone with everything billed first to them, after asking: a stray name from a video, a podcast, a misheard tag.
        val own = remember(st, key) { st.plays.count { it.artist == key } }
        var sure by remember(key) { mutableStateOf(false) }
        Btn(when {
            !sure -> t("Удалить артиста", "Delete the artist")
            own > 0 -> t("Удалить вместе с прослушиваниями ($own)?", "Delete with their listens ($own)?")
            else -> t("Точно удалить?", "Delete it?")
        }, {
            if (!sure) sure = true else {
                co.write { db.dao().forgetArtist(key) }
                nav.toast(if (ru) "«${a.name}» удалён" else "«${a.name}» deleted"); nav.close()
            }
        }, Modifier.fillMaxWidth().padding(top = 8.dp), kind = if (sure) BtnKind.CORAL else BtnKind.GHOST)
        if (sure) Text(t("Уйдут его прослушивания, песни и альбомы – из статистики, времени питомца и заданий. Песни, где он только гость (feat.), останутся у главного артиста.",
            "Their listens, songs and albums go – from the statistics, the pet's time and the goals. Songs where they're only a guest (feat.) stay with the lead artist."), style = T.Small)
    }
}

// ---------------------------------------------------------------- song

@Composable
fun TrackSheet(st: Studio, key: String, nav: Nav, hint: app.songling.listen.NowPlaying? = null) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    val tl = st.byTrack[key]
    val tr = st.tracks[key] ?: Track(key, tl?.name ?: hint?.title ?: key.substringAfter('|'), tl?.artistName ?: hint?.artist ?: st.artistName(key.substringBefore('|')),
        albumName = tl?.albumName ?: hint?.album.orEmpty())
    val al = st.albums[tr.album]
    fun save(x: Track) = co.write { db.dao().upsert(x) }
    Sheet(onDismiss = nav::close) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            app.songling.ui.components.Sleeve(al?.cover ?: hint?.cover, al?.title ?: tr.albumName.ifEmpty { tr.title }, tr.artistName, tr.genre ?: al?.genre, 96.dp)
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
        // The album it's in, and a way to put it in another one when the player named the wrong one (#22).
        var picking by remember(key) { mutableStateOf(false) }
        app.songling.ui.components.LinkRow(al?.title ?: tr.albumName.ifEmpty { t("Без альбома", "No album") }, t("альбом · нажми, чтобы поменять", "album · tap to change"), { picking = true })
        if (picking) app.songling.ui.Floating { TrackAlbumPick(st, tr, onDone = { picking = false }) }
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
        // Several genres at once (#23): the song's own list once it was told by hand, else the album's.
        val trMore = (if (tr.more.isNotEmpty() || tr.pinned || tr.own.isNotEmpty()) tr.more else al?.more.orEmpty()).split(',').filter { it.isNotBlank() }
        GenresPick(st, tr.genre ?: al?.genre, tr.own, trMore) { g, o, m ->
            save(tr.copy(genre = g, own = o, more = m.joinToString(","), pinned = if (o.isEmpty()) g != null else tr.pinned || g != null))
        }
        if (tl == null) Btn(t("Убрать с полки", "Remove from the shelf"), { co.write { db.dao().delete(tr) }; nav.close() }, Modifier.fillMaxWidth(), kind = BtnKind.GHOST)
        else {
            var sure by remember(key) { mutableStateOf(false) }
            Btn(if (sure) t("Удалить вместе с прослушиваниями (${tl.plays})?", "Delete it with its listens (${tl.plays})?") else t("Удалить песню", "Delete the song"), {
                if (!sure) sure = true else { co.write { val dao = db.dao(); dao.forgetTrack(key); dao.track(key)?.let { dao.delete(it) } }; nav.close() }
            }, Modifier.fillMaxWidth(), kind = if (sure) BtnKind.CORAL else BtnKind.GHOST)
            if (sure) Text(t("Например, сборник одним файлом: прослушивания пропадут из статистики, времени питомца и заданий.",
                "A compilation in one file, say: the listens leave the statistics, the pet's time and the goals."), style = T.Small)
        }
    }
}

/** Which album a song belongs to: the artist's albums first, any album on the shelf by search. */
@Composable
private fun TrackAlbumPick(st: Studio, tr: Track, onDone: () -> Unit) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    var q by remember { mutableStateOf("") }
    val nq = Keys.norm(q)
    val options = remember(st, nq) {
        val all = (st.albums.keys + st.byAlbum.keys).distinct()
        // This artist's own albums lead; a search reaches every album on the shelf.
        all.filter { k ->
            val artist = st.albums[k]?.artist ?: k.substringBefore('|')
            if (nq.isEmpty()) artist == tr.artist || tr.artist in Keys.artists(st.albums[k]?.artistName ?: st.byAlbum[k]?.artistName.orEmpty())
            else Keys.norm(st.albumTitle(k)).contains(nq) || Keys.norm(st.albums[k]?.artistName ?: st.byAlbum[k]?.artistName.orEmpty()).contains(nq)
        }.sortedByDescending { st.byAlbum[it]?.ms ?: 0L }.take(30)
    }
    Sheet(onDismiss = onDone) {
        Text(t("В какой альбом?", "Which album?"), style = T.Title)
        Text(t("«${tr.title}» переедет туда вместе с прослушиваниями, и новые прослушивания этой песни тоже будут попадать туда.",
            "«${tr.title}» moves there with its listens, and its next listens land there too."), style = T.Muted)
        Field(q, { q = it }, t("Найти альбом", "Find an album"), search = true)
        if (options.isEmpty()) Text(if (nq.isEmpty()) t("У этого артиста других альбомов нет – найди любой поиском.", "This artist has no other albums – search for any.")
            else t("Ничего не нашлось.", "Nothing found."), style = T.Muted)
        for (k in options) {
            val a = st.albums[k]
            val on = k == tr.album
            Row(Modifier.fillMaxWidth().clip(app.songling.ui.theme.R8.Md).background(if (on) C.amberSoft else androidx.compose.ui.graphics.Color.Transparent)
                .clickable {
                    if (!on) {
                        val target = a ?: Album(k, st.albumTitle(k), st.byAlbum[k]?.artistName ?: tr.artistName)
                        co.write { db.dao().placeTrackByHand(tr.key, target, tr) }
                    }
                    onDone()
                }.padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Cover(a?.cover, st.albumTitle(k), a?.genre, 44.dp)
                Column(Modifier.weight(1f)) {
                    Text(st.albumTitle(k), style = T.Strong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(listOfNotNull(a?.artistName ?: st.byAlbum[k]?.artistName, a?.year?.toString()).joinToString(" · "), style = T.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (on) app.songling.ui.components.Sign(app.songling.ui.components.Glyph.CHECK, color = C.amber, size = 14.dp)
            }
        }
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
