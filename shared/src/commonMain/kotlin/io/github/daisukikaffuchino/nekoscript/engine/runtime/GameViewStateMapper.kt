package io.github.daisukikaffuchino.nekoscript.engine.runtime

import io.github.daisukikaffuchino.nekoscript.engine.effect.VisualEffect
import io.github.daisukikaffuchino.nekoscript.engine.project.GameProject
import io.github.daisukikaffuchino.nekoscript.engine.viewport.GameViewport

/** Converts an engine state snapshot into the smaller renderer-facing model. */
fun GameState.toGameViewState(
    isAutoMode: Boolean = false,
    isSkipMode: Boolean = false,
    isBacklogOpen: Boolean = false,
    textSpeedMillis: Int = DEFAULT_TEXT_SPEED_MILLIS,
    project: GameProject? = null,
): GameViewState = GameViewState(
    viewport = project?.viewport?.toGameViewport() ?: GameViewport.DEFAULT,
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
    debug = project?.takeIf(GameProject::debuggable)?.let(::toDebugView),
)

private fun GameState.toDebugView(project: GameProject): GameDebugView = GameDebugView(
    scriptId = scriptId,
    nextNodeIndex = position.nodeIndex,
    textId = dialogue?.let { "$scriptId:node_${(position.nodeIndex - 1).coerceAtLeast(0)}" },
    background = background?.backgroundId?.let { id ->
        project.backgrounds[id]?.let { DebugAssetView(id, it) }
    },
    characters = characters.mapNotNull { character ->
        val definition = project.characters[character.characterId] ?: return@mapNotNull null
        val expression = character.expression ?: definition.defaultExpression
        definition.expressions[expression]?.let { location ->
            DebugAssetView("${character.characterId}:$expression", location)
        }
    },
    cg = cgId?.let { id -> project.cg[id]?.let { DebugAssetView(id, it) } },
    bgm = audio.bgmId?.let { id -> project.audio.bgm[id]?.let { DebugAssetView(id, it) } },
    voice = audio.voiceId?.let { id -> project.audio.voice[id]?.let { DebugAssetView(id, it) } },
    effect = visualEffect?.takeIf { status == RuntimeStatus.WaitingForEffect }?.effect?.let { effect ->
        when (effect) {
            is VisualEffect.Transition -> "transition:${effect.type.name.lowercase()}"
            is VisualEffect.Shake -> "shake"
            is VisualEffect.CharacterMove -> "move:${effect.characterId}"
        }
    },
)
