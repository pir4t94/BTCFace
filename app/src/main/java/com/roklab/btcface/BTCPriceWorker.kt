package com.roklab.btcface

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class BTCPriceWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val price = BTCPriceFetcher.fetchPrice() ?: return Result.retry()
        BTCPriceFetcher.cachePrice(applicationContext, price)
        return Result.success()
    }

    companion object {
        private const val WORK_TAG = "btc_price_sync"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<BTCPriceWorker>(
                15, TimeUnit.MINUTES
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
    }
}
