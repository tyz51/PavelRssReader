package com.pavel.pavelrssreader.domain.model

data class Feed(
    val id: Long = 0L,
    val url: String,
    val title: String,
    val addedAt: Long = System.currentTimeMillis()
)
