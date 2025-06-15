package com.example.apigrafik

import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.apigrafik.Fragment.Stock
import com.example.apigrafik.Fragment.Meta
import com.example.apigrafik.Fragment.Body
import com.example.apigrafik.network.RetrofitInstance
import com.google.gson.JsonObject
import com.google.gson.Gson
import com.google.common.reflect.TypeToken
import kotlinx.coroutines.launch
import android.util.Log
import androidx.lifecycle.AndroidViewModel

class StockViewModel(application: Application) : AndroidViewModel(application) {
    private val _stocks = MutableLiveData<List<Stock>>()
    val stocks: LiveData<List<Stock>> = _stocks

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private var hasLoadedData = false
    private val stockCache = mutableMapOf<String, Stock>()

    private val sharedPref = application.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    private val CACHE_TIMEOUT = 5 * 60 * 60 * 1000 // 5 hours in milliseconds

    fun loadStockData() {
        // Check if the cached data is still valid
        val lastFetchTime = sharedPref.getLong("last_fetch_time", 0)
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastFetchTime < CACHE_TIMEOUT) {
            // Use cached data if not expired (less than 5 hours)
            val cachedStocks = getCachedStockData()
            _stocks.value = cachedStocks ?: emptyList()
            hasLoadedData = cachedStocks != null
            return
        }

        if (hasLoadedData && !_stocks.value.isNullOrEmpty()) {
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val stockGroups = listOf(
                    listOf("AAPL", "GOOGL", "MSFT", "AMZN", "NVDA", "TSLA", "META", "BRK-A", "JNJ", "V"),
                    listOf("BBCA.JK", "BBNI.JK", "BBRI.JK", "GOTO.JK", "TLKM.JK", "UNVR.JK", "SIDO.JK", "EMTK.JK", "ADRO.JK", "ANTM.JK"),
                    listOf("BABA", "NFLX", "DIS", "PYPL", "INTC", "CSCO", "AMD", "WMT", "KO", "PFE")
                )

                // Combine all successfully loaded stocks
                val allStocks = mutableListOf<Stock>()
                for (group in stockGroups) {
                    loadStockBatch(group)?.let { allStocks.addAll(it) }
                }

                _stocks.value = allStocks
                saveStockDataToCache(allStocks) // Save data to cache after loading
                saveLastFetchTime(currentTime) // Save the last fetch time
                hasLoadedData = true
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "An error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadStockBatch(tickers: List<String>): List<Stock>? {
        return try {
            val tickerQuery = tickers.joinToString(",")
            val response = RetrofitInstance.api.getStockQuotes(tickerQuery)
            parseStockList(response).also { stocks ->
                stocks.forEach { stock ->
                    val symbol = stock.body.firstOrNull()?.symbol.orEmpty()
                    if (symbol.isNotEmpty()) {
                        stockCache[symbol] = stock
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("StockViewModel", "Error loading batch: ${e.message}", e)
            null
        }
    }

    private fun parseStockList(response: JsonObject): List<Stock> {
        return try {
            val bodyArray = response.getAsJsonArray("body") ?: return emptyList()
            bodyArray.mapNotNull { element ->
                val body = element.asJsonObject
                Stock(
                    meta = Meta(
                        version = response.getAsJsonObject("meta")?.get("version")?.asString ?: "v1.0",
                        status = response.getAsJsonObject("meta")?.get("status")?.asInt ?: 200,
                        copywrite = response.getAsJsonObject("meta")?.get("copywrite")?.asString.orEmpty(),
                        symbol = body.get("symbol")?.asString.orEmpty(),
                        processedTime = response.getAsJsonObject("meta")?.get("processedTime")?.asString.orEmpty()
                    ),
                    body = listOf(
                        Body(
                            shortName = body.get("shortName")?.asString.orEmpty(),
                            longName = body.get("longName")?.asString.orEmpty(),
                            regularMarketPrice = body.get("regularMarketPrice")?.asDouble ?: 0.0,
                            regularMarketDayHigh = body.get("regularMarketDayHigh")?.asDouble ?: 0.0,
                            regularMarketDayLow = body.get("regularMarketDayLow")?.asDouble ?: 0.0,
                            regularMarketVolume = body.get("regularMarketVolume")?.asLong ?: 0,
                            fullExchangeName = body.get("fullExchangeName")?.asString.orEmpty(),
                            averageAnalystRating = body.get("averageAnalystRating")?.asString.orEmpty(),
                            fiftyTwoWeekRange = body.get("fiftyTwoWeekRange")?.asString.orEmpty(),
                            fiftyTwoWeekLow = body.get("fiftyTwoWeekLow")?.asDouble ?: 0.0,
                            fiftyTwoWeekHigh = body.get("fiftyTwoWeekHigh")?.asDouble ?: 0.0,
                            dividendDate = body.get("dividendDate")?.asLong,
                            dividendYield = body.get("dividendYield")?.asDouble,
                            marketCap = body.get("marketCap")?.asLong,
                            exchange = body.get("exchange")?.asString.orEmpty(),
                            market = body.get("market")?.asString.orEmpty(),
                            regularMarketChangePercent = body.get("regularMarketChangePercent")?.asDouble ?: 0.0,
                            displayName = body.get("displayName")?.asString.orEmpty(),
                            symbol = body.get("symbol")?.asString.orEmpty()
                        )
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("ParseStockList", "Error parsing stock list", e)
            emptyList()
        }
    }

    private fun getCachedStockData(): List<Stock>? {
        val cachedData = sharedPref.getString("stock_data", null)
        return if (cachedData != null) {
            val gson = Gson()
            val type = object : TypeToken<List<Stock>>() {}.type
            gson.fromJson(cachedData, type)
        } else {
            null
        }
    }

    private fun saveStockDataToCache(stocks: List<Stock>) {
        val editor = sharedPref.edit()
        val gson = Gson()
        val json = gson.toJson(stocks)
        editor.putString("stock_data", json)
        editor.apply()
    }

    private fun saveLastFetchTime(time: Long) {
        val editor = sharedPref.edit()
        editor.putLong("last_fetch_time", time)
        editor.apply()
    }
}
