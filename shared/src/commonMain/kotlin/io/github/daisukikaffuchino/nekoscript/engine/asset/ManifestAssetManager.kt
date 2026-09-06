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

    private fun resolve(type: String, id: String, location: String?): Asset =
        location?.let { Asset(id, it) } ?: notFound(type, id)

    private fun notFound(type: String, id: String): Nothing = throw EngineException.AssetNotFound(
        "project: ${project.id}, asset type: $type, id: $id: asset not found",
    )
}
