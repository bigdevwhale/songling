package app.songling.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.songling.data.Covers
import app.songling.domain.Genre
import app.songling.ui.art.Buf
import app.songling.ui.art.Look
import app.songling.ui.art.PetArt
import app.songling.ui.art.PixelCanvas
import app.songling.ui.art.hashI
import app.songling.ui.theme.C
import app.songling.ui.theme.R8
import app.songling.ui.theme.T
import app.songling.ui.theme.paper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Covers in lists as pixels instead of the picture itself. */
val LocalPixelCovers = androidx.compose.runtime.staticCompositionLocalOf { false }

/**
 * An album cover: the picture when there is one (or its pixels, by the setting), else a drawn sleeve
 * in the genre's colour with the title's first letter.
 */
@Composable
fun Cover(cover: String?, title: String, genre: Genre?, size: Dp, modifier: Modifier = Modifier, round: Dp = 8.dp) {
    val ctx = LocalContext.current
    val pixel = LocalPixelCovers.current
    val img by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, cover, pixel) {
        value = if (cover == null) null else withContext(Dispatchers.IO) {
            if (pixel) Covers.pixels(ctx, cover, 24)?.let { b -> android.graphics.Bitmap.createBitmap(b.d, b.w, b.h, android.graphics.Bitmap.Config.ARGB_8888).asImageBitmap() }
            else Covers.bitmap(ctx, cover, 300)?.asImageBitmap()
        }
    }
    val shape = RoundedCornerShape(round)
    Box(modifier.size(size).clip(shape).background(C.surface2), contentAlignment = Alignment.Center) {
        val i = img
        if (i != null) Image(i, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, filterQuality = if (pixel) FilterQuality.None else FilterQuality.Medium)
        else {
            val base = genre?.let { C.genre(it) } ?: Color(0xFF000000 or (0x404040L + (hashI(title) % 0x606060)))
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Brush.linearGradient(listOf(base, base.copy(alpha = .55f).compositeOver(C.bg))))
                // A record peeking out.
                drawCircle(Color(0xFF16141C), this.size.minDimension * .34f, Offset(this.size.width * .62f, this.size.height * .5f))
                drawCircle(base.copy(alpha = .9f), this.size.minDimension * .1f, Offset(this.size.width * .62f, this.size.height * .5f))
            }
            Text(title.take(1).uppercase(), style = T.Name.copy(color = Color.White.copy(alpha = .9f)), modifier = Modifier.align(Alignment.TopStart).padding(6.dp))
        }
    }
}

private fun Color.compositeOver(bg: Color) = Color(red * alpha + bg.red * (1 - alpha), green * alpha + bg.green * (1 - alpha), blue * alpha + bg.blue * (1 - alpha))

/** Five stars; tappable with [onRate], and tapping the current one clears it. */
@Composable
fun Stars(rating: Int, modifier: Modifier = Modifier, size: Dp = 20.dp, onRate: ((Int) -> Unit)? = null) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(size / 6)) {
        for (i in 1..5) {
            val on = i <= rating
            Canvas(Modifier.size(size).then(if (onRate != null) Modifier.clickable { onRate(if (i == rating) 0 else i) } else Modifier)) {
                val p = Path()
                val cx = this.size.width / 2; val cy = this.size.height / 2; val r = this.size.minDimension / 2
                for (k in 0 until 10) {
                    val a = Math.PI / 5 * k - Math.PI / 2
                    val rr = if (k % 2 == 0) r else r * .45f
                    val x = cx + (Math.cos(a) * rr).toFloat(); val y = cy + (Math.sin(a) * rr).toFloat()
                    if (k == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                p.close()
                drawPath(p, if (on) C.honey else C.track)
            }
        }
    }
}

/** The pet says something: a paper bubble with its name. */
@Composable
fun Says(text: String, who: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().paper(20.dp).clip(RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)).background(C.surface).padding(horizontal = 16.dp, vertical = 11.dp)) {
        Text(who.uppercase(), style = T.Eyebrow.copy(color = C.coral))
        Text(text, style = T.Say)
    }
}

/** A pet, alive: blinking, dancing to what plays; [gentle] sways it slowly and softly instead. */
@Composable
fun PetView(look: Look, modifier: Modifier = Modifier, gentle: Boolean = false) {
    val buf = remember { Buf(PetArt.W, PetArt.H, Buf.FINE) }
    val time = rememberSeconds()
    // The pixels stay still; the whole picture bobs, sways and breathes smoothly, every frame.
    Box(modifier.aspectRatio(PetArt.W / PetArt.H.toFloat()).graphicsLayer {
        val p = if (gentle) app.songling.ui.art.Motion.gentle(time.floatValue, look.species.ordinal * 0.9f) else app.songling.ui.art.Motion.at(look, time.floatValue)
        translationY = p.dy * size.height
        rotationZ = p.rot
        scaleX = p.sx; scaleY = p.sy
        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, (PetArt.H - 5f) / PetArt.H)
    }) {
        PixelCanvas(PetArt.W, PetArt.H, Modifier.fillMaxSize(), fine = Buf.FINE) { b, tick -> PetArt.frame(look, tick, buf, motion = false); b.copyFrom(buf) }
    }
}

/** Seconds since this appeared, ticking every frame (still at 0 for renders). */
@Composable
fun rememberSeconds(): androidx.compose.runtime.MutableFloatState {
    val t = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    if (!app.songling.ui.art.Art.still) LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { now -> t.floatValue = (now - start) / 1e9f }
    }
    return t
}

/** The den with the pet laid over it, so the pet moves smoothly while the room stays pixel-still. */
@Composable
fun DenView(den: app.songling.ui.art.DenLook, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier.fillMaxWidth()) {
        val k = maxWidth / app.songling.ui.art.DenArt.W
        PixelCanvas(app.songling.ui.art.DenArt.W, app.songling.ui.art.DenArt.H, Modifier.fillMaxWidth(), fine = Buf.FINE) { b, tick -> app.songling.ui.art.DenArt.draw(b, den, tick) }
        // Posters as they are, over their pixel frames, unless covers are meant to be pixels.
        if (!LocalPixelCovers.current && app.songling.domain.Item.POSTER in den.items) {
            val ctx = LocalContext.current
            den.posterFiles.forEachIndexed { i, file ->
                val r = app.songling.ui.art.DenArt.POSTER_RECTS.getOrNull(i) ?: return@forEachIndexed
                val img by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, file) {
                    value = file?.let { f -> withContext(Dispatchers.IO) { Covers.bitmap(ctx, f, 240)?.asImageBitmap() } }
                }
                img?.let { Image(it, null, Modifier.offset(x = k * r[0], y = k * r[1]).size(k * r[2], k * r[3]), contentScale = ContentScale.Crop, filterQuality = FilterQuality.Medium) }
            }
        }
        den.pet?.let { look ->
            PetView(look, Modifier.offset(x = k * app.songling.ui.art.DenArt.PET_X, y = k * app.songling.ui.art.DenArt.PET_Y).width(k * PetArt.W))
        }
    }
}

/** A big number with a label under it. */
@Composable
fun Stat(value: String, label: String, modifier: Modifier = Modifier, color: Color = C.amber) {
    Column(modifier.lift(LocalOnSheet.current, 16.dp).clip(R8.Md).background(raised()).padding(horizontal = 12.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        // A long one («8 ч 11 мин» in a third of the width) shrinks to fit instead of being cut.
        androidx.compose.foundation.text.BasicText(value, style = T.Number.copy(color = color), maxLines = 1, softWrap = false,
            autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = 18.sp, stepSize = 0.5.sp))
        Text(label, style = T.Small.copy(fontSize = T.Small.fontSize * .95f, lineHeight = T.Small.lineHeight * .95f), maxLines = 2, softWrap = true)
    }
}

/**
 * Bars over labels: minutes per day, hour or month. [labels] can skip some to stay readable. With [describe], a tap
 * on a bar says what it stands for (#14) and the others step back; a second tap puts it away.
 */
@Composable
fun Bars(values: List<Long>, labels: List<String>, modifier: Modifier = Modifier, color: Color = C.amber, highlight: Int = -1, every: Int = 1, wide: Boolean = false, describe: ((Int) -> String)? = null) {
    val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    var picked by remember(values) { mutableIntStateOf(-1) }
    Column(modifier.fillMaxWidth()) {
        if (describe != null) Text(if (picked in values.indices) describe(picked) else t("Нажми на столбик", "Tap a bar"),
            style = T.Small.copy(color = if (picked in values.indices) C.ink else C.faint), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(bottom = 4.dp))
        val tap = if (describe == null) Modifier else Modifier.pointerInput(values) {
            detectTapGestures { o -> val i = (o.x / (size.width / values.size.coerceAtLeast(1).toFloat())).toInt().coerceIn(0, values.size - 1); picked = if (picked == i) -1 else i }
        }
        Canvas(Modifier.fillMaxWidth().height(110.dp).then(tap)) {
            val n = values.size.coerceAtLeast(1)
            val w = size.width / n
            values.forEachIndexed { i, v ->
                val h = (v.toFloat() / max) * (size.height - 4)
                val c = if (i == highlight) C.coral else color
                val dim = picked >= 0 && i != picked
                drawRoundRect((if (v == 0L) C.track else c).let { if (dim) it.copy(alpha = it.alpha * .35f) else it },
                    Offset(i * w + w * .15f, size.height - h.coerceAtLeast(2f)), Size(w * .7f, h.coerceAtLeast(2f)), CornerRadius(w * .2f))
                // The picked one gets a dot on top, so a short bar is still found.
                if (i == picked) drawCircle(C.ink, (w * .22f).coerceIn(2.5f, 6f), Offset(i * w + w / 2, size.height - h.coerceAtLeast(2f) - 7f))
            }
        }
        // Few long labels (dates): spread across the width, left, middle, right.
        if (wide) Row(Modifier.fillMaxWidth()) {
            val shown = labels.filter { it.isNotEmpty() }
            shown.forEachIndexed { i, l -> Text(l, style = T.Small.copy(fontSize = T.Small.fontSize * .9f), maxLines = 1, modifier = Modifier.weight(1f),
                textAlign = when (i) { 0 -> TextAlign.Start; shown.size - 1 -> TextAlign.End; else -> TextAlign.Center }) }
        } else Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { i, l ->
                Text(if (i % every == 0) l else "", style = T.Small.copy(fontSize = T.Small.fontSize * .85f), textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** One line of a top list: rank, cover or dot, name, sub and a bar for its share. */
@Composable
fun TopRow(rank: Int, name: String, sub: String, value: String, share: Float, modifier: Modifier = Modifier, cover: String? = null, genre: Genre? = null, showCover: Boolean = false, onClick: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().clip(R8.Sm).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 7.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("$rank", style = T.Small.copy(color = if (rank <= 3) C.amber else C.faint), modifier = Modifier.size(width = 20.dp, height = 18.dp), textAlign = TextAlign.End)
        if (showCover) Sleeve(cover, name, sub, genre, 44.dp)
        Column(Modifier.weight(1f)) {
            Text(name, style = T.Strong, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (sub.isNotEmpty()) Text(sub, style = T.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box(Modifier.fillMaxWidth().padding(top = 5.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(C.track)) {
                Box(Modifier.fillMaxWidth(share.coerceIn(0.02f, 1f)).height(5.dp).clip(RoundedCornerShape(3.dp)).background(genre?.let { C.genre(it) } ?: C.amber))
            }
        }
        Text(value, style = T.Small.copy(color = C.muted))
    }
}
