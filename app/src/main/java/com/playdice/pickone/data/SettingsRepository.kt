package com.playdice.pickone.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.playdice.pickone.model.AppLanguage
import com.playdice.pickone.model.AppSettings
import com.playdice.pickone.model.HomeLayout
import com.playdice.pickone.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "pickone_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val themeColor = longPreferencesKey("theme_color")
        val homeLayout = stringPreferencesKey("home_layout")
        val language = stringPreferencesKey("language")
        val seeded = booleanPreferencesKey("seeded")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { values ->
        AppSettings(
            themeMode = values[Keys.themeMode].enumOrDefault(ThemeMode.SYSTEM),
            themeColor = values[Keys.themeColor] ?: 0xFF7479E8,
            homeLayout = values[Keys.homeLayout].enumOrDefault(HomeLayout.CARD),
            language = values[Keys.language].enumOrDefault(AppLanguage.SYSTEM),
            seeded = values[Keys.seeded] ?: false,
        )
    }

    suspend fun setThemeMode(value: ThemeMode) = context.settingsDataStore.edit {
        it[Keys.themeMode] = value.name
    }

    suspend fun setThemeColor(value: Long) = context.settingsDataStore.edit {
        it[Keys.themeColor] = value
    }

    suspend fun setHomeLayout(value: HomeLayout) = context.settingsDataStore.edit {
        it[Keys.homeLayout] = value.name
    }

    suspend fun setLanguage(value: AppLanguage) = context.settingsDataStore.edit {
        it[Keys.language] = value.name
    }

    suspend fun markSeeded() = context.settingsDataStore.edit { it[Keys.seeded] = true }
}
