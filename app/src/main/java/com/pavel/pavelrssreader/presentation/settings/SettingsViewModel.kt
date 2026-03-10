package com.pavel.pavelrssreader.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pavel.pavelrssreader.domain.model.ThemePreference
import com.pavel.pavelrssreader.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val titleFontSize: Float = SettingsRepository.DEFAULT_TITLE_FONT_SIZE,
    val bodyFontSize: Float = SettingsRepository.DEFAULT_BODY_FONT_SIZE,
    val themePreference: ThemePreference = ThemePreference.SYSTEM
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.titleFontSize,
        settingsRepository.bodyFontSize,
        settingsRepository.themePreference
    ) { title, body, theme ->
        SettingsUiState(title, body, theme)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setTitleFontSize(sp: Float) {
        viewModelScope.launch { settingsRepository.setTitleFontSize(sp) }
    }

    fun setBodyFontSize(sp: Float) {
        viewModelScope.launch { settingsRepository.setBodyFontSize(sp) }
    }

    fun setThemePreference(pref: ThemePreference) {
        viewModelScope.launch { settingsRepository.setThemePreference(pref) }
    }
}
