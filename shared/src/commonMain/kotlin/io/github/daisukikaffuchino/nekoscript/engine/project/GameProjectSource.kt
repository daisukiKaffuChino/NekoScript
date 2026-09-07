package io.github.daisukikaffuchino.nekoscript.engine.project

/** Platform or host boundary for reading UTF-8 project text files. */
fun interface GameProjectSource {
    /** Reads the project-relative text file at [location]. */
    suspend fun readText(location: String): String

    /** Reads an opaque project-relative binary file. */
    suspend fun readBytes(location: String): ByteArray = readText(location).encodeToByteArray()
}

/** In-memory project source for embedded games, previews, and tests. */
class MapGameProjectSource(
    private val files: Map<String, String>,
    private val binaryFiles: Map<String, ByteArray> = emptyMap(),
) : GameProjectSource {
    override suspend fun readText(location: String): String = files[location]
        ?: binaryFiles[location]?.decodeToString()
        ?: throw IllegalArgumentException("Project file not found: $location")

    override suspend fun readBytes(location: String): ByteArray = binaryFiles[location]?.copyOf()
        ?: files[location]?.encodeToByteArray()
        ?: throw IllegalArgumentException("Project file not found: $location")
}
