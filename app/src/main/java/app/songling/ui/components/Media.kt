package app.songling.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.songling.domain.Genre
import app.songling.ui.theme.C
import app.songling.ui.theme.Caveat
import app.songling.ui.theme.Mono
import app.songling.ui.theme.T
import app.songling.ui.theme.paper

/** How music looks everywhere: records in sleeves, or cassettes with J-cards. */
enum class Medium { VINYL, TAPE }

val LocalMedium = staticCompositionLocalOf { Medium.VINYL }

/**
 * An album as a thing: a sleeve with the record peeking out (vinyl), or a cassette J-card with the
 * cover above and the title handwritten on the ruled label below (tape). [size] is the height.
 */
@Composable
fun Sleeve(cover: String?, title: String, artist: String, genre: Genre?, size: Dp, modifier: Modifier = Modifier, slide: Boolean = false) {
    when (LocalMedium.current) {
        Medium.VINYL -> {
            val out = remember { Animatable(if (slide) 0f else 1f) }
            LaunchedEffect(slide) { if (slide) out.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
            Box(modifier.width(size * 1.2f).height(size)) {
                // The record, half out of the sleeve.
                Canvas(Modifier.size(size * .94f).align(Alignment.CenterStart).offset(x = size * (0.03f + 0.24f * out.value))) {
                    val r = this.size.minDimension / 2; val c = center
                    drawCircle(C.vinyl, r, c)
                    for (k in 1..7) drawCircle(Color.White.copy(alpha = .05f), r * (0.42f + k * 0.075f), c, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
                    drawArc(Color.White.copy(alpha = .12f), -60f, 35f, false, Offset(c.x - r * .8f, c.y - r * .8f), androidx.compose.ui.geometry.Size(r * 1.6f, r * 1.6f), style = androidx.compose.ui.graphics.drawscope.Stroke(r * .14f))
                    drawCircle(C.amber, r * .3f, c)
                    drawCircle(C.vinyl, r * .04f, c)
                }
                Box(Modifier.size(size).paper(6.dp, 3.dp)) { Cover(cover, title, genre, size, round = 4.dp) }
            }
        }
        Medium.TAPE -> {
            val w = size * .72f
            Column(modifier.width(w).height(size).paper(5.dp, 3.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFFF6EEDF))) {
                Box(Modifier.fillMaxWidth().height(size * .6f)) { Cover(cover, title, genre, size, Modifier.fillMaxSize(), round = 0.dp) }
                // The ruled label, written by hand.
                Column(Modifier.fillMaxWidth().weight(1f).drawBehind {
                    val step = this.size.height / 3
                    for (k in 1..2) drawLine(Color(0xFFB9C7D6), Offset(0f, step * k), Offset(this.size.width, step * k), 1.2f)
                    drawLine(Color(0xFFE2A0A0), Offset(this.size.width * .1f, 0f), Offset(this.size.width * .1f, this.size.height), 1f)
                }.padding(start = w * .14f, end = 4.dp), verticalArrangement = Arrangement.Center) {
                    val big = (size.value / 7.5f).coerceIn(9f, 26f)
                    Text(title, fontFamily = Caveat, fontSize = big.sp, lineHeight = (big * 1.05f).sp, color = Color(0xFF2B3A6A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (size > 70.dp) Text(artist, fontFamily = Caveat, fontSize = (big * .8f).sp, lineHeight = (big * .85f).sp, color = Color(0xFF5A6A8A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** «СТОРОНА А · 21:50»: a typewritten heading for half an album. */
@Composable
fun SideHeader(text: String, note: String? = null, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp), verticalAlignment = Alignment.Bottom) {
        Text(text.uppercase(), fontFamily = Mono, fontSize = 12.sp, letterSpacing = 2.sp, color = C.faint, modifier = Modifier.weight(1f))
        if (note != null) Text(note, fontFamily = Mono, fontSize = 12.sp, color = C.faint)
    }
}

/**
 * A field in a form, written onto the thing: printed credits on a sleeve (a typewritten label and a
 * line), or handwriting on the ruled cassette label.
 */
@Composable
fun FormField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    val tape = LocalMedium.current == Medium.TAPE
    Column(modifier.fillMaxWidth()) {
        Text(label.uppercase(), fontFamily = Mono, fontSize = 11.sp, letterSpacing = 1.5.sp, color = C.faint)
        val style = if (tape) T.Body.copy(fontFamily = Caveat, fontSize = 24.sp, lineHeight = 28.sp, color = if (app.songling.ui.theme.Mode.dark) Color(0xFFB8C8FF) else Color(0xFF2B3A6A))
            else T.Body.copy(fontSize = 17.sp)
        BasicTextField(value, onChange, Modifier.fillMaxWidth().padding(top = 2.dp).drawBehind {
            drawLine(if (tape) Color(0xFFB9C7D6) else C.faint.copy(alpha = .6f), Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), if (tape) 1.5f else 2f)
        }.padding(vertical = 6.dp), textStyle = style, singleLine = true, cursorBrush = SolidColor(C.amber))
    }
}

/** The top of a sheet: grooves for vinyl, the J-card's stripes for tape. */
@Composable
fun SheetMark() {
    when (LocalMedium.current) {
        Medium.VINYL -> Canvas(Modifier.fillMaxWidth().height(10.dp).clipToBounds()) {
            val r = size.width * .9f
            for (k in 0 until 4) drawCircle(C.faint.copy(alpha = .18f), r + k * 6f, Offset(size.width / 2, r + 2f + k * 6f - size.height * 0f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.2f))
        }
        Medium.TAPE -> Row(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))) {
            for (c in listOf(C.amber, C.honey, C.lilac)) Box(Modifier.weight(1f).height(8.dp).background(c))
        }
    }
}
