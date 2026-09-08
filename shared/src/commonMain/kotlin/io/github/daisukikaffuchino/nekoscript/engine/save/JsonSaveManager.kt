package io.github.daisukikaffuchino.nekoscript.engine.save

import io.github.daisukikaffuchino.nekoscript.engine.error.EngineException
import io.github.daisukikaffuchino.nekoscript.engine.runtime.GameState
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * JSON [SaveManager] with an explicit adjacent-version migration chain.
 *
 * @param storage platform-specific raw data store
 * @param currentVersion schema version written by this manager
 * @param migrations migrations keyed by their source version
 * @param timestampProvider platform-provided epoch millisecond clock
 */
class JsonSaveManager(
    private val storage: SaveStorage,
    private val currentVersion: Int = CURRENT_SAVE_VERSION,
    migrations: List<SaveMigration> = emptyList(),
    private val timestampProvider: () -> Long,
) : SaveManager {
    private val migrationsByVersion = migrations.associateBy(SaveMigration::fromVersion)
    private val mutex = Mutex()
    private val json = Json {
        classDiscriminator = "type"
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    init {
        require(currentVersion > 0) { "Current save version must be positive." }
        require(migrationsByVersion.size == migrations.size) { "Duplicate save migration source version." }
        migrations.forEach {
            require(it.toVersion == it.fromVersion + 1) { "Save migrations must advance exactly one version." }
        }
    }

    override suspend fun save(slot: String, state: GameState): SaveData = mutex.withLock {
        validateSlot(slot)
        val save = SaveData(currentVersion, timestampProvider(), state)
        try {
            storage.write(slot, json.encodeToString(save))
        } catch (error: EngineException.SaveError) {
            throw error
        } catch (error: Exception) {
            throw EngineException.SaveError("slot: $slot: failed to write save", error)
        }
        save
    }

    override suspend fun load(slot: String): SaveData? = mutex.withLock {
        validateSlot(slot)
        val raw = try {
            storage.read(slot)
        } catch (error: EngineException.SaveError) {
            throw error
        } catch (error: Exception) {
            throw EngineException.SaveError("slot: $slot: failed to read save", error)
        } ?: return@withLock null

        try {
            var data = json.parseToJsonElement(raw) as? JsonObject
                ?: saveError(slot, "save root must be a JSON object")
            var version = data["version"]?.jsonPrimitive?.intOrNull
                ?: saveError(slot, "missing or invalid save version")
            if (version > currentVersion) {
                saveError(slot, "save version $version is newer than supported version $currentVersion")
            }
            while (version < currentVersion) {
                val migration = migrationsByVersion[version]
                    ?: saveError(slot, "missing migration from save version $version")
                data = migration.migrate(data)
                val migratedVersion = data["version"]?.jsonPrimitive?.intOrNull
                    ?: saveError(slot, "migration from version $version did not emit a version")
                if (migratedVersion != migration.toVersion) {
                    saveError(
                        slot,
                        "migration from version $version emitted version $migratedVersion instead of ${migration.toVersion}",
                    )
                }
                version = migratedVersion
            }
            json.decodeFromJsonElement(SaveData.serializer(), data)
        } catch (error: EngineException.SaveError) {
            throw error
        } catch (error: SerializationException) {
            throw EngineException.SaveError("slot: $slot: malformed save data", error)
        } catch (error: IllegalArgumentException) {
            throw EngineException.SaveError("slot: $slot: invalid save data", error)
        }
    }

    override suspend fun delete(slot: String) = mutex.withLock {
        validateSlot(slot)
        try {
            storage.delete(slot)
        } catch (error: EngineException.SaveError) {
            throw error
        } catch (error: Exception) {
            throw EngineException.SaveError("slot: $slot: failed to delete save", error)
        }
    }

    private fun validateSlot(slot: String) {
        if (!SLOT_PATTERN.matches(slot)) saveError(slot, "invalid save slot name")
    }

    private fun saveError(slot: String, detail: String): Nothing =
        throw EngineException.SaveError("slot: $slot: $detail")

    /** Current schema version emitted by the engine. */
    companion object {
        const val CURRENT_SAVE_VERSION: Int = 1
        private val SLOT_PATTERN = Regex("[A-Za-z0-9_-]{1,64}")
    }
}
