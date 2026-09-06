package io.github.daisukikaffuchino.nekoscript

import io.github.daisukikaffuchino.nekoscript.engine.project.GameProjectParser
import io.github.daisukikaffuchino.nekoscript.engine.runtime.GameAction
import io.github.daisukikaffuchino.nekoscript.engine.runtime.GameSession
import io.github.daisukikaffuchino.nekoscript.engine.runtime.GameSessionFactory
import io.github.daisukikaffuchino.nekoscript.engine.runtime.SaveManagerFactory
import io.github.daisukikaffuchino.nekoscript.engine.save.InMemorySaveStorage
import io.github.daisukikaffuchino.nekoscript.engine.save.JsonSaveManager
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DemoGameTest {
    @Test
    fun embeddedProjectRunsThePositiveRouteFromChoiceToEnding() = runTest {
        val session = createSession()

        session.engine.dispatch(GameAction.Next)
        session.engine.dispatch(GameAction.Next)
        session.engine.dispatch(GameAction.Next)
        assertEquals(listOf("早上好", "你是谁？"), session.engine.viewState.value.choices.map { it.text })

        session.engine.dispatch(GameAction.SelectChoice(0))
        assertEquals("嗯，出发吧。", session.engine.viewState.value.dialogue?.text)
        session.engine.dispatch(GameAction.Next)
        assertEquals("第一幕  完", session.engine.viewState.value.dialogue?.text)
    }

    @Test
    fun embeddedProjectRunsTheAlternativeRoute() = runTest {
        val session = createSession()

        repeat(3) { session.engine.dispatch(GameAction.Next) }
        session.engine.dispatch(GameAction.SelectChoice(1))

        assertEquals("诶？我们不是每天都见面吗？", session.engine.viewState.value.dialogue?.text)
    }

    @Test
    fun embeddedManifestUsesTheSessionEntryScriptAndDeclaredAssets() = runTest {
        val manifest = demoProjectSource.readText("game.json")
        val project = GameProjectParser().parse(manifest)
        val session = createSession()

        assertEquals("scripts/main.avg", project.entryScript)
        assertEquals("backgrounds/school_day.jpg", session.assetManager.loadBackground("school_day").location)
        assertEquals("characters/yuki/normal.png", session.assetManager.loadCharacter("yuki", null).location)
    }

    private suspend fun createSession(): GameSession = GameSessionFactory(
        source = demoProjectSource,
        saveManagerFactory = SaveManagerFactory { project ->
            JsonSaveManager(InMemorySaveStorage(), timestampProvider = { 0L })
        },
    ).create()
}
