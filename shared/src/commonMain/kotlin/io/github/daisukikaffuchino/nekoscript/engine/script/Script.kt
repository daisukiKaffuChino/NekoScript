package io.github.daisukikaffuchino.nekoscript.engine.script

/**
 * Parsed, immutable script consumed by a runtime.
 *
 * @property id project-defined script identifier
 * @property nodes ordered script syntax tree
 * @property labels node indexes keyed by label name
 */
data class Script(
    val id: String,
    val nodes: List<ScriptNode>,
    val labels: Map<String, Int>,
) {
    init {
        require(id.isNotBlank()) { "Script id must not be blank." }
    }
}
