package com.pavel.pavelrssreader.domain.repository

import com.pavel.pavelrssreader.domain.model.ThemePreference
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val titleFontSize: Flow<Float>
    val bodyFontSize: Flow<Float>
    val themePreference: Flow<ThemePreference>
    suspend fun setTitleFontSize(sp: Float)
    suspend fun setBodyFontSize(sp: Float)
    suspend fun setThemePreference(pref: ThemePreference)

    companion object {
        const val DEFAULT_TITLE_FONT_SIZE = 14f
        const val DEFAULT_BODY_FONT_SIZE = 17f
    }
}
