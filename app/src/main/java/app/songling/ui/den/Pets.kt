package app.songling.ui.den

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.songling.container
import app.songling.data.MIN
import app.songling.domain.Item
import app.songling.domain.Species
import app.songling.domain.genres
import app.songling.domain.Stage
import app.songling.domain.Studio
import app.songling.domain.Wall
import app.songling.domain.Words
import app.songling.ui.LocalRu
import app.songling.ui.Nav
import app.songling.ui.Overlay
import app.songling.ui.art.Look
import app.songling.ui.art.PetArt
import app.songling.ui.components.Bar
import app.songling.ui.components.Btn
import app.songling.ui.components.BufImage
import app.songling.ui.components.Card
import app.songling.ui.components.Field
import app.songling.ui.components.Flow
import app.songling.ui.components.Kind
import app.songling.ui.components.Page
import app.songling.ui.components.PetView
import app.songling.ui.components.Section
import app.songling.ui.components.Sheet
import app.songling.ui.components.t
import app.songling.ui.title
import app.songling.ui.theme.C
import app.songling.ui.theme.R8
import app.songling.ui.theme.T
import app.songling.ui.theme.paper
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PetSheet(st: Studio, nav: Nav) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    val p = st.pet ?: run { nav.close(); return }
    var name by remember(p.pet.id) { mutableStateOf(p.pet.name) }
    Sheet(onDismiss = { if (name != p.pet.name) co.write { db.dao().update(p.pet.copy(name = name.trim())) }; nav.close() }) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { PetView(st.look(false)!!.copy(windup = 0f, sleeping = false), Modifier.width(220.dp)) }
        Text(p.pet.title(ru), style = T.Title)
        Text(Words.species(p.species, ru) + (if (p.pet.shiny) t(" · редкий окрас", " · rare colouring") else ""), style = T.Muted)
        Field(name, { name = it.take(24) }, t("Имя", "Name"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            app.songling.ui.components.Stat(Words.stage(p.stage, ru), t("стадия", "stage"), Modifier.weight(1f))
            app.songling.ui.components.Stat(Words.dur(p.fed * MIN, ru), t("съедено музыки", "music eaten"), Modifier.weight(1f), C.lilac)
        }
        Bar(p.progress, Modifier.fillMaxWidth(), C.lilac)
        Card {
            Text(t("Любит", "Loves"), style = T.Eyebrow)
            Text(Words.loves(p.species, ru), style = T.Body)
            Text(t("Любимое идёт в зачёт в полтора раза. Уже съедено любимого: ${Words.dur(p.favMs, true)}.",
                "A favourite counts one and a half times. Favourites so far: ${Words.dur(p.favMs, false)}."), style = T.Small)
        }
        Card {
            Text(t("Характер", "Nature"), style = T.Eyebrow)
            Text(Words.nature(p.pet.nature, ru), style = T.Body)
            val f = p.form ?: p.leaning
            if (f != null) {
                Text(if (p.form != null) t("Форма", "Form") else t("Склоняется к форме", "Leaning to the form"), style = T.Eyebrow, modifier = Modifier.padding(top = 8.dp))
                Text(Words.form(f, ru), style = T.Body.copy(color = Color(f.argb)))
                if (p.form == null) Text(t("Форма выбирается навсегда, когда питомец вырастет, – по музыке, которой он питался.",
                    "The form is set for good when the pet grows up, by the music it was fed."), style = T.Small)
            }
            Text(t("Появился ", "Laid on ") + date(p.pet.laid, ru), style = T.Small, modifier = Modifier.padding(top = 8.dp))
        }
        if (p.byGenre.isNotEmpty()) {
            Section(t("Чем питался", "What it ate"))
            val total = p.byGenre.values.sum().coerceAtLeast(1)
            for ((g, ms) in p.byGenre.entries.sortedByDescending { it.value }.take(6)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(Words.genre(g, ru), style = T.Small, modifier = Modifier.width(110.dp))
                    Bar(ms / total.toFloat(), Modifier.weight(1f), C.genre(g))
                    Text(Words.dur(ms, ru), style = T.Small)
                }
            }
        }
        Btn(t("Все питомцы", "All pets"), { nav.go(Overlay.Bestiary) }, Modifier.fillMaxWidth(), kind = Kind.SOFT)
    }
}

fun date(t: Long, ru: Boolean): String = DateTimeFormatter.ofPattern(if (ru) "d MMMM yyyy" else "MMMM d, yyyy", if (ru) Locale("ru") else Locale.ENGLISH)
    .format(Instant.ofEpochMilli(t).atZone(java.time.ZoneId.systemDefault()))

/** Every pet: the ones raised, and the eggs, each with its tastes and gifts a tap away. */
@Composable
fun BestiaryPage(st: Studio, nav: Nav) {
    val ru = LocalRu.current
    var page by remember { mutableStateOf(if (st.pets.isEmpty()) 1 else 0) }
    var petOpen by remember { mutableStateOf<Long?>(null) }
    var eggOpen by remember { mutableStateOf<Species?>(null) }
    val open = st.hints.count { it.open }
    Page(t("Бестиарий", "Bestiary"), nav::close) {
        app.songling.ui.components.FirstVisit(app.songling.ui.components.Tip.BESTIARY, st.s)
        app.songling.ui.components.Segmented(listOf(t("Мои питомцы · ${st.pets.size}", "My pets · ${st.pets.size}"), t("Яйца · $open из ${st.hints.size}", "Eggs · $open of ${st.hints.size}")), page, { page = it })
        if (page == 0) {
            if (st.pets.isEmpty()) app.songling.ui.components.Empty(t("Пока никого", "No one yet"), t("Выбери яйцо – и оно вылупится через три часа музыки.", "Pick an egg, and it hatches after three hours of music.")) {
                Btn(t("К яйцам", "See the eggs"), { page = 1 }, kind = Kind.AMBER, small = true)
            }
            val sorted = st.pets.sortedWith(compareByDescending<app.songling.domain.PetState> { it.pet.id == st.pet?.pet?.id }.thenByDescending { it.fed })
            for (row in sorted.chunked(2)) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (p in row) PetTile(st, p, Modifier.weight(1f)) { petOpen = p.pet.id }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        } else {
            for (row in st.hints.sortedByDescending { it.open }.chunked(3)) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (h in row) EggTile(st, h, Modifier.weight(1f)) { eggOpen = h.species }
                repeat(3 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
    petOpen?.let { id -> st.pets.firstOrNull { it.pet.id == id }?.let { PetDetail(st, it, nav) { petOpen = null } } }
    eggOpen?.let { sp -> EggDetail(st, st.hints.first { it.species == sp }, nav) { eggOpen = null } }
}

@Composable
private fun PetTile(st: Studio, p: app.songling.domain.PetState, modifier: Modifier, onClick: () -> Unit) {
    val ru = LocalRu.current
    val out = p.pet.id == st.pet?.pet?.id
    Column(modifier.paper(20.dp, 3.dp).clip(RoundedCornerShape(20.dp)).background(C.surface).then(if (out) Modifier.border(2.dp, C.amber, RoundedCornerShape(20.dp)) else Modifier)
        .clickable(onClick = onClick).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.surface2), contentAlignment = Alignment.Center) {
            BufImage(PetArt.still(p.species, p.stage, p.pet.shiny, p.form), scale = 1.65.dp)
            if (out) Text(t("в логове", "in the den"), style = T.Small.copy(color = C.onAmber),
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp).clip(CircleShape).background(C.amber).padding(horizontal = 8.dp, vertical = 2.dp))
            if (p.pet.shiny) Text("✦", style = T.Strong.copy(color = C.honey), modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
        }
        Text(p.pet.title(ru), style = T.Name, maxLines = 1)
        Text(Words.species(p.species, ru) + " · " + Words.stage(p.stage, ru), style = T.Small, maxLines = 1)
        // What it loves, in its colours.
        val gs = p.species.genres.take(2)
        if (gs.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { for (g in gs) Dot(Words.genre(g, ru), C.genre(g)) }
        else Text(Words.loves(p.species, ru), style = T.Small.copy(color = C.muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Dot(text: String, color: Color) {
    Text(text, style = T.Small.copy(color = color, fontSize = T.Small.fontSize * .92f), maxLines = 1, modifier = Modifier.clip(CircleShape).background(color.copy(alpha = .13f)).padding(horizontal = 7.dp, vertical = 2.dp))
}

@Composable
private fun EggTile(st: Studio, h: app.songling.domain.Hint, modifier: Modifier, onClick: () -> Unit) {
    val ru = LocalRu.current
    val have = st.pets.any { it.species == h.species }
    Column(modifier.paper(18.dp, 3.dp).clip(RoundedCornerShape(18.dp)).background(C.surface).clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        BufImage(PetArt.still(h.species, Stage.EGG, dim = !h.open), scale = 0.95.dp)
        Text(if (h.open || have) Words.species(h.species, ru) else "???", style = T.Small.copy(color = C.ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (h.open) Text(if (have) t("уже есть", "you have one") else t("можно взять", "ready"), style = T.Small.copy(color = if (have) C.faint else C.mint))
        else Bar(h.have / h.need.toFloat(), Modifier.fillMaxWidth().padding(horizontal = 4.dp), C.lilac)
    }
}

/** Everything about one pet, and a way to call it out. */
@Composable
private fun PetDetail(st: Studio, p: app.songling.domain.PetState, nav: Nav, onClose: () -> Unit) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    val out = p.pet.id == st.pet?.pet?.id
    Sheet(onDismiss = onClose) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { PetView(Look(p.species, p.stage, p.pet.shiny, p.form), Modifier.width(200.dp)) }
        Text(p.pet.title(ru), style = T.Title)
        Text(Words.species(p.species, ru) + " · " + Words.stage(p.stage, ru) + (p.form?.let { " · " + Words.form(it, ru) } ?: "") + (if (p.pet.shiny) t(" · редкий окрас", " · rare colouring") else ""), style = T.Muted)
        Bar(p.progress, Modifier.fillMaxWidth(), C.lilac)
        Text(p.stage.next?.let { n -> t("до «${Words.stage(n, true)}» – ${Words.dur((n.from - p.fed).coerceAtLeast(0) * MIN, true)} музыки", "${Words.dur((n.from - p.fed).coerceAtLeast(0) * MIN, false)} of music to «${Words.stage(n, false)}»") }
            ?: t("Древнее не бывает.", "As old as they come."), style = T.Small)
        InfoBlock(t("Любит", "Loves")) {
            Text(Words.loves(p.species, ru).replaceFirstChar { it.uppercase() }, style = T.Body)
            if (p.species.genres.isNotEmpty()) app.songling.ui.components.Flow { for (g in p.species.genres) Dot(Words.genre(g, ru), C.genre(g)) }
        }
        InfoBlock(t("Способности", "Gifts")) {
            Gift("♪", Words.favouriteGift(ru))
            Gift("✦", t("Характер – ${Words.nature(p.pet.nature, true)}. ", "Nature: ${Words.nature(p.pet.nature, false)}. ") + Words.natureGift(p.pet.nature, ru))
            Gift("↑", Words.growth(p.species, ru))
        }
        InfoBlock(t("С тобой", "With you")) {
            Text(t("Съел ${Words.dur(p.fed * MIN, true)} музыки, из них любимого – ${Words.dur(p.favMs, true)}.", "Ate ${Words.dur(p.fed * MIN, false)} of music, ${Words.dur(p.favMs, false)} of it favourite."), style = T.Body)
            if (p.bonus > 0) Text(t("Принёс медиаторов: ${p.bonus}", "Plectrums it brought: ${p.bonus}"), style = T.Body.copy(color = C.amber))
            Text(t("Появился ", "Laid on ") + date(p.pet.laid, ru), style = T.Small)
            if (p.byGenre.isNotEmpty()) {
                val total = p.byGenre.values.sum().coerceAtLeast(1)
                for ((g, ms) in p.byGenre.entries.sortedByDescending { it.value }.take(4)) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(Words.genre(g, ru), style = T.Small, modifier = Modifier.width(96.dp), maxLines = 1)
                    Bar(ms / total.toFloat(), Modifier.weight(1f), C.genre(g))
                }
            }
        }
        if (out) Text(t("Сейчас в логове.", "In the den now."), style = T.Strong.copy(color = C.mint))
        else Btn(t("Позвать в логово", "Call it to the den"), {
            co.write { bringOut(p.pet.id) }
            nav.toast(if (ru) "${p.pet.title(true)} снова в логове" else "${p.pet.title(false)} is back in the den"); onClose()
        }, Modifier.fillMaxWidth(), kind = Kind.AMBER)
        Text(t("Остальные ждут в коробке и не растут, но ничего не забывают.", "The others wait in their box and don't grow, but forget nothing."), style = T.Small)
    }
}

/** An egg: what's in it, what it'll love, and how it's found. */
@Composable
private fun EggDetail(st: Studio, h: app.songling.domain.Hint, nav: Nav, onClose: () -> Unit) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    val sp = h.species
    Sheet(onDismiss = onClose) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (h.open) PetView(Look(sp, Stage.EGG), Modifier.width(160.dp)) else BufImage(PetArt.still(sp, Stage.EGG, dim = true), scale = 1.6.dp)
        }
        Text(if (h.open) t("Яйцо: ", "Egg: ") + Words.species(sp, ru) else t("Неизвестное яйцо", "An unknown egg"), style = T.Title)
        if (!h.open) {
            InfoBlock(t("Как найти", "How to find it")) {
                Text(Words.find(sp, h.asked, ru), style = T.Body)
                Bar(h.have / h.need.toFloat(), Modifier.fillMaxWidth(), C.lilac)
                Text(if (h.minutes) "${Words.dur(h.have.coerceAtMost(h.need) * MIN, ru)} / ${Words.dur(h.need * MIN, ru)}" else "${h.have.coerceAtMost(h.need)} / ${h.need}", style = T.Small)
            }
        }
        InfoBlock(t("Кто внутри будет любить", "What it will love")) {
            Text(Words.loves(sp, ru).replaceFirstChar { it.uppercase() }, style = T.Body)
            if (sp.genres.isNotEmpty()) app.songling.ui.components.Flow { for (g in sp.genres) Dot(Words.genre(g, ru), C.genre(g)) }
        }
        if (h.open) InfoBlock(t("Каким вырастет", "How it grows")) { Text(Words.growth(sp, ru), style = T.Body) }
        if (h.open) {
            if (st.pet != null) Text(t("${st.pet!!.pet.title(true)} уйдёт в коробку и подождёт там – его можно позвать обратно.",
                "${st.pet!!.pet.title(false)} goes into a box to wait; you can call them back any time."), style = T.Small)
            // The first of a kind is free; another of a kind already raised costs plectrums.
            val price = if (st.pets.any { it.species == sp }) app.songling.domain.EGG_AGAIN else 0
            Btn(t("Положить яйцо в логово", "Put the egg in the den") + if (price > 0) " · $price" else "", {
                co.write { if (price > 0) settings.update { it.copy(spent = it.spent + price) }; layEgg(sp) }; onClose(); nav.close()
            }, Modifier.fillMaxWidth(), kind = Kind.AMBER, enabled = st.plectrums >= price)
            Text(if (price > 0) t("Такой питомец у тебя уже есть: второе яйцо – за $price медиаторов (у тебя ${st.plectrums}). Вылупится через три часа музыки.",
                "You have one of these already: another egg costs $price plectrums (you have ${st.plectrums}). Hatches after three hours of music.")
                else t("Первое яйцо каждого вида – даром. Вылупится через три часа музыки.", "The first egg of each kind is free. Hatches after three hours of music."), style = T.Small)
        }
    }
}

@Composable
private fun InfoBlock(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.surface2).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title.uppercase(), style = T.Eyebrow.copy(color = C.coral))
        content()
    }
}

@Composable
private fun Gift(mark: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(mark, style = T.Strong.copy(color = C.amber), modifier = Modifier.width(16.dp))
        Text(text, style = T.Body, modifier = Modifier.weight(1f))
    }
}

/** Furniture and wallpaper, bought with plectrums. Tapping a thing tries it in the den above. */
@Composable
fun ShopPage(st: Studio, nav: Nav) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    var page by remember { mutableStateOf(0) }
    var tryItem by remember { mutableStateOf<Item?>(null) }
    // The place opened to choose what stands in it.
    var place by remember { mutableStateOf<Item?>(null) }
    var tryWall by remember { mutableStateOf<Wall?>(null) }
    val owned = Item.entries.filter { it.name in st.s.bought && it.name !in st.s.hidden }.toSet()
    val wall = Wall.entries.firstOrNull { it.name == st.s.wall } ?: Wall.PLUM
    val zoned = java.time.Instant.ofEpochMilli(st.now).atZone(st.zone)
    val ctx = LocalContext.current
    val pictures by androidx.compose.runtime.produceState<List<app.songling.ui.art.Buf?>>(emptyList(), st.s.posters, st.s.bought, st.albums.values.count { it.cover != null }) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { Posters.pictures(ctx, st) }.getOrDefault(emptyList()) }
    }
    val posterFiles by androidx.compose.runtime.produceState<List<String?>>(emptyList(), st.s.posters, st.s.bought, st.albums.values.count { it.cover != null }) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { Posters.files(ctx, st) }
    }
    var picking by remember { mutableStateOf<Int?>(null) }
    val den = app.songling.ui.art.DenLook(
        posters = pictures,
        posterFiles = posterFiles,
        wall = tryWall ?: wall,
        items = if (tryItem != null) owned + tryItem!! else owned,
        hour = zoned.hour, minute = zoned.minute, playing = true,
        spines = st.wholeRuns.map { it.first }.distinct().takeLast(42).map { k -> st.albums[k]?.genre?.argb?.toInt() ?: (0xFF8A7A6A).toInt() },
        pet = st.look(false),
        outside = if (st.s.realWeather) app.songling.data.Outside.of(st.s.outside) else null, now = st.now,
    )
    // Choosing what stands in a place happens under the room, so the room is seen while choosing.
    val scroll = rememberScrollState()
    androidx.compose.runtime.LaunchedEffect(place) { if (place != null) scroll.animateScrollTo(0) }
    Page(t("Обустроить логово", "Furnish the den"), nav::close, scroll) {
        app.songling.ui.components.FirstVisit(app.songling.ui.components.Tip.SHOP, st.s)
        Box(Modifier.fillMaxWidth().paper(24.dp, 4.dp).clip(RoundedCornerShape(24.dp)).background(C.surface).padding(6.dp).clip(RoundedCornerShape(18.dp))) {
            app.songling.ui.components.DenView(den)
            Text(t("медиаторов: ${st.plectrums}", "plectrums: ${st.plectrums}"), style = T.Small.copy(color = C.onAmber),
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(CircleShape).background(C.amber).padding(horizontal = 10.dp, vertical = 4.dp))
        }
        // What's being tried, and what can be done with it.
        val ti = tryItem; val tw = tryWall
        when {
            ti != null -> TryBar(ti.name(ru), ti.note(ru), ti.name in st.s.bought, ti.price, st.plectrums, onBuy = {
                // What stood in its place is put away, not sold: it can come back.
                co.write { settings.update { s -> s.copy(spent = s.spent + ti.price, bought = s.bought + ti.name, hidden = s.hidden - ti.name + Item.others(ti, s.bought)) } }
                nav.toast(if (ru) "«${ti.name(true)}» – теперь в логове" else "«${ti.name(false)}» is in the den now"); tryItem = null
            }, onCancel = { tryItem = null })
            tw != null -> TryBar(t("Обои «", "Wallpaper «") + tw.name(ru) + "»", "", tw.price == 0 || "wall:${tw.name}" in st.s.bought, tw.price, st.plectrums, onBuy = {
                co.write { settings.update { s -> if (tw.price == 0 || "wall:${tw.name}" in s.bought) s.copy(wall = tw.name) else s.copy(wall = tw.name, spent = s.spent + tw.price, bought = s.bought + "wall:${tw.name}") } }
                tryWall = null
            }, onCancel = { tryWall = null }, haveLabel = t("Поклеить", "Put up"))
            place != null -> {}
            else -> {
                // Each pet has a room of its own (#26): say whose this one is.
                st.pet?.let { p -> Text(t("Это комната питомца ${p.pet.title(true)} – у каждого питомца своя.", "This is ${p.pet.title(false)}'s room – each pet has its own."), style = T.Strong) }
                Text(t("Нажми на вещь – она появится в комнате. Понравится – купи.", "Tap a thing to try it in the room. Like it? Buy it."), style = T.Muted)
            }
        }
        place?.let { p ->
            val placed = Item.placed(owned)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t("Что поставить на место «", "What goes where the «") + p.name(ru) + t("»", "» stands"), style = T.Strong, modifier = Modifier.weight(1f))
                Btn(t("Готово", "Done"), { place = null; tryItem = null }, kind = Kind.GHOST, small = true)
            }
            Choices(st, p, placed[p], tryItem, wall, onTry = { v -> tryItem = if (tryItem == v) null else v }, onPlace = { v ->
                tryItem = null
                co.write { settings.update { s -> s.copy(hidden = if (v == null) s.hidden + Item.variants(p).map { it.name } else s.hidden - v.name + Item.others(v, s.bought)) } }
            })
        }
        if (Item.POSTER.name in st.s.bought && Item.POSTER.name !in st.s.hidden) {
            Text(t("Постеры на стене", "Posters on the wall"), style = T.Strong)
            PosterFrames(st, pictures) { picking = it }
        }
        app.songling.ui.components.Tabs(listOf(t("Вещи", "Things"), t("Обои", "Wallpaper"), t("Пластинки", "Records")), page, { page = it; tryItem = null; tryWall = null; place = null })
        if (page == 2) RecordsTab(st, nav)
        else if (page == 0) {
            // One tile a place; a place with things to choose from opens a row of them under the room.
            val placed = Item.placed(owned)
            for (row in Item.slots.sortedBy { it.price }.chunked(3)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (it in row) {
                        val choice = Item.variants(it).size > 1
                        val inPlace = placed[it]
                        val have = Item.variants(it).any { v -> v.name in st.s.bought }
                        val on = if (choice) place == it else tryItem == it
                        val shows = inPlace ?: tryItem?.takeIf { v -> v.slot == it } ?: it
                        Column(Modifier.weight(1f).paper(18.dp, 3.dp).clip(RoundedCornerShape(18.dp)).background(C.surface)
                            .then(if (on) Modifier.border(2.dp, C.amber, RoundedCornerShape(18.dp)) else Modifier)
                            .clickable {
                                when {
                                    choice -> { place = if (on) null else it; tryItem = null }
                                    have -> co.write { settings.update { s -> s.copy(hidden = if (inPlace != null) s.hidden + it.name else s.hidden - it.name) } }
                                    else -> tryItem = if (on) null else it
                                }
                            }.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Thumb(shows, wall, 64.dp)
                            Text(shows.name(ru), style = T.Small.copy(color = C.ink), maxLines = 2, minLines = 2, textAlign = TextAlign.Center)
                            val from = Item.variants(it).filter { v -> v.name !in st.s.bought }.minOfOrNull { v -> v.price }
                            Text(when { inPlace != null -> t("в комнате", "in the room"); have -> t("убрано", "put away"); choice -> t("от $from", "from $from"); else -> "${it.price}" },
                                style = T.Small.copy(color = when { inPlace != null -> C.mint; have -> C.faint; st.plectrums < (from ?: it.price) -> C.faint; else -> C.amber }))
                        }
                    }
                    repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                }
            }
            Text(t("У многих мест есть из чего выбрать: нажми на место и выбери, что там будет стоять. Купленное не пропадает, его можно вернуть.",
                "Many places have things to choose from: tap a place and pick what stands there. What you have bought stays yours to bring back."), style = T.Small)
        } else {
            for (row in Wall.entries.chunked(4)) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (w in row) {
                    val have = w.price == 0 || "wall:${w.name}" in st.s.bought
                    val on = (tryWall ?: wall) == w
                    Column(Modifier.weight(1f).paper(16.dp, 3.dp).clip(RoundedCornerShape(16.dp)).background(C.surface)
                        .then(if (on) Modifier.border(2.dp, C.amber, RoundedCornerShape(16.dp)) else Modifier)
                        .clickable { tryWall = if (w == wall) null else w }.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(10.dp))) {
                            for (k in 0 until 6) Box(Modifier.weight(1f).height(44.dp).background(Color(if (k % 2 == 0) w.a else w.b)))
                        }
                        Text(w.name(ru), style = T.Small.copy(color = C.ink), maxLines = 1)
                        Text(if (w == wall) t("сейчас", "now") else if (have) t("есть", "yours") else "${w.price}", style = T.Small.copy(color = if (have) C.faint else C.amber))
                    }
                }
            }
        }
        Text(t("Медиатор – за каждые 10 минут музыки, пять – за альбом целиком, ещё – за задания недели и цели. Покупки не отнимают рост у питомца. Когда комната обставлена, медиаторы идут на пластинки и новые яйца.",
            "One plectrum per 10 minutes of music, five per whole album, more for the week's tasks and goals. Buying never takes growth from the pet. Once the room is furnished, plectrums go on records and new eggs."), style = T.Small)
    }
    picking?.let { f -> PosterPick(st, f) { picking = null } }
}

/** A thing as it looks in the den, on a tile [height] high. */
@Composable
private fun Thumb(i: Item, wall: Wall, height: androidx.compose.ui.unit.Dp) {
    val thumb = remember(i, wall) { app.songling.ui.art.DenArt.thumb(i, wall) }
    Box(Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(12.dp)).background(C.surface2), contentAlignment = Alignment.Center) {
        val room = height.value - 8f
        val k = minOf(room / thumb.w, room / thumb.h)
        BufImage(thumb, scale = k.dp)
    }
}

/**
 * What can stand in [place]: what is bought is put there with a tap, the rest is tried in the room first; «Пусто»
 * puts away whatever stands there.
 */
@Composable
private fun Choices(st: Studio, place: Item, now: Item?, trying: Item?, wall: Wall, onTry: (Item) -> Unit, onPlace: (Item?) -> Unit) {
    val ru = LocalRu.current
    Row(Modifier.fillMaxWidth().horizontalScroll(remember(place) { androidx.compose.foundation.ScrollState(0) }).padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (v in Item.variants(place)) {
            val have = v.name in st.s.bought
            val on = v == now || v == trying
            Column(Modifier.width(108.dp).clip(RoundedCornerShape(16.dp)).background(C.surface)
                .border(if (on) 2.dp else 1.dp, if (on) C.amber else C.line, RoundedCornerShape(16.dp))
                .clickable { if (have) onPlace(v) else onTry(v) }.padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Thumb(v, wall, 52.dp)
                Text(v.name(ru), style = T.Small.copy(color = C.ink), maxLines = 2, minLines = 2, textAlign = TextAlign.Center)
                Text(when { v == now -> t("стоит", "here"); have -> t("поставить", "place"); else -> "${v.price}" },
                    style = T.Small.copy(color = when { v == now -> C.mint; have -> C.ink; st.plectrums < v.price -> C.faint; else -> C.amber }))
            }
        }
        if (now != null) Column(Modifier.width(108.dp).height(118.dp).clip(RoundedCornerShape(16.dp)).border(1.dp, C.line, RoundedCornerShape(16.dp))
            .clickable { onPlace(null) }.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(t("Пусто", "Empty"), style = T.Small.copy(color = C.ink))
            Text(t("убрать", "put away"), style = T.Small.copy(color = C.faint))
        }
    }
}

@Composable
private fun TryBar(title: String, note: String, have: Boolean, price: Int, purse: Int, onBuy: () -> Unit, onCancel: () -> Unit, haveLabel: String = "") {
    Row(Modifier.fillMaxWidth().paper(20.dp).clip(RoundedCornerShape(20.dp)).background(C.surface).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = T.Strong)
            if (note.isNotEmpty()) Text(note, style = T.Small)
            else if (!have && purse < price) Text(t("не хватает ${price - purse}", "${price - purse} short"), style = T.Small.copy(color = C.coral))
        }
        Btn(t("Отмена", "Cancel"), onCancel, kind = Kind.GHOST, small = true)
        Btn(if (have) haveLabel.ifEmpty { t("Поставить", "Place") } else t("Купить · $price", "Buy · $price"), onBuy, kind = Kind.AMBER, small = true, enabled = have || purse >= price)
    }
}

/** The moment an egg hatches. */
@Composable
fun HatchSheet(st: Studio, id: Long, nav: Nav) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    val p = st.pets.firstOrNull { it.pet.id == id } ?: run { nav.close(); return }
    var name by remember { mutableStateOf("") }
    Sheet(onDismiss = { if (name.isNotBlank()) co.write { db.dao().update(p.pet.copy(name = name.trim())) }; nav.close() }) {
        Text(t("Вылупился!", "It hatched!"), style = T.Display.copy(color = C.amber))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { PetView(Look(p.species, p.stage, p.pet.shiny, mood = app.songling.domain.Mood.HEARTS, happy = true), Modifier.size(240.dp)) }
        Text(Words.species(p.species, ru) + if (p.pet.shiny) t(" – и редкого окраса!", " – and in a rare colouring!") else "", style = T.Name)
        Text(t("Характер: ${Words.nature(p.pet.nature, true)}. Любит ${Words.loves(p.species, true)}.", "Nature: ${Words.nature(p.pet.nature, false)}. Loves ${Words.loves(p.species, false)}."), style = T.Muted)
        Field(name, { name = it.take(24) }, Words.defaultName(p.species, ru))
        Btn(t("Привет!", "Hello!"), { if (name.isNotBlank()) co.write { db.dao().update(p.pet.copy(name = name.trim())) }; nav.close() }, Modifier.fillMaxWidth(), kind = Kind.AMBER)
    }
}
