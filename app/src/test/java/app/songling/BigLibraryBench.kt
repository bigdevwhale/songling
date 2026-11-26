package app.songling

import app.songling.data.Album
import app.songling.data.Artist
import app.songling.data.Keys
import app.songling.data.MIN
import app.songling.data.Pet
import app.songling.data.Play
import app.songling.data.Settings
import app.songling.data.Source
import app.songling.data.Track
import app.songling.domain.Genre
import app.songling.domain.Species
import app.songling.domain.Studio
import org.junit.Test
import kotlin.random.Random

/** A library the size of a long Spotify history: how long one [Studio] takes to work out. Run by hand. */
class BigLibraryBench {
    @Test fun bigLibrary() {
        if (System.getProperty("bench") == null && System.getenv("BENCH") == null) return
        val r = Random(1)
        val now = 1_790_000_000_000L
        val artists = (0 until 2550).map { Artist(Keys.artist("Artist $it"), "Artist $it", tagGenre = Genre.entries[it % Genre.entries.size]) }
        val albums = (0 until 6000).map { i -> val a = i % 2550; Album(Keys.album("Artist $a", "Album $i"), "Album $i", "Artist $a", tracks = 4, length = 16 * MIN, genre = Genre.entries[i % Genre.entries.size]) }
        val tracks = (0 until 21539).map { i -> val al = i % 6000; val a = al % 2550
            Track(Keys.track("Artist $a", "Song $i"), "Song $i", "Artist $a", album = Keys.album("Artist $a", "Album $al"), albumName = "Album $al", length = 4 * MIN) }
        val byAlbum = tracks.groupBy { it.album }.values.toList()
        val n = (System.getenv("BENCH_PLAYS") ?: "150000").toInt()
        var at = now - 6L * 365 * 24 * 3_600_000L
        val step = (now - at) / n
        // Half the time whole albums in a row (a Spotify history has thousands of those), else songs here and there.
        val plays = ArrayList<Play>(n)
        while (plays.size < n) {
            val run = if (r.nextBoolean()) byAlbum[r.nextInt(byAlbum.size)] else List(10) { tracks[(r.nextDouble().let { x -> x * x } * tracks.size).toInt()] }
            for (t in run) {
                at += step
                val i = plays.size
                plays += Play(id = i + 1L, at = at, ms = 3 * MIN, title = t.title, artistName = t.artistName, albumName = t.albumName, length = 4 * MIN,
                    source = if (i < n * 0.9) Source.IMPORT else Source.PLAYER, whole = true)
            }
        }
        val pets = listOf(Pet(id = 1, species = Species.CONDUCTOR, laid = now - 30L * 24 * 3_600_000L, spans = "${now - 30L * 24 * 3_600_000L}-0"))
        // A poor man's sampler: where the work thread spends its time, by our own frames.
        val me = Thread.currentThread()
        val hits = java.util.concurrent.ConcurrentHashMap<String, Int>()
        val running = java.util.concurrent.atomic.AtomicBoolean(true)
        val sampler = Thread {
            while (running.get()) {
                val st = me.stackTrace
                val ours = st.filter { it.className.startsWith("app.songling") && !it.className.contains("Bench") }.take(2)
                    .joinToString(" < ") { "${it.className.substringAfterLast('.')}.${it.methodName}:${it.lineNumber}" }
                val top = st.firstOrNull()?.let { "${it.className.substringAfterLast('.')}.${it.methodName}" } ?: "?"
                if (ours.isNotEmpty()) hits.merge("$ours  [$top]", 1, Int::plus)
                Thread.sleep(1)
            }
        }.apply { isDaemon = true; start() }
        repeat(3) { round ->
            val t0 = System.nanoTime()
            val st = Studio(plays, albums, artists, tracks, pets, emptyList(), Settings(pet = 1), now)
            val t1 = System.nanoTime()
            st.hints; val t2 = System.nanoTime()
            st.pets; st.streak; st.plectrums; st.feats; st.quests; st.goalsDone; st.recordsToBuy; val t3 = System.nanoTime()
            println("round $round: studio ${(t1 - t0) / 1_000_000} ms, hints ${(t2 - t1) / 1_000_000} ms, rest ${(t3 - t2) / 1_000_000} ms")
        }
        running.set(false); sampler.join()
        for ((k, v) in hits.entries.sortedByDescending { it.value }.take(30)) println("hot $v  $k")
    }
}
