package com.roklab.btcface.model

data class BTCPrice(
    val priceUsd: Double,
    val priceUsdFormatted: String,
    val timestamp: Long,
    val currencySymbol: String = "$"
) {
    fun toMap(): Map<String, Any> = mapOf(
        "price" to priceUsd,
        "price_formatted" to priceUsdFormatted,
        "timestamp" to timestamp
    )
}
