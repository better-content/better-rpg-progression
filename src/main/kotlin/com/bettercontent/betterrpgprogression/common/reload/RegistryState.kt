package com.bettercontent.betterrpgprogression.common.reload

import com.bettercontent.betterrpgprogression.common.config.json.StatDefinition
import net.minecraft.resources.ResourceLocation

object RegistryState {
    @Volatile private var defs: Map<ResourceLocation, StatDefinition> = emptyMap()

    fun set(newDefs: Map<ResourceLocation, StatDefinition>) {
        defs = newDefs
    }

    fun snapshot(): Map<ResourceLocation, StatDefinition> = defs

    /** Keep every canonical Life choice visible; individual optional effects show unavailable. */
    fun activeSnapshot(): Map<ResourceLocation, StatDefinition> = defs
}
