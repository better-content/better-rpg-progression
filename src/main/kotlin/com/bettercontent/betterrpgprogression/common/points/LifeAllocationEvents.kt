package com.bettercontent.betterrpgprogression.common.points

import com.bettercontent.betterrpgprogression.api.event.LifeAllocationEvent
import com.bettercontent.betterrpgprogression.common.data.PlayerStats
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.common.MinecraftForge

internal object LifeAllocationEvents {
    fun newEpisode(player: ServerPlayer): String = "${player.uuid}:life:${player.server.tickCount}"

    fun beginEpisode(stats: PlayerStats, episodeId: String): String? {
        if (stats.lifeAllocationEpisode != null) return null
        stats.lifeAllocationEpisode = episodeId
        return episodeId
    }

    fun post(player: ServerPlayer, state: LifeAllocationEvent.State, episodeId: String) {
        MinecraftForge.EVENT_BUS.post(LifeAllocationEvent(player, state, episodeId))
    }
}
