package io.github.daisukikaffuchino.nekoscript.engine.save

import io.github.daisukikaffuchino.nekoscript.engine.error.EngineException
import io.github.daisukikaffuchino.nekoscript.engine.runtime.GameState
import io.github.daisukikaffuchino.nekoscript.engine.runtime.RuntimeStatus
import io.github.daisukikaffuchino.nekoscript.engine.script.ScriptPosition
import io.github.daisukikaffuchino.nekoscript.engine.variable.Variable
import io.github.daisukikaffuchino.nekoscript.engine.variable.VariableStore
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JsonSaveManagerTest {
    @Test
    fun savesAndLoadsCompleteVersionedState() = runTest {
        val storage = FakeStorage()
        val manager = JsonSaveManager(storage, timestampProvider = { 123_456L })
        val state = GameState(
            scriptId = "main.avg",
            position = ScriptPosition(9),
            variables = VariableStore().put("affection.yuki", Variable.IntValue(4)),
            status = RuntimeStatus.WaitingForInput,
        )

        val saved = manager.save("slot-1", state)
        val loaded = manager.load("slot-1")

        assertEquals(1, saved.version)
        assertEquals(123_456L, saved.timestamp)
        assertEquals(saved, loaded)
        assertTrue(storage.values.getValue("slot-1").contains("\"version\":1"))
        assertNull(manager.load("empty"))
    }

    @Test
    fun deletesSavedSlots() = runTest {
        val storage = FakeStorage()
        val manager = JsonSaveManager(storage, timestampProvider = { 0L })
        manager.save("slot-1", GameState("main.avg"))

        manager.delete("slot-1")

        assertNull(storage.values["slot-1"])
        assertNull(manager.load("slot-1"))
    }

    @Test
    fun appliesEveryAdjacentMigrationBeforeDecoding() = runTest {
        val storage = FakeStorage()
        JsonSaveManager(storage, currentVersion = 1, timestampProvider = { 10L })
            .save("quick", GameState("main.avg"))
        val manager = JsonSaveManager(
            storage = storage,
            currentVersion = 3,
            migrations = listOf(VersionMigration(1), VersionMigration(2)),
            timestampProvider = { 20L },
        )

        val loaded = manager.load("quick")

        assertEquals(3, loaded?.version)
        assertEquals(10L, loaded?.timestamp)
        assertEquals("main.avg", loaded?.state?.scriptId)
    }

    @Test
    fun rejectsMissingMigrationFutureVersionMalformedDataAndInvalidSlot() = runTest {
        val storage = FakeStorage()
        storage.values["old"] = "{\"version\":1}"
        assertFailsWith<EngineException.SaveError> {
            JsonSaveManager(storage, currentVersion = 2, timestampProvider = { 0L }).load("old")
        }

        storage.values["future"] = "{\"version\":99}"
        val manager = JsonSaveManager(storage, timestampProvider = { 0L })
        assertFailsWith<EngineException.SaveError> { manager.load("future") }

        storage.values["broken"] = "not-json"
        assertFailsWith<EngineException.SaveError> { manager.load("broken") }
        assertFailsWith<EngineException.SaveError> { manager.load("../outside") }
    }

    private class FakeStorage : SaveStorage {
        val values = mutableMapOf<String, String>()

        override suspend fun read(slot: String): String? = values[slot]

        override suspend fun write(slot: String, data: String) {
            values[slot] = data
        }

        override suspend fun delete(slot: String) {
            values.remove(slot)
        }
    }

    private class VersionMigration(override val fromVersion: Int) : SaveMigration {
        override val toVersion: Int = fromVersion + 1

        override fun migrate(data: JsonObject): JsonObject = JsonObject(
            data + ("version" to JsonPrimitive(toVersion)),
        )
    }
}
