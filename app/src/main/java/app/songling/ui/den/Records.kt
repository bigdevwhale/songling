package app.songling.ui.den

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.songling.AppContainer
import app.songling.container
import app.songling.domain.Records
import app.songling.domain.Studio
import app.songling.domain.Words
import app.songling.ui.LocalRu
import app.songling.ui.Nav
import app.songling.ui.Overlay
import app.songling.ui.components.Btn
import app.songling.ui.components.Cover
import app.songling.ui.components.Kind
import app.songling.ui.components.t
import app.songling.ui.theme.C
import app.songling.ui.theme.R8
import app.songling.ui.theme.T

/** Puts a record of [album] in the collection for [price] plectrums; nothing if it's there already. */
fun buyRecord(co: AppContainer, album: String, price: Int) = co.write {
    settings.update { s -> if (album in s.records) s else s.copy(spent = s.spent + price, records = s.records + album) }
}

/**
 * The record collection, in the shop: the covers bought, newest first, and the albums heard that could join them,
 * the most listened first. It never fills up, so plectrums always have somewhere to go.
 */
@Composable
fun RecordsTab(st: Studio, nav: Nav) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    val mine = st.s.records.asReversed()
    Text(if (mine.isEmpty()) t("Пластинки альбомов, которые ты слушал. Каждая следующая чуть дороже, а прослушанный целиком альбом – вдвое дешевле.",
        "Records of albums you've heard. Each one costs a little more than the last; an album heard whole, half.")
        else t("В коллекции: ${mine.size}. Следующая – ${Records.price(mine.size, false)}, альбом, прослушанный целиком, – ${Records.price(mine.size, true)}.",
            "In the collection: ${mine.size}. The next costs ${Records.price(mine.size, false)}; an album heard whole, ${Records.price(mine.size, true)}."), style = T.Muted)
    if (mine.isNotEmpty()) {
        var all by remember { mutableStateOf(false) }
        // The covers as they stand in the crate, four in a row.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val side = (maxWidth - 8.dp * 3) / 4
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (row in mine.take(if (all) mine.size else 8).chunked(4)) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (k in row) {
                        val a = st.albums[k]
                        Box(Modifier.clip(R8.Sm).clickable { nav.go(Overlay.AlbumOf(k)) }) { Cover(a?.cover, st.albumTitle(k), a?.genre, side) }
                    }
                }
            }
        }
        if (mine.size > 8) Btn(if (all) t("Свернуть", "Less") else t("Вся коллекция", "The whole collection"), { all = !all }, Modifier.fillMaxWidth(), kind = Kind.GHOST, small = true)
    }
    val next = st.recordsToBuy
    if (next.isNotEmpty()) {
        var more by remember { mutableStateOf(false) }
        Text(t("Можно взять", "To get"), style = T.Strong, modifier = Modifier.padding(top = 4.dp))
        for (k in next.take(if (more) 40 else 6)) {
            val a = st.albums[k]
            val tl = st.byAlbum[k]
            val price = st.recordPrice(k)
            Row(Modifier.fillMaxWidth().clip(R8.Sm).clickable { nav.go(Overlay.AlbumOf(k)) }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Cover(a?.cover, st.albumTitle(k), a?.genre, 44.dp)
                Column(Modifier.weight(1f)) {
                    Text(st.albumTitle(k), style = T.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(listOfNotNull(a?.artistName ?: tl?.artistName, Words.dur(tl?.ms ?: 0, ru), if ((tl?.whole ?: 0) > 0) t("целиком – вдвое дешевле", "heard whole – half price") else null)
                        .joinToString(" · "), style = T.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Btn("$price", {
                    buyRecord(co, k, price)
                    nav.toast(if (ru) "«${st.albumTitle(k)}» – в коллекции" else "«${st.albumTitle(k)}» is in the collection")
                }, kind = Kind.AMBER, small = true, enabled = st.plectrums >= price)
            }
        }
        if (next.size > 6) Btn(if (more) t("Свернуть", "Less") else t("Показать ещё", "Show more"), { more = !more }, Modifier.fillMaxWidth(), kind = Kind.GHOST, small = true)
    }
    if (mine.isNotEmpty()) Text(t("Когда музыка не играет, на проигрывателе в логове – последняя купленная пластинка.",
        "When no music plays, the den's turntable has your newest record on it."), style = T.Small)
}
