package app.songling.ui.den

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.songling.container
import app.songling.data.Covers
import app.songling.data.Keys
import app.songling.data.Meta
import app.songling.domain.Item
import app.songling.domain.Studio
import app.songling.ui.LocalRu
import app.songling.ui.Nav
import app.songling.ui.art.Buf
import app.songling.ui.components.Btn
import app.songling.ui.components.BufImage
import app.songling.ui.components.Cover
import app.songling.ui.components.Field
import app.songling.ui.components.Kind
import app.songling.ui.components.Segmented
import app.songling.ui.components.Sheet
import app.songling.ui.components.t
import app.songling.ui.theme.C
import app.songling.ui.theme.T
import app.songling.ui.theme.paper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder

/** What hangs in the den's two frames: album covers or a band's picture. */
object Posters {
    fun artistName(key: String) = "artist|$key"

    /** A picture of the artist (Deezer, no key needed), kept with the covers. Null if none was found. */
    fun artistPicture(ctx: Context, key: String, name: String): String? {
        val file = Covers.nameFor(artistName(key))
        if (Covers.file(ctx, file).exists()) return file
        val body = Meta.get("https://api.deezer.com/search/artist?limit=3&q=" + URLEncoder.encode(name, "UTF-8")) ?: return null
        val arr = runCatching { JSONObject(body).optJSONArray("data") }.getOrNull() ?: return null
        val hit = (0 until arr.length()).map { arr.getJSONObject(it) }.firstOrNull { Keys.norm(it.optString("name")) == Keys.norm(name) } ?: arr.optJSONObject(0) ?: return null
        val url = hit.optString("picture_big").ifBlank { hit.optString("picture_medium") }.ifBlank { return null }
        if (url.contains("/artist//")) return null
        return Covers.download(ctx, artistName(key), url)
    }

    /** What each frame shows: the choice made, or the favourites (top album left, top artist right). */
    fun choices(st: Studio): List<String> {
        val topAlbum = st.byAlbum.values.maxByOrNull { it.ms }?.key
        val topArtist = st.byArtist.values.maxByOrNull { it.ms }?.key
        return listOf(
            st.s.posters.getOrNull(0).orEmpty().ifEmpty { topAlbum?.let { "album:$it" }.orEmpty() },
            st.s.posters.getOrNull(1).orEmpty().ifEmpty { topArtist?.let { "artist:$it" }.orEmpty() },
        )
    }

    fun label(st: Studio, choice: String): String = when {
        choice.startsWith("album:") -> st.albumTitle(choice.removePrefix("album:"))
        choice.startsWith("artist:") -> st.artistName(choice.removePrefix("artist:"))
        else -> ""
    }

    /** The picture file for a choice (an artist's is fetched if need be). Call off the main thread. */
    fun file(ctx: Context, st: Studio, choice: String): String? = when {
        choice.startsWith("album:") -> st.albums[choice.removePrefix("album:")]?.cover
        choice.startsWith("artist:") -> choice.removePrefix("artist:").let { k ->
            artistPicture(ctx, k, st.artistName(k)) ?: st.byArtist[k]?.albums?.maxByOrNull { st.byAlbum[it]?.ms ?: 0 }?.let { st.albums[it]?.cover }
        }
        else -> null
    }

    /** Both frames' picture files. Call off the main thread. */
    fun files(ctx: Context, st: Studio): List<String?> = choices(st).map { c -> runCatching { file(ctx, st, c) }.getOrNull() }

    /** Both frames as small pixel pictures. Call off the main thread. */
    fun pictures(ctx: Context, st: Studio): List<Buf?> = choices(st).map { c -> file(ctx, st, c)?.let { Covers.pixels(ctx, it, 20) } }
}

/** Hang an album or a band in one of the two frames. */
@Composable
fun PosterSheet(st: Studio, choice: String, nav: Nav) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    val have = Item.POSTER.name in st.s.bought
    val current = Posters.choices(st)
    var frame by remember { mutableStateOf(if (choice.startsWith("artist:")) 1 else 0) }
    Sheet(onDismiss = nav::close) {
        Text(t("Повесить в логово", "Hang it in the den"), style = T.Title)
        Text("«${Posters.label(st, choice)}»", style = T.Name.copy(color = C.amber))
        if (!have) {
            Text(t("Для постеров нужны рамки: «Постеры» в магазине логова, ${Item.POSTER.price} медиаторов.",
                "Posters need frames: «Posters» in the den shop, ${Item.POSTER.price} plectrums."), style = T.Muted)
            Btn(t("Купить рамки и повесить · ${Item.POSTER.price}", "Buy frames and hang · ${Item.POSTER.price}"), {
                co.write { settings.update { s -> s.copy(spent = s.spent + Item.POSTER.price, bought = s.bought + Item.POSTER.name, hidden = s.hidden - Item.POSTER.name,
                    posters = s.posters.toMutableList().also { it[frame] = choice }) } }
                nav.toast(if (ru) "Повесили в логове" else "Hung in the den"); nav.close()
            }, Modifier.fillMaxWidth(), kind = Kind.AMBER, enabled = st.plectrums >= Item.POSTER.price)
            return@Sheet
        }
        Text(t("В какую рамку?", "Which frame?"), style = T.Strong)
        Segmented(listOf(t("Левая, квадратная", "Left, square"), t("Правая, с подписью", "Right, with a name strip")), frame, { frame = it })
        Text(t("Сейчас там: «${Posters.label(st, current[frame]).ifEmpty { "пусто" }}»", "Now there: «${Posters.label(st, current[frame]).ifEmpty { "nothing" }}»"), style = T.Small)
        Btn(t("Повесить", "Hang it"), {
            co.write { settings.update { s -> s.copy(hidden = s.hidden - Item.POSTER.name, posters = s.posters.toMutableList().also { it[frame] = choice }) } }
            nav.toast(if (ru) "Повесили в логове" else "Hung in the den"); nav.close()
        }, Modifier.fillMaxWidth(), kind = Kind.AMBER)
    }
}

/** Choose what hangs in a frame: a search over the shelf's albums and artists. */
@Composable
fun PosterPick(st: Studio, frame: Int, onDone: () -> Unit) {
    val co = LocalContext.current.container
    var q by remember { mutableStateOf("") }
    Sheet(onDismiss = onDone) {
        Text(if (frame == 0) t("Левая рамка", "Left frame") else t("Правая рамка", "Right frame"), style = T.Title)
        Field(q, { q = it }, t("Альбом или артист", "An album or an artist"))
        val nq = Keys.norm(q)
        val artists = st.byArtist.values.sortedByDescending { it.ms }.filter { nq.isEmpty() || Keys.norm(st.artistName(it.key)).contains(nq) }.take(6)
        val albums = st.byAlbum.values.sortedByDescending { it.ms }.filter { nq.isEmpty() || Keys.norm(st.albumTitle(it.key)).contains(nq) }.take(8)
        fun pick(c: String) { co.write { settings.update { s -> s.copy(posters = s.posters.toMutableList().also { it[frame] = c }) } }; onDone() }
        Text(t("Группы", "Bands"), style = T.Eyebrow)
        for (a in artists) Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { pick("artist:${a.key}") }.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ArtistFace(st, a.key)
            Text(st.artistName(a.key), style = T.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(t("Альбомы", "Albums"), style = T.Eyebrow)
        for (a in albums) Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { pick("album:${a.key}") }.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Cover(st.albums[a.key]?.cover, st.albumTitle(a.key), st.albums[a.key]?.genre, 40.dp)
            Column {
                Text(st.albumTitle(a.key), style = T.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(a.artistName, style = T.Small, maxLines = 1)
            }
        }
        Btn(t("Как раньше: любимое", "Back to the favourite"), { pick("") }, Modifier.fillMaxWidth(), kind = Kind.GHOST, small = true)
    }
}

/** An artist's picture, round, fetched once. */
@Composable
fun ArtistFace(st: Studio, key: String, size: androidx.compose.ui.unit.Dp = 40.dp) {
    val ctx = LocalContext.current
    val file by produceState<String?>(null, key) { value = withContext(Dispatchers.IO) { runCatching { Posters.artistPicture(ctx, key, st.artistName(key)) }.getOrNull() } }
    Cover(file, st.artistName(key), st.artists[key]?.genre, size, round = size / 2)
}

/** The two frames, side by side, with what's in them and a way to change it (for the shop). */
@Composable
fun PosterFrames(st: Studio, pictures: List<Buf?>, onPick: (Int) -> Unit) {
    val ch = Posters.choices(st)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        for (i in 0..1) Row(Modifier.weight(1f).paper(18.dp, 3.dp).clip(RoundedCornerShape(18.dp)).background(C.surface).clickable { onPick(i) }.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).background(C.surface2), contentAlignment = Alignment.Center) {
                pictures.getOrNull(i)?.let { BufImage(it, scale = 2.dp) }
            }
            Column(Modifier.weight(1f)) {
                Text(if (i == 0) t("Левая", "Left") else t("Правая", "Right"), style = T.Small)
                Text(Posters.label(st, ch[i]).ifEmpty { t("выбрать", "choose") }, style = T.Strong, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
