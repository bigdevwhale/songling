package app.songling

import app.songling.data.Keys
import app.songling.data.LastFm
import app.songling.data.Source
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LastFmTest {
    private val page = """
        {"recenttracks":{"track":[
          {"artist":{"#text":"Royal Blood"},"name":"Ride","album":{"#text":"Dead Company"},"@attr":{"nowplaying":"true"}},
          {"artist":{"#text":"Royal Blood"},"name":"Figure It Out","album":{"#text":"Royal Blood"},"date":{"uts":"1763140000","#text":"14 Nov 2026, 17:06"}},
          {"artist":{"#text":"Daft Punk"},"name":"Veridis Quo","album":{"#text":""},"date":{"uts":"1763139000"}}
        ],"@attr":{"user":"x","page":"1","totalPages":"3"}}}
    """.trimIndent()

    /** What plays right now waits until it's scrobbled; a known length is used, else three and a half minutes. */
    @Test fun readsScrobbles() {
        val known = Keys.track("Royal Blood", "Figure It Out")
        val p = LastFm.parse(JSONObject(page)) { if (it == known) 185_000L else 0L }
        assertEquals(3, p.pages)
        assertEquals(listOf("Figure It Out", "Veridis Quo"), p.plays.map { it.title })
        val a = p.plays[0]
        assertEquals(1763140000_000L, a.at)
        assertEquals(185_000L, a.ms)
        assertEquals("Royal Blood", a.albumName)
        assertEquals(Source.LASTFM, a.source)
        assertEquals(210_000L, p.plays[1].ms)
        assertEquals("", p.plays[1].album)
    }

    /** A page with a single scrobble holds an object, not a list. */
    @Test fun readsASingleScrobble() {
        val one = """{"recenttracks":{"track":{"artist":{"#text":"A"},"name":"B","album":{"#text":"C"},"date":{"uts":"5"}},"@attr":{"totalPages":"1"}}}"""
        assertEquals(listOf("B"), LastFm.parse(JSONObject(one)) { 0L }.plays.map { it.title })
    }

    /** What plays now on another device: only a track marked «now playing», else nothing. */
    @Test fun readsWhatPlaysNow() {
        val n = LastFm.playing(JSONObject(page))
        assertEquals("Ride", n?.title)
        assertEquals("Royal Blood", n?.artist)
        assertEquals("Dead Company", n?.album)
        assertEquals(LastFm.APP, n?.app)
        val done = """{"recenttracks":{"track":[{"artist":{"#text":"A"},"name":"B","date":{"uts":"5"}}]}}"""
        assertEquals(null, LastFm.playing(JSONObject(done)))
    }
}
