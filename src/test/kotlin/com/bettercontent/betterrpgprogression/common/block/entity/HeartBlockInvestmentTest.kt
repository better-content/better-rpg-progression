package com.bettercontent.betterrpgprogression.common.block.entity

import net.minecraft.nbt.CompoundTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HeartBlockInvestmentTest {
    @Test
    fun `investment survives block entity save and reload tag`() {
        val saved = CompoundTag()
        HeartBlockInvestment.write(saved, 37)

        assertEquals(37, HeartBlockInvestment.read(saved))
    }

    @Test
    fun `corrupt negative investment cannot create LP`() {
        val saved = CompoundTag()
        saved.putLong(HeartBlockEntity.FRAGMENTS_TAG, -7)

        assertEquals(0, HeartBlockInvestment.read(saved))
    }

    @Test
    fun `one-fragment insertion rejects only representational overflow`() {
        assertEquals(4, HeartBlockInvestment.addOne(3))
        assertEquals(1, HeartBlockInvestment.addOne(-4))
        assertNull(HeartBlockInvestment.addOne(Long.MAX_VALUE))
    }
}
