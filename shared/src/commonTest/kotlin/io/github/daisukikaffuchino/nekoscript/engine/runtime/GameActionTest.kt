package io.github.daisukikaffuchino.nekoscript.engine.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GameActionTest {
    @Test
    fun choiceCarriesSelectionIndex() {
        assertEquals(2, GameAction.SelectChoice(2).index)
    }

    @Test
    fun choiceRejectsNegativeIndex() {
        assertFailsWith<IllegalArgumentException> { GameAction.SelectChoice(-1) }
    }

    @Test
    fun statelessActionsHaveStableIdentity() {
        assertEquals(GameAction.Next, GameAction.Next)
        assertEquals(GameAction.QuickSave, GameAction.QuickSave)
        assertEquals(GameAction.OpenSaveMenu, GameAction.OpenSaveMenu)
        assertEquals(GameAction.OpenLoadMenu, GameAction.OpenLoadMenu)
        assertEquals(GameAction.CloseSaveMenu, GameAction.CloseSaveMenu)
    }

    @Test
    fun textSpeedValidatesPresentationDelay() {
        assertEquals(35, GameAction.SetTextSpeed(35).millisPerCharacter)
        assertFailsWith<IllegalArgumentException> { GameAction.SetTextSpeed(-1) }
        assertFailsWith<IllegalArgumentException> { GameAction.SetTextSpeed(251) }
    }

    @Test
    fun visualEffectCompletionRequiresPositiveSequence() {
        assertEquals(2L, GameAction.CompleteVisualEffect(2).sequence)
        assertFailsWith<IllegalArgumentException> { GameAction.CompleteVisualEffect(0) }
    }

    @Test
    fun hotspotActivationCarriesOnlyAValidatedLogicalId() {
        assertEquals("door", GameAction.ActivateHotspot("door").hotspotId)
        assertFailsWith<IllegalArgumentException> { GameAction.ActivateHotspot(" ") }
    }

    @Test
    fun saveSlotActionsValidateSlotIds() {
        assertEquals("slot-01", GameAction.SaveToSlot("slot-01").slotId)
        assertEquals("slot-01", GameAction.LoadFromSlot("slot-01").slotId)
        assertEquals("slot-01", GameAction.DeleteSaveSlot("slot-01").slotId)
        assertFailsWith<IllegalArgumentException> { GameAction.SaveToSlot(" ") }
        assertFailsWith<IllegalArgumentException> { GameAction.LoadFromSlot(" ") }
        assertFailsWith<IllegalArgumentException> { GameAction.DeleteSaveSlot(" ") }
    }
}
