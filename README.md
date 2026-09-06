# NekoScript

A cross-platform Visual Novel Engine powered by Kotlin Multiplatform and Compose Multiplatform.

Current targets are Android, iOS, and Desktop. Engine Core lives in `shared/commonMain`, has no
Compose or platform dependency, and exposes immutable state through `StateFlow`.

## Script example

```text
label start
background "school"
character "yuki" "happy" center
say "Yuki" "Good morning."

choice:
    "Say hello":
        set affection.yuki = 10
        jump result
    "Leave":
        jump end

label result
if affection.yuki >= 10:
    jump good_end
else:
    jump end
```

The MVP syntax also supports BGM, sound effects, voice, CG, transitions, character movement,
screen shake, typed variables, narration, and conditional branches.

The default application loads an embedded sample project through `GameSessionFactory`. The same
`GameProjectSource` boundary also supports text files packaged under
`shared/src/commonMain/composeResources/files` via `ComposeResourceGameProjectSource`.

## Build

Open the project in Android Studio. The Android app is in `androidApp`, the Desktop entry point is
in `desktopApp`, and the Xcode shell is in `iosApp`.

Common engine tests can be run with Android Studio's Gradle integration using the `:shared:jvmTest`
task. The key cross-target verification tasks are `:shared:testAndroidHostTest`,
`:shared:compileKotlinIosSimulatorArm64`, `:androidApp:assembleDebug`, and `:desktopApp:classes`.
iOS application launch and simulator tests require macOS/Xcode.

## License

NekoScript is licensed under the Apache License, Version 2.0.

You are free to use NekoScript in both open-source and
proprietary projects, including commercial games.

See LICENSE for the full license text.
