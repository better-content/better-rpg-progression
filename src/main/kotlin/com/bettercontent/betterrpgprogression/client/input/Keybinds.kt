package com.bettercontent.betterrpgprogression.client.input

import com.bettercontent.betterrpgprogression.RpgStatsMod
import com.bettercontent.betterrpgprogression.client.ui.StatsScreen
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RegisterKeyMappingsEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.client.settings.KeyConflictContext
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = RpgStatsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object KeybindRegister {
    private const val OPEN_STATS_KEY = "key.better_rpg_progression.open_stats"
    private const val KEY_CATEGORY = "key.categories.better_rpg_progression"

    private val OPEN_STATS = KeyMapping(
        OPEN_STATS_KEY,
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        InputConstants.KEY_O,
        KEY_CATEGORY
    )

    @SubscribeEvent
    fun onRegisterKeybinds(event: RegisterKeyMappingsEvent) {
        event.register(OPEN_STATS)
    }

    fun mapping(): KeyMapping = OPEN_STATS
}

@Mod.EventBusSubscriber(modid = RpgStatsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = [Dist.CLIENT])
object Keybinds {
    fun init() {
        // no-op; events handle everything.
    }

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        val mc = Minecraft.getInstance()
        if (mc.player == null) return

        if (KeybindRegister.mapping().consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(StatsScreen())
            } else if (mc.screen is StatsScreen) {
                mc.setScreen(null)
            }
        }
    }
}
