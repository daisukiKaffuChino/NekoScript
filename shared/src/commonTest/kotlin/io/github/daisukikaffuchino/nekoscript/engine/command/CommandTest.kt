package io.github.daisukikaffuchino.nekoscript.engine.command

import io.github.daisukikaffuchino.nekoscript.engine.character.CharacterPosition
import io.github.daisukikaffuchino.nekoscript.engine.choice.ChoiceItem
import io.github.daisukikaffuchino.nekoscript.engine.variable.Variable
import io.github.daisukikaffuchino.nekoscript.engine.variable.ComparisonOperator
import io.github.daisukikaffuchino.nekoscript.engine.variable.Condition
import io.github.daisukikaffuchino.nekoscript.engine.effect.TransitionType
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class CommandTest {
    @Test
    fun saySupportsDialogueAndNarration() {
        assertEquals("Hello", Command.Say("yuki", "Hello").text)
        assertNull(Command.Say(null, "A quiet morning.").speaker)
    }

    @Test
    fun backgroundCarriesProjectIdentifier() {
        assertEquals("school", Command.ChangeBackground("school").backgroundId)
        assertFailsWith<IllegalArgumentException> { Command.ChangeBackground(" ") }
    }

    @Test
    fun showCharacterCarriesExpressionAndPosition() {
        val command = Command.ShowCharacter("yuki", "happy", CharacterPosition.Left)

        assertEquals("yuki", command.characterId)
        assertEquals("happy", command.expression)
        assertEquals(CharacterPosition.Left, command.position)
        assertFailsWith<IllegalArgumentException> { Command.ShowCharacter("") }
    }

    @Test
    fun hideCharacterCarriesProjectIdentifier() {
        assertEquals("yuki", Command.HideCharacter("yuki").characterId)
        assertFailsWith<IllegalArgumentException> { Command.HideCharacter(" ") }
    }

    @Test
    fun jumpCarriesDestinationLabel() {
        assertEquals("morning", Command.Jump("morning").label)
        assertFailsWith<IllegalArgumentException> { Command.Jump("") }
    }

    @Test
    fun setVariableCarriesTypedValue() {
        val command = Command.SetVariable("affection.yuki", Variable.IntValue(1))

        assertEquals("affection.yuki", command.name)
        assertEquals(Variable.IntValue(1), command.value)
        assertFailsWith<IllegalArgumentException> {
            Command.SetVariable(" ", Variable.BooleanValue(true))
        }
    }

    @Test
    fun choiceCarriesOrderedBranchCommands() {
        val command = Command.Choice(
            listOf(
                ChoiceItem(
                    text = "早上好",
                    commands = listOf(
                        Command.SetVariable("greeted", Variable.BooleanValue(true)),
                        Command.Jump("morning"),
                    ),
                ),
            ),
        )

        assertEquals("早上好", command.choices.single().text)
        assertEquals(2, command.choices.single().commands.size)
        assertFailsWith<IllegalArgumentException> { Command.Choice(emptyList()) }
        assertFailsWith<IllegalArgumentException> { ChoiceItem(" ", emptyList()) }
    }

    @Test
    fun allCommandsRoundTripThroughJson() {
        val commands: List<Command> = listOf(
            Command.Say("yuki", "Hello"),
            Command.ChangeBackground("school"),
            Command.ShowCharacter("yuki", "happy", CharacterPosition.Right),
            Command.HideCharacter("yuki"),
            Command.Jump("morning"),
            Command.SetVariable("seen", Variable.BooleanValue(true)),
            Command.Choice(listOf(ChoiceItem("Continue", listOf(Command.Jump("end"))))),
            Command.PlayBgm("morning_theme", loop = false),
            Command.StopBgm(250),
            Command.PlaySe("door"),
            Command.PlayVoice("yuki_001"),
            Command.StopVoice,
            Command.If(
                condition = Condition.VariableComparison(
                    "score",
                    ComparisonOperator.GreaterOrEqual,
                    Variable.IntValue(10),
                ),
                thenCommands = listOf(Command.Jump("good")),
                elseCommands = listOf(Command.Jump("normal")),
            ),
            Command.ShowCg("cg_01", TransitionType.Fade, 500),
            Command.HideCg,
            Command.Transition(TransitionType.Flash, 120),
            Command.Shake(300, 2f),
            Command.MoveCharacter("yuki", CharacterPosition.Right, 450),
            Command.Wait(250),
        )
        val serializer = ListSerializer(Command.serializer())

        val encoded = Json.encodeToString(serializer, commands)
        val decoded = Json.decodeFromString(serializer, encoded)

        assertEquals(commands, decoded)
    }

    @Test
    fun audioCommandsValidateIdentifiersAndDurations() {
        assertFailsWith<IllegalArgumentException> { Command.PlayBgm(" ") }
        assertFailsWith<IllegalArgumentException> { Command.StopBgm(-1) }
        assertFailsWith<IllegalArgumentException> { Command.PlaySe("") }
        assertFailsWith<IllegalArgumentException> { Command.PlayVoice(" ") }
    }

    @Test
    fun visualCommandsValidateIdentifiersDurationsAndIntensity() {
        assertFailsWith<IllegalArgumentException> { Command.ShowCg(" ") }
        assertFailsWith<IllegalArgumentException> { Command.ShowCg("cg", durationMillis = -1) }
        assertFailsWith<IllegalArgumentException> { Command.Transition(TransitionType.Fade, -1) }
        assertFailsWith<IllegalArgumentException> { Command.Shake(100, 11f) }
        assertFailsWith<IllegalArgumentException> { Command.MoveCharacter("", CharacterPosition.Left) }
        assertFailsWith<IllegalArgumentException> { Command.Wait(-1) }
    }
}
