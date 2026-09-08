package io.github.daisukikaffuchino.nekoscript.engine.runtime

import io.github.daisukikaffuchino.nekoscript.engine.script.AvgScriptParser
import io.github.daisukikaffuchino.nekoscript.engine.script.DefaultScriptRuntime
import io.github.daisukikaffuchino.nekoscript.engine.save.InMemorySaveStorage
import io.github.daisukikaffuchino.nekoscript.engine.save.JsonSaveManager
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
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
    fun manualSaveSlotsSupportSaveLoadAndDelete() = runTest {
        val script = AvgScriptParser().parse(
            "main.avg",
            "say \"Yuki\" \"First\"\nsay \"Yuki\" \"Second\"",
        )
        val engine = GameEngine(
            runtime = DefaultScriptRuntime(script),
            saveManager = JsonSaveManager(InMemorySaveStorage(), timestampProvider = { 99L }),
        )

        engine.dispatch(GameAction.Next)
        engine.dispatch(GameAction.OpenSaveMenu)

        val openSaveMenu = engine.viewState.value.saveMenu
        assertEquals(SaveMenuMode.Save, openSaveMenu?.mode)
        assertEquals(12, openSaveMenu?.slots?.size)
        assertTrue(openSaveMenu?.slots?.all { it.isEmpty } == true)

        engine.dispatch(GameAction.SaveToSlot("slot-01"))
        val savedMenu = requireNotNull(engine.viewState.value.saveMenu)
        val slot01 = savedMenu.slots.single { it.slotId == "slot-01" }
        assertFalse(slot01.isEmpty)
        assertEquals("Yuki", slot01.speaker)
        assertEquals("First", slot01.previewText)

        engine.dispatch(GameAction.OpenLoadMenu)
        val loadMenu = requireNotNull(engine.viewState.value.saveMenu)
        assertEquals(SaveMenuMode.Load, loadMenu.mode)
        assertFalse(loadMenu.slots.single { it.slotId == "slot-01" }.isEmpty)

        engine.dispatch(GameAction.Next)
        assertEquals("Second", engine.viewState.value.dialogue?.text)
        engine.dispatch(GameAction.LoadFromSlot("slot-01"))
        assertEquals("First", engine.viewState.value.dialogue?.text)
        assertTrue(engine.viewState.value.saveMenu == null)

        engine.dispatch(GameAction.DeleteSaveSlot("slot-01"))
        engine.dispatch(GameAction.OpenLoadMenu)
        assertTrue(requireNotNull(engine.viewState.value.saveMenu).slots.single { it.slotId == "slot-01" }.isEmpty)
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

    @Test
    fun boundHotspotJumpsToItsProjectTarget() = runTest {
        val script = AvgScriptParser().parse(
            "main.avg",
            "say \"Before\"\nlabel notice\nsay \"Notice\"",
        )
        val engine = GameEngine(
            runtime = DefaultScriptRuntime(script),
            hotspotBindings = mapOf(
                "notice" to io.github.daisukikaffuchino.nekoscript.engine.project.HotspotDefinition(
                    x = 0.0,
                    y = 0.0,
                    width = 100.0,
                    height = 100.0,
                    target = "notice",
                ),
            ),
        )

        engine.dispatch(GameAction.ActivateHotspot("notice"))

        assertEquals("Notice", engine.viewState.value.dialogue?.text)
    }
}
