package io.github.daisukikaffuchino.nekoscript.engine.command

import io.github.daisukikaffuchino.nekoscript.engine.character.CharacterPosition
import io.github.daisukikaffuchino.nekoscript.engine.choice.ChoiceItem
import io.github.daisukikaffuchino.nekoscript.engine.variable.Variable
import io.github.daisukikaffuchino.nekoscript.engine.variable.Condition
import io.github.daisukikaffuchino.nekoscript.engine.effect.TransitionType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Platform-independent instruction produced by the script parser.
 *
 * Commands describe intent only. They neither mutate [io.github.daisukikaffuchino.nekoscript.engine.runtime.GameState]
 * directly nor interact with a renderer.
 */
@Serializable
sealed interface Command {
    /**
     * Presents one complete dialogue line.
     *
     * @property speaker speaker identifier; `null` denotes narration
     * @property text complete dialogue text
     */
    @Serializable
    @SerialName("say")
    data class Say(
        val speaker: String?,
        val text: String,
    ) : Command

    /** @property backgroundId project-defined background identifier */
    @Serializable
    @SerialName("change_background")
    data class ChangeBackground(
        val backgroundId: String,
    ) : Command {
        init {
            require(backgroundId.isNotBlank()) { "Background id must not be blank." }
        }
    }

    /**
     * Makes a character visible or replaces its visible state.
     *
     * @property characterId project-defined character identifier
     * @property expression project-defined expression identifier
     * @property position logical placement in the scene
     */
    @Serializable
    @SerialName("show_character")
    data class ShowCharacter(
        val characterId: String,
        val expression: String? = null,
        val position: CharacterPosition = CharacterPosition.Center,
    ) : Command {
        init {
            require(characterId.isNotBlank()) { "Character id must not be blank." }
        }
    }

    /** @property characterId identifier of the character to remove from the scene */
    @Serializable
    @SerialName("hide_character")
    data class HideCharacter(
        val characterId: String,
    ) : Command {
        init {
            require(characterId.isNotBlank()) { "Character id must not be blank." }
        }
    }

    /** @property label script label that becomes the next execution position */
    @Serializable
    @SerialName("jump")
    data class Jump(
        val label: String,
    ) : Command {
        init {
            require(label.isNotBlank()) { "Jump label must not be blank." }
        }
    }

    /**
     * Replaces a script variable with a typed value.
     *
     * @property name script-visible variable name
     * @property value value assigned to the variable
     */
    @Serializable
    @SerialName("set_variable")
    data class SetVariable(
        val name: String,
        val value: Variable,
    ) : Command {
        init {
            require(name.isNotBlank()) { "Variable name must not be blank." }
        }
    }

    /** @property choices options presented to the player */
    @Serializable
    @SerialName("choice")
    data class Choice(
        val choices: List<ChoiceItem>,
    ) : Command {
        init {
            require(choices.isNotEmpty()) { "Choice command must contain at least one option." }
        }
    }

    /**
     * Executes one command branch based on [condition].
     *
     * Branches currently contain immediate commands so evaluation cannot create
     * an untracked nested pause point.
     */
    @Serializable
    @SerialName("if")
    data class If(
        val condition: Condition,
        val thenCommands: List<Command>,
        val elseCommands: List<Command> = emptyList(),
    ) : Command {
        init {
            require(thenCommands.isNotEmpty()) { "If command must contain a non-empty then branch." }
        }
    }

    /** Starts project-defined background music. */
    @Serializable
    @SerialName("play_bgm")
    data class PlayBgm(
        val audioId: String,
        val loop: Boolean = true,
    ) : Command {
        init {
            require(audioId.isNotBlank()) { "BGM id must not be blank." }
        }
    }

    /** Stops background music with an optional logical fade duration. */
    @Serializable
    @SerialName("stop_bgm")
    data class StopBgm(
        val fadeOutMillis: Long = 0,
    ) : Command {
        init {
            require(fadeOutMillis >= 0) { "BGM fade duration must not be negative." }
        }
    }

    /** Plays a one-shot project-defined sound effect. */
    @Serializable
    @SerialName("play_se")
    data class PlaySe(val audioId: String) : Command {
        init {
            require(audioId.isNotBlank()) { "Sound effect id must not be blank." }
        }
    }

    /** Starts a project-defined voice clip. */
    @Serializable
    @SerialName("play_voice")
    data class PlayVoice(val audioId: String) : Command {
        init {
            require(audioId.isNotBlank()) { "Voice id must not be blank." }
        }
    }

    /** Stops the active voice clip. */
    @Serializable
    @SerialName("stop_voice")
    data object StopVoice : Command

    /** Shows a project-defined CG using an optional transition. */
    @Serializable
    @SerialName("show_cg")
    data class ShowCg(
        val cgId: String,
        val transition: TransitionType = TransitionType.CrossFade,
        val durationMillis: Long = 400,
    ) : Command {
        init {
            require(cgId.isNotBlank()) { "CG id must not be blank." }
            require(durationMillis >= 0) { "CG transition duration must not be negative." }
        }
    }

    /** Hides the active CG. */
    @Serializable
    @SerialName("hide_cg")
    data object HideCg : Command

    /** Requests a renderer-independent scene transition. */
    @Serializable
    @SerialName("transition")
    data class Transition(
        @SerialName("transition_type")
        val type: TransitionType,
        val durationMillis: Long,
    ) : Command {
        init {
            require(durationMillis >= 0) { "Transition duration must not be negative." }
        }
    }

    /** Requests a screen shake. */
    @Serializable
    @SerialName("shake")
    data class Shake(
        val durationMillis: Long,
        val intensity: Float = 1f,
    ) : Command {
        init {
            require(durationMillis >= 0) { "Shake duration must not be negative." }
            require(intensity in 0f..10f) { "Shake intensity must be between 0 and 10." }
        }
    }

    /** Moves a visible character to [position]. */
    @Serializable
    @SerialName("move_character")
    data class MoveCharacter(
        val characterId: String,
        val position: CharacterPosition,
        val durationMillis: Long = 300,
    ) : Command {
        init {
            require(characterId.isNotBlank()) { "Character id must not be blank." }
            require(durationMillis >= 0) { "Character move duration must not be negative." }
        }
    }

    /** Suspends script execution for [durationMillis] without involving Compose. */
    @Serializable
    @SerialName("wait")
    data class Wait(val durationMillis: Long) : Command {
        init {
            require(durationMillis >= 0) { "Wait duration must not be negative." }
        }
    }
}
