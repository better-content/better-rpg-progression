package com.bettercontent.rpgstats.common.network.packets

import com.bettercontent.rpgstats.client.cache.ClientCache
import com.bettercontent.rpgstats.client.cache.ClientStatsSnapshot
import net.minecraft.network.FriendlyByteBuf
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.fml.DistExecutor
import net.minecraftforge.network.NetworkEvent
import java.util.function.Supplier

data class S2CStatsSync(
    val unspent: Int,
    val lifePeak: Int,
    val allocations: Map<String, Int>,
    val autoAllocationEnabled: Boolean,
    val autoAllocationPlan: List<String>,
    val autoAllocationCursor: Int
) {
    companion object {
        fun encode(msg: S2CStatsSync, buf: FriendlyByteBuf) {
            buf.writeVarInt(msg.unspent)
            buf.writeVarInt(msg.lifePeak)
            buf.writeVarInt(msg.allocations.size)
            msg.allocations.forEach { (k, v) ->
                buf.writeUtf(k)
                buf.writeVarInt(v)
            }
            buf.writeBoolean(msg.autoAllocationEnabled)
            buf.writeVarInt(msg.autoAllocationPlan.size)
            msg.autoAllocationPlan.forEach(buf::writeUtf)
            buf.writeVarInt(msg.autoAllocationCursor)
        }

        fun decode(buf: FriendlyByteBuf): S2CStatsSync {
            val unspent = buf.readVarInt()
            val lifePeak = buf.readVarInt()
            val n = buf.readVarInt()
            val map = mutableMapOf<String, Int>()
            repeat(n) {
                val k = buf.readUtf(32767)
                val v = buf.readVarInt()
                if (v > 0) map[k] = v
            }
            val enabled = buf.readBoolean()
            val plan = MutableList(buf.readVarInt().coerceIn(0, 64)) { buf.readUtf(128) }
            return S2CStatsSync(unspent, lifePeak, map, enabled, plan, buf.readVarInt())
        }

        fun handle(msg: S2CStatsSync, ctx: Supplier<NetworkEvent.Context>) {
            ctx.get().enqueueWork {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT) {
                    Runnable {
                        ClientCache.stats = ClientStatsSnapshot(msg.unspent, msg.lifePeak, msg.allocations,
                            msg.autoAllocationEnabled, msg.autoAllocationPlan, msg.autoAllocationCursor)
                    }
                }
            }
            ctx.get().packetHandled = true
        }
    }
}
