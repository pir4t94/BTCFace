package com.roklab.btcface

import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import java.text.NumberFormat
import java.util.Locale

/**
 * Provides BTC price as a complication data source.
 * Other watch faces can use this to show Bitcoin price.
 * Updates every ~15 minutes (configured in AndroidManifest).
 */
class BTCPriceComplicationService : SuspendingComplicationDataSourceService() {

    private val fmt = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val preview = "$${fmt.format(104250)}"
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(preview).build(),
                contentDescription = PlainComplicationText.Builder("Bitcoin Price").build()
            )
                .setTitle(PlainComplicationText.Builder("BTC").build())
                .build()

            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                text = PlainComplicationText.Builder("Bitcoin $preview").build(),
                contentDescription = PlainComplicationText.Builder("Bitcoin Price $preview").build()
            ).build()

            else -> null
        }
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        // Try fresh fetch, fall back to cache
        var price = BTCPriceFetcher.fetchPrice()
        if (price != null) {
            BTCPriceFetcher.cachePrice(applicationContext, price)
        } else {
            price = BTCPriceFetcher.getCachedPrice(applicationContext)
            if (price == 0.0) return null
        }

        val formatted = "$${fmt.format(price)}"

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(formatted).build(),
                contentDescription = PlainComplicationText.Builder("BTC $formatted").build()
            )
                .setTitle(PlainComplicationText.Builder("BTC").build())
                .build()

            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                text = PlainComplicationText.Builder("Bitcoin $formatted").build(),
                contentDescription = PlainComplicationText.Builder("Bitcoin Price $formatted").build()
            ).build()

            else -> null
        }
    }
}
