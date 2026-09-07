package io.github.daisukikaffuchino.nekoscript.engine.audio

import io.github.daisukikaffuchino.nekoscript.engine.asset.Asset
import io.github.daisukikaffuchino.nekoscript.engine.asset.AssetManager
import io.github.daisukikaffuchino.nekoscript.engine.error.EngineException
import io.github.daisukikaffuchino.nekoscript.engine.project.GameProjectSource
import korlibs.audio.format.AudioDecodingProps
import korlibs.audio.format.AudioFormats
import korlibs.audio.format.MP3Decoder
import korlibs.audio.format.WAV
import korlibs.audio.sound.PlaybackParameters
import korlibs.audio.sound.Sound
import korlibs.audio.sound.SoundChannel
import korlibs.audio.sound.infinitePlaybackTimes
import korlibs.audio.sound.playbackTimes
import korlibs.audio.sound.playingOrPaused
import korlibs.audio.sound.readSound
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.math.min

/** Cross-platform [AudioPlayer] backed by the independently published Korlibs audio module. */
class KorlibsAudioPlayer internal constructor(
    private val assetManager: AssetManager,
    private val source: GameProjectSource,
    private val backend: AudioBackend,
    private val scope: CoroutineScope,
    private val fadeStepMillis: Long,
) : AudioPlayer {
    constructor(
        assetManager: AssetManager,
        source: GameProjectSource,
    ) : this(
        assetManager = assetManager,
        source = source,
        backend = KorlibsAudioBackend,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        fadeStepMillis = DEFAULT_FADE_STEP_MILLIS,
    )

    private var bgmChannel: AudioChannel? = null
    private var voiceChannel: AudioChannel? = null
    private val seChannels = mutableListOf<AudioChannel>()
    private val seCache = mutableMapOf<String, AudioClip>()
    private var fadeJob: Job? = null
    private var released = false

    init {
        require(fadeStepMillis > 0) { "Fade step must be positive." }
    }

    override suspend fun playBgm(id: String, loop: Boolean) {
        ensureUsable()
        cancelFade()
        bgmChannel?.stop()
        bgmChannel = null

        val asset = assetManager.loadBgm(id)
        val clip = loadClip("BGM", asset, streaming = true)
        bgmChannel = clip.play(scope.coroutineContext, loop)
    }

    override suspend fun stopBgm(fadeOutMillis: Long) {
        ensureUsable()
        require(fadeOutMillis >= 0) { "BGM fade duration must not be negative." }
        cancelFade()
        val channel = bgmChannel ?: return
        if (fadeOutMillis == 0L) {
            channel.stop()
            bgmChannel = null
            return
        }

        val startingVolume = channel.volume
        fadeJob = scope.launch {
            var elapsed = 0L
            while (elapsed < fadeOutMillis) {
                val step = min(fadeStepMillis, fadeOutMillis - elapsed)
                delay(step)
                elapsed += step
                channel.volume = startingVolume * (1.0 - elapsed.toDouble() / fadeOutMillis)
            }
            channel.stop()
            if (bgmChannel === channel) bgmChannel = null
        }
    }

    override suspend fun playSe(id: String) {
        ensureUsable()
        seChannels.removeAll { !it.isPlaying }
        val clip = seCache[id] ?: run {
            val asset = assetManager.loadSe(id)
            loadClip("sound effect", asset, streaming = false).also { seCache[id] = it }
        }
        seChannels += clip.play(scope.coroutineContext, loop = false)
    }

    override suspend fun playVoice(id: String) {
        ensureUsable()
        voiceChannel?.stop()
        voiceChannel = null
        val asset = assetManager.loadVoice(id)
        val clip = loadClip("voice", asset, streaming = false)
        voiceChannel = clip.play(scope.coroutineContext, loop = false)
    }

    override suspend fun stopVoice() {
        ensureUsable()
        voiceChannel?.stop()
        voiceChannel = null
    }

    override fun release() {
        if (released) return
        released = true
        cancelFade()
        bgmChannel?.stop()
        voiceChannel?.stop()
        seChannels.forEach(AudioChannel::stop)
        bgmChannel = null
        voiceChannel = null
        seChannels.clear()
        seCache.clear()
        scope.cancel()
    }

    private suspend fun loadClip(type: String, asset: Asset, streaming: Boolean): AudioClip {
        ensureSupportedFormat(type, asset)
        return try {
            val bytes = source.readBytes(asset.location)
            if (bytes.isEmpty()) error("audio data is empty")
            backend.load(bytes, streaming, asset.location)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (error is EngineException.AssetLoadError) throw error
            throw EngineException.AssetLoadError(
                assetType = type,
                assetId = asset.id,
                location = asset.location,
                message = "$type asset '${asset.id}' at '${asset.location}' failed to load or decode",
                cause = error,
            )
        }
    }

    private fun ensureSupportedFormat(type: String, asset: Asset) {
        val extension = asset.location.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        if (extension !in SUPPORTED_EXTENSIONS) {
            throw EngineException.AssetLoadError(
                assetType = type,
                assetId = asset.id,
                location = asset.location,
                message = "$type asset '${asset.id}' at '${asset.location}' uses unsupported format " +
                    "'${extension.ifBlank { "unknown" }}'; supported formats are MP3 and WAV",
            )
        }
    }

    private fun cancelFade() {
        fadeJob?.cancel()
        fadeJob = null
    }

    private fun ensureUsable() {
        check(!released) { "Audio player has been released." }
    }

    private companion object {
        const val DEFAULT_FADE_STEP_MILLIS = 16L
        val SUPPORTED_EXTENSIONS = setOf("mp3", "wav")
    }
}

internal interface AudioBackend {
    suspend fun load(data: ByteArray, streaming: Boolean, name: String): AudioClip
}

internal interface AudioClip {
    fun play(context: CoroutineContext, loop: Boolean): AudioChannel
}

internal interface AudioChannel {
    var volume: Double
    val isPlaying: Boolean
    fun stop()
}

private object KorlibsAudioBackend : AudioBackend {
    private val formats = AudioFormats(WAV, MP3Decoder)

    override suspend fun load(data: ByteArray, streaming: Boolean, name: String): AudioClip {
        val sound = data.readSound(
            props = AudioDecodingProps(formats = formats),
            streaming = streaming,
        )
        return KorlibsAudioClip(sound)
    }
}

private class KorlibsAudioClip(
    private val sound: Sound,
) : AudioClip {
    override fun play(context: CoroutineContext, loop: Boolean): AudioChannel {
        val times = if (loop) infinitePlaybackTimes else 1.playbackTimes
        val channel = sound.play(context, PlaybackParameters(times = times))
        return KorlibsAudioChannel(channel)
    }
}

private class KorlibsAudioChannel(
    private val channel: SoundChannel,
) : AudioChannel {
    override var volume: Double
        get() = channel.volume
        set(value) {
            channel.volume = value
        }

    override val isPlaying: Boolean
        get() = channel.playingOrPaused

    override fun stop() = channel.stop()
}
