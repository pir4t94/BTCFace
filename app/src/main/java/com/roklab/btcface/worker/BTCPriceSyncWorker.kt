package com.roklab.btcface.worker

import android.content.Context
import androidx.work.*
import com.roklab.btcface.sync.BTCPriceFetcher
import com.roklab.btcface.sync.DataLayerSender
import java.util.concurrent.TimeUnit

class BTCPriceSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Fetch latest price from Coinlore API
            val price = BTCPriceFetcher.fetchPrice() ?: return Result.retry()
            
            // Cache it locally on phone
            BTCPriceFetcher.cachePrice(applicationContext, price)
            
            // Send to watch via Data Layer
            val success = DataLayerSender.sendPriceToWatch(applicationContext, price)
            
            if (success) Result.success() else Result.retry()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        private const val WORK_TAG = "btc_price_sync"
        const val DEFAULT_SYNC_INTERVAL_MIN = 15

        fun schedule(context: Context, intervalMinutes: Int = DEFAULT_SYNC_INTERVAL_MIN) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<BTCPriceSyncWorker>(
                intervalMinutes.toLong(), TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .addTag(WORK_TAG)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_TAG,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }

        fun reschedule(context: Context, intervalMinutes: Int) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<BTCPriceSyncWorker>(
                intervalMinutes.toLong(), TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .addTag(WORK_TAG)
                .build()

            // Use REPLACE to update the existing work with new interval
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_TAG,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    request
                )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG)
        }
    }
}
