package app.songling.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.songling.container
import app.songling.data.Settings
import app.songling.ui.LocalRu
import app.songling.ui.theme.C
import app.songling.ui.theme.R8
import app.songling.ui.theme.T
import app.songling.ui.theme.paper

/** The first-visit hints, one per screen. */
enum class Tip { DEN, SHELF, SAGA, GOALS, BESTIARY, SHOP, SETTINGS }

private fun text(tip: Tip, ru: Boolean): Pair<String, List<String>> = if (ru) when (tip) {
    Tip.DEN -> "Это логово" to listOf("Питомец слушает с тобой музыку из любого плеера и растёт от каждой минуты.",
        "Погладь его – нажми на комнату. Долгое нажатие – карточка питомца.", "Внизу – задания недели и быстрые действия.")
    Tip.SHELF -> "Это полка" to listOf("Всё, что ты слушал: альбомы, синглы, песни и артисты.",
        "«Хочу послушать» под поиском показывает то, до чего не доходили руки: послушаешь – получишь медиаторы.", "Поиск, порядок и фильтры – наверху.")
    Tip.SAGA -> "Это сага" to listOf("Статистика за неделю, месяц, год или всё время.", "«Фильтры» сужают её до жанра, плеера или наушников. Во вкладке «Календарь» – каждый день года, в «Истории» – каждое прослушивание.")
    Tip.GOALS -> "Это цели" to listOf("Задай себе: альбомы целиком, новых артистов, часы любимой группы, страны, десятилетия.",
        "Цели бывают на неделю, месяц, год или разовые – разовая приносит 15 медиаторов.")
    Tip.BESTIARY -> "Это бестиарий" to listOf("Твои питомцы и яйца, которые ещё предстоит найти.",
        "У каждого свой вкус: любимая музыка кормит в полтора раза и приносит вдвое больше медиаторов.", "Нажми на питомца, чтобы узнать о нём всё и позвать в логово.")
    Tip.SHOP -> "Здесь обустраивают логово" to listOf("Нажми на вещь – она появится в комнате на примерку. Понравится – купи за медиаторы.",
        "Купленное убирается и достаётся нажатием.")
    Tip.SETTINGS -> "Настройки" to listOf("Здесь меняется вид: пластинки или кассеты, светлая или тёмная тема, фон.",
        "И ещё: свои жанры, перенос истории из Spotify и Last.fm, резервные копии.")
} else when (tip) {
    Tip.DEN -> "This is the den" to listOf("Your pet listens with you to any player and grows with every minute.",
        "Tap the room to stroke it; long-press for its card.", "Below are this week's tasks and quick actions.")
    Tip.SHELF -> "This is the shelf" to listOf("Everything you've heard: albums, singles, songs and artists.",
        "«To hear» under the search shows what you never got round to – hearing it pays plectrums.", "Search, order and filters are at the top.")
    Tip.SAGA -> "This is the saga" to listOf("Stats for a week, a month, a year or all time.", "«Filters» narrow them to a genre, a player or headphones. «Calendar» shows every day of the year, «History» every listen.")
    Tip.GOALS -> "These are goals" to listOf("Set yourself whole albums, new artists, hours of a band, countries, decades.",
        "Goals run weekly, monthly, yearly or once – a one-off goal pays 15 plectrums.")
    Tip.BESTIARY -> "This is the bestiary" to listOf("Your pets, and the eggs still to find.",
        "Each has its taste: favourite music feeds it one and a half times and pays double plectrums.", "Tap a pet to learn all about it and call it to the den.")
    Tip.SHOP -> "Furnish the den here" to listOf("Tap a thing to try it in the room. Like it? Buy it with plectrums.", "Tap what you own to put it away or bring it back.")
    Tip.SETTINGS -> "Settings" to listOf("Change the look here: records or tapes, light or dark, the background.",
        "Also: your own genres, history from Spotify and Last.fm, backups.")
}

/** A paper note the first time a screen is opened; «Понятно» puts it away for good. */
@Composable
fun FirstVisit(tip: Tip, s: Settings, modifier: Modifier = Modifier) {
    if (tip.name in s.tips) return
    val ru = LocalRu.current
    val co = LocalContext.current.container
    val (title, lines) = text(tip, ru)
    Column(modifier.fillMaxWidth().paper(20.dp).clip(R8.Lg).background(C.amberSoft).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = T.Name)
        for (l in lines) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("•", style = T.Body.copy(color = C.amber))
            Text(l, style = T.Muted.copy(color = C.ink))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Text(t("Понятно", "Got it"), style = T.Button.copy(color = C.onAmber), modifier = Modifier.clip(CircleShape).background(C.amber)
                .clickable { co.write { settings.update { it.copy(tips = it.tips + tip.name) } } }.padding(horizontal = 16.dp, vertical = 8.dp))
        }
    }
}
