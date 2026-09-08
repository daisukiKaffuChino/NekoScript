package io.github.daisukikaffuchino.nekoscript.engine.runtime

import io.github.daisukikaffuchino.nekoscript.engine.save.SaveSlotSummary

/** Presentation-only mode for the save/load menu. */
sealed interface SaveMenuMode {
    data object Save : SaveMenuMode
    data object Load : SaveMenuMode
}

/** Read-only snapshot of the current galgame-style save menu. */
data class SaveMenuView(
    val mode: SaveMenuMode,
    val slots: List<SaveSlotSummary>,
)
