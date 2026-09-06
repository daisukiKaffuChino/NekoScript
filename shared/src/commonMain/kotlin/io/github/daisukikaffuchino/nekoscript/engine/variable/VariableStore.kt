package io.github.daisukikaffuchino.nekoscript.engine.variable

import kotlinx.serialization.Serializable

/**
 * Immutable collection of script variables.
 *
 * Dotted names such as `affection.yuki` are ordinary keys and are not tied to
 * any particular game project.
 *
 * @property values variables keyed by their script-visible names
 */
@Serializable
data class VariableStore(
    val values: Map<String, Variable> = emptyMap(),
) {
    /** Returns the variable associated with [name], or `null` when it is unset. */
    operator fun get(name: String): Variable? = values[name]

    /** Returns a new store in which [name] is associated with [value]. */
    fun put(name: String, value: Variable): VariableStore {
        require(name.isNotBlank()) { "Variable name must not be blank." }
        return copy(values = values + (name to value))
    }

    /** Returns a new store without the variable named [name]. */
    fun remove(name: String): VariableStore = copy(values = values - name)
}
