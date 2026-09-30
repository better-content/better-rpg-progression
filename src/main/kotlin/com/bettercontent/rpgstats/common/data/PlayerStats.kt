package com.bettercontent.rpgstats.common.data

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag

class PlayerStats {
    var lifePeakLevel: Int = 0
    var unspentPoints: Int = 0
    var lifeAllocationEpisode: String? = null
    val allocations: MutableMap<String, Int> = mutableMapOf()
    /** A durable, ordered spending preference. It belongs to the character, not a Life. */
    var autoAllocationEnabled: Boolean = false
    var autoAllocationCursor: Int = 0
    val autoAllocationPlan: MutableList<String> = mutableListOf()

    fun totalAllocated(): Int = allocations.values.sum()

    fun totalPointsThisLife(): Int = unspentPoints + totalAllocated()

    fun resetForDeath(currentLevel: Int) {
        lifePeakLevel = currentLevel.coerceAtLeast(0)
        unspentPoints = 0
        lifeAllocationEpisode = null
        allocations.clear()
    }

    fun serializeNBT(): CompoundTag {
        val tag = CompoundTag()
        tag.putInt("lifePeakLevel", lifePeakLevel)
        tag.putInt("unspentPoints", unspentPoints)
        lifeAllocationEpisode?.takeIf { it.isNotBlank() && it.length <= 128 }?.let { tag.putString("lifeAllocationEpisode", it) }

        val list = ListTag()
        allocations.forEach { (id, pts) ->
            val e = CompoundTag()
            e.putString("id", id)
            e.putInt("pts", pts)
            list.add(e)
        }
        tag.put("allocations", list)
        tag.putBoolean("autoAllocationEnabled", autoAllocationEnabled)
        tag.putInt("autoAllocationCursor", autoAllocationCursor.coerceAtLeast(0))
        val plan = ListTag()
        autoAllocationPlan.take(256).forEach { id ->
            plan.add(net.minecraft.nbt.StringTag.valueOf(id.take(128)))
        }
        tag.put("autoAllocationPlan", plan)
        return tag
    }

    fun deserializeNBT(tag: CompoundTag) {
        lifePeakLevel = tag.getInt("lifePeakLevel")
        unspentPoints = tag.getInt("unspentPoints")
        lifeAllocationEpisode = tag.getString("lifeAllocationEpisode").takeIf { it.isNotBlank() && it.length <= 128 }
        allocations.clear()

        val list = tag.getList("allocations", Tag.TAG_COMPOUND.toInt())
        for (i in 0 until list.size) {
            val e = list.getCompound(i)
            val id = e.getString("id")
            val pts = e.getInt("pts")
            if (id.isNotBlank() && pts > 0) allocations[id] = pts
        }
        autoAllocationEnabled = tag.getBoolean("autoAllocationEnabled")
        autoAllocationCursor = tag.getInt("autoAllocationCursor").coerceAtLeast(0)
        autoAllocationPlan.clear()
        val plan = tag.getList("autoAllocationPlan", Tag.TAG_STRING.toInt())
        for (i in 0 until minOf(plan.size, 256)) {
            val id = plan.getString(i)
            autoAllocationPlan += id.take(128)
        }
        if (autoAllocationPlan.isEmpty()) autoAllocationCursor = 0
    }
}
