package io.github.daisukikaffuchino.nekoscript.engine.interaction

import io.github.daisukikaffuchino.nekoscript.engine.viewport.LogicalPoint

/**
 * Mutable collection of active scene hotspots.
 *
 * IDs are unique. Adding an existing ID replaces its area and makes it the most recently added,
 * so it receives priority when hotspots overlap.
 */
class HotspotRegistry(
    initialAreas: Iterable<InteractiveArea> = emptyList(),
) {
    private val areas = mutableListOf<InteractiveArea>()

    init {
        initialAreas.forEach(::add)
    }

    /** Number of currently registered hotspots. */
    val size: Int
        get() = areas.size

    /** Adds [area], replacing and reprioritizing an existing area with the same ID. */
    fun add(area: InteractiveArea) {
        require(area.id.isNotBlank()) { "Hotspot id must not be blank." }
        remove(area.id)
        areas += area
    }

    /** Removes the hotspot identified by [id], returning whether one was present. */
    fun remove(id: String): Boolean = areas.removeAll { it.id == id }

    /** Removes all hotspots. */
    fun clear() = areas.clear()

    /** Returns the hotspot identified by [id], or `null` when it is not registered. */
    fun findById(id: String): InteractiveArea? = areas.firstOrNull { it.id == id }

    /** Returns the highest-priority hotspot containing [point], or `null` when none is hit. */
    fun hitTest(point: LogicalPoint): InteractiveArea? =
        areas.asReversed().firstOrNull { it.hitTest(point) }
}
