package io.github.daisukikaffuchino.nekoscript.engine.character

import kotlinx.serialization.Serializable

/** Logical placement of a character in the scene. */
@Serializable
enum class CharacterPosition {
    Left,
    Center,
    Right,
}

/**
 * Runtime state of a visible character.
 *
 * Asset lookup is deliberately left to an asset manager and is not represented
 * by a file path here.
 *
 * @property characterId project-defined character identifier
 * @property expression project-defined expression identifier, if one is selected
 * @property position logical placement in the scene
 */
@Serializable
data class CharacterState(
    val characterId: String,
    val expression: String? = null,
    val position: CharacterPosition = CharacterPosition.Center,
) {
    init {
        require(characterId.isNotBlank()) { "Character id must not be blank." }
    }
}
