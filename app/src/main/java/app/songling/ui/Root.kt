package app.songling.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.songling.container
import app.songling.data.Album
import app.songling.data.Artist
import app.songling.data.Goal
import app.songling.data.Pet
import app.songling.data.Play
import app.songling.data.Settings
import app.songling.data.Track
import app.songling.domain.Stage
import app.songling.domain.Studio
import app.songling.domain.Words
import app.songling.listen.NowPlaying
import app.songling.ru
import app.songling.ui.components.LocalPixelCovers
import app.songling.ui.components.pressable
import app.songling.ui.components.t
import app.songling.ui.den.DenScreen
import app.songling.ui.den.look
import app.songling.ui.onboarding.Onboarding
import app.songling.ui.saga.SagaScreen
import app.songling.ui.shelf.ShelfScreen
import app.songling.ui.theme.C
import app.songling.ui.theme.T
import app.songling.ui.theme.paper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

val LocalRu = staticCompositionLocalOf { true }

enum class Tab { DEN, SHELF, SAGA, GOALS }

/** What is open over the tabs. */
sealed class Overlay {
    data class AlbumOf(val key: String) : Overlay()
    data class ArtistOf(val key: String) : Overlay()
    /** A song's sheet; [hint] names a song the shelf doesn't know yet (the one playing now, #25). */
    data class TrackOf(val key: String, val hint: app.songling.listen.NowPlaying? = null) : Overlay()
    /** Putting an album, single or song on the shelf. */
    data class Add(val wish: Boolean = true) : Overlay()
    /** A listen the phone didn't hear: vinyl, radio, a concert. */
    data class Log(val concert: Boolean = false) : Overlay()
    data object PetInfo : Overlay()
    data object Quests : Overlay()
    /** «I listened to this»: an album whole ([track] null) or one song. */
    data class Mark(val album: String?, val track: String?) : Overlay()
    /** An album made or put right by hand ([key] null: a new one). */
    data class AlbumEdit(val key: String?) : Overlay()
    /** Hang an album («album:key») or a band («artist:key») in the den. */
    data class Poster(val choice: String) : Overlay()
    data object Bestiary : Overlay()
    data object Shop : Overlay()
    data class GoalEdit(val id: Long? = null, val draft: Goal? = null) : Overlay()
    data object Settings : Overlay()
    data object Import : Overlay()
    data object How : Overlay()
    data class Hatched(val pet: Long) : Overlay()
    /** Postcards to share. */
    data object Cards : Overlay()
}

@Stable
class Nav(val go: (Overlay?) -> Unit, val tab: (Tab) -> Unit) {
    fun close() = go(null)
    var said by mutableStateOf("" to 0L)
        private set
    var toast by mutableStateOf("" to 0L)
        private set
    /** Strokes on the pet light up its eyes for a moment. */
    var stroked by mutableStateOf(0L)
    /** A sheet a screen opens over everything, the dock included (see [Floating]). */
    var floating by mutableStateOf<(@Composable () -> Unit)?>(null)
    fun say(t: String) { said = t to System.nanoTime() }
    fun toast(t: String) { toast = t to System.nanoTime() }
}

val LocalNav = staticCompositionLocalOf<Nav> { error("no nav") }

/** Shows [content] over the whole app while this is in the composition. */
@Composable
fun Floating(content: @Composable () -> Unit) {
    val nav = LocalNav.current
    val latest by androidx.compose.runtime.rememberUpdatedState(content)
    androidx.compose.runtime.DisposableEffect(Unit) {
        nav.floating = { latest() }
        onDispose { nav.floating = null }
    }
}

/** The pet's name: the one given, or its species' default. */
fun Pet.title(ru: Boolean) = name.ifBlank { Words.defaultName(species, ru) }

@Composable
fun SonglingRoot(s: Settings, clock: Long? = null, tab0: Tab = Tab.DEN, overlay0: Overlay? = null, playing0: NowPlaying? = null) {
    val ctx = LocalContext.current
    val c = ctx.container
    val ru = ctx.ru
    CompositionLocalProvider(LocalRu provides ru) {
        if (!s.onboarded) { Onboarding(); return@CompositionLocalProvider }
        val dao = c.db.dao()
        val plays: List<Play>? by dao.plays().collectAsStateWithLifecycle(initialValue = null)
        val albums: List<Album>? by dao.albums().collectAsStateWithLifecycle(initialValue = null)
        val artists: List<Artist>? by dao.artists().collectAsStateWithLifecycle(initialValue = null)
        val tracks: List<Track>? by dao.tracks().collectAsStateWithLifecycle(initialValue = null)
        val pets: List<Pet>? by dao.pets().collectAsStateWithLifecycle(initialValue = null)
        val goals: List<Goal>? by dao.goals().collectAsStateWithLifecycle(initialValue = null)
        val live by c.tracker.now.collectAsStateWithLifecycle()
        var now by remember { mutableStateOf(clock ?: System.currentTimeMillis()) }
        LaunchedEffect(Unit) { if (clock == null) while (true) { delay(30_000); now = System.currentTimeMillis() } }
        var st by remember { mutableStateOf<Studio?>(null) }
        LaunchedEffect(plays, albums, artists, tracks, pets, goals, s, now) {
            val p = plays; val a = albums; val ar = artists; val tr = tracks; val pe = pets; val g = goals
            if (p != null && a != null && ar != null && tr != null && pe != null && g != null)
                st = withContext(Dispatchers.Default) { Studio(p, a, ar, tr, pe, g, s, now) }
        }
        val studio = st ?: run { Box(Modifier.fillMaxSize().background(C.bg)); return@CompositionLocalProvider }

        var tab by rememberSaveable { mutableStateOf(tab0) }
        var overlay by remember { mutableStateOf(overlay0) }
        val nav = remember { Nav({ overlay = it }, { tab = it; overlay = null }) }
        val playing = playing0 ?: live

        // A grown pet takes its form for good; an egg that has eaten enough hatches.
        var lastStage by remember { mutableStateOf<Pair<Long, Stage>?>(null) }
        LaunchedEffect(studio.pet?.pet?.id, studio.pet?.stage) {
            val p = studio.pet ?: return@LaunchedEffect
            if (p.stage >= Stage.ADULT && p.pet.form == null && p.leaning != null) c.write { db.dao().update(p.pet.copy(form = p.leaning)) }
            val before = lastStage
            if (before != null && before.first == p.pet.id && before.second == Stage.EGG && p.stage == Stage.BABY && clock == null) overlay = Overlay.Hatched(p.pet.id)
            else if (before != null && before.first == p.pet.id && before.second < p.stage && clock == null)
                nav.toast(if (ru) "${p.pet.title(true)} вырос: теперь ${Words.stage(p.stage, true)}!" else "${p.pet.title(false)} grew: now ${Words.stage(p.stage, false)}!")
            lastStage = p.pet.id to p.stage
        }
        // The den's line follows the pet: a new pet clears whatever the old one was saying (#18).
        LaunchedEffect(studio.pet?.pet?.id) { if (nav.said.first.isNotEmpty()) nav.say("") }

        val backdrop = rememberBackdrop(s.backdrop, s.backdropStamp)
        // The colour of the music: from what plays now, else what's played most this week, else the pet's genre.
        val source = remember(studio, playing?.cover) {
            val week = studio.plays.filter { it.at >= studio.now - 7 * app.songling.data.DAY && it.album.isNotEmpty() }
                .groupBy { it.album }.maxByOrNull { e -> e.value.sumOf { it.ms } }?.key
            val album = studio.albums[week ?: ""]
            (playing?.cover ?: album?.cover) to (album?.genre ?: studio.byGenre.maxByOrNull { it.value }?.key)
        }
        val pal by androidx.compose.runtime.produceState<Pair<Int, Int>?>(null, source) {
            value = withContext(Dispatchers.IO) { app.songling.data.Covers.palette(ctx, source.first) }
                ?: source.second?.let { g -> app.songling.ui.theme.fromGenre(g) }
        }
        val accent by androidx.compose.animation.animateColorAsState(pal?.let { androidx.compose.ui.graphics.Color(it.first) } ?: app.songling.ui.theme.Accent.DEFAULT, androidx.compose.animation.core.tween(900), label = "accent")
        val accent2 by androidx.compose.animation.animateColorAsState(pal?.let { androidx.compose.ui.graphics.Color(it.second) } ?: app.songling.ui.theme.Accent.DEFAULT2, androidx.compose.animation.core.tween(900), label = "accent2")
        app.songling.ui.theme.Accent.main = accent
        app.songling.ui.theme.Accent.second = accent2
        CompositionLocalProvider(LocalNav provides nav, LocalPixelCovers provides s.pixelCovers, LocalBackdrop provides backdrop,
            app.songling.ui.components.LocalMedium provides (if (s.look == "TAPE") app.songling.ui.components.Medium.TAPE else app.songling.ui.components.Medium.VINYL)) {
            Box(Modifier.fillMaxSize()) {
                BackdropLayer()
                Column(Modifier.fillMaxSize()) {
                    Header(studio, onPicks = { overlay = Overlay.Shop }, onSettings = { overlay = Overlay.Settings })
                    // The dock sits above the system navigation bar, so the screens end above it too:
                    // with three-button navigation their last lines would otherwise hide under the dock.
                    Box(Modifier.weight(1f).navigationBarsPadding()) {
                        // One screen fades into the next instead of snapping.
                        androidx.compose.animation.Crossfade(tab, animationSpec = androidx.compose.animation.core.tween(170), label = "tab") { shown ->
                            when (shown) {
                                Tab.DEN -> DenScreen(studio, playing, nav)
                                Tab.SHELF -> ShelfScreen(studio, nav)
                                Tab.SAGA -> SagaScreen(studio, nav)
                                Tab.GOALS -> app.songling.ui.saga.GoalsScreen(studio, nav)
                            }
                        }
                    }
                }
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                    Box(Modifier.matchParentSize().padding(top = 20.dp).background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(C.bg.copy(alpha = 0f), C.bg.copy(alpha = .92f), C.bg))))
                    Dock(tab, playing?.playing == true, { tab = it }, { overlay = Overlay.Add(wish = false) })
                }
                when (val o = overlay) {
                    is Overlay.AlbumOf -> app.songling.ui.sheets.AlbumSheet(studio, o.key, nav)
                    is Overlay.ArtistOf -> app.songling.ui.sheets.ArtistSheet(studio, o.key, nav)
                    is Overlay.TrackOf -> app.songling.ui.sheets.TrackSheet(studio, o.key, nav, o.hint)
                    is Overlay.Add -> app.songling.ui.sheets.AddSheet(studio, o.wish, nav)
                    is Overlay.Log -> app.songling.ui.sheets.LogSheet(studio, o.concert, nav)
                    Overlay.PetInfo -> app.songling.ui.den.PetSheet(studio, nav)
                    Overlay.Quests -> app.songling.ui.den.QuestsSheet(studio, nav)
                    is Overlay.Mark -> app.songling.ui.sheets.MarkSheet(studio, o.album, o.track, nav)
                    is Overlay.AlbumEdit -> app.songling.ui.sheets.AlbumEditSheet(studio, o.key, nav)
                    is Overlay.Poster -> app.songling.ui.den.PosterSheet(studio, o.choice, nav)
                    Overlay.Bestiary -> app.songling.ui.den.BestiaryPage(studio, nav)
                    Overlay.Shop -> app.songling.ui.den.ShopPage(studio, nav)
                    is Overlay.GoalEdit -> app.songling.ui.saga.GoalSheet(studio, o.id, o.draft, nav)
                    Overlay.Settings -> app.songling.ui.settings.SettingsPage(studio, nav)
                    Overlay.Import -> app.songling.ui.settings.ImportPage(studio, nav)
                    Overlay.How -> app.songling.ui.settings.HowPage(nav)
                    is Overlay.Hatched -> app.songling.ui.den.HatchSheet(studio, o.pet, nav)
                    Overlay.Cards -> app.songling.ui.cards.CardsPage(studio, nav)
                    null -> Unit
                }
                nav.floating?.invoke()
                Toast(nav, Modifier.align(Alignment.TopCenter))
            }
        }
    }
}

@Composable
private fun Header(st: Studio, onPicks: () -> Unit, onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Songling", style = T.Title.copy(color = C.amber), modifier = Modifier.weight(1f))
        val src = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
        val purse = t("Медиаторы: ${st.plectrums}. Обустроить логово", "Plectrums: ${st.plectrums}. Furnish the den")
        Row(Modifier.pressable(src).paper(99.dp, 2.dp).clip(CircleShape).background(C.surface)
            .clickable(src, androidx.compose.foundation.LocalIndication.current, onClick = onPicks).semantics(mergeDescendants = true) { contentDescription = purse }
            .padding(horizontal = 13.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Canvas(Modifier.size(12.dp, 14.dp)) {
                val p = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width / 2, size.height); cubicTo(0f, size.height * .55f, 0f, 0f, size.width / 2, 0f); cubicTo(size.width, 0f, size.width, size.height * .55f, size.width / 2, size.height); close()
                }
                drawPath(p, C.amber)
            }
            Text("${st.plectrums}", style = T.Strong.copy(color = C.amber))
        }
        val label = t("Настройки", "Settings")
        Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onSettings).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(20.dp)) {
                val r = size.width / 2; val w = 2.dp.toPx()
                for (i in 0 until 8) {
                    val a = i * Math.PI / 4
                    drawLine(C.muted, Offset(r + (r * .55f) * kotlin.math.cos(a).toFloat(), r + (r * .55f) * kotlin.math.sin(a).toFloat()),
                        Offset(r + r * kotlin.math.cos(a).toFloat(), r + r * kotlin.math.sin(a).toFloat()), w * 1.4f)
                }
                drawCircle(C.muted, r * .62f, style = Stroke(w))
                drawCircle(C.muted, r * .22f, style = Stroke(w))
            }
        }
    }
}

@Composable
private fun Toast(nav: Nav, modifier: Modifier) {
    val (text, stamp) = nav.toast
    var shown by remember { mutableStateOf(false) }
    // The words stay while the note slides away.
    var last by remember { mutableStateOf("") }
    LaunchedEffect(stamp) { if (text.isNotEmpty()) { last = text; shown = true; delay(3200); shown = false } }
    androidx.compose.animation.AnimatedVisibility(shown, modifier.statusBarsPadding().padding(top = 56.dp, start = 16.dp, end = 16.dp),
        enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(180)) + androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(240)) { -it / 2 },
        exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200)) + androidx.compose.animation.slideOutVertically(androidx.compose.animation.core.tween(200)) { -it / 2 }) {
        // Ink on the light page, cream on the dark one: it reads over anything under it.
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(C.ink).padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(22.dp).clip(CircleShape).background(C.amber), contentAlignment = Alignment.Center) {
                app.songling.ui.components.Sign(app.songling.ui.components.Glyph.CHECK, color = C.onAmber, size = 10.dp, stroke = 2.dp)
            }
            Text(last, style = T.Body.copy(color = C.bg), modifier = Modifier.weight(1f))
        }
    }
}
