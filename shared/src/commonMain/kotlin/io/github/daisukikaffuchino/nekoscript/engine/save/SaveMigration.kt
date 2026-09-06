package io.github.daisukikaffuchino.nekoscript.engine.save

import kotlinx.serialization.json.JsonObject

/** One adjacent schema migration applied before deserializing [SaveData]. */
interface SaveMigration {
    /** Schema version accepted by this migration. */
    val fromVersion: Int

    /** Schema version emitted by this migration. Must equal `fromVersion + 1`. */
    val toVersion: Int

    /** Migrates the complete save envelope represented by [data]. */
    fun migrate(data: JsonObject): JsonObject
}
