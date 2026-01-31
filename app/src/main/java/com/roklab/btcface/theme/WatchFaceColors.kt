package com.roklab.btcface.theme

import android.graphics.Color

data class WatchFaceColorScheme(
    val id: String,
    val name: String,
    val primary: Int,
    val secondary: Int,
    val accent: Int,
    val bgCenter: Int,
    val bgEdge: Int,
    val logoTint: Int,
    val priceText: Int
)

object WatchFaceColors {

    val BITCOIN_GOLD = WatchFaceColorScheme(
        id = "gold",
        name = "Bitcoin Gold",
        primary = Color.parseColor("#F7931A"),
        secondary = Color.parseColor("#FFD700"),
        accent = Color.parseColor("#FFAA00"),
        bgCenter = Color.parseColor("#0E0E18"),
        bgEdge = Color.parseColor("#06060C"),
        logoTint = Color.parseColor("#1A1510"),
        priceText = Color.parseColor("#FFD700")
    )

    val SILVER = WatchFaceColorScheme(
        id = "silver",
        name = "Silver",
        primary = Color.parseColor("#C0C0C0"),
        secondary = Color.parseColor("#E8E8E8"),
        accent = Color.parseColor("#A0A0B0"),
        bgCenter = Color.parseColor("#0E0E12"),
        bgEdge = Color.parseColor("#060608"),
        logoTint = Color.parseColor("#14141A"),
        priceText = Color.parseColor("#E0E0E0")
    )

    val SATOSHI_GREEN = WatchFaceColorScheme(
        id = "green",
        name = "Satoshi Green",
        primary = Color.parseColor("#00E676"),
        secondary = Color.parseColor("#76FF03"),
        accent = Color.parseColor("#00C853"),
        bgCenter = Color.parseColor("#0A100E"),
        bgEdge = Color.parseColor("#040806"),
        logoTint = Color.parseColor("#0C1A10"),
        priceText = Color.parseColor("#76FF03")
    )

    val ICE_BLUE = WatchFaceColorScheme(
        id = "blue",
        name = "Ice Blue",
        primary = Color.parseColor("#40C4FF"),
        secondary = Color.parseColor("#80D8FF"),
        accent = Color.parseColor("#0091EA"),
        bgCenter = Color.parseColor("#0A0E14"),
        bgEdge = Color.parseColor("#04060A"),
        logoTint = Color.parseColor("#0C1420"),
        priceText = Color.parseColor("#80D8FF")
    )

    val ALL = listOf(BITCOIN_GOLD, SILVER, SATOSHI_GREEN, ICE_BLUE)

    fun fromId(id: String): WatchFaceColorScheme = when (id) {
        "silver" -> SILVER
        "green" -> SATOSHI_GREEN
        "blue" -> ICE_BLUE
        else -> BITCOIN_GOLD
    }
}
