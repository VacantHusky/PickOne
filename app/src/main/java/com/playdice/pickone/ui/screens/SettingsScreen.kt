@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.playdice.pickone.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.playdice.pickone.R
import com.playdice.pickone.model.AppLanguage
import com.playdice.pickone.model.ThemeMode
import com.playdice.pickone.ui.components.NeuCard
import com.playdice.pickone.ui.components.NeuColorButton
import com.playdice.pickone.ui.components.NeuSlider
import com.playdice.pickone.ui.components.RowSelection
import com.playdice.pickone.ui.viewmodel.SettingsViewModel

private val ThemeColors = listOf(0xFF7479E8, 0xFF4F8DD8, 0xFF4FA8A1, 0xFF65A765, 0xFFE0A03B, 0xFFE27D60, 0xFFD06091, 0xFF8B6FC0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.padding(start = 4.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SettingsGroup(Icons.Rounded.Language, stringResource(R.string.language)) {
                RowSelection(
                    options = AppLanguage.entries,
                    selected = settings.language,
                    label = { language ->
                        stringResource(
                            when (language) {
                                AppLanguage.SYSTEM -> R.string.language_system
                                AppLanguage.CHINESE -> R.string.language_chinese
                                AppLanguage.ENGLISH -> R.string.language_english
                            },
                        )
                    },
                    onSelectionChange = viewModel::setLanguage,
                )
            }
            SettingsGroup(Icons.Rounded.DarkMode, stringResource(R.string.theme_mode)) {
                RowSelection(
                    options = ThemeMode.entries,
                    selected = settings.themeMode,
                    label = { mode ->
                        stringResource(
                            when (mode) {
                                ThemeMode.SYSTEM -> R.string.theme_system
                                ThemeMode.LIGHT -> R.string.theme_light
                                ThemeMode.DARK -> R.string.theme_dark
                            },
                        )
                    },
                    onSelectionChange = viewModel::setThemeMode,
                )
            }
            SettingsGroup(Icons.Rounded.ColorLens, stringResource(R.string.theme_color)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ThemeColors.forEach { colorValue ->
                        ColorSwatch(Color(colorValue), settings.themeColor == colorValue) { viewModel.setThemeColor(colorValue) }
                    }
                }
                HsvColorControls(settings.themeColor, viewModel::setThemeColor)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsGroup(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, content: @Composable ColumnScope.() -> Unit) {
    NeuCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}

@Composable
private fun ColorSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    NeuColorButton(color = color, selected = selected, onClick = onClick)
}

@Composable
private fun HsvColorControls(colorValue: Long, onChange: (Long) -> Unit) {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(colorValue.toInt(), hsv)
    Text("H ${hsv[0].toInt()}°  ·  S ${(hsv[1] * 100).toInt()}%  ·  V ${(hsv[2] * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    NeuSlider(
        value = hsv[0],
        onValueChange = { hue -> onChange(AndroidColor.HSVToColor(floatArrayOf(hue, hsv[1], hsv[2])).toLong() and 0xFFFFFFFFL) },
        valueRange = 0f..360f,
    )
    NeuSlider(
        value = hsv[1],
        onValueChange = { saturation -> onChange(AndroidColor.HSVToColor(floatArrayOf(hsv[0], saturation, hsv[2])).toLong() and 0xFFFFFFFFL) },
        valueRange = .25f..1f,
    )
    NeuSlider(
        value = hsv[2],
        onValueChange = { brightness -> onChange(AndroidColor.HSVToColor(floatArrayOf(hsv[0], hsv[1], brightness)).toLong() and 0xFFFFFFFFL) },
        valueRange = .45f..1f,
    )
}
