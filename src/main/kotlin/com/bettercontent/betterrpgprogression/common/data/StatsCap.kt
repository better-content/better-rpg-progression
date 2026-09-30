package com.bettercontent.betterrpgprogression.common.data

import com.bettercontent.betterrpgprogression.RpgStatsMod
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityManager
import net.minecraftforge.common.capabilities.CapabilityToken
import net.minecraftforge.common.util.LazyOptional

object StatsCap {
    val CAP: Capability<PlayerStats> = CapabilityManager.get(object : CapabilityToken<PlayerStats>() {})
    val KEY: ResourceLocation = ResourceLocation(RpgStatsMod.MODID, "player_stats")

    fun get(player: Player): PlayerStats? = player.getCapability(CAP).resolve().orElse(null)

    fun opt(player: Player): LazyOptional<PlayerStats> = player.getCapability(CAP)
}
