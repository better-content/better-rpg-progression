package com.bettercontent.betterrpgprogression.common.item

import net.minecraft.nbt.CompoundTag
import java.util.UUID
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HeartFragmentEntitlementsTest {
    @Test
    fun `final death capture has one UUID entitlement across retry and reload`() {
        val playerData = CompoundTag()
        val id = UUID.randomUUID()
        HeartFragmentEntitlements.captureFinalDeath(playerData, 20, id)

        val entitlement = HeartFragmentEntitlements.finalizeCapturedDeath(playerData)!!
        assertEquals(id, entitlement.id)
        assertEquals(BigInteger.valueOf(124), entitlement.fragments)
        assertTrue(HeartFragmentEntitlements.enqueue(playerData, entitlement))
        assertFalse(HeartFragmentEntitlements.enqueue(playerData, entitlement))
        assertEquals(listOf(entitlement.copy(sequence = 1)), HeartFragmentEntitlements.pending(playerData.copy()))
    }

    @Test
    fun `partial inventory delivery retains only the undistributed amount`() {
        val playerData = CompoundTag()
        val entitlement = HeartFragmentEntitlements.Entitlement(UUID.randomUUID(), BigInteger.valueOf(80))
        assertTrue(HeartFragmentEntitlements.enqueue(playerData, entitlement))

        assertTrue(HeartFragmentEntitlements.recordDelivery(playerData, entitlement.id, BigInteger.valueOf(64)))
        assertEquals(listOf(entitlement.copy(fragments = BigInteger.valueOf(16), sequence = 1)), HeartFragmentEntitlements.pending(playerData.copy()))
        assertTrue(HeartFragmentEntitlements.recordDelivery(playerData, entitlement.id, BigInteger.valueOf(16)))
        assertEquals(emptyList(), HeartFragmentEntitlements.pending(playerData))
        assertEquals(1, playerData.getLong("better_rpg_progression_completed_heart_entitlement_through"))
        assertFalse(playerData.contains("better_rpg_progression_completed_heart_fragment_entitlements"))
    }

    @Test
    fun `legacy personalized heart migration derives an idempotent UUID`() {
        val data = CompoundTag()
        val legacy = listOf(HeartFragmentEntitlements.LegacyHeart(level = 20, fingerprint = "old-heart-nbt"))

        assertEquals(1, HeartFragmentEntitlements.migrateLegacyPending(data, legacy))
        assertEquals(0, HeartFragmentEntitlements.migrateLegacyPending(data, legacy))
        assertEquals(BigInteger.valueOf(124), HeartFragmentEntitlements.pending(data).single().fragments)
    }


    @Test
    fun `old pending UUID rows compact to ordered watermark schema`() {
        val data = CompoundTag()
        val oldRow = CompoundTag()
        oldRow.putString("id", UUID.randomUUID().toString())
        oldRow.putLong("count", 7)
        net.minecraft.nbt.ListTag().also { rows ->
            rows.add(oldRow)
            data.put("better_rpg_progression_pending_heart_fragment_entitlements", rows)
        }
        data.put("better_rpg_progression_completed_heart_fragment_entitlements", net.minecraft.nbt.ListTag())

        assertEquals(1, HeartFragmentEntitlements.nextPending(data)!!.sequence)
        assertFalse(data.contains("better_rpg_progression_completed_heart_fragment_entitlements"))
    }

    @Test
    fun `cancelled death capture mutates no entitlement state`() {
        val data = CompoundTag()
        HeartFragmentEntitlements.captureFinalDeath(data, 20, UUID.randomUUID())
        HeartFragmentEntitlements.discardCapturedDeath(data)

        assertEquals(emptyList(), HeartFragmentEntitlements.pending(data))
        assertEquals(null, HeartFragmentEntitlements.finalizeCapturedDeath(data))
    }

    @Test
    fun `unrepresentable high level is durably unresolved instead of throwing in death handling`() {
        val data = CompoundTag()
        HeartFragmentEntitlements.captureFinalDeath(data, 260, UUID.randomUUID())

        val entitlement = HeartFragmentEntitlements.finalizeCapturedDeath(data)!!
        assertEquals(BigInteger("147573952589676412924"), entitlement.fragments)
        assertTrue(HeartFragmentEntitlements.enqueue(data, entitlement))
        assertTrue(HeartFragmentEntitlements.recordDelivery(data, entitlement.id, BigInteger.valueOf(64)))
        assertEquals(BigInteger("147573952589676412860"), HeartFragmentEntitlements.nextPending(data)!!.fragments)
    }

}
