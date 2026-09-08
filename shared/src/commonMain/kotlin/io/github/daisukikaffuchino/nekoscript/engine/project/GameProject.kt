package io.github.daisukikaffuchino.nekoscript.engine.project

import kotlinx.serialization.Serializable
import io.github.daisukikaffuchino.nekoscript.engine.viewport.GameViewport

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
    val debuggable: Boolean = false,
    val entryScript: String,
    val viewport: ViewportConfig,
    val backgrounds: Map<String, String> = emptyMap(),
    val characters: Map<String, CharacterDefinition> = emptyMap(),
    val cg: Map<String, String> = emptyMap(),
    val audio: AudioManifest = AudioManifest(),
    val hotspots: Map<String, HotspotDefinition> = emptyMap(),
) {
    init {
        require(id.isNotBlank()) { "Game project id must not be blank." }
        require(name.isNotBlank()) { "Game project name must not be blank." }
        require(version.isNotBlank()) { "Game project version must not be blank." }
        require(entryScript.isNotBlank()) { "Game project entry script must not be blank." }
    }
}

/** Serializable project-level logical canvas configuration. */
@Serializable
data class ViewportConfig(
    val width: Int = GameViewport.DEFAULT_LOGICAL_WIDTH,
    val height: Int = GameViewport.DEFAULT_LOGICAL_HEIGHT,
) {
    init {
        require(width > 0) { "Viewport width must be positive." }
        require(height > 0) { "Viewport height must be positive." }
    }

    fun toGameViewport(): GameViewport = GameViewport(width, height)
}

/** Project-defined logical scene hotspot with a first-version jump action. */
@Serializable
data class HotspotDefinition(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
    val action: String = ACTION_JUMP,
    val target: String,
) {
    init {
        require(x.isFinite() && y.isFinite()) { "Hotspot position must be finite." }
        require(width.isFinite() && height.isFinite() && width > 0.0 && height > 0.0) {
            "Hotspot size must be finite and positive."
        }
        require(action == ACTION_JUMP) { "Unsupported hotspot action: $action" }
        require(target.isNotBlank()) { "Hotspot target must not be blank." }
    }

    private companion object {
        const val ACTION_JUMP = "jump"
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
