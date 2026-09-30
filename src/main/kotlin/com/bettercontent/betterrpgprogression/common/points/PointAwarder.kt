package com.bettercontent.betterrpgprogression.common.points

import com.bettercontent.betterrpgprogression.common.data.StatsCap
import com.bettercontent.betterrpgprogression.api.event.LifeAllocationEvent
import com.bettercontent.betterrpgprogression.common.network.Network
import com.bettercontent.betterrpgprogression.common.network.packets.S2CAllocationResult
import com.bettercontent.betterrpgprogression.common.attribute.StatAttributeProjector
import com.bettercontent.betterrpgprogression.common.reload.RegistryState
import net.minecraft.server.level.ServerPlayer

object PointAwarder {

    /**
     * Award +1 point for each level above the lifePeakLevel.
     * This runs on server tick.
     */
    fun tick(player: ServerPlayer) {
        val stats = StatsCap.get(player) ?: return
        val cur = player.experienceLevel
        if (cur > stats.lifePeakLevel) {
            val previousPeak = stats.lifePeakLevel
            val diff = cur - previousPeak
            stats.lifePeakLevel = cur
            stats.unspentPoints += diff
            val spent = AutoAllocationPlan.apply(stats, RegistryState.activeSnapshot().mapKeys { it.key.toString() }
                .mapValues { it.value.maxPoints }, previousPeak + 1, cur)
            if (spent.isNotEmpty()) {
                StatAttributeProjector.reapply(player)
                Network.sendTo(player, S2CAllocationResult(spent))
            }
            LifeAllocationEvents.beginEpisode(stats, LifeAllocationEvents.newEpisode(player))?.let { episodeId ->
                LifeAllocationEvents.post(player, LifeAllocationEvent.State.AVAILABLE, episodeId)
            }
            Network.syncTo(player)
        }
    }
}
