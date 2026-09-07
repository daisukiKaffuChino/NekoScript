package io.github.daisukikaffuchino.nekoscript

import io.github.daisukikaffuchino.nekoscript.engine.character.CharacterPosition
import io.github.daisukikaffuchino.nekoscript.engine.command.Command
import io.github.daisukikaffuchino.nekoscript.engine.project.GameProjectParser
import io.github.daisukikaffuchino.nekoscript.engine.runtime.GameAction
import io.github.daisukikaffuchino.nekoscript.engine.runtime.GameSession
import io.github.daisukikaffuchino.nekoscript.engine.runtime.GameSessionFactory
import io.github.daisukikaffuchino.nekoscript.engine.runtime.SaveManagerFactory
import io.github.daisukikaffuchino.nekoscript.engine.runtime.VisualEffectView
import io.github.daisukikaffuchino.nekoscript.engine.save.InMemorySaveStorage
import io.github.daisukikaffuchino.nekoscript.engine.save.JsonSaveManager
import io.github.daisukikaffuchino.nekoscript.engine.script.AvgScriptParser
import io.github.daisukikaffuchino.nekoscript.engine.script.ScriptNode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DemoGameTest {
    @Test
    fun embeddedProjectRunsTheClassroomRouteThroughReunionToEnding() = runTest {
        val session = createSession()

        session.advanceUntilChoice()
        assertEquals(listOf("直接去教室", "先看看公告栏"), session.engine.viewState.value.choices.map { it.text })
        assertEquals(
            listOf(
                "清晨的校门口，阳光刚刚越过教学楼。",
                "你终于来了，我还以为要迟到了。",
                "早上好。今天也一起进去吧？",
                "她把书包带往肩上提了提，等着我的回答。",
            ),
            session.engine.viewState.value.history.map { it.text },
        )
        assertEquals(CharacterPosition.Center, session.engine.viewState.value.characters.single().position)

        session.engine.dispatch(GameAction.SelectChoice(0))
        assertEquals("好呀，我正好有件东西想给你看。", session.engine.viewState.value.dialogue?.text)
        assertEquals("happy", session.engine.viewState.value.characters.single().expression)

        session.advanceUntilDialogue("上课铃响起时，我们刚好在座位上坐下。")
        assertEquals("classroom", session.engine.viewState.value.background?.assetId)

        session.engine.dispatch(GameAction.Next)
        assertEquals("club_photo", session.engine.viewState.value.cg?.assetId)
        session.completePendingEffect()
        assertEquals("桌面上放着一张昨天拍下的社团合照。", session.engine.viewState.value.dialogue?.text)

        session.engine.dispatch(GameAction.Next)
        assertNull(session.engine.viewState.value.cg)
        assertEquals(CharacterPosition.Right, session.engine.viewState.value.characters.single().position)
        assertEquals("放学后，也别忘了来活动室。", session.engine.viewState.value.dialogue?.text)

        session.engine.dispatch(GameAction.Next)
        assertTrue(session.engine.viewState.value.characters.isEmpty())
        assertEquals("她回到座位，短暂的晨间约定就这样定下了。", session.engine.viewState.value.dialogue?.text)
        session.engine.dispatch(GameAction.Next)
        assertEquals("NekoScript Demo  完", session.engine.viewState.value.dialogue?.text)
        session.engine.dispatch(GameAction.Next)
        assertNull(session.engine.viewState.value.dialogue)
    }

    @Test
    fun embeddedProjectRunsTheNoticeRouteAndRejoinsTheCommonStory() = runTest {
        val session = createSession()

        session.advanceUntilChoice()
        session.engine.dispatch(GameAction.SelectChoice(1))
        assertEquals("公告栏？难道社团名单已经贴出来了？", session.engine.viewState.value.dialogue?.text)
        assertEquals("surprised", session.engine.viewState.value.characters.single().expression)

        session.engine.dispatch(GameAction.Next)
        assertTrue(session.engine.viewState.value.visualEffect is VisualEffectView.Shake)
        session.completePendingEffect()
        assertEquals("原来只是风吹动了纸张……吓我一跳。", session.engine.viewState.value.dialogue?.text)

        session.advanceUntilDialogue("上课铃响起时，我们刚好在座位上坐下。")
        assertEquals("classroom", session.engine.viewState.value.background?.assetId)
    }

    @Test
    fun embeddedManifestUsesTheSessionEntryScriptAndDeclaredAssets() = runTest {
        val manifest = demoProjectSource.readText("game.json")
        val project = GameProjectParser().parse(manifest)
        val session = createSession()

        assertEquals("scripts/main.avg", project.entryScript)
        assertEquals("backgrounds/school_day.jpg", session.assetManager.loadBackground("school_day").location)
        assertEquals("backgrounds/classroom.jpg", session.assetManager.loadBackground("classroom").location)
        assertEquals("characters/yuki/normal.png", session.assetManager.loadCharacter("yuki", null).location)
        assertEquals("cg/club_photo.jpg", session.assetManager.loadCg("club_photo").location)
        assertEquals("audio/bgm/morning_theme.ogg", project.audio.bgm["morning_theme"])
        assertEquals("audio/se/school_bell.ogg", project.audio.se["school_bell"])
        assertEquals("audio/voice/yuki_good_morning.ogg", project.audio.voice["yuki_good_morning"])
    }

    @Test
    fun embeddedScriptParsesAndCoversEveryCurrentCommandType() = runTest {
        val source = demoProjectSource.readText("scripts/main.avg")
        val script = AvgScriptParser().parse("scripts/main.avg", source)
        val commands = script.nodes
            .filterIsInstance<ScriptNode.CommandNode>()
            .flatMap { flattenCommands(it.command) }

        assertEquals(
            setOf(
                "say",
                "background",
                "character",
                "hide",
                "jump",
                "set",
                "choice",
                "if",
                "play_bgm",
                "stop_bgm",
                "play_se",
                "play_voice",
                "stop_voice",
                "show_cg",
                "hide_cg",
                "transition",
                "shake",
                "move_character",
                "wait",
            ),
            commands.mapTo(mutableSetOf(), ::commandName),
        )
    }

    private suspend fun GameSession.advanceUntilChoice() {
        repeat(MAX_ADVANCE_STEPS) {
            if (engine.viewState.value.choices.isNotEmpty()) return
            advanceOnePausePoint()
        }
        error("Demo did not reach its choice within $MAX_ADVANCE_STEPS steps.")
    }

    private suspend fun GameSession.advanceUntilDialogue(text: String) {
        repeat(MAX_ADVANCE_STEPS) {
            if (engine.viewState.value.dialogue?.text == text) return
            advanceOnePausePoint()
        }
        error("Demo did not reach dialogue '$text' within $MAX_ADVANCE_STEPS steps.")
    }

    private suspend fun GameSession.advanceOnePausePoint() {
        val effect = engine.viewState.value.visualEffect
        if (effect == null) {
            engine.dispatch(GameAction.Next)
        } else {
            engine.dispatch(GameAction.CompleteVisualEffect(effect.sequence))
        }
    }

    private suspend fun GameSession.completePendingEffect() {
        val effect = engine.viewState.value.visualEffect ?: error("Expected a pending visual effect.")
        engine.dispatch(GameAction.CompleteVisualEffect(effect.sequence))
    }

    private fun flattenCommands(command: Command): List<Command> = when (command) {
        is Command.Choice -> listOf(command) + command.choices.flatMap { choice ->
            choice.commands.flatMap(::flattenCommands)
        }
        is Command.If -> listOf(command) +
            (command.thenCommands + command.elseCommands).flatMap(::flattenCommands)
        else -> listOf(command)
    }

    private fun commandName(command: Command): String = when (command) {
        is Command.Say -> "say"
        is Command.ChangeBackground -> "background"
        is Command.ShowCharacter -> "character"
        is Command.HideCharacter -> "hide"
        is Command.Jump -> "jump"
        is Command.SetVariable -> "set"
        is Command.Choice -> "choice"
        is Command.If -> "if"
        is Command.PlayBgm -> "play_bgm"
        is Command.StopBgm -> "stop_bgm"
        is Command.PlaySe -> "play_se"
        is Command.PlayVoice -> "play_voice"
        Command.StopVoice -> "stop_voice"
        is Command.ShowCg -> "show_cg"
        Command.HideCg -> "hide_cg"
        is Command.Transition -> "transition"
        is Command.Shake -> "shake"
        is Command.MoveCharacter -> "move_character"
        is Command.Wait -> "wait"
    }

    private suspend fun createSession(): GameSession = GameSessionFactory(
        source = demoProjectSource,
        saveManagerFactory = SaveManagerFactory { project ->
            JsonSaveManager(InMemorySaveStorage(), timestampProvider = { 0L })
        },
    ).create()

    private companion object {
        const val MAX_ADVANCE_STEPS = 30
    }
}
