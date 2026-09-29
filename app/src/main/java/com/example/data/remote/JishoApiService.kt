package com.example.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface JishoApiService {
    @GET("api/v1/search/words")
    suspend fun searchWords(
        @Query("keyword") keyword: String
    ): JishoResponse
}
