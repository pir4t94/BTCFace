package com.roklab.btcface.sync

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.roklab.btcface.model.BTCPrice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DataLayerSender {

    private const val BTC_PRICE_PATH = "/btc_price"
    private const val KEY_PRICE = "price"
    private const val KEY_PRICE_FORMATTED = "price_formatted"
    private const val KEY_TIMESTAMP = "timestamp"

    suspend fun sendPriceToWatch(context: Context, price: BTCPrice) = withContext(Dispatchers.IO) {
        try {
            val putDataMapRequest = PutDataMapRequest.create(BTC_PRICE_PATH)
            putDataMapRequest.dataMap.apply {
                putDouble(KEY_PRICE, price.priceUsd)
                putString(KEY_PRICE_FORMATTED, price.priceUsdFormatted)
                putLong(KEY_TIMESTAMP, price.timestamp)
            }
            putDataMapRequest.setUrgent()

            val dataClient = Wearable.getDataClient(context)
            val task = dataClient.putDataItem(putDataMapRequest.asPutDataRequest())
            
            Tasks.await(task)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
