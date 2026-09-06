package io.github.daisukikaffuchino.nekoscript.engine.save

import io.github.daisukikaffuchino.nekoscript.engine.runtime.GameState

/** Creates and restores versioned game-state snapshots. */
interface SaveManager {
    /** Serializes [state] into [slot] and returns its persisted envelope. */
    suspend fun save(slot: String, state: GameState): SaveData

    /** Returns the migrated save in [slot], or `null` when the slot is empty. */
    suspend fun load(slot: String): SaveData?
}
