package com.roklab.btcface

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BTCDataLayerListener {

    private const val BTC_PRICE_PATH = "/btc_price"
    private const val PREFS_NAME = "btc_price_watch"
    private const val KEY_PRICE = "price"
    private const val KEY_PRICE_FORMATTED = "price_formatted"
    private const val KEY_TIMESTAMP = "timestamp"

    suspend fun listenForPriceUpdates(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val dataClient = Wearable.getDataClient(context)
            val task = dataClient.getDataItems()
            val dataItems = Tasks.await(task)
            
            for (item in dataItems) {
                if (item.uri.path.compareTo(BTC_PRICE_PATH) == 0) {
                    val dataMap = item.data
                    val price = dataMap.getDouble(KEY_PRICE, 0.0)
                    val formatted = dataMap.getString(KEY_PRICE_FORMATTED) ?: "$0.00"
                    val timestamp = dataMap.getLong(KEY_TIMESTAMP, System.currentTimeMillis())
                    
                    cachePrice(context, price, formatted, timestamp)
                    return@withContext true
                }
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getCachedPrice(context: Context): Double {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PRICE, "0.0")?.toDoubleOrNull() ?: 0.0
    }

    fun getCachedPriceFormatted(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PRICE_FORMATTED, "$0.00") ?: "$0.00"
    }

    fun getCachedTimestamp(context: Context): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_TIMESTAMP, 0L)
    }

    fun cachePrice(context: Context, price: Double, formatted: String, timestamp: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PRICE, price.toString())
            .putString(KEY_PRICE_FORMATTED, formatted)
            .putLong(KEY_TIMESTAMP, timestamp)
            .apply()
    }
}
