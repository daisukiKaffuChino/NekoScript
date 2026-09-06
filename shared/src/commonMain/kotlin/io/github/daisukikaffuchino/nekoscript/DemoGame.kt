package io.github.daisukikaffuchino.nekoscript

import io.github.daisukikaffuchino.nekoscript.engine.project.GameProjectSource
import io.github.daisukikaffuchino.nekoscript.engine.project.MapGameProjectSource

private val DEMO_MANIFEST = """
    {
      "id": "nekoscript-demo",
      "name": "NekoScript Demo",
      "version": "1.0.0",
      "entryScript": "scripts/main.avg",
      "backgrounds": { "school_day": "backgrounds/school_day.jpg" },
      "characters": {
        "yuki": {
          "name": "悠希",
          "defaultExpression": "normal",
          "expressions": {
            "normal": "characters/yuki/normal.png",
            "happy": "characters/yuki/happy.png",
            "surprised": "characters/yuki/surprised.png"
          }
        }
      }
    }
""".trimIndent()

private val DEMO_SCRIPT = """
    label start
    background "school_day"
    character "yuki" "normal" center
    say "悠希" "早上好。"
    say "今天也一起去学校吧。"
    choice:
        "早上好":
            set affection.yuki = 1
            jump route_check
        "你是谁？":
            set affection.yuki = 0
            jump route_check
    label route_check
    if affection.yuki >= 1:
        jump morning
    else:
        jump stranger
    label morning
    character "yuki" "happy" center
    say "悠希" "嗯，出发吧。"
    jump end
    label stranger
    character "yuki" "surprised" center
    say "悠希" "诶？我们不是每天都见面吗？"
    label end
    say "第一幕  完"
""".trimIndent()

/** Embedded sample project used by the default application shell and previews. */
internal val defaultProjectSource: GameProjectSource = ComposeResourceGameProjectSource()

/** In-memory copy of the sample project used by common tests and host previews. */
internal val demoProjectSource: GameProjectSource = MapGameProjectSource(
    mapOf(
        "game.json" to DEMO_MANIFEST,
        "scripts/main.avg" to DEMO_SCRIPT,
    ),
)
