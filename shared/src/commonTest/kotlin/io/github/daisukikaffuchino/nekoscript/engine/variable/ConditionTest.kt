package io.github.daisukikaffuchino.nekoscript.engine.variable

import io.github.daisukikaffuchino.nekoscript.engine.error.EngineException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConditionTest {
    @Test
    fun evaluatesEqualityAndNumericOrdering() {
        val variables = VariableStore()
            .put("score", Variable.IntValue(10))
            .put("name", Variable.StringValue("Yuki"))

        assertTrue(comparison("score", ComparisonOperator.GreaterOrEqual, Variable.DoubleValue(10.0)).evaluate(variables))
        assertTrue(comparison("name", ComparisonOperator.Equal, Variable.StringValue("Yuki")).evaluate(variables))
        assertTrue(comparison("score", ComparisonOperator.NotEqual, Variable.IntValue(9)).evaluate(variables))
        assertFalse(comparison("missing", ComparisonOperator.NotEqual, Variable.IntValue(0)).evaluate(variables))
    }

    @Test
    fun rejectsOrderedComparisonAcrossIncompatibleTypes() {
        val variables = VariableStore().put("flag", Variable.BooleanValue(true))

        assertFailsWith<EngineException.InvalidVariable> {
            comparison("flag", ComparisonOperator.Greater, Variable.BooleanValue(false)).evaluate(variables)
        }
    }

    private fun comparison(
        name: String,
        operator: ComparisonOperator,
        value: Variable,
    ) = Condition.VariableComparison(name, operator, value)
}
