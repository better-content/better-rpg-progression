package com.bettercontent.betterrpgprogression.common.reload

import com.bettercontent.betterrpgprogression.common.attribute.EffectAvailability
import com.bettercontent.betterrpgprogression.common.config.json.AttributeEffect
import com.bettercontent.betterrpgprogression.common.config.json.StatDefinition
import net.minecraft.resources.ResourceLocation

object RegistryState {
    @Volatile private var defs: Map<ResourceLocation, StatDefinition> = emptyMap()

    fun set(newDefs: Map<ResourceLocation, StatDefinition>) {
        defs = newDefs
    }

    fun snapshot(): Map<ResourceLocation, StatDefinition> = defs

    fun activeSnapshot(): Map<ResourceLocation, StatDefinition> = defs.filterValues { definition ->
        definition.effects.any { effect ->
            effect is AttributeEffect && EffectAvailability.isAvailable(effect)
        }
    }
}
