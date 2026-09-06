package io.github.daisukikaffuchino.nekoscript.engine.script

import kotlinx.serialization.Serializable

/**
 * Identifies the next node to execute in a script.
 *
 * The script itself is identified by the owning game state, so this value only
 * contains the zero-based node index.
 *
 * @property nodeIndex zero-based index of the next script node
 */
@Serializable
data class ScriptPosition(
    val nodeIndex: Int = 0,
) {
    init {
        require(nodeIndex >= 0) { "Script node index must not be negative." }
    }
}
