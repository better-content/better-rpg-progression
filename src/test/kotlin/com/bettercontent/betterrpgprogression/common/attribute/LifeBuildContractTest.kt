package com.bettercontent.betterrpgprogression.common.attribute

import com.bettercontent.betterrpgprogression.common.config.json.CurveDef
import com.bettercontent.betterrpgprogression.common.curve.Curves
import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LifeBuildContractTest {
    private val builds = listOf("vanguard", "sharpshooter", "artisan", "trailblazer", "survivalist", "spellcaster", "skirmisher", "aerialist")
    private val retired = listOf("arms", "hands", "fingers", "lungs", "blood", "skin", "liver", "eyes")

    @Test
    fun `active stat resources have eight new IDs and no retired definitions`() {
        builds.forEach { assertTrue(javaClass.getResource("/data/better_rpg_progression/stats/$it.json") != null, it) }
        retired.forEach { assertFalse(javaClass.getResource("/data/better_rpg_progression/stats/$it.json") != null, it) }
        builds.forEach { id ->
            val json = JsonParser.parseReader(javaClass.getResourceAsStream("/data/better_rpg_progression/stats/$id.json")!!.reader()).asJsonObject
            assertTrue(json.getAsJsonArray("effects").size() >= 3, id)
            assertEquals("stat.better_rpg_progression.$id", json.get("name_key").asString)
        }
    }

    @Test
    fun `steady and landmark curves follow approved values at each checkpoint`() {
        val points = listOf(0, 1, 5, 10, 20, 21, 40)
        builds.forEach { id ->
            val json = JsonParser.parseReader(javaClass.getResourceAsStream("/data/better_rpg_progression/stats/$id.json")!!.reader()).asJsonObject
            json.getAsJsonArray("effects").forEach { row ->
                val effect = row.asJsonObject
                val curve = effect.getAsJsonObject("curve")
                val primary = effect.get("is_primary").asBoolean
                val def = if (primary) CurveDef(type = "linear", perPoint = curve.get("per_point").asDouble,
                    min = curve.get("min").asDouble, max = curve.get("max").asDouble)
                else CurveDef(type = "hyperbola", cap = curve.get("cap").asDouble, k = curve.get("k").asDouble)
                points.forEach { p ->
                    val expected = if (primary) curve.get("per_point").asDouble * p.coerceAtMost(20)
                        else curve.get("cap").asDouble * p / (p + 10.0)
                    assertEquals(expected, Curves.eval(p, def), 1e-9, "$id/${effect.get("attribute").asString} at $p")
                }
            }
        }
    }

    @Test
    fun `air attack cost and air slash ratio preserve the pinned base`() {
        assertEquals(1.84f, LifeEffectsMath.airAttackCost(2f, 0.08), 1e-5f)
        assertTrue(LifeEffectsMath.airAttackCost(0.0001f, 1.0) > 0f)
        assertEquals(0f, LifeEffectsMath.airAttackCost(0f, 0.99))
        assertEquals(12f, LifeEffectsMath.airSlashDamage(10f, 0.30), 1e-5f)
        assertEquals(10f, LifeEffectsMath.airSlashDamage(10f, 0.0))
        assertEquals(10f, LifeEffectsMath.airSlashDamage(10f, Double.NaN))
    }
}
