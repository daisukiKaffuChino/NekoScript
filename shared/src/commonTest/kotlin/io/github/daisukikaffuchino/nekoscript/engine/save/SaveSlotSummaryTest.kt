package io.github.daisukikaffuchino.nekoscript.engine.save

import io.github.daisukikaffuchino.nekoscript.engine.dialogue.Dialogue
import io.github.daisukikaffuchino.nekoscript.engine.runtime.GameState
import io.github.daisukikaffuchino.nekoscript.engine.script.ScriptPosition
import io.github.daisukikaffuchino.nekoscript.engine.character.CharacterState
import io.github.daisukikaffuchino.nekoscript.engine.background.BackgroundState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SaveSlotSummaryTest {
    @Test
    fun derivesHumanReadablePreviewFromSaveState() {
        val save = SaveData(
            version = 1,
            timestamp = 12_345L,
            state = GameState(
                scriptId = "main.avg",
                position = ScriptPosition(8),
                playtimeMillis = 65_000L,
                background = BackgroundState("school"),
                characters = listOf(CharacterState("yuki", "happy")),
                dialogue = Dialogue("Yuki", "Good morning"),
            ),
        )

        val summary = save.toSlotSummary("slot-01", "Slot 01")

        assertEquals("slot-01", summary.slotId)
        assertEquals("Slot 01", summary.displayName)
        assertEquals(65_000L, summary.playtimeMillis)
        assertEquals("Yuki", summary.speaker)
        assertEquals("Good morning", summary.previewText)
        assertEquals("main.avg · node 8", summary.contextLabel)
        assertEquals("school", summary.thumbnail?.backgroundId)
        assertEquals(1, summary.thumbnail?.characters?.size)
        assertFalse(summary.isEmpty)
        assertFalse(summary.isBroken)
    }

    @Test
    fun marksEmptyAndBrokenSlotsExplicitly() {
        assertTrue(SaveSlotSummary.empty("slot-02", "Slot 02").isEmpty)
        assertTrue(SaveSlotSummary.broken("slot-03", "Slot 03", "broken").isBroken)
    }
}
