package io.github.daisukikaffuchino.nekoscript.ui.asset

import io.github.daisukikaffuchino.nekoscript.engine.asset.ManifestAssetManager
import io.github.daisukikaffuchino.nekoscript.engine.asset.AssetLoadIssue
import io.github.daisukikaffuchino.nekoscript.engine.asset.AssetLoadMonitor
import io.github.daisukikaffuchino.nekoscript.engine.error.EngineException
import io.github.daisukikaffuchino.nekoscript.engine.project.GameProject
import io.github.daisukikaffuchino.nekoscript.engine.project.MapGameProjectSource
import io.github.daisukikaffuchino.nekoscript.engine.project.ViewportConfig
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ImageAssetResolverTest {
    @Test
    fun reportsLogicalIdentityAndLocationWhenImageReadingFails() = runTest {
        val project = GameProject(
            id = "test",
            name = "Test",
            version = "1",
            entryScript = "main.avg",
            viewport = ViewportConfig(),
            backgrounds = mapOf("school" to "backgrounds/missing.jpg"),
        )
        val monitor = AssetLoadMonitor()
        val resolver = ComposeResourceImageAssetResolver(
            ManifestAssetManager(project),
            MapGameProjectSource(emptyMap()),
            monitor,
        )

        val error = assertFailsWith<EngineException.AssetLoadError> {
            resolver.resolveBackground("school")
        }

        assertEquals("background", error.assetType)
        assertEquals("school", error.assetId)
        assertEquals("backgrounds/missing.jpg", error.location)
        assertTrue(error.cause is IllegalArgumentException)
        assertEquals(
            listOf(AssetLoadIssue("background", "school", "backgrounds/missing.jpg")),
            monitor.issues.value,
        )
    }
}
