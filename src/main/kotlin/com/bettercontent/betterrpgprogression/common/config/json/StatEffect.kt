package com.bettercontent.betterrpgprogression.common.config.json

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.ai.attributes.AttributeModifier

sealed interface StatEffect

data class AttributeEffect(
    val attributeId: ResourceLocation,
    val operation: AttributeModifier.Operation,
    val curve: CurveDef,
    val isPrimary: Boolean = true,
    val requiredMod: String? = null,
    val displayAsPercent: Boolean = operation != AttributeModifier.Operation.ADDITION
) : StatEffect
