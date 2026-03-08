package com.pavel.pavelrssreader.data.network

import retrofit2.http.GET
import retrofit2.http.Url

interface RssApiService {
    @GET
    suspend fun fetchFeed(@Url url: String): String
}
