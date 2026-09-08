package io.github.daisukikaffuchino.nekoscript.engine.audio

import io.github.daisukikaffuchino.nekoscript.engine.asset.ManifestAssetManager
import io.github.daisukikaffuchino.nekoscript.engine.error.EngineException
import io.github.daisukikaffuchino.nekoscript.engine.project.AudioManifest
import io.github.daisukikaffuchino.nekoscript.engine.project.GameProject
import io.github.daisukikaffuchino.nekoscript.engine.project.MapGameProjectSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class KorlibsAudioPlayerTest {
    @Test
    fun replacesBgmAndPreservesRequestedLoopMode() = runTest {
        val backend = FakeAudioBackend()
        val player = player(backend, this)

        player.playBgm("theme", loop = true)
        val first = backend.clips.single().channels.single()
        player.playBgm("alternate", loop = false)

        assertTrue(first.stopped)
        assertEquals(listOf(true, false), backend.clips.flatMap { clip -> clip.channels.map { it.loop } })
        assertTrue(backend.loads.all { it.streaming })
    }

    @Test
    fun cachesDecodedSeAndAllowsConcurrentChannels() = runTest {
        val backend = FakeAudioBackend()
        val player = player(backend, this)

        player.playSe("bell")
        player.playSe("bell")

        assertEquals(1, backend.loads.count { it.name.endsWith("bell.mp3") })
        assertEquals(2, backend.clips.single().channels.size)
        assertTrue(backend.loads.none { it.streaming })
        assertTrue(backend.clips.single().channels.none { it.stopped })
    }

    @Test
    fun replacesAndStopsVoiceChannel() = runTest {
        val backend = FakeAudioBackend()
        val player = player(backend, this)

        player.playVoice("line")
        val first = backend.clips.single().channels.single()
        player.playVoice("line2")
        val second = backend.clips.last().channels.single()
        player.stopVoice()

        assertTrue(first.stopped)
        assertTrue(second.stopped)
        assertTrue(backend.loads.none { it.streaming })
    }

    @Test
    fun fadesBgmLinearlyBeforeStopping() = runTest {
        val backend = FakeAudioBackend()
        val player = player(backend, this, fadeStepMillis = 25)
        player.playBgm("theme", loop = true)
        val channel = backend.clips.single().channels.single()

        player.stopBgm(100)
        runCurrent()
        assertFalse(channel.stopped)
        advanceTimeBy(50)
        runCurrent()
        assertEquals(0.5, channel.volume, absoluteTolerance = 0.001)
        advanceUntilIdle()

        assertTrue(channel.stopped)
        assertEquals(0.0, channel.volume, absoluteTolerance = 0.001)
    }

    @Test
    fun releaseCancelsOwnedPlaybackAndRejectsFurtherUse() = runTest {
        val backend = FakeAudioBackend()
        val player = player(backend, this)
        player.playBgm("theme", loop = true)
        player.playSe("bell")
        player.playSe("bell")
        player.playVoice("line")

        player.release()
        player.release()

        assertTrue(backend.clips.flatMap { it.channels }.all { it.stopped })
        assertFailsWith<IllegalStateException> { player.playSe("bell") }
    }

    @Test
    fun reportsUnsupportedMissingAndUnknownAudioAssets() = runTest {
        val backend = FakeAudioBackend()
        val oggProject = project().copy(audio = AudioManifest(bgm = mapOf("theme" to "audio/theme.ogg")))
        val oggPlayer = player(backend, this, oggProject)
        val unsupported = assertFailsWith<EngineException.AssetLoadError> {
            oggPlayer.playBgm("theme")
        }
        assertEquals("BGM", unsupported.assetType)
        assertEquals("theme", unsupported.assetId)
        assertEquals("audio/theme.ogg", unsupported.location)
        assertTrue(unsupported.message.orEmpty().contains("supported formats are MP3 and WAV"))

        val missingPlayer = KorlibsAudioPlayer(
            assetManager = ManifestAssetManager(project()),
            source = MapGameProjectSource(emptyMap()),
            backend = backend,
            scope = this,
            fadeStepMillis = 10,
        )
        val missing = assertFailsWith<EngineException.AssetLoadError> { missingPlayer.playSe("bell") }
        assertEquals("audio/bell.mp3", missing.location)
        assertTrue(missing.cause is IllegalArgumentException)

        assertFailsWith<EngineException.AssetNotFound> { missingPlayer.playSe("unknown") }
    }

    private fun player(
        backend: FakeAudioBackend,
        scope: CoroutineScope,
        project: GameProject = project(),
        fadeStepMillis: Long = 10,
    ): KorlibsAudioPlayer = KorlibsAudioPlayer(
        assetManager = ManifestAssetManager(project),
        source = MapGameProjectSource(
            files = emptyMap(),
            binaryFiles = project.audio.allLocations().associateWith { byteArrayOf(1, 2, 3) },
        ),
        backend = backend,
        scope = CoroutineScope(scope.coroutineContext + SupervisorJob()),
        fadeStepMillis = fadeStepMillis,
    )

    private fun project(): GameProject = GameProject(
        id = "audio-test",
        name = "Audio Test",
        version = "1",
        entryScript = "main.avg",
        viewport = io.github.daisukikaffuchino.nekoscript.engine.project.ViewportConfig(),
        audio = AudioManifest(
            bgm = mapOf("theme" to "audio/theme.mp3", "alternate" to "audio/alternate.mp3"),
            se = mapOf("bell" to "audio/bell.mp3"),
            voice = mapOf("line" to "audio/line.mp3", "line2" to "audio/line2.wav"),
        ),
    )

    private fun AudioManifest.allLocations(): List<String> = bgm.values + se.values + voice.values

    private data class Load(val data: ByteArray, val streaming: Boolean, val name: String)

    private class FakeAudioBackend : AudioBackend {
        val loads = mutableListOf<Load>()
        val clips = mutableListOf<FakeAudioClip>()

        override suspend fun load(data: ByteArray, streaming: Boolean, name: String): AudioClip {
            loads += Load(data, streaming, name)
            return FakeAudioClip().also(clips::add)
        }
    }

    private class FakeAudioClip : AudioClip {
        val channels = mutableListOf<FakeAudioChannel>()

        override fun play(context: CoroutineContext, loop: Boolean): AudioChannel =
            FakeAudioChannel(loop).also(channels::add)
    }

    private class FakeAudioChannel(val loop: Boolean) : AudioChannel {
        override var volume: Double = 1.0
        var stopped = false
        override val isPlaying: Boolean get() = !stopped
        override fun stop() {
            stopped = true
        }
    }
}
