package io.github.daisukikaffuchino.nekoscript.engine.variable

import io.github.daisukikaffuchino.nekoscript.engine.error.EngineException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Boolean expression evaluated against a [VariableStore] by the runtime. */
@Serializable
sealed interface Condition {
    /**
     * Compares the variable named [name] with [value] using [operator].
     * An unset variable evaluates to `false` for every operator.
     */
    @Serializable
    @SerialName("variable_comparison")
    data class VariableComparison(
        val name: String,
        val operator: ComparisonOperator,
        val value: Variable,
    ) : Condition {
        init {
            require(name.isNotBlank()) { "Condition variable name must not be blank." }
        }
    }
}

/** Operators supported by [Condition.VariableComparison]. */
@Serializable
enum class ComparisonOperator {
    Equal,
    NotEqual,
    Greater,
    GreaterOrEqual,
    Less,
    LessOrEqual,
}

/** Evaluates this condition against [variables]. */
fun Condition.evaluate(variables: VariableStore): Boolean {
    return when (this) {
        is Condition.VariableComparison -> {
            val actual = variables[name] ?: return false
            when (operator) {
                ComparisonOperator.Equal -> valuesEqual(actual, value)
                ComparisonOperator.NotEqual -> !valuesEqual(actual, value)
                ComparisonOperator.Greater -> compareOrdered(actual, value) > 0
                ComparisonOperator.GreaterOrEqual -> compareOrdered(actual, value) >= 0
                ComparisonOperator.Less -> compareOrdered(actual, value) < 0
                ComparisonOperator.LessOrEqual -> compareOrdered(actual, value) <= 0
            }
        }
    }
}

private fun valuesEqual(left: Variable, right: Variable): Boolean = when {
    left is Variable.IntValue && right is Variable.DoubleValue -> left.value.toDouble() == right.value
    left is Variable.DoubleValue && right is Variable.IntValue -> left.value == right.value.toDouble()
    else -> left == right
}

private fun compareOrdered(left: Variable, right: Variable): Int {
    val leftNumber = left.asDoubleOrNull()
    val rightNumber = right.asDoubleOrNull()
    if (leftNumber != null && rightNumber != null) return leftNumber.compareTo(rightNumber)
    if (left is Variable.StringValue && right is Variable.StringValue) return left.value.compareTo(right.value)
    throw EngineException.InvalidVariable(
        "Cannot compare ${left::class.simpleName} with ${right::class.simpleName} using an ordered operator.",
    )
}

private fun Variable.asDoubleOrNull(): Double? = when (this) {
    is Variable.IntValue -> value.toDouble()
    is Variable.DoubleValue -> value
    is Variable.BooleanValue, is Variable.StringValue -> null
}
