package com.example.apigrafik.network

import com.google.gson.JsonObject
import retrofit2.http.GET
import retrofit2.http.Query

interface StockApiService {
    @GET("api/v1/markets/stock/quotes")
    suspend fun getStockQuotes(
        @Query("ticker") ticker: String
    ): JsonObject
}
