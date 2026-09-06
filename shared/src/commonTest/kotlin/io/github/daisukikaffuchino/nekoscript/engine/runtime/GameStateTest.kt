package io.github.daisukikaffuchino.nekoscript.engine.runtime

import io.github.daisukikaffuchino.nekoscript.engine.background.BackgroundState
import io.github.daisukikaffuchino.nekoscript.engine.character.CharacterPosition
import io.github.daisukikaffuchino.nekoscript.engine.character.CharacterState
import io.github.daisukikaffuchino.nekoscript.engine.dialogue.Dialogue
import io.github.daisukikaffuchino.nekoscript.engine.script.ScriptPosition
import io.github.daisukikaffuchino.nekoscript.engine.variable.Variable
import io.github.daisukikaffuchino.nekoscript.engine.variable.VariableStore
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GameStateTest {
    @Test
    fun defaultsRepresentAnEmptySceneAtScriptStart() {
        val state = GameState(scriptId = "main")

        assertEquals(ScriptPosition(0), state.position)
        assertEquals(VariableStore(), state.variables)
        assertEquals(emptyList(), state.characters)
        assertEquals(null, state.background)
        assertEquals(null, state.dialogue)
        assertEquals(emptyList(), state.history)
    }

    @Test
    fun completeStateRoundTripsThroughJson() {
        val dialogue = Dialogue(speaker = "yuki", text = "早上好。")
        val state = GameState(
            scriptId = "main",
            position = ScriptPosition(12),
            variables = VariableStore().put("affection.yuki", Variable.IntValue(3)),
            characters = listOf(
                CharacterState("yuki", "happy", CharacterPosition.Left),
            ),
            background = BackgroundState("school_day"),
            route = "yuki",
            dialogue = dialogue,
            history = listOf(dialogue),
        )

        val encoded = Json.encodeToString(GameState.serializer(), state)
        val decoded = Json.decodeFromString(GameState.serializer(), encoded)

        assertEquals(state, decoded)
    }

    @Test
    fun rejectsBlankScriptIdentifier() {
        assertFailsWith<IllegalArgumentException> { GameState(scriptId = "") }
    }
}
