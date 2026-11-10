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
 * «Плед»: a light, warm room the colour of oatmeal, paper cards with a soft shadow under them and a
 * quilted cushion at the bottom. The accent ([amber]) and the second colour ([lilac]) come from the
 * cover of the music, warmed and darkened enough to read on the light page.
 */
object Accent {
    val DEFAULT = Color(0xFFC8553D)
    val DEFAULT2 = Color(0xFF6E9B6A)
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
    // Cold hues (blues, cyans, violets) go dusty, so the room stays warm.
    val cold = hue in 165f..285f
    val s = sat.coerceIn(if (second) .32f else .45f, if (second) .55f else .72f) * if (cold) .62f else 1f
    // Darker for the light page, brighter for the dark one.
    val v = (if (Mode.dark) (if (second) .80f else .90f) else (if (second) .62f else .74f)) * if (cold) .86f else 1f
    return android.graphics.Color.HSVToColor(floatArrayOf(hue, s, v))
}

private fun Color.over(bg: Color, a: Float) = Color(red * a + bg.red * (1 - a), green * a + bg.green * (1 - a), blue * a + bg.blue * (1 - a))

/** Light («Плед») or dark («Плед ночью»): read by every colour below, so switching recolours everything. */
object Mode {
    var dark by mutableStateOf(false)
}

private fun pick(light: Long, dark: Long) = Color(if (Mode.dark) dark else light)

object C {
    val bg get() = pick(0xFFF3EADB, 0xFF1B1411)
    /** Cards: paper (by day) or felt (at night). */
    val surface get() = pick(0xFFFFF8EE, 0xFF2A201A)
    /** Wells, empty bars, chips that are off. */
    val surface2 get() = pick(0xFFEFE3D1, 0xFF362A22)
    /** The soft shadow under cards and thin lines. */
    val line get() = pick(0xFFE2D4BF, 0xFF110B09)
    val ink get() = pick(0xFF3B2A22, 0xFFF6E9D7)
    val muted get() = pick(0xFF7E6A59, 0xFFC7AD93)
    val faint get() = pick(0xFFA8957F, 0xFF8E7764)
    val amber: Color get() = Accent.main
    val amberSoft: Color get() = Accent.main.over(surface, .13f)
    val onAmber: Color get() = if (Accent.main.luminance() > .55f) Color(0xFF2A1D17) else Color.White
    val coral get() = pick(0xFFC8553D, 0xFFE8775A)
    val coralSoft get() = pick(0xFFF7E0D6, 0xFF3E2219)
    val lilac: Color get() = Accent.second
    val lilacSoft: Color get() = Accent.second.over(surface, .14f)
    val mint get() = pick(0xFF4E9A78, 0xFF6FC29C)
    /** Stars: always honey, whatever the cover. */
    val honey get() = pick(0xFFE2A13A, 0xFFF2B54A)
    /** The vinyl itself and the cassette shell. */
    val vinyl = Color(0xFF221D1C)
    val shell get() = pick(0xFFE9E2D6, 0xFF3A322C)

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
    val Eyebrow: TextStyle get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, letterSpacing = 0.12.em, color = C.faint)
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
