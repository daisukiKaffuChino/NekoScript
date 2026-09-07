package io.github.daisukikaffuchino.nekoscript.engine.interaction

import io.github.daisukikaffuchino.nekoscript.engine.viewport.LogicalPoint
import io.github.daisukikaffuchino.nekoscript.engine.viewport.LogicalRect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class HotspotRegistryTest {
    @Test
    fun rectangularHotspotHitsInsideAndMissesOutside() {
        val hotspot = hotspot("door", x = 100.0, y = 100.0, width = 200.0, height = 100.0)

        assertTrue(hotspot.hitTest(LogicalPoint(150.0, 150.0)))
        assertFalse(hotspot.hitTest(LogicalPoint(99.999, 150.0)))
        assertFalse(hotspot.hitTest(LogicalPoint(150.0, 99.999)))
        assertFalse(hotspot.hitTest(LogicalPoint(301.0, 150.0)))
        assertFalse(hotspot.hitTest(LogicalPoint(150.0, 201.0)))
    }

    @Test
    fun rectangularHotspotUsesHalfOpenEdges() {
        val hotspot = hotspot("door", x = 100.0, y = 100.0, width = 200.0, height = 100.0)

        assertTrue(hotspot.hitTest(LogicalPoint(100.0, 100.0)))
        assertTrue(hotspot.hitTest(LogicalPoint(299.999, 199.999)))
        assertTrue(hotspot.hitTest(LogicalPoint(100.0, 199.999)))
        assertTrue(hotspot.hitTest(LogicalPoint(299.999, 100.0)))
        assertFalse(hotspot.hitTest(LogicalPoint(300.0, 100.0)))
        assertFalse(hotspot.hitTest(LogicalPoint(100.0, 200.0)))
        assertFalse(hotspot.hitTest(LogicalPoint(300.0, 200.0)))
    }

    @Test
    fun rejectsInvalidHotspotIdsAndBounds() {
        assertFailsWith<IllegalArgumentException> {
            RectHotspot(" ", LogicalRect(0.0, 0.0, 10.0, 10.0))
        }
        assertFailsWith<IllegalArgumentException> { LogicalRect(0.0, 0.0, 0.0, 10.0) }
        assertFailsWith<IllegalArgumentException> { LogicalRect(0.0, 0.0, 10.0, -1.0) }
        assertFailsWith<IllegalArgumentException> { LogicalRect(Double.NaN, 0.0, 10.0, 10.0) }
    }

    @Test
    fun emptyRegistryHasNoMatches() {
        val registry = HotspotRegistry()

        assertEquals(0, registry.size)
        assertNull(registry.findById("missing"))
        assertNull(registry.hitTest(LogicalPoint(10.0, 10.0)))
    }

    @Test
    fun addsFindsAndTestsNonOverlappingHotspots() {
        val left = hotspot("left", 0.0, 0.0, 100.0, 100.0)
        val right = hotspot("right", 100.0, 0.0, 100.0, 100.0)
        val registry = HotspotRegistry(listOf(left, right))

        assertEquals(2, registry.size)
        assertSame(left, registry.findById("left"))
        assertSame(left, registry.hitTest(LogicalPoint(50.0, 50.0)))
        assertSame(right, registry.hitTest(LogicalPoint(150.0, 50.0)))
    }

    @Test
    fun laterAddedOverlappingHotspotHasPriority() {
        val back = hotspot("back", 0.0, 0.0, 200.0, 200.0)
        val front = hotspot("front", 50.0, 50.0, 100.0, 100.0)
        val registry = HotspotRegistry()

        registry.add(back)
        registry.add(front)

        assertSame(front, registry.hitTest(LogicalPoint(75.0, 75.0)))
        assertSame(back, registry.hitTest(LogicalPoint(25.0, 25.0)))
    }

    @Test
    fun addingDuplicateIdReplacesAndReprioritizesTheHotspot() {
        val first = hotspot("target", 0.0, 0.0, 200.0, 200.0)
        val overlapping = hotspot("overlap", 0.0, 0.0, 200.0, 200.0)
        val replacement = hotspot("target", 300.0, 300.0, 100.0, 100.0)
        val registry = HotspotRegistry(listOf(first, overlapping))

        registry.add(replacement)

        assertEquals(2, registry.size)
        assertSame(replacement, registry.findById("target"))
        assertSame(overlapping, registry.hitTest(LogicalPoint(50.0, 50.0)))
        assertSame(replacement, registry.hitTest(LogicalPoint(350.0, 350.0)))
    }

    @Test
    fun removeMakesTheHotspotUnavailable() {
        val registry = HotspotRegistry(listOf(hotspot("door", 0.0, 0.0, 100.0, 100.0)))

        assertTrue(registry.remove("door"))
        assertFalse(registry.remove("door"))
        assertEquals(0, registry.size)
        assertNull(registry.findById("door"))
        assertNull(registry.hitTest(LogicalPoint(50.0, 50.0)))
    }

    @Test
    fun clearRemovesEveryHotspot() {
        val registry = HotspotRegistry(
            listOf(
                hotspot("one", 0.0, 0.0, 100.0, 100.0),
                hotspot("two", 100.0, 0.0, 100.0, 100.0),
            ),
        )

        registry.clear()

        assertEquals(0, registry.size)
        assertNull(registry.hitTest(LogicalPoint(50.0, 50.0)))
        assertNull(registry.hitTest(LogicalPoint(150.0, 50.0)))
    }

    private fun hotspot(
        id: String,
        x: Double,
        y: Double,
        width: Double,
        height: Double,
    ): RectHotspot = RectHotspot(id, LogicalRect(x, y, width, height))
}
