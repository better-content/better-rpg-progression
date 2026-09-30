package com.bettercontent.betterrpgprogression.common.config

import net.minecraftforge.common.ForgeConfigSpec

/** Single server authority for final-death heart-fragment curve tuning. */
object HeartFragmentConfig {
    const val DEFAULT_ENTITLEMENT_SCALE = 4
    private val builder = ForgeConfigSpec.Builder()
    private val entitlementScaleValue = builder
        .comment("Multiplier for final-death heart fragments: ceil(scale * (2^(level / 4) - 1)).")
        .defineInRange("heart_fragments.entitlement_scale", DEFAULT_ENTITLEMENT_SCALE, 1, 1_000_000)
    val SPEC: ForgeConfigSpec = builder.build()

    // Unit tests construct curve owners before Forge loads server config; runtime receives the
    // registered server value as soon as the config lifecycle is active.
    fun entitlementScale(): Int = runCatching { entitlementScaleValue.get() }.getOrDefault(DEFAULT_ENTITLEMENT_SCALE)
}
