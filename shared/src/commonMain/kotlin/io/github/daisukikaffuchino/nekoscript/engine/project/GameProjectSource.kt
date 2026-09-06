package io.github.daisukikaffuchino.nekoscript.engine.project

/** Platform or host boundary for reading UTF-8 project text files. */
fun interface GameProjectSource {
    /** Reads the project-relative text file at [location]. */
    suspend fun readText(location: String): String
}

/** In-memory project source for embedded games, previews, and tests. */
class MapGameProjectSource(
    private val files: Map<String, String>,
) : GameProjectSource {
    override suspend fun readText(location: String): String = files[location]
        ?: throw IllegalArgumentException("Project file not found: $location")
}
