package io.github.daisukikaffuchino.nekoscript

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import io.github.daisukikaffuchino.nekoscript.engine.runtime.GameAction
import io.github.daisukikaffuchino.nekoscript.engine.runtime.GameSession
import io.github.daisukikaffuchino.nekoscript.engine.runtime.GameSessionFactory
import io.github.daisukikaffuchino.nekoscript.engine.runtime.SaveManagerFactory
import io.github.daisukikaffuchino.nekoscript.engine.project.GameProjectSource
import io.github.daisukikaffuchino.nekoscript.engine.save.InMemorySaveStorage
import io.github.daisukikaffuchino.nekoscript.engine.save.JsonSaveManager
import io.github.daisukikaffuchino.nekoscript.engine.save.SaveStorage
import io.github.daisukikaffuchino.nekoscript.ui.GameScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun App(
    saveStorage: SaveStorage,
    timestampProvider: () -> Long,
    projectSource: GameProjectSource = defaultProjectSource,
) {
    MaterialTheme {
        var session by remember(saveStorage, projectSource) { mutableStateOf<GameSession?>(null) }
        var loadError by remember(saveStorage, projectSource) { mutableStateOf<Throwable?>(null) }

        LaunchedEffect(saveStorage, projectSource) {
            session = null
            loadError = null
            try {
                session = GameSessionFactory(
                    source = projectSource,
                    saveManagerFactory = SaveManagerFactory { project ->
                        JsonSaveManager(saveStorage, timestampProvider = timestampProvider)
                    },
                ).create()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                loadError = error
            }
        }

        when {
            loadError != null -> ProjectLoadErrorScreen(loadError!!)
            session == null -> ProjectLoadingScreen()
            else -> {
                val engine = session!!.engine
                val state by engine.viewState.collectAsState()
                val scope = rememberCoroutineScope()
                LaunchedEffect(engine) { engine.dispatch(GameAction.Next) }
                GameScreen(
                    state = state,
                    onAction = { action -> scope.launch { engine.dispatch(action) } },
                )
            }
        }
    }
}

@Composable
private fun ProjectLoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize().safeContentPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Text("Loading project...")
    }
}

@Composable
private fun ProjectLoadErrorScreen(error: Throwable) {
    Column(
        modifier = Modifier.fillMaxSize().safeContentPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Unable to load project",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = error.message ?: error::class.simpleName.orEmpty(),
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
@Preview
fun AppPreview() {
    App(
        saveStorage = remember { InMemorySaveStorage() },
        timestampProvider = { 0L },
    )
}
