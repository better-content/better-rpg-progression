package com.bettercontent.betterrpgprogression.common.data

import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.common.util.INBTSerializable
import net.minecraftforge.common.util.LazyOptional

class PlayerStatsProvider : ICapabilityProvider, INBTSerializable<CompoundTag> {
    private val stats = PlayerStats()
    private val opt = LazyOptional.of { stats }

    override fun <T> getCapability(cap: net.minecraftforge.common.capabilities.Capability<T>, side: Direction?): LazyOptional<T> {
        return if (cap === StatsCap.CAP) opt.cast() else LazyOptional.empty()
    }

    override fun serializeNBT(): CompoundTag = stats.serializeNBT()

    override fun deserializeNBT(nbt: CompoundTag) {
        stats.deserializeNBT(nbt)
    }

    fun data(): PlayerStats = stats
}
