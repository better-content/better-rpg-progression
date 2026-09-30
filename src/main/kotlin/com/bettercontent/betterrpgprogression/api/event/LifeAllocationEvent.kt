package com.bettercontent.betterrpgprogression.api.event

import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.eventbus.api.Event

/** Posted after the server changes Life allocations, including a final death reset. */
class LifeAllocationEvent(
    val player: ServerPlayer,
    val state: State,
    val episodeId: String
) : Event() {
    enum class State {
        AVAILABLE,
        SPENT,
        LOST_ON_DEATH
    }
}
