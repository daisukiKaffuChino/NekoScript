package io.github.daisukikaffuchino.nekoscript.engine.save

import platform.Foundation.NSUserDefaults

/** Persists save JSON in the application's iOS user defaults domain. */
class IosUserDefaultsSaveStorage(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
    private val keyPrefix: String = "nekoscript.save.",
) : SaveStorage {
    override suspend fun read(slot: String): String? = defaults.stringForKey(keyPrefix + slot)

    override suspend fun write(slot: String, data: String) {
        defaults.setObject(data, forKey = keyPrefix + slot)
    }

    override suspend fun delete(slot: String) {
        defaults.removeObjectForKey(keyPrefix + slot)
    }
}
