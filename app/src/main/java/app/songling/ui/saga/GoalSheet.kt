package app.songling.ui.saga

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.songling.container
import app.songling.data.Goal
import app.songling.data.GoalKind
import app.songling.data.Keys
import app.songling.data.MIN
import app.songling.data.Period
import app.songling.domain.Genre
import app.songling.domain.Goals
import app.songling.domain.Progress
import app.songling.domain.Studio
import app.songling.domain.Words
import app.songling.ui.LocalRu
import app.songling.ui.Nav
import app.songling.ui.components.Bar
import app.songling.ui.components.Btn
import app.songling.ui.components.Chip
import app.songling.ui.components.Field
import app.songling.ui.components.Flow
import app.songling.ui.components.Kind
import app.songling.ui.components.Section
import app.songling.ui.components.Sheet
import app.songling.ui.components.Stepper
import app.songling.ui.components.t
import app.songling.ui.theme.C
import app.songling.ui.theme.T

object GoalText {
    fun title(st: Studio, g: Goal, ru: Boolean): String {
        val n = g.target
        val h = if (Goals.inMinutes(g.kind)) Words.dur(n * MIN, ru) else "$n"
        val ref = g.label.ifBlank {
            when (g.kind) {
                GoalKind.ARTIST -> st.artistName(g.ref)
                GoalKind.ALBUM -> st.albumTitle(g.ref)
                GoalKind.TRACK -> st.trackTitle(g.ref)
                GoalKind.GENRE -> if (g.ref.startsWith("own:")) st.owns[g.ref.removePrefix("own:")]?.name ?: "" else Genre.entries.firstOrNull { it.name == g.ref }?.let { Words.genre(it, ru) } ?: g.ref
                else -> ""
            }
        }
        return if (ru) when (g.kind) {
            GoalKind.MINUTES -> "$h музыки"
            GoalKind.ALBUMS -> "$n ${Words.plural(n, "альбом", "альбома", "альбомов")} целиком"
            GoalKind.NEW_ARTISTS -> "$n ${Words.plural(n, "новый артист", "новых артиста", "новых артистов")}"
            GoalKind.NEW_TRACKS -> "$n ${Words.plural(n, "новая песня", "новые песни", "новых песен")}"
            GoalKind.ARTIST -> "$h – $ref"
            GoalKind.ALBUM -> "«$ref» целиком" + if (n > 1) " $n ${Words.plural(n, "раз", "раза", "раз")}" else ""
            GoalKind.TRACK -> "«$ref» – ${Words.times(n, true)}"
            GoalKind.GENRE -> "$h – $ref"
            GoalKind.GENRES -> "$n ${Words.plural(n, "жанр", "жанра", "жанров")}"
            GoalKind.COUNTRIES -> "Артисты из $n ${Words.plural(n, "страны", "стран", "стран")}"
            GoalKind.DECADES -> "Музыка $n ${Words.plural(n, "десятилетия", "десятилетий", "десятилетий")}"
            GoalKind.DAYS -> "$n ${Words.plural(n, "день", "дня", "дней")} с музыкой"
            GoalKind.WISHES -> "$n из списка «послушать»"
            GoalKind.PLAYS -> "$n ${Words.plural(n, "прослушивание", "прослушивания", "прослушиваний")}"
            GoalKind.NEW_ALBUMS -> "$n ${Words.plural(n, "новый альбом", "новых альбома", "новых альбомов")}"
            GoalKind.NEW_GENRES -> "$n ${Words.plural(n, "новый жанр", "новых жанра", "новых жанров")}"
            GoalKind.NEW_COUNTRIES -> "Музыка из $n ${Words.plural(n, "новой страны", "новых стран", "новых стран")}"
        } else when (g.kind) {
            GoalKind.MINUTES -> "$h of music"
            GoalKind.ALBUMS -> "$n whole album${if (n == 1) "" else "s"}"
            GoalKind.NEW_ARTISTS -> "$n new artist${if (n == 1) "" else "s"}"
            GoalKind.NEW_TRACKS -> "$n new song${if (n == 1) "" else "s"}"
            GoalKind.ARTIST -> "$h of $ref"
            GoalKind.ALBUM -> "«$ref», whole" + if (n > 1) " $n times" else ""
            GoalKind.TRACK -> "«$ref», ${Words.times(n, false)}"
            GoalKind.GENRE -> "$h of $ref"
            GoalKind.GENRES -> "$n genres"
            GoalKind.COUNTRIES -> "Artists from $n countries"
            GoalKind.DECADES -> "Music from $n decades"
            GoalKind.DAYS -> "$n days with music"
            GoalKind.WISHES -> "$n from the «to hear» list"
            GoalKind.PLAYS -> "$n listens"
            GoalKind.NEW_ALBUMS -> "$n new album${if (n == 1) "" else "s"}"
            GoalKind.NEW_GENRES -> "$n new genre${if (n == 1) "" else "s"}"
            GoalKind.NEW_COUNTRIES -> "Music from $n new countr${if (n == 1) "y" else "ies"}"
        }
    }

    fun progress(g: Goal, p: Progress, ru: Boolean) =
        if (Goals.inMinutes(g.kind)) "${Words.dur(p.have * MIN, ru)} / ${Words.dur(p.need * MIN, ru)}" else "${p.have} / ${p.need}"
}

object Presets {
    fun all(ru: Boolean) = listOf(
        Goal(kind = GoalKind.NEW_ALBUMS, target = 4, period = Period.MONTH),
        Goal(kind = GoalKind.NEW_ARTISTS, target = 10, period = Period.MONTH),
        Goal(kind = GoalKind.NEW_GENRES, target = 2, period = Period.MONTH),
        Goal(kind = GoalKind.ALBUMS, target = 4, period = Period.MONTH),
        Goal(kind = GoalKind.MINUTES, target = 30 * 60, period = Period.MONTH),
        Goal(kind = GoalKind.ALBUMS, target = 52, period = Period.YEAR),
        Goal(kind = GoalKind.COUNTRIES, target = 10, period = Period.YEAR),
        Goal(kind = GoalKind.WISHES, target = 3, period = Period.MONTH),
    )
}

private val NEEDS_REF = setOf(GoalKind.ARTIST, GoalKind.ALBUM, GoalKind.TRACK, GoalKind.GENRE)

@Composable
fun GoalSheet(st: Studio, id: Long?, draft: Goal?, nav: Nav) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    val old = id?.let { i -> st.goals.firstOrNull { it.id == i } }
    val base = old ?: draft ?: Goal(kind = GoalKind.ALBUMS, target = 4, period = Period.MONTH)
    var kind by remember { mutableStateOf(base.kind) }
    var target by remember { mutableIntStateOf(base.target) }
    var period by remember { mutableStateOf(base.period) }
    var ref by remember { mutableStateOf(base.ref) }
    var label by remember { mutableStateOf(base.label) }
    var q by remember { mutableStateOf("") }
    val g = base.copy(kind = kind, target = target, period = period, ref = ref, label = label)
    Sheet(onDismiss = nav::close) {
        Text(if (old == null) t("Новая цель", "New goal") else t("Цель", "Goal"), style = T.Title)
        Text(GoalText.title(st, g, ru), style = T.Name.copy(color = C.amber))
        if (old != null) {
            val p = Goals.progress(st, old)
            Bar(p.part, Modifier.fillMaxWidth(), if (p.done) C.mint else C.amber)
            Text(GoalText.progress(old, p, ru), style = T.Small)
        }
        // The kinds of goal, in groups that say what they're for.
        val groups = listOf(
            t("Открывать новое", "Discover") to listOf(GoalKind.NEW_ALBUMS, GoalKind.NEW_ARTISTS, GoalKind.NEW_TRACKS, GoalKind.NEW_GENRES, GoalKind.NEW_COUNTRIES, GoalKind.WISHES),
            t("Слушать больше", "Listen more") to listOf(GoalKind.MINUTES, GoalKind.ALBUMS, GoalKind.DAYS, GoalKind.PLAYS),
            t("Что-то одно", "One thing") to listOf(GoalKind.ARTIST, GoalKind.ALBUM, GoalKind.TRACK, GoalKind.GENRE),
            t("Разнообразие", "Variety") to listOf(GoalKind.GENRES, GoalKind.COUNTRIES, GoalKind.DECADES),
        )
        for ((title, kinds) in groups) {
            Section(title)
            Flow {
                for (k in kinds) Chip(Words.goalKind(k, ru), k == kind, {
                    if (k != kind) {
                        kind = k; ref = ""; label = ""
                        target = when (k) { GoalKind.MINUTES -> 20 * 60; GoalKind.ARTIST, GoalKind.GENRE -> 5 * 60; GoalKind.ALBUM -> 1; GoalKind.TRACK -> 10; GoalKind.ALBUMS -> 4; GoalKind.DAYS -> 20; GoalKind.PLAYS -> 300
                            GoalKind.NEW_ALBUMS -> 4; GoalKind.NEW_ARTISTS -> 10; GoalKind.NEW_TRACKS -> 30; GoalKind.NEW_GENRES -> 2; GoalKind.NEW_COUNTRIES -> 3; else -> 5 }
                    }
                })
            }
        }
        if (kind in NEEDS_REF) {
            Section(when (kind) { GoalKind.ARTIST -> t("Артист", "Artist"); GoalKind.ALBUM -> t("Альбом", "Album"); GoalKind.TRACK -> t("Песня", "Song"); else -> t("Жанр", "Genre") })
            if (kind == GoalKind.GENRE) Flow {
                for (o in st.s.own) Chip(o.name, ref == "own:" + o.id, { ref = "own:" + o.id; label = o.name }, color = androidx.compose.ui.graphics.Color(o.color))
                for (gg in Genre.entries) Chip(Words.genre(gg, ru), ref == gg.name, { ref = gg.name; label = "" }, color = C.genre(gg))
            } else {
                if (ref.isNotEmpty()) Text("✓ " + (label.ifBlank { GoalText.title(st, g, ru) }), style = T.Strong.copy(color = C.mint))
                Field(q, { q = it }, t("Найти на полке", "Search the shelf"))
                val nq = Keys.norm(q)
                val options = when (kind) {
                    GoalKind.ARTIST -> st.shelfArtists.toSet().map { it to st.artistName(it) }
                    GoalKind.ALBUM -> st.shelfAlbums.toSet().map { it to "${st.albumTitle(it)} · ${st.albums[it]?.artistName ?: st.byAlbum[it]?.artistName.orEmpty()}" }
                    else -> st.shelfTracks.toSet().map { it to "${st.trackTitle(it)} · ${st.byTrack[it]?.artistName ?: st.tracks[it]?.artistName.orEmpty()}" }
                }.filter { nq.isEmpty() || Keys.norm(it.second).contains(nq) }
                    .sortedByDescending { (k, _) -> when (kind) { GoalKind.ARTIST -> st.byArtist[k]?.ms; GoalKind.ALBUM -> st.byAlbum[k]?.ms; else -> st.byTrack[k]?.ms } ?: 0 }
                    .take(8)
                for ((k, name) in options) Text(name, style = T.Body.copy(color = if (k == ref) C.amber else C.ink), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().clickable { ref = k; label = name.substringBefore(" · ") }.padding(vertical = 6.dp))
            }
        }
        Section(t("Сколько", "How much"))
        val minutes = Goals.inMinutes(kind)
        Stepper(if (minutes) Words.dur(target * MIN, ru) else "$target") { d ->
            target = if (minutes) (target + d * 60).coerceIn(60, 100_000) else (target + d).coerceIn(1, 10_000)
        }
        Section(t("За какой срок", "Over what time"))
        Flow { for (p in Period.entries) Chip(Words.period(p, ru), p == period, { period = p }) }
        Text(when (period) {
            Period.ONCE -> t("Считается с сегодняшнего дня, пока не наберётся. Готовая разовая цель – 15 медиаторов.", "Counts from today until it's reached. A finished one-off goal pays 15 plectrums.")
            else -> t("Начинается заново каждую неделю, месяц или год.", "Starts over every week, month or year.")
        }, style = T.Small)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
            Btn(t("Сохранить", "Save"), {
                co.write { if (old == null) db.dao().insert(g.copy(id = 0, created = System.currentTimeMillis())) else db.dao().update(g) }
                nav.close()
            }, Modifier.weight(1f), kind = Kind.AMBER, enabled = kind !in NEEDS_REF || ref.isNotEmpty())
            if (old != null) Btn(if (old.archived) t("Вернуть", "Restore") else t("В архив", "Archive"), { co.write { db.dao().update(old.copy(archived = !old.archived)) }; nav.close() }, kind = Kind.SOFT)
        }
        if (old != null) Btn(t("Удалить цель", "Delete the goal"), { co.write { db.dao().delete(old) }; nav.close() }, Modifier.fillMaxWidth(), kind = Kind.GHOST, small = true)
    }
}
