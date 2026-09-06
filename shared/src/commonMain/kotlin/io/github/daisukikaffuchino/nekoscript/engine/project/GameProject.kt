package io.github.daisukikaffuchino.nekoscript.engine.project

import kotlinx.serialization.Serializable

/**
 * Portable game project manifest normally loaded from `game.json`.
 *
 * Locations are opaque to Engine Core and interpreted by the host asset loader.
 */
@Serializable
data class GameProject(
    val id: String,
    val name: String,
    val version: String,
    val entryScript: String,
    val backgrounds: Map<String, String> = emptyMap(),
    val characters: Map<String, CharacterDefinition> = emptyMap(),
    val cg: Map<String, String> = emptyMap(),
    val audio: AudioManifest = AudioManifest(),
) {
    init {
        require(id.isNotBlank()) { "Game project id must not be blank." }
        require(name.isNotBlank()) { "Game project name must not be blank." }
        require(version.isNotBlank()) { "Game project version must not be blank." }
        require(entryScript.isNotBlank()) { "Game project entry script must not be blank." }
    }
}

/** Character metadata and expression locations declared by a game project. */
@Serializable
data class CharacterDefinition(
    val name: String,
    val defaultExpression: String,
    val expressions: Map<String, String>,
) {
    init {
        require(name.isNotBlank()) { "Character name must not be blank." }
        require(defaultExpression in expressions) { "Default expression must exist in the expression map." }
    }
}

/** Logical audio identifier mappings declared by a game project. */
@Serializable
data class AudioManifest(
    val bgm: Map<String, String> = emptyMap(),
    val se: Map<String, String> = emptyMap(),
    val voice: Map<String, String> = emptyMap(),
)
