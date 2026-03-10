package com.pavel.pavelrssreader.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pavel.pavelrssreader.domain.model.ThemePreference
import com.pavel.pavelrssreader.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override val titleFontSize: Flow<Float> =
        dataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { it[KEY_TITLE_FONT_SIZE] ?: SettingsRepository.DEFAULT_TITLE_FONT_SIZE }

    override val bodyFontSize: Flow<Float> =
        dataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { it[KEY_BODY_FONT_SIZE] ?: SettingsRepository.DEFAULT_BODY_FONT_SIZE }

    override val themePreference: Flow<ThemePreference> =
        dataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { prefs ->
                when (prefs[KEY_THEME_PREFERENCE]) {
                    ThemePreference.LIGHT.name -> ThemePreference.LIGHT
                    ThemePreference.DARK.name -> ThemePreference.DARK
                    else -> ThemePreference.SYSTEM
                }
            }

    override suspend fun setTitleFontSize(sp: Float) {
        dataStore.edit { it[KEY_TITLE_FONT_SIZE] = sp }
    }

    override suspend fun setBodyFontSize(sp: Float) {
        dataStore.edit { it[KEY_BODY_FONT_SIZE] = sp }
    }

    override suspend fun setThemePreference(pref: ThemePreference) {
        dataStore.edit { it[KEY_THEME_PREFERENCE] = pref.name }
    }

    companion object {
        private val KEY_TITLE_FONT_SIZE = floatPreferencesKey("title_font_size")
        private val KEY_BODY_FONT_SIZE = floatPreferencesKey("body_font_size")
        private val KEY_THEME_PREFERENCE = stringPreferencesKey("theme_preference")
    }
}
