package app.songling.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.songling.ui.LocalRu
import app.songling.ui.art.Art
import app.songling.ui.art.Buf
import app.songling.ui.art.toBitmap
import app.songling.ui.theme.C
import app.songling.ui.theme.R8
import app.songling.ui.theme.T
import app.songling.ui.theme.paper
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Russian or English, by the phone's (or the app's) language. */
@Composable
fun t(ru: String, en: String): String = if (LocalRu.current) ru else en

/**
 * True inside a [Sheet]. A sheet is paper itself, so what would be a paper card on the page turns into a
 * flat well there: a card the colour of the sheet would only show by its shadow.
 */
val LocalOnSheet = compositionLocalOf { false }

/** The fill of a card, or of a chip that's off: paper on the page, a well on a sheet. */
@Composable
fun raised(): Color = if (LocalOnSheet.current) C.surface2 else C.surface

/** The paper shadow, where there's a page under the thing to cast it on. */
fun Modifier.lift(onSheet: Boolean, radius: Dp = 22.dp, depth: Dp = 3.dp): Modifier = if (onSheet) this else paper(radius, depth)

/** A soft press: the thing sinks a little under the finger. Goes first in the chain, so the shadow sinks too. */
fun Modifier.pressable(source: MutableInteractionSource, enabled: Boolean = true): Modifier = if (!enabled) this else composed {
    val pressed by source.collectIsPressedAsState()
    val k by animateFloatAsState(if (pressed) .97f else 1f, tween(110), label = "press")
    graphicsLayer { scaleX = k; scaleY = k }
}

@Composable
fun Card(modifier: Modifier = Modifier, fill: Color? = null, onClick: (() -> Unit)? = null, padding: PaddingValues = PaddingValues(16.dp), content: @Composable ColumnScope.() -> Unit) {
    val src = remember { MutableInteractionSource() }
    Column(
        modifier.fillMaxWidth().pressable(src, onClick != null).lift(LocalOnSheet.current).clip(R8.Lg).background(fill ?: raised())
            .then(if (onClick != null) Modifier.clickable(src, LocalIndication.current, onClick = onClick) else Modifier).padding(padding),
    ) {
        // What lies on a card lies on paper, as on a sheet: chips and tiles in it are wells.
        CompositionLocalProvider(LocalOnSheet provides true) { content() }
    }
}

enum class Kind { CORAL, AMBER, SOFT, GHOST }

@Composable
fun Btn(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, kind: Kind = Kind.CORAL, enabled: Boolean = true, small: Boolean = false) {
    val (fill, ink) = when (kind) {
        Kind.CORAL -> C.coral to Color.White
        Kind.AMBER -> C.amber to C.onAmber
        Kind.SOFT -> raised() to C.ink
        Kind.GHOST -> Color.Transparent to C.ink
    }
    val a = if (enabled) 1f else .4f
    val src = remember { MutableInteractionSource() }
    val flat = kind == Kind.GHOST || !enabled || (kind == Kind.SOFT && LocalOnSheet.current)
    Box(
        modifier.pressable(src, enabled).then(if (flat) Modifier else Modifier.paper(99.dp, if (small) 2.dp else 3.dp)).clip(CircleShape).background(fill.copy(alpha = fill.alpha * a))
            .then(if (kind == Kind.GHOST) Modifier.border(1.5.dp, C.faint.copy(alpha = .38f * a), CircleShape) else Modifier)
            .clickable(src, LocalIndication.current, enabled = enabled, onClick = onClick)
            .heightIn(min = if (small) 38.dp else 50.dp)
            .padding(horizontal = if (small) 16.dp else 22.dp, vertical = if (small) 8.dp else 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = (if (small) T.Button.copy(fontSize = T.Button.fontSize * .9f) else T.Button).copy(color = ink.copy(alpha = a)), textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun Chip(text: String, on: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color? = null) {
    val fill by animateColorAsState(if (on) color ?: C.amber else raised(), tween(160), label = "chip")
    val ink by animateColorAsState(when { on && color != null -> Color.White; on -> C.onAmber; else -> C.muted }, tween(160), label = "chipInk")
    val src = remember { MutableInteractionSource() }
    Text(text, style = T.Small.copy(color = ink, fontSize = T.Small.fontSize * 1.1f), maxLines = 1,
        modifier = modifier.pressable(src).lift(LocalOnSheet.current, 99.dp, 2.dp).clip(CircleShape).background(fill)
            .clickable(src, LocalIndication.current, onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp))
}

/** A genre label in its colour. */
@Composable
fun Tag(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(text, style = T.Small.copy(color = Color.White), modifier = modifier.clip(CircleShape).background(color).padding(horizontal = 10.dp, vertical = 3.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Flow(modifier: Modifier = Modifier, gap: Dp = 8.dp, content: @Composable FlowRowScope.() -> Unit) =
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(gap), verticalArrangement = Arrangement.spacedBy(gap), content = content)

/** A pixel picture with hard pixels, [scale] dp per art pixel. */
@Composable
fun BufImage(buf: Buf, modifier: Modifier = Modifier, scale: Dp = 2.dp, smooth: Boolean = false) {
    val image = remember(buf) { buf.toBitmap().asImageBitmap() }
    Canvas(modifier.size(scale * buf.w, scale * buf.h)) {
        drawImage(image, srcOffset = IntOffset.Zero, srcSize = IntSize(buf.fw, buf.fh), dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()), filterQuality = if (smooth) FilterQuality.Medium else FilterQuality.None)
    }
}

// ---------------------------------------------------------------- signs

/** The app's own small signs: a few round-capped strokes each, in one colour, so they sit with any text. */
enum class Glyph { BACK, NEXT, DOWN, UP, CLOSE, PLUS, CHECK, SEARCH, SORT, FILTER }

@Composable
fun Sign(g: Glyph, modifier: Modifier = Modifier, color: Color = C.muted, size: Dp = 16.dp, stroke: Dp = 2.dp) {
    Canvas(modifier.size(size)) {
        val w = stroke.toPx(); val s = this.size.minDimension
        fun line(x0: Float, y0: Float, x1: Float, y1: Float) = drawLine(color, Offset(x0 * s, y0 * s), Offset(x1 * s, y1 * s), w, StrokeCap.Round)
        when (g) {
            Glyph.BACK -> { line(.64f, .12f, .26f, .5f); line(.26f, .5f, .64f, .88f) }
            Glyph.NEXT -> { line(.36f, .12f, .74f, .5f); line(.74f, .5f, .36f, .88f) }
            Glyph.DOWN -> { line(.12f, .34f, .5f, .72f); line(.5f, .72f, .88f, .34f) }
            Glyph.UP -> { line(.12f, .66f, .5f, .28f); line(.5f, .28f, .88f, .66f) }
            Glyph.CLOSE -> { line(.14f, .14f, .86f, .86f); line(.86f, .14f, .14f, .86f) }
            Glyph.PLUS -> { line(.5f, .1f, .5f, .9f); line(.1f, .5f, .9f, .5f) }
            Glyph.CHECK -> { line(.1f, .54f, .38f, .82f); line(.38f, .82f, .9f, .22f) }
            Glyph.SEARCH -> { drawCircle(color, s * .3f, Offset(s * .42f, s * .42f), style = Stroke(w)); line(.66f, .66f, .9f, .9f) }
            // Lines growing shorter: an order.
            Glyph.SORT -> { line(.08f, .22f, .92f, .22f); line(.08f, .5f, .66f, .5f); line(.08f, .78f, .4f, .78f) }
            // Two sliders.
            Glyph.FILTER -> {
                line(.08f, .28f, .92f, .28f); line(.08f, .72f, .92f, .72f)
                drawCircle(color, s * .13f, Offset(s * .34f, s * .28f)); drawCircle(color, s * .13f, Offset(s * .66f, s * .72f))
            }
        }
    }
}

/** A round button that is only a sign: back, close. Big enough for a thumb whatever the sign's size. */
@Composable
fun SignButton(g: Glyph, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = C.ink, size: Dp = 18.dp, stroke: Dp = 2.2.dp) {
    Box(modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Sign(g, color = color, size = size, stroke = stroke)
    }
}

// ---------------------------------------------------------------- fields

/** A text field on paper; the edge takes the music's colour while it's being written in. [search] adds a glass and a cross to clear it. */
@Composable
fun Field(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier, style: TextStyle = T.Body, single: Boolean = true, number: Boolean = false, onDone: (() -> Unit)? = null, search: Boolean = false) {
    var focused by remember { mutableStateOf(false) }
    val edge by animateColorAsState(if (focused) C.amber else C.line, tween(160), label = "edge")
    val clear = t("Очистить", "Clear")
    BasicTextField(value, onChange, modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }.clip(R8.Md).background(C.surface).border(1.5.dp, edge, R8.Md), textStyle = style, singleLine = single, cursorBrush = SolidColor(C.amber),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = if (number) KeyboardType.Number else KeyboardType.Text,
            imeAction = if (onDone != null) ImeAction.Search else ImeAction.Default),
        keyboardActions = KeyboardActions(onSearch = { onDone?.invoke() }),
        decorationBox = { inner ->
            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = if (single) Alignment.CenterVertically else Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (search) Sign(Glyph.SEARCH, color = if (focused) C.amber else C.faint)
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(hint, style = style.copy(color = C.faint), maxLines = if (single) 1 else Int.MAX_VALUE, overflow = TextOverflow.Ellipsis)
                    inner()
                }
                if (search && value.isNotEmpty()) Box(Modifier.size(22.dp).clip(CircleShape).background(C.surface2).clickable { onChange("") }.semantics { contentDescription = clear }, contentAlignment = Alignment.Center) {
                    Sign(Glyph.CLOSE, color = C.muted, size = 8.dp, stroke = 1.7.dp)
                }
            }
        })
}

// ---------------------------------------------------------------- sheets and pages

/** A sheet that slides up from the bottom over a dim layer; a tap outside, a pull down or back sends it away. */
@Composable
fun Sheet(onDismiss: () -> Unit, footer: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val scope = rememberCoroutineScope()
    val latest by rememberUpdatedState(onDismiss)
    // 0 below the screen .. 1 in place.
    val shown = remember { Animatable(if (Art.still) 1f else 0f) }
    var pulled by remember { mutableFloatStateOf(0f) }
    var leaving by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown.animateTo(1f, tween(260, easing = FastOutSlowInEasing)) }
    fun leave() {
        if (leaving) return
        leaving = true
        scope.launch { shown.animateTo(0f, tween(170)); latest() }
    }
    BackHandler(onBack = ::leave)
    val close = t("Закрыть", "Close")
    val top = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    // The dim layer is drawn, not composed: reading the animation here would recompose the whole sheet every frame.
    Box(Modifier.fillMaxSize().drawBehind { drawRect(Color(0xFF0C0C10), alpha = .5f * shown.value) }.clickable(remember { MutableInteractionSource() }, null, onClick = ::leave)
        .statusBarsPadding().padding(top = 20.dp).imePadding(), contentAlignment = Alignment.BottomCenter) {
        CompositionLocalProvider(LocalOnSheet provides true) {
            Column(
                Modifier.fillMaxWidth().widthIn(max = 560.dp).heightIn(max = 780.dp)
                    .graphicsLayer { translationY = (1f - shown.value) * size.height + pulled }
                    .clip(top).background(C.surface)
                    .clickable(remember { MutableInteractionSource() }, null) {}
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (pulled > 96.dp.toPx()) leave()
                                else scope.launch { androidx.compose.animation.core.animate(pulled, 0f, animationSpec = tween(160)) { v, _ -> pulled = v } }
                            },
                            onDragCancel = { pulled = 0f },
                        ) { _, d -> pulled = (pulled + d).coerceAtLeast(0f) }
                    }
                    .navigationBarsPadding(),
            ) {
                Box(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    Box(Modifier.align(Alignment.TopCenter).width(44.dp).height(5.dp).clip(CircleShape).background(C.track))
                    SignButton(Glyph.CLOSE, close, ::leave, Modifier.align(Alignment.TopEnd).padding(end = 6.dp), color = C.muted, size = 14.dp, stroke = 2.dp)
                }
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 0.dp, bottom = if (footer == null) 28.dp else 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
                // What must stay in reach (a «Save») sits under the scroll, with a thin rule over it.
                if (footer != null) {
                    Rule()
                    Box(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp)) { footer() }
                }
            }
        }
    }
}

/** A full-screen page over the tabs with a back arrow; it comes in from the side. */
@Composable
fun Page(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onBack)
    val enter = remember { Animatable(if (Art.still) 1f else 0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(220, easing = FastOutSlowInEasing)) }
    val back = t("Назад", "Back")
    Box(Modifier.fillMaxSize().graphicsLayer { alpha = enter.value; translationX = (1f - enter.value) * 28.dp.toPx() }) {
        app.songling.ui.BackdropLayer()
        Column(Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null) {}.imePadding()) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                SignButton(Glyph.BACK, back, onBack)
                Spacer(Modifier.width(2.dp))
                Text(title, style = T.Title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
        }
    }
}

// ---------------------------------------------------------------- choices

/** A row of choices in one pill; the chosen one sits raised on paper. */
@Composable
fun Segmented(options: List<String>, selected: Int, onPick: (Int) -> Unit, modifier: Modifier = Modifier) {
    // On a sheet the groove is darker than the wells around it, and the chosen one is the sheet's own paper.
    val groove = if (LocalOnSheet.current) C.track else C.surface2
    Row(modifier.fillMaxWidth().clip(CircleShape).background(groove).padding(4.dp)) {
        options.forEachIndexed { i, label ->
            val sel = i == selected
            val fill by animateColorAsState(if (sel) C.surface else groove, tween(160), label = "seg")
            val ink by animateColorAsState(if (sel) C.ink else C.muted, tween(160), label = "segInk")
            Text(label, style = T.Small.copy(color = ink, fontSize = T.Small.fontSize * 1.05f), maxLines = 1, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).then(if (sel) Modifier.paper(99.dp, 2.dp) else Modifier).clip(CircleShape).background(fill)
                    .clickable { onPick(i) }.padding(vertical = 10.dp, horizontal = 4.dp))
        }
    }
}

@Composable
fun Gap(h: Dp = 8.dp) = Spacer(Modifier.height(h))

/** A section header: small ticket capitals, an optional note on the right. */
@Composable
fun Section(text: String, note: String? = null, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp, start = 2.dp, end = 2.dp), verticalAlignment = Alignment.Bottom) {
        Text(text.uppercase(), style = T.Eyebrow.copy(color = C.amber), modifier = Modifier.weight(1f))
        if (note != null) Text(note, style = T.Small)
    }
}

/** A thin progress bar. */
@Composable
fun Bar(p: Float, modifier: Modifier = Modifier, color: Color = C.amber) {
    // Knitted: a stripe of the colour and a lighter one, on the slant.
    val light = Color(color.red + (1 - color.red) * .25f, color.green + (1 - color.green) * .25f, color.blue + (1 - color.blue) * .25f)
    val part by animateFloatAsState(p.coerceIn(0f, 1f), tween(500, easing = FastOutSlowInEasing), label = "bar")
    Box(modifier.height(10.dp).clip(CircleShape).background(C.track)) {
        Canvas(Modifier.fillMaxWidth(part).height(10.dp).clip(CircleShape)) {
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
 * Pages of one screen, a tap away at the top: plain words over one thin rule, the open one in the music's
 * colour with a short bar standing on the rule, so they never look like the filter chips below.
 * [counts] go beside the names when given.
 */
@Composable
fun Tabs(names: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, counts: List<Int?> = emptyList()) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            names.forEachIndexed { i, name ->
                val on = i == selected
                val ink by animateColorAsState(if (on) C.amber else C.muted, tween(180), label = "tab")
                val bar by animateFloatAsState(if (on) 1f else 0f, tween(200, easing = FastOutSlowInEasing), label = "tabBar")
                Column(Modifier.weight(1f).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).clickable { onSelect(i) }.padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    // One line, whatever the width: the count is the first thing to give way.
                    val count = counts.getOrNull(i)
                    Text(buildAnnotatedString {
                        append(name)
                        if (count != null) withStyle(SpanStyle(fontSize = T.Small.fontSize, color = if (on) C.amber.copy(alpha = .8f) else C.faint)) { append(" $count") }
                    }, style = T.Strong.copy(color = ink, fontSize = T.Strong.fontSize * (if (names.size >= 4) .88f else 1f)), maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
                    Spacer(Modifier.height(9.dp))
                    Box(Modifier.fillMaxWidth(.25f + .45f * bar).height(3.dp).graphicsLayer { alpha = bar }.clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)).background(C.amber))
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(C.faint.copy(alpha = .28f)))
    }
}

/** A small square way into another part of the app: what it is and how it stands. */
@Composable
fun Tile(title: String, note: String, modifier: Modifier = Modifier, accent: Color = C.amber, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Column(modifier.pressable(src).lift(LocalOnSheet.current).clip(R8.Lg).background(raised()).clickable(src, LocalIndication.current, onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = T.Strong, maxLines = 1)
        Text(note, style = T.Small.copy(color = accent), maxLines = 1)
    }
}

/** A line that leads somewhere: a title, a word under it, and an arrow. On paper, or on [fill]. */
@Composable
fun LinkRow(title: String, note: String?, onClick: () -> Unit, modifier: Modifier = Modifier, fill: Color? = null, accent: Color = C.faint, trailing: (@Composable () -> Unit)? = null) {
    val src = remember { MutableInteractionSource() }
    Row(modifier.fillMaxWidth().pressable(src).lift(LocalOnSheet.current).clip(R8.Lg).background(fill ?: raised()).clickable(src, LocalIndication.current, onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = T.Strong)
            if (note != null) Text(note, style = T.Small)
        }
        trailing?.invoke()
        Sign(Glyph.NEXT, color = accent, size = 14.dp)
    }
}

/** Nothing here yet: an empty record, a word about it, and maybe a way to change that. */
@Composable
fun Empty(title: String, text: String? = null, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.padding(bottom = 6.dp).size(60.dp)) {
            val r = size.minDimension / 2
            drawCircle(C.track, r)
            for (k in 1..3) drawCircle(C.faint.copy(alpha = .28f), r * (.42f + k * .15f), style = Stroke(1.dp.toPx()))
            drawCircle(C.amber.copy(alpha = .55f), r * .3f)
            drawCircle(C.bg, r * .07f)
        }
        Text(title, style = T.Name, textAlign = TextAlign.Center)
        if (text != null) Text(text, style = T.Muted, textAlign = TextAlign.Center)
        if (action != null) { Spacer(Modifier.height(6.dp)); action() }
    }
}

/** A fact in a list of them: what it is on the left, how much on the right. */
@Composable
fun Fact(label: String, value: String, modifier: Modifier = Modifier, color: Color = C.ink) {
    Row(modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, style = T.Muted, modifier = Modifier.weight(1f))
        Text(value, style = T.Strong.copy(color = color), textAlign = TextAlign.End)
    }
}

/** A thin line between the rows of one card. */
@Composable
fun Rule(modifier: Modifier = Modifier) = Box(modifier.fillMaxWidth().height(1.dp).background(C.faint.copy(alpha = .18f)))

/**
 * A number with «−» and «+»: a tap moves it by one, and held down it keeps going, faster the longer it's
 * held (#6). [onStep] gets −1 or +1.
 */
@Composable
fun Stepper(label: String, modifier: Modifier = Modifier, onStep: (Int) -> Unit) {
    val step = rememberUpdatedState(onStep)
    @Composable
    fun Key(text: String, d: Int) {
        Box(
            Modifier.clip(CircleShape).background(C.surface2).heightIn(min = 40.dp).widthIn(min = 52.dp)
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
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) { Text(text, style = T.Button.copy(color = C.ink)) }
    }
    Row(modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Key("−", -1)
        Text(label, style = T.Strong, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Key("+", 1)
    }
}
