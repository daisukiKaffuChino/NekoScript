package io.github.daisukikaffuchino.nekoscript.engine.project

import io.github.daisukikaffuchino.nekoscript.engine.choice.ChoiceItem
import io.github.daisukikaffuchino.nekoscript.engine.command.Command
import io.github.daisukikaffuchino.nekoscript.engine.error.EngineException
import io.github.daisukikaffuchino.nekoscript.engine.script.Script
import io.github.daisukikaffuchino.nekoscript.engine.script.ScriptNode
import io.github.daisukikaffuchino.nekoscript.engine.variable.ComparisonOperator
import io.github.daisukikaffuchino.nekoscript.engine.variable.Condition
import io.github.daisukikaffuchino.nekoscript.engine.variable.Variable
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GameProjectValidatorTest {
    private val validator = GameProjectValidator()

    @Test
    fun validatesResourcesAndLabelsInsideNestedChoiceAndIfCommands() {
        val nested = Command.Choice(
            listOf(
                ChoiceItem(
                    "Continue",
                    listOf(
                        Command.If(
                            condition = Condition.VariableComparison(
                                "route",
                                ComparisonOperator.Equal,
                                Variable.StringValue("main"),
                            ),
                            thenCommands = listOf(Command.PlaySe("bell"), Command.Jump("ending")),
                            elseCommands = listOf(Command.PlayVoice("line")),
                        ),
                    ),
                ),
            ),
        )
        val script = scriptOf(nested)

        validator.validate(validProject(), script)
    }

    @Test
    fun rejectsUnknownNestedAssetAndMissingLabel() {
        val unknownAsset = assertFailsWith<EngineException.ProjectLoadError> {
            validator.validate(validProject(), scriptOf(Command.Choice(listOf(ChoiceItem("Go", listOf(Command.PlaySe("missing")))))))
        }
        assertTrue(unknownAsset.message.orEmpty().contains("sound effect 'missing'"))

        val missingLabel = assertFailsWith<EngineException.ProjectLoadError> {
            validator.validate(validProject(), scriptOf(Command.If(condition(), listOf(Command.Jump("missing")))))
        }
        assertTrue(missingLabel.message.orEmpty().contains("label 'missing'"))
    }

    @Test
    fun rejectsUnknownCharacterExpressionAndSceneResources() {
        val expression = assertFailsWith<EngineException.ProjectLoadError> {
            validator.validate(validProject(), scriptOf(Command.ShowCharacter("yuki", "missing")))
        }
        assertTrue(expression.message.orEmpty().contains("yuki:missing"))

        val background = assertFailsWith<EngineException.ProjectLoadError> {
            validator.validate(validProject(), scriptOf(Command.ChangeBackground("missing")))
        }
        assertTrue(background.message.orEmpty().contains("background 'missing'"))

        val cg = assertFailsWith<EngineException.ProjectLoadError> {
            validator.validate(validProject(), scriptOf(Command.ShowCg("missing")))
        }
        assertTrue(cg.message.orEmpty().contains("CG 'missing'"))
    }

    @Test
    fun rejectsUnsafeEmptyAndUnsupportedManifestLocations() {
        val unsafe = assertFailsWith<EngineException.ProjectLoadError> {
            validator.validate(
                validProject().copy(backgrounds = mapOf("school" to "../school.jpg")),
                scriptOf(Command.Say(null, "Ready")),
            )
        }
        assertTrue(unsafe.message.orEmpty().contains("must not escape"))

        val empty = assertFailsWith<EngineException.ProjectLoadError> {
            validator.validate(
                validProject().copy(cg = mapOf("ending" to "")),
                scriptOf(Command.Say(null, "Ready")),
            )
        }
        assertTrue(empty.message.orEmpty().contains("must not be blank"))

        val ogg = assertFailsWith<EngineException.ProjectLoadError> {
            validator.validate(
                validProject().copy(audio = AudioManifest(voice = mapOf("line" to "audio/line.ogg"))),
                scriptOf(Command.Say(null, "Ready")),
            )
        }
        assertTrue(ogg.message.orEmpty().contains("unsupported audio format 'ogg'"))
    }

    private fun scriptOf(command: Command): Script = Script(
        id = "scripts/main.avg",
        nodes = listOf(ScriptNode.CommandNode(command), ScriptNode.Label("ending")),
        labels = mapOf("ending" to 1),
    )

    private fun validProject(): GameProject = GameProject(
        id = "test",
        name = "Test",
        version = "1",
        entryScript = "scripts/main.avg",
        backgrounds = mapOf("school" to "backgrounds/school.jpg"),
        characters = mapOf(
            "yuki" to CharacterDefinition(
                name = "Yuki",
                defaultExpression = "normal",
                expressions = mapOf("normal" to "characters/yuki/normal.png"),
            ),
        ),
        cg = mapOf("ending" to "cg/ending.jpg"),
        audio = AudioManifest(
            bgm = mapOf("theme" to "audio/theme.mp3"),
            se = mapOf("bell" to "audio/bell.wav"),
            voice = mapOf("line" to "audio/line.mp3"),
        ),
    )

    private fun condition(): Condition = Condition.VariableComparison(
        "route",
        ComparisonOperator.Equal,
        Variable.StringValue("main"),
    )
}
