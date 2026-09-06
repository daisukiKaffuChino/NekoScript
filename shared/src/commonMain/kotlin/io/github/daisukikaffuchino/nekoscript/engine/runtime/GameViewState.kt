package io.github.daisukikaffuchino.nekoscript.engine.runtime

import io.github.daisukikaffuchino.nekoscript.engine.character.CharacterPosition
import io.github.daisukikaffuchino.nekoscript.engine.effect.TransitionType

/**
 * Presentation-ready snapshot consumed by a UI implementation.
 *
 * This model intentionally contains no Compose types, which keeps alternate
 * renderers free to consume it.
 *
 * @property background background to render
 * @property characters characters to render, in back-to-front order
 * @property dialogue current dialogue to render
 * @property choices currently available choices
 * @property isWaiting whether the engine is paused for player input
 * @property isAutoMode whether automatic progression is active
 * @property isSkipMode whether skip mode is active
 * @property history presentation-ready dialogue history
 * @property isBacklogOpen whether the backlog overlay is requested
 * @property textSpeedMillis delay between characters used by a typewriter renderer
 * @property cg renderer-facing CG selection
 * @property visualEffect latest presentation effect request
 */
data class GameViewState(
    val background: BackgroundView? = null,
    val characters: List<CharacterView> = emptyList(),
    val dialogue: DialogueView? = null,
    val choices: List<ChoiceView> = emptyList(),
    val isWaiting: Boolean = false,
    val isAutoMode: Boolean = false,
    val isSkipMode: Boolean = false,
    val history: List<DialogueView> = emptyList(),
    val isBacklogOpen: Boolean = false,
    val textSpeedMillis: Int = DEFAULT_TEXT_SPEED_MILLIS,
    val cg: CgView? = null,
    val visualEffect: VisualEffectView? = null,
)

/** Default delay between characters in the standard presentation. */
const val DEFAULT_TEXT_SPEED_MILLIS: Int = 35

/** @property assetId renderer-facing background asset identifier */
data class BackgroundView(val assetId: String)

/**
 * @property characterId project-defined character identifier
 * @property expression project-defined expression identifier
 * @property position logical position used by the renderer
 */
data class CharacterView(
    val characterId: String,
    val expression: String? = null,
    val position: CharacterPosition = CharacterPosition.Center,
)

/**
 * @property speaker display-ready speaker text, or `null` for narration
 * @property text complete display-ready dialogue text
 */
data class DialogueView(
    val speaker: String?,
    val text: String,
)

/**
 * @property index zero-based index sent back through [GameAction.SelectChoice]
 * @property text display-ready choice text
 * @property isEnabled whether the choice currently accepts input
 */
data class ChoiceView(
    val index: Int,
    val text: String,
    val isEnabled: Boolean = true,
)

/** @property assetId renderer-facing CG asset identifier */
data class CgView(val assetId: String)

/** Presentation-safe visual effect request. */
sealed interface VisualEffectView {
    val sequence: Long
    val durationMillis: Long

    data class Transition(
        override val sequence: Long,
        override val durationMillis: Long,
        val type: TransitionType,
    ) : VisualEffectView

    data class Shake(
        override val sequence: Long,
        override val durationMillis: Long,
        val intensity: Float,
    ) : VisualEffectView

    data class CharacterMove(
        override val sequence: Long,
        override val durationMillis: Long,
        val characterId: String,
    ) : VisualEffectView
}
