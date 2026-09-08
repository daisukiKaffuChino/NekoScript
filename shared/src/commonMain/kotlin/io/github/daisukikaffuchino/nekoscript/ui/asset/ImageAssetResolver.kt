package io.github.daisukikaffuchino.nekoscript.ui.asset

import io.github.daisukikaffuchino.nekoscript.engine.asset.Asset
import io.github.daisukikaffuchino.nekoscript.engine.asset.AssetLoadMonitor
import io.github.daisukikaffuchino.nekoscript.engine.asset.AssetManager
import io.github.daisukikaffuchino.nekoscript.engine.error.EngineException
import io.github.daisukikaffuchino.nekoscript.engine.project.GameProjectSource
import kotlinx.coroutines.CancellationException

/** Compressed image data for the current presentation request and its stable Coil cache identity. */
class ResolvedImageAsset(
    val assetId: String,
    val data: ByteArray,
    val cacheKey: String,
)

/** Resolves engine-level logical asset IDs without exposing image APIs to Engine Core. */
interface ImageAssetResolver {
    suspend fun resolveBackground(assetId: String): ResolvedImageAsset

    suspend fun resolveCharacter(characterId: String, expression: String?): ResolvedImageAsset

    suspend fun resolveCg(assetId: String): ResolvedImageAsset
}

/** Reads manifest asset locations from packaged Compose resources. */
class ComposeResourceImageAssetResolver(
    private val assetManager: AssetManager,
    private val source: GameProjectSource,
    private val assetLoadMonitor: AssetLoadMonitor? = null,
) : ImageAssetResolver {
    override suspend fun resolveBackground(assetId: String): ResolvedImageAsset =
        resolve("background", assetManager.loadBackground(assetId))

    override suspend fun resolveCharacter(characterId: String, expression: String?): ResolvedImageAsset =
        resolve("character", assetManager.loadCharacter(characterId, expression))

    override suspend fun resolveCg(assetId: String): ResolvedImageAsset =
        resolve("cg", assetManager.loadCg(assetId))

    private suspend fun resolve(kind: String, asset: Asset): ResolvedImageAsset {
        val data = try {
            source.readBytes(asset.location)
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            val loadError = EngineException.AssetLoadError(
                assetType = kind,
                assetId = asset.id,
                location = asset.location,
                message = "$kind asset '${asset.id}' at '${asset.location}' failed to load",
                cause = error,
            )
            assetLoadMonitor?.report(loadError)
            throw loadError
        }
        assetLoadMonitor?.markLoaded(kind, asset.id, asset.location)
        return ResolvedImageAsset(
            assetId = asset.id,
            data = data,
            cacheKey = "nekoscript:$kind:${asset.id}:${asset.location}",
        )
    }
}
