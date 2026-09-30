package com.bettercontent.betterrpgprogression.common.network.packets

import com.bettercontent.betterrpgprogression.common.attribute.StatAttributeProjector
import com.bettercontent.betterrpgprogression.api.event.LifeAllocationEvent
import com.bettercontent.betterrpgprogression.common.points.LifeAllocationEvents
import com.bettercontent.betterrpgprogression.common.data.StatsCap
import com.bettercontent.betterrpgprogression.common.network.Network
import com.bettercontent.betterrpgprogression.common.reload.RegistryState
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.network.NetworkEvent
import java.util.function.Supplier
import kotlin.math.max

data class C2SApplyStats(
    val requested: Map<String, Int>
) {
    companion object {
        fun encode(msg: C2SApplyStats, buf: FriendlyByteBuf) {
            buf.writeVarInt(msg.requested.size)
            msg.requested.forEach { (k, v) ->
                buf.writeUtf(k)
                buf.writeVarInt(max(0, v))
            }
        }

        fun decode(buf: FriendlyByteBuf): C2SApplyStats {
            val n = buf.readVarInt()
            val map = mutableMapOf<String, Int>()
            repeat(n) {
                val k = buf.readUtf(32767)
                val v = buf.readVarInt()
                if (v > 0) map[k] = v
            }
            return C2SApplyStats(map)
        }

        fun handle(msg: C2SApplyStats, ctx: Supplier<NetworkEvent.Context>) {
            val context = ctx.get()
            context.enqueueWork {
                val sender: ServerPlayer = context.sender ?: return@enqueueWork
                val stats = StatsCap.get(sender) ?: return@enqueueWork
                val defs = RegistryState.activeSnapshot()

                val requestedKnown = mutableMapOf<String, Int>()
                for ((idStr, ptsRaw) in msg.requested) {
                    val id = ResourceLocation.tryParse(idStr) ?: continue
                    if (id !in defs) continue
                    if (ptsRaw > 0) requestedKnown[id.toString()] = ptsRaw
                }

                val decision = AllocationPolicy.apply(
                    current = stats.allocations,
                    unspentPoints = stats.unspentPoints,
                    requested = requestedKnown,
                    maxPointsById = defs.mapKeys { it.key.toString() }.mapValues { it.value.maxPoints }
                )
                if (decision == null) {
                    Network.syncTo(sender)
                    return@enqueueWork
                }

                val deltas = decision.allocations.mapValues { (id, points) -> points - (stats.allocations[id] ?: 0) }
                    .filterValues { it > 0 }

                stats.allocations.clear()
                stats.allocations.putAll(decision.allocations)
                stats.unspentPoints = decision.unspentPoints

                StatAttributeProjector.reapply(sender)
                Network.syncTo(sender)
                Network.sendTo(sender, S2CAllocationResult(deltas))
                if (deltas.isNotEmpty()) stats.lifeAllocationEpisode?.let { token ->
                    LifeAllocationEvents.post(sender, LifeAllocationEvent.State.SPENT, token)
                    stats.lifeAllocationEpisode = null
                }
            }
            context.packetHandled = true
        }
    }
}
