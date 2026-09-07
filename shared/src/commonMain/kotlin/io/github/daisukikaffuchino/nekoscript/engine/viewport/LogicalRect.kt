package io.github.daisukikaffuchino.nekoscript.engine.viewport

/**
 * Axis-aligned rectangle expressed entirely in game logical coordinates.
 *
 * The left and top edges are included; the right and bottom edges are excluded.
 */
data class LogicalRect(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
) {
    init {
        require(x.isFinite()) { "Logical rectangle x must be finite." }
        require(y.isFinite()) { "Logical rectangle y must be finite." }
        require(width.isFinite() && width > 0.0) {
            "Logical rectangle width must be finite and positive."
        }
        require(height.isFinite() && height > 0.0) {
            "Logical rectangle height must be finite and positive."
        }
        require((x + width).isFinite()) { "Logical rectangle right edge must be finite." }
        require((y + height).isFinite()) { "Logical rectangle bottom edge must be finite." }
    }

    /** Exclusive horizontal boundary. */
    val right: Double
        get() = x + width

    /** Exclusive vertical boundary. */
    val bottom: Double
        get() = y + height

    /** Returns whether [point] lies inside this rectangle using half-open edges. */
    operator fun contains(point: LogicalPoint): Boolean =
        point.x >= x && point.x < right && point.y >= y && point.y < bottom
}
