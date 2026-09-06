package io.github.daisukikaffuchino.nekoscript.engine.dialogue

import kotlinx.serialization.Serializable

/**
 * One complete dialogue line produced by the engine.
 *
 * @property speaker speaker identifier or display name; `null` denotes narration
 * @property text complete text of the line, independent of presentation animation
 */
@Serializable
data class Dialogue(
    val speaker: String?,
    val text: String,
)
