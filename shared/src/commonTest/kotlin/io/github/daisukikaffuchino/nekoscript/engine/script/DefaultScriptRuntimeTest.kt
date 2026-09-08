package io.github.daisukikaffuchino.nekoscript.engine.script

import io.github.daisukikaffuchino.nekoscript.engine.audio.AudioPlayer
import io.github.daisukikaffuchino.nekoscript.engine.character.CharacterPosition
import io.github.daisukikaffuchino.nekoscript.engine.effect.TransitionType
import io.github.daisukikaffuchino.nekoscript.engine.logging.EngineLogger
import io.github.daisukikaffuchino.nekoscript.engine.error.EngineException
import io.github.daisukikaffuchino.nekoscript.engine.runtime.RuntimeStatus
import io.github.daisukikaffuchino.nekoscript.engine.runtime.GameState
import io.github.daisukikaffuchino.nekoscript.engine.variable.Variable
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefaultScriptRuntimeTest {
    private val parser = AvgScriptParser()

    @Test
    fun executesSceneCommandsAndPausesAtEachDialogue() = runTest {
        val runtime = runtime(
            """
                label start
                background "school"
                character "yuki" "happy" left
                say "Yuki" "Hello"
                hide "yuki"
                say "Yuki" "Goodbye"
            """.trimIndent(),
        )

        runtime.next()
        assertEquals("school", runtime.state.value.background?.backgroundId)
        assertEquals("yuki", runtime.state.value.characters.single().characterId)
        assertEquals(CharacterPosition.Left, runtime.state.value.characters.single().position)
        assertEquals("Hello", runtime.state.value.dialogue?.text)
        assertEquals(RuntimeStatus.WaitingForInput, runtime.state.value.status)

        runtime.next()
        assertEquals(emptyList(), runtime.state.value.characters)
        assertEquals("Goodbye", runtime.state.value.dialogue?.text)
        assertEquals(2, runtime.state.value.history.size)

        runtime.next()
        assertNull(runtime.state.value.dialogue)
        assertEquals(RuntimeStatus.Completed, runtime.state.value.status)
    }

    @Test
    fun choiceUpdatesVariableJumpsAndContinuesToDestination() = runTest {
        val runtime = runtime(
            """
                label start
                choice:
                    "Greet":
                        set affection.yuki = 1
                        jump morning
                    "Leave":
                        jump stranger
                label morning
                say "Yuki" "Morning"
                jump end
                label stranger
                say "Yuki" "Who are you?"
                label end
            """.trimIndent(),
        )

        runtime.next()
        assertEquals(RuntimeStatus.WaitingForChoice, runtime.state.value.status)
        assertEquals(listOf("Greet", "Leave"), runtime.state.value.pendingChoices.map { it.text })

        runtime.selectChoice(0)
        assertEquals(Variable.IntValue(1), runtime.state.value.variables["affection.yuki"])
        assertEquals("Morning", runtime.state.value.dialogue?.text)
        assertEquals(RuntimeStatus.WaitingForInput, runtime.state.value.status)
    }

    @Test
    fun jumpToLabelClearsPauseStateAndContinuesFromTheBoundSceneTarget() = runTest {
        val runtime = runtime(
            """
                label start
                say "Before hotspot"
                label notice
                say "Notice opened"
            """.trimIndent(),
        )

        runtime.next()
        assertEquals("Before hotspot", runtime.state.value.dialogue?.text)

        runtime.jumpToLabel("notice")

        assertEquals("Notice opened", runtime.state.value.dialogue?.text)
        assertEquals(RuntimeStatus.WaitingForInput, runtime.state.value.status)
        assertEquals(listOf("Before hotspot", "Notice opened"), runtime.state.value.history.map { it.text })
    }

    @Test
    fun jumpToLabelStopsInterruptedVoice() = runTest {
        val player = RecordingAudioPlayer()
        val runtime = DefaultScriptRuntime(
            parser.parse("main.avg", "play_voice \"line\"\nsay \"Before\"\nlabel notice\nsay \"Notice\""),
            audioPlayer = player,
        )

        runtime.next()
        runtime.jumpToLabel("notice")

        assertEquals(null, runtime.state.value.audio.voiceId)
        assertEquals("stopVoice", player.events.last())
    }

    @Test
    fun rejectsInvalidActionsAndUnknownLabels() = runTest {
        val runtime = runtime("label start\nchoice:\n    \"Only\":\n        jump missing")

        assertFailsWith<EngineException.InvalidAction> { runtime.selectChoice(0) }
        runtime.next()
        assertFailsWith<EngineException.InvalidAction> { runtime.next() }
        assertFailsWith<EngineException.InvalidAction> { runtime.selectChoice(4) }
        assertFailsWith<EngineException.UnknownLabel> { runtime.selectChoice(0) }
    }

    @Test
    fun replacingCharacterDoesNotDuplicateIt() = runTest {
        val runtime = runtime(
            "character \"yuki\" \"normal\" left\ncharacter \"yuki\" \"happy\" right\nsay \"Yuki\" \"Hi\"",
        )

        runtime.next()

        assertEquals(1, runtime.state.value.characters.size)
        assertEquals("happy", runtime.state.value.characters.single().expression)
        assertEquals(CharacterPosition.Right, runtime.state.value.characters.single().position)
    }

    @Test
    fun restoreValidatesScriptIdentityAndPosition() = runTest {
        val runtime = runtime("say \"Hello\"")

        runtime.restore(GameState("main.avg", ScriptPosition(1), status = RuntimeStatus.Completed))
        assertEquals(RuntimeStatus.Completed, runtime.state.value.status)
        assertFailsWith<IllegalArgumentException> { runtime.restore(GameState("other.avg")) }
        assertFailsWith<IllegalArgumentException> {
            runtime.restore(GameState("main.avg", ScriptPosition(2)))
        }
    }

    @Test
    fun audioCommandsCallInjectedPlayerAndUpdatePersistentState() = runTest {
        val player = RecordingAudioPlayer()
        val script = parser.parse(
            "main.avg",
            "play_bgm \"theme\" once\nplay_se \"door\"\nplay_voice \"line\"\nsay \"Hello\"\nstop_voice\nstop_bgm 200",
        )
        val runtime = DefaultScriptRuntime(script, audioPlayer = player)

        runtime.next()
        assertEquals(listOf("bgm:theme:false", "se:door", "voice:line"), player.events)
        assertEquals("theme", runtime.state.value.audio.bgmId)
        assertEquals(false, runtime.state.value.audio.bgmLoop)
        assertEquals("line", runtime.state.value.audio.voiceId)

        runtime.next()
        assertEquals(listOf("stopVoice", "stopBgm:200"), player.events.takeLast(2))
        assertNull(runtime.state.value.audio.bgmId)
        assertNull(runtime.state.value.audio.voiceId)
    }

    @Test
    fun restoreSynchronizesLogicalAudioWithPlayer() = runTest {
        val player = RecordingAudioPlayer()
        val script = parser.parse("main.avg", "say \"Hello\"")
        val runtime = DefaultScriptRuntime(script, audioPlayer = player)
        val restored = GameState(
            scriptId = "main.avg",
            audio = io.github.daisukikaffuchino.nekoscript.engine.audio.AudioState(
                bgmId = "theme",
                bgmLoop = true,
                voiceId = "line",
            ),
        )

        runtime.restore(restored)

        assertEquals(listOf("stopBgm:0", "bgm:theme:true", "stopVoice", "voice:line"), player.events)
    }

    @Test
    fun variableConditionSelectsTheExpectedRoute() = runTest {
        val source = """
            set affection.yuki = 10
            if affection.yuki >= 10:
                jump good_end
            else:
                jump normal_end
            label good_end
            say "Good ending"
            jump end
            label normal_end
            say "Normal ending"
            label end
        """.trimIndent()
        val runtime = runtime(source)

        runtime.next()

        assertEquals("Good ending", runtime.state.value.dialogue?.text)
        assertEquals(Variable.IntValue(10), runtime.state.value.variables["affection.yuki"])
    }

    @Test
    fun visualCommandsUpdateSceneAndEmitSequencedEffects() = runTest {
        val script = parser.parse(
            "main.avg",
            "character \"yuki\" \"normal\" left\nshow_cg \"cg_01\" fade 500\nmove_character \"yuki\" right 450\ntransition flash 100\nsay \"Done\"",
        )
        val runtime = runtime(script)

        runtime.next()
        assertEquals("cg_01", runtime.state.value.cgId)
        assertEquals(RuntimeStatus.WaitingForEffect, runtime.state.value.status)
        runtime.completeVisualEffect(1)

        assertEquals(CharacterPosition.Right, runtime.state.value.characters.single().position)
        assertEquals(RuntimeStatus.WaitingForEffect, runtime.state.value.status)
        runtime.completeVisualEffect(2)

        assertEquals(RuntimeStatus.WaitingForEffect, runtime.state.value.status)
        assertEquals(3L, runtime.state.value.visualEffect?.sequence)
        assertEquals(TransitionType.Flash, (runtime.state.value.visualEffect?.effect as io.github.daisukikaffuchino.nekoscript.engine.effect.VisualEffect.Transition).type)
        runtime.completeVisualEffect(3)
        assertEquals("Done", runtime.state.value.dialogue?.text)

        runtime.restore(runtime.state.value.copy(cgId = null))
        assertEquals(null, runtime.state.value.cgId)
    }

    @Test
    fun movingHiddenCharacterIsRejected() = runTest {
        val runtime = runtime("move_character \"yuki\" left")

        assertFailsWith<EngineException.InvalidAction> { runtime.next() }
    }

    @Test
    fun visualEffectCompletionRejectsStaleSequence() = runTest {
        val runtime = runtime("transition fade 100\nsay \"Done\"")
        runtime.next()

        assertFailsWith<EngineException.InvalidAction> { runtime.completeVisualEffect(2) }
        runtime.completeVisualEffect(1)
        assertEquals("Done", runtime.state.value.dialogue?.text)
        assertFailsWith<EngineException.InvalidAction> { runtime.completeVisualEffect(1) }
    }

    @Test
    fun waitUsesInjectedSuspensionAndLogger() = runTest {
        val waits = mutableListOf<Long>()
        val messages = mutableListOf<String>()
        val runtime = DefaultScriptRuntime(
            script = parser.parse("main.avg", "wait 250\nsay \"Done\""),
            logger = RecordingLogger(messages),
            wait = { waits += it },
        )

        runtime.next()

        assertEquals(listOf(250L), waits)
        assertEquals("Done", runtime.state.value.dialogue?.text)
        assertTrue(messages.single().contains("command=Wait"))
    }

    private fun runtime(source: String) = DefaultScriptRuntime(parser.parse("main.avg", source))
    private fun runtime(script: Script) = DefaultScriptRuntime(script)

    private class RecordingAudioPlayer : AudioPlayer {
        val events = mutableListOf<String>()

        override suspend fun playBgm(id: String, loop: Boolean) {
            events += "bgm:$id:$loop"
        }

        override suspend fun stopBgm(fadeOutMillis: Long) {
            events += "stopBgm:$fadeOutMillis"
        }

        override suspend fun playSe(id: String) {
            events += "se:$id"
        }

        override suspend fun playVoice(id: String) {
            events += "voice:$id"
        }

        override suspend fun stopVoice() {
            events += "stopVoice"
        }
    }

    private class RecordingLogger(private val messages: MutableList<String>) : EngineLogger {
        override fun debug(message: String) {
            messages += message
        }

        override fun info(message: String) = Unit
        override fun warn(message: String) = Unit
        override fun error(message: String, throwable: Throwable?) = Unit
    }
}
