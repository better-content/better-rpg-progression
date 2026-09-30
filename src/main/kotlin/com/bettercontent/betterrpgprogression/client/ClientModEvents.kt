package com.bettercontent.betterrpgprogression.client

import com.bettercontent.betterrpgprogression.RpgStatsMod
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent

@Mod.EventBusSubscriber(modid = RpgStatsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object ClientModEvents {
    @SubscribeEvent
    fun onClientSetup(event: FMLClientSetupEvent) {
        // Run on main thread after setup.
        event.enqueueWork {
            ClientInit.init()
        }
    }
}
