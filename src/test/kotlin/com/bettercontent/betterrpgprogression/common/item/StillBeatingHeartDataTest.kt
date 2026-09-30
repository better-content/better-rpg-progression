package com.bettercontent.betterrpgprogression.common.item

import com.bettercontent.betterrpgprogression.TestMinecraftBootstrap
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.junit.jupiter.api.BeforeAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StillBeatingHeartDataTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() {
            TestMinecraftBootstrap.bootstrap()
        }
    }

    @Test
    fun `new hearts write only schema and level`() {
        val stack = StillBeatingHeartData.createForLevel(17, Items.DIAMOND)
        val data = assertNotNull(StillBeatingHeartData.getData(stack))

        assertEquals(3, data.getInt("schema_version"))
        assertEquals(17, data.getInt("level"))
        assertEquals(setOf("schema_version", "level"), data.allKeys)
    }

    @Test
    fun `level reads current schema only`() {
        val modern = StillBeatingHeartData.createForLevel(24, Items.DIAMOND)
        assertEquals(24, StillBeatingHeartData.getLevel(modern))

        val legacyData = CompoundTag()
        val legacyPlayer = CompoundTag()
        legacyPlayer.putInt("experience_level", 31)
        legacyData.put("player", legacyPlayer)
        val legacy = ItemStack(Items.DIAMOND)
        legacy.orCreateTag.put(StillBeatingHeartData.DATA_TAG, legacyData)

        assertEquals(0, StillBeatingHeartData.getLevel(legacy))
    }

    @Test
    fun `invalid stacks are not valid blood sources`() {
        assertFalse(StillBeatingHeartData.isValid(ItemStack.EMPTY))
        assertFalse(StillBeatingHeartData.isValid(ItemStack(Items.DIAMOND)))
        assertEquals(0, StillBeatingHeartData.getLevel(ItemStack(Items.DIAMOND)))
    }

    @Test
    fun `lp formula is stable and clamps high levels`() {
        assertEquals(0, StillBeatingHeartData.lpPerTick(0))
        assertEquals(5, StillBeatingHeartData.lpPerTick(1))
        assertEquals(10, StillBeatingHeartData.lpPerTick(10))
        assertEquals(20, StillBeatingHeartData.lpPerTick(20))
        assertEquals(4096, StillBeatingHeartData.lpPerTick(200))
    }

    @Test
    fun `negative levels are normalized on write`() {
        val stack = StillBeatingHeartData.createForLevel(-10, Items.DIAMOND)
        assertFalse(StillBeatingHeartData.isValid(stack))
        assertTrue(stack.isEmpty)
        assertEquals(0, StillBeatingHeartData.getLevel(stack))
    }

    @Test
    fun `level zero does not create a still beating heart`() {
        val stack = StillBeatingHeartData.createForLevel(0, Items.DIAMOND)

        assertTrue(stack.isEmpty)
        assertFalse(StillBeatingHeartData.isValid(stack))
        assertEquals(0, StillBeatingHeartData.lpPerTick(stack))
    }

    @Test
    fun `captured death level survives another handler clearing player xp`() {
        val persistentData = CompoundTag()

        StillBeatingHeartData.captureDeathLevel(persistentData, 2)
        val playerLevelAfterConfigurableDeath = 0

        assertEquals(0, playerLevelAfterConfigurableDeath)
        assertEquals(2, StillBeatingHeartData.consumeCapturedDeathLevel(persistentData))
        assertEquals(0, StillBeatingHeartData.consumeCapturedDeathLevel(persistentData))
    }
}
