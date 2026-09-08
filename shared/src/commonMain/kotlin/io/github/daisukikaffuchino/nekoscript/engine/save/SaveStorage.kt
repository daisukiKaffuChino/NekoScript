package io.github.daisukikaffuchino.nekoscript.engine.save

/** Platform persistence boundary used by [SaveManager]. */
interface SaveStorage {
    /** Returns the raw JSON stored in [slot], or `null` when the slot is empty. */
    suspend fun read(slot: String): String?

    /** Durably replaces [slot] with raw save [data]. */
    suspend fun write(slot: String, data: String)

    /** Removes [slot] from persistence, if present. */
    suspend fun delete(slot: String)
}
