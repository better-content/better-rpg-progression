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
    private data class AspectContract(val order: Int, val color: String, val icon: String, val visibleName: String)
    private data class EffectContract(
        val cap: Double,
        val operation: String,
        val requiredMod: String? = null,
        val displayAsPercent: Boolean = operation != "add"
    )

    private val aspects = linkedMapOf(
        "arms" to AspectContract(10, "#FF4055", "✦", "arms"),
        "hands" to AspectContract(20, "#00A985", "»", "hands"),
        "fingers" to AspectContract(30, "#F0E2C5", "⚒", "fingers"),
        "lungs" to AspectContract(40, "#E0B01F", "➜", "lungs"),
        "blood" to AspectContract(50, "#52606A", "∞", "blood"),
        "skin" to AspectContract(60, "#AF6A2F", "◆", "skin"),
        "liver" to AspectContract(70, "#6CCAF0", "✚", "liver"),
        "eyes" to AspectContract(80, "#8E5BB7", "⊕", "eyes")
    )

    private val effects = mapOf(
        "arms" to mapOf(
            "better_rpg_progression:outgoing_damage" to EffectContract(0.6, "multiply_base"),
            "epicfight:impact" to EffectContract(0.75, "add", "epicfight"),
            "minecraft:generic.attack_knockback" to EffectContract(0.4, "add")
        ),
        "hands" to mapOf(
            "minecraft:generic.attack_speed" to EffectContract(0.6, "add"),
            "tconstruct:player.use_item_speed" to EffectContract(0.25, "multiply_base", "tconstruct")
        ),
        "fingers" to mapOf(
            "better_rpg_progression:mining_speed" to EffectContract(0.75, "multiply_base"),
            "forge:block_reach" to EffectContract(1.0, "add")
        ),
        "lungs" to mapOf(
            "minecraft:generic.movement_speed" to EffectContract(0.05, "add"),
            "forge:swim_speed" to EffectContract(0.25, "multiply_base"),
            "forge:step_height_addition" to EffectContract(0.5, "add"),
            "better_rpg_progression:air_capacity" to EffectContract(1.0, "multiply_base")
        ),
        "blood" to mapOf(
            "better_rpg_progression:hunger_efficiency" to EffectContract(1.0, "multiply_base"),
            "better_rpg_progression:thirst_efficiency" to EffectContract(1.0, "multiply_base"),
            "epicfight:staminar" to EffectContract(0.3, "multiply_base", "epicfight")
        ),
        "skin" to mapOf(
            "cold_sweat:heat_resistance" to EffectContract(0.6, "add", "cold_sweat", true),
            "cold_sweat:cold_resistance" to EffectContract(0.6, "add", "cold_sweat", true),
            "minecraft:generic.knockback_resistance" to EffectContract(0.15, "add")
        ),
        "liver" to mapOf(
            "better_rpg_progression:harmful_effect_duration_reduction" to EffectContract(0.25, "add", displayAsPercent = true),
            "better_rpg_progression:beneficial_effect_duration" to EffectContract(0.25, "add", displayAsPercent = true)
        ),
        "eyes" to mapOf(
            "better_rpg_progression:arrow_spread_reduction" to EffectContract(0.35, "add", displayAsPercent = true),
            "better_rpg_progression:arrow_speed_bonus" to EffectContract(0.08, "add", displayAsPercent = true),
            "better_rpg_progression:recoil_reduction" to EffectContract(0.35, "add", "tacz", true),
            "better_rpg_progression:dispersion_reduction" to EffectContract(0.35, "add", "tacz", true),
            "goety:spell_range" to EffectContract(0.25, "multiply_base", "goety")
        )
    )

    @Test
    fun `bundled rows are exactly the eight categorical Life identities`() {
        val files = statFiles()
        assertEquals(aspects.keys, files.map { it.name.removeSuffix(".json") }.toSet())

        files.forEach { path ->
            val id = path.name.removeSuffix(".json")
            val expected = aspects.getValue(id)
            val json = readJson(path)

            assertEquals("stat.better_rpg_progression.${expected.visibleName}", json.string("name_key"))
            assertEquals(expected.order, json.int("order"))
            assertEquals(expected.color, json.string("color"))
            assertEquals(expected.icon, json.string("icon"))
            assertFalse(json.has("max_points"), "Life development remains uncapped in $path")
            assertTrue(json.getAsJsonArray("effects").size() > 0, "dead aspect definition in $path")
        }
    }

    @Test
    fun `every projection uses the approved cap over points plus twenty curve`() {
        aspects.keys.forEach { id ->
            val actual = statJson(id).getAsJsonArray("effects")
                .associate { element -> element.asJsonObject.string("attribute") to element.asJsonObject }
            val expected = effects.getValue(id)

            assertEquals(expected.keys, actual.keys, "unexpected concrete projection for $id")
            actual.forEach { (attribute, effect) ->
                val contract = expected.getValue(attribute)
                assertEquals("attribute", effect.string("type"))
                assertEquals(contract.operation, effect.string("operation"))
                assertEquals(contract.requiredMod, effect.optionalString("requires_mod"))
                val displayAsPercent = effect.get("display_as_percent")?.asBoolean
                    ?: (contract.operation != "add")
                assertEquals(contract.displayAsPercent, displayAsPercent)

                val curve = effect.getAsJsonObject("curve")
                assertEquals("hyperbola", curve.string("type"))
                assertEquals(contract.cap, curve.double("cap"), 0.000001)
                assertEquals(20.0, curve.double("k"), 0.000001)
                assertEquals(0.0, curve.double("min"), 0.000001)
                assertEquals(contract.cap, curve.double("max"), 0.000001)
            }
        }
    }

    @Test
    fun `Life development leaves health and combat recovery to their owning systems`() {
        val forbidden = listOf(
            "max_health", "armor", "toughness", "damage_reduction",
            "stun_armor", "execution_resistance", "healing", "regeneration", "revive"
        )
        effects.values.flatMap { it.keys }.forEach { attribute ->
            forbidden.forEach { term ->
                assertFalse(term in attribute, "$attribute violates permanent-Life projection policy")
            }
        }
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
