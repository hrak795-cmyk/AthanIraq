package com.athaniqraq.app.data

import retrofit2.http.GET
import retrofit2.http.Query

interface AladhanApi {
    @GET("v1/timings")
    suspend fun timings(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = 7
    ): PrayerApiResponse
}
