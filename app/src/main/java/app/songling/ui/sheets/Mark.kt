package app.songling.ui.sheets

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.songling.container
import app.songling.data.DAY
import app.songling.data.MIN
import app.songling.data.Play
import app.songling.data.Source
import app.songling.domain.Studio
import app.songling.domain.Words
import app.songling.ui.LocalRu
import app.songling.ui.Nav
import app.songling.ui.components.Btn
import app.songling.ui.components.Chip
import app.songling.ui.components.Flow
import app.songling.ui.components.Kind
import app.songling.ui.components.Section
import app.songling.ui.components.Sheet
import app.songling.ui.components.Stepper
import app.songling.ui.components.t
import app.songling.ui.theme.C
import app.songling.ui.theme.T

/**
 * «I listened to this»: a whole album (each of its songs, in order, when the track list is known) or
 * one song, from an album or on its own. Says when and how.
 */
@Composable
fun MarkSheet(st: Studio, album: String?, track: String?, nav: Nav) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    var source by remember { mutableStateOf(Source.MANUAL) }
    var daysAgo by remember { mutableIntStateOf(0) }
    val a = album?.let { st.albums[it] }
    val tr = track?.let { st.tracks[it] }
    val songTitle = track?.let { st.trackTitle(it) }
    val artist = tr?.artistName ?: a?.artistName ?: track?.let { st.byTrack[it]?.artistName } ?: album?.let { st.byAlbum[it]?.artistName } ?: ""
    val albumTitle = a?.title ?: tr?.albumName ?: album?.let { st.albumTitle(it) } ?: ""
    // The album's songs in order, from the catalogue and from what was heard.
    val songs = album?.let { k ->
        (st.tracks.values.filter { it.album == k }.map { Triple(it.key, it.title, it.number) } +
            st.plays.filter { it.album == k }.map { Triple(it.track, it.title, st.tracks[it.track]?.number ?: 0) })
            .distinctBy { it.first }.sortedBy { if (it.third > 0) it.third else 999 }
    }.orEmpty()
    Sheet(onDismiss = nav::close) {
        Text(t("Отметить прослушивание", "Log a listen"), style = T.Title)
        Text(if (track != null) "«$songTitle» · $artist" else "«$albumTitle» · $artist", style = T.Name.copy(color = C.amber))
        Text(when {
            track != null && albumTitle.isNotEmpty() -> t("Песня с альбома «$albumTitle».", "A song from «$albumTitle».")
            track != null -> t("Песня сама по себе.", "A song on its own.")
            songs.size >= 3 -> t("Весь альбом целиком: ${songs.size} ${Words.plural(songs.size, "песня", "песни", "песен")} подряд.", "The whole album: ${songs.size} songs in a row.")
            else -> t("Весь альбом целиком.", "The whole album.")
        }, style = T.Muted)
        Section(t("Как", "How"))
        Flow { for (s in listOf(Source.MANUAL, Source.VINYL, Source.RADIO)) Chip(Words.source(s, ru), s == source, { source = s }) }
        Section(t("Когда", "When"))
        Stepper(when (daysAgo) { 0 -> t("сегодня", "today"); 1 -> t("вчера", "yesterday"); else -> t("$daysAgo дн. назад", "$daysAgo days ago") }) { daysAgo = (daysAgo + it).coerceIn(0, 365) }
        Btn(t("Отметить", "Log it"), {
            val now = System.currentTimeMillis()
            co.write {
                val dao = db.dao()
                if (track != null) {
                    val len = tr?.length?.takeIf { it > 0 } ?: (3.5 * MIN).toLong()
                    val at = if (daysAgo == 0) now - len else st.startOf(st.today.minusDays(daysAgo.toLong())) + 18 * 3_600_000L
                    val p = Play(at = at, ms = len, title = songTitle!!, artistName = artist, albumName = albumTitle, length = len, source = source, whole = true)
                    dao.insert(p); meta.note(p)
                } else if (album != null) {
                    var at = if (daysAgo == 0) now - songs.sumOf { (st.tracks[it.first]?.length ?: 210_000L) } else st.startOf(st.today.minusDays(daysAgo.toLong())) + 19 * 3_600_000L
                    if (songs.size >= 3) for ((k, title, _) in songs) {
                        val len = st.tracks[k]?.length?.takeIf { it > 0 } ?: 210_000L
                        val p = Play(at = at, ms = len, title = title, artistName = artist, albumName = albumTitle, length = len, source = source, whole = true)
                        dao.insert(p); meta.note(p); at += len + 2_000
                    } else {
                        val len = a?.length?.takeIf { it > 0 } ?: 40 * MIN
                        val p = Play(at = at, ms = len, title = albumTitle, artistName = artist, albumName = albumTitle, source = source, whole = true)
                        dao.insert(p); meta.note(p)
                    }
                }
            }
            nav.toast(if (ru) "Отмечено" else "Logged")
            nav.close()
        }, Modifier.fillMaxWidth(), kind = Kind.AMBER)
        if (daysAgo > 0) Text(t("Время – вечер того дня.", "Logged for that evening."), style = T.Small)
    }
}
