package io.github.daisukikaffuchino.nekoscript.engine.interaction

import io.github.daisukikaffuchino.nekoscript.engine.viewport.LogicalPoint
import io.github.daisukikaffuchino.nekoscript.engine.viewport.LogicalRect

/** Platform-independent scene area that can be hit-tested with logical coordinates. */
interface InteractiveArea {
    /** Stable identifier forwarded to the engine when this area is activated. */
    val id: String

    /** Returns whether [point] activates this area. */
    fun hitTest(point: LogicalPoint): Boolean
}

/** First-version rectangular interactive area. */
data class RectHotspot(
    override val id: String,
    val bounds: LogicalRect,
) : InteractiveArea {
    init {
        require(id.isNotBlank()) { "Hotspot id must not be blank." }
    }

    override fun hitTest(point: LogicalPoint): Boolean = point in bounds
}
