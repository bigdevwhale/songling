package app.songling.domain

/**
 * Things for the den, bought with plectrums. Each stands in a place of the room; the first twenty are the places
 * themselves, the rest other things for the same places ([slot]), of which the room shows one at a time.
 */
enum class Item(val price: Int, private val of: String? = null, private val light: Boolean = false) {
    RUG(20), PLANT(20), GARLAND(25, light = true), CANDLES(25, light = true), POSTER(30), CASSETTES(35), LAVA(35, light = true), STARS(40),
    SHELF(45), SPEAKERS(50), HEADPHONES(40), TURNTABLE(60), NEON(70), DISCO(80), GRAMOPHONE(90), CLOCK(30),
    METRONOME(35), BONGOS(55), GUITAR(75), GOLD_RECORD(100),
    // Other things for the same places: a place shows one of them at a time.
    RUG_STRIPES(25, "RUG"), RUG_VINYL(35, "RUG"), RUG_FLUFFY(30, "RUG"),
    CACTUS(20, "PLANT"), SUNFLOWERS(30, "PLANT"), LEMON_TREE(40, "PLANT"),
    BUNTING(25, "GARLAND"), LANTERNS(35, "GARLAND", light = true),
    LANTERN(30, "CANDLES", light = true), SNOW_GLOBE(35, "CANDLES"),
    WALKMAN(40, "CASSETTES"), MUSIC_BOX(45, "CASSETTES"),
    HOURGLASS(30, "LAVA"), MUSHROOM_LAMP(35, "LAVA", light = true),
    MOON_STICKERS(40, "STARS", light = true), BUTTERFLIES(35, "STARS"),
    AMPS(60, "SPEAKERS"), FLOOR_LAMPS(55, "SPEAKERS", light = true),
    DREAMCATCHER(35, "HEADPHONES"), TAMBOURINE(40, "HEADPHONES"),
    SYNTH(80, "TURNTABLE"), BOOMBOX(65, "TURNTABLE"),
    NEON_HEART(70, "NEON"), NEON_MOON(70, "NEON"), NEON_BOLT(70, "NEON"),
    PAPER_LAMP(45, "DISCO", light = true), MOBILE(50, "DISCO"),
    TOY_PIANO(85, "GRAMOPHONE"), HARP(95, "GRAMOPHONE"),
    SUN_CLOCK(35, "CLOCK"), DONUT_CLOCK(35, "CLOCK"),
    MARACAS(30, "METRONOME"), BELL(30, "METRONOME"),
    POUF(45, "BONGOS"), DRUM(60, "BONGOS"),
    VIOLIN(75, "GUITAR"), BANJO(70, "GUITAR"), UKULELE(55, "GUITAR"),
    PLATINUM_RECORD(120, "GOLD_RECORD"), TICKET(40, "GOLD_RECORD");

    /** The place it stands in: one of the first twenty. */
    val slot: Item get() = of?.let { valueOf(it) } ?: this

    /** Its picture in resources/den. */
    val sprite: String get() = name.lowercase()

    /** A lamp: the room stays lighter at night with it. */
    val lit: Boolean get() = light


    fun name(ru: Boolean) = when (this) {
        RUG -> Words.t(ru, "Круглый ковёр", "Round rug")
        PLANT -> Words.t(ru, "Монстера", "Monstera")
        GARLAND -> Words.t(ru, "Гирлянда", "Fairy lights")
        CANDLES -> Words.t(ru, "Свечи", "Candles")
        POSTER -> Words.t(ru, "Постеры", "Posters")
        CASSETTES -> Words.t(ru, "Стопка кассет", "Stack of tapes")
        LAVA -> Words.t(ru, "Лава-лампа", "Lava lamp")
        STARS -> Words.t(ru, "Звёзды", "Glow stars")
        SHELF -> Words.t(ru, "Полка", "Record shelf")
        SPEAKERS -> Words.t(ru, "Колонки", "Speakers")
        HEADPHONES -> Words.t(ru, "Наушники", "Headphones")
        TURNTABLE -> Words.t(ru, "Проигрыватель", "Turntable")
        NEON -> Words.t(ru, "Неоновая нота", "Neon note")
        DISCO -> Words.t(ru, "Диско-шар", "Disco ball")
        GRAMOPHONE -> Words.t(ru, "Граммофон", "Gramophone")
        CLOCK -> Words.t(ru, "Часы", "Clock")
        METRONOME -> Words.t(ru, "Метроном", "Metronome")
        BONGOS -> Words.t(ru, "Бонго", "Bongos")
        GUITAR -> Words.t(ru, "Электрогитара", "Electric guitar")
        GOLD_RECORD -> Words.t(ru, "Золотой диск", "Gold record")
        RUG_STRIPES -> Words.t(ru, "Полосатый коврик", "Striped rug")
        RUG_VINYL -> Words.t(ru, "Коврик-пластинка", "Record rug")
        RUG_FLUFFY -> Words.t(ru, "Пушистый коврик", "Fluffy rug")
        CACTUS -> Words.t(ru, "Кактус", "Cactus")
        SUNFLOWERS -> Words.t(ru, "Подсолнухи", "Sunflowers")
        LEMON_TREE -> Words.t(ru, "Лимонное деревце", "Lemon tree")
        BUNTING -> Words.t(ru, "Флажки", "Bunting")
        LANTERNS -> Words.t(ru, "Бумажные фонарики", "Paper lanterns")
        LANTERN -> Words.t(ru, "Фонарь со свечой", "Candle lantern")
        SNOW_GLOBE -> Words.t(ru, "Снежный шар", "Snow globe")
        WALKMAN -> Words.t(ru, "Кассетный плеер", "Cassette player")
        MUSIC_BOX -> Words.t(ru, "Музыкальная шкатулка", "Music box")
        HOURGLASS -> Words.t(ru, "Песочные часы", "Hourglass")
        MUSHROOM_LAMP -> Words.t(ru, "Ночник-гриб", "Mushroom lamp")
        MOON_STICKERS -> Words.t(ru, "Луна и планеты", "Moon and planets")
        BUTTERFLIES -> Words.t(ru, "Бумажные бабочки", "Paper butterflies")
        AMPS -> Words.t(ru, "Гитарные комбики", "Guitar amps")
        FLOOR_LAMPS -> Words.t(ru, "Торшеры", "Floor lamps")
        DREAMCATCHER -> Words.t(ru, "Ловец снов", "Dreamcatcher")
        TAMBOURINE -> Words.t(ru, "Бубен", "Tambourine")
        SYNTH -> Words.t(ru, "Синтезатор", "Synthesizer")
        BOOMBOX -> Words.t(ru, "Бумбокс", "Boombox")
        NEON_HEART -> Words.t(ru, "Неоновое сердце", "Neon heart")
        NEON_MOON -> Words.t(ru, "Неоновый месяц", "Neon moon")
        NEON_BOLT -> Words.t(ru, "Неоновая молния", "Neon bolt")
        PAPER_LAMP -> Words.t(ru, "Бумажный абажур", "Paper lamp")
        MOBILE -> Words.t(ru, "Мобиль из нот", "Note mobile")
        TOY_PIANO -> Words.t(ru, "Игрушечный рояль", "Toy piano")
        HARP -> Words.t(ru, "Арфа", "Harp")
        SUN_CLOCK -> Words.t(ru, "Часы-солнце", "Sun clock")
        DONUT_CLOCK -> Words.t(ru, "Часы-пончик", "Donut clock")
        MARACAS -> Words.t(ru, "Маракасы", "Maracas")
        BELL -> Words.t(ru, "Колокольчик", "Hand bell")
        POUF -> Words.t(ru, "Вязаный пуф", "Knitted pouf")
        DRUM -> Words.t(ru, "Малый барабан", "Snare drum")
        VIOLIN -> Words.t(ru, "Скрипка", "Violin")
        BANJO -> Words.t(ru, "Банджо", "Banjo")
        UKULELE -> Words.t(ru, "Укулеле", "Ukulele")
        PLATINUM_RECORD -> Words.t(ru, "Платиновый диск", "Platinum record")
        TICKET -> Words.t(ru, "Билет с концерта", "Concert ticket")
    }

    fun note(ru: Boolean) = when (this) {
        SHELF -> Words.t(ru, "Корешки цветов альбомов, прослушанных целиком", "Spines in the colours of whole albums")
        TURNTABLE -> Words.t(ru, "Крутится, пока играет музыка", "Spins while music plays")
        POSTER -> Words.t(ru, "Две рамки: обложка альбома и группа – какие выберешь", "Two frames: an album cover and a band of your choice")
        DISCO -> Words.t(ru, "Блики бегут по стенам под музыку", "Spots run over the walls to the music")
        CLOCK -> Words.t(ru, "Часы-пластинка, показывают настоящее время", "A record clock that tells the real time")
        CANDLES -> Words.t(ru, "Горят на подоконнике", "Burning on the sill")
        HEADPHONES -> Words.t(ru, "Висят на крючке", "On a hook")
        METRONOME -> Words.t(ru, "Стоит на подоконнике", "On the window sill")
        GUITAR -> Words.t(ru, "Висит на стене у окна", "On the wall by the window")
        GOLD_RECORD -> Words.t(ru, "В рамке над проигрывателем", "Framed above the turntable")
        LANTERNS -> Words.t(ru, "Светятся вечером", "Glow in the evening")
        LANTERN -> Words.t(ru, "Горит на подоконнике", "Burning on the sill")
        SNOW_GLOBE -> Words.t(ru, "Стоит на подоконнике", "On the window sill")
        MUSHROOM_LAMP -> Words.t(ru, "Светится в темноте", "Glows in the dark")
        MOON_STICKERS -> Words.t(ru, "Светятся ночью", "Glow at night")
        FLOOR_LAMPS -> Words.t(ru, "Два торшера по бокам", "A pair, one on each side")
        TAMBOURINE -> Words.t(ru, "Висит на крючке", "On a hook")
        PAPER_LAMP -> Words.t(ru, "Светится под потолком", "Glowing under the ceiling")
        SUN_CLOCK -> Words.t(ru, "Показывают настоящее время", "Tell the real time")
        DONUT_CLOCK -> Words.t(ru, "Показывают настоящее время", "Tell the real time")
        MARACAS -> Words.t(ru, "Стоят на подоконнике", "On the window sill")
        BELL -> Words.t(ru, "Стоит на подоконнике", "On the window sill")
        VIOLIN -> Words.t(ru, "Висит на стене у окна", "On the wall by the window")
        BANJO -> Words.t(ru, "Висит на стене у окна", "On the wall by the window")
        UKULELE -> Words.t(ru, "Висит на стене у окна", "On the wall by the window")
        PLATINUM_RECORD -> Words.t(ru, "В рамке над проигрывателем", "Framed above the turntable")
        TICKET -> Words.t(ru, "В рамке над проигрывателем", "Framed above the turntable")
        else -> ""
    }

    companion object {
        /** The places, and what can stand in each. */
        val slots: List<Item> get() = entries.filter { it.slot == it }
        fun variants(slot: Item): List<Item> = entries.filter { it.slot == slot }

        /** What the room shows in each place, of the things [shown]: one a place, the last bought if ever two. */
        fun placed(shown: Set<Item>): Map<Item, Item> = shown.groupBy { it.slot }.mapValues { it.value.last() }

        /** The names of the room's other things in [item]'s place, to put away when [item] goes there. */
        fun others(item: Item, bought: Set<String>): Set<String> = variants(item.slot).map { it.name }.filter { it != item.name && it in bought }.toSet()
    }
}

/**
 * Records of albums you've heard, kept in a collection that never fills up: each costs more than the one before, up
 * to [MAX], and an album heard whole costs half. Where plectrums go once the den is furnished.
 */
object Records {
    const val FIRST = 20
    const val STEP = 5
    const val MAX = 150

    fun price(owned: Int, whole: Boolean): Int = (FIRST + STEP * owned).coerceAtMost(MAX).let { if (whole) (it + 1) / 2 else it }
}

/** Eggs: the first of each kind is free; another of a kind you already have costs this. */
const val EGG_AGAIN = 200

/** Wallpapers. The first two are free. */
enum class Wall(val a: Long, val b: Long, val price: Int) {
    PLUM(0xFF2E2240, 0xFF261C36, 0), NIGHT(0xFF1C2440, 0xFF182036, 0),
    FOREST(0xFF1E3A30, 0xFF1A3229, 25), BRICK(0xFF5A2E2A, 0xFF4C2622, 30), TEAL(0xFF1C4048, 0xFF18363E, 25),
    ROSE(0xFF5A2E46, 0xFF4E263C, 30), CREAM(0xFFCDB89A, 0xFFBEA888, 35), VINYL(0xFF1A1A22, 0xFF2A2A34, 40);

    fun name(ru: Boolean) = when (this) {
        PLUM -> Words.t(ru, "Слива", "Plum")
        NIGHT -> Words.t(ru, "Ночь", "Night")
        FOREST -> Words.t(ru, "Лес", "Forest")
        BRICK -> Words.t(ru, "Кирпич", "Brick")
        TEAL -> Words.t(ru, "Морская волна", "Teal")
        ROSE -> Words.t(ru, "Роза", "Rose")
        CREAM -> Words.t(ru, "Сливки", "Cream")
        VINYL -> Words.t(ru, "Винил", "Vinyl")
    }
}
