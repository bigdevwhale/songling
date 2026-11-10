package app.songling.domain

/** Things for the den, bought with plectrums. */
enum class Item(val price: Int) {
    RUG(20), PLANT(20), GARLAND(25), CANDLES(25), POSTER(30), CASSETTES(35), LAVA(35), STARS(40),
    SHELF(45), SPEAKERS(50), HEADPHONES(40), TURNTABLE(60), NEON(70), DISCO(80), GRAMOPHONE(90), CLOCK(30);

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
    }

    fun note(ru: Boolean) = when (this) {
        SHELF -> Words.t(ru, "Корешки цветов альбомов, прослушанных целиком", "Spines in the colours of whole albums")
        TURNTABLE -> Words.t(ru, "Крутится, пока играет музыка", "Spins while music plays")
        POSTER -> Words.t(ru, "Две рамки: обложка альбома и группа – какие выберешь", "Two frames: an album cover and a band of your choice")
        DISCO -> Words.t(ru, "Блики бегут по стенам под музыку", "Spots run over the walls to the music")
        CLOCK -> Words.t(ru, "Часы-пластинка, показывают настоящее время", "A record clock that tells the real time")
        CANDLES -> Words.t(ru, "Горят на подоконнике", "Burning on the sill")
        HEADPHONES -> Words.t(ru, "Висят на крючке", "On a hook")
        else -> ""
    }
}

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
