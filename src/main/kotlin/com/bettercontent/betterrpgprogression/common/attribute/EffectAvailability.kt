package com.bettercontent.betterrpgprogression.common.attribute

import com.bettercontent.betterrpgprogression.common.config.json.AttributeEffect
import net.minecraftforge.fml.ModList
import net.minecraftforge.registries.ForgeRegistries

object EffectAvailability {
    fun isAvailable(effect: AttributeEffect): Boolean =
        isAvailable(
            effect = effect,
            isModLoaded = { ModList.get().isLoaded(it) },
            isAttributeRegistered = { ForgeRegistries.ATTRIBUTES.containsKey(it) }
        )

    internal fun isAvailable(
        effect: AttributeEffect,
        isModLoaded: (String) -> Boolean,
        isAttributeRegistered: (net.minecraft.resources.ResourceLocation) -> Boolean
    ): Boolean =
        (effect.requiredMod == null || isModLoaded(effect.requiredMod)) &&
            isAttributeRegistered(effect.attributeId)
}
