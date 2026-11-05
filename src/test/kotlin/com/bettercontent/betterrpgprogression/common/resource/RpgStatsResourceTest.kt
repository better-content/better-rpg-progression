package com.bettercontent.betterrpgprogression.common.resource

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.name
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RpgStatsResourceTest {
    private data class AspectContract(val order: Int, val color: String, val icon: String)
    private val aspects = linkedMapOf(
        "vanguard" to AspectContract(10, "#FF4055", "✦"),
        "sharpshooter" to AspectContract(20, "#8E5BB7", "⊕"),
        "artisan" to AspectContract(30, "#F0E2C5", "⚒"),
        "trailblazer" to AspectContract(40, "#E0B01F", "➜"),
        "survivalist" to AspectContract(50, "#AF6A2F", "◆"),
        "spellcaster" to AspectContract(60, "#6CCAF0", "✚"),
        "skirmisher" to AspectContract(70, "#52606A", "∞"),
        "aerialist" to AspectContract(80, "#00A985", "»")
    )
    private val effectIds = mapOf(
        "vanguard" to setOf("epicfight:armor_negation", "minecraft:generic.attack_knockback", "epicfight:impact"),
        "sharpshooter" to setOf("better_rpg_progression:arrow_spread_reduction", "better_rpg_progression:dispersion_reduction",
            "better_rpg_progression:recoil_reduction", "better_rpg_progression:arrow_speed_bonus"),
        "artisan" to setOf("better_rpg_progression:mining_speed", "tconstruct:player.use_item_speed", "forge:block_reach"),
        "trailblazer" to setOf("tconstruct:generic.safe_fall_distance", "forge:swim_speed", "forge:step_height_addition"),
        "survivalist" to setOf("better_rpg_progression:heat_drift_reduction", "better_rpg_progression:cold_drift_reduction",
            "better_rpg_progression:freezing_boundary_offset", "better_rpg_progression:burning_boundary_offset"),
        "spellcaster" to setOf("ars_nouveau:ars_nouveau.perk.max_mana", "ars_nouveau:ars_nouveau.perk.mana_regen",
            "goety:soul_discount", "goety:casting_speed", "goety:spell_range"),
        "skirmisher" to setOf("parcool:max_stamina", "parcool:stamina_recovery", "epicfight:staminar", "epicfight:stamina_regen"),
        "aerialist" to setOf("tconstruct:generic.jump_boost", "better_rpg_progression:air_attack_stamina_reduction",
            "better_rpg_progression:air_slash_damage_bonus")
    )
    private val landmarks = mapOf(
        "vanguard" to setOf("epicfight:impact"),
        "sharpshooter" to setOf("better_rpg_progression:arrow_speed_bonus"),
        "artisan" to setOf("forge:block_reach"),
        "trailblazer" to setOf("forge:step_height_addition"),
        "survivalist" to setOf("better_rpg_progression:freezing_boundary_offset", "better_rpg_progression:burning_boundary_offset"),
        "spellcaster" to setOf("goety:spell_range"),
        "skirmisher" to emptySet(),
        "aerialist" to setOf("better_rpg_progression:air_slash_damage_bonus")
    )
    private val landmarkAt20 = mapOf(
        "epicfight:impact" to 0.60, "better_rpg_progression:arrow_speed_bonus" to 0.08,
        "forge:block_reach" to 1.0, "forge:step_height_addition" to 0.4,
        "better_rpg_progression:freezing_boundary_offset" to -0.5,
        "better_rpg_progression:burning_boundary_offset" to 0.5,
        "goety:spell_range" to 1.0, "better_rpg_progression:air_slash_damage_bonus" to 0.30
    )
    private val steadyCaps = mapOf(
        "epicfight:armor_negation" to 6.0, "minecraft:generic.attack_knockback" to 0.20,
        "better_rpg_progression:arrow_spread_reduction" to 0.18,
        "better_rpg_progression:dispersion_reduction" to 0.18,
        "better_rpg_progression:recoil_reduction" to 0.12,
        "better_rpg_progression:mining_speed" to 0.12, "tconstruct:player.use_item_speed" to 0.09,
        "tconstruct:generic.safe_fall_distance" to 3.0, "forge:swim_speed" to 0.15,
        "better_rpg_progression:heat_drift_reduction" to 0.15,
        "better_rpg_progression:cold_drift_reduction" to 0.15,
        "ars_nouveau:ars_nouveau.perk.max_mana" to 60.0,
        "ars_nouveau:ars_nouveau.perk.mana_regen" to 1.2,
        "goety:soul_discount" to 0.06, "goety:casting_speed" to 0.08,
        "parcool:max_stamina" to 0.12, "parcool:stamina_recovery" to 0.10,
        "epicfight:staminar" to 0.12, "epicfight:stamina_regen" to 0.10,
        "tconstruct:generic.jump_boost" to 0.09,
        "better_rpg_progression:air_attack_stamina_reduction" to 0.12
    )

    @Test
    fun `bundled rows are exactly the eight build-oriented Life identities`() {
        val files = statFiles()
        assertEquals(aspects.keys, files.map { it.name.removeSuffix(".json") }.toSet())
        files.forEach { path ->
            val id = path.name.removeSuffix(".json")
            val expected = aspects.getValue(id)
            val json = readJson(path)
            assertEquals("stat.better_rpg_progression.$id", json.string("name_key"))
            assertEquals(expected.order, json.int("order"))
            assertEquals(expected.color, json.string("color"))
            assertEquals(expected.icon, json.string("icon"))
            assertFalse(json.has("max_points"), "Life development remains uncapped in $path")
            assertTrue(json.getAsJsonArray("effects").size() > 0, "dead build definition in $path")
        }
    }

    @Test
    fun `every projection uses its approved steady or capped landmark curve`() {
        aspects.keys.forEach { id ->
            val actual = statJson(id).getAsJsonArray("effects")
                .associate { element -> element.asJsonObject.string("attribute") to element.asJsonObject }
            assertEquals(effectIds.getValue(id), actual.keys, "unexpected projection for $id")
            val primary = actual.filterValues { it.get("is_primary")?.asBoolean == true }.keys
            assertEquals(landmarks.getValue(id), primary, "$id landmarks")
            actual.forEach { (attribute, effect) ->
                assertEquals("attribute", effect.string("type"))
                assertEquals(if (attribute in setOf("better_rpg_progression:mining_speed", "forge:swim_speed",
                        "parcool:max_stamina", "parcool:stamina_recovery", "epicfight:staminar",
                        "epicfight:stamina_regen")) "multiply_base" else "add", effect.string("operation"), attribute)
                val requiredMod = when {
                    attribute.startsWith("epicfight:") || attribute in setOf(
                        "better_rpg_progression:air_attack_stamina_reduction", "better_rpg_progression:air_slash_damage_bonus") -> "epicfight"
                    attribute.startsWith("tconstruct:") -> "tconstruct"
                    attribute.startsWith("ars_nouveau:") -> "ars_nouveau"
                    attribute.startsWith("goety:") -> "goety"
                    attribute.startsWith("parcool:") -> "parcool"
                    attribute in setOf("better_rpg_progression:dispersion_reduction", "better_rpg_progression:recoil_reduction") -> "tacz"
                    attribute in setOf("better_rpg_progression:heat_drift_reduction", "better_rpg_progression:cold_drift_reduction",
                        "better_rpg_progression:freezing_boundary_offset", "better_rpg_progression:burning_boundary_offset") -> "cold_sweat"
                    else -> null
                }
                assertEquals(requiredMod, effect.optionalString("requires_mod"), attribute)
                val curve = effect.getAsJsonObject("curve")
                if (attribute in landmarkAt20) {
                    assertEquals("linear", curve.string("type"))
                    assertEquals(landmarkAt20.getValue(attribute) / 20.0, curve.double("per_point"), 1e-9)
                    assertEquals(landmarkAt20.getValue(attribute),
                        if (landmarkAt20.getValue(attribute) < 0) curve.double("min") else curve.double("max"), 1e-9)
                } else {
                    assertEquals("hyperbola", curve.string("type"))
                    assertEquals(steadyCaps.getValue(attribute), curve.double("cap"), 1e-9)
                    assertEquals(10.0, curve.double("k"), 1e-9)
                }
            }
        }
    }

    @Test
    fun `Life development leaves health and combat recovery to their owning systems`() {
        val forbidden = listOf("max_health", "generic.armor", "toughness", "damage_reduction",
            "stun_armor", "execution_resistance", "healing", "revive")
        effectIds.values.flatMap { it }.forEach { attribute ->
            forbidden.forEach { term ->
                assertFalse(term in attribute, "$attribute violates permanent-Life projection policy")
            }
        }
    }

    @Test
    fun `permanent projections do not replace nutrition or spirit commerce rewards`() {
        val forbidden = setOf("better_rpg_progression:outgoing_damage", "minecraft:generic.attack_damage",
            "better_rpg_progression:hunger_efficiency", "better_rpg_progression:thirst_efficiency",
            "cold_sweat:heat_resistance", "cold_sweat:cold_resistance",
            "better_rpg_progression:harmful_effect_duration_reduction")
        statFiles().forEach { path ->
            val rows = readJson(path).getAsJsonArray("effects").map { it.asJsonObject }
            rows.forEach { assertFalse(it.string("attribute") in forbidden, "$path replaces nutrition") }
            rows.forEach { assertFalse("spirit" in it.string("attribute"), "$path modifies spirit commerce") }
        }
        val language = readJson(Path.of("src/main/resources/assets/better_rpg_progression/lang/en_us.json"))
        assertEquals("Survivalist", language.string("stat.better_rpg_progression.survivalist"))
    }

    @Test
    fun `still beating heart has a visible double pulse animation`() {
        val texturePath = Path.of("src/main/resources/assets/better_rpg_progression/textures/item/still_beating_heart.png")
        val metadataPath = texturePath.resolveSibling("still_beating_heart.png.mcmeta")
        val image = assertNotNull(ImageIO.read(texturePath.toFile()), "heart texture must be a readable PNG")
        assertEquals(16, image.width)
        assertEquals(64, image.height)
        val frameHeight = image.width
        val frameCount = image.height / frameHeight
        assertEquals(4, frameCount)
        val framePixels = (0 until frameCount).map { frame ->
            image.getRGB(0, frame * frameHeight, image.width, frameHeight, null, 0, image.width).toList()
        }
        assertEquals(frameCount, framePixels.distinct().size, "every heartbeat frame must be visually distinct")
        val animation = readJson(metadataPath).getAsJsonObject("animation")
        assertEquals(image.width, animation.int("width"))
        assertEquals(frameHeight, animation.int("height"))
        assertFalse(animation.get("interpolate").asBoolean, "direct frames keep the pulse visible at GUI scale")
        val scheduledFrames = animation.getAsJsonArray("frames").map { it.asJsonObject }
        assertTrue(scheduledFrames.size >= 9, "the schedule must include two beats and a rest")
        scheduledFrames.forEach { frame ->
            assertTrue(frame.int("index") in 0 until frameCount, "animation frame index must exist")
            assertTrue(frame.int("time") > 0, "animation frame duration must be positive")
        }
        assertTrue(scheduledFrames.count { it.int("index") == 2 } >= 2, "the peak frame must occur twice")
        assertEquals(0, scheduledFrames.first().int("index"))
        assertEquals(0, scheduledFrames.last().int("index"))
    }

    private fun statFiles(): List<Path> =
        Files.list(Path.of("src/main/resources/data/better_rpg_progression/stats")).use { paths ->
            paths.filter { it.name.endsWith(".json") }.sorted().toList()
        }

    private fun statJson(id: String): JsonObject =
        readJson(Path.of("src/main/resources/data/better_rpg_progression/stats/$id.json"))

    private fun readJson(path: Path): JsonObject =
        Files.newBufferedReader(path).use { JsonParser.parseReader(it).asJsonObject }

    private fun JsonObject.string(name: String): String = get(name).asString
    private fun JsonObject.optionalString(name: String): String? = get(name)?.asString
    private fun JsonObject.int(name: String): Int = get(name).asInt
    private fun JsonObject.double(name: String): Double = get(name).asDouble
}
