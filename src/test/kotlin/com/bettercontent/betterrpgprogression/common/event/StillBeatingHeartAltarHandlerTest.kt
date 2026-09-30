package com.bettercontent.betterrpgprogression.common.event

import com.bettercontent.betterrpgprogression.TestMinecraftBootstrap
import com.bettercontent.betterrpgprogression.common.item.StillBeatingHeartData
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.junit.jupiter.api.BeforeAll
import kotlin.test.Test
import kotlin.test.assertEquals

class StillBeatingHeartAltarHandlerTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() {
            TestMinecraftBootstrap.bootstrap()
        }
    }

    @Test
    fun `valid heart in first slot fills altar by stored level`() {
        val container = SimpleContainer(StillBeatingHeartData.createForLevel(20, Items.DIAMOND))
        var requestedLp = 0

        val inserted = StillBeatingHeartAltarHandler.fillHeartContainerForTests(container) { amount ->
            requestedLp = amount
            amount
        }

        assertEquals(20, requestedLp)
        assertEquals(20, inserted)
    }

    @Test
    fun `invalid or absent heart does not fill altar`() {
        listOf(
            SimpleContainer(ItemStack.EMPTY),
            SimpleContainer(ItemStack(Items.DIAMOND)),
            SimpleContainer(StillBeatingHeartData.createForLevel(0, Items.DIAMOND)),
            SimpleContainer(0)
        ).forEach { container ->
            var called = false
            val inserted = StillBeatingHeartAltarHandler.fillHeartContainerForTests(container) {
                called = true
                it
            }

            assertEquals(0, inserted)
            assertEquals(false, called)
        }
    }

    @Test
    fun `empty altar tank reports no insertion without mutating heart`() {
        val heart = StillBeatingHeartData.createForLevel(10, Items.DIAMOND)
        val container = SimpleContainer(heart)

        val inserted = StillBeatingHeartAltarHandler.fillHeartContainerForTests(container) { 0 }

        assertEquals(0, inserted)
        assertEquals(1, container.getItem(0).count)
    }
}
