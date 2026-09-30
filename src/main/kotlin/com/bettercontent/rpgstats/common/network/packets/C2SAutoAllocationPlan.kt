package com.bettercontent.rpgstats.common.network.packets

import com.bettercontent.rpgstats.common.data.StatsCap
import com.bettercontent.rpgstats.common.network.Network
import com.bettercontent.rpgstats.common.points.AutoAllocationPlan
import com.bettercontent.rpgstats.common.reload.RegistryState
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.network.NetworkEvent
import java.util.function.Supplier

/** Replaces a character's plan atomically. Invalid/replayed input has no side effects. */
data class C2SAutoAllocationPlan(val enabled: Boolean, val orderedIds: List<String>) {
    companion object {
        fun encode(msg: C2SAutoAllocationPlan, buf: FriendlyByteBuf) {
            buf.writeBoolean(msg.enabled)
            buf.writeVarInt(msg.orderedIds.size)
            msg.orderedIds.forEach(buf::writeUtf)
        }

        fun decode(buf: FriendlyByteBuf): C2SAutoAllocationPlan {
            val enabled = buf.readBoolean()
            val count = buf.readVarInt()
            if (count !in 0..AutoAllocationPlan.MAX_ENTRIES) return C2SAutoAllocationPlan(false, listOf("invalid"))
            return C2SAutoAllocationPlan(enabled, MutableList(count) { buf.readUtf(128) })
        }

        fun handle(msg: C2SAutoAllocationPlan, ctx: Supplier<NetworkEvent.Context>) {
            val context = ctx.get()
            context.enqueueWork {
                val player: ServerPlayer = context.sender ?: return@enqueueWork
                val stats = StatsCap.get(player) ?: return@enqueueWork
                val admitted = RegistryState.activeSnapshot().mapKeys { it.key.toString() }.mapValues { it.value.maxPoints }
                val canonical = msg.orderedIds.mapNotNull { if (it.isEmpty()) "" else ResourceLocation.tryParse(it)?.toString() }
                if (canonical.size != msg.orderedIds.size || !AutoAllocationPlan.replace(stats, msg.enabled, canonical, admitted)) {
                    Network.syncTo(player)
                    return@enqueueWork
                }
                Network.syncTo(player)
            }
            context.packetHandled = true
        }
    }
}
