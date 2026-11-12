package app.songling.ui.cards

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import app.songling.domain.Span
import app.songling.domain.Studio
import app.songling.ui.LocalRu
import app.songling.ui.Nav
import app.songling.ui.components.Btn
import app.songling.ui.components.Chip
import app.songling.ui.components.Kind
import app.songling.ui.components.Page
import app.songling.ui.components.Segmented
import app.songling.ui.components.t
import app.songling.ui.theme.C
import app.songling.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Postcards for a social feed: flip through them, pick the shape and the period, share or keep one. */
@Composable
fun CardsPage(st: Studio, nav: Nav) {
    val ctx = LocalContext.current
    val ru = LocalRu.current
    val scope = rememberCoroutineScope()
    var shape by rememberSaveable { mutableStateOf(Shape.POST) }
    var span by rememberSaveable { mutableStateOf(Span.MONTH) }
    val cards = remember(st, span) { Postcards.available(st, span) }
    val pager = rememberPagerState { cards.size }
    val card = cards.getOrNull(pager.currentPage)
    var busy by remember { mutableStateOf(false) }

    Page(t("Открытки", "Postcards"), nav::close) {
        Text(t("Листай в сторону. Каждую можно отправить в соцсети или сохранить в галерею.", "Swipe sideways. Share any of them or keep it in the gallery."), style = T.Muted)
        Segmented(listOf(t("Пост 4:5", "Post 4:5"), t("История 9:16", "Story 9:16")), shape.ordinal, { shape = Shape.entries[it] })
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (c in cards) Chip(c.title(ru), c == card, { scope.launch { pager.animateScrollToPage(cards.indexOf(c)) } })
        }
        HorizontalPager(pager, Modifier.fillMaxWidth(), pageSpacing = 12.dp, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = if (shape == Shape.STORY) 48.dp else 12.dp)) { i ->
            val c = cards[i]
            val bmp by produceState<Bitmap?>(null, c, span, shape, st) {
                value = withContext(Dispatchers.Default) { runCatching { Postcards.draw(ctx, st, c, span, shape, ru) }.getOrNull() }
            }
            Box(Modifier.fillMaxWidth().aspectRatio(Postcards.W / shape.h.toFloat()).clip(RoundedCornerShape(18.dp)).background(C.surface2), contentAlignment = Alignment.Center) {
                bmp?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth) }
                    ?: Text(t("Рисую…", "Drawing…"), style = T.Muted)
            }
        }
        // Dots under the cards.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            cards.indices.forEach { i -> Box(Modifier.padding(3.dp).size(if (i == pager.currentPage) 8.dp else 6.dp).clip(CircleShape).background(if (i == pager.currentPage) C.amber else C.line)) }
        }
        if (card?.bySpan == true) {
            val spans = listOf(Span.WEEK, Span.MONTH, Span.YEAR, Span.LAST_YEAR, Span.ALL)
            Segmented(listOf(t("Неделя", "Week"), t("Месяц", "Month"), t("Год", "Year"), t("Прошлый", "Last yr"), t("Всё", "All")), spans.indexOf(span), { span = spans[it] })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(t("Сохранить", "Save"), {
                val c = card ?: return@Btn
                busy = true
                scope.launch {
                    val ok = withContext(Dispatchers.IO) { runCatching { save(ctx, Postcards.draw(ctx, st, c, span, shape, ru), c) }.getOrDefault(false) }
                    busy = false
                    nav.toast(if (ok) t2(ru, "Открытка в галерее, в папке Songling", "Saved to the gallery, in Songling") else t2(ru, "Не получилось сохранить", "Couldn't save it"))
                }
            }, Modifier.weight(1f), kind = Kind.GHOST, enabled = !busy && card != null)
            Btn(t("Поделиться", "Share"), {
                val c = card ?: return@Btn
                busy = true
                scope.launch {
                    val f = withContext(Dispatchers.IO) { runCatching { file(ctx, Postcards.draw(ctx, st, c, span, shape, ru), c) }.getOrNull() }
                    busy = false
                    if (f != null) share(ctx, f, ru)
                }
            }, Modifier.weight(1f), kind = Kind.AMBER, enabled = !busy && card != null)
        }
    }
}

private fun t2(ru: Boolean, r: String, e: String) = if (ru) r else e

private fun file(ctx: Context, bmp: Bitmap, c: Postcard): File {
    val dir = File(ctx.cacheDir, "share").apply { mkdirs() }
    val f = File(dir, "songling_${c.name.lowercase()}.png")
    f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    return f
}

private fun share(ctx: Context, f: File, ru: Boolean) {
    val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
    val send = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    ctx.startActivity(Intent.createChooser(send, t2(ru, "Поделиться открыткой", "Share the postcard")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** Into Pictures/Songling; older Android, without the media store's own folders, gets it shared instead. */
private fun save(ctx: Context, bmp: Bitmap, c: Postcard): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "songling_${c.name.lowercase()}_${System.currentTimeMillis() / 1000}.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Songling")
    }
    val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
    ctx.contentResolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } ?: return false
    return true
}
