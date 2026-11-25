/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.prime.engine

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerAndAlgorithmTests {
    @Test
    fun `ad block engine detects promoted and ad slot nodes`() {
        val before = AdBlockEngine.blockedCount.get()
        val adNode = buildJsonObject {
            putJsonObject("adSlotRenderer") { put("id", "ad_123") }
        }
        val cleanNode = buildJsonObject {
            putJsonObject("musicResponsiveListItemRenderer") { put("title", "Song") }
        }
        assertTrue(AdBlockEngine.isAdOrPromotedNode(adNode))
        assertFalse(AdBlockEngine.isAdOrPromotedNode(cleanNode))
        assertTrue(AdBlockEngine.isAdTrackingStreamUrl("https://googleads.g.doubleclick.net/pagead/interaction"))
        assertFalse(AdBlockEngine.isAdTrackingStreamUrl("https://rr1---sn.googlevideo.com/videoplayback?mime=audio/mp4"))
        assertTrue(AdBlockEngine.blockedCount.get() >= before + 2)
    }

    @Test
    fun `sapisid auth signer generates valid SAPISIDHASH`() {
        val cookie = "PREF=f1=500; SAPISID=abc123XYZ/def456; HSID=xyz"
        val header = SapisidAuthSigner.buildAuthHeader(cookie, epochSeconds = 1_700_000_000L)
        assertNotNull(header)
        assertTrue(header!!.startsWith("SAPISIDHASH 1700000000_"))
        assertEquals("SAPISIDHASH 1700000000_".length + 40, header.length)
    }

    @Test
    fun `spotify client extracts playlist id and generates 6 digit totp`() {
        val client = SpotifyTasteClient()
        assertEquals(
            "37i9dQZF1DXcBWIGoYBM5M",
            client.extractSpotifyPlaylistId("https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M?si=abcd"),
        )
        assertEquals(
            "37i9dQZF1DXcBWIGoYBM5M",
            client.extractSpotifyPlaylistId("https://open.spotify.com/intl-es/playlist/37i9dQZF1DXcBWIGoYBM5M"),
        )
        val totp = client.generateTotp("JBSWY3DPEHPK3PXP", 1_700_000_000L)
        assertEquals(6, totp.length)
        assertTrue(totp.all(Char::isDigit))
    }

    @Test
    fun `hybrid algorithm interleaves tracks proportionally by blend weight`() {
        val engine = HybridAlgorithmEngine(InnerTubeClient())
        val spotifyTracks = (1..6).map { Track("sp_$it", "Spotify $it", "Artist $it") }
        val ytTracks = (1..6).map { Track("yt_$it", "YT $it", "Artist $it") }

        val balanced = engine.interleaveByWeight(spotifyTracks, ytTracks, primaryWeight = 0.5f, limit = 10)
        val spCount = balanced.count { it.id.startsWith("sp_") }
        val ytCount = balanced.count { it.id.startsWith("yt_") }
        assertEquals(10, balanced.size)
        assertTrue("Expected balanced mix, got sp=$spCount yt=$ytCount", spCount in 4..6 && ytCount in 4..6)

        val heavySpotify = engine.interleaveByWeight(spotifyTracks, ytTracks, primaryWeight = 0.9f, limit = 8)
        assertTrue(heavySpotify.count { it.id.startsWith("sp_") } >= 5)
    }
}
