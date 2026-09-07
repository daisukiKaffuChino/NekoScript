package io.github.daisukikaffuchino.nekoscript.ui.asset

import io.github.daisukikaffuchino.nekoscript.composeResourcePath
import io.github.daisukikaffuchino.nekoscript.engine.asset.Asset
import io.github.daisukikaffuchino.nekoscript.engine.asset.AssetManager
import nekoscript.shared.generated.resources.Res

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
    private val root: String = "files",
) : ImageAssetResolver {
    override suspend fun resolveBackground(assetId: String): ResolvedImageAsset =
        resolve("background", assetManager.loadBackground(assetId))

    override suspend fun resolveCharacter(characterId: String, expression: String?): ResolvedImageAsset =
        resolve("character", assetManager.loadCharacter(characterId, expression))

    override suspend fun resolveCg(assetId: String): ResolvedImageAsset =
        resolve("cg", assetManager.loadCg(assetId))

    private suspend fun resolve(kind: String, asset: Asset): ResolvedImageAsset {
        val resourcePath = composeResourcePath(root, asset.location)
        return ResolvedImageAsset(
            assetId = asset.id,
            data = Res.readBytes(resourcePath),
            cacheKey = "nekoscript:$kind:${asset.id}:$resourcePath",
        )
    }
}
