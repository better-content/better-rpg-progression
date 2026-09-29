package com.bettercontent.rpgstats.common.network

import com.bettercontent.rpgstats.RpgStatsMod
import com.bettercontent.rpgstats.common.data.StatsCap
import com.bettercontent.rpgstats.common.network.packets.C2SApplyStats
import com.bettercontent.rpgstats.common.network.packets.C2SAutoAllocationPlan
import com.bettercontent.rpgstats.common.network.packets.S2CStatDefsSync
import com.bettercontent.rpgstats.common.network.packets.S2CStatsSync
import com.bettercontent.rpgstats.common.network.packets.S2CAllocationResult
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.network.NetworkRegistry
import net.minecraftforge.network.PacketDistributor
import net.minecraftforge.network.simple.SimpleChannel

object Network {
    private const val PROTOCOL = "3"

    val CHANNEL: SimpleChannel = NetworkRegistry.newSimpleChannel(
        ResourceLocation(RpgStatsMod.MODID, "main"),
        { PROTOCOL },
        PROTOCOL::equals,
        PROTOCOL::equals
    )

    fun init() {
        var id = 0
        CHANNEL.registerMessage(id++, S2CStatDefsSync::class.java, S2CStatDefsSync::encode, S2CStatDefsSync::decode, S2CStatDefsSync::handle)
        CHANNEL.registerMessage(id++, S2CStatsSync::class.java, S2CStatsSync::encode, S2CStatsSync::decode, S2CStatsSync::handle)
        CHANNEL.registerMessage(id++, C2SApplyStats::class.java, C2SApplyStats::encode, C2SApplyStats::decode, C2SApplyStats::handle)
        CHANNEL.registerMessage(id++, C2SAutoAllocationPlan::class.java, C2SAutoAllocationPlan::encode, C2SAutoAllocationPlan::decode, C2SAutoAllocationPlan::handle)
        CHANNEL.registerMessage(id++, S2CAllocationResult::class.java, S2CAllocationResult::encode, S2CAllocationResult::decode, S2CAllocationResult::handle)
    }

    fun sendToServer(msg: Any) {
        CHANNEL.sendToServer(msg)
    }

    fun sendTo(player: ServerPlayer, msg: Any) {
        CHANNEL.send(PacketDistributor.PLAYER.with { player }, msg)
    }

    fun sendToAll(msg: Any) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), msg)
    }

    fun syncTo(player: ServerPlayer) {
        val stats = StatsCap.get(player) ?: return
        sendTo(
            player,
            S2CStatsSync(
                unspent = stats.unspentPoints,
                lifePeak = stats.lifePeakLevel,
                allocations = stats.allocations.toMap(),
                autoAllocationEnabled = stats.autoAllocationEnabled,
                autoAllocationPlan = stats.autoAllocationPlan.toList(),
                autoAllocationCursor = stats.autoAllocationCursor
            )
        )
    }
}
