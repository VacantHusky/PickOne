# PickOne

[中文版](README.md)

An offline Android decision helper built with Kotlin and Jetpack Compose. Create weighted choice scenes and resolve them with a wheel, slot machine, dice, coins, or a stylized Russian roulette animation.

## Features

- Weighted choices with exact integer-range sampling
- Multi-die and multi-coin outcome mapping with automatic rejection sampling
- Neumorphic light and dark themes with a customizable accent color
- Card and list home layouts
- Simplified Chinese and English app languages
- Five selector types: wheel, slot machine, dice, coins, and Russian roulette
- Configurable sounds, animation duration, and scene colors
- Offline Room storage for scenes and DataStore preferences

## Build

Requirements: JDK 17 and Android SDK 36.

```bash
./gradlew assembleDebug
```

The APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

## Acknowledgements

Thanks to the public-benefit site [Linux.do](https://linux.do/).
