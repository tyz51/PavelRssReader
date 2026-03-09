package com.pavel.pavelrssreader.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val titleFontSize: Flow<Float>
    val bodyFontSize: Flow<Float>
    suspend fun setTitleFontSize(sp: Float)
    suspend fun setBodyFontSize(sp: Float)

    companion object {
        const val DEFAULT_TITLE_FONT_SIZE = 14f
        const val DEFAULT_BODY_FONT_SIZE = 17f
    }
}
