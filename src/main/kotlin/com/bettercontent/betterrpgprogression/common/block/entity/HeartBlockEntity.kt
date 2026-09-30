package com.bettercontent.betterrpgprogression.common.block.entity

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import wayoftime.bloodmagic.altar.IBloodAltar

class HeartBlockEntity(pos: BlockPos, state: BlockState) : BlockEntity(ModBlockEntities.HEART_BLOCK.get(), pos, state) {
    var installedFragments: Long = 0
        private set

    fun installOneFragment(): Boolean {
        val next = HeartBlockInvestment.addOne(installedFragments) ?: return false
        installedFragments = next
        setChanged()
        return true
    }

    fun setInstalledFragments(value: Long) {
        installedFragments = HeartBlockInvestment.normalize(value)
        setChanged()
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        HeartBlockInvestment.write(tag, installedFragments)
    }

    override fun load(tag: CompoundTag) {
        super.load(tag)
        installedFragments = HeartBlockInvestment.read(tag)
    }

    companion object {
        const val FRAGMENTS_TAG = "Fragments"

        @JvmStatic
        fun tick(level: Level, pos: BlockPos, state: BlockState, heart: HeartBlockEntity) {
            if (heart.installedFragments <= 0) return
            // A fixed neighbour order makes a shared altar choice deterministic. One heart never
            // pays the same installed fragment into multiple adjacent altars in one tick.
            val altar = Direction.values().asSequence()
                .mapNotNull { direction -> level.getBlockEntity(pos.relative(direction)) as? IBloodAltar }
                .firstOrNull() ?: return
            HeartBlockEmission.fill(heart.installedFragments, altar::fillMainTank)
        }
    }
}

/**
 * Converts permanently installed fragments to an altar request without silently truncating a
 * valid long count at the int-shaped Blood Magic API boundary.
 */
internal object HeartBlockInvestment {
    fun normalize(value: Long): Long = value.coerceAtLeast(0)
    fun addOne(value: Long): Long? = normalize(value).takeUnless { it == Long.MAX_VALUE }?.plus(1)
    fun write(tag: CompoundTag, fragments: Long) = tag.putLong(HeartBlockEntity.FRAGMENTS_TAG, normalize(fragments))
    fun read(tag: CompoundTag): Long = normalize(tag.getLong(HeartBlockEntity.FRAGMENTS_TAG))
}

internal object HeartBlockEmission {
    fun fill(installedFragments: Long, fillAltar: (Int) -> Int): Long {
        var remaining = installedFragments.coerceAtLeast(0)
        var inserted = 0L
        while (remaining > 0) {
            val request = minOf(remaining, Int.MAX_VALUE.toLong()).toInt()
            val accepted = fillAltar(request).coerceIn(0, request)
            inserted += accepted.toLong()
            if (accepted < request) break
            remaining -= request.toLong()
        }
        return inserted
    }
}
