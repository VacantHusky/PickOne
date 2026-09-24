package com.playdice.pickone.ui.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.Games
import androidx.compose.material.icons.rounded.LensBlur
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.playdice.pickone.R
import com.playdice.pickone.model.SelectorType
import com.playdice.pickone.model.SoundChoice

fun selectorIcon(type: SelectorType): ImageVector = when (type) {
    SelectorType.WHEEL -> Icons.Rounded.TrackChanges
    SelectorType.SLOT_MACHINE -> Icons.Rounded.Casino
    SelectorType.DICE -> Icons.Rounded.Games
    SelectorType.COIN -> Icons.Rounded.Circle
    SelectorType.RUSSIAN_ROULETTE -> Icons.Rounded.LensBlur
}

@StringRes
fun selectorLabelRes(type: SelectorType): Int = when (type) {
    SelectorType.WHEEL -> R.string.type_wheel
    SelectorType.SLOT_MACHINE -> R.string.type_slot
    SelectorType.DICE -> R.string.type_dice
    SelectorType.COIN -> R.string.type_coin
    SelectorType.RUSSIAN_ROULETTE -> R.string.type_roulette
}

@Composable
fun selectorLabel(type: SelectorType): String = stringResource(selectorLabelRes(type))

@StringRes
fun soundLabelRes(sound: SoundChoice): Int = when (sound) {
    SoundChoice.NONE -> R.string.sound_none
    SoundChoice.CRISP -> R.string.sound_crisp
    SoundChoice.SOFT -> R.string.sound_soft
    SoundChoice.MECHANICAL -> R.string.sound_mechanical
}
