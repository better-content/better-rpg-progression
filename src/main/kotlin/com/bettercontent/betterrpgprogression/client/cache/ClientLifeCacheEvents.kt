package com.bettercontent.betterrpgprogression.client.cache

import com.bettercontent.betterrpgprogression.RpgStatsMod
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.ClientPlayerNetworkEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

/** Never display a previous server's five Life choices or allocations during reconnect. */
@Mod.EventBusSubscriber(modid = RpgStatsMod.MODID, value = [Dist.CLIENT], bus = Mod.EventBusSubscriber.Bus.FORGE)
object ClientLifeCacheEvents {
    @SubscribeEvent
    fun onLogout(event: ClientPlayerNetworkEvent.LoggingOut) {
        ClientCache.defs = emptyList()
        ClientCache.stats = ClientStatsSnapshot()
    }
}
