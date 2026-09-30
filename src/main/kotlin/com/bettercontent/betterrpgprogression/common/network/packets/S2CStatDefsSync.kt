package com.bettercontent.betterrpgprogression.common.network.packets

import com.bettercontent.betterrpgprogression.client.cache.ClientCache
import com.bettercontent.betterrpgprogression.client.cache.ClientCurveDef
import com.bettercontent.betterrpgprogression.client.cache.ClientEffectDef
import com.bettercontent.betterrpgprogression.client.cache.ClientStatDef
import com.bettercontent.betterrpgprogression.common.config.json.AttributeEffect
import com.bettercontent.betterrpgprogression.common.config.json.StatDefinition
import com.bettercontent.betterrpgprogression.common.attribute.EffectAvailability
import net.minecraft.network.FriendlyByteBuf
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.fml.DistExecutor
import net.minecraftforge.network.NetworkEvent
import java.util.function.Supplier

data class S2CStatDefsSync(
    val defs: List<ClientStatDef>
) {
    companion object {
        private fun parseColor(hex: String): Int {
            return try {
                val cleanHex = hex.removePrefix("#")
                val rgb = cleanHex.toInt(16)
                // Add full alpha channel (0xFF) to the front for ARGB format
                0xFF000000.toInt() or rgb
            } catch (e: Exception) {
                0xFFFFFFFF.toInt() // White with full alpha
            }
        }

        fun fromDefs(defs: List<StatDefinition>): S2CStatDefsSync {
            val list = defs.map { d ->
                val effs = d.effects.mapNotNull { e ->
                    if (e is AttributeEffect && EffectAvailability.isAvailable(e)) {
                        ClientEffectDef(
                            attributeId = e.attributeId.toString(),
                            operation = e.operation.ordinal,
                            curve = ClientCurveDef(
                                type = e.curve.type,
                                cap = e.curve.cap,
                                k = e.curve.k,
                                perPoint = e.curve.perPoint,
                                min = e.curve.min,
                                max = e.curve.max
                            ),
                            isPrimary = e.isPrimary,
                            displayAsPercent = e.displayAsPercent
                        )
                    } else null
                }
                ClientStatDef(
                    id = d.id.toString(),
                    nameKey = d.nameKey,
                    maxPoints = d.maxPoints,
                    effects = effs,
                    icon = d.icon,
                    color = parseColor(d.color)
                )
            }
            return S2CStatDefsSync(list)
        }

        fun encode(msg: S2CStatDefsSync, buf: FriendlyByteBuf) {
            buf.writeVarInt(msg.defs.size)
            msg.defs.forEach { d ->
                buf.writeUtf(d.id)
                buf.writeUtf(d.nameKey)
                buf.writeVarInt(d.maxPoints)
                buf.writeUtf(d.icon)
                buf.writeVarInt(d.color)
                buf.writeVarInt(d.effects.size)
                d.effects.forEach { e ->
                    buf.writeUtf(e.attributeId)
                    buf.writeVarInt(e.operation)
                    buf.writeBoolean(e.isPrimary)
                    buf.writeBoolean(e.displayAsPercent)
                    buf.writeUtf(e.curve.type)
                    buf.writeDouble(e.curve.cap)
                    buf.writeDouble(e.curve.k)
                    buf.writeDouble(e.curve.perPoint)
                    buf.writeDouble(e.curve.min)
                    buf.writeDouble(e.curve.max)
                }
            }
        }

        fun decode(buf: FriendlyByteBuf): S2CStatDefsSync {
            val n = buf.readVarInt()
            val list = ArrayList<ClientStatDef>(n)
            repeat(n) {
                val id = buf.readUtf(32767)
                val nameKey = buf.readUtf(32767)
                val maxPoints = buf.readVarInt()
                val icon = buf.readUtf(32767)
                val color = buf.readVarInt()
                val eN = buf.readVarInt()
                val effs = ArrayList<ClientEffectDef>(eN)
                repeat(eN) {
                    val attr = buf.readUtf(32767)
                    val op = buf.readVarInt()
                    val isPrimary = buf.readBoolean()
                    val displayAsPercent = buf.readBoolean()
                    val t = buf.readUtf(32767)
                    val cap = buf.readDouble()
                    val k = buf.readDouble()
                    val per = buf.readDouble()
                    val min = buf.readDouble()
                    val max = buf.readDouble()
                    effs += ClientEffectDef(
                        attr,
                        op,
                        ClientCurveDef(t, cap, k, per, min, max),
                        isPrimary,
                        displayAsPercent
                    )
                }
                list += ClientStatDef(id, nameKey, maxPoints, effs, icon, color)
            }
            return S2CStatDefsSync(list)
        }

        fun handle(msg: S2CStatDefsSync, ctx: Supplier<NetworkEvent.Context>) {
            ctx.get().enqueueWork {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT) {
                    Runnable {
                        ClientCache.defs = msg.defs
                    }
                }
            }
            ctx.get().packetHandled = true
        }
    }
}
