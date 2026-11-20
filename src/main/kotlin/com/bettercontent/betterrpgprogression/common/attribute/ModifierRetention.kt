package com.bettercontent.betterrpgprogression.common.attribute

/** Cold Sweat starts each default gather from the player's existing modifiers. Retain one owned
 * instance per trait without changing any other mod's entries, including on saved-world reopen. */
internal fun <T : Any> retainSingleOwnedModifier(
    modifiers: MutableList<T>,
    owned: (T) -> Boolean,
    create: () -> T,
) {
    val existing = modifiers.filter(owned)
    if (existing.size == 1) return
    if (existing.isNotEmpty()) modifiers.removeAll(owned)
    modifiers.add(existing.firstOrNull() ?: create())
}
