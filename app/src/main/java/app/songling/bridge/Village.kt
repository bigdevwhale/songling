package app.songling.bridge

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import app.songling.BuildConfig
import app.songling.SonglingApp
import app.songling.AppContainer
import app.songling.data.Backup
import app.songling.data.MIN
import app.songling.data.Source
import app.songling.domain.Item
import app.songling.domain.Nature
import app.songling.domain.Species
import app.songling.domain.Stage
import app.songling.domain.Wall
import app.songling.domain.Words
import app.songling.ru
import app.songling.ui.art.Buf
import app.songling.ui.art.DenArt
import app.songling.ui.art.DenLook
import app.songling.ui.art.toBitmap
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The bridge to Lingvale, the village where the pets of all the -ling apps live together.
 *
 * Lingvale asks through ContentResolver.call() and nobody else gets an answer: the caller must be Lingvale signed
 * with its release key (a debug or demo build also answers a Lingvale built with the same debug key).
 * Protocol v2, the same in every -ling app:
 *  - "hello"    → {v}
 *  - "snapshot" → {json}: pets, species, days of activity, the last week's events
 *  - "art"      → {bytes}: one picture from res/drawable-nodpi (pet_*, egg_*, item_*, *_bg)
 *  - "backup"   (extras {fd}: a file open for writing) → {ok, name}: writes the same backup file the settings save
 *  - "restore"  (extras {fd}: a file open for reading) → {ok, error}: loads such a file, as the settings' «Restore» does,
 *    replacing what is on the phone; error is "newer" (a file from a newer version), "busy" or "bad"
 * Only "restore" changes anything here, and Lingvale asks the player before it calls it.
 */
class VillageProvider : ContentProvider() {
    override fun onCreate() = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val ctx = context ?: return null
        if (!trusted(ctx)) throw SecurityException("Only Lingvale may ask")
        return when (method) {
            "hello" -> Bundle().apply { putInt("v", VERSION) }
            "snapshot" -> Bundle().apply { putString("json", runBlocking { snapshot(ctx, container(ctx)) }.toString()) }
            "art" -> art(ctx, arg)
            "backup" -> backup(ctx, extras)
            "restore" -> restore(ctx, extras)
            else -> null
        }
    }

    private fun trusted(ctx: Context): Boolean {
        val pkg = callingPackage ?: return false
        if (pkg != HUB && !pkg.startsWith("$HUB.")) return false
        val pm = ctx.packageManager
        if (HUB_CERTS.any { pm.hasSigningCertificate(pkg, it, PackageManager.CERT_INPUT_SHA256) }) return true
        return BuildConfig.DEBUG && pm.checkSignatures(pkg, ctx.packageName) == PackageManager.SIGNATURE_MATCH
    }

    private fun art(ctx: Context, name: String?): Bundle? {
        if (name == null || !ART.matches(name)) return null
        val bytes = resArt(ctx, name) ?: return null
        return Bundle().apply { putByteArray("bytes", bytes) }
    }

    @Suppress("DEPRECATION")
    private fun fd(extras: Bundle?): ParcelFileDescriptor? = extras?.getParcelable("fd")

    /** Writes the settings' backup file into the file Lingvale opened. */
    private fun backup(ctx: Context, extras: Bundle?): Bundle {
        val fd = fd(extras) ?: return Bundle().apply { putBoolean("ok", false) }
        val ok = runCatching {
            val c = container(ctx)
            ParcelFileDescriptor.AutoCloseOutputStream(fd).use { out -> runBlocking { Backup.write(ctx, c.db.dao(), c.settings, out) } }
        }.onFailure { runCatching { fd.close() } }.isSuccess
        return Bundle().apply { putBoolean("ok", ok); putString("name", "songling.zip") }
    }

    /** Loads a backup file from Lingvale, the way the settings do. */
    private fun restore(ctx: Context, extras: Bundle?): Bundle {
        val fd = fd(extras) ?: return Bundle().apply { putBoolean("ok", false); putString("error", "bad") }
        val ok = runCatching {
            val c = container(ctx)
            ParcelFileDescriptor.AutoCloseInputStream(fd).use { input -> runBlocking { Backup.read(ctx, c.db.dao(), c.settings, input) } }
        }.onFailure { runCatching { fd.close() } }.isSuccess
        return Bundle().apply { putBoolean("ok", ok); if (!ok) putString("error", "bad") }
    }

    /** The provider can be asked before the application has finished starting; wait for it a little. */
    private fun container(ctx: Context): AppContainer {
        repeat(100) {
            runCatching { return (ctx.applicationContext as SonglingApp).container }
            Thread.sleep(30)
        }
        error("not ready")
    }

    /**
     * Songling's pets are 160×160 pictures in java resources (/pets/<kind>_<stage>.png, eggs /pets/<kind>_egg.png),
     * sent twice as big, pixel for pixel. The den ("den_bg") is painted in code: the wallpaper and things of the room
     * that's up now, without the pet.
     */
    private fun resArt(ctx: Context, name: String): ByteArray? {
        if (name == "den_bg") return runCatching { den(ctx) }.getOrNull()
        val path = when {
            name.startsWith("egg_") -> "/pets/${name.removePrefix("egg_")}_egg.png"
            name.startsWith("pet_") -> {
                val sp = name.removePrefix("pet_").substringBeforeLast('_')
                val stage = when (name.substringAfterLast('_')) { "0" -> "baby"; "1" -> "young"; "3", "4" -> "ancient"; else -> "adult" }
                "/pets/${sp}_$stage.png".takeIf { VillageProvider::class.java.getResource(it) != null } ?: "/pets/${sp}_adult.png"
            }
            else -> return null
        }
        val bmp = VillageProvider::class.java.getResourceAsStream(path)?.use { BitmapFactory.decodeStream(it) } ?: return null
        return png(Bitmap.createScaledBitmap(bmp, bmp.width * 2, bmp.height * 2, false))
    }

    private fun den(ctx: Context): ByteArray {
        val s = runBlocking { container(ctx).settings.now() }
        val now = Instant.now().atZone(ZoneId.systemDefault())
        val look = DenLook(
            wall = Wall.entries.firstOrNull { it.name == s.wall } ?: Wall.PLUM,
            items = Item.entries.filter { it.name in s.bought && it.name !in s.hidden }.toSet(),
            hour = now.hour, minute = now.minute, spines = SPINES, playing = false,
        )
        val b = Buf(DenArt.W, DenArt.H, Buf.FINE)
        DenArt.draw(b, look, 0)
        return png(b.toBitmap())
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0

    companion object {
        const val VERSION = 2
        const val HUB = "app.lingvale"
        /** SHA-256 of Lingvale's release certificate. */
        private val HUB_CERTS = listOf(hex("CEF40A1786896D829CFA53115693AC9A53FC0349EAB84BAF64EFA648592D1ABF"))
        private val ART = Regex("""(pet|egg|item|find)_[a-z0-9_]+|[a-z]+_bg""")
        private fun hex(s: String) = ByteArray(s.length / 2) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
        private fun color(c: Long) = "#%06X".format(c and 0xFFFFFF)
        private fun dayOf(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()

        private fun png(b: Bitmap): ByteArray = ByteArrayOutputStream().also { b.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()

        private val SPINES = listOf(0xFFE2584A, 0xFFF0A63A, 0xFF4CB4D8, 0xFF6CC46A, 0xFFB06AD8, 0xFFF2D84C, 0xFFE86AA8, 0xFF6A7AE0, 0xFF3EA06A).map { it.toInt() }.let { it + it + it }

        /** The ones found by something that seldom happens: a concert, a long silence, the radio, an eight-minute song. */
        private val RARE = setOf(Species.FIREFLY, Species.ECHO, Species.RAVEN, Species.BAT, Species.DRAGON, Species.MOTH, Species.PHOENIX, Species.UNICORN)

        private val colours = HashMap<Species, Long>()

        /** A species' colour: the average of the coloured pixels of its grown picture. */
        private fun colourOf(sp: Species): Long = synchronized(colours) {
            colours.getOrPut(sp) {
                runCatching {
                    val bmp = VillageProvider::class.java.getResourceAsStream("/pets/${sp.file}_adult.png")?.use { BitmapFactory.decodeStream(it) }!!
                    val px = IntArray(bmp.width * bmp.height).also { bmp.getPixels(it, 0, bmp.width, 0, 0, bmp.width, bmp.height) }
                    val hsv = FloatArray(3)
                    var r = 0L; var g = 0L; var b = 0L; var n = 0L
                    for (c in px) {
                        if (c ushr 24 < 200) continue
                        android.graphics.Color.colorToHSV(c, hsv)
                        if (hsv[1] < 0.25f || hsv[2] < 0.25f) continue
                        r += (c shr 16) and 0xFF; g += (c shr 8) and 0xFF; b += c and 0xFF; n++
                    }
                    if (n == 0L) 0x8A7A6AL else ((r / n) shl 16) or ((g / n) shl 8) or (b / n)
                }.getOrDefault(0x8A7A6AL)
            }
        }

        /** Everything Lingvale shows of Songling: listens as days, the pet that's out as the leader. */
        suspend fun snapshot(ctx: Context, c: AppContainer): JSONObject {
            val st = c.studio()
            val today = LocalDate.now().toEpochDay()
            val leader = st.pet?.pet?.id
            val live = st.plays.filter { it.source != Source.IMPORT }

            val petsJson = JSONArray()
            st.pets.forEach { p ->
                // When it hatched isn't kept: it's the day its first three hours of music (less what it brought along) were eaten.
                val hatched = if (p.stage == Stage.EGG) 0L else {
                    val ranges = p.pet.spans.split(',').mapNotNull { r ->
                        val a = r.split('-'); if (a.size != 2) return@mapNotNull null
                        val from = a[0].toLongOrNull() ?: return@mapNotNull null
                        from..(a[1].toLongOrNull()?.takeIf { it > 0 } ?: Long.MAX_VALUE)
                    }
                    val need = (Stage.BABY.from - p.pet.extra).coerceAtLeast(0) * MIN / (if (p.pet.nature == Nature.GREEDY) 1.1 else 1.0)
                    var ms = 0L; var day = dayOf(p.pet.laid)
                    for (pl in live) {
                        if (ms >= need) break
                        if (ranges.none { pl.at in it }) continue
                        ms += pl.ms; day = dayOf(pl.at)
                    }
                    day
                }
                petsJson.put(JSONObject()
                    .put("id", p.pet.id).put("species", p.species.file).put("name", p.pet.name)
                    .put("stage", p.stage.ordinal).put("progress", p.progress.toDouble())
                    .put("slot", if (p.pet.id == leader) 0 else -1).put("shiny", p.pet.shiny)
                    .put("found", dayOf(p.pet.laid)).put("hatched", hatched))
            }
            val speciesJson = JSONArray()
            Species.known.forEach { s ->
                speciesJson.put(JSONObject().put("id", s.file).put("ru", Words.species(s, true)).put("en", Words.species(s, false))
                    .put("color", color(colourOf(s))).put("aboutRu", "любит " + Words.loves(s, true)).put("aboutEn", "loves " + Words.loves(s, false))
                    .put("rare", s in RARE))
            }
            val daysJson = JSONArray()
            st.plays.groupBy { dayOf(it.at) }.toSortedMap().forEach { (d, l) ->
                if (d > today - 400) daysJson.put(JSONObject().put("d", d).put("n", l.size).put("min", l.sumOf { it.ms } / MIN))
            }
            val eventsJson = JSONArray()
            st.plays.filter { dayOf(it.at) > today - 8 }.forEach { p ->
                val line = if (p.artistName.isBlank()) p.title else "${p.artistName} – ${p.title}"
                eventsJson.put(JSONObject().put("at", p.at).put("min", p.ms / MIN).put("ru", line).put("en", line))
            }
            return JSONObject()
                .put("v", VERSION).put("app", "songling").put("version", BuildConfig.VERSION_NAME)
                .put("ru", if (BuildConfig.DEMO && BuildConfig.DEMO_LANG.isNotEmpty()) BuildConfig.DEMO_LANG == "ru" else ctx.ru)
                .put("scene", "den_bg")
                .put("pets", petsJson).put("species", speciesJson).put("days", daysJson).put("events", eventsJson)
        }
    }
}
