package com.bettercontent.betterrpgprogression.common.network.packets

data class AllocationDecision(
    val allocations: Map<String, Int>,
    val unspentPoints: Int
)

/**
 * Validates an allocation draft without allowing committed points to move.
 * Existing allocations whose definitions are temporarily absent are preserved verbatim.
 */
object AllocationPolicy {
    fun apply(
        current: Map<String, Int>,
        unspentPoints: Int,
        requested: Map<String, Int>,
        maxPointsById: Map<String, Int>
    ): AllocationDecision? {
        if (unspentPoints < 0) return null

        val result = current.filterValues { it > 0 }.toMutableMap()
        var additionalSpent = 0L

        for ((id, maxPoints) in maxPointsById) {
            val committed = current[id]?.coerceAtLeast(0) ?: 0
            val desired = requested[id]?.coerceAtLeast(0) ?: 0
            if (desired < committed) return null

            val allowedMaximum = if (maxPoints >= 0) maxOf(committed, maxPoints) else Int.MAX_VALUE
            if (desired > allowedMaximum) return null

            additionalSpent += desired.toLong() - committed.toLong()
            if (additionalSpent > unspentPoints.toLong()) return null

            if (desired > 0) result[id] = desired else result.remove(id)
        }

        return AllocationDecision(
            allocations = result,
            unspentPoints = unspentPoints - additionalSpent.toInt()
        )
    }
}
