package io.github.daisukikaffuchino.nekoscript.engine.script

import io.github.daisukikaffuchino.nekoscript.engine.background.BackgroundState
import io.github.daisukikaffuchino.nekoscript.engine.audio.AudioPlayer
import io.github.daisukikaffuchino.nekoscript.engine.audio.NoOpAudioPlayer
import io.github.daisukikaffuchino.nekoscript.engine.character.CharacterState
import io.github.daisukikaffuchino.nekoscript.engine.command.Command
import io.github.daisukikaffuchino.nekoscript.engine.dialogue.Dialogue
import io.github.daisukikaffuchino.nekoscript.engine.error.EngineException
import io.github.daisukikaffuchino.nekoscript.engine.effect.VisualEffect
import io.github.daisukikaffuchino.nekoscript.engine.effect.VisualEffectState
import io.github.daisukikaffuchino.nekoscript.engine.logging.EngineLogger
import io.github.daisukikaffuchino.nekoscript.engine.logging.NoOpEngineLogger
import kotlinx.coroutines.delay
import io.github.daisukikaffuchino.nekoscript.engine.runtime.GameState
import io.github.daisukikaffuchino.nekoscript.engine.runtime.RuntimeStatus
import io.github.daisukikaffuchino.nekoscript.engine.variable.evaluate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Default deterministic runtime for one parsed [script].
 *
 * It executes non-blocking commands in one pass and pauses for dialogue, choice
 * input, or renderer effect completion. Calls are serialized so concurrent UI
 * actions cannot interleave.
 */
class DefaultScriptRuntime(
    private val script: Script,
    initialState: GameState = GameState(scriptId = script.id),
    private val audioPlayer: AudioPlayer = NoOpAudioPlayer,
    private val logger: EngineLogger = NoOpEngineLogger,
    private val wait: suspend (Long) -> Unit = { delay(it) },
) : ScriptRuntime {
    private val mutableState = MutableStateFlow(initialState.validateFor(script))
    private val mutex = Mutex()

    override val state: StateFlow<GameState> = mutableState.asStateFlow()

    override suspend fun next() = mutex.withLock {
        when (mutableState.value.status) {
            RuntimeStatus.WaitingForChoice -> invalidAction("select a choice before advancing")
            RuntimeStatus.WaitingForEffect -> invalidAction("complete the pending visual effect before advancing")
            RuntimeStatus.Completed -> return@withLock
            RuntimeStatus.WaitingForInput -> mutableState.value = mutableState.value.copy(
                dialogue = null,
                status = RuntimeStatus.Running,
            )
            RuntimeStatus.Ready, RuntimeStatus.Running -> Unit
        }
        executeUntilPause()
    }

    override suspend fun selectChoice(index: Int) = mutex.withLock {
        val current = mutableState.value
        if (current.status != RuntimeStatus.WaitingForChoice) {
            invalidAction("no choice is awaiting selection")
        }
        val choice = current.pendingChoices.getOrNull(index)
            ?: invalidAction("choice index $index is outside 0..${current.pendingChoices.lastIndex}")

        mutableState.value = current.copy(
            pendingChoices = emptyList(),
            status = RuntimeStatus.Running,
        )
        choice.commands.forEach(::executeImmediateChoiceCommand)
        executeUntilPause()
    }

    override suspend fun restore(state: GameState) = mutex.withLock {
        val restored = state.validateFor(script)
        restoreAudio(restored)
        mutableState.value = restored
    }

    override suspend fun completeVisualEffect(sequence: Long) = mutex.withLock {
        val current = mutableState.value
        if (current.status != RuntimeStatus.WaitingForEffect) {
            invalidAction("no visual effect is awaiting completion")
        }
        if (current.visualEffect?.sequence != sequence) {
            invalidAction("visual effect sequence $sequence is not pending")
        }
        mutableState.value = current.copy(status = RuntimeStatus.Running)
        executeUntilPause()
    }

    private suspend fun executeUntilPause() {
        while (mutableState.value.position.nodeIndex < script.nodes.size) {
            val current = mutableState.value
            when (val node = script.nodes[current.position.nodeIndex]) {
                is ScriptNode.Label -> advance(current)
                is ScriptNode.CommandNode -> {
                    if (executeTopLevel(node.command, current)) return
                }
            }
        }
        mutableState.value = mutableState.value.copy(
            dialogue = null,
            pendingChoices = emptyList(),
            status = RuntimeStatus.Completed,
        )
    }

    private suspend fun executeTopLevel(command: Command, current: GameState): Boolean = when (command) {
        is Command.Say -> {
            val dialogue = Dialogue(command.speaker, command.text)
            mutableState.value = current.copy(
                position = current.position.next(),
                dialogue = dialogue,
                history = current.history + dialogue,
                status = RuntimeStatus.WaitingForInput,
            )
            true
        }
        is Command.Choice -> {
            mutableState.value = current.copy(
                position = current.position.next(),
                dialogue = null,
                pendingChoices = command.choices,
                status = RuntimeStatus.WaitingForChoice,
            )
            true
        }
        else -> {
            logger.debug("script=${script.id} node=${current.position.nodeIndex} command=${command::class.simpleName}")
            executeImmediate(command, current)
            if (command.producesVisualEffect()) {
                mutableState.value = mutableState.value.copy(status = RuntimeStatus.WaitingForEffect)
                true
            } else {
                false
            }
        }
    }

    private suspend fun executeImmediate(command: Command, current: GameState) {
        mutableState.value = when (command) {
            is Command.ChangeBackground -> current.copy(
                position = current.position.next(),
                background = BackgroundState(command.backgroundId),
                status = RuntimeStatus.Running,
            )
            is Command.ShowCharacter -> current.copy(
                position = current.position.next(),
                characters = current.characters.replaceById(
                    CharacterState(command.characterId, command.expression, command.position),
                ),
                status = RuntimeStatus.Running,
            )
            is Command.HideCharacter -> current.copy(
                position = current.position.next(),
                characters = current.characters.filterNot { it.characterId == command.characterId },
                status = RuntimeStatus.Running,
            )
            is Command.Jump -> current.copy(
                position = ScriptPosition(resolveLabel(command.label)),
                status = RuntimeStatus.Running,
            )
            is Command.SetVariable -> current.copy(
                position = current.position.next(),
                variables = current.variables.put(command.name, command.value),
                status = RuntimeStatus.Running,
            )
            is Command.If -> {
                mutableState.value = current.copy(
                    position = current.position.next(),
                    status = RuntimeStatus.Running,
                )
                val branch = if (command.condition.evaluate(current.variables)) {
                    command.thenCommands
                } else {
                    command.elseCommands
                }
                branch.forEach(::executeImmediateBranchCommand)
                mutableState.value
            }
            is Command.PlayBgm -> {
                audioPlayer.playBgm(command.audioId, command.loop)
                current.copy(
                    position = current.position.next(),
                    audio = current.audio.copy(bgmId = command.audioId, bgmLoop = command.loop),
                    status = RuntimeStatus.Running,
                )
            }
            is Command.StopBgm -> {
                audioPlayer.stopBgm(command.fadeOutMillis)
                current.copy(
                    position = current.position.next(),
                    audio = current.audio.copy(bgmId = null),
                    status = RuntimeStatus.Running,
                )
            }
            is Command.PlaySe -> {
                audioPlayer.playSe(command.audioId)
                current.copy(position = current.position.next(), status = RuntimeStatus.Running)
            }
            is Command.PlayVoice -> {
                audioPlayer.playVoice(command.audioId)
                current.copy(
                    position = current.position.next(),
                    audio = current.audio.copy(voiceId = command.audioId),
                    status = RuntimeStatus.Running,
                )
            }
            Command.StopVoice -> {
                audioPlayer.stopVoice()
                current.copy(
                    position = current.position.next(),
                    audio = current.audio.copy(voiceId = null),
                    status = RuntimeStatus.Running,
                )
            }
            is Command.ShowCg -> current.copy(
                position = current.position.next(),
                cgId = command.cgId,
                visualEffect = current.nextEffect(
                    VisualEffect.Transition(command.transition, command.durationMillis),
                ),
                status = RuntimeStatus.Running,
            )
            Command.HideCg -> current.copy(
                position = current.position.next(),
                cgId = null,
                status = RuntimeStatus.Running,
            )
            is Command.Transition -> current.copy(
                position = current.position.next(),
                visualEffect = current.nextEffect(VisualEffect.Transition(command.type, command.durationMillis)),
                status = RuntimeStatus.Running,
            )
            is Command.Shake -> current.copy(
                position = current.position.next(),
                visualEffect = current.nextEffect(VisualEffect.Shake(command.durationMillis, command.intensity)),
                status = RuntimeStatus.Running,
            )
            is Command.MoveCharacter -> {
                if (current.characters.none { it.characterId == command.characterId }) {
                    invalidAction("cannot move hidden character: ${command.characterId}")
                }
                current.copy(
                    position = current.position.next(),
                    characters = current.characters.map {
                        if (it.characterId == command.characterId) it.copy(position = command.position) else it
                    },
                    visualEffect = current.nextEffect(
                        VisualEffect.CharacterMove(command.characterId, command.durationMillis),
                    ),
                    status = RuntimeStatus.Running,
                )
            }
            is Command.Wait -> {
                wait(command.durationMillis)
                current.copy(position = current.position.next(), status = RuntimeStatus.Running)
            }
            is Command.Say, is Command.Choice -> invalidAction("pause command cannot execute immediately")
        }
    }

    private fun executeImmediateChoiceCommand(command: Command) {
        executeImmediateBranchCommand(command)
    }

    private fun executeImmediateBranchCommand(command: Command) {
        val current = mutableState.value
        mutableState.value = when (command) {
            is Command.Jump -> current.copy(position = ScriptPosition(resolveLabel(command.label)))
            is Command.SetVariable -> current.copy(variables = current.variables.put(command.name, command.value))
            else -> invalidAction("unsupported command in choice branch: ${command::class.simpleName}")
        }
    }

    private fun advance(current: GameState) {
        mutableState.value = current.copy(
            position = current.position.next(),
            status = RuntimeStatus.Running,
        )
    }

    private fun resolveLabel(label: String): Int = script.labels[label]
        ?: throw EngineException.UnknownLabel("script: ${script.id}, label: $label: unknown label")

    private fun invalidAction(detail: String): Nothing = throw EngineException.InvalidAction(
        "script: ${script.id}, position: ${mutableState.value.position.nodeIndex}: $detail",
    )

    private fun ScriptPosition.next() = ScriptPosition(nodeIndex + 1)

    private fun List<CharacterState>.replaceById(character: CharacterState): List<CharacterState> =
        filterNot { it.characterId == character.characterId } + character

    private suspend fun restoreAudio(state: GameState) {
        audioPlayer.stopBgm()
        state.audio.bgmId?.let { audioPlayer.playBgm(it, state.audio.bgmLoop) }
        audioPlayer.stopVoice()
        state.audio.voiceId?.let { audioPlayer.playVoice(it) }
    }

    private fun GameState.nextEffect(effect: VisualEffect): VisualEffectState = VisualEffectState(
        sequence = (visualEffect?.sequence ?: 0L) + 1L,
        effect = effect,
    )

    private fun Command.producesVisualEffect(): Boolean = when (this) {
        is Command.ShowCg, is Command.Transition, is Command.Shake, is Command.MoveCharacter -> true
        else -> false
    }

    private fun GameState.validateFor(script: Script): GameState {
        require(scriptId == script.id) { "Initial state script id '$scriptId' does not match '${script.id}'." }
        require(position.nodeIndex <= script.nodes.size) { "Initial script position is outside the script." }
        return this
    }
}
