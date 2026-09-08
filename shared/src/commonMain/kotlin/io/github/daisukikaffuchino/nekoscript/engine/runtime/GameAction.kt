package io.github.daisukikaffuchino.nekoscript.engine.runtime

/** An input intent sent from a presentation layer to the game engine. */
sealed interface GameAction {
    /** Requests that the engine advance from the current pause point. */
    data object Next : GameAction

    /** Selects the choice at the zero-based [index]. */
    data class SelectChoice(val index: Int) : GameAction {
        init {
            require(index >= 0) { "Choice index must not be negative." }
        }
    }

    /** Requests skip mode. The engine remains responsible for deciding what can be skipped. */
    data object Skip : GameAction

    /** Toggles automatic dialogue progression. */
    data object ToggleAuto : GameAction

    /** Requests that the presentation layer show dialogue history. */
    data object OpenBacklog : GameAction

    /** Requests that the presentation layer close dialogue history. */
    data object CloseBacklog : GameAction

    /** Requests a save in the designated quick-save slot. */
    data object QuickSave : GameAction

    /** Requests loading from the designated quick-save slot. */
    data object QuickLoad : GameAction

    /** Opens the galgame-style manual save menu. */
    data object OpenSaveMenu : GameAction

    /** Opens the galgame-style manual load menu. */
    data object OpenLoadMenu : GameAction

    /** Closes the save/load menu. */
    data object CloseSaveMenu : GameAction

    /** Saves the current runtime state to [slotId]. */
    data class SaveToSlot(val slotId: String) : GameAction {
        init {
            require(slotId.isNotBlank()) { "Save slot id must not be blank." }
        }
    }

    /** Loads the runtime state from [slotId]. */
    data class LoadFromSlot(val slotId: String) : GameAction {
        init {
            require(slotId.isNotBlank()) { "Save slot id must not be blank." }
        }
    }

    /** Deletes the save stored in [slotId]. */
    data class DeleteSaveSlot(val slotId: String) : GameAction {
        init {
            require(slotId.isNotBlank()) { "Save slot id must not be blank." }
        }
    }

    /** Selects the one-based save-menu page index. */
    data class SelectSaveMenuPage(val pageIndex: Int) : GameAction {
        init {
            require(pageIndex > 0) { "Save menu page index must be positive." }
        }
    }

    /** Updates the presentation delay between revealed characters. */
    data class SetTextSpeed(val millisPerCharacter: Int) : GameAction {
        init {
            require(millisPerCharacter in 0..MAX_TEXT_DELAY_MILLIS) {
                "Text speed must be between 0 and $MAX_TEXT_DELAY_MILLIS milliseconds."
            }
        }
    }

    /** Confirms that the renderer completed the visual effect identified by [sequence]. */
    data class CompleteVisualEffect(val sequence: Long) : GameAction {
        init {
            require(sequence > 0) { "Visual effect sequence must be positive." }
        }
    }

    /**
     * Reports activation of a logical scene hotspot.
     *
     * Binding the hotspot to script behavior is intentionally outside the first hotspot phase.
     */
    data class ActivateHotspot(val hotspotId: String) : GameAction {
        init {
            require(hotspotId.isNotBlank()) { "Hotspot id must not be blank." }
        }
    }

    companion object {
        /** Upper bound accepted by [SetTextSpeed]. */
        const val MAX_TEXT_DELAY_MILLIS: Int = 250
    }
}
