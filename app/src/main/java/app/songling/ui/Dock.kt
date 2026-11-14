package app.songling.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.songling.R
import app.songling.ui.art.Art
import app.songling.ui.components.t
import app.songling.ui.theme.C
import app.songling.ui.theme.T
import app.songling.ui.theme.paper

/**
 * The bottom of every screen: a paper bar with four little pixel things (a house, a cassette, a book,
 * a cup). The chosen one sits in a soft patch of the music's colour. In the middle, set into the bar,
 * a record that turns while music plays; tapping it puts something on the shelf.
 */
@Composable
fun Dock(tab: Tab, playing: Boolean, onTab: (Tab) -> Unit, onRecord: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 14.dp, end = 14.dp, bottom = 10.dp)
            .paper(26.dp, 4.dp).clip(RoundedCornerShape(26.dp)).background(C.surface).padding(horizontal = 6.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Slot(Tab.DEN, tab, t("Логово", "Den"), R.drawable.dock_den, onTab)
        Slot(Tab.SHELF, tab, t("Полка", "Shelf"), R.drawable.dock_shelf, onTab)
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Record(playing, onRecord) }
        Slot(Tab.SAGA, tab, t("Сага", "Saga"), R.drawable.dock_saga, onTab)
        Slot(Tab.GOALS, tab, t("Цели", "Goals"), R.drawable.dock_goals, onTab)
    }
}

@Composable
private fun RowScope.Slot(me: Tab, tab: Tab, label: String, icon: Int, onTab: (Tab) -> Unit) {
    val on = me == tab
    val patch by animateColorAsState(if (on) C.amber.copy(alpha = .14f) else Color.Transparent, tween(220), label = "patch")
    val ink by animateColorAsState(if (on) C.amber else C.faint, tween(220), label = "ink")
    val size by animateDpAsState(if (on) 26.dp else 24.dp, tween(220), label = "size")
    Column(
        Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).clickable(remember { MutableInteractionSource() }, null) { onTab(me) }.padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(Modifier.width(52.dp).height(34.dp).clip(RoundedCornerShape(17.dp)).background(patch), contentAlignment = Alignment.Center) {
            Image(ImageBitmap.imageResource(icon), null, Modifier.size(size), filterQuality = FilterQuality.Low, colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(ink))
        }
        Text(label, style = T.Small.copy(color = ink, fontSize = T.Small.fontSize * .95f), maxLines = 1)
    }
}

@Composable
private fun Record(playing: Boolean, onClick: () -> Unit) {
    val spin = rememberInfiniteTransition(label = "spin")
    val turn by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "turn")
    val angle = if (playing && !Art.still) turn else 0f
    val label = t("Добавить на полку", "Add to the shelf")
    Box(Modifier.size(50.dp).clip(CircleShape).clickable(onClick = onClick).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(50.dp).rotate(angle)) {
            val c = center; val r = size.minDimension / 2
            drawCircle(Color(0xFF2B2524), r, c)
            drawCircle(Color.White.copy(alpha = .14f), r - 1f, c, style = Stroke(1.5f))
            for (k in 1..4) drawCircle(Color(0xFF3B3432), r * (0.5f + k * 0.11f), c, style = Stroke(1.2f))
            drawArc(Color.White.copy(alpha = .16f), -70f, 40f, false, Offset(c.x - r * .78f, c.y - r * .78f), Size(r * 1.56f, r * 1.56f), style = Stroke(r * .2f))
            drawCircle(C.amber, r * .42f, c)
        }
        Canvas(Modifier.size(12.dp)) {
            val w = 2.4.dp.toPx()
            drawLine(Color.White, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), w, StrokeCap.Round)
            drawLine(Color.White, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), w, StrokeCap.Round)
        }
    }
}
