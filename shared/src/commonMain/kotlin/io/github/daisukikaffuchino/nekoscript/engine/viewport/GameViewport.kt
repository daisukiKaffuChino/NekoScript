package io.github.daisukikaffuchino.nekoscript.engine.viewport

/** Fixed logical canvas used by a game independently of the host window size. */
data class GameViewport(
    val logicalWidth: Int = DEFAULT_LOGICAL_WIDTH,
    val logicalHeight: Int = DEFAULT_LOGICAL_HEIGHT,
) {
    init {
        require(logicalWidth > 0) { "Logical viewport width must be positive." }
        require(logicalHeight > 0) { "Logical viewport height must be positive." }
    }

    /** Width-to-height ratio of the logical canvas. */
    val aspectRatio: Double
        get() = logicalWidth.toDouble() / logicalHeight.toDouble()

    /** Alias for [logicalWidth] when treating the viewport as a rectangle. */
    val width: Int
        get() = logicalWidth

    /** Alias for [logicalHeight] when treating the viewport as a rectangle. */
    val height: Int
        get() = logicalHeight

    /** Logical dimensions represented by this viewport. */
    val logicalSize: LogicalSize
        get() = LogicalSize(logicalWidth.toDouble(), logicalHeight.toDouble())

    /** Calculates an equal-scale fit of this viewport inside [container]. */
    fun fit(container: ContainerSize): ViewportFit = fit(container.width, container.height)

    /** Calculates an equal-scale fit inside a host container. */
    fun fit(containerWidth: Double, containerHeight: Double): ViewportFit {
        val container = ContainerSize(containerWidth, containerHeight)
        val scale = minOf(
            container.width / logicalWidth.toDouble(),
            container.height / logicalHeight.toDouble(),
        )
        val contentWidth = logicalWidth * scale
        val contentHeight = logicalHeight * scale
        val contentRect = ViewportRect(
            left = (container.width - contentWidth) / 2.0,
            top = (container.height - contentHeight) / 2.0,
            width = contentWidth,
            height = contentHeight,
        )
        return ViewportFit(this, container, scale, contentRect)
    }

    companion object {
        /** Default logical canvas width. */
        const val DEFAULT_LOGICAL_WIDTH: Int = 1_920

        /** Default logical canvas height. */
        const val DEFAULT_LOGICAL_HEIGHT: Int = 1_080

        /** Standard 16:9 game viewport. */
        val DEFAULT: GameViewport = GameViewport()
    }
}

/** A logical coordinate in the game canvas. */
data class LogicalPoint(
    val x: Double,
    val y: Double,
)

/** A logical size in the game canvas. */
data class LogicalSize(
    val width: Double,
    val height: Double,
) {
    init {
        require(width > 0.0) { "Logical size width must be positive." }
        require(height > 0.0) { "Logical size height must be positive." }
    }
}

/** Dimensions supplied by a host window or device container. */
data class ContainerSize(
    val width: Double,
    val height: Double,
) {
    init {
        require(width > 0.0) { "Container width must be positive." }
        require(height > 0.0) { "Container height must be positive." }
    }
}

/** A point in the host container's coordinate space. */
data class ContainerPoint(
    val x: Double,
    val y: Double,
)

/** The centered, uniformly scaled game rectangle inside a host container. */
data class ViewportRect(
    val left: Double,
    val top: Double,
    val width: Double,
    val height: Double,
) {
    init {
        require(width >= 0.0) { "Viewport rectangle width must not be negative." }
        require(height >= 0.0) { "Viewport rectangle height must not be negative." }
    }

    val right: Double
        get() = left + width

    val bottom: Double
        get() = top + height
}

/** Equal-scale mapping from logical coordinates to a host container. */
data class ViewportFit(
    val viewport: GameViewport,
    val container: ContainerSize,
    val scale: Double,
    val contentRect: ViewportRect,
) {
    init {
        require(scale > 0.0) { "Viewport scale must be positive." }
    }

    /** Horizontal letterbox/pillarbox inset in container coordinates. */
    val offsetX: Double
        get() = contentRect.left

    /** Vertical letterbox/pillarbox inset in container coordinates. */
    val offsetY: Double
        get() = contentRect.top

    /** Width of the fitted game content in container coordinates. */
    val scaledWidth: Double
        get() = contentRect.width

    /** Height of the fitted game content in container coordinates. */
    val scaledHeight: Double
        get() = contentRect.height

    /** Maps a logical point into the fitted content rectangle. */
    fun toContainer(point: LogicalPoint): ContainerPoint = ContainerPoint(
        x = contentRect.left + point.x * scale,
        y = contentRect.top + point.y * scale,
    )

    /** Maps a host-container point back into logical coordinates. */
    fun toLogical(point: ContainerPoint): LogicalPoint = LogicalPoint(
        x = (point.x - contentRect.left) / scale,
        y = (point.y - contentRect.top) / scale,
    )

    /**
     * Maps a host-container point into logical coordinates when it is inside the game content.
     * Points in letterbox or pillarbox space return `null`.
     */
    fun toLogicalOrNull(point: ContainerPoint): LogicalPoint? =
        if (contains(point)) toLogical(point) else null

    /** Returns whether a host point lies inside the fitted game content. */
    fun contains(point: ContainerPoint): Boolean =
        point.x >= contentRect.left && point.x <= contentRect.right &&
            point.y >= contentRect.top && point.y <= contentRect.bottom
}
