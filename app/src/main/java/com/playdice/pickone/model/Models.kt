package com.playdice.pickone.model

import java.util.UUID

enum class SelectorType { WHEEL, SLOT_MACHINE, DICE, COIN, RUSSIAN_ROULETTE }

enum class SoundChoice { NONE, CRISP, SOFT, MECHANICAL }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class HomeLayout { LIST, CARD }

enum class AppLanguage(val languageTag: String) {
    SYSTEM(""),
    CHINESE("zh-CN"),
    ENGLISH("en"),
}

data class ChoiceItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val weight: Int = 1,
    val position: Int = 0,
)

data class Scene(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val selectorType: SelectorType = SelectorType.WHEEL,
    val color: Long = 0xFF7479E8,
    val sound: SoundChoice = SoundChoice.CRISP,
    val animationDurationMs: Int = 3_000,
    val choices: List<ChoiceItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themeColor: Long = 0xFF7479E8,
    val homeLayout: HomeLayout = HomeLayout.CARD,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val seeded: Boolean = false,
)

data class SelectionOutcome(
    val selected: ChoiceItem,
    val normalizedWeights: List<Int>,
    val sampledIndex: Int,
    val unitValues: List<Int> = emptyList(),
    val rejectedRolls: Int = 0,
)
