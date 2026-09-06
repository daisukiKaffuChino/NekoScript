package io.github.daisukikaffuchino.nekoscript.engine.asset

/** Resolves project-defined logical asset identifiers without rendering them. */
interface AssetManager {
    /** Resolves a background by logical [id]. */
    suspend fun loadBackground(id: String): Asset

    /** Resolves a character expression by logical identifiers. */
    suspend fun loadCharacter(characterId: String, expression: String?): Asset

    /** Resolves a CG by logical [id]. */
    suspend fun loadCg(id: String): Asset
}
