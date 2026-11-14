package app.songling.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.songling.R
import app.songling.domain.Genre

/**
 * A quiet page: near-white by day, graphite by night, white (or dark) paper cards with a thin edge under
 * them, and ink for text. Nothing on it is coloured but the music: the accent ([amber]) and the second
 * colour ([lilac]) come from the cover of what plays, kept clear and bright enough to read.
 */
object Accent {
    val DEFAULT = Color(0xFFE0553F)
    val DEFAULT2 = Color(0xFF4C8FA8)
    var main by mutableStateOf(DEFAULT)
    var second by mutableStateOf(DEFAULT2)
}

/** Two interface colours from a genre, when there's no cover to take them from. */
fun fromGenre(g: Genre): Pair<Int, Int> {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(g.argb.toInt(), hsv)
    return warm(hsv[0], hsv[1]) to warm((hsv[0] + 140f) % 360f, .45f, second = true)
}

/** A colour of [hue] that reads on the light page: saturated but not loud, dark enough for text. */
fun warm(hue: Float, sat: Float, second: Boolean = false): Int {
    // Oranges and yellows turn brown when darkened: they stay bright and saturated instead (amber, not tan).
    val sunny = hue in 12f..62f
    val s = if (sunny) sat.coerceIn(.78f, .92f) else sat.coerceIn(if (second) .42f else .55f, if (second) .62f else .78f)
    // Darker for the light page, brighter for the dark one.
    val v = when {
        sunny -> if (Mode.dark) .96f else .86f
        Mode.dark -> if (second) .84f else .92f
        else -> if (second) .66f else .76f
    }
    return android.graphics.Color.HSVToColor(floatArrayOf(hue, s, v))
}

private fun Color.over(bg: Color, a: Float) = Color(red * a + bg.red * (1 - a), green * a + bg.green * (1 - a), blue * a + bg.blue * (1 - a))

/** Light («Плед») or dark («Плед ночью»): read by every colour below, so switching recolours everything. */
object Mode {
    var dark by mutableStateOf(false)
}

private fun pick(light: Long, dark: Long) = Color(if (Mode.dark) dark else light)

object C {
    val bg get() = pick(0xFFF4F3F0, 0xFF121214)
    /** Cards: white paper by day, dark by night. */
    val surface get() = pick(0xFFFFFFFF, 0xFF1D1D21)
    /** Wells, chips that are off. */
    val surface2 get() = pick(0xFFF0EFEB, 0xFF2A2A30)
    /** The thin edge under cards and thin lines. */
    val line get() = pick(0xFFE3E1DB, 0xFF08080A)
    /** The empty part of a bar: a shade under [surface2], so it shows on a well too. */
    val track get() = pick(0xFFE4E2DC, 0xFF37373F)
    val ink get() = pick(0xFF1D1D21, 0xFFF3F3F5)
    /** Second-rank text, and third: both dark enough to read on the page and on paper. */
    val muted get() = pick(0xFF575761, 0xFFB9B9C3)
    val faint get() = pick(0xFF85858F, 0xFF8B8B96)
    val amber: Color get() = Accent.main
    val amberSoft: Color get() = Accent.main.over(surface, .13f)
    val onAmber: Color get() = if (Accent.main.luminance() > .40f) Color(0xFF1D1D21) else Color.White
    val coral get() = pick(0xFFE0553F, 0xFFF0715A)
    val coralSoft get() = pick(0xFFFCE6E1, 0xFF3A1F1B)
    val lilac: Color get() = Accent.second
    val lilacSoft: Color get() = Accent.second.over(surface, .14f)
    val mint get() = pick(0xFF2E9C74, 0xFF5CC79B)
    /** Stars: always honey, whatever the cover. */
    val honey get() = pick(0xFFF2A62B, 0xFFF7B84A)
    /** The vinyl itself and the cassette shell. */
    val vinyl = Color(0xFF1C1C1F)
    val shell get() = pick(0xFFE9E8E4, 0xFF34343A)

    fun genre(g: Genre) = Color(g.argb)
}

/** A paper card's soft shadow: a strip of [C.line] peeking out under it. */
fun Modifier.paper(radius: Dp = 22.dp, depth: Dp = 3.dp) = drawBehind {
    val r = radius.toPx()
    drawRoundRect(C.line, Offset(0f, depth.toPx()), size, CornerRadius(r))
}

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun comfortaa(w: Int) = Font(R.font.comfortaa, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))

val Comfortaa = FontFamily(comfortaa(500), comfortaa(700))

/** Handwriting, for cassette labels. */
val Caveat = FontFamily(Font(R.font.caveat, FontWeight.Normal))

/** Typewriter capitals, for «SIDE A» and catalogue numbers. */
val Mono = FontFamily(Font(R.font.plex_mono, FontWeight.Normal))

val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
)

object R8 {
    val Xl = RoundedCornerShape(28.dp)
    val Lg = RoundedCornerShape(22.dp)
    val Md = RoundedCornerShape(16.dp)
    val Sm = RoundedCornerShape(12.dp)
}

object T {
    val Display: TextStyle get() = TextStyle(fontFamily = Comfortaa, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp, color = C.ink)
    val Title: TextStyle get() = TextStyle(fontFamily = Comfortaa, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, color = C.ink)
    val Name: TextStyle get() = TextStyle(fontFamily = Comfortaa, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 21.sp, color = C.ink)
    val Eyebrow: TextStyle get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, lineHeight = 16.sp, letterSpacing = 0.12.em, color = C.faint)
    val Strong: TextStyle get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 15.5.sp, lineHeight = 21.sp, color = C.ink)
    val Body: TextStyle get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 22.sp, color = C.ink)
    val Muted: TextStyle get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, lineHeight = 19.sp, color = C.muted)
    val Small: TextStyle get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp, color = C.faint)
    val Button: TextStyle get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    val Number: TextStyle get() = TextStyle(fontFamily = Comfortaa, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = C.coral, fontFeatureSettings = "tnum")
    val Say: TextStyle get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, lineHeight = 20.sp, color = C.ink)
}

@Composable
fun SonglingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (Mode.dark) androidx.compose.material3.darkColorScheme(
            primary = C.coral, onPrimary = Color.White, background = C.bg, surface = C.surface, onSurface = C.ink,
            onBackground = C.ink, surfaceContainer = C.surface, surfaceContainerHigh = C.surface2, outline = C.line, error = C.coral,
        ) else lightColorScheme(
            primary = C.coral, onPrimary = Color.White, background = C.bg, surface = C.surface, onSurface = C.ink,
            onBackground = C.ink, surfaceContainer = C.surface, surfaceContainerHigh = C.surface2, outline = C.line, error = C.coral,
        ),
        typography = MaterialTheme.typography.copy(bodyLarge = T.Body, bodyMedium = T.Body, bodySmall = T.Small, labelLarge = T.Button, titleLarge = T.Title),
        content = content,
    )
}
