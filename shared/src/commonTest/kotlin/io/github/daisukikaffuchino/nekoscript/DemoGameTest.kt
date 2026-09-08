package io.github.daisukikaffuchino.nekoscript

import io.github.daisukikaffuchino.nekoscript.engine.character.CharacterPosition
import io.github.daisukikaffuchino.nekoscript.engine.command.Command
import io.github.daisukikaffuchino.nekoscript.engine.effect.TransitionType
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
import io.github.daisukikaffuchino.nekoscript.engine.viewport.LogicalPoint

class DemoGameTest {
    @Test
    fun embeddedProjectRunsTheClassroomRouteThroughReunionToEnding() = runTest {
        val session = createSession()

        session.advanceUntilChoice()
        assertEquals(listOf("帮悠希拿社团资料", "提醒葵别再催了"), session.engine.viewState.value.choices.map { it.text })
        assertEquals(
            listOf(
                "清晨的校门口，阳光刚刚越过教学楼。",
                "你终于来了，我还以为要迟到了。",
                "我就知道你会卡着铃声出现，和小时候一模一样。",
                "站在另一边的是葵，从小和我一起长大的青梅竹马。",
                "怎么，见到我太惊讶，连招呼都忘了？",
                "早上好。葵也来了，那我们一起进去吧？",
                "可以，不过谁最后进教室，谁就负责放学后的饮料。",
                "两个人同时看向我，看来这个早晨得先做个决定。",
            ),
            session.engine.viewState.value.history.map { it.text },
        )
        assertEquals(
            mapOf("yuki" to CharacterPosition.Left, "aoi" to CharacterPosition.Right),
            session.engine.viewState.value.characters.associate { it.characterId to it.position },
        )
        assertNull(session.engine.viewState.value.debug?.voice)
        assertNull(session.engine.viewState.value.debug?.textId)

        session.engine.dispatch(GameAction.SelectChoice(0))
        assertEquals("谢谢，资料有点多。幸好有你帮忙。", session.engine.viewState.value.dialogue?.text)
        assertTrue(session.engine.viewState.value.debug?.textId.orEmpty().startsWith("scripts/main.avg:node_"))
        assertEquals(
            mapOf("yuki" to "happy", "aoi" to "teasing"),
            session.engine.viewState.value.characters.associate { it.characterId to it.expression },
        )

        session.advanceUntilDialogue("上课铃响起时，我们刚好在座位上坐下。")
        assertEquals("classroom", session.engine.viewState.value.background?.assetId)
        assertEquals(setOf("yuki", "aoi"), session.engine.viewState.value.characters.mapTo(mutableSetOf()) { it.characterId })

        session.engine.dispatch(GameAction.Next)
        assertEquals("club_photo", session.engine.viewState.value.cg?.assetId)
        session.completePendingEffect()
        assertEquals("桌面上放着一张昨天拍下的社团合照。", session.engine.viewState.value.dialogue?.text)

        session.engine.dispatch(GameAction.Next)
        assertNull(session.engine.viewState.value.cg)
        session.completePendingEffect()
        assertEquals(2, session.engine.viewState.value.characters.size)
        assertEquals("放学后，也别忘了来活动室。", session.engine.viewState.value.dialogue?.text)

        session.advanceUntilDialogue("别忘了饮料，我可记得很清楚。")
        assertEquals("aoi", session.engine.viewState.value.characters.single().characterId)
        session.engine.dispatch(GameAction.Next)
        assertTrue(session.engine.viewState.value.characters.isEmpty())
        assertEquals("晨光落在空下来的走道上，平常的一天也有了值得期待的结尾。", session.engine.viewState.value.dialogue?.text)
        session.engine.dispatch(GameAction.Next)
        assertEquals("NekoScript Demo  完", session.engine.viewState.value.dialogue?.text)
        session.engine.dispatch(GameAction.Next)
        assertNull(session.engine.viewState.value.dialogue)
    }

    @Test
    fun embeddedProjectRunsTheAoiRouteAndRejoinsTheCommonStory() = runTest {
        val session = createSession()

        session.advanceUntilChoice()
        session.engine.dispatch(GameAction.SelectChoice(1))
        assertEquals("我是在救我们三个人不被老师记迟到，这叫经验。", session.engine.viewState.value.dialogue?.text)
        assertEquals(
            mapOf("yuki" to "surprised", "aoi" to "happy"),
            session.engine.viewState.value.characters.associate { it.characterId to it.expression },
        )

        session.engine.dispatch(GameAction.Next)
        assertTrue(session.engine.viewState.value.visualEffect is VisualEffectView.Shake)
        session.completePendingEffect()
        assertEquals("明明是你刚才差点撞上校门，还说得这么理直气壮。", session.engine.viewState.value.dialogue?.text)

        session.advanceUntilDialogue("上课铃响起时，我们刚好在座位上坐下。")
        assertEquals("classroom", session.engine.viewState.value.background?.assetId)
    }

    @Test
    fun embeddedManifestUsesTheSessionEntryScriptAndDeclaredAssets() = runTest {
        val manifest = demoProjectSource.readText("game.json")
        val project = GameProjectParser().parse(manifest)
        val session = createSession()

        assertEquals("scripts/main.avg", project.entryScript)
        assertEquals(1920, project.viewport.width)
        assertEquals(1080, project.viewport.height)
        assertEquals("backgrounds/school_day.jpg", session.assetManager.loadBackground("school_day").location)
        assertEquals("backgrounds/classroom.jpg", session.assetManager.loadBackground("classroom").location)
        assertEquals("characters/yuki/normal.png", session.assetManager.loadCharacter("yuki", null).location)
        assertEquals("characters/aoi/normal.png", session.assetManager.loadCharacter("aoi", null).location)
        assertEquals("characters/aoi/teasing.png", session.assetManager.loadCharacter("aoi", "teasing").location)
        assertEquals("cg/club_photo.jpg", session.assetManager.loadCg("club_photo").location)
        assertEquals("audio/bgm/morning_theme.mp3", project.audio.bgm["morning_theme"])
        assertEquals("audio/se/school_bell.mp3", project.audio.se["school_bell"])
        assertEquals("audio/se/class_bell.mp3", project.audio.se["class_bell"])
        assertEquals("audio/voice/yuki_1.mp3", project.audio.voice["yuki_1"])
        assertEquals(13, project.audio.voice.size)
        assertTrue(project.debuggable)
        assertTrue(project.audio.voice.all { (id, location) ->
            id.matches(Regex("(yuki|aoi)_[0-9]+")) && location == "audio/voice/$id.mp3"
        })
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
        assertEquals(
            setOf("school_bell", "class_bell"),
            commands.filterIsInstance<Command.PlaySe>().mapTo(mutableSetOf()) { it.audioId },
        )
        assertEquals(
            TransitionType.entries.toSet(),
            commands.filterIsInstance<Command.Transition>().mapTo(mutableSetOf()) { it.type },
        )
    }

    @Test
    fun onlyCharacterDialogueHasADeclaredVoiceCueImmediatelyBeforeIt() = runTest {
        val project = GameProjectParser().parse(demoProjectSource.readText("game.json"))
        val script = AvgScriptParser().parse(
            "scripts/main.avg",
            demoProjectSource.readText("scripts/main.avg"),
        )
        val commands = script.nodes.filterIsInstance<ScriptNode.CommandNode>().map { it.command }

        commands.forEachIndexed { index, command ->
            if (command is Command.Say) {
                val voice = commands.getOrNull(index - 1) as? Command.PlayVoice
                if (command.speaker == null) {
                    assertNull(voice, "Narration '${command.text}' must not have a voice cue")
                } else {
                    assertTrue(voice != null, "Character dialogue '${command.text}' must have a voice cue")
                    assertTrue(voice.audioId in project.audio.voice, "Voice '${voice.audioId}' must be declared")
                }
            }
        }
        assertEquals(
            commands.filterIsInstance<Command.Say>().count { it.speaker != null },
            commands.count { it is Command.PlayVoice },
        )
        assertTrue(project.audio.voice.keys.none { it.startsWith("narrator_") })
    }

    @Test
    fun demoContainsNarrationAndAOneCharacterScene() = runTest {
        val session = createSession()

        session.advanceUntilDialogue("站在另一边的是葵，从小和我一起长大的青梅竹马。")
        assertNull(session.engine.viewState.value.dialogue?.speaker)
        assertEquals(setOf("yuki", "aoi"), session.engine.viewState.value.characters.mapTo(mutableSetOf()) { it.characterId })

        session.advanceUntilChoice()
        session.engine.dispatch(GameAction.SelectChoice(0))
        session.advanceUntilDialogue("别忘了饮料，我可记得很清楚。")
        assertEquals("葵", session.engine.viewState.value.dialogue?.speaker)
        assertEquals("aoi", session.engine.viewState.value.characters.single().characterId)

        session.engine.dispatch(GameAction.Next)
        assertTrue(session.engine.viewState.value.characters.isEmpty())
        assertNull(session.engine.viewState.value.dialogue?.speaker)
    }

    @Test
    fun packagedDemoHotspotUsesLogicalBoundsAndJumpsToItsScene() = runTest {
        val session = createSession()
        val hotspot = session.hotspotRegistry.hitTest(LogicalPoint(1500.0, 500.0))

        assertEquals("club_notice", hotspot?.id)
        session.engine.dispatch(GameAction.ActivateHotspot(hotspot!!.id))

        assertEquals("走廊公告栏上贴着新的社团通知。", session.engine.viewState.value.dialogue?.text)
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
        const val MAX_ADVANCE_STEPS = 60
    }
}
