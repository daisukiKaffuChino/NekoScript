package io.github.daisukikaffuchino.nekoscript.engine.script

import io.github.daisukikaffuchino.nekoscript.engine.runtime.GameState
import kotlinx.coroutines.flow.StateFlow

/** Executes a parsed script and exposes immutable game state snapshots. */
interface ScriptRuntime {
    /** Current logical game state. */
    val state: StateFlow<GameState>

    /** Advances execution from the current input pause point. */
    suspend fun next()

    /** Selects a pending choice by zero-based [index] and resumes execution. */
    suspend fun selectChoice(index: Int)

    /** Jumps to a validated script label in response to a bound scene interaction. */
    suspend fun jumpToLabel(label: String)

    /** Replaces the current runtime state with a validated save snapshot. */
    suspend fun restore(state: GameState)

    /** Resumes after the renderer completes the pending visual effect. */
    suspend fun completeVisualEffect(sequence: Long)
}
