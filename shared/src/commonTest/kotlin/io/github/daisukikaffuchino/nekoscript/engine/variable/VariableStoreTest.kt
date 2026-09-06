package io.github.daisukikaffuchino.nekoscript.engine.variable

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class VariableStoreTest {
    @Test
    fun supportsEveryVariableTypeWithoutMutatingOriginalStore() {
        val empty = VariableStore()
        val populated = empty
            .put("score", Variable.IntValue(7))
            .put("seen", Variable.BooleanValue(true))
            .put("ratio", Variable.DoubleValue(0.5))
            .put("player.name", Variable.StringValue("Mira"))

        assertNull(empty["score"])
        assertEquals(Variable.IntValue(7), populated["score"])
        assertEquals(Variable.BooleanValue(true), populated["seen"])
        assertEquals(Variable.DoubleValue(0.5), populated["ratio"])
        assertEquals(Variable.StringValue("Mira"), populated["player.name"])
    }

    @Test
    fun canReplaceAndRemoveVariable() {
        val initial = VariableStore().put("score", Variable.IntValue(1))
        val replaced = initial.put("score", Variable.IntValue(2))

        assertEquals(Variable.IntValue(1), initial["score"])
        assertEquals(Variable.IntValue(2), replaced["score"])
        assertNull(replaced.remove("score")["score"])
    }

    @Test
    fun rejectsBlankVariableName() {
        assertFailsWith<IllegalArgumentException> {
            VariableStore().put(" ", Variable.BooleanValue(true))
        }
    }
}
