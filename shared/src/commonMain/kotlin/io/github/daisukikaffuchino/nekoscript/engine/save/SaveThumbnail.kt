package io.github.daisukikaffuchino.nekoscript.engine.save

import io.github.daisukikaffuchino.nekoscript.engine.character.CharacterPosition
import io.github.daisukikaffuchino.nekoscript.engine.runtime.GameState
import kotlinx.serialization.Serializable

/** Compact logical snapshot used to render a save-slot thumbnail. */
@Serializable
data class SaveThumbnail(
    val backgroundId: String? = null,
    val cgId: String? = null,
    val characters: List<SaveThumbnailCharacter> = emptyList(),
) {
    init {
        require(characters.isNotEmpty() || backgroundId != null || cgId != null) {
            "Save thumbnail must contain at least one visible element or background."
        }
    }
}

@Serializable
data class SaveThumbnailCharacter(
    val characterId: String,
    val expression: String? = null,
    val position: CharacterPosition = CharacterPosition.Center,
)

fun GameState.toSaveThumbnail(): SaveThumbnail? {
    val thumbnailCharacters = characters.map {
        SaveThumbnailCharacter(
            characterId = it.characterId,
            expression = it.expression,
            position = it.position,
        )
    }
    return if (background != null || cgId != null || thumbnailCharacters.isNotEmpty()) {
        SaveThumbnail(
            backgroundId = background?.backgroundId,
            cgId = cgId,
            characters = thumbnailCharacters,
        )
    } else {
        null
    }
}
