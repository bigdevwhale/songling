package app.songling.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.songling.container
import app.songling.data.OwnGenre
import app.songling.domain.Genre
import app.songling.domain.Studio
import app.songling.domain.Words
import app.songling.ui.Floating
import app.songling.ui.LocalRu
import app.songling.ui.components.Btn
import app.songling.ui.components.Chip
import app.songling.ui.components.Flow
import app.songling.ui.components.FormField
import app.songling.ui.components.Kind
import app.songling.ui.components.Section
import app.songling.ui.components.Sheet
import app.songling.ui.components.t
import app.songling.ui.theme.C
import app.songling.ui.theme.T

/** Colours a genre of one's own can take. */
private val SWATCHES = listOf(0xFFD0483A, 0xFFE86A3A, 0xFFF0A63A, 0xFFE2C24A, 0xFF8AB44A, 0xFF4CA86A, 0xFF3EA0A0, 0xFF4C8AD8, 0xFF6A6AE0, 0xFF9A5AD0, 0xFFD05AA0, 0xFF8A6A5A)

/**
 * Picks a genre: one's own first (with «+ своё» to make a new one), then the built-in ones. Picking
 * one's own calls [onOwn] with its id; picking a built-in one calls [onPick] (and clears one's own).
 */
@Composable
fun GenrePick(st: Studio, current: Genre?, own: String, onPick: (Genre?) -> Unit, onOwn: (String) -> Unit) {
    val ru = LocalRu.current
    var editing by remember { mutableStateOf<OwnGenre?>(null) }
    var making by remember { mutableStateOf(false) }
    Flow {
        for (o in st.s.own) Chip(o.name, o.id == own, { onOwn(if (o.id == own) "" else o.id) }, color = Color(o.color))
        Chip(t("+ свой жанр", "+ your own"), false, { making = true }, color = C.lilac)
    }
    Flow {
        for (g in Genre.entries) Chip(Words.genre(g, ru), own.isEmpty() && g == current, { onPick(if (g == current && own.isEmpty()) null else g) }, color = C.genre(g))
    }
    if (making || editing != null) Floating {
        OwnGenreSheet(st, editing, onSaved = { g -> if (making) onOwn(g.id); making = false; editing = null }, onClose = { making = false; editing = null })
    }
}

/** Make or change a genre of one's own: a name, a colour and what it's closest to. */
@Composable
fun OwnGenreSheet(st: Studio, old: OwnGenre?, onSaved: (OwnGenre) -> Unit, onClose: () -> Unit) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    var name by remember { mutableStateOf(old?.name.orEmpty()) }
    var color by remember { mutableStateOf(old?.color ?: SWATCHES[(st.s.own.size * 5) % SWATCHES.size]) }
    var base by remember { mutableStateOf(old?.base?.let { b -> Genre.entries.firstOrNull { it.name == b } } ?: Genre.OTHER) }
    Sheet(onDismiss = onClose) {
        Text(if (old == null) t("Свой жанр", "A genre of your own") else t("Жанр", "Genre"), style = T.Title)
        FormField(name, { name = it.take(28) }, t("Название: «пост-рок для работы», «бабушкины пластинки»…", "Name: «post-rock for work», «grandma's records»…"))
        Section(t("Цвет", "Colour"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (c in SWATCHES.take(6)) Swatch(c, c == color) { color = c }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (c in SWATCHES.drop(6)) Swatch(c, c == color) { color = c }
        }
        Section(t("На что похож", "Closest to"), t("для питомцев и форм", "for pets and forms"))
        Flow { for (g in Genre.entries) Chip(Words.genre(g, ru), g == base, { base = g }, color = C.genre(g)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (old != null) Btn(t("Удалить", "Delete"), {
                co.write { settings.update { s -> s.copy(own = s.own.filter { it.id != old.id }) } }
                onClose()
            }, Modifier.weight(1f), kind = Kind.GHOST)
            Btn(t("Сохранить", "Save"), {
                val g = OwnGenre(old?.id ?: "g${System.currentTimeMillis()}", name.trim(), color, base.name)
                co.write { settings.update { s -> s.copy(own = if (old == null) s.own + g else s.own.map { if (it.id == g.id) g else it }) } }
                onSaved(g)
            }, Modifier.weight(1f), kind = Kind.AMBER, enabled = name.isNotBlank())
        }
    }
}

@Composable
private fun Swatch(c: Long, on: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(36.dp).clip(CircleShape).background(Color(c)).then(if (on) Modifier.border(3.dp, C.ink, CircleShape) else Modifier).clickable(onClick = onClick))
}

/** What a thing's genre is called: one's own if set, else the built-in one. */
fun genreName(st: Studio, own: String, g: Genre?, ru: Boolean): String? = st.owns[own]?.name ?: g?.let { Words.genre(it, ru) }

/** And its colour. */
fun genreColour(st: Studio, own: String, g: Genre?): Color? = st.owns[own]?.let { Color(it.color) } ?: g?.let { C.genre(it) }
