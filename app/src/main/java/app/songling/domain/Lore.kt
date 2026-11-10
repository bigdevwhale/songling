package app.songling.domain

/** The grown form a pet takes for good, after the music it was raised on. */
enum class Form(val argb: Long) {
    STORM(0xFFE2584A), NEON(0xFF4CD8E8), VELVET(0xFFB06AD8), CHOIR(0xFFF2D88C), STREET(0xFFF0A63A), ROOTS(0xFF6CC46A),
}

/** Genres as the app tells them apart; the catalogue's many names fold into these. */
enum class Genre(val argb: Long, val form: Form) {
    ROCK(0xFFD0483A, Form.STORM),
    METAL(0xFF6E6064, Form.STORM),
    PUNK(0xFFE86A3A, Form.STORM),
    ALT(0xFF8A6AC8, Form.STORM),
    POP(0xFFE86AA8, Form.STREET),
    HIPHOP(0xFFF0A63A, Form.STREET),
    RNB(0xFFC0508A, Form.VELVET),
    JAZZ(0xFF3C6AA8, Form.VELVET),
    BLUES(0xFF3A4A8A, Form.VELVET),
    ELECTRONIC(0xFF2CB8D8, Form.NEON),
    DANCE(0xFF6A7AE0, Form.NEON),
    AMBIENT(0xFF5AA8A0, Form.CHOIR),
    CLASSICAL(0xFFD8B860, Form.CHOIR),
    SOUNDTRACK(0xFFA89878, Form.CHOIR),
    FOLK(0xFF6C9A44, Form.ROOTS),
    COUNTRY(0xFFB07A3E, Form.ROOTS),
    WORLD(0xFF3EA06A, Form.ROOTS),
    LATIN(0xFFE2584A, Form.ROOTS),
    REGGAE(0xFF4CA83C, Form.ROOTS),
    CHANSON(0xFF9A5A6A, Form.VELVET),
    KIDS(0xFFF2D84C, Form.STREET),
    OTHER(0xFF8A8296, Form.STREET);

    companion object {
        /** Folds a catalogue's genre name (iTunes, Spotify, tags) into ours. */
        fun of(name: String?): Genre? {
            val n = name?.lowercase()?.trim().orEmpty()
            if (n.isEmpty()) return null
            return when {
                "metal" in n || "метал" in n -> METAL
                "punk" in n || "hardcore" in n || "панк" in n -> PUNK
                "alternative" in n || "indie" in n || "альтерн" in n || "инди" in n || "grunge" in n || "shoegaze" in n -> ALT
                "hip" in n || "rap" in n || "рэп" in n || "хип" in n || "trap" in n || "grime" in n -> HIPHOP
                "r&b" in n || "soul" in n || "соул" in n || "funk" in n || "rnb" in n -> RNB
                "jazz" in n || "джаз" in n || "swing" in n || "bossa" in n -> JAZZ
                "blues" in n || "блюз" in n -> BLUES
                "ambient" in n || "эмбиент" in n || "new age" in n || "lo-fi" in n || "lofi" in n || "chill" in n || "meditat" in n || "relax" in n -> AMBIENT
                "dance" in n || "house" in n || "techno" in n || "trance" in n || "disco" in n || "edm" in n || "drum" in n || "dubstep" in n -> DANCE
                "electr" in n || "электр" in n || "synth" in n || "idm" in n || "downtempo" in n || "trip" in n -> ELECTRONIC
                "classical" in n || "класси" in n || "opera" in n || "опера" in n || "baroque" in n || "orchestr" in n || "piano" in n -> CLASSICAL
                "soundtrack" in n || "score" in n || "саундтрек" in n || "film" in n || "game" in n || "anime" in n || "musical" in n -> SOUNDTRACK
                "folk" in n || "фолк" in n || "singer/songwriter" in n || "songwriter" in n || "acoustic" in n || "bard" in n || "бард" in n -> FOLK
                "country" in n || "кантри" in n || "americana" in n || "bluegrass" in n -> COUNTRY
                "reggae" in n || "ska" in n || "dub" in n || "dancehall" in n -> REGGAE
                "latin" in n || "латин" in n || "salsa" in n || "reggaeton" in n || "tango" in n || "brazil" in n || "flamenco" in n -> LATIN
                "world" in n || "african" in n || "afro" in n || "celtic" in n || "k-pop" == n || "j-pop" == n || "этни" in n -> WORLD
                "chanson" in n || "шансон" in n || "romance" in n || "эстрад" in n || "french pop" in n -> CHANSON
                "children" in n || "kids" in n || "детск" in n -> KIDS
                "rock" in n || "рок" in n || "britpop" in n || "psychedel" in n -> ROCK
                "pop" in n || "поп" in n || "k-pop" in n || "j-pop" in n || "singer" in n -> POP
                else -> OTHER
            }
        }
    }
}

/** Life stages; a pet moves up by the minutes of music it was fed. */
enum class Stage(val from: Int) {
    EGG(0), BABY(180), YOUNG(1_200), ADULT(4_000), ANCIENT(12_000);

    val next: Stage? get() = entries.getOrNull(ordinal + 1)
}

/** How a pet takes things. Decided when the egg is laid. */
enum class Nature { CALM, SLEEPY, DANCER, CURIOUS, SHY, CHATTY, GREEDY }

/** What shows on the pet's face for a while after a listen. */
enum class Mood { NONE, HEADBANG, SWAY, DREAMY, HEARTS, SPARKLE, SLEEPY, PROUD }

/**
 * The pets. Three eggs are there from the start; the rest are found by listening a certain way.
 * [loves] says in a few words what each one is fed best by.
 */
enum class Species(val starter: Boolean = false, val floats: Boolean = false) {
    LYREBIRD(true), MUSICBOX(true), LONGEAR(true),
    BASILISK, CONDUCTOR, PURR, AXOLOTL, RACCOON, WANDERER,
    CRICKET, LARK, SNAIL, PARROT, TORTOISE,
    WHALE(floats = true), ECHO(floats = true), FIREFLY(floats = true), POTPOURRI;

    val file get() = name.lowercase()
}

/** The genres a species is especially fond of, if any. */
val Species.genres: Set<Genre> get() = when (this) {
    Species.BASILISK -> setOf(Genre.ROCK, Genre.METAL, Genre.PUNK, Genre.ALT)
    Species.CONDUCTOR -> setOf(Genre.CLASSICAL, Genre.SOUNDTRACK)
    Species.PURR -> setOf(Genre.JAZZ, Genre.BLUES, Genre.RNB)
    Species.AXOLOTL -> setOf(Genre.ELECTRONIC, Genre.DANCE)
    Species.RACCOON -> setOf(Genre.HIPHOP)
    Species.WANDERER -> setOf(Genre.FOLK, Genre.WORLD, Genre.LATIN, Genre.COUNTRY, Genre.REGGAE)
    Species.WHALE -> setOf(Genre.AMBIENT)
    else -> emptySet()
}
