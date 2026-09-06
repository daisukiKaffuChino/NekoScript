package io.github.daisukikaffuchino.nekoscript.engine.choice

import io.github.daisukikaffuchino.nekoscript.engine.command.Command
import kotlinx.serialization.Serializable

/**
 * One player-selectable branch and the commands it contributes when selected.
 *
 * @property text text presented for this option
 * @property commands commands executed in order after selection
 */
@Serializable
data class ChoiceItem(
    val text: String,
    val commands: List<Command>,
) {
    init {
        require(text.isNotBlank()) { "Choice text must not be blank." }
    }
}
