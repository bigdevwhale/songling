package app.songling.domain

import app.songling.data.GoalKind
import app.songling.data.Kind
import app.songling.data.Period
import app.songling.data.Source
import java.util.Locale
import kotlin.random.Random

/** Everything the app says, in Russian and English. */
object Words {
    fun t(ru: Boolean, r: String, e: String) = if (ru) r else e

    fun species(sp: Species, ru: Boolean): String = when (sp) {
        Species.LYREBIRD -> t(ru, "Лирохвост", "Lyrebird")
        Species.MUSICBOX -> t(ru, "Шкатулка", "Music box")
        Species.LONGEAR -> t(ru, "Ушан", "Longear")
        Species.BASILISK -> t(ru, "Басилиск", "Bassilisk")
        Species.CONDUCTOR -> t(ru, "Филин-дирижёр", "Owl conductor")
        Species.PURR -> t(ru, "Мурлыка", "Purr")
        Species.AXOLOTL -> t(ru, "Аксолотль", "Axolotl")
        Species.RACCOON -> t(ru, "Енот", "Raccoon")
        Species.WANDERER -> t(ru, "Ёж-странник", "Wandering hedgehog")
        Species.CRICKET -> t(ru, "Сверчок", "Cricket")
        Species.LARK -> t(ru, "Жаворонок", "Lark")
        Species.SNAIL -> t(ru, "Улитка", "Vinyl snail")
        Species.PARROT -> t(ru, "Попугай", "Parrot")
        Species.TORTOISE -> t(ru, "Черепаха", "Tortoise")
        Species.RAVEN -> t(ru, "Ворон", "Raven")
        Species.WHALE -> t(ru, "Кит", "Whale")
        Species.ECHO -> t(ru, "Эхо", "Echo")
        Species.FIREFLY -> t(ru, "Светлячок", "Firefly")
        Species.POTPOURRI -> t(ru, "Попурри", "Potpourri")
        Species.HUMMINGBIRD -> t(ru, "Колибри", "Hummingbird")
        Species.MAGPIE -> t(ru, "Сорока", "Magpie")
        Species.BULLFINCH -> t(ru, "Снегирь", "Bullfinch")
        Species.BAT -> t(ru, "Летучая мышь", "Bat")
        Species.DRAGON -> t(ru, "Дракончик", "Little dragon")
        Species.HOUND -> t(ru, "Пёс", "Hound")
    }

    /** What the pet loves to eat most. */
    fun loves(sp: Species, ru: Boolean): String = when (sp) {
        Species.LYREBIRD -> t(ru, "новых артистов", "new artists")
        Species.MUSICBOX -> t(ru, "альбомы целиком, по порядку", "whole albums, in order")
        Species.LONGEAR -> t(ru, "музыку в наушниках", "music on headphones")
        Species.BASILISK -> t(ru, "рок, метал и панк", "rock, metal and punk")
        Species.CONDUCTOR -> t(ru, "классику и саундтреки", "classical and soundtracks")
        Species.PURR -> t(ru, "джаз, блюз и соул", "jazz, blues and soul")
        Species.AXOLOTL -> t(ru, "электронику", "electronic music")
        Species.RACCOON -> t(ru, "хип-хоп", "hip-hop")
        Species.WANDERER -> t(ru, "фолк и музыку разных стран", "folk and music from everywhere")
        Species.CRICKET -> t(ru, "музыку после десяти вечера", "music after ten at night")
        Species.LARK -> t(ru, "музыку до десяти утра", "music before ten in the morning")
        Species.SNAIL -> t(ru, "долгие сессии, от 45 минут", "long sittings, 45 minutes and more")
        Species.PARROT -> t(ru, "песни на повторе", "songs on repeat")
        Species.TORTOISE -> t(ru, "музыку старше тридцати лет", "music over thirty years old")
        Species.RAVEN -> t(ru, "песни тех, кого уже нет", "songs by those who are gone")
        Species.WHALE -> t(ru, "эмбиент, lo-fi и тишину", "ambient, lo-fi and quiet things")
        Species.ECHO -> t(ru, "возвращения после тишины", "coming back after a silence")
        Species.FIREFLY -> t(ru, "живые концерты", "live concerts")
        Species.POTPOURRI -> t(ru, "дни, где намешано много жанров", "days with many genres mixed")
        Species.HUMMINGBIRD -> t(ru, "поп-музыку", "pop music")
        Species.MAGPIE -> t(ru, "песни двух и более артистов", "songs by two artists or more")
        Species.BULLFINCH -> t(ru, "музыку зимой и новогодние песни", "music in winter and Christmas songs")
        Species.BAT -> t(ru, "пластинки и радио", "records and the radio")
        Species.DRAGON -> t(ru, "песни длиннее восьми минут", "songs over eight minutes long")
        Species.HOUND -> t(ru, "твоего самого любимого артиста", "your most played artist")
    }

    /** How the egg is found. */
    fun find(sp: Species, need: Int, ru: Boolean): String = when (sp) {
        Species.LYREBIRD, Species.MUSICBOX, Species.LONGEAR -> t(ru, "Есть с самого начала", "There from the start")
        Species.BASILISK -> t(ru, "$need часов рока, метала или панка", "$need hours of rock, metal or punk")
        Species.CONDUCTOR -> t(ru, "$need часов классики или саундтреков", "$need hours of classical or soundtracks")
        Species.PURR -> t(ru, "$need часов джаза, блюза или соула", "$need hours of jazz, blues or soul")
        Species.AXOLOTL -> t(ru, "$need часов электроники", "$need hours of electronic music")
        Species.RACCOON -> t(ru, "$need часов хип-хопа", "$need hours of hip-hop")
        Species.WHALE -> t(ru, "$need часов эмбиента или lo-fi", "$need hours of ambient or lo-fi")
        Species.WANDERER -> t(ru, "Артисты из $need стран", "Artists from $need countries")
        Species.CRICKET -> t(ru, "$need вечеров с музыкой после десяти", "$need nights of music after ten")
        Species.LARK -> t(ru, "$need утр с музыкой до десяти", "$need mornings of music before ten")
        Species.SNAIL -> t(ru, "$need сессии от 45 минут без перерыва", "$need sittings of 45 minutes on end")
        Species.PARROT -> t(ru, "Одна песня $need раз", "One song $need times")
        Species.TORTOISE -> t(ru, "$need альбомов старше тридцати лет", "$need albums over thirty years old")
        Species.RAVEN -> t(ru, "$need ч музыки ушедших артистов", "$need h of music by artists who are gone")
        Species.ECHO -> t(ru, "Вернуться к музыке после недели тишины", "Come back to music after a silent week")
        Species.FIREFLY -> t(ru, "Отметить живой концерт", "Log a live concert")
        Species.POTPOURRI -> t(ru, "$need жанра за один день", "$need genres in one day")
        Species.HUMMINGBIRD -> t(ru, "$need часов поп-музыки", "$need hours of pop music")
        Species.MAGPIE -> t(ru, "$need песен двух и более артистов", "$need songs by two artists or more")
        Species.BULLFINCH -> t(ru, "$need зимних дней с музыкой", "$need winter days with music")
        Species.BAT -> t(ru, "$need песен с пластинки или по радио", "$need songs off a record or the radio")
        Species.DRAGON -> t(ru, "$need песен длиннее восьми минут до конца", "$need songs over eight minutes, to the end")
        Species.HOUND -> t(ru, "$need часов одного артиста", "$need hours of one artist")
    }

    fun stage(s: Stage, ru: Boolean) = when (s) {
        Stage.EGG -> t(ru, "яйцо", "egg")
        Stage.BABY -> t(ru, "малыш", "baby")
        Stage.YOUNG -> t(ru, "подросток", "youngster")
        Stage.ADULT -> t(ru, "взрослый", "grown")
        Stage.ANCIENT -> t(ru, "древний", "ancient")
    }

    fun nature(n: Nature, ru: Boolean) = when (n) {
        Nature.CALM -> t(ru, "спокойный", "calm")
        Nature.SLEEPY -> t(ru, "соня", "sleepy")
        Nature.DANCER -> t(ru, "танцор", "a dancer")
        Nature.CURIOUS -> t(ru, "любопытный", "curious")
        Nature.SHY -> t(ru, "стеснительный", "shy")
        Nature.CHATTY -> t(ru, "болтун", "chatty")
        Nature.GREEDY -> t(ru, "обжора", "greedy")
    }

    /** What a nature does, in a few words. */
    fun natureGift(n: Nature, ru: Boolean) = when (n) {
        Nature.CALM -> t(ru, "Ничем не удивишь. Растёт ровно.", "Hard to surprise. Grows steadily.")
        Nature.SLEEPY -> t(ru, "Засыпает раньше и спит крепче.", "Goes to sleep early and sleeps soundly.")
        Nature.DANCER -> t(ru, "Пускается в пляс от любой песни.", "Starts dancing at the first beat.")
        Nature.CURIOUS -> t(ru, "Медиатор за каждого нового артиста, пока он в логове.", "A plectrum for every new artist while it's in the den.")
        Nature.SHY -> t(ru, "Не любит внимания, но гладить себя даёт.", "Keeps to itself, but loves being stroked.")
        Nature.CHATTY -> t(ru, "Заговаривает с тобой чаще остальных.", "Speaks up more often than the others.")
        Nature.GREEDY -> t(ru, "Ест на десятую часть больше – растёт быстрее.", "Eats a tenth more of everything and grows faster.")
    }

    /** What every pet does with its favourite music. */
    fun favouriteGift(ru: Boolean) = t(ru, "Любимое кормит в полтора раза и приносит вдвое больше медиаторов.", "Its favourite music feeds it one and a half times and pays double plectrums.")

    /** How it changes as it grows. */
    fun growth(sp: Species, ru: Boolean): String = when (sp) {
        Species.LYREBIRD -> t(ru, "Хвост превращается в лиру со струнами, у древнего – золотые кончики.", "Its tail becomes a lyre with strings; golden tips when ancient.")
        Species.MUSICBOX -> t(ru, "Внутри появляется валик с штырьками, у древней – балерина на крышке.", "A pinned cylinder appears inside; a ballerina on the lid when ancient.")
        Species.LONGEAR -> t(ru, "Уши складываются в наушники, у древнего они светятся.", "Its ears become headphones; they glow when ancient.")
        Species.BASILISK -> t(ru, "Рога-грифы, ирокез, у древнего – броня из усилителей.", "Headstock horns, a mohawk; amplifier armour when ancient.")
        Species.CONDUCTOR -> t(ru, "Палочка, бабочка, у древнего – фрак и седые брови.", "A baton, a bow tie; a tailcoat and white brows when ancient.")
        Species.PURR -> t(ru, "Хвост-саксофон, шляпа, у древнего – бархатный жилет.", "A saxophone tail, a hat; a velvet waistcoat when ancient.")
        Species.AXOLOTL -> t(ru, "Огоньков на жабрах всё больше, у древнего – клавиши на спине.", "More lights on its gills; keys along its back when ancient.")
        Species.RACCOON -> t(ru, "Кепка, цепь, у древнего – бумбокс.", "A cap, a chain; a boombox when ancient.")
        Species.WANDERER -> t(ru, "Лоскутный плащ, у древнего – посох-флейта.", "A patchwork cloak; a flute staff when ancient.")
        Species.CRICKET -> t(ru, "Звёзды на надкрыльях, у древнего – месяц.", "Stars on its wings; a crescent moon when ancient.")
        Species.LARK -> t(ru, "Хохолок-камертон, у древнего – венок.", "A tuning-fork crest; a flower crown when ancient.")
        Species.SNAIL -> t(ru, "Раковина-пластинка, у древней – золотая.", "A record for a shell; a golden one when ancient.")
        Species.PARROT -> t(ru, "Хохолок-повтор, ноты на крыльях, у древнего – поёт.", "A repeat-sign crest, notes on its wings; sings when ancient.")
        Species.TORTOISE -> t(ru, "Панцирь из этикеток разных лет, у древней – граммофон.", "A shell of labels from every decade; a gramophone when ancient.")
        Species.RAVEN -> t(ru, "Шарф и ключик, потом фонарь, у древнего – цилиндр с цветком.", "A scarf and a key, then a lantern; a top hat with a flower when ancient.")
        Species.WHALE -> t(ru, "Поёт нотами, у древнего – звёзды на спине.", "Sings in notes; stars on its back when ancient.")
        Species.ECHO -> t(ru, "Отголосков всё больше, у древнего – звенящий нимб.", "More echoes; a ringing halo when ancient.")
        Species.FIREFLY -> t(ru, "Огонёк-зажигалка, браслет фестиваля, у древнего – искры.", "A lighter glow, a festival wristband; sparks when ancient.")
        Species.POTPOURRI -> t(ru, "Лоскутов всё больше, у древнего – бант.", "More patches; a bow when ancient.")
        Species.HUMMINGBIRD -> t(ru, "Клюв-микрофон, блёстки на крыльях, у древнего – корона со звездой.", "A microphone beak, glitter on its wings; a star crown when ancient.")
        Species.MAGPIE -> t(ru, "Две ноты в хвосте, блестящие значки, у древней – ожерелье из медиаторов.", "Two notes in its tail, shiny badges; a necklace of plectrums when ancient.")
        Species.BULLFINCH -> t(ru, "Вязаная шапка, бубенчик, у древнего – шарф и снежинки на крыльях.", "A knitted hat, a little bell; a scarf and snowflakes on its wings when ancient.")
        Species.BAT -> t(ru, "Уши-антенны, крылья как шкала приёмника, у древней – светится радиолампа.", "Antenna ears, wings like a radio dial; a glowing valve when ancient.")
        Species.DRAGON -> t(ru, "Гребень-клавиши, хвост-свиток с нотами, у древнего – крылья из органных труб.", "A crest of keys, a tail like a scroll of notes; organ-pipe wings when ancient.")
        Species.HOUND -> t(ru, "Ошейник с пластинкой, уши-наушники, у древнего – медаль и седая морда.", "A collar with a record tag, headphone ears; a medal and a grey muzzle when ancient.")
    }

    fun form(f: Form, ru: Boolean) = when (f) {
        Form.STORM -> t(ru, "Гроза", "Storm")
        Form.NEON -> t(ru, "Неон", "Neon")
        Form.VELVET -> t(ru, "Бархат", "Velvet")
        Form.CHOIR -> t(ru, "Хор", "Choir")
        Form.STREET -> t(ru, "Улица", "Street")
        Form.ROOTS -> t(ru, "Корни", "Roots")
    }

    fun genre(g: Genre, ru: Boolean) = when (g) {
        Genre.ROCK -> t(ru, "рок", "rock")
        Genre.METAL -> t(ru, "метал", "metal")
        Genre.PUNK -> t(ru, "панк", "punk")
        Genre.ALT -> t(ru, "альтернатива", "alternative")
        Genre.POP -> t(ru, "поп", "pop")
        Genre.HIPHOP -> t(ru, "хип-хоп", "hip-hop")
        Genre.RNB -> t(ru, "R&B и соул", "R&B and soul")
        Genre.JAZZ -> t(ru, "джаз", "jazz")
        Genre.BLUES -> t(ru, "блюз", "blues")
        Genre.ELECTRONIC -> t(ru, "электроника", "electronic")
        Genre.DANCE -> t(ru, "танцевальная", "dance")
        Genre.AMBIENT -> t(ru, "эмбиент", "ambient")
        Genre.CLASSICAL -> t(ru, "классика", "classical")
        Genre.SOUNDTRACK -> t(ru, "саундтреки", "soundtracks")
        Genre.FOLK -> t(ru, "фолк", "folk")
        Genre.COUNTRY -> t(ru, "кантри", "country")
        Genre.WORLD -> t(ru, "музыка мира", "world")
        Genre.LATIN -> t(ru, "латино", "latin")
        Genre.REGGAE -> t(ru, "регги", "reggae")
        Genre.CHANSON -> t(ru, "шансон и эстрада", "chanson")
        Genre.KIDS -> t(ru, "детская", "kids")
        Genre.OTHER -> t(ru, "другое", "other")
    }

    fun kind(k: Kind, ru: Boolean) = when (k) {
        Kind.ALBUM -> t(ru, "альбом", "album")
        Kind.EP -> "EP"
        Kind.SINGLE -> t(ru, "сингл", "single")
    }

    fun source(s: Source, ru: Boolean) = when (s) {
        Source.PLAYER -> t(ru, "плеер", "player")
        Source.MANUAL -> t(ru, "вручную", "by hand")
        Source.VINYL -> t(ru, "пластинка", "vinyl")
        Source.RADIO -> t(ru, "радио", "radio")
        Source.CONCERT -> t(ru, "концерт", "concert")
        Source.IMPORT -> t(ru, "импорт", "import")
    }

    fun period(p: Period, ru: Boolean) = when (p) {
        Period.WEEK -> t(ru, "за неделю", "a week")
        Period.MONTH -> t(ru, "за месяц", "a month")
        Period.YEAR -> t(ru, "за год", "a year")
        Period.ONCE -> t(ru, "один раз", "once")
    }

    fun span(s: Span, ru: Boolean) = when (s) {
        Span.WEEK -> t(ru, "7 дней", "7 days")
        Span.MONTH -> t(ru, "30 дней", "30 days")
        Span.YEAR -> t(ru, "этот год", "this year")
        Span.LAST_YEAR -> t(ru, "прошлый год", "last year")
        Span.ALL -> t(ru, "всё время", "all time")
    }

    fun goalKind(k: GoalKind, ru: Boolean) = when (k) {
        GoalKind.MINUTES -> t(ru, "Часы музыки", "Hours of music")
        GoalKind.ALBUMS -> t(ru, "Альбомы целиком", "Whole albums")
        GoalKind.NEW_ARTISTS -> t(ru, "Новые артисты", "New artists")
        GoalKind.NEW_TRACKS -> t(ru, "Новые песни", "New songs")
        GoalKind.ARTIST -> t(ru, "Часы одного артиста", "Hours of one artist")
        GoalKind.ALBUM -> t(ru, "Альбом целиком", "An album, whole")
        GoalKind.TRACK -> t(ru, "Песня", "A song")
        GoalKind.GENRE -> t(ru, "Часы жанра", "Hours of a genre")
        GoalKind.GENRES -> t(ru, "Разные жанры", "Different genres")
        GoalKind.COUNTRIES -> t(ru, "Страны", "Countries")
        GoalKind.DECADES -> t(ru, "Десятилетия", "Decades")
        GoalKind.DAYS -> t(ru, "Дни с музыкой", "Days with music")
        GoalKind.WISHES -> t(ru, "Из списка «послушать»", "From the «to hear» list")
        GoalKind.PLAYS -> t(ru, "Прослушивания", "Listens")
        GoalKind.NEW_ALBUMS -> t(ru, "Новые альбомы", "New albums")
        GoalKind.NEW_GENRES -> t(ru, "Новые жанры", "New genres")
        GoalKind.NEW_COUNTRIES -> t(ru, "Новые страны", "New countries")
    }

    fun quest(q: Quest, ru: Boolean) = when (q) {
        Quest.WHOLE -> t(ru, "Послушать альбом целиком", "Hear an album whole")
        Quest.WHOLE3 -> t(ru, "Три альбома целиком", "Three albums whole")
        Quest.NEW_ARTISTS -> t(ru, "Три новых артиста", "Three new artists")
        Quest.NEW_COUNTRY -> t(ru, "Артист из новой страны", "An artist from a new country")
        Quest.OLD -> t(ru, "Альбом старше тридцати лет", "An album over thirty years old")
        Quest.GENRES -> t(ru, "Пять разных жанров", "Five different genres")
        Quest.MORNINGS -> t(ru, "Музыка до девяти утра три дня", "Music before nine, three days")
        Quest.NO_SKIPS -> t(ru, "15 песен подряд без пропусков", "15 songs in a row, no skips")
        Quest.WISH -> t(ru, "Что-то из списка «послушать»", "Something from the «to hear» list")
        Quest.HOURS -> t(ru, "Пять часов музыки", "Five hours of music")
        Quest.DECADES -> t(ru, "Музыка трёх десятилетий", "Music from three decades")
        Quest.FORGOTTEN -> t(ru, "Артист, которого не было полгода", "An artist unheard for half a year")
        Quest.LONG -> t(ru, "Сессия от 45 минут", "A 45-minute sitting")
        Quest.NIGHT -> t(ru, "Два вечера с музыкой после десяти", "Two nights of music after ten")
    }

    fun country(code: String, ru: Boolean): String =
        if (code.isBlank()) "" else Locale("", code).getDisplayCountry(if (ru) Locale("ru") else Locale.ENGLISH).ifBlank { code }

    fun flag(code: String): String = if (code.length != 2) "" else code.uppercase().map { Character.toChars(0x1F1E6 + (it - 'A')).concatToString() }.joinToString("")

    /** «3 ч 20 мин» / «3 h 20 min». */
    fun dur(ms: Long, ru: Boolean): String {
        val m = ms / 60_000
        val h = m / 60
        return when {
            h >= 100 -> t(ru, "$h ч", "$h h")
            h > 0 && m % 60 > 0 -> t(ru, "$h ч ${m % 60} мин", "$h h ${m % 60} min")
            h > 0 -> t(ru, "$h ч", "$h h")
            else -> t(ru, "$m мин", "$m min")
        }
    }

    /**
     * A listen's length to the nearest half minute (#24): x:00–x:15 is x, x:16–x:44 is x and a half, x:45–x:59 is x+1.
     * Never under half a minute; an hour and more as [dur].
     */
    fun songDur(ms: Long, ru: Boolean): String {
        if (ms >= 3_600_000L) return dur(ms, ru)
        val s = ms / 1000
        val m = s / 60; val r = s % 60
        val halves = (when { r <= 15 -> m * 2; r <= 44 -> m * 2 + 1; else -> m * 2 + 2 }).coerceAtLeast(1)
        val whole = halves / 2
        return when {
            halves % 2 == 0L -> t(ru, "$whole мин", "$whole min")
            else -> t(ru, "$whole,5 мин", "$whole.5 min")
        }
    }

    fun hours(ms: Long, ru: Boolean): String {
        val h = ms / 3_600_000.0
        return if (h >= 10) t(ru, "${h.toInt()} ч", "${h.toInt()} h") else t(ru, String.format(Locale.US, "%.1f ч", h), String.format(Locale.US, "%.1f h", h))
    }

    fun num(n: Int): String = String.format(Locale("ru"), "%,d", n).replace(',', ' ').replace(' ', ' ')

    fun plural(n: Int, one: String, few: String, many: String): String {
        val a = n % 100; val b = n % 10
        return when { a in 11..14 -> many; b == 1 -> one; b in 2..4 -> few; else -> many }
    }

    fun times(n: Int, ru: Boolean) = if (ru) "$n ${plural(n, "раз", "раза", "раз")}" else if (n == 1) "once" else "$n times"
    fun plays(n: Int, ru: Boolean) = if (ru) "$n ${plural(n, "прослушивание", "прослушивания", "прослушиваний")}" else "$n ${if (n == 1) "listen" else "listens"}"

    fun defaultName(sp: Species, ru: Boolean) = when (sp) {
        Species.LYREBIRD -> t(ru, "Лирик", "Lyric")
        Species.MUSICBOX -> t(ru, "Бубенец", "Tinkle")
        Species.LONGEAR -> t(ru, "Ушастик", "Earl")
        Species.BASILISK -> t(ru, "Бас", "Bass")
        Species.CONDUCTOR -> t(ru, "Маэстро", "Maestro")
        Species.PURR -> t(ru, "Майлз", "Miles")
        Species.AXOLOTL -> t(ru, "Синти", "Synthy")
        Species.RACCOON -> t(ru, "Бит", "Beat")
        Species.WANDERER -> t(ru, "Репей", "Bramble")
        Species.CRICKET -> t(ru, "Скрипач", "Fiddler")
        Species.LARK -> t(ru, "Рассвет", "Dawn")
        Species.SNAIL -> t(ru, "Винил", "Groove")
        Species.PARROT -> t(ru, "Кеша", "Polly")
        Species.TORTOISE -> t(ru, "Патефон", "Gramps")
        Species.RAVEN -> t(ru, "Хранитель", "Keeper")
        Species.WHALE -> t(ru, "Гул", "Hum")
        Species.ECHO -> t(ru, "Отзвук", "Echo")
        Species.FIREFLY -> t(ru, "Огонёк", "Spark")
        Species.POTPOURRI -> t(ru, "Лоскут", "Patch")
        Species.HUMMINGBIRD -> t(ru, "Блёстка", "Glitter")
        Species.MAGPIE -> t(ru, "Дуэт", "Duet")
        Species.BULLFINCH -> t(ru, "Морозко", "Frost")
        Species.BAT -> t(ru, "Эфир", "Static")
        Species.DRAGON -> t(ru, "Опус", "Opus")
        Species.HOUND -> t(ru, "Хатико", "Buddy")
    }

    /** A line for the den, about what's going on. */
    fun poke(st: Studio, ru: Boolean, rnd: Random = Random(st.now / 600_000)): String {
        val p = st.pet ?: return t(ru, "Выбери яйцо – и включай музыку.", "Pick an egg, then play some music.")
        val name = p.pet.name.ifBlank { defaultName(p.species, ru) }
        if (p.egg) {
            val left = Stage.BABY.from - p.fed
            return if (left <= 0) t(ru, "Яйцо дрожит. Ещё песня – и оно треснет!", "The egg is shaking. One more song and it cracks!")
            else t(ru, "В яйце кто-то подпевает. Ещё ${dur(left * 60_000L, true)} музыки.", "Someone in the egg is humming along. ${dur(left * 60_000L, false)} of music to go.")
        }
        if (st.windup >= 1f) return t(ru, "$name совсем остановился. Заведи его музыкой.", "$name has stopped. Wind them up with some music.")
        if (st.windup > 0.3f) return t(ru, "$name двигается всё медленнее. Давно не было музыки.", "$name moves slower and slower. It's been a while.")
        if (st.sleeping) return t(ru, "$name спит в обнимку с наушниками.", "$name sleeps hugging the headphones.")
        st.earworm?.let { e -> if (rnd.nextInt(3) == 0) return t(ru, "$name напевает «${e.name}». Опять.", "$name is humming «${e.name}». Again.") }
        val lines = buildList {
            add(t(ru, "$name любит ${loves(p.species, true)}.", "$name loves ${loves(p.species, false)}."))
            if (st.streak >= 3) add(t(ru, "${st.streak} дней подряд с музыкой. $name доволен.", "${st.streak} days of music in a row. $name is pleased."))
            st.quests.firstOrNull { !it.p.done }?.let { add(t(ru, "На этой неделе: ${quest(it.q, true).lowercase()}.", "This week: ${quest(it.q, false).lowercase()}.")) }
            st.recentGenre?.let { g -> add(t(ru, "После ${genre(g, true)} у $name всё ещё качается хвост.", "After some ${genre(g, false)}, $name's tail is still swinging.")) }
            add(t(ru, "$name слушает вместе с тобой.", "$name is listening with you."))
        }
        return lines[rnd.nextInt(lines.size)]
    }
}
