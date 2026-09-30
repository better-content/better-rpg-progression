package com.bettercontent.betterrpgprogression.common.points

import com.bettercontent.betterrpgprogression.common.data.PlayerStats

/** Server-side plan validation and one-point-at-a-time application. */
object AutoAllocationPlan {
    const val MAX_ENTRIES = 256

    fun replace(stats: PlayerStats, enabled: Boolean, requested: List<String>, admitted: Map<String, Int>): Boolean {
        if (requested.size > MAX_ENTRIES) return false
        if (requested.any { it.isNotEmpty() && it !in admitted }) return false
        stats.autoAllocationPlan.clear()
        stats.autoAllocationPlan += requested
        stats.autoAllocationEnabled = enabled && requested.any(String::isNotEmpty)
        stats.autoAllocationCursor = 0
        return true
    }

    /** The entry at index level - 1 owns only that level's point. Gaps remain unspent. */
    fun apply(stats: PlayerStats, admitted: Map<String, Int>, firstLevel: Int, lastLevel: Int): Map<String, Int> {
        if (!stats.autoAllocationEnabled || stats.unspentPoints <= 0 || stats.autoAllocationPlan.isEmpty()) return emptyMap()
        val spent = linkedMapOf<String, Int>()
        for (level in firstLevel..minOf(lastLevel, stats.autoAllocationPlan.size)) {
            if (level <= 0 || stats.unspentPoints <= 0) continue
            val id = stats.autoAllocationPlan[level - 1]
            if (id.isEmpty()) continue
            val maximum = admitted[id]
            val current = stats.allocations[id] ?: 0
            if (maximum == null || (maximum >= 0 && current >= maximum)) continue
            stats.allocations[id] = current + 1
            stats.unspentPoints--
            spent[id] = (spent[id] ?: 0) + 1
        }
        return spent
    }
}
