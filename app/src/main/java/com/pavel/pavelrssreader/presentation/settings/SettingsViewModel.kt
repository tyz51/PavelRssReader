package com.pavel.pavelrssreader.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pavel.pavelrssreader.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val titleFontSize: Float = 14f,
    val bodyFontSize: Float = 17f
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.titleFontSize,
        settingsRepository.bodyFontSize
    ) { title, body -> SettingsUiState(title, body) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setTitleFontSize(sp: Float) {
        viewModelScope.launch { settingsRepository.setTitleFontSize(sp) }
    }

    fun setBodyFontSize(sp: Float) {
        viewModelScope.launch { settingsRepository.setBodyFontSize(sp) }
    }
}
