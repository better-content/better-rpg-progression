package com.bettercontent.betterrpgprogression.common.item

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import java.nio.charset.StandardCharsets
import java.math.BigInteger
import java.util.UUID

/**
 * Durable final-death rewards. Pending rows live with the inventory in playerdata, so normal
 * player saves commit fragment insertion and row mutation together. Completed history compacts
 * to one monotonic watermark; delivery always processes the oldest pending row first.
 */
object HeartFragmentEntitlements {
    private const val CAPTURE_TAG = "better_rpg_progression_captured_heart_entitlement"
    private const val UNRESOLVED_TAG = "better_rpg_progression_unresolved_heart_entitlement"
    private const val PENDING_TAG = "better_rpg_progression_pending_heart_fragment_entitlements"
    private const val NEXT_SEQUENCE_TAG = "better_rpg_progression_next_heart_entitlement_sequence"
    private const val COMPLETED_THROUGH_TAG = "better_rpg_progression_completed_heart_entitlement_through"
    private const val ID_TAG = "id"
    private const val COUNT_TAG = "count"
    private const val SEQUENCE_TAG = "sequence"
    private const val LEVEL_TAG = "level"
    private const val LEGACY_PREFIX = "legacy-personalized-heart-v1:"

    data class Entitlement(val id: UUID, val fragments: BigInteger, val sequence: Long = 0)

    fun captureFinalDeath(data: CompoundTag, heldLevel: Int, id: UUID = UUID.randomUUID()) {
        val capture = CompoundTag()
        capture.putString(ID_TAG, id.toString())
        capture.putInt(LEVEL_TAG, heldLevel.coerceAtLeast(0))
        data.put(CAPTURE_TAG, capture)
    }

    fun discardCapturedDeath(data: CompoundTag) {
        data.remove(CAPTURE_TAG)
    }

    /** Called only after the low-priority death event confirms this life actually ended. */
    fun finalizeCapturedDeath(data: CompoundTag): Entitlement? {
        if (!data.contains(CAPTURE_TAG, Tag.TAG_COMPOUND.toInt())) return null
        val capture = data.getCompound(CAPTURE_TAG)
        val id = capture.uuid(ID_TAG) ?: run {
            data.remove(CAPTURE_TAG)
            return null
        }
        val fragments = runCatching { HeartFragmentData.fragmentsForLevelBig(capture.getInt(LEVEL_TAG)) }.getOrElse {
            // The present stackable long carrier cannot represent this exact result. Preserve the
            // source UUID and held level for a later carrier migration instead of crashing a
            // LivingDeathEvent, clamping, or silently discarding value.
            val unresolved = if (data.contains(UNRESOLVED_TAG, Tag.TAG_LIST.toInt())) {
                data.getList(UNRESOLVED_TAG, Tag.TAG_COMPOUND.toInt())
            } else ListTag()
            unresolved.add(capture.copy())
            data.put(UNRESOLVED_TAG, unresolved)
            data.remove(CAPTURE_TAG)
            return null
        }
        data.remove(CAPTURE_TAG)
        return Entitlement(id, fragments).takeIf { it.fragments > BigInteger.ZERO }
    }

    /** An active UUID can only occupy one pending row. Completed history is a bounded sequence. */
    fun enqueue(data: CompoundTag, entitlement: Entitlement): Boolean {
        if (entitlement.fragments <= BigInteger.ZERO || containsPendingId(data, entitlement.id)) return false
        val pending = normalizedPending(data)
        val next = data.getLong(NEXT_SEQUENCE_TAG)
        require(next < Long.MAX_VALUE) { "Heart entitlement sequence exhausted" }
        val sequence = next + 1
        pending.add(row(entitlement.id, entitlement.fragments, sequence))
        data.put(PENDING_TAG, pending)
        data.putLong(NEXT_SEQUENCE_TAG, sequence)
        return true
    }

    /** Pending rows remain in creation order; only the oldest is eligible to deliver. */
    fun nextPending(data: CompoundTag): Entitlement? {
        val pending = normalizedPending(data)
        if (pending.isEmpty()) return null
        return pending.getCompound(0).toEntitlement()
    }

    fun pending(data: CompoundTag): List<Entitlement> = (0 until normalizedPending(data).size).mapNotNull { index ->
        normalizedPending(data).getCompound(index).toEntitlement()
    }

    /** Replaces the oldest row after a partial insert or advances the bounded receipt watermark. */
    fun recordDelivery(data: CompoundTag, id: UUID, delivered: BigInteger): Boolean {
        if (delivered <= BigInteger.ZERO) return false
        val pending = normalizedPending(data)
        if (pending.isEmpty()) return false
        val row = pending.getCompound(0)
        if (row.uuid(ID_TAG) != id) return false
        val remaining = row.count()
        require(delivered <= remaining) { "Cannot deliver more heart fragments than entitlement $id owns" }
        if (delivered == remaining) {
            val sequence = row.getLong(SEQUENCE_TAG)
            require(sequence == data.getLong(COMPLETED_THROUGH_TAG) + 1) { "Heart entitlement delivery order is not contiguous" }
            pending.removeAt(0)
            data.put(PENDING_TAG, pending)
            data.putLong(COMPLETED_THROUGH_TAG, sequence)
        } else {
            row.putString(COUNT_TAG, remaining.subtract(delivered).toString())
            pending[0] = row
            data.put(PENDING_TAG, pending)
        }
        return true
    }

    /** Exposes durable source states for admin recovery when exact count exceeds the current carrier. */
    fun unresolvedLevels(data: CompoundTag): List<Int> =
        if (!data.contains(UNRESOLVED_TAG, Tag.TAG_LIST.toInt())) emptyList()
        else (0 until data.getList(UNRESOLVED_TAG, Tag.TAG_COMPOUND.toInt()).size)
            .map { index -> data.getList(UNRESOLVED_TAG, Tag.TAG_COMPOUND.toInt()).getCompound(index).getInt(LEVEL_TAG) }

    /**
     * Converts old pending personalized-heart rows exactly once. The deterministic UUID means a
     * crash before legacy-list removal retries the same active row harmlessly on reconnect.
     */
    fun migrateLegacyPending(data: CompoundTag, stacks: List<LegacyHeart>): Int {
        var migrated = 0
        stacks.forEachIndexed { index, legacy ->
            val fragments = runCatching { HeartFragmentData.fragmentsForLevelBig(legacy.level) }.getOrNull() ?: return@forEachIndexed
            if (fragments <= BigInteger.ZERO) return@forEachIndexed
            val id = UUID.nameUUIDFromBytes((LEGACY_PREFIX + index + ':' + legacy.fingerprint).toByteArray(StandardCharsets.UTF_8))
            if (enqueue(data, Entitlement(id, fragments))) migrated += 1
        }
        return migrated
    }

    data class LegacyHeart(val level: Int, val fingerprint: String)

    private fun containsPendingId(data: CompoundTag, id: UUID): Boolean =
        (0 until normalizedPending(data).size).any { index -> normalizedPending(data).getCompound(index).uuid(ID_TAG) == id }

    /** Migrates the short-lived UUID receipt-list schema without treating old IDs as rewards. */
    private fun normalizedPending(data: CompoundTag): ListTag {
        val pending = list(data)
        var sequence = maxOf(data.getLong(NEXT_SEQUENCE_TAG), data.getLong(COMPLETED_THROUGH_TAG))
        var changed = false
        for (index in 0 until pending.size) {
            val row = pending.getCompound(index)
            if (row.getLong(SEQUENCE_TAG) > 0) {
                sequence = maxOf(sequence, row.getLong(SEQUENCE_TAG))
                continue
            }
            require(sequence < Long.MAX_VALUE) { "Heart entitlement sequence exhausted" }
            row.putLong(SEQUENCE_TAG, ++sequence)
            pending[index] = row
            changed = true
        }
        if (changed) data.put(PENDING_TAG, pending)
        if (sequence != data.getLong(NEXT_SEQUENCE_TAG)) data.putLong(NEXT_SEQUENCE_TAG, sequence)
        // Prior source revisions retained an ever-growing UUID completion list. Its values are
        // already materialized inventory delivery, so discard only the obsolete receipt cache.
        data.remove("better_rpg_progression_completed_heart_fragment_entitlements")
        return pending
    }

    private fun list(data: CompoundTag): ListTag =
        if (data.contains(PENDING_TAG, Tag.TAG_LIST.toInt())) data.getList(PENDING_TAG, Tag.TAG_COMPOUND.toInt()) else ListTag()

    private fun row(id: UUID, count: BigInteger, sequence: Long): CompoundTag = CompoundTag().also {
        it.putString(ID_TAG, id.toString())
        it.putString(COUNT_TAG, count.max(BigInteger.ZERO).toString())
        it.putLong(SEQUENCE_TAG, sequence)
    }

    private fun CompoundTag.toEntitlement(): Entitlement? = uuid(ID_TAG)?.let { id ->
        Entitlement(id, count(), getLong(SEQUENCE_TAG))
            .takeIf { it.fragments > BigInteger.ZERO && it.sequence > 0 }
    }

    private fun CompoundTag.count(): BigInteger = runCatching {
        if (contains(COUNT_TAG, Tag.TAG_STRING.toInt())) BigInteger(getString(COUNT_TAG))
        else BigInteger.valueOf(getLong(COUNT_TAG))
    }.getOrDefault(BigInteger.ZERO).max(BigInteger.ZERO)

    private fun CompoundTag.uuid(key: String): UUID? = runCatching { UUID.fromString(getString(key)) }.getOrNull()
}
