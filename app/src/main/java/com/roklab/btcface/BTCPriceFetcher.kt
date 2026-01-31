package com.roklab.btcface

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

object BTCPriceFetcher {

    private const val API_URL = "https://api.coinlore.net/api/ticker/?id=90"
    private const val PREFS_NAME = "btc_price_cache"
    private const val KEY_PRICE = "price_usd"
    private const val KEY_TIMESTAMP = "last_update"

    suspend fun fetchPrice(): Double? = withContext(Dispatchers.IO) {
        try {
            val url = URL(API_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.requestMethod = "GET"

            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().readText()
                val arr = JSONArray(body)
                if (arr.length() > 0) {
                    arr.getJSONObject(0).getDouble("price_usd")
                } else null
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun getCachedPrice(context: Context): Double {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Use string to avoid float precision issues
        val raw = prefs.getString(KEY_PRICE, null) ?: return 0.0
        return raw.toDoubleOrNull() ?: 0.0
    }

    fun cachePrice(context: Context, price: Double) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PRICE, price.toString())
            .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    fun getLastUpdateMs(context: Context): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_TIMESTAMP, 0L)
    }
}
