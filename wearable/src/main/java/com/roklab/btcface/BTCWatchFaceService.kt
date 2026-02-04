package com.roklab.btcface

import android.graphics.RectF
import android.view.SurfaceHolder
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.SurfaceHolder
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.WatchFaceLayer
import androidx.wear.watchface.complications.CanvasComplicationFactory
import androidx.wear.watchface.complications.ComplicationSlot
import androidx.wear.watchface.complications.ComplicationSlotBounds
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy
import androidx.wear.watchface.complications.SystemDataSources
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
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
            resources,
            R.string.color_theme_label,
            R.string.color_theme_desc,
            null,
            listOf(
                UserStyleSetting.ListUserStyleSetting.ListOption(
                    UserStyleSetting.Option.Id("gold"),
                    resources,
                    R.string.theme_bitcoin_gold,
                    R.string.theme_bitcoin_gold,
                    null
                ),
                UserStyleSetting.ListUserStyleSetting.ListOption(
                    UserStyleSetting.Option.Id("silver"),
                    resources,
                    R.string.theme_silver,
                    R.string.theme_silver,
                    null
                ),
                UserStyleSetting.ListUserStyleSetting.ListOption(
                    UserStyleSetting.Option.Id("green"),
                    resources,
                    R.string.theme_satoshi_green,
                    R.string.theme_satoshi_green,
                    null
                ),
                UserStyleSetting.ListUserStyleSetting.ListOption(
                    UserStyleSetting.Option.Id("blue"),
                    resources,
                    R.string.theme_ice_blue,
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
            defaultValue = true
        )

        val showPriceSetting = UserStyleSetting.BooleanUserStyleSetting(
            UserStyleSetting.Id("show_price"),
            resources,
            R.string.show_price_label,
            R.string.show_price_desc,
            null,
            listOf(WatchFaceLayer.BASE),
            defaultValue = true
        )

        return UserStyleSchema(listOf(colorThemeSetting, showSecondsSetting, showPriceSetting))
    }

    override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ComplicationSlotsManager {
        val leftSlot = ComplicationSlot.createRoundRectComplicationSlotBuilder(
            LEFT_COMPLICATION_ID,
            CanvasComplicationFactory { watchState, invalidateCallback ->
                CanvasComplicationDrawable(
                    ComplicationDrawable(this@BTCWatchFaceService),
                    watchState,
                    invalidateCallback
                )
            },
            listOf(
                ComplicationType.SHORT_TEXT,
                ComplicationType.SMALL_IMAGE,
                ComplicationType.RANGED_VALUE,
                ComplicationType.MONOCHROMATIC_IMAGE
            ),
            DefaultComplicationDataSourcePolicy(
                SystemDataSources.DATA_SOURCE_DATE,
                ComplicationType.SHORT_TEXT
            ),
            ComplicationSlotBounds(RectF(0.08f, 0.38f, 0.30f, 0.62f))
        ).setEnabled(true).build()

        val rightSlot = ComplicationSlot.createRoundRectComplicationSlotBuilder(
            RIGHT_COMPLICATION_ID,
            CanvasComplicationFactory { watchState, invalidateCallback ->
                CanvasComplicationDrawable(
                    ComplicationDrawable(this@BTCWatchFaceService),
                    watchState,
                    invalidateCallback
                )
            },
            listOf(
                ComplicationType.SHORT_TEXT,
                ComplicationType.SMALL_IMAGE,
                ComplicationType.RANGED_VALUE,
                ComplicationType.MONOCHROMATIC_IMAGE
            ),
            DefaultComplicationDataSourcePolicy(
                SystemDataSources.DATA_SOURCE_STEP_COUNT,
                ComplicationType.SHORT_TEXT
            ),
            ComplicationSlotBounds(RectF(0.70f, 0.38f, 0.92f, 0.62f))
        ).setEnabled(true).build()

        return ComplicationSlotsManager(listOf(leftSlot, rightSlot), currentUserStyleRepository)
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
