package io.github.daisukikaffuchino.nekoscript

import io.github.daisukikaffuchino.nekoscript.engine.project.GameProjectSource
import io.github.daisukikaffuchino.nekoscript.engine.project.MapGameProjectSource

private val DEMO_MANIFEST = """
    {
      "id": "nekoscript-demo",
      "name": "NekoScript Demo",
      "version": "1.0.0",
      "entryScript": "scripts/main.avg",
      "backgrounds": {
        "school_day": "backgrounds/school_day.jpg",
        "classroom": "backgrounds/classroom.jpg"
      },
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
      },
      "cg": { "club_photo": "cg/club_photo.jpg" },
      "audio": {
        "bgm": { "morning_theme": "audio/bgm/morning_theme.ogg" },
        "se": {
          "school_bell": "audio/se/school_bell.ogg",
          "class_bell": "audio/se/class_bell.ogg"
        },
        "voice": { "yuki_good_morning": "audio/voice/yuki_good_morning.ogg" }
      }
    }
""".trimIndent()

private val DEMO_SCRIPT = """
    label start
    background "school_day"
    play_bgm "morning_theme" loop
    transition fade 500
    say "清晨的校门口，阳光刚刚越过教学楼。"
    play_se "school_bell"
    character "yuki" "normal" left
    say "悠希" "你终于来了，我还以为要迟到了。"
    move_character "yuki" center 300
    play_voice "yuki_good_morning"
    say "悠希" "早上好。今天也一起进去吧？"
    stop_voice
    say "她把书包带往肩上提了提，等着我的回答。"
    choice:
        "直接去教室":
            set morning.route = "classroom"
            jump route_check
        "先看看公告栏":
            set morning.route = "notice"
            jump route_check
    label route_check
    if morning.route == "classroom":
        jump classroom_route
    else:
        jump notice_route
    label classroom_route
    character "yuki" "happy" center
    say "悠希" "好呀，我正好有件东西想给你看。"
    jump reunion
    label notice_route
    character "yuki" "surprised" center
    say "悠希" "公告栏？难道社团名单已经贴出来了？"
    shake 180 1.2
    say "悠希" "原来只是风吹动了纸张……吓我一跳。"
    jump reunion
    label reunion
    background "classroom"
    transition crossfade 450
    play_se "class_bell"
    say "上课铃响起时，我们刚好在座位上坐下。"
    show_cg "club_photo" fade 400
    say "桌面上放着一张昨天拍下的社团合照。"
    hide_cg
    character "yuki" "happy" right
    say "悠希" "放学后，也别忘了来活动室。"
    hide "yuki"
    wait 150
    stop_bgm 500
    say "她回到座位，短暂的晨间约定就这样定下了。"
    say "NekoScript Demo  完"
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
