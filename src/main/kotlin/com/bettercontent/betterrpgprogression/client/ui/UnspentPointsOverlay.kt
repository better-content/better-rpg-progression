package com.bettercontent.betterrpgprogression.client.ui

import com.bettercontent.betterrpgprogression.RpgStatsMod
import com.bettercontent.betterrpgprogression.client.cache.ClientCache
import com.bettercontent.betterrpgprogression.client.input.KeybindRegister
import com.bettercontent.gameplaynotices.GameplayNotice
import com.bettercontent.gameplaynotices.NoticeReminder
import com.bettercontent.gameplaynotices.NoticeTheme
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = RpgStatsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = [Dist.CLIENT])
object UnspentPointsOverlay {
    private const val NOTICE_KEY = "guidance:life:unspent"
    private val reminder = NoticeReminder()

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        val mc = Minecraft.getInstance()
        val player = mc.player
        if (player == null || mc.level == null || ClientCache.stats.unspent <= 0) {
            reminder.clear()
            return
        }
        val notice = GameplayNotice(NOTICE_KEY, NoticeTheme.LIFE,
            Component.translatable("overlay.better_rpg_progression.unspent_points", ClientCache.stats.unspent),
            Component.translatable("overlay.better_rpg_progression.press_key", KeybindRegister.mapping().translatedKeyMessage))
        // Spectator onboarding guidance has priority; do not replace its one transient slot.
        reminder.tick(NOTICE_KEY, notice, { mc.player != null && mc.level != null &&
            ClientCache.stats.unspent > 0 && mc.player?.isSpectator != true },
            mc.screen == null && !mc.options.hideGui && !player.isSpectator)
    }
}
