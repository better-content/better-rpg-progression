package com.bettercontent.rpgstats.client.cache

data class ClientStatsSnapshot(
    val unspent: Int = 0,
    val lifePeak: Int = 0,
    val allocations: Map<String, Int> = emptyMap(),
    val autoAllocationEnabled: Boolean = false,
    val autoAllocationPlan: List<String> = emptyList(),
    val autoAllocationCursor: Int = 0
)

data class ClientCurveDef(
    val type: String,
    val cap: Double,
    val k: Double,
    val perPoint: Double,
    val min: Double,
    val max: Double
)

data class ClientEffectDef(
    val attributeId: String,
    val operation: Int,
    val curve: ClientCurveDef,
    val isPrimary: Boolean = true,
    val displayAsPercent: Boolean = operation != 0
)

data class ClientStatDef(
    val id: String,
    val nameKey: String,
    val maxPoints: Int,
    val effects: List<ClientEffectDef>,
    val icon: String = "",
    val color: Int = 0xFFFFFFFF.toInt()
)

object ClientCache {
    @Volatile var defs: List<ClientStatDef> = emptyList()
    @Volatile var stats: ClientStatsSnapshot = ClientStatsSnapshot()
}
