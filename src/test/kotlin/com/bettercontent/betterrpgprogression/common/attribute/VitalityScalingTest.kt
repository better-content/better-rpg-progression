package com.bettercontent.betterrpgprogression.common.attribute

import com.bettercontent.betterrpgprogression.TestMinecraftBootstrap
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals

class VitalityScalingTest {
    @Test
    fun `duration copy preserves hidden effect curatives and separate visual flags`() {
        TestMinecraftBootstrap.bootstrap()
        val hidden = MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 1, true, false, true)
        val original = MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 2, false, true, false, hidden, Optional.empty())
        original.setCurativeItems(listOf(ItemStack(Items.HONEY_BOTTLE)))

        val expected = original.save(CompoundTag()).also { it.putInt("Duration", 120) }
        val copied = VitalityScaling.withDuration(original, 120)

        assertEquals(expected, copied.save(CompoundTag()))
        assertEquals(80, original.duration)
    }
}
