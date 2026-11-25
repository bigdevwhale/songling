package app.songling.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
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
/** Colours for genres of one's own: 24 – the bright twelve, soft pastels and deep shades (#36) – and any hue by the slider. */
private val SWATCHES = listOf(0xFFD0483A, 0xFFE86A3A, 0xFFF0A63A, 0xFFE2C24A, 0xFF8AB44A, 0xFF4CA86A, 0xFF3EA0A0, 0xFF4C8AD8, 0xFF6A6AE0, 0xFF9A5AD0, 0xFFD05AA0, 0xFF8A6A5A,
    0xFFF2A0A0, 0xFFF6C08A, 0xFFF0DE8A, 0xFFB6DC8E, 0xFF8ED8C0, 0xFF9CC8F0, 0xFFC0B0F2, 0xFFF0B0DC,
    0xFF8A2E2E, 0xFF2E5E3A, 0xFF24486E, 0xFF3A3A44)

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

/**
 * Picks one genre or several (#23). The first one picked is the main one – the colour and the cover go by it – and
 * the rest go to [more]; a listen counts in full for every one of them. [onChange] gets the main built-in genre, the
 * main genre of one's own (or ""), and the rest as tokens: a [Genre] name or «own:<id>».
 */
@Composable
fun GenresPick(st: Studio, main: Genre?, own: String, more: List<String>, onChange: (Genre?, String, List<String>) -> Unit) {
    val ru = LocalRu.current
    var making by remember { mutableStateOf(false) }
    val picked = (listOfNotNull(if (own.isNotEmpty()) "own:$own" else main?.name) + more).distinct()
    fun set(list: List<String>) {
        val first = list.firstOrNull()
        when {
            first == null -> onChange(null, "", emptyList())
            first.startsWith("own:") -> onChange(main, first.removePrefix("own:"), list.drop(1))
            else -> onChange(Genre.entries.firstOrNull { it.name == first }, "", list.drop(1))
        }
    }
    fun toggle(k: String) = set(if (k in picked) picked - k else picked + k)
    Text(if (picked.size > 1) t("Главный – ${tokenName(st, picked.first(), true)}: по нему цвет. Считаются все.", "The main one is ${tokenName(st, picked.first(), false)}: the colour goes by it. All of them count.")
        else t("Можно выбрать несколько – первый станет главным.", "Pick several if you like – the first one is the main one."), style = T.Small)
    Flow {
        for (o in st.s.own) Chip(o.name, "own:${o.id}" in picked, { toggle("own:" + o.id) }, color = Color(o.color))
        Chip(t("+ свой жанр", "+ your own"), false, { making = true }, color = C.lilac)
    }
    Flow {
        for (g in Genre.entries) Chip(Words.genre(g, ru), g.name in picked, { toggle(g.name) }, color = C.genre(g))
    }
    if (making) Floating {
        OwnGenreSheet(st, null, onSaved = { g -> set(picked + ("own:" + g.id)); making = false }, onClose = { making = false })
    }
}

private fun tokenName(st: Studio, k: String, ru: Boolean): String =
    if (k.startsWith("own:")) st.owns[k.removePrefix("own:")]?.name ?: "?" else Genre.entries.firstOrNull { it.name == k }?.let { Words.genre(it, ru) } ?: k

/** A thing's genres in a line: the main one, then the rest (#23). */
fun genresName(st: Studio, own: String, g: Genre?, more: String, ru: Boolean): String? {
    val rest = more.split(',').filter { it.isNotBlank() }.map { tokenName(st, it, ru) }
    return (listOfNotNull(genreName(st, own, g, ru)) + rest).distinct().joinToString(", ").ifEmpty { null }
}

/** Make or change a genre of one's own: a name, a colour and what it's closest to. */
@Composable
fun OwnGenreSheet(st: Studio, old: OwnGenre?, onSaved: (OwnGenre) -> Unit, onClose: () -> Unit) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    var name by remember { mutableStateOf(old?.name.orEmpty()) }
    var color by remember { mutableStateOf(old?.color ?: SWATCHES[(st.s.own.size * 5) % SWATCHES.size]) }
    var base by remember { mutableStateOf(old?.genre ?: Genre.OTHER) }
    // Until a genre is picked by hand, the name suggests one: «Эмбиент» is closest to ambient (#62).
    var picked by remember { mutableStateOf(old != null && old.base != Genre.OTHER.name) }
    Sheet(onDismiss = onClose) {
        Text(if (old == null) t("Свой жанр", "A genre of your own") else t("Жанр", "Genre"), style = T.Title)
        FormField(name, { name = it.take(28); if (!picked) base = Genre.of(name) ?: Genre.OTHER }, t("Название: «пост-рок для работы», «бабушкины пластинки»…", "Name: «post-rock for work», «grandma's records»…"))
        Section(t("Цвет", "Colour"))
        // colours other genres of one's own already have get a small dot, so each can have its own (#36)
        val taken = st.s.own.filter { it.id != old?.id }.map { it.color }.toSet()
        for (row in SWATCHES.chunked(6)) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (c in row) Swatch(c, c == color, c in taken) { color = c }
        }
        // any hue at all
        var hue by remember { mutableStateOf(FloatArray(3).also { android.graphics.Color.colorToHSV(color.toInt(), it) }[0]) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(t("Свой оттенок", "Any hue"), style = T.Small)
            androidx.compose.material3.Slider(hue, { hue = it; color = android.graphics.Color.HSVToColor(floatArrayOf(it, .62f, .82f)).toLong() and 0xFFFFFFFFL }, Modifier.weight(1f), valueRange = 0f..359f,
                colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = Color(color), activeTrackColor = Color(color)))
            Box(Modifier.size(28.dp).clip(CircleShape).background(Color(color)))
        }
        Section(t("На что похож", "Closest to"), t("для питомцев и форм", "for pets and forms"))
        Flow { for (g in Genre.entries) Chip(Words.genre(g, ru), g == base, { base = g; picked = true }, color = C.genre(g)) }
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
private fun Swatch(c: Long, on: Boolean, taken: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.size(36.dp).clip(CircleShape).background(Color(c)).then(if (on) Modifier.border(3.dp, C.ink, CircleShape) else Modifier).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        if (taken && !on) Box(Modifier.size(8.dp).clip(CircleShape).background(Color.White.copy(alpha = .85f)))
    }
}

/** What a thing's genre is called: one's own if set, else the built-in one. */
fun genreName(st: Studio, own: String, g: Genre?, ru: Boolean): String? = st.owns[own]?.name ?: g?.let { Words.genre(it, ru) }

/** And its colour. */
fun genreColour(st: Studio, own: String, g: Genre?): Color? = st.owns[own]?.let { Color(it.color) } ?: g?.let { C.genre(it) }
