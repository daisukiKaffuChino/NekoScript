package io.github.daisukikaffuchino.nekoscript.engine.runtime

import io.github.daisukikaffuchino.nekoscript.engine.effect.VisualEffect

/** Converts an engine state snapshot into the smaller renderer-facing model. */
fun GameState.toGameViewState(
    isAutoMode: Boolean = false,
    isSkipMode: Boolean = false,
    isBacklogOpen: Boolean = false,
    textSpeedMillis: Int = DEFAULT_TEXT_SPEED_MILLIS,
): GameViewState = GameViewState(
    background = background?.let { BackgroundView(it.backgroundId) },
    characters = characters.map {
        CharacterView(
            characterId = it.characterId,
            expression = it.expression,
            position = it.position,
        )
    },
    dialogue = dialogue?.let { DialogueView(it.speaker, it.text) },
    choices = pendingChoices.mapIndexed { index, choice -> ChoiceView(index, choice.text) },
    isWaiting = status == RuntimeStatus.WaitingForInput || status == RuntimeStatus.WaitingForChoice,
    isAutoMode = isAutoMode,
    isSkipMode = isSkipMode,
    history = history.map { DialogueView(it.speaker, it.text) },
    isBacklogOpen = isBacklogOpen,
    textSpeedMillis = textSpeedMillis,
    cg = cgId?.let(::CgView),
    visualEffect = visualEffect?.takeIf { status == RuntimeStatus.WaitingForEffect }?.let { state ->
        when (val effect = state.effect) {
            is VisualEffect.Transition -> VisualEffectView.Transition(
                state.sequence,
                effect.durationMillis,
                effect.type,
            )
            is VisualEffect.Shake -> VisualEffectView.Shake(
                state.sequence,
                effect.durationMillis,
                effect.intensity,
            )
            is VisualEffect.CharacterMove -> VisualEffectView.CharacterMove(
                state.sequence,
                effect.durationMillis,
                effect.characterId,
            )
        }
    },
)
