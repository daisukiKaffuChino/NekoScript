package io.github.daisukikaffuchino.nekoscript.engine.runtime

import io.github.daisukikaffuchino.nekoscript.engine.character.CharacterPosition
import io.github.daisukikaffuchino.nekoscript.engine.audio.AudioState
import io.github.daisukikaffuchino.nekoscript.engine.background.BackgroundState
import io.github.daisukikaffuchino.nekoscript.engine.character.CharacterState
import io.github.daisukikaffuchino.nekoscript.engine.dialogue.Dialogue
import io.github.daisukikaffuchino.nekoscript.engine.project.CharacterDefinition
import io.github.daisukikaffuchino.nekoscript.engine.project.AudioManifest
import io.github.daisukikaffuchino.nekoscript.engine.project.GameProject
import io.github.daisukikaffuchino.nekoscript.engine.script.ScriptPosition
import io.github.daisukikaffuchino.nekoscript.engine.project.ViewportConfig
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
        assertNull(state.debug)
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

    @Test
    fun projectsTheConfiguredLogicalViewport() {
        val project = GameProject(
            id = "demo",
            name = "Demo",
            version = "1",
            entryScript = "main.avg",
            viewport = ViewportConfig(width = 1024, height = 768),
        )

        val viewState = GameState("main.avg").toGameViewState(project = project)

        assertEquals(1024, viewState.viewport.logicalWidth)
        assertEquals(768, viewState.viewport.logicalHeight)
        assertEquals(4.0 / 3.0, viewState.viewport.aspectRatio)
    }

    @Test
    fun projectsCurrentLogicalIdsAndManifestLocationsOnlyWhenDebuggable() {
        val state = GameState(
            scriptId = "scripts/main.avg",
            position = ScriptPosition(12),
            background = BackgroundState("school"),
            characters = listOf(CharacterState("yuki", null, CharacterPosition.Left)),
            dialogue = Dialogue(null, "Morning"),
            audio = AudioState(bgmId = "theme", voiceId = "yuki_1"),
            cgId = "photo",
        )
        val project = GameProject(
            id = "demo",
            name = "Demo",
            version = "1",
            debuggable = true,
            entryScript = "scripts/main.avg",
            viewport = io.github.daisukikaffuchino.nekoscript.engine.project.ViewportConfig(),
            backgrounds = mapOf("school" to "backgrounds/school.jpg"),
            characters = mapOf(
                "yuki" to CharacterDefinition(
                    "Yuki",
                    "normal",
                    mapOf("normal" to "characters/yuki/normal.png"),
                ),
            ),
            cg = mapOf("photo" to "cg/photo.jpg"),
            audio = AudioManifest(
                bgm = mapOf("theme" to "audio/theme.mp3"),
                voice = mapOf("yuki_1" to "audio/voice/yuki_1.mp3"),
            ),
        )

        val debug = state.toGameViewState(project = project).debug

        assertEquals("scripts/main.avg:node_11", debug?.textId)
        assertEquals(DebugAssetView("school", "backgrounds/school.jpg"), debug?.background)
        assertEquals(DebugAssetView("yuki:normal", "characters/yuki/normal.png"), debug?.characters?.single())
        assertEquals(DebugAssetView("photo", "cg/photo.jpg"), debug?.cg)
        assertEquals(DebugAssetView("theme", "audio/theme.mp3"), debug?.bgm)
        assertEquals(DebugAssetView("yuki_1", "audio/voice/yuki_1.mp3"), debug?.voice)
        assertNull(state.toGameViewState(project = project.copy(debuggable = false)).debug)
    }
}
