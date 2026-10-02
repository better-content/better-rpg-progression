package com.bettercontent.betterrpgprogression.harness

import com.bettercontent.betterrpgprogression.client.cache.ClientCache
import com.bettercontent.betterrpgprogression.client.cache.ClientCurveDef
import com.bettercontent.betterrpgprogression.client.cache.ClientEffectDef
import com.bettercontent.betterrpgprogression.client.cache.ClientStatDef
import com.bettercontent.betterrpgprogression.client.cache.ClientStatsSnapshot
import com.bettercontent.betterrpgprogression.client.ui.StatsScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.Screenshot
import net.minecraft.client.gui.components.Button
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import com.bettercontent.betterrpgprogression.RpgStatsMod

/**
 * Development-only visual fixture. The dedicated runVisualHarness Gradle run includes this
 * source set alongside RPG Stats and captures the stats screen without a modpack or world.
 */
@Mod.EventBusSubscriber(modid = RpgStatsMod.MODID, value = [Dist.CLIENT], bus = Mod.EventBusSubscriber.Bus.FORGE)
object RpgStatsVisualHarness {
    private val heartCaptureTicks = setOf(1, 13, 25, 37)
    private var opened = false
    private var startupTicks = 0
    private var screenTicks = 0
    private var statsCaptures = 0
    private var soundScreenTicks = 0
    private var soundCaptured = false
    private var heartScreenTicks = 0
    private var heartCaptures = 0
    private var ticksAfterCapture = 0

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END) return

        val minecraft = Minecraft.getInstance()
        if (!opened) {
            if (minecraft.overlay == null && minecraft.screen != null) startupTicks++
            if (startupTicks >= 20) {
                seedFixture()
                ClientCache.stats = ClientStatsSnapshot(unspent = 4, lifePeak = 27, allocations = emptyMap())
                minecraft.setScreen(StatsScreen())
                opened = true
                println("RPG_STATS_VISUAL_HARNESS empty-compact-stats-ready")
            }
            return
        }

        if (statsCaptures < 3 && minecraft.screen is StatsScreen) {
            screenTicks++
            if (screenTicks >= 40) {
                Screenshot.grab(minecraft.gameDirectory, minecraft.mainRenderTarget) { message ->
                    val state = when (statsCaptures) {
                        0 -> "empty gui-scale=3"
                        1 -> "pending gui-scale=3"
                        else -> "pending gui-scale=2"
                    }
                    println("RPG_STATS_VISUAL_HARNESS stats-screenshot $state $message")
                }
                statsCaptures++
                screenTicks = 0
                if (statsCaptures == 1) {
                    seedStats()
                    val screen = StatsScreen()
                    minecraft.setScreen(screen)
                    screen.children().filterIsInstance<Button>().lastOrNull { it.message.string == "+" }?.onPress()
                    println("RPG_STATS_VISUAL_HARNESS pending-compact-stats-ready")
                } else if (statsCaptures == 2) {
                    minecraft.options.guiScale().set(2)
                    minecraft.resizeDisplay()
                    val screen = StatsScreen()
                    minecraft.setScreen(screen)
                    screen.children().filterIsInstance<Button>().lastOrNull { it.message.string == "+" }?.onPress()
                    println("RPG_STATS_VISUAL_HARNESS expanded-stats-ready")
                } else {
                    minecraft.setScreen(IdentitySoundReviewScreen())
                    println("RPG_STATS_VISUAL_HARNESS sound-review-ready")
                }
            }
            return
        }

        if (!soundCaptured && minecraft.screen is IdentitySoundReviewScreen) {
            soundScreenTicks++
            if (soundScreenTicks % 12 == 1) {
                val index = soundScreenTicks / 12
                com.bettercontent.betterrpgprogression.common.salience.AspectIdentity.entries.getOrNull(index)?.let {
                    (minecraft.screen as IdentitySoundReviewScreen).play(it)
                }
            }
            if (soundScreenTicks >= 100) {
                Screenshot.grab(minecraft.gameDirectory, minecraft.mainRenderTarget) { message ->
                    println("RPG_STATS_VISUAL_HARNESS sound-review-screenshot $message")
                }
                soundCaptured = true
                minecraft.setScreen(StillBeatingHeartAnimationScreen())
                println("RPG_STATS_VISUAL_HARNESS heart-screen-ready")
            }
            return
        }

        if (minecraft.screen is StillBeatingHeartAnimationScreen && heartCaptures < heartCaptureTicks.size) {
            heartScreenTicks++
            if (heartScreenTicks in heartCaptureTicks) {
                Screenshot.grab(minecraft.gameDirectory, minecraft.mainRenderTarget) { message ->
                    println("RPG_STATS_VISUAL_HARNESS heart-screenshot tick=$heartScreenTicks $message")
                }
                heartCaptures++
            }
            return
        }

        if (heartCaptures == heartCaptureTicks.size) {
            ticksAfterCapture++
            if (ticksAfterCapture >= 40) minecraft.stop()
        }
    }

    private fun seedFixture() {
        ClientCache.defs = listOf(
            stat(
                "arms", "✦", 0xFF4055,
                effect("minecraft:generic.attack_damage", 0, 6.0),
                effect("epicfight:impact", 0, 0.75, primary = false),
                effect("minecraft:generic.attack_knockback", 0, 0.4, primary = false)
            ),
            stat(
                "hands", "»", 0x00A985,
                effect("minecraft:generic.attack_speed", 0, 0.6),
                effect("tconstruct:player.use_item_speed", 1, 0.25, primary = false)
            ),
            stat("fingers", "⚒", 0xF0E2C5, effect("better_rpg_progression:mining_speed", 1, 0.75), effect("forge:block_reach", 0, 1.0, primary = false)),
            stat("lungs", "➜", 0xE0B01F, effect("minecraft:generic.movement_speed", 0, 0.05), effect("forge:swim_speed", 1, 0.25, primary = false), effect("forge:step_height_addition", 0, 0.5, primary = false), effect("better_rpg_progression:air_capacity", 1, 1.0, primary = false)),
            stat(
                "blood", "∞", 0x52606A,
                effect("better_rpg_progression:hunger_efficiency", 1, 1.0),
                effect("better_rpg_progression:thirst_efficiency", 1, 1.0, primary = false),
                effect("epicfight:staminar", 1, 0.3, primary = false)
            ),
            stat(
                "skin", "◆", 0xAF6A2F,
                effect("cold_sweat:heat_resistance", 0, 0.6, displayAsPercent = true),
                effect("cold_sweat:cold_resistance", 0, 0.6, primary = false, displayAsPercent = true)
            ),
            stat(
                "liver", "✚", 0x6CCAF0,
                effect("better_rpg_progression:harmful_effect_duration_reduction", 0, 0.25, displayAsPercent = true),
                effect("better_rpg_progression:beneficial_effect_duration", 0, 0.25, primary = false, displayAsPercent = true)
            ),
            stat(
                "eyes", "⊕", 0x8E5BB7,
                effect("better_rpg_progression:arrow_spread_reduction", 0, 0.35, displayAsPercent = true),
                effect("better_rpg_progression:arrow_speed_bonus", 0, 0.08, primary = false, displayAsPercent = true),
                effect("better_rpg_progression:recoil_reduction", 0, 0.35, primary = false, displayAsPercent = true),
                effect("better_rpg_progression:dispersion_reduction", 0, 0.35, primary = false, displayAsPercent = true),
                effect("goety:spell_range", 1, 0.25, primary = false)
            )
        )
        seedStats()
    }

    private fun seedStats() {
        ClientCache.stats = ClientStatsSnapshot(
            unspent = 4,
            lifePeak = 27,
            allocations = mapOf(
                "better_rpg_progression:arms" to 4,
                "better_rpg_progression:hands" to 2,
                "better_rpg_progression:fingers" to 3,
                "better_rpg_progression:lungs" to 1,
                "better_rpg_progression:blood" to 2,
                "better_rpg_progression:skin" to 2,
                "better_rpg_progression:liver" to 2,
                "better_rpg_progression:eyes" to 1
            )
        )
    }

    private fun stat(id: String, icon: String, color: Int, vararg effects: ClientEffectDef): ClientStatDef =
        ClientStatDef(
            id = "better_rpg_progression:$id",
            nameKey = "stat.better_rpg_progression.${visibleNames.getValue(id)}",
            maxPoints = -1,
            effects = effects.toList(),
            icon = icon,
            color = color
        )

    private val visibleNames = mapOf(
        "arms" to "arms",
        "hands" to "hands",
        "fingers" to "fingers",
        "lungs" to "lungs",
        "blood" to "blood",
        "skin" to "skin",
        "liver" to "liver",
        "eyes" to "eyes"
    )

    private fun effect(
        attributeId: String,
        operation: Int,
        cap: Double,
        primary: Boolean = true,
        displayAsPercent: Boolean = operation != 0
    ) =
        ClientEffectDef(
            attributeId = attributeId,
            operation = operation,
            curve = ClientCurveDef(
                type = "hyperbola",
                cap = cap,
                k = 20.0,
                perPoint = 0.0,
                min = 0.0,
                max = cap
            ),
            isPrimary = primary,
            displayAsPercent = displayAsPercent
        )

}
