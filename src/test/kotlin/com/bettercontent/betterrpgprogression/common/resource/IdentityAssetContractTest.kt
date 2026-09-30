package com.bettercontent.betterrpgprogression.common.resource

import com.bettercontent.betterrpgprogression.common.salience.AspectIdentity
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.MessageDigest
import java.util.HexFormat
import javax.imageio.ImageIO

class IdentityAssetContractTest {
    private val motifHashes = mapOf(
        "impact" to "f4fc74364f8a7e91df3a1f9dd3c07fd4c9022b0fef00e221e80266b8edabc489",
        "tempo" to "eac16819679ac96f9fa3ddb2c965514a29f414a40e24d50a1463d642cce85412",
        "work" to "9037dfbca5376beecee554594140742365ae4c68283724df026893390d303ee7",
        "mobility" to "c35d440d3088381b0c4add94e32f42c9550dbb27a866c8b421e28ef9ee833ff3",
        "endurance" to "75a19e1418f7d03d4e1a04029edb8d36d4163ca2fda11e85124ba3d3b547113d",
        "robustness" to "6974a8030e6b6afe642be4d949bc5087f3400a13ffe5a5b98c42a46bff0bb7ff",
        "renewal" to "c5c657748621d9a2b327cf6ed6bff5d8d829205c71e8286aab28abbe7ac79db3",
        "control" to "c9ebb0fb9fadcc8b4aa8eb53328cd440e2e6f4b97a9313d88d37d5954f870579"
    )

    @Test
    fun `badge font and motifs cover the exact identity contract`() {
        val badge = resource("/assets/better_rpg_progression/textures/gui/aspect_badges.png")
        assertEquals("84bc0c5fe762fe2df5f1ed53a2e138ecb03bbfbb386657039ad8333927bf51ab",
            HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(badge)))
        ImageIO.read(badge.inputStream()).also { image -> assertEquals(144, image.width); assertEquals(18, image.height) }
        val font = resource("/assets/better_rpg_progression/font/aspects.json").decodeToString()
        assertTrue(font.contains(""))
        val sounds = resource("/assets/better_rpg_progression/sounds.json").decodeToString()
        AspectIdentity.entries.forEachIndexed { index, aspect ->
            assertEquals(index, aspect.index)
            val id = aspect.name.lowercase()
            assertTrue(sounds.contains("aspect.$id"))
            val ogg = resource("/assets/better_rpg_progression/sounds/aspect/$id.ogg")
            assertTrue(ogg.size > 4000)
            assertArrayEquals("OggS".encodeToByteArray(), ogg.copyOfRange(0, 4))
            assertEquals(motifHashes[id], HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(ogg)), id)
        }
    }

    private fun resource(path: String): ByteArray = javaClass.getResourceAsStream(path).also { assertNotNull(it, path) }!!.readAllBytes()
}
