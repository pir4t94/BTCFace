package com.roklab.btcface.sync

import android.content.Context
import com.roklab.btcface.model.BTCPrice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

object BTCPriceFetcher {

    private const val API_URL = "https://api.coinlore.net/api/ticker/?id=90"
    private const val PREFS_NAME = "btc_price_cache"
    private const val KEY_PRICE = "price_usd"
    private const val KEY_FORMATTED = "price_formatted"
    private const val KEY_TIMESTAMP = "last_update"

    suspend fun fetchPrice(): BTCPrice? = withContext(Dispatchers.IO) {
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
                    val priceDouble = arr.getJSONObject(0).getDouble("price_usd")
                    val formatted = formatPrice(priceDouble)
                    BTCPrice(
                        priceUsd = priceDouble,
                        priceUsdFormatted = formatted,
                        timestamp = System.currentTimeMillis()
                    )
                } else null
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getCachedPrice(context: Context): BTCPrice? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val price = prefs.getString(KEY_PRICE, null)?.toDoubleOrNull() ?: return null
        val formatted = prefs.getString(KEY_FORMATTED, null) ?: return null
        val timestamp = prefs.getLong(KEY_TIMESTAMP, 0L)
        return BTCPrice(price, formatted, timestamp)
    }

    fun cachePrice(context: Context, price: BTCPrice) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PRICE, price.priceUsd.toString())
            .putString(KEY_FORMATTED, price.priceUsdFormatted)
            .putLong(KEY_TIMESTAMP, price.timestamp)
            .apply()
    }

    fun getLastUpdateMs(context: Context): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_TIMESTAMP, 0L)
    }

    private fun formatPrice(price: Double): String {
        return NumberFormat.getCurrencyInstance(Locale.US).apply {
            maximumFractionDigits = 0
        }.format(price)
    }
}
