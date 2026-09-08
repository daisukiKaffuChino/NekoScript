package io.github.daisukikaffuchino.nekoscript.engine.save

/** Volatile save storage intended for previews, tests, and embedded sessions. */
class InMemorySaveStorage : SaveStorage {
    private val slots = mutableMapOf<String, String>()

    override suspend fun read(slot: String): String? = slots[slot]

    override suspend fun write(slot: String, data: String) {
        slots[slot] = data
    }

    override suspend fun delete(slot: String) {
        slots.remove(slot)
    }
}
