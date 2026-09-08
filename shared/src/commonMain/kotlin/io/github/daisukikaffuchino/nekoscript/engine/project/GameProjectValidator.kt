package io.github.daisukikaffuchino.nekoscript.engine.project

import io.github.daisukikaffuchino.nekoscript.engine.command.Command
import io.github.daisukikaffuchino.nekoscript.engine.error.EngineException
import io.github.daisukikaffuchino.nekoscript.engine.script.Script
import io.github.daisukikaffuchino.nekoscript.engine.script.ScriptNode

/** Validates a parsed project's static asset and label references before runtime creation. */
class GameProjectValidator {
    fun validate(project: GameProject, script: Script) {
        validateLocation(project, "entry script", script.id, project.entryScript)
        project.backgrounds.forEach { (id, location) -> validateLocation(project, "background", id, location) }
        project.characters.forEach { (characterId, character) ->
            character.expressions.forEach { (expressionId, location) ->
                validateLocation(project, "character expression", "$characterId:$expressionId", location)
            }
        }
        project.cg.forEach { (id, location) -> validateLocation(project, "CG", id, location) }
        project.audio.bgm.forEach { (id, location) -> validateAudioLocation(project, "BGM", id, location) }
        project.audio.se.forEach { (id, location) -> validateAudioLocation(project, "sound effect", id, location) }
        project.audio.voice.forEach { (id, location) -> validateAudioLocation(project, "voice", id, location) }
        project.hotspots.forEach { (id, hotspot) ->
            if (id.isBlank()) invalid(project, "hotspot id must not be blank")
            if (hotspot.target !in script.labels) {
                invalid(project, "hotspot '$id' targets undeclared label '${hotspot.target}'")
            }
        }

        script.nodes.filterIsInstance<ScriptNode.CommandNode>().forEach { node ->
            validateCommand(project, script, node.command)
        }
    }

    private fun validateCommand(project: GameProject, script: Script, command: Command) {
        when (command) {
            is Command.ChangeBackground -> requireAsset(project, "background", command.backgroundId, project.backgrounds)
            is Command.ShowCharacter -> {
                val character = project.characters[command.characterId]
                    ?: invalid(project, "character '${command.characterId}' is not declared")
                val expression = command.expression ?: character.defaultExpression
                if (expression !in character.expressions) {
                    invalid(project, "character expression '${command.characterId}:$expression' is not declared")
                }
            }
            is Command.HideCharacter -> requireCharacter(project, command.characterId)
            is Command.MoveCharacter -> requireCharacter(project, command.characterId)
            is Command.ShowCg -> requireAsset(project, "CG", command.cgId, project.cg)
            is Command.PlayBgm -> requireAsset(project, "BGM", command.audioId, project.audio.bgm)
            is Command.PlaySe -> requireAsset(project, "sound effect", command.audioId, project.audio.se)
            is Command.PlayVoice -> requireAsset(project, "voice", command.audioId, project.audio.voice)
            is Command.Jump -> if (command.label !in script.labels) {
                invalid(project, "label '${command.label}' is not declared in script '${script.id}'")
            }
            is Command.Choice -> command.choices.forEach { choice ->
                choice.commands.forEach { validateCommand(project, script, it) }
            }
            is Command.If -> {
                command.thenCommands.forEach { validateCommand(project, script, it) }
                command.elseCommands.forEach { validateCommand(project, script, it) }
            }
            is Command.Say,
            is Command.SetVariable,
            is Command.StopBgm,
            Command.StopVoice,
            Command.HideCg,
            is Command.Transition,
            is Command.Shake,
            is Command.Wait,
            -> Unit
        }
    }

    private fun requireCharacter(project: GameProject, id: String) {
        if (id !in project.characters) invalid(project, "character '$id' is not declared")
    }

    private fun requireAsset(project: GameProject, type: String, id: String, assets: Map<String, String>) {
        if (id !in assets) invalid(project, "$type '$id' is not declared")
    }

    private fun validateAudioLocation(project: GameProject, type: String, id: String, location: String) {
        validateLocation(project, type, id, location)
        val extension = location.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        if (extension !in SUPPORTED_AUDIO_EXTENSIONS) {
            invalid(
                project,
                "$type '$id' at '$location' uses unsupported audio format '${extension.ifBlank { "unknown" }}'; " +
                    "supported formats are MP3 and WAV",
            )
        }
    }

    private fun validateLocation(project: GameProject, type: String, id: String, location: String) {
        try {
            validateProjectLocation(location)
        } catch (error: IllegalArgumentException) {
            invalid(project, "$type '$id' has invalid location '$location': ${error.message}")
        }
    }

    private fun invalid(project: GameProject, detail: String): Nothing =
        throw EngineException.ProjectLoadError("project: ${project.id}: validation failed: $detail")

    private companion object {
        val SUPPORTED_AUDIO_EXTENSIONS = setOf("mp3", "wav")
    }
}
