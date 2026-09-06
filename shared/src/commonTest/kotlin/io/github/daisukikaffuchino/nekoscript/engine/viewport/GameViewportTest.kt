package io.github.daisukikaffuchino.nekoscript.engine.viewport

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameViewportTest {
    @Test
    fun defaultsToA1920By1080SixteenByNineLogicalCanvas() {
        val viewport = GameViewport.DEFAULT

        assertEquals(1_920, viewport.logicalWidth)
        assertEquals(1_080, viewport.logicalHeight)
        assertClose(16.0 / 9.0, viewport.aspectRatio)
        assertEquals(LogicalSize(1_920.0, 1_080.0), viewport.logicalSize)
    }

    @Test
    fun rejectsNonPositiveLogicalOrContainerDimensions() {
        assertFailsWith<IllegalArgumentException> { GameViewport(0, 1_080) }
        assertFailsWith<IllegalArgumentException> { GameViewport(1_920, -1) }
        assertFailsWith<IllegalArgumentException> { ContainerSize(0.0, 100.0) }
        assertFailsWith<IllegalArgumentException> { GameViewport.DEFAULT.fit(100.0, -1.0) }
    }

    @Test
    fun fitsAnUltraTallAndroidContainerWithoutStretching() {
        val fit = GameViewport.DEFAULT.fit(1_080.0, 2_400.0)

        assertClose(0.5625, fit.scale)
        assertClose(1_080.0, fit.contentRect.width)
        assertClose(607.5, fit.contentRect.height)
        assertClose(0.0, fit.contentRect.left)
        assertClose(896.25, fit.contentRect.top)
        assertClose(16.0 / 9.0, fit.contentRect.width / fit.contentRect.height)
    }

    @Test
    fun centersContentInWideSixteenByNineAndSixteenByTenContainers() {
        val wide = GameViewport.DEFAULT.fit(2_400.0, 1_080.0)
        assertClose(1.0, wide.scale)
        assertClose(240.0, wide.contentRect.left)
        assertClose(0.0, wide.contentRect.top)

        val sixteenByTen = GameViewport.DEFAULT.fit(1_600.0, 1_000.0)
        assertClose(0.8333333333333334, sixteenByTen.scale)
        assertClose(1_600.0, sixteenByTen.contentRect.width)
        assertClose(900.0, sixteenByTen.contentRect.height)
        assertClose(0.0, sixteenByTen.contentRect.left)
        assertClose(50.0, sixteenByTen.contentRect.top)
    }

    @Test
    fun fitsFourByThreeContainerWithVerticalLetterbox() {
        val fit = GameViewport.DEFAULT.fit(ContainerSize(1_024.0, 768.0))

        assertClose(0.5333333333333333, fit.scale)
        assertClose(1_024.0, fit.contentRect.width)
        assertClose(576.0, fit.contentRect.height)
        assertClose(0.0, fit.offsetX)
        assertClose(96.0, fit.offsetY)
    }

    @Test
    fun mapsLogicalCoordinatesAndRejectsLetterboxInput() {
        val fit = GameViewport.DEFAULT.fit(1_080.0, 2_400.0)
        val logical = LogicalPoint(960.0, 540.0)
        val container = fit.toContainer(logical)

        assertClose(540.0, container.x)
        assertClose(1_200.0, container.y)
        assertTrue(fit.contains(container))
        assertFalse(fit.contains(ContainerPoint(10.0, 10.0)))

        val roundTrip = fit.toLogical(container)
        assertClose(logical.x, roundTrip.x)
        assertClose(logical.y, roundTrip.y)
    }

    private fun assertClose(expected: Double, actual: Double) {
        assertTrue(
            abs(expected - actual) < 0.000001,
            "Expected $expected, but was $actual",
        )
    }
}
