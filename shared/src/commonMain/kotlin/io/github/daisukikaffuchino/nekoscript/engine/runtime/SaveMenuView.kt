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
    val currentPage: Int,
    val pageCount: Int,
    val slots: List<SaveSlotSummary>,
) {
    init {
        require(pageCount > 0) { "Save menu must have at least one page." }
        require(currentPage in 1..pageCount) { "Save menu current page must be within the available page range." }
    }
}
