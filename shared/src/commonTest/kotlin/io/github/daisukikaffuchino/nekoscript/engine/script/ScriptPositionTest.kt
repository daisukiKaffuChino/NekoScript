package io.github.daisukikaffuchino.nekoscript.engine.script

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ScriptPositionTest {
    @Test
    fun startsAtFirstNodeByDefault() {
        assertEquals(0, ScriptPosition().nodeIndex)
    }

    @Test
    fun rejectsNegativeNodeIndex() {
        assertFailsWith<IllegalArgumentException> { ScriptPosition(-1) }
    }
}
