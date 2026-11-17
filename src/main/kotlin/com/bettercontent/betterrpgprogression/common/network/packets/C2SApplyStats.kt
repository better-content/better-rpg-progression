package com.bettercontent.betterrpgprogression.common.network.packets

import com.bettercontent.betterrpgprogression.common.attribute.StatAttributeProjector
import com.bettercontent.betterrpgprogression.diagnostics.PlaytestDiagnostics
import com.bettercontent.betterrpgprogression.diagnostics.PlaytestLog
import com.bettercontent.betterrpgprogression.diagnostics.ProgressionOutcomes
import com.bettercontent.betterrpgprogression.api.event.LifeAllocationEvent
import com.bettercontent.betterrpgprogression.common.points.LifeAllocationEvents
import com.bettercontent.betterrpgprogression.common.data.StatsCap
import com.bettercontent.betterrpgprogression.common.network.Network
import com.bettercontent.betterrpgprogression.common.reload.RegistryState
import net.minecraft.network.FriendlyByteBuf
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
            if (n !in 0..256) return C2SApplyStats(mapOf("" to -1))
            val map = mutableMapOf<String, Int>()
            var invalid = false
            repeat(n) {
                val k = buf.readUtf(128)
                val v = buf.readVarInt()
                if (k in map || v <= 0) invalid = true
                map[k] = v
            }
            // Preserve invalid/duplicate input as one rejected request, not a valid subset.
            if (invalid) map[""] = -1
            return C2SApplyStats(map)
        }

        fun handle(msg: C2SApplyStats, ctx: Supplier<NetworkEvent.Context>) {
            val context = ctx.get()
            context.enqueueWork {
                val sender: ServerPlayer = context.sender ?: return@enqueueWork
                val stats = StatsCap.get(sender) ?: return@enqueueWork
                val defs = RegistryState.activeSnapshot()

                // AllocationPolicy rejects the entire packet for ANY unknown/stale/invalid
                // entry. Never turn a mixed old+new request into a partial committed grant.
                val decision = AllocationPolicy.apply(
                    current = stats.allocations,
                    unspentPoints = stats.unspentPoints,
                    requested = msg.requested,
                    maxPointsById = defs.mapKeys { it.key.toString() }.mapValues { it.value.maxPoints }
                )
                if (decision == null) {
                    if (PlaytestDiagnostics.LOG.enabled("allocation")) PlaytestDiagnostics.LOG.event(
                        "allocation", "allocation.manual", "server", ProgressionOutcomes.manual(false, 0), "allocation_policy", null)
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
                if (PlaytestDiagnostics.LOG.enabled("allocation")) PlaytestDiagnostics.LOG.event(
                    "allocation", "allocation.manual", "server", ProgressionOutcomes.manual(true, deltas.size),
                    "stats_updated_sync_submitted", { PlaytestLog.fields("changed_stats", deltas.size, "unspent", stats.unspentPoints) })
            }
            context.packetHandled = true
        }
    }
}
