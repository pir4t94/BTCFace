package com.roklab.btcface

import android.view.SurfaceHolder
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.WatchFaceLayer
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable

class BTCWatchFaceService : WatchFaceService(), DataClient.OnDataChangedListener {

    companion object {
        const val LEFT_COMPLICATION_ID = 100
        const val RIGHT_COMPLICATION_ID = 101
    }

    override fun createUserStyleSchema(): UserStyleSchema {
        val colorThemeSetting = UserStyleSetting.ListUserStyleSetting(
            UserStyleSetting.Id("color_theme"),
            "Color Theme",
            "Choose the watch face color palette",
            null,
            listOf(
                UserStyleSetting.ListUserStyleSetting.ListOption(
                    UserStyleSetting.Option.Id("gold"),
                    "Bitcoin Gold",
                    null
                ),
                UserStyleSetting.ListUserStyleSetting.ListOption(
                    UserStyleSetting.Option.Id("silver"),
                    "Silver",
                    null
                ),
                UserStyleSetting.ListUserStyleSetting.ListOption(
                    UserStyleSetting.Option.Id("green"),
                    "Satoshi Green",
                    null
                ),
                UserStyleSetting.ListUserStyleSetting.ListOption(
                    UserStyleSetting.Option.Id("blue"),
                    "Ice Blue",
                    null
                )
            ),
            listOf(WatchFaceLayer.BASE, WatchFaceLayer.COMPLICATIONS_OVERLAY)
        )

        val showSecondsSetting = UserStyleSetting.BooleanUserStyleSetting(
            UserStyleSetting.Id("show_seconds"),
            "Seconds Hand",
            "Show or hide the seconds hand",
            null,
            listOf(WatchFaceLayer.BASE),
            defaultValue = true
        )

        val showPriceSetting = UserStyleSetting.BooleanUserStyleSetting(
            UserStyleSetting.Id("show_price"),
            "BTC Price",
            "Show live Bitcoin price on the watch face",
            null,
            listOf(WatchFaceLayer.BASE),
            defaultValue = true
        )

        return UserStyleSchema(listOf(colorThemeSetting, showSecondsSetting, showPriceSetting))
    }

    override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ComplicationSlotsManager {
        // Complications disabled for now - keep it simple
        return ComplicationSlotsManager(emptyList(), currentUserStyleRepository)
    }

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        val renderer = BTCWatchFaceRenderer(
            context = applicationContext,
            surfaceHolder = surfaceHolder,
            watchState = watchState,
            complicationSlotsManager = complicationSlotsManager,
            currentUserStyleRepository = currentUserStyleRepository
        )
        return WatchFace(WatchFaceType.ANALOG, renderer)
    }

    override fun onDataChanged(dataEventBuffer: DataEventBuffer) {
        dataEventBuffer.forEach { event: DataEvent ->
            if (event.type == DataEvent.TYPE_CHANGED) {
                val item = event.dataItem
                if (item.uri?.path == "/btc_price") {
                    val dataMap = DataMapItem.fromDataItem(item).dataMap
                    val price = dataMap.getDouble("price", 0.0)
                    val formatted = dataMap.getString("price_formatted") ?: "$0.00"
                    val timestamp = dataMap.getLong("timestamp", System.currentTimeMillis())
                    
                    BTCDataLayerListener.cachePrice(this, price, formatted, timestamp)
                }
            }
        }
    }
}
