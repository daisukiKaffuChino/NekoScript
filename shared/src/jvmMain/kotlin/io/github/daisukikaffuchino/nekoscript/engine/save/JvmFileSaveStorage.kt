package io.github.daisukikaffuchino.nekoscript.engine.save

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Persists each save slot as a UTF-8 JSON file with atomic replacement when supported. */
class JvmFileSaveStorage(
    private val directory: Path,
) : SaveStorage {
    override suspend fun read(slot: String): String? = withContext(Dispatchers.IO) {
        val path = pathFor(slot)
        if (Files.notExists(path)) null else Files.readString(path, StandardCharsets.UTF_8)
    }

    override suspend fun write(slot: String, data: String) = withContext(Dispatchers.IO) {
        Files.createDirectories(directory)
        val destination = pathFor(slot)
        val temporary = Files.createTempFile(directory, "$slot-", ".tmp")
        try {
            Files.writeString(temporary, data, StandardCharsets.UTF_8)
            try {
                Files.move(
                    temporary,
                    destination,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
        Unit
    }

    override suspend fun delete(slot: String) = withContext(Dispatchers.IO) {
        Files.deleteIfExists(pathFor(slot))
        Unit
    }

    private fun pathFor(slot: String): Path {
        require(SLOT_PATTERN.matches(slot)) { "Invalid save slot name." }
        return directory.resolve("$slot.json")
    }

    private companion object {
        val SLOT_PATTERN = Regex("[A-Za-z0-9_-]{1,64}")
    }
}
