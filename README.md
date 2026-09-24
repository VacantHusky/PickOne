# PickOne / 帮我选

[English](README_en.md)

一款使用 Kotlin 和 Jetpack Compose 构建的离线 Android 决策助手。创建带权重的选择场景，再通过轮盘、老虎机、骰子、硬币或风格化的俄罗斯轮盘动画做出选择。

## 功能

- 支持带权重的选择项，并使用精确的整数区间采样
- 骰子和硬币支持多单位结果映射，并自动进行拒绝采样
- 提供新拟态亮色和暗色主题，可自定义主题色
- 首页支持卡片和列表两种布局
- 支持简体中文和 English
- 支持轮盘、老虎机、骰子、硬币和俄罗斯轮盘五种选择器
- 支持声音、动画时长和场景颜色等设置
- 使用 Room 和 DataStore 在本地保存场景及偏好设置

## 构建

环境要求：JDK 17 和 Android SDK 36。

```bash
./gradlew assembleDebug
```

生成的 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

## 致谢

感谢 [Linux 站](https://linux.do/) 的公益站。
