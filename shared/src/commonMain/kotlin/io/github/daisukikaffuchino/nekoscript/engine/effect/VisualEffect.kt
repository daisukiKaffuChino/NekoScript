package io.github.daisukikaffuchino.nekoscript.engine.effect

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Renderer-independent transition types requested by scripts. */
@Serializable
enum class TransitionType {
    Fade,
    CrossFade,
    Slide,
    Flash,
}

/** Renderer-independent visual effect descriptor. */
@Serializable
sealed interface VisualEffect {
    /** Requests a scene transition of [type] lasting [durationMillis]. */
    @Serializable
    @SerialName("transition")
    data class Transition(
        @SerialName("transition_type")
        val type: TransitionType,
        val durationMillis: Long,
    ) : VisualEffect {
        init {
            require(durationMillis >= 0) { "Transition duration must not be negative." }
        }
    }

    /** Requests a screen shake lasting [durationMillis] at normalized [intensity]. */
    @Serializable
    @SerialName("shake")
    data class Shake(
        val durationMillis: Long,
        val intensity: Float = 1f,
    ) : VisualEffect {
        init {
            require(durationMillis >= 0) { "Shake duration must not be negative." }
            require(intensity in 0f..10f) { "Shake intensity must be between 0 and 10." }
        }
    }

    /** Requests animated movement for [characterId]. */
    @Serializable
    @SerialName("character_move")
    data class CharacterMove(
        val characterId: String,
        val durationMillis: Long,
    ) : VisualEffect {
        init {
            require(characterId.isNotBlank()) { "Character id must not be blank." }
            require(durationMillis >= 0) { "Character move duration must not be negative." }
        }
    }
}

/**
 * Latest visual effect request and its monotonically increasing [sequence].
 * The sequence lets renderers replay adjacent effects with identical values.
 */
@Serializable
data class VisualEffectState(
    val sequence: Long,
    val effect: VisualEffect,
) {
    init {
        require(sequence > 0) { "Visual effect sequence must be positive." }
    }
}
