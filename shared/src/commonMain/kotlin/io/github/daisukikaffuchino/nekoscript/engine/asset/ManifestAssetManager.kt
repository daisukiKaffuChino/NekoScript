package io.github.daisukikaffuchino.nekoscript.engine.asset

import io.github.daisukikaffuchino.nekoscript.engine.error.EngineException
import io.github.daisukikaffuchino.nekoscript.engine.project.GameProject

/** Resolves assets from mappings declared by a [GameProject]. */
class ManifestAssetManager(
    private val project: GameProject,
) : AssetManager {
    override suspend fun loadBackground(id: String): Asset = resolve("background", id, project.backgrounds[id])

    override suspend fun loadCharacter(characterId: String, expression: String?): Asset {
        val character = project.characters[characterId]
            ?: notFound("character", characterId)
        val expressionId = expression ?: character.defaultExpression
        return resolve(
            "character expression",
            "$characterId:$expressionId",
            character.expressions[expressionId],
        )
    }

    override suspend fun loadCg(id: String): Asset = resolve("CG", id, project.cg[id])

    override suspend fun loadBgm(id: String): Asset = resolve("BGM", id, project.audio.bgm[id])

    override suspend fun loadSe(id: String): Asset = resolve("sound effect", id, project.audio.se[id])

    override suspend fun loadVoice(id: String): Asset = resolve("voice", id, project.audio.voice[id])

    private fun resolve(type: String, id: String, location: String?): Asset =
        location?.let { Asset(id, it) } ?: notFound(type, id)

    private fun notFound(type: String, id: String): Nothing = throw EngineException.AssetNotFound(
        assetType = type,
        assetId = id,
        location = "<manifest>",
        message = "project: ${project.id}, asset type: $type, id: $id: asset not found",
    )
}
