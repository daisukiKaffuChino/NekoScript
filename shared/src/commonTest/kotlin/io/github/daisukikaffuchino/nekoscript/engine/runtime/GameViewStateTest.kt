package io.github.daisukikaffuchino.nekoscript.engine.runtime

import io.github.daisukikaffuchino.nekoscript.engine.character.CharacterPosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameViewStateTest {
    @Test
    fun defaultsRepresentAnIdleEmptyRenderer() {
        val state = GameViewState()

        assertNull(state.background)
        assertNull(state.dialogue)
        assertEquals(emptyList(), state.characters)
        assertEquals(emptyList(), state.choices)
        assertFalse(state.isWaiting)
        assertFalse(state.isAutoMode)
        assertFalse(state.isSkipMode)
    }

    @Test
    fun carriesOnlyPresentationReadyValues() {
        val state = GameViewState(
            background = BackgroundView("school_day"),
            characters = listOf(CharacterView("yuki", "happy", CharacterPosition.Right)),
            dialogue = DialogueView("悠希", "早上好。"),
            choices = listOf(ChoiceView(0, "早上好")),
            isWaiting = true,
        )

        assertEquals("school_day", state.background?.assetId)
        assertEquals("yuki", state.characters.single().characterId)
        assertEquals("早上好。", state.dialogue?.text)
        assertEquals(0, state.choices.single().index)
        assertTrue(state.isWaiting)
    }
}
