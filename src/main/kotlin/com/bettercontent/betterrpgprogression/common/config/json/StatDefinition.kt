package com.bettercontent.betterrpgprogression.common.config.json

import net.minecraft.resources.ResourceLocation

data class StatDefinition(
    val id: ResourceLocation,
    val nameKey: String,
    val order: Int = 0,
    val maxPoints: Int = -1,
    val effects: List<StatEffect> = emptyList(),
    val icon: String = "",
    val color: String = "#FFFFFF"
)
