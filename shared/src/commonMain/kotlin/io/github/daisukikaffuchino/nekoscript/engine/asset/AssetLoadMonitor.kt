package io.github.daisukikaffuchino.nekoscript.engine.asset

import io.github.daisukikaffuchino.nekoscript.engine.error.EngineException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** One recoverable failure encountered while loading a project asset. */
data class AssetLoadIssue(
    val assetType: String,
    val assetId: String,
    val location: String,
)

/** Session-scoped diagnostics for assets that could not be loaded. */
class AssetLoadMonitor {
    private val mutableIssues = MutableStateFlow<List<AssetLoadIssue>>(emptyList())

    val issues: StateFlow<List<AssetLoadIssue>> = mutableIssues.asStateFlow()

    fun report(error: EngineException.AssetLoadError) {
        val issue = AssetLoadIssue(
            assetType = error.assetType,
            assetId = error.assetId,
            location = error.location,
        )
        mutableIssues.update { current ->
            if (issue in current) current else current + issue
        }
    }

    fun markLoaded(assetType: String, assetId: String, location: String) {
        mutableIssues.update { current ->
            current.filterNot { issue ->
                issue.assetType == assetType && issue.assetId == assetId && issue.location == location
            }
        }
    }
}
