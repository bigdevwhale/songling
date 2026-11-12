package app.songling.ui.saga

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.songling.container
import app.songling.domain.Feat
import app.songling.domain.FeatGroup
import app.songling.domain.FeatState
import app.songling.domain.Feats
import app.songling.domain.Studio
import app.songling.domain.Words
import app.songling.ui.LocalRu
import app.songling.ui.components.Bar
import app.songling.ui.components.Card
import app.songling.ui.components.Chip
import app.songling.ui.components.Section
import app.songling.ui.components.t
import app.songling.ui.theme.C
import app.songling.ui.theme.R8
import app.songling.ui.theme.T

/** Achievements: a page of the stats, by shelves; steps not seen yet are marked new until the page is opened. */
object FeatsPage {
    private val icons = HashMap<Feat, ImageBitmap?>()
    private val grey = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

    /** The achievement's picture, drawn by Codex (tools/icons_cut.py). */
    private fun icon(f: Feat): ImageBitmap? = synchronized(icons) {
        icons.getOrPut(f) {
            runCatching { FeatsPage::class.java.getResourceAsStream("/feats/${f.name.lowercase()}.png")?.use { BitmapFactory.decodeStream(it) }?.asImageBitmap() }.getOrNull()
        }
    }

    private fun seen(st: Studio, name: String): Int = st.s.feats.firstOrNull { it.substringBefore(':') == name }?.substringAfter(':')?.toIntOrNull() ?: 0

    /** Achievements that climbed a step since they were last shown. */
    fun fresh(st: Studio): List<FeatState> = st.feats.filter { it.level > seen(st, it.f.name) }

    /** Bronze, silver, gold and on: the colour of the step reached. */
    private fun medal(level: Int, max: Int): Color = when {
        level == 0 -> C.surface2
        level == max -> Color(0xFFE8B931)
        level * 3 >= max * 2 -> Color(0xFFB9C3CC)
        else -> Color(0xFFCD8B5A)
    }

    @Composable
    fun Page(st: Studio) {
        val ru = LocalRu.current
        val co = LocalContext.current.container
        // What was new when the page opened stays marked while it is looked at.
        val news = remember { fresh(st).map { it.f }.toSet() }
        LaunchedEffect(st.feats) {
            if (news.isNotEmpty() || fresh(st).isNotEmpty()) co.write { settings.update { s -> s.copy(feats = st.feats.filter { it.level > 0 }.map { "${it.f.name}:${it.level}" }.toSet()) } }
        }
        val all = st.feats
        val steps = all.sumOf { it.level }
        val most = all.sumOf { it.max }
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t("$steps из $most ступеней", "$steps of $most steps"), style = T.Title)
                    Text(t("Наград получено: ${all.count { it.level > 0 }} из ${all.size}. Каждая ступень – ${Feats.REWARD} медиаторов.",
                        "Awards earned: ${all.count { it.level > 0 }} of ${all.size}. Every step pays ${Feats.REWARD} plectrums."), style = T.Small)
                }
                icon(Feat.GOALS)?.let { Image(it, null, Modifier.size(44.dp), filterQuality = FilterQuality.Low) }
            }
            Bar(steps / most.toFloat().coerceAtLeast(1f), Modifier.fillMaxWidth().padding(top = 8.dp))
        }
        var only by remember { mutableStateOf(false) }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip(t("Все", "All"), !only, { only = false })
            Chip(t("Полученные", "Earned"), only, { only = true })
        }
        for (g in FeatGroup.entries) {
            val list = all.filter { it.f.group == g && (!only || it.level > 0) }
            if (list.isEmpty()) continue
            Section(Feats.group(g, ru), "${list.count { it.level > 0 }}/${all.count { it.f.group == g }}")
            for (fs in list) Item(fs, fs.f in news, ru)
        }
    }

    @Composable
    private fun Item(fs: FeatState, new: Boolean, ru: Boolean) {
        val color = medal(fs.level, fs.max)
        Row(Modifier.fillMaxWidth().clip(R8.Md).background(C.surface).border(1.dp, if (new) C.amber else C.line, R8.Md).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                icon(fs.f)?.let { Image(it, null, Modifier.size(40.dp), filterQuality = FilterQuality.Low, alpha = if (fs.level > 0) 1f else .45f,
                    colorFilter = if (fs.level > 0) null else grey) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(Feats.name(fs.f, ru), style = T.Strong.copy(color = if (fs.level > 0) C.ink else C.muted), modifier = Modifier.weight(1f))
                    if (new) Text(t("новое", "new"), style = T.Small.copy(color = C.amber))
                    // One dot a step.
                    if (fs.max > 1) Row(Modifier.padding(start = 6.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(fs.max) { i -> Box(Modifier.size(7.dp).clip(CircleShape).background(if (i < fs.level) medal(i + 1, fs.max) else C.surface2)) }
                    }
                }
                Text(Feats.goal(fs.f, fs.need, ru), style = T.Small)
                if (!fs.done) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Bar(fs.part, Modifier.weight(1f), if (fs.level > 0) C.mint else C.amber)
                    Text("${Words.num(fs.have)} / ${Words.num(fs.need)}", style = T.Small.copy(color = C.faint))
                } else Text(t("Получено полностью", "Fully earned"), style = T.Small.copy(color = C.mint))
            }
        }
    }
}
