package com.bettercontent.betterrpgprogression.common.attribute

import com.bettercontent.betterrpgprogression.common.config.json.AttributeEffect
import com.bettercontent.betterrpgprogression.common.config.json.CurveDef
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EffectAvailabilityTest {
    private val attributeId = ResourceLocation("example", "optional_attribute")
    private val effect = AttributeEffect(
        attributeId = attributeId,
        operation = AttributeModifier.Operation.ADDITION,
        curve = CurveDef(cap = 1.0, k = 20.0),
        requiredMod = "optional_mod"
    )

    @Test
    fun `optional projection requires both its owner mod and registered attribute`() {
        assertFalse(EffectAvailability.isAvailable(effect, { false }, { true }))
        assertFalse(EffectAvailability.isAvailable(effect, { true }, { false }))
        assertTrue(EffectAvailability.isAvailable(effect, { true }, { true }))
    }
}
