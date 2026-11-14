package app.songling.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore("settings")

/** A genre of one's own: a name, a colour, and the built-in genre it's closest to (for pets, forms and quests). */
data class OwnGenre(val id: String, val name: String, val color: Long, val base: String) {
    fun json(): JSONObject = JSONObject().put("id", id).put("name", name).put("color", color).put("base", base)

    companion object {
        fun of(o: JSONObject) = OwnGenre(o.getString("id"), o.getString("name"), o.optLong("color", 0xFF8A7A6A), o.optString("base", "OTHER"))
    }
}

/**
 * One pet's room (#26): the things bought for it («wall:<name>» for wallpaper), those put away, the wallpaper up and
 * what hangs in the two frames. Each pet furnishes its own.
 */
data class Room(
    val bought: Set<String> = emptySet(),
    val hidden: Set<String> = emptySet(),
    val wall: String = "PLUM",
    val posters: List<String> = listOf("", ""),
) {
    fun json(): JSONObject = JSONObject().put("bought", JSONArray(bought.toList())).put("hidden", JSONArray(hidden.toList())).put("wall", wall).put("posters", JSONArray(posters))

    companion object {
        fun of(o: JSONObject): Room {
            fun set(k: String) = o.optJSONArray(k)?.let { a -> (0 until a.length()).map { a.getString(it) }.toSet() } ?: emptySet()
            return Room(set("bought"), set("hidden"), o.optString("wall", "PLUM"), o.optJSONArray("posters")?.let { a -> List(2) { i -> a.optString(i, "") } } ?: listOf("", ""))
        }
    }
}

data class Settings(
    val onboarded: Boolean = false,
    /** The pet out of its box now; null before the first egg is chosen. */
    val pet: Long? = null,
    /** Plectrums spent on the den. */
    val spent: Int = 0,
    val bought: Set<String> = emptySet(),
    /** Bought things put away for now. */
    val hidden: Set<String> = emptySet(),
    /** The den's wallpaper: a Den.Wall name. */
    val wall: String = "PLUM",
    /** The picture behind the screens: see Backdrops. */
    val backdrop: String = "none",
    val backdropStamp: Long = 0,
    /** A listen counts from this many seconds. */
    val minSeconds: Int = 30,
    /** Tracks longer than [longMinutes] (podcasts, audiobooks) are left out. */
    val skipLong: Boolean = true,
    val longMinutes: Int = 40,
    /** Count a song only when it was played to within ten seconds of its end (when its length is known). */
    val onlyWhole: Boolean = false,
    /** The shelf's tab that opens first: a ShelfTab name. */
    val shelfFirst: String = "ALBUMS",
    /** Players whose listens are left out (podcasts, video). */
    val excluded: Set<String> = emptySet(),
    /** Only the players in [tracked] are heard, instead of every player but [excluded]. */
    val only: Boolean = false,
    val tracked: Set<String> = emptySet(),
    /** Quests whose plectrums were taken: «week:quest». */
    val claimed: Set<String> = emptySet(),
    /** Covers in lists shown as pixels instead of the picture itself. */
    val pixelCovers: Boolean = false,
    /** The day the earworm was unstuck, so a forgotten song is offered instead. */
    val unstuck: Long = 0,
    /** How the shelf was last sorted and filtered. */
    val shelfSort: String = "",
    val statsPeriod: String = "",
    /** Pets seen for the first time (for the «new egg» mark): species names. */
    val seen: Set<String> = emptySet(),
    /** Strokes given, for the pet's mood. */
    val strokes: Int = 0,
    /** Hide the pet's chatter lines. */
    val quiet: Boolean = false,
    /** «The pet hums»: the earworm card on the den. */
    val earworm: Boolean = true,
    /** Day-start hour for «a day of music» (listens after midnight count to the evening before). */
    val dayStart: Int = 4,
    /** «light», «dark» or «system». */
    val theme: String = "light",
    /** How music looks everywhere: «VINYL» (sleeves, labels) or «TAPE» (J-cards, handwriting). */
    val look: String = "VINYL",
    /** What hangs in the two frames: «album:<key>» or «artist:<key>»; empty picks the favourites. */
    val posters: List<String> = listOf("", ""),
    /** Genres of one's own. */
    val own: List<OwnGenre> = emptyList(),
    /** First-visit hints already read, by screen. */
    val tips: Set<String> = emptySet(),
    /** The window shows the real weather and time of day in [town]. */
    val realWeather: Boolean = true,
    val town: String = "",
    val lat: Double = Double.NaN,
    val lon: Double = Double.NaN,
    /** The last weather fetched, as Outside JSON. */
    val outside: String = "",
    /** Achievement steps already shown: «FEAT:level», so new ones are marked. */
    val feats: Set<String> = emptySet(),
    /** Last.fm read as it happens: the username («» when off), the newest scrobble taken (seconds), the last try and what went wrong. */
    val lastfm: String = "",
    val lastfmFrom: Long = 0,
    val lastfmAt: Long = 0,
    val lastfmError: String = "",
    /**
     * The rooms of the pets in their boxes, by pet id (#26). [bought], [hidden], [wall] and [posters] are the room of
     * the pet out now; bringing out another one swaps them ([switchRoom]).
     */
    val rooms: Map<String, Room> = emptyMap(),
    /** The one shared den of before was given to every pet, once. */
    val roomsSplit: Boolean = false,
) {
    /** The room of the pet out now. */
    val room: Room get() = Room(bought, hidden, wall, posters)

    /** Every pet's room, the one out now included. */
    val allRooms: List<Room> get() = rooms.values + room

    /** Puts the pet out now's room away and brings out pet [to]'s (an empty one for a new pet). */
    fun switchRoom(to: Long?): Settings {
        if (to == pet) return this
        val kept = if (pet != null) rooms + (pet.toString() to room) else rooms
        val next = to?.let { kept[it.toString()] } ?: Room()
        return copy(pet = to, bought = next.bought, hidden = next.hidden, wall = next.wall, posters = next.posters, rooms = kept - to.toString())
    }

    /** Once: every pet [ids] gets a copy of the den shared before rooms were each pet's own. */
    fun splitRooms(ids: List<Long>): Settings =
        if (roomsSplit) this else copy(roomsSplit = true, rooms = rooms + ids.filter { it != pet }.associate { it.toString() to room })

    fun json(): String = JSONObject()
        .put("onboarded", onboarded).put("pet", pet ?: -1).put("spent", spent)
        .put("bought", JSONArray(bought.toList())).put("hidden", JSONArray(hidden.toList()))
        .put("wall", wall).put("backdrop", backdrop).put("backdropStamp", backdropStamp)
        .put("minSeconds", minSeconds).put("skipLong", skipLong).put("longMinutes", longMinutes).put("onlyWhole", onlyWhole).put("shelfFirst", shelfFirst)
        .put("excluded", JSONArray(excluded.toList())).put("only", only).put("tracked", JSONArray(tracked.toList())).put("claimed", JSONArray(claimed.toList()))
        .put("pixelCovers", pixelCovers).put("unstuck", unstuck).put("shelfSort", shelfSort).put("statsPeriod", statsPeriod)
        .put("seen", JSONArray(seen.toList())).put("strokes", strokes).put("quiet", quiet).put("earworm", earworm).put("dayStart", dayStart).put("theme", theme).put("look", look).put("posters", JSONArray(posters)).put("own", JSONArray(own.map { it.json() })).put("tips", JSONArray(tips.toList())).put("realWeather", realWeather).put("town", town)
        .put("lat", if (lat.isNaN()) JSONObject.NULL else lat).put("lon", if (lon.isNaN()) JSONObject.NULL else lon).put("outside", outside)
        .put("feats", JSONArray(feats.toList()))
        .put("lastfm", lastfm).put("lastfmFrom", lastfmFrom).put("lastfmAt", lastfmAt).put("lastfmError", lastfmError)
        .put("rooms", JSONObject().also { o -> rooms.forEach { (k, r) -> o.put(k, r.json()) } }).put("roomsSplit", roomsSplit)
        .toString()

    companion object {
        private fun JSONObject.set(k: String): Set<String> = optJSONArray(k)?.let { a -> (0 until a.length()).map { a.getString(it) }.toSet() } ?: emptySet()

        fun of(s: String?): Settings {
            val o = runCatching { JSONObject(s ?: return Settings()) }.getOrNull() ?: return Settings()
            val d = Settings()
            return Settings(
                onboarded = o.optBoolean("onboarded"),
                pet = o.optLong("pet", -1).takeIf { it >= 0 },
                spent = o.optInt("spent"),
                bought = o.set("bought"), hidden = o.set("hidden"),
                wall = o.optString("wall", d.wall), backdrop = o.optString("backdrop", d.backdrop), backdropStamp = o.optLong("backdropStamp"),
                minSeconds = o.optInt("minSeconds", d.minSeconds), skipLong = o.optBoolean("skipLong", d.skipLong), longMinutes = o.optInt("longMinutes", d.longMinutes), onlyWhole = o.optBoolean("onlyWhole", d.onlyWhole), shelfFirst = o.optString("shelfFirst", d.shelfFirst),
                excluded = o.set("excluded"), only = o.optBoolean("only"), tracked = o.set("tracked"), claimed = o.set("claimed"),
                pixelCovers = o.optBoolean("pixelCovers"), unstuck = o.optLong("unstuck"),
                shelfSort = o.optString("shelfSort"), statsPeriod = o.optString("statsPeriod"),
                seen = o.set("seen"), strokes = o.optInt("strokes"), quiet = o.optBoolean("quiet"), earworm = o.optBoolean("earworm", true),
                dayStart = o.optInt("dayStart", d.dayStart),
                theme = o.optString("theme", d.theme), look = o.optString("look", d.look),
                tips = o.set("tips"),
                realWeather = o.optBoolean("realWeather", true), town = o.optString("town"),
                lat = o.optDouble("lat", Double.NaN), lon = o.optDouble("lon", Double.NaN), outside = o.optString("outside"), feats = o.set("feats"),
                own = o.optJSONArray("own")?.let { a -> (0 until a.length()).mapNotNull { runCatching { OwnGenre.of(a.getJSONObject(it)) }.getOrNull() } } ?: emptyList(),
                posters = o.optJSONArray("posters")?.let { a -> List(2) { i -> a.optString(i, "") } } ?: d.posters,
                lastfm = o.optString("lastfm"), lastfmFrom = o.optLong("lastfmFrom"), lastfmAt = o.optLong("lastfmAt"), lastfmError = o.optString("lastfmError"),
                rooms = o.optJSONObject("rooms")?.let { r -> r.keys().asSequence().associateWith { k -> Room.of(r.getJSONObject(k)) } } ?: emptyMap(),
                roomsSplit = o.optBoolean("roomsSplit"),
            )
        }
    }
}

/** Whether music from the player [pkg] is heard at all. */
fun Settings.hears(pkg: String): Boolean = if (only) pkg in tracked else pkg !in excluded

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore
    private val KEY = stringPreferencesKey("settings")

    val settings: Flow<Settings> = store.data.map { Settings.of(it[KEY]) }

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit { it[KEY] = change(Settings.of(it[KEY])).json() }
    }

    /** A one-off chore done once per install. */
    suspend fun done(chore: String): Boolean = store.data.first()[booleanPreferencesKey("done_$chore")] ?: false
    suspend fun markDone(chore: String) { store.edit { it[booleanPreferencesKey("done_$chore")] = true } }
}
