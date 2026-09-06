package io.github.daisukikaffuchino.nekoscript.engine.script

import io.github.daisukikaffuchino.nekoscript.engine.character.CharacterPosition
import io.github.daisukikaffuchino.nekoscript.engine.command.Command
import io.github.daisukikaffuchino.nekoscript.engine.error.EngineException
import io.github.daisukikaffuchino.nekoscript.engine.variable.Variable
import io.github.daisukikaffuchino.nekoscript.engine.variable.ComparisonOperator
import io.github.daisukikaffuchino.nekoscript.engine.variable.Condition
import io.github.daisukikaffuchino.nekoscript.engine.effect.TransitionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AvgScriptParserTest {
    private val parser = AvgScriptParser()

    @Test
    fun parsesMinimalScriptIntoAstWithoutExecutingIt() {
        val script = parser.parse(
            "main.avg",
            """
                label start
                background "school"
                character "yuki" "happy" left
                say "Yuki" "Hello"
                hide "yuki"
                jump end
                label end
            """.trimIndent(),
        )

        assertEquals("main.avg", script.id)
        assertEquals(mapOf("start" to 0, "end" to 6), script.labels)
        assertEquals(ScriptNode.Label("start"), script.nodes[0])
        assertEquals(Command.ChangeBackground("school"), commandAt(script, 1))
        assertEquals(
            Command.ShowCharacter("yuki", "happy", CharacterPosition.Left),
            commandAt(script, 2),
        )
        assertEquals(Command.Say("Yuki", "Hello"), commandAt(script, 3))
        assertEquals(Command.HideCharacter("yuki"), commandAt(script, 4))
        assertEquals(Command.Jump("end"), commandAt(script, 5))
    }

    @Test
    fun parsesAllVariableTypes() {
        val script = parser.parse(
            "variables.avg",
            """
                set affection.yuki = 10
                set flag.met_yuki = true
                set ratio = 1.5
                set player.name = "主人公"
            """.trimIndent(),
        )

        assertEquals(Command.SetVariable("affection.yuki", Variable.IntValue(10)), commandAt(script, 0))
        assertEquals(Command.SetVariable("flag.met_yuki", Variable.BooleanValue(true)), commandAt(script, 1))
        assertEquals(Command.SetVariable("ratio", Variable.DoubleValue(1.5)), commandAt(script, 2))
        assertEquals(Command.SetVariable("player.name", Variable.StringValue("主人公")), commandAt(script, 3))
    }

    @Test
    fun supportsUnicodeEmptyInputCommentsAndEscapes() {
        assertEquals(emptyList(), parser.parse("empty.avg", "\n # only a comment\n").nodes)

        val script = parser.parse(
            "unicode.avg",
            "say \"悠希\" \"早上好。今日は晴れ。\\n\\\"Hello\\\"\\\\world\"",
        )
        assertEquals(
            Command.Say("悠希", "早上好。今日は晴れ。\n\"Hello\"\\world"),
            commandAt(script, 0),
        )
        assertEquals(
            Command.Say(null, "Narration"),
            commandAt(parser.parse("narration.avg", "say \"Narration\""), 0),
        )
    }

    @Test
    fun parsesIndentedChoicesWithOrderedBranchCommands() {
        val script = parser.parse(
            "choice.avg",
            """
                label start
                choice:
                    "早上好":
                        set affection.yuki = 1
                        jump morning
                    "你是谁？":
                        jump stranger
                label morning
                label stranger
            """.trimIndent(),
        )

        val choice = assertIs<Command.Choice>(commandAt(script, 1))
        assertEquals(listOf("早上好", "你是谁？"), choice.choices.map { it.text })
        assertEquals(
            listOf(
                Command.SetVariable("affection.yuki", Variable.IntValue(1)),
                Command.Jump("morning"),
            ),
            choice.choices[0].commands,
        )
        assertEquals(listOf(Command.Jump("stranger")), choice.choices[1].commands)
        assertEquals(2, script.labels.getValue("morning"))
        assertEquals(3, script.labels.getValue("stranger"))
    }

    @Test
    fun parsesAudioCommandsWithoutExecutingThem() {
        val script = parser.parse(
            "audio.avg",
            """
                play_bgm "morning_theme" once
                play_se "door"
                play_voice "yuki_001"
                stop_voice
                stop_bgm 500
            """.trimIndent(),
        )

        assertEquals(Command.PlayBgm("morning_theme", false), commandAt(script, 0))
        assertEquals(Command.PlaySe("door"), commandAt(script, 1))
        assertEquals(Command.PlayVoice("yuki_001"), commandAt(script, 2))
        assertEquals(Command.StopVoice, commandAt(script, 3))
        assertEquals(Command.StopBgm(500), commandAt(script, 4))
    }

    @Test
    fun parsesConditionalBranchesIntoAst() {
        val script = parser.parse(
            "route.avg",
            """
                if affection.yuki >= 10:
                    jump good_end
                else:
                    set route = "normal"
                    jump normal_end
            """.trimIndent(),
        )

        val command = assertIs<Command.If>(commandAt(script, 0))
        assertEquals(
            Condition.VariableComparison(
                "affection.yuki",
                ComparisonOperator.GreaterOrEqual,
                Variable.IntValue(10),
            ),
            command.condition,
        )
        assertEquals(listOf(Command.Jump("good_end")), command.thenCommands)
        assertEquals(
            listOf(Command.SetVariable("route", Variable.StringValue("normal")), Command.Jump("normal_end")),
            command.elseCommands,
        )
    }

    @Test
    fun parsesVisualEffectCommands() {
        val script = parser.parse(
            "effects.avg",
            """
                show_cg "cg_01" fade 500
                transition flash 120
                shake 300 2
                move_character "yuki" right 450
                hide_cg
                wait 250
            """.trimIndent(),
        )

        assertEquals(Command.ShowCg("cg_01", TransitionType.Fade, 500), commandAt(script, 0))
        assertEquals(Command.Transition(TransitionType.Flash, 120), commandAt(script, 1))
        assertEquals(Command.Shake(300, 2f), commandAt(script, 2))
        assertEquals(Command.MoveCharacter("yuki", CharacterPosition.Right, 450), commandAt(script, 3))
        assertEquals(Command.HideCg, commandAt(script, 4))
        assertEquals(Command.Wait(250), commandAt(script, 5))
    }

    @Test
    fun rejectsUnknownCommandWithSourceContext() {
        val error = assertFailsWith<EngineException.UnknownCommand> {
            parser.parse("main.avg", "label start\nexplode now")
        }

        assertTrue(error.message.orEmpty().contains("script: main.avg"))
        assertTrue(error.message.orEmpty().contains("line: 2"))
        assertTrue(error.message.orEmpty().contains("command: explode"))
    }

    @Test
    fun rejectsMalformedSyntaxAndDuplicateLabels() {
        val unclosed = assertFailsWith<EngineException.ScriptParseError> {
            parser.parse("main.avg", "say \"Yuki\" \"Hello")
        }
        assertTrue(unclosed.message.orEmpty().contains("line: 1"))

        assertFailsWith<EngineException.ScriptParseError> {
            parser.parse("main.avg", "say Yuki \"Hello\"")
        }
        assertFailsWith<EngineException.ScriptParseError> {
            parser.parse("main.avg", "label start\nlabel start")
        }
        assertFailsWith<EngineException.ScriptParseError> {
            parser.parse("main.avg", "set score = many")
        }
        assertFailsWith<EngineException.ScriptParseError> {
            parser.parse("main.avg", "character \"yuki\" \"happy\" above")
        }
        assertFailsWith<EngineException.ScriptParseError> {
            parser.parse("main.avg", "choice:\n    \"Option\":")
        }
        assertFailsWith<EngineException.ScriptParseError> {
            parser.parse("main.avg", "choice:\n    \"Option\":\n        say \"Yuki\" \"No\"")
        }
        assertFailsWith<EngineException.ScriptParseError> {
            parser.parse("main.avg", "\tlabel start")
        }
        assertFailsWith<EngineException.ScriptParseError> {
            parser.parse("main.avg", "play_bgm \"theme\" forever")
        }
        assertFailsWith<EngineException.ScriptParseError> {
            parser.parse("main.avg", "stop_bgm -1")
        }
        assertFailsWith<EngineException.ScriptParseError> {
            parser.parse("main.avg", "show_cg \"cg\" spin")
        }
        assertFailsWith<EngineException.ScriptParseError> {
            parser.parse("main.avg", "shake 200 11")
        }
        assertFailsWith<EngineException.ScriptParseError> {
            parser.parse("main.avg", "wait -1")
        }
        assertFailsWith<EngineException.ScriptParseError> {
            parser.parse("main.avg", "if score ~~ 1:\n    jump end")
        }
        assertFailsWith<EngineException.ScriptParseError> {
            parser.parse("main.avg", "if score == 1:")
        }
    }

    private fun commandAt(script: Script, index: Int): Command =
        assertIs<ScriptNode.CommandNode>(script.nodes[index]).command
}
