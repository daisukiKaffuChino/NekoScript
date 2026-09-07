package io.github.daisukikaffuchino.nekoscript.engine.runtime

import io.github.daisukikaffuchino.nekoscript.engine.script.AvgScriptParser
import io.github.daisukikaffuchino.nekoscript.engine.script.DefaultScriptRuntime
import io.github.daisukikaffuchino.nekoscript.engine.save.InMemorySaveStorage
import io.github.daisukikaffuchino.nekoscript.engine.save.JsonSaveManager
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GameEngineTest {
    @Test
    fun dispatchPublishesOnlyPresentationReadyState() = runTest {
        val script = AvgScriptParser().parse(
            "main.avg",
            "background \"school\"\ncharacter \"yuki\" \"happy\"\nsay \"Yuki\" \"Hello\"",
        )
        val engine = GameEngine(DefaultScriptRuntime(script))

        engine.dispatch(GameAction.Next)

        assertEquals("school", engine.viewState.value.background?.assetId)
        assertEquals("yuki", engine.viewState.value.characters.single().characterId)
        assertEquals("Hello", engine.viewState.value.dialogue?.text)
        assertTrue(engine.viewState.value.isWaiting)
    }

    @Test
    fun dispatchesChoiceSelectionAndRejectsUnavailableActions() = runTest {
        val script = AvgScriptParser().parse(
            "main.avg",
            "choice:\n    \"Continue\":\n        jump end\nlabel end\nsay \"Done\"",
        )
        val engine = GameEngine(DefaultScriptRuntime(script))

        engine.dispatch(GameAction.Next)
        assertEquals("Continue", engine.viewState.value.choices.single().text)
        engine.dispatch(GameAction.SelectChoice(0))
        assertEquals("Done", engine.viewState.value.dialogue?.text)

        engine.dispatch(GameAction.ToggleAuto)
        assertTrue(engine.viewState.value.isAutoMode)
    }

    @Test
    fun quickSaveAndQuickLoadRestoreRuntimeSnapshot() = runTest {
        val script = AvgScriptParser().parse(
            "main.avg",
            "background \"school\"\nsay \"First\"\nsay \"Second\"",
        )
        val storage = InMemorySaveStorage()
        val engine = GameEngine(
            runtime = DefaultScriptRuntime(script),
            saveManager = JsonSaveManager(storage, timestampProvider = { 42L }),
        )

        engine.dispatch(GameAction.Next)
        assertEquals("First", engine.viewState.value.dialogue?.text)
        engine.dispatch(GameAction.QuickSave)
        engine.dispatch(GameAction.Next)
        assertEquals("Second", engine.viewState.value.dialogue?.text)

        engine.dispatch(GameAction.QuickLoad)
        assertEquals("First", engine.viewState.value.dialogue?.text)
        assertEquals("school", engine.viewState.value.background?.assetId)
    }

    @Test
    fun presentationActionsDoNotMutateRuntimeProgress() = runTest {
        val script = AvgScriptParser().parse("main.avg", "say \"Yuki\" \"Hello\"")
        val engine = GameEngine(DefaultScriptRuntime(script))
        engine.dispatch(GameAction.Next)

        engine.dispatch(GameAction.OpenBacklog)
        engine.dispatch(GameAction.ToggleAuto)
        engine.dispatch(GameAction.Skip)
        engine.dispatch(GameAction.SetTextSpeed(70))
        engine.dispatch(GameAction.ActivateHotspot("door"))

        val state = engine.viewState.value
        assertTrue(state.isBacklogOpen)
        assertEquals(false, state.isAutoMode)
        assertTrue(state.isSkipMode)
        assertEquals(70, state.textSpeedMillis)
        assertEquals("Hello", state.dialogue?.text)
        assertEquals(listOf("Hello"), state.history.map { it.text })

        engine.dispatch(GameAction.CloseBacklog)
        assertEquals(false, engine.viewState.value.isBacklogOpen)
    }
}
