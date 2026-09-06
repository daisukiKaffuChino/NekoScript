package io.github.daisukikaffuchino.nekoscript.engine.save

import io.github.daisukikaffuchino.nekoscript.engine.runtime.GameState
import kotlinx.serialization.Serializable

/**
 * Versioned persistence envelope for one game-state snapshot.
 *
 * @property version schema version used to encode this save
 * @property timestamp epoch time in milliseconds when the save was created
 * @property state complete logical runtime state
 */
@Serializable
data class SaveData(
    val version: Int,
    val timestamp: Long,
    val state: GameState,
) {
    init {
        require(version >= 0) { "Save version must not be negative." }
        require(timestamp >= 0) { "Save timestamp must not be negative." }
    }
}
