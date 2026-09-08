package io.github.daisukikaffuchino.nekoscript

import io.github.daisukikaffuchino.nekoscript.engine.project.GameProjectSource
import io.github.daisukikaffuchino.nekoscript.engine.project.MapGameProjectSource

private val DEMO_MANIFEST = """
    {
      "id": "nekoscript-demo",
      "name": "NekoScript Demo",
      "version": "1.0.0",
      "debuggable": true,
      "entryScript": "scripts/main.avg",
      "viewport": {
        "width": 1920,
        "height": 1080
      },
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
        },
        "aoi": {
          "name": "葵",
          "defaultExpression": "normal",
          "expressions": {
            "normal": "characters/aoi/normal.png",
            "happy": "characters/aoi/happy.png",
            "teasing": "characters/aoi/teasing.png"
          }
        }
      },
      "cg": { "club_photo": "cg/club_photo.jpg" },
      "hotspots": {
        "club_notice": {
          "x": 1360,
          "y": 360,
          "width": 360,
          "height": 360,
          "action": "jump",
          "target": "hotspot_scene"
        }
      },
      "audio": {
        "bgm": { "morning_theme": "audio/bgm/morning_theme.mp3" },
        "se": {
          "school_bell": "audio/se/school_bell.mp3",
          "class_bell": "audio/se/class_bell.mp3"
        },
        "voice": {
          "yuki_1": "audio/voice/yuki_1.mp3",
          "yuki_2": "audio/voice/yuki_2.mp3",
          "yuki_3": "audio/voice/yuki_3.mp3",
          "yuki_4": "audio/voice/yuki_4.mp3",
          "yuki_5": "audio/voice/yuki_5.mp3",
          "yuki_6": "audio/voice/yuki_6.mp3",
          "aoi_1": "audio/voice/aoi_1.mp3",
          "aoi_2": "audio/voice/aoi_2.mp3",
          "aoi_3": "audio/voice/aoi_3.mp3",
          "aoi_4": "audio/voice/aoi_4.mp3",
          "aoi_5": "audio/voice/aoi_5.mp3",
          "aoi_6": "audio/voice/aoi_6.mp3",
          "aoi_7": "audio/voice/aoi_7.mp3"
        }
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
    play_voice "yuki_2"
    say "悠希" "你终于来了，我还以为要迟到了。"
    character "aoi" "normal" right
    play_voice "aoi_1"
    say "葵" "我就知道你会卡着铃声出现，和小时候一模一样。"
    stop_voice
    say "站在另一边的是葵，从小和我一起长大的青梅竹马。"
    character "aoi" "teasing" right
    play_voice "aoi_2"
    say "葵" "怎么，见到我太惊讶，连招呼都忘了？"
    move_character "yuki" center 300
    play_voice "yuki_1"
    say "悠希" "早上好。葵也来了，那我们一起进去吧？"
    move_character "yuki" left 250
    character "aoi" "happy" right
    play_voice "aoi_3"
    say "葵" "可以，不过谁最后进教室，谁就负责放学后的饮料。"
    stop_voice
    say "两个人同时看向我，看来这个早晨得先做个决定。"
    choice:
        "帮悠希拿社团资料":
            set morning.route = "help_yuki"
            jump route_check
        "提醒葵别再催了":
            set morning.route = "answer_aoi"
            jump route_check
    label route_check
    if morning.route == "help_yuki":
        jump yuki_route
    else:
        jump aoi_route
    label yuki_route
    character "yuki" "happy" left
    character "aoi" "teasing" right
    play_voice "yuki_3"
    say "悠希" "谢谢，资料有点多。幸好有你帮忙。"
    play_voice "aoi_4"
    say "葵" "配合得真默契。看来我只能替你们看着时间了。"
    jump reunion
    label aoi_route
    character "yuki" "surprised" left
    character "aoi" "happy" right
    play_voice "aoi_5"
    say "葵" "我是在救我们三个人不被老师记迟到，这叫经验。"
    shake 180 1.2
    play_voice "yuki_4"
    say "悠希" "明明是你刚才差点撞上校门，还说得这么理直气壮。"
    jump reunion
    label reunion
    background "classroom"
    transition slide 450
    character "yuki" "normal" left
    character "aoi" "normal" right
    play_se "class_bell"
    transition flash 180
    stop_voice
    say "上课铃响起时，我们刚好在座位上坐下。"
    show_cg "club_photo" fade 400
    say "桌面上放着一张昨天拍下的社团合照。"
    hide_cg
    transition crossfade 350
    character "yuki" "happy" left
    character "aoi" "teasing" right
    play_voice "yuki_5"
    say "悠希" "放学后，也别忘了来活动室。"
    play_voice "aoi_6"
    say "葵" "社团结束以后归我。青梅竹马的回家搭档可不能缺席。"
    move_character "aoi" center 300
    stop_voice
    say "一个约定变成了两个，我只能在她们的注视下点头。"
    move_character "aoi" right 250
    play_voice "yuki_6"
    say "悠希" "那就放学后见。"
    hide "yuki"
    play_voice "aoi_7"
    say "葵" "别忘了饮料，我可记得很清楚。"
    hide "aoi"
    wait 150
    stop_voice
    stop_bgm 500
    say "晨光落在空下来的走道上，平常的一天也有了值得期待的结尾。"
    say "NekoScript Demo  完"
    jump demo_end
    label hotspot_scene
    stop_voice
    say "走廊公告栏上贴着新的社团通知。"
    say "看来放学后的活动，又会比想象中热闹一些。"
    label demo_end
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
