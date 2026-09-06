package io.github.daisukikaffuchino.nekoscript.engine.save

import android.content.Context
import io.github.daisukikaffuchino.nekoscript.engine.error.EngineException

/** Persists save JSON in an Android private SharedPreferences file. */
class AndroidPreferencesSaveStorage(
    context: Context,
    preferencesName: String = "nekoscript_saves",
) : SaveStorage {
    private val preferences = context.applicationContext.getSharedPreferences(
        preferencesName,
        Context.MODE_PRIVATE,
    )

    override suspend fun read(slot: String): String? = preferences.getString(slot, null)

    override suspend fun write(slot: String, data: String) {
        if (!preferences.edit().putString(slot, data).commit()) {
            throw EngineException.SaveError("slot: $slot: Android storage commit failed")
        }
    }
}
