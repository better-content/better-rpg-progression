package com.bettercontent.betterrpgprogression.common.network.packets

import com.bettercontent.betterrpgprogression.client.ui.AllocationRecap
import net.minecraft.network.FriendlyByteBuf
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.fml.DistExecutor
import net.minecraftforge.network.NetworkEvent
import java.util.function.Supplier

data class S2CAllocationResult(val deltas: Map<String, Int>) {
    companion object {
        fun encode(msg: S2CAllocationResult, buf: FriendlyByteBuf) {
            buf.writeVarInt(msg.deltas.size)
            msg.deltas.forEach { (id, amount) -> buf.writeUtf(id); buf.writeVarInt(amount) }
        }

        fun decode(buf: FriendlyByteBuf): S2CAllocationResult {
            val result = linkedMapOf<String, Int>()
            repeat(buf.readVarInt()) { result[buf.readUtf(32767)] = buf.readVarInt() }
            return S2CAllocationResult(result)
        }

        fun handle(msg: S2CAllocationResult, contextSupplier: Supplier<NetworkEvent.Context>) {
            val context = contextSupplier.get()
            context.enqueueWork {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT) { Runnable { AllocationRecap.accept(msg.deltas) } }
            }
            context.packetHandled = true
        }
    }
}
