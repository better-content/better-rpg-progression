package com.bettercontent.betterrpgprogression.harness

import com.bettercontent.betterrpgprogression.RpgStatsMod
import com.bettercontent.betterrpgprogression.client.cache.*
import com.bettercontent.betterrpgprogression.client.ui.StatsScreen
import com.bettercontent.betterrpgprogression.common.salience.AspectIdentity
import com.google.gson.JsonParser
import net.minecraft.client.Minecraft
import net.minecraft.client.Screenshot
import net.minecraft.client.gui.components.Button
import net.minecraft.client.resources.language.I18n
import net.minecraft.network.chat.Component
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.common.Mod
import java.security.MessageDigest

/** Development-only: production rendering, authored definitions, no world, fake mouse or pack. */
@Mod.EventBusSubscriber(modid = RpgStatsMod.MODID, value = [Dist.CLIENT], bus = Mod.EventBusSubscriber.Bus.FORGE)
object RpgStatsVisualHarness {
    private data class Case(val name: String, val scale: Int = 3, val pending: Boolean = false,
        val empty: Boolean = false, val exhausted: Boolean = false, val capped: Boolean = false,
        val nativeOnly: Boolean = false, val width: Int = 1920, val height: Int = 1080)
    private val cases = listOf(
        Case("empty-full-gui3", empty = true),
        Case("draft-full-gui3", pending = true),
        Case("draft-full-gui2", scale = 2, pending = true),
        Case("draft-full-gui4", scale = 4, pending = true),
        Case("no-points-full-gui3", exhausted = true),
        Case("capped-full-gui3", capped = true),
        Case("empty-native-gui3", nativeOnly = true, empty = true),
        Case("draft-1280-gui3", pending = true, width = 1280, height = 720)
    )
    private var startupTicks = 0
    private var caseIndex = -1
    private var ticks = 0
    private var capturing = false
    private var finished = false

    @SubscribeEvent
    fun tick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        val mc = Minecraft.getInstance()
        if (finished) {
            if (++ticks > 15) mc.stop()
            return
        }
        if (caseIndex < 0) {
            if (mc.overlay == null && mc.screen != null && ++startupTicks >= 20) open(mc, 0)
            return
        }
        if (capturing) return
        if (++ticks == 15 && cases[caseIndex].pending) {
            // Exercise native callbacks, never synthesize pointer events. Also check auto-plan return.
            val screen = mc.screen as StatsScreen
            val before = buttons(screen).first { it.message.string == "Auto plan" }
            before.onPress()
            mc.screen!!.onClose()
            check(mc.screen === screen) { "Auto plan did not return to the same allocation draft" }
            check(buttons(screen).any { it.message.string == "Commit 3 points" && it.active }) {
                "Returning from Auto plan lost the allocation draft"
            }
        }
        if (ticks < 35) return
        val case = cases[caseIndex]
        check(mc.window.width == case.width && mc.window.height == case.height) { "Wrong framebuffer dimensions" }
        check(mc.screen is StatsScreen)
        check(buttons(mc.screen as StatsScreen).size == 20) { "Expected 16 allocation and 4 footer controls" }
        capturing = true
        val fileName = "allocation-${case.name}.png"
        Screenshot.grab(mc.gameDirectory, fileName, mc.mainRenderTarget) { message ->
            println("RPG_STATS_REVIEW captured=$fileName framebuffer=${case.width}x${case.height} $message")
            mc.execute {
                capturing = false
                if (caseIndex + 1 < cases.size) open(mc, caseIndex + 1) else {
                    finished = true
                    ticks = 0
                    println("RPG_STATS_REVIEW PASS ${cases.size} native captures; draft/reset/undo/cap/return callbacks verified; no pointer simulation")
                }
            }
        }
    }

    private fun open(mc: Minecraft, index: Int) {
        caseIndex = index
        ticks = 0
        val case = cases[index]
        mc.window.setWindowed(case.width, case.height)
        mc.options.guiScale().set(case.scale)
        mc.resizeDisplay()
        val definitions = loadDefinitions(case.nativeOnly)
        ClientCache.defs = if (case.capped) definitions.map { if (it.id.endsWith(":arms")) it.copy(maxPoints = 4) else it } else definitions
        ClientCache.stats = ClientStatsSnapshot(
            unspent = if (case.exhausted) 0 else 4, lifePeak = 27,
            allocations = if (case.empty) emptyMap() else AspectIdentity.entries.associate {
                "better_rpg_progression:${it.statId}" to if (it.statId == "arms") 4 else 2
            }
        )
        val screen = StatsScreen()
        mc.setScreen(screen)
        val plus = buttons(screen).filter { it.message.string.startsWith("Add a point") }
        val minus = buttons(screen).filter { it.message.string.startsWith("Remove a draft") }
        check(minus.none { it.active }) { "Committed points can be refunded" }
        if (case.pending) {
            listOf(0, 3, 7).forEach { plus[it].onPress() }
            check(buttons(screen).any { it.message.string == "Commit 3 points" && it.active })
            minus[7].onPress()
            check(buttons(screen).any { it.message.string == "Commit 2 points" && it.active })
            buttons(screen).first { it.message.string == "Reset draft" }.onPress()
            check(buttons(screen).none { it.message.string.startsWith("Commit") && it.active })
            listOf(0, 3, 7).forEach { plus[it].onPress() }
        }
        if (case.capped) check(!plus.first().active) { "Capped allocation can be incremented" }
        if (case.exhausted) check(plus.none { it.active }) { "Allocation possible with no points" }
        auditText(mc, ClientCache.defs)
        println("RPG_STATS_REVIEW ready=${case.name} active-effects=${ClientCache.defs.sumOf { it.effects.size }} gui=${mc.window.guiScaledWidth}x${mc.window.guiScaledHeight}")
    }

    private fun buttons(screen: StatsScreen) = screen.children().filterIsInstance<Button>()

    private fun loadDefinitions(nativeOnly: Boolean): List<ClientStatDef> = AspectIdentity.entries.map { aspect ->
        val path = "/data/better_rpg_progression/stats/${aspect.statId}.json"
        val bytes = requireNotNull(RpgStatsVisualHarness::class.java.getResourceAsStream(path)).use { it.readBytes() }
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        println("RPG_STATS_REVIEW input=$path sha256=$hash mode=${if (nativeOnly) "loaded-mods" else "all-authored-integrations (presentation stress only)"}")
        val obj = JsonParser.parseString(bytes.toString(Charsets.UTF_8)).asJsonObject
        val effects = obj.getAsJsonArray("effects").map { it.asJsonObject }.filter {
            !nativeOnly || !it.has("requires_mod") || ModList.get().isLoaded(it.get("requires_mod").asString)
        }.map { effect ->
            val curve = effect.getAsJsonObject("curve")
            val operation = when (effect.get("operation").asString) { "multiply_base" -> 1; "multiply_total" -> 2; else -> 0 }
            ClientEffectDef(effect.get("attribute").asString, operation,
                ClientCurveDef(curve.get("type").asString, curve.get("cap").asDouble, curve.get("k").asDouble,
                    curve.get("per_point")?.asDouble ?: 0.0, curve.get("min").asDouble, curve.get("max").asDouble),
                effect.get("is_primary")?.asBoolean ?: true,
                effect.get("display_as_percent")?.asBoolean ?: (operation != 0))
        }
        ClientStatDef("better_rpg_progression:${aspect.statId}", obj.get("name_key").asString,
            obj.get("max_points")?.asInt ?: -1, effects, obj.get("icon").asString,
            obj.get("color").asString.removePrefix("#").toInt(16))
    }

    /** Real font metrics, including the 5-effect worst case, before any screenshot is accepted. */
    private fun auditText(mc: Minecraft, definitions: List<ClientStatDef>) {
        val scale = minOf(1f, mc.window.guiScaledWidth / 800f, mc.window.guiScaledHeight / 500f)
        val canvasWidth = (mc.window.guiScaledWidth / scale).toInt()
        val cardWidth = (canvasWidth - 32 - 30) / 4
        definitions.forEach { def ->
            check(mc.font.width(Component.translatable(def.nameKey)) <= cardWidth - 43) { "Name does not fit: ${def.id}" }
            val purpose = Component.translatable("${def.nameKey}.purpose")
            check(mc.font.split(purpose, cardWidth - 20).size <= 2) { "Purpose exceeds two lines: ${def.id}" }
            check(def.effects.size <= 5) { "Effect table needs more space: ${def.id}" }
            def.effects.forEach { effect ->
                val key = "effect.better_rpg_progression.${effect.attributeId.replace(':', '.')}"
                check(I18n.exists(key)) { "Missing concise effect label: $key" }
                check(mc.font.width(Component.translatable(key)) <= cardWidth - 106) { "Effect label overlaps values: $key" }
            }
        }
    }
}
