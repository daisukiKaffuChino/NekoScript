package io.github.daisukikaffuchino.nekoscript.engine.runtime

import io.github.daisukikaffuchino.nekoscript.engine.error.EngineException
import io.github.daisukikaffuchino.nekoscript.engine.script.ScriptRuntime
import io.github.daisukikaffuchino.nekoscript.engine.save.SaveManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** UI-facing facade that translates [GameAction] values into runtime operations. */
class GameEngine(
    private val runtime: ScriptRuntime,
    private val saveManager: SaveManager? = null,
) {
    private val dispatchMutex = Mutex()
    private var isAutoMode = false
    private var isSkipMode = false
    private var isBacklogOpen = false
    private var textSpeedMillis = DEFAULT_TEXT_SPEED_MILLIS
    private val mutableViewState = MutableStateFlow(currentViewState())

    /** Presentation-ready state observed by a renderer. */
    val viewState: StateFlow<GameViewState> = mutableViewState.asStateFlow()

    /** Handles one player or presentation action. */
    suspend fun dispatch(action: GameAction) = dispatchMutex.withLock {
        try {
            when (action) {
                GameAction.Next -> runtime.next()
                is GameAction.SelectChoice -> runtime.selectChoice(action.index)
                GameAction.ToggleAuto -> {
                    isAutoMode = !isAutoMode
                    if (isAutoMode) isSkipMode = false
                }
                GameAction.Skip -> {
                    isSkipMode = !isSkipMode
                    if (isSkipMode) isAutoMode = false
                }
                GameAction.OpenBacklog -> isBacklogOpen = true
                GameAction.CloseBacklog -> isBacklogOpen = false
                GameAction.QuickSave -> requireSaveManager().save(QUICK_SAVE_SLOT, runtime.state.value)
                GameAction.QuickLoad -> requireSaveManager().load(QUICK_SAVE_SLOT)?.let {
                    runtime.restore(it.state)
                    isBacklogOpen = false
                }
                is GameAction.SetTextSpeed -> textSpeedMillis = action.millisPerCharacter
                is GameAction.CompleteVisualEffect -> runtime.completeVisualEffect(action.sequence)
                // Recognition is complete here; Script/Scene behavior binding is a later phase.
                is GameAction.ActivateHotspot -> Unit
            }
        } finally {
            mutableViewState.value = currentViewState()
        }
    }

    private fun currentViewState(): GameViewState = runtime.state.value.toGameViewState(
        isAutoMode = isAutoMode,
        isSkipMode = isSkipMode,
        isBacklogOpen = isBacklogOpen,
        textSpeedMillis = textSpeedMillis,
    )

    private fun requireSaveManager(): SaveManager = saveManager
        ?: throw EngineException.InvalidAction("Save support is not configured for this game engine.")

    companion object {
        /** Stable storage slot used by quick-save actions. */
        const val QUICK_SAVE_SLOT: String = "quick"
    }
}
