package io.github.daisukikaffuchino.nekoscript.engine.runtime

import io.github.daisukikaffuchino.nekoscript.engine.background.BackgroundState
import io.github.daisukikaffuchino.nekoscript.engine.audio.AudioState
import io.github.daisukikaffuchino.nekoscript.engine.character.CharacterState
import io.github.daisukikaffuchino.nekoscript.engine.choice.ChoiceItem
import io.github.daisukikaffuchino.nekoscript.engine.dialogue.Dialogue
import io.github.daisukikaffuchino.nekoscript.engine.effect.VisualEffectState
import io.github.daisukikaffuchino.nekoscript.engine.script.ScriptPosition
import io.github.daisukikaffuchino.nekoscript.engine.variable.VariableStore
import kotlinx.serialization.Serializable

/**
 * Complete platform-independent logical state of a running game.
 *
 * Presentation details such as animation progress and button state must not be
 * stored here.
 *
 * @property scriptId identifier of the active script
 * @property position next script node to execute
 * @property variables current script variables
 * @property characters characters currently visible in the scene
 * @property background current background, or `null` when none is selected
 * @property route optional project-defined route identifier
 * @property dialogue dialogue line currently presented to the player
 * @property history dialogue lines already reached by the runtime
 * @property pendingChoices choices awaiting player selection
 * @property status current logical execution status
 * @property audio persistent logical audio playback state
 * @property cgId project-defined CG identifier currently covering the scene
 * @property visualEffect latest renderer-independent effect request
 * @property playtimeMillis accumulated session playtime used by save-slot previews
 */
@Serializable
data class GameState(
    val scriptId: String,
    val position: ScriptPosition = ScriptPosition(),
    val variables: VariableStore = VariableStore(),
    val characters: List<CharacterState> = emptyList(),
    val background: BackgroundState? = null,
    val route: String? = null,
    val dialogue: Dialogue? = null,
    val history: List<Dialogue> = emptyList(),
    val pendingChoices: List<ChoiceItem> = emptyList(),
    val status: RuntimeStatus = RuntimeStatus.Ready,
    val audio: AudioState = AudioState(),
    val cgId: String? = null,
    val visualEffect: VisualEffectState? = null,
    val playtimeMillis: Long = 0L,
) {
    init {
        require(scriptId.isNotBlank()) { "Script id must not be blank." }
        require(playtimeMillis >= 0) { "Playtime must not be negative." }
    }
}

/** Logical execution state of a script runtime. */
@Serializable
enum class RuntimeStatus {
    Ready,
    Running,
    WaitingForInput,
    WaitingForChoice,
    WaitingForEffect,
    Completed,
}
