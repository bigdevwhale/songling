package app.songling.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import app.songling.ui.LocalRu
import app.songling.ui.art.Buf
import app.songling.ui.art.toBitmap
import app.songling.ui.theme.C
import app.songling.ui.theme.R8
import app.songling.ui.theme.T
import app.songling.ui.theme.paper
import kotlin.math.roundToInt

/** Russian or English, by the phone's (or the app's) language. */
@Composable
fun t(ru: String, en: String): String = if (LocalRu.current) ru else en

@Composable
fun Card(modifier: Modifier = Modifier, fill: Color = C.surface, onClick: (() -> Unit)? = null, padding: PaddingValues = PaddingValues(16.dp), content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.paper().clip(R8.Lg).background(fill)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(padding),
        content = content,
    )
}

enum class Kind { CORAL, AMBER, SOFT, GHOST }

@Composable
fun Btn(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, kind: Kind = Kind.CORAL, enabled: Boolean = true, small: Boolean = false) {
    val (fill, ink) = when (kind) {
        Kind.CORAL -> C.coral to Color.White
        Kind.AMBER -> C.amber to C.onAmber
        Kind.SOFT -> C.surface to C.ink
        Kind.GHOST -> Color.Transparent to C.ink
    }
    val a = if (enabled) 1f else .4f
    Box(
        modifier.then(if (kind != Kind.GHOST && enabled) Modifier.paper(99.dp, if (small) 2.dp else 3.dp) else Modifier).clip(CircleShape).background(fill.copy(alpha = fill.alpha * a))
            .then(if (kind == Kind.GHOST) Modifier.border(1.dp, C.line, CircleShape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = if (small) 36.dp else 48.dp)
            .padding(horizontal = if (small) 14.dp else 20.dp, vertical = if (small) 7.dp else 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = (if (small) T.Button.copy(fontSize = T.Button.fontSize * .9f) else T.Button).copy(color = ink.copy(alpha = a)), textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
fun Chip(text: String, on: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color? = null) {
    val fill = if (on) color ?: C.amber else C.surface
    val ink = when { on && color != null -> Color.White; on -> C.onAmber; else -> C.muted }
    Text(text, style = T.Small.copy(color = ink, fontSize = T.Small.fontSize * 1.1f), maxLines = 1,
        modifier = modifier.paper(99.dp, 2.dp).clip(CircleShape).background(fill).clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 8.dp))
}

/** A genre label in its colour. */
@Composable
fun Tag(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(text, style = T.Small.copy(color = Color.White), modifier = modifier.clip(CircleShape).background(color).padding(horizontal = 10.dp, vertical = 3.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Flow(modifier: Modifier = Modifier, gap: Dp = 6.dp, content: @Composable FlowRowScope.() -> Unit) =
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(gap), verticalArrangement = Arrangement.spacedBy(gap), content = content)

/** A pixel picture with hard pixels, [scale] dp per art pixel. */
@Composable
fun BufImage(buf: Buf, modifier: Modifier = Modifier, scale: Dp = 2.dp, smooth: Boolean = false) {
    val image = remember(buf) { buf.toBitmap().asImageBitmap() }
    Canvas(modifier.size(scale * buf.w, scale * buf.h)) {
        drawImage(image, srcOffset = IntOffset.Zero, srcSize = IntSize(buf.w, buf.h), dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()), filterQuality = if (smooth) FilterQuality.Medium else FilterQuality.None)
    }
}

@Composable
fun Field(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier, style: TextStyle = T.Body, single: Boolean = true, number: Boolean = false, onDone: (() -> Unit)? = null) {
    BasicTextField(value, onChange, modifier.fillMaxWidth().clip(R8.Md).background(C.surface).border(1.5.dp, C.line, R8.Md), textStyle = style, singleLine = single, cursorBrush = SolidColor(C.amber),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = if (number) KeyboardType.Number else KeyboardType.Text,
            imeAction = if (onDone != null) ImeAction.Search else ImeAction.Default),
        keyboardActions = KeyboardActions(onSearch = { onDone?.invoke() }),
        decorationBox = { inner -> Box(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) { if (value.isEmpty()) Text(hint, style = style.copy(color = C.faint)); inner() } })
}

/** A sheet from the bottom over a dim layer; tap outside, swipe down or back closes it. */
@Composable
fun Sheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onDismiss)
    val close = t("Закрыть", "Close")
    Box(Modifier.fillMaxSize().background(Color(0x8A3B2A22)).clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss).imePadding(), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier.fillMaxWidth().widthIn(max = 560.dp).heightIn(max = 780.dp)
                .clip(R8.Xl).background(C.surface)
                .clickable(remember { MutableInteractionSource() }, null) {}
                .pointerInput(Unit) { var dy = 0f; detectVerticalDragGestures(onDragStart = { dy = 0f }, onDragEnd = { if (dy > 120) onDismiss() }) { _, d -> dy += d } }
                .navigationBarsPadding(),
        ) {
            Box(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Box(Modifier.align(Alignment.TopCenter).width(40.dp).height(4.dp).clip(CircleShape).background(C.line))
                Box(Modifier.align(Alignment.TopEnd).padding(end = 8.dp).size(40.dp).clip(CircleShape).clickable(onClick = onDismiss).semantics { contentDescription = close }, contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(14.dp)) {
                        val w = 2.dp.toPx()
                        drawLine(C.muted, Offset.Zero, Offset(size.width, size.height), w, StrokeCap.Round)
                        drawLine(C.muted, Offset(size.width, 0f), Offset(0f, size.height), w, StrokeCap.Round)
                    }
                }
            }
            Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SheetMark()
                content()
            }
        }
    }
}

/** A full-screen page over the tabs with a back arrow. */
@Composable
fun Page(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onBack)
    Box(Modifier.fillMaxSize()) {
        app.songling.ui.BackdropLayer()
        Column(Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null) {}.imePadding()) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(18.dp)) {
                        val w = 2.2.dp.toPx(); val m = size.height / 2
                        drawLine(C.ink, Offset(size.width * .65f, 0f), Offset(size.width * .2f, m), w, StrokeCap.Round)
                        drawLine(C.ink, Offset(size.width * .2f, m), Offset(size.width * .65f, size.height), w, StrokeCap.Round)
                    }
                }
                Spacer(Modifier.width(4.dp))
                Text(title, style = T.Title, modifier = Modifier.weight(1f), maxLines = 1)
            }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 8.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
        }
    }
}

/** A row of choices in one pill; the chosen one sits raised on paper. */
@Composable
fun Segmented(options: List<String>, selected: Int, onPick: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(CircleShape).background(C.surface2).padding(4.dp)) {
        options.forEachIndexed { i, label ->
            val sel = i == selected
            Text(label, style = T.Small.copy(color = if (sel) C.ink else C.muted, fontSize = T.Small.fontSize * 1.05f), maxLines = 1, textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).then(if (sel) Modifier.paper(99.dp, 2.dp) else Modifier).clip(CircleShape).background(if (sel) C.surface else C.surface2)
                    .clickable { onPick(i) }.padding(vertical = 9.dp))
        }
    }
}

@Composable
fun Gap(h: Dp = 8.dp) = Spacer(Modifier.height(h))

/** A section header: small ticket capitals, an optional note on the right. */
@Composable
fun Section(text: String, note: String? = null, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp), verticalAlignment = Alignment.Bottom) {
        Text(text.uppercase(), style = T.Eyebrow.copy(color = C.amber), modifier = Modifier.weight(1f))
        if (note != null) Text(note, style = T.Small)
    }
}

/** A thin progress bar. */
@Composable
fun Bar(p: Float, modifier: Modifier = Modifier, color: Color = C.amber) {
    // Knitted: a stripe of the colour and a lighter one, on the slant.
    val light = Color(color.red + (1 - color.red) * .25f, color.green + (1 - color.green) * .25f, color.blue + (1 - color.blue) * .25f)
    Box(modifier.height(10.dp).clip(CircleShape).background(C.surface2)) {
        Canvas(Modifier.fillMaxWidth(p.coerceIn(0f, 1f)).height(10.dp).clip(CircleShape)) {
            drawRect(color)
            val step = 7.dp.toPx(); var x = -size.height
            while (x < size.width) {
                val path = androidx.compose.ui.graphics.Path().apply { moveTo(x, size.height); lineTo(x + size.height, 0f); lineTo(x + size.height + step / 2, 0f); lineTo(x + step / 2, size.height); close() }
                drawPath(path, light); x += step
            }
        }
    }
}

/**
 * Pages of one screen, a tap away at the top: plain words, the open one gold and underlined, so
 * they never look like the filter chips below. [counts] go beside the names when given.
 */
@Composable
fun Tabs(names: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, counts: List<Int?> = emptyList()) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        names.forEachIndexed { i, name ->
            val on = i == selected
            Column(Modifier.weight(1f).clip(R8.Sm).clickable { onSelect(i) }.padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                // One line, whatever the width: the count is the first thing to give way.
                val count = counts.getOrNull(i)
                Text(androidx.compose.ui.text.buildAnnotatedString {
                    append(name)
                    if (count != null) withStyle(androidx.compose.ui.text.SpanStyle(fontSize = T.Small.fontSize, color = if (on) C.amber.copy(alpha = .8f) else C.faint)) { append(" $count") }
                }, style = T.Strong.copy(color = if (on) C.amber else C.muted), maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(2.dp).clip(CircleShape).background(if (on) C.amber else C.line))
            }
        }
    }
}

/** A small square way into another part of the cinema: what it is and how it stands. */
@Composable
fun Tile(title: String, note: String, modifier: Modifier = Modifier, accent: Color = C.amber, onClick: () -> Unit) {
    Column(modifier.paper().clip(R8.Lg).background(C.surface).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = T.Strong, maxLines = 1)
        Text(note, style = T.Small.copy(color = accent), maxLines = 1)
    }
}

/**
 * A number with «−» and «+»: a tap moves it by one, and held down it keeps going, faster the longer it's
 * held (#6). [onStep] gets −1 or +1.
 */
@Composable
fun Stepper(label: String, modifier: Modifier = Modifier, onStep: (Int) -> Unit) {
    val step = androidx.compose.runtime.rememberUpdatedState(onStep)
    @Composable
    fun Key(text: String, d: Int) {
        Box(
            Modifier.clip(CircleShape).background(C.surface2).heightIn(min = 36.dp).widthIn(min = 48.dp)
                .pointerInput(d) {
                    awaitEachGesture {
                        awaitFirstDown()
                        step.value(d)
                        val held = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                            kotlinx.coroutines.delay(420)
                            var wait = 140L
                            while (true) { step.value(d); kotlinx.coroutines.delay(wait); wait = (wait * 0.88).toLong().coerceAtLeast(35) }
                        }
                        waitForUpOrCancellation()
                        held.cancel()
                    }
                }
                .padding(horizontal = 14.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center,
        ) { Text(text, style = T.Button.copy(fontSize = T.Button.fontSize * .9f, color = C.ink)) }
    }
    Row(modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Key("−", -1)
        Text(label, style = T.Strong, modifier = Modifier.weight(1f))
        Key("+", 1)
    }
}
