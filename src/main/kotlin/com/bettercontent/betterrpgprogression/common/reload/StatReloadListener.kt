package com.bettercontent.betterrpgprogression.common.reload

import com.bettercontent.betterrpgprogression.RpgStatsMod
import com.bettercontent.betterrpgprogression.common.attribute.StatAttributeProjector
import com.bettercontent.betterrpgprogression.common.config.json.AttributeEffect
import com.bettercontent.betterrpgprogression.common.config.json.CurveDef
import com.bettercontent.betterrpgprogression.common.config.json.StatDefinition
import com.bettercontent.betterrpgprogression.common.config.json.StatEffect
import com.bettercontent.betterrpgprogression.common.network.Network
import com.bettercontent.betterrpgprogression.common.network.packets.S2CStatDefsSync
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraftforge.server.ServerLifecycleHooks
import org.apache.logging.log4j.LogManager

class StatReloadListener : SimpleJsonResourceReloadListener(GSON, "stats") {

    override fun apply(
        objects: MutableMap<ResourceLocation, JsonElement>,
        resourceManager: ResourceManager,
        profiler: ProfilerFiller
    ) {
        val parsed = mutableMapOf<ResourceLocation, StatDefinition>()

        for ((id, elem) in objects) {
            try {
                val obj = elem.asJsonObject
                val def = parseStat(id, obj)
                parsed[id] = def
            } catch (t: Throwable) {
                LOGGER.error("Failed parsing stat json for $id", t)
            }
        }

        RegistryState.set(
            parsed.entries
                .sortedWith(compareBy({ it.value.order }, { it.key.toString() }))
                .associate { it.key to it.value }
        )
        val server = ServerLifecycleHooks.getCurrentServer()
        if (server != null) {
            server.execute {
                broadcastDefsAndReapply(server)
            }
        }
        LOGGER.info("Loaded ${parsed.size} stat definitions.")
    }

    private fun broadcastDefsAndReapply(server: MinecraftServer) {
        val defs = RegistryState.activeSnapshot().values.toList()
        Network.sendToAll(S2CStatDefsSync.fromDefs(defs))

        for (p in server.playerList.players) {
            StatAttributeProjector.reapply(p)
            Network.syncTo(p)
        }
    }

    private fun parseStat(id: ResourceLocation, obj: JsonObject): StatDefinition {
        val nameKey = obj.get("name_key")?.asString ?: "stat.${RpgStatsMod.MODID}.${id.path}"
        val order = obj.get("order")?.asInt ?: 0
        val maxPoints = obj.get("max_points")?.asInt ?: -1
        val icon = obj.get("icon")?.asString ?: ""
        val color = obj.get("color")?.asString ?: "#FFFFFF"

        val effects = mutableListOf<StatEffect>()
        val effArr = obj.getAsJsonArray("effects")
        if (effArr != null) {
            for (e in effArr) {
                val eo = e.asJsonObject
                val type = eo.get("type")?.asString?.lowercase() ?: continue
                if (type == "attribute") {
                    val attrId = ResourceLocation.tryParse(eo.get("attribute")?.asString ?: "")
                        ?: continue
                    val opStr = eo.get("operation")?.asString?.lowercase() ?: "add"
                    val op = when (opStr) {
                        "add", "addition" -> AttributeModifier.Operation.ADDITION
                        "multiply_base" -> AttributeModifier.Operation.MULTIPLY_BASE
                        "multiply_total" -> AttributeModifier.Operation.MULTIPLY_TOTAL
                        else -> AttributeModifier.Operation.ADDITION
                    }
                    val curveObj = eo.getAsJsonObject("curve") ?: JsonObject()
                    val curve = CurveDef(
                        type = curveObj.get("type")?.asString ?: "hyperbola",
                        cap = curveObj.get("cap")?.asDouble ?: 0.0,
                        k = curveObj.get("k")?.asDouble ?: 1.0,
                        perPoint = curveObj.get("per_point")?.asDouble ?: 0.0,
                        min = curveObj.get("min")?.asDouble ?: Double.NEGATIVE_INFINITY,
                        max = curveObj.get("max")?.asDouble ?: Double.POSITIVE_INFINITY
                    )
                    val isPrimary = eo.get("is_primary")?.asBoolean ?: true
                    val requiredMod = eo.get("requires_mod")?.asString?.takeIf(String::isNotBlank)
                    val displayAsPercent = eo.get("display_as_percent")?.asBoolean
                        ?: (op != AttributeModifier.Operation.ADDITION)
                    effects += AttributeEffect(attrId, op, curve, isPrimary, requiredMod, displayAsPercent)
                }
            }
        }

        return StatDefinition(
            id = id,
            nameKey = nameKey,
            order = order,
            maxPoints = maxPoints,
            effects = effects,
            icon = icon,
            color = color
        )
    }

    companion object {
        private val LOGGER = LogManager.getLogger("RPGStats/Reload")
        private val GSON = Gson()
    }
}
