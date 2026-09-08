package io.github.daisukikaffuchino.nekoscript.engine.save

/**
 * Human-readable save-slot projection shown by the presentation layer.
 *
 * It is derived from [SaveData] and is intentionally separate from the persisted
 * envelope so the runtime can show galgame-style slot previews without changing
 * the core save payload.
 */
data class SaveSlotSummary(
    val slotId: String,
    val displayName: String,
    val playtimeMillis: Long? = null,
    val speaker: String? = null,
    val previewText: String? = null,
    val contextLabel: String? = null,
    val thumbnail: SaveThumbnail? = null,
    val isEmpty: Boolean = false,
    val isBroken: Boolean = false,
    val errorMessage: String? = null,
) {
    init {
        require(slotId.isNotBlank()) { "Save slot id must not be blank." }
        require(displayName.isNotBlank()) { "Save slot display name must not be blank." }
        require(playtimeMillis == null || playtimeMillis >= 0) { "Save slot playtime must not be negative." }
    }

    companion object {
        fun empty(slotId: String, displayName: String): SaveSlotSummary =
            SaveSlotSummary(slotId = slotId, displayName = displayName, isEmpty = true)

        fun broken(slotId: String, displayName: String, errorMessage: String): SaveSlotSummary =
            SaveSlotSummary(
                slotId = slotId,
                displayName = displayName,
                isBroken = true,
                errorMessage = errorMessage,
            )
    }
}

/** Converts a full save envelope into a presentation-friendly slot summary. */
fun SaveData.toSlotSummary(slotId: String, displayName: String): SaveSlotSummary {
    val state = state
    val currentDialogue = state.dialogue
    val previewText = when {
        currentDialogue != null -> currentDialogue.text
        state.pendingChoices.isNotEmpty() -> "Choice pending"
        state.visualEffect != null -> "Effect pending"
        state.history.isNotEmpty() -> state.history.last().text
        else -> "Empty"
    }.take(64)
    val speaker = currentDialogue?.speaker?.takeIf(String::isNotBlank)
        ?: if (currentDialogue == null) state.history.lastOrNull()?.speaker?.takeIf(String::isNotBlank) else null
    val contextLabel = buildString {
        append(state.scriptId)
        append(" · node ")
        append(state.position.nodeIndex)
    }
    return SaveSlotSummary(
        slotId = slotId,
        displayName = displayName,
        playtimeMillis = state.playtimeMillis,
        speaker = speaker,
        previewText = previewText,
        contextLabel = contextLabel,
        thumbnail = this.thumbnail ?: state.toSaveThumbnail(),
    )
}
