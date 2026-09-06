package io.github.daisukikaffuchino.nekoscript.engine.asset

/** Opaque asset reference resolved by a project-specific [AssetManager]. */
data class Asset(
    val id: String,
    val location: String,
) {
    init {
        require(id.isNotBlank()) { "Asset id must not be blank." }
        require(location.isNotBlank()) { "Asset location must not be blank." }
    }
}
