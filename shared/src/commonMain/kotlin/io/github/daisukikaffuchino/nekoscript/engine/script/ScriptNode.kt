package io.github.daisukikaffuchino.nekoscript.engine.script

import io.github.daisukikaffuchino.nekoscript.engine.command.Command

/** One node in a parsed script syntax tree. */
sealed interface ScriptNode {
    /** @property name label name available as a flow-control destination */
    data class Label(val name: String) : ScriptNode

    /** @property command declarative engine command represented by this node */
    data class CommandNode(val command: Command) : ScriptNode
}
