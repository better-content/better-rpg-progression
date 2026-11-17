package com.bettercontent.betterrpgprogression.common.network.packets

import com.bettercontent.betterrpgprogression.common.config.json.StatDefinition
import com.bettercontent.betterrpgprogression.common.reload.RegistryState
import com.bettercontent.betterrpgprogression.common.salience.AspectIdentity
import io.netty.buffer.Unpooled
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FiveLifeRequestTest {
    private val ids = listOf("power", "motion", "endurance", "renewal", "control")
    private val base = "better_rpg_progression:"

    @Test
    fun `all five Life identities are canonical and obsolete namespaces cannot alias them`() {
        assertEquals(ids, AspectIdentity.entries.map { it.statId })
        assertEquals(listOf(0xFF4055, 0xE0B01F, 0x52606A, 0x6CCAF0, 0x8E5BB7),
            AspectIdentity.entries.map { it.color })
        ids.forEach { assertNotNull(AspectIdentity.fromStatId(base + it)) }
        listOf("vanguard", "sharpshooter", "artisan", "trailblazer", "survivalist",
            "spellcaster", "skirmisher", "aerialist", "arms", "eyes").forEach {
            assertNull(AspectIdentity.fromStatId(base + it))
        }
        assertNull(AspectIdentity.fromStatId("minecraft:power"))
        assertNull(AspectIdentity.fromStatId("other:control"))
    }

    @Test
    fun `all choices remain visible even if an optional integration is unavailable`() {
        val before = RegistryState.snapshot()
        try {
            val def = ids.associate { id ->
                val key = ResourceLocation("better_rpg_progression", id)
                key to StatDefinition(key, "stat.better_rpg_progression.$id")
            }
            RegistryState.set(def)
            assertEquals(def, RegistryState.activeSnapshot())
        } finally {
            RegistryState.set(before)
        }
    }

    @Test
    fun `invalid wire counts cannot turn a valid Life spend into a partial grant`() {
        val buffer = FriendlyByteBuf(Unpooled.buffer())
        buffer.writeVarInt(2)
        buffer.writeUtf(base + "power"); buffer.writeVarInt(2)
        buffer.writeUtf(base + "power"); buffer.writeVarInt(3)
        val decoded = C2SApplyStats.decode(buffer)
        assertTrue(decoded.requested.containsKey(""), "Duplicated wire IDs must mark the whole request invalid")
        assertNull(AllocationPolicy.apply(emptyMap(), 5, decoded.requested, mapOf(base + "power" to -1)))

        val invalid = FriendlyByteBuf(Unpooled.buffer())
        invalid.writeVarInt(2)
        invalid.writeUtf(base + "power"); invalid.writeVarInt(1)
        invalid.writeUtf(base + "vanguard"); invalid.writeVarInt(0)
        val mixed = C2SApplyStats.decode(invalid)
        assertNull(AllocationPolicy.apply(emptyMap(), 5, mixed.requested, mapOf(base + "power" to -1)))
    }
}
