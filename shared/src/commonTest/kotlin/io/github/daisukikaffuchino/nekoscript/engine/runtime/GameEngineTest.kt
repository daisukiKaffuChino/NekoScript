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
    fun manualSaveSlotsSupportPagingThumbnailAndPlaytime() = runTest {
        val script = AvgScriptParser().parse(
            "main.avg",
            "background \"school\"\ncharacter \"yuki\" \"happy\"\nsay \"Yuki\" \"First\"\nsay \"Yuki\" \"Second\"",
        )
        val clock = AdjustablePlaytimeSource()
        val engine = GameEngine(
            runtime = DefaultScriptRuntime(script),
            saveManager = JsonSaveManager(InMemorySaveStorage(), timestampProvider = { 99L }),
            playtimeSource = clock,
        )

        engine.dispatch(GameAction.Next)
        clock.advanceBy(65_000L)
        engine.dispatch(GameAction.OpenSaveMenu)

        val openSaveMenu = engine.viewState.value.saveMenu
        assertEquals(SaveMenuMode.Save, openSaveMenu?.mode)
        assertEquals(1, openSaveMenu?.currentPage)
        assertEquals(10, openSaveMenu?.pageCount)
        assertEquals(10, openSaveMenu?.slots?.size)
        assertEquals("slot-001", openSaveMenu?.slots?.first()?.slotId)
        assertEquals("slot-010", openSaveMenu?.slots?.last()?.slotId)

        engine.dispatch(GameAction.SaveToSlot("slot-001"))
        val savedMenu = requireNotNull(engine.viewState.value.saveMenu)
        val slot01 = savedMenu.slots.single { it.slotId == "slot-001" }
        assertFalse(slot01.isEmpty)
        assertEquals("Yuki", slot01.speaker)
        assertEquals("First", slot01.previewText)
        assertEquals(65_000L, slot01.playtimeMillis)
        assertEquals("school", slot01.thumbnail?.backgroundId)
        assertEquals(1, slot01.thumbnail?.characters?.size ?: 0)

        engine.dispatch(GameAction.SelectSaveMenuPage(2))
        val pageTwo = requireNotNull(engine.viewState.value.saveMenu)
        assertEquals(2, pageTwo.currentPage)
        assertEquals("slot-011", pageTwo.slots.first().slotId)

        engine.dispatch(GameAction.SaveToSlot("slot-011"))
        val slot011 = requireNotNull(engine.viewState.value.saveMenu).slots.first()
        assertEquals(65_000L, slot011.playtimeMillis)
        assertEquals("school", slot011.thumbnail?.backgroundId)

        engine.dispatch(GameAction.OpenLoadMenu)
        val loadMenu = requireNotNull(engine.viewState.value.saveMenu)
        assertEquals(SaveMenuMode.Load, loadMenu.mode)
        assertEquals("slot-011", loadMenu.slots.first().slotId)
        assertFalse(loadMenu.slots.first().isEmpty)
        assertEquals("school", loadMenu.slots.first().thumbnail?.backgroundId)

        engine.dispatch(GameAction.DeleteSaveSlot("slot-011"))
        engine.dispatch(GameAction.OpenLoadMenu)
        assertTrue(requireNotNull(engine.viewState.value.saveMenu).slots.first().isEmpty)
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

    private class AdjustablePlaytimeSource : PlaytimeSource {
        private var nowMillis: Long = 0L

        fun advanceBy(millis: Long) {
            nowMillis += millis
        }

        override fun mark(): PlaytimeMark {
            val startMillis = nowMillis
            return PlaytimeMark { nowMillis - startMillis }
        }
    }
}
