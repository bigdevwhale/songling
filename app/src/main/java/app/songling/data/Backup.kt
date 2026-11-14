package app.songling.data

import android.content.Context
import app.songling.domain.Form
import app.songling.domain.Genre
import app.songling.domain.Nature
import app.songling.domain.Species
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Everything in one zip: listens, shelf, pets, goals, settings and covers. */
object Backup {
    suspend fun write(ctx: Context, dao: MusicDao, store: SettingsStore, out: OutputStream) {
        val o = JSONObject()
        o.put("version", 1)
        o.put("plays", JSONArray(dao.playList().map { p ->
            JSONObject().put("at", p.at).put("ms", p.ms).put("title", p.title).put("artist", p.artistName).put("album", p.albumName).put("length", p.length)
                .put("source", p.source.name).put("app", p.app).put("whole", p.whole).put("skipped", p.skipped).put("hp", p.headphones).put("ref", p.ref ?: "")
        }))
        o.put("albums", JSONArray(dao.albumList().map { a ->
            JSONObject().put("key", a.key).put("title", a.title).put("artist", a.artistName).put("kind", a.kind.name).put("year", a.year ?: 0).put("genre", a.genre?.name ?: "")
                .put("genreName", a.genreName).put("cover", a.cover ?: "").put("tracks", a.tracks).put("length", a.length).put("checked", a.checked).put("stars", a.stars)
                .put("wish", a.wish).put("added", a.added).put("pinned", a.pinned).put("note", a.note).put("own", a.own).put("aliases", a.aliases).put("more", a.more)
        }))
        o.put("artists", JSONArray(dao.artists().first().map { a ->
            JSONObject().put("key", a.key).put("name", a.name).put("country", a.country).put("genre", a.genre?.name ?: "").put("checked", a.checked).put("stars", a.stars).put("wish", a.wish).put("added", a.added).put("own", a.own).put("gone", a.gone)
        }))
        o.put("tracks", JSONArray(dao.tracks().first().map { t ->
            JSONObject().put("key", t.key).put("title", t.title).put("artist", t.artistName).put("album", t.album).put("albumName", t.albumName).put("number", t.number)
                .put("length", t.length).put("year", t.year ?: 0).put("genre", t.genre?.name ?: "").put("checked", t.checked).put("stars", t.stars).put("wish", t.wish)
                .put("added", t.added).put("loved", t.loved).put("own", t.own).put("pinned", t.pinned).put("more", t.more)
        }))
        o.put("pets", JSONArray(dao.petList().map { p ->
            JSONObject().put("id", p.id).put("species", p.species.name).put("name", p.name).put("laid", p.laid).put("shiny", p.shiny).put("nature", p.nature.name)
                .put("form", p.form?.name ?: "").put("spans", p.spans).put("extra", p.extra)
        }))
        o.put("goals", JSONArray(dao.goals().first().map { g ->
            JSONObject().put("kind", g.kind.name).put("target", g.target).put("period", g.period.name).put("ref", g.ref).put("label", g.label).put("created", g.created).put("until", g.until).put("archived", g.archived)
        }))
        o.put("settings", store.now().json())
        ZipOutputStream(out).use { z ->
            z.putNextEntry(ZipEntry("songling.json")); z.write(o.toString().toByteArray()); z.closeEntry()
            Covers.dir(ctx).listFiles()?.filter { it.name.endsWith(".jpg") }?.forEach { f -> z.putNextEntry(ZipEntry("covers/${f.name}")); f.inputStream().use { it.copyTo(z) }; z.closeEntry() }
        }
    }

    private fun g(s: String) = Genre.entries.firstOrNull { it.name == s }

    /** Replaces everything with the backup. Returns the number of listens restored. */
    suspend fun read(ctx: Context, dao: MusicDao, store: SettingsStore, input: InputStream): Int {
        var json: JSONObject? = null
        ZipInputStream(input).use { z ->
            while (true) {
                val e = z.nextEntry ?: break
                when {
                    e.name == "songling.json" -> json = JSONObject(z.readBytes().toString(Charsets.UTF_8))
                    e.name.startsWith("covers/") -> Covers.file(ctx, e.name.substringAfter('/')).outputStream().use { z.copyTo(it) }
                }
            }
        }
        val o = json ?: error("no songling.json")
        dao.wipe()
        fun arr(k: String) = o.optJSONArray(k)?.let { a -> (0 until a.length()).map { a.getJSONObject(it) } }.orEmpty()
        dao.insertPlays(arr("plays").map { p ->
            Play(at = p.getLong("at"), ms = p.getLong("ms"), title = p.getString("title"), artistName = p.getString("artist"), albumName = p.optString("album"), length = p.optLong("length"),
                source = Source.entries.firstOrNull { it.name == p.optString("source") } ?: Source.PLAYER, app = p.optString("app"), whole = p.optBoolean("whole"),
                skipped = p.optBoolean("skipped"), headphones = p.optBoolean("hp"), ref = p.optString("ref").ifBlank { null })
        })
        for (a in arr("albums")) dao.upsert(Album(a.getString("key"), a.getString("title"), a.getString("artist"), kind = Kind.entries.firstOrNull { it.name == a.optString("kind") } ?: Kind.ALBUM,
            year = a.optInt("year").takeIf { it > 0 }, genre = g(a.optString("genre")), genreName = a.optString("genreName"), cover = a.optString("cover").ifBlank { null },
            tracks = a.optInt("tracks"), length = a.optLong("length"), checked = a.optLong("checked"), stars = a.optInt("stars"), wish = a.optBoolean("wish"),
            added = a.optLong("added"), pinned = a.optBoolean("pinned"), note = a.optString("note"), own = a.optString("own"), aliases = a.optString("aliases"), more = a.optString("more")))
        for (a in arr("artists")) dao.upsert(Artist(a.getString("key"), a.getString("name"), a.optString("country"), g(a.optString("genre")), a.optLong("checked"), a.optInt("stars"), a.optBoolean("wish"), a.optLong("added"), a.optString("own"), a.optInt("gone")))
        for (t in arr("tracks")) dao.upsert(Track(t.getString("key"), t.getString("title"), t.getString("artist"), album = t.optString("album"), albumName = t.optString("albumName"),
            number = t.optInt("number"), length = t.optLong("length"), year = t.optInt("year").takeIf { it > 0 }, genre = g(t.optString("genre")), checked = t.optLong("checked"),
            stars = t.optInt("stars"), wish = t.optBoolean("wish"), added = t.optLong("added"), loved = t.optBoolean("loved"), own = t.optString("own"), pinned = t.optBoolean("pinned"), more = t.optString("more")))
        val ids = HashMap<Long, Long>()
        for (p in arr("pets")) ids[p.getLong("id")] = dao.insert(Pet(species = Species.entries.firstOrNull { it.name == p.getString("species") } ?: Species.LYREBIRD, name = p.optString("name"),
            laid = p.getLong("laid"), shiny = p.optBoolean("shiny"), nature = Nature.entries.firstOrNull { it.name == p.optString("nature") } ?: Nature.CALM,
            form = Form.entries.firstOrNull { it.name == p.optString("form") }, spans = p.getString("spans"), extra = p.optInt("extra")))
        for (x in arr("goals")) dao.insert(Goal(kind = GoalKind.entries.firstOrNull { it.name == x.getString("kind") } ?: GoalKind.MINUTES, target = x.getInt("target"),
            period = Period.entries.firstOrNull { it.name == x.optString("period") } ?: Period.MONTH, ref = x.optString("ref"), label = x.optString("label"),
            created = x.optLong("created"), until = x.optLong("until"), archived = x.optBoolean("archived")))
        val s = Settings.of(o.optString("settings"))
        store.update { s.copy(pet = s.pet?.let { ids[it] }) }
        return arr("plays").size
    }
}
