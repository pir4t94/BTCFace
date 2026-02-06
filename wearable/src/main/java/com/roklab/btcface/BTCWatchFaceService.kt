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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BTCWatchFaceService : WatchFaceService(), DataClient.OnDataChangedListener {

    companion object {
        const val LEFT_COMPLICATION_ID = 100
        const val RIGHT_COMPLICATION_ID = 101
        private const val BTC_PRICE_PATH = "/btc_price"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var renderer: BTCWatchFaceRenderer? = null

    override fun onCreate() {
        super.onCreate()
        Wearable.getDataClient(this).addListener(this)
        serviceScope.launch {
            BTCDataLayerListener.listenForPriceUpdates(this@BTCWatchFaceService)
        }
    }

    override fun onDestroy() {
        Wearable.getDataClient(this).removeListener(this)
        super.onDestroy()
    }

    override fun createUserStyleSchema(): UserStyleSchema {
        val colorThemeSetting = UserStyleSetting.ListUserStyleSetting(
            UserStyleSetting.Id("color_theme"),
            resources,
            R.string.color_theme_label,
            R.string.color_theme_desc,
            null,
            listOf(
                UserStyleSetting.ListUserStyleSetting.ListOption(
                    UserStyleSetting.Option.Id("gold"),
                    resources,
                    R.string.theme_bitcoin_gold,
                    null
                ),
                UserStyleSetting.ListUserStyleSetting.ListOption(
                    UserStyleSetting.Option.Id("silver"),
                    resources,
                    R.string.theme_silver,
                    null
                ),
                UserStyleSetting.ListUserStyleSetting.ListOption(
                    UserStyleSetting.Option.Id("green"),
                    resources,
                    R.string.theme_satoshi_green,
                    null
                ),
                UserStyleSetting.ListUserStyleSetting.ListOption(
                    UserStyleSetting.Option.Id("blue"),
                    resources,
                    R.string.theme_ice_blue,
                    null
                )
            ),
            listOf(WatchFaceLayer.BASE, WatchFaceLayer.COMPLICATIONS_OVERLAY)
        )

        val showSecondsSetting = UserStyleSetting.BooleanUserStyleSetting(
            UserStyleSetting.Id("show_seconds"),
            resources,
            R.string.show_seconds_label,
            R.string.show_seconds_desc,
            null,
            listOf(WatchFaceLayer.BASE),
            true
        )

        val showPriceSetting = UserStyleSetting.BooleanUserStyleSetting(
            UserStyleSetting.Id("show_price"),
            resources,
            R.string.show_price_label,
            R.string.show_price_desc,
            null,
            listOf(WatchFaceLayer.BASE),
            true
        )

        return UserStyleSchema(listOf(colorThemeSetting, showSecondsSetting, showPriceSetting))
    }

    override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ComplicationSlotsManager {
        return ComplicationSlotsManager(emptyList(), currentUserStyleRepository)
    }

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        renderer = BTCWatchFaceRenderer(
            context = applicationContext,
            surfaceHolder = surfaceHolder,
            watchState = watchState,
            complicationSlotsManager = complicationSlotsManager,
            currentUserStyleRepository = currentUserStyleRepository
        )
        return WatchFace(WatchFaceType.ANALOG, renderer!!)
    }

    override fun onDataChanged(dataEventBuffer: DataEventBuffer) {
        dataEventBuffer.forEach { event: DataEvent ->
            if (event.type == DataEvent.TYPE_CHANGED) {
                val item = event.dataItem
                if (item.uri?.path == BTC_PRICE_PATH) {
                    val dataMap = DataMapItem.fromDataItem(item).dataMap
                    val price = dataMap.getDouble("price", 0.0)
                    val formatted = dataMap.getString("price_formatted") ?: "$0.00"
                    val timestamp = dataMap.getLong("timestamp", System.currentTimeMillis())
                    BTCDataLayerListener.cachePrice(this, price, formatted, timestamp)
                    renderer?.onPriceUpdated()
                }
            }
        }
        dataEventBuffer.release()
    }
}
