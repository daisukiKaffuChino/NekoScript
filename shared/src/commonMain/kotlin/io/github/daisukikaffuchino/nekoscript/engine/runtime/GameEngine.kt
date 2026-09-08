package io.github.daisukikaffuchino.nekoscript.engine.runtime

import io.github.daisukikaffuchino.nekoscript.engine.error.EngineException
import io.github.daisukikaffuchino.nekoscript.engine.script.ScriptRuntime
import io.github.daisukikaffuchino.nekoscript.engine.save.SaveManager
import io.github.daisukikaffuchino.nekoscript.engine.save.SaveSlotSummary
import io.github.daisukikaffuchino.nekoscript.engine.save.toSlotSummary
import io.github.daisukikaffuchino.nekoscript.engine.project.GameProject
import io.github.daisukikaffuchino.nekoscript.engine.project.HotspotDefinition
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** UI-facing facade that translates [GameAction] values into runtime operations. */
class GameEngine(
    private val runtime: ScriptRuntime,
    private val saveManager: SaveManager? = null,
    private val project: GameProject? = null,
    private val hotspotBindings: Map<String, HotspotDefinition> = emptyMap(),
    private val playtimeSource: PlaytimeSource = PlaytimeSource.monotonic(),
) {
    private val dispatchMutex = Mutex()
    private var isAutoMode = false
    private var isSkipMode = false
    private var isBacklogOpen = false
    private var saveMenuMode: SaveMenuMode? = null
    private var saveMenuPageIndex: Int = 1
    private var saveSlotSummaries: List<SaveSlotSummary> = emptyList()
    private var textSpeedMillis = DEFAULT_TEXT_SPEED_MILLIS
    private var accumulatedPlaytimeMillis: Long = runtime.state.value.playtimeMillis
    private var playtimeMark = playtimeSource.mark()
    private val mutableViewState = MutableStateFlow(currentViewState())

    /** Presentation-ready state observed by a renderer. */
    val viewState: StateFlow<GameViewState> = mutableViewState.asStateFlow()

    /** Handles one player or presentation action. */
    suspend fun dispatch(action: GameAction) = dispatchMutex.withLock {
        try {
            advancePlaytime()
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
                GameAction.OpenSaveMenu -> {
                    saveMenuMode = SaveMenuMode.Save
                    refreshSaveMenuSlots()
                }
                GameAction.OpenLoadMenu -> {
                    saveMenuMode = SaveMenuMode.Load
                    refreshSaveMenuSlots()
                }
                GameAction.CloseSaveMenu -> saveMenuMode = null
                is GameAction.SelectSaveMenuPage -> {
                    saveMenuPageIndex = action.pageIndex.coerceIn(1, SAVE_MENU_PAGE_COUNT)
                    if (saveMenuMode != null) refreshSaveMenuSlots()
                }
                is GameAction.SaveToSlot -> {
                    requireSaveManager().save(action.slotId, captureSaveState(), action.thumbnail)
                    refreshSaveMenuSlots()
                }
                is GameAction.LoadFromSlot -> {
                    requireSaveManager().load(action.slotId)?.let {
                        runtime.restore(it.state)
                        accumulatedPlaytimeMillis = it.state.playtimeMillis
                        isBacklogOpen = false
                        saveMenuMode = null
                        resetPlaytimeMark()
                    }
                }
                is GameAction.DeleteSaveSlot -> {
                    requireSaveManager().delete(action.slotId)
                    refreshSaveMenuSlots()
                }
                is GameAction.QuickSave -> {
                    requireSaveManager().save(QUICK_SAVE_SLOT, captureSaveState(), action.thumbnail)
                    if (saveMenuMode != null) refreshSaveMenuSlots()
                }
                GameAction.QuickLoad -> requireSaveManager().load(QUICK_SAVE_SLOT)?.let {
                    runtime.restore(it.state)
                    accumulatedPlaytimeMillis = it.state.playtimeMillis
                    isBacklogOpen = false
                    if (saveMenuMode != null) refreshSaveMenuSlots()
                    resetPlaytimeMark()
                }
                is GameAction.SetTextSpeed -> textSpeedMillis = action.millisPerCharacter
                is GameAction.CompleteVisualEffect -> runtime.completeVisualEffect(action.sequence)
                is GameAction.ActivateHotspot -> {
                    if (hotspotBindings.isNotEmpty()) {
                        val hotspot = hotspotBindings[action.hotspotId]
                            ?: throw EngineException.InvalidAction("hotspot '${action.hotspotId}' is not bound")
                        runtime.jumpToLabel(hotspot.target)
                    }
                }
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
        project = project,
    ).copy(
        saveMenu = saveMenuMode?.let { mode ->
            SaveMenuView(
                mode = mode,
                currentPage = saveMenuPageIndex,
                pageCount = SAVE_MENU_PAGE_COUNT,
                slots = saveSlotSummaries,
            )
        },
    )

    private fun requireSaveManager(): SaveManager = saveManager
        ?: throw EngineException.InvalidAction("Save support is not configured for this game engine.")

    private suspend fun refreshSaveMenuSlots() {
        val manager = requireSaveManager()
        saveSlotSummaries = saveSlotsForPage(saveMenuPageIndex).map { slot ->
            try {
                manager.load(slot.slotId)?.toSlotSummary(slot.slotId, slot.displayName)
                    ?: SaveSlotSummary.empty(slot.slotId, slot.displayName)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                SaveSlotSummary.broken(
                    slotId = slot.slotId,
                    displayName = slot.displayName,
                    errorMessage = error.message ?: error::class.simpleName.orEmpty(),
                )
            }
        }
    }

    private fun captureSaveState(): io.github.daisukikaffuchino.nekoscript.engine.runtime.GameState =
        runtime.state.value.copy(playtimeMillis = accumulatedPlaytimeMillis)

    private fun advancePlaytime() {
        val elapsedMillis = playtimeMark.elapsedMillis()
        if (elapsedMillis > 0) {
            accumulatedPlaytimeMillis += elapsedMillis
        }
        resetPlaytimeMark()
    }

    private fun resetPlaytimeMark() {
        playtimeMark = playtimeSource.mark()
    }

    private fun saveSlotsForPage(pageIndex: Int): List<SaveSlotDefinition> {
        val page = pageIndex.coerceIn(1, SAVE_MENU_PAGE_COUNT)
        val startInclusive = (page - 1) * SAVE_MENU_PAGE_SIZE + 1
        return (startInclusive until startInclusive + SAVE_MENU_PAGE_SIZE).map { slotNumber ->
            SaveSlotDefinition(
                slotId = saveSlotId(slotNumber),
                displayName = saveSlotDisplayName(slotNumber),
            )
        }
    }

    private fun saveSlotId(slotNumber: Int): String =
        "slot-${slotNumber.toString().padStart(3, '0')}"

    private fun saveSlotDisplayName(slotNumber: Int): String =
        "Slot ${slotNumber.toString().padStart(3, '0')}"

    companion object {
        /** Stable storage slot used by quick-save actions. */
        const val QUICK_SAVE_SLOT: String = "quick"
        private const val SAVE_MENU_PAGE_COUNT: Int = 10
        private const val SAVE_MENU_PAGE_SIZE: Int = 10
    }
}

private data class SaveSlotDefinition(
    val slotId: String,
    val displayName: String,
)
