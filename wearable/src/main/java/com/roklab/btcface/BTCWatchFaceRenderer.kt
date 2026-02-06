package com.roklab.btcface

import android.content.Context
import android.graphics.*
import android.view.SurfaceHolder
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleSetting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class BTCWatchFaceRenderer(
    private val context: Context,
    surfaceHolder: SurfaceHolder,
    watchState: WatchState,
    private val complicationSlotsManager: ComplicationSlotsManager,
    currentUserStyleRepository: CurrentUserStyleRepository
) : Renderer.CanvasRenderer2<BTCWatchFaceRenderer.Assets>(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    CanvasType.HARDWARE,
    interactiveDrawModeUpdateDelayMillis = 16L,
    clearWithBackgroundTintBeforeRenderingHighlightLayer = true
) {

    class Assets : Renderer.SharedAssets {
        override fun onDestroy() {}
    }

    // Theme colors - Bitcoin Gold (default)
    private var primaryColor = Color.parseColor("#F7931A")
    private var secondaryColor = Color.parseColor("#FFD700")
    private var accentColor = Color.parseColor("#FFAA00")
    private var bgCenterColor = Color.parseColor("#0E0E18")
    private var bgEdgeColor = Color.parseColor("#06060C")
    private var priceTextColor = Color.parseColor("#FFD700")

    private var showSeconds = true
    private var showPrice = true

    // Paints
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = primaryColor
    }
    private val hourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        strokeCap = Paint.Cap.ROUND
        color = primaryColor
    }
    private val minutePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        strokeCap = Paint.Cap.ROUND
        color = secondaryColor
    }
    private val secondPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        strokeCap = Paint.Cap.ROUND
        color = accentColor
    }
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = primaryColor
    }
    private val priceWindowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = primaryColor
    }
    private val priceTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        color = priceTextColor
        textSize = 28f
    }
    private val priceLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        color = secondaryColor
        textSize = 14f
    }
    private val ambientOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.WHITE
    }

    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(supervisorJob + Dispatchers.Main.immediate)

    init {
        scope.launch {
            currentUserStyleRepository.userStyle.collect { userStyle ->
                applyUserStyle(userStyle)
            }
        }
    }

    override fun onDestroy() {
        supervisorJob.cancel()
        super.onDestroy()
    }

    override suspend fun createSharedAssets(): Assets = Assets()

    override fun renderHighlightLayer(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: Assets
    ) {
        // Not needed
    }

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: Assets
    ) {
        val isAmbientMode = renderParameters.drawMode == DrawMode.AMBIENT
        val centerX = bounds.exactCenterX()
        val centerY = bounds.exactCenterY()
        val radius = min(bounds.width(), bounds.height()) / 2f - 10f

        drawBackground(canvas, bounds, isAmbientMode)
        if (!isAmbientMode) drawMarkers(canvas, centerX, centerY, radius)
        drawHands(canvas, centerX, centerY, radius, zonedDateTime, isAmbientMode)

        val centerRadius = 8f
        if (isAmbientMode) {
            ambientOutlinePaint.strokeWidth = 2f
            canvas.drawCircle(centerX, centerY, centerRadius, ambientOutlinePaint)
        } else {
            canvas.drawCircle(centerX, centerY, centerRadius, centerPaint)
        }

        if (showPrice) drawPriceWindow(canvas, centerX, centerY, radius, isAmbientMode)
    }

    private fun drawBackground(canvas: Canvas, bounds: Rect, isAmbientMode: Boolean) {
        if (isAmbientMode) {
            canvas.drawColor(Color.BLACK)
        } else {
            bgPaint.shader = RadialGradient(
                bounds.exactCenterX(),
                bounds.exactCenterY(),
                min(bounds.width(), bounds.height()) / 2f,
                intArrayOf(bgCenterColor, bgEdgeColor),
                floatArrayOf(0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(bounds, bgPaint)
        }
    }

    private fun drawMarkers(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        val markerDistance = radius - 15f
        for (i in 0..11) {
            val angle = (i * 30 - 90) * Math.PI / 180
            val x = centerX + (markerDistance * cos(angle)).toFloat()
            val y = centerY + (markerDistance * sin(angle)).toFloat()
            val markerSize = if (i % 3 == 0) 10f else 6f
            canvas.drawCircle(x, y, markerSize, markerPaint)
        }
    }

    private fun drawHands(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        zonedDateTime: ZonedDateTime,
        isAmbientMode: Boolean
    ) {
        val hour = zonedDateTime.hour % 12
        val minute = zonedDateTime.minute
        val second = zonedDateTime.second

        val hourAngle = ((hour + minute / 60f) * 30 - 90) * Math.PI / 180
        drawHand(canvas, centerX, centerY, (radius * 0.5f), hourAngle,
            if (isAmbientMode) ambientOutlinePaint else hourPaint,
            if (isAmbientMode) 4f else 10f)

        val minuteAngle = ((minute + second / 60f) * 6 - 90) * Math.PI / 180
        drawHand(canvas, centerX, centerY, (radius * 0.7f), minuteAngle,
            if (isAmbientMode) ambientOutlinePaint else minutePaint,
            if (isAmbientMode) 3f else 7f)

        if (showSeconds && !isAmbientMode) {
            val secondAngle = (second * 6 - 90) * Math.PI / 180
            drawHand(canvas, centerX, centerY, (radius * 0.8f), secondAngle, secondPaint, 4f)
        }
    }

    private fun drawHand(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        length: Float,
        angle: Double,
        paint: Paint,
        strokeWidth: Float
    ) {
        val endX = centerX + (length * cos(angle)).toFloat()
        val endY = centerY + (length * sin(angle)).toFloat()
        paint.strokeWidth = strokeWidth
        canvas.drawLine(centerX, centerY, endX, endY, paint)
    }

    private fun drawPriceWindow(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        isAmbientMode: Boolean
    ) {
        if (isAmbientMode) return

        val windowTop = centerY + radius - 60f
        val windowBottom = centerY + radius - 10f
        val windowLeft = centerX - 50f
        val windowRight = centerX + 50f

        canvas.drawRoundRect(
            RectF(windowLeft, windowTop, windowRight, windowBottom), 8f, 8f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = bgCenterColor }
        )
        canvas.drawRoundRect(
            RectF(windowLeft, windowTop, windowRight, windowBottom), 8f, 8f, priceWindowPaint
        )

        val priceFormatted = BTCDataLayerListener.getCachedPriceFormatted(context)
        canvas.drawText("BTC", centerX, windowTop + 18f, priceLabelPaint)
        canvas.drawText(priceFormatted, centerX, windowBottom - 8f, priceTextPaint)
    }

    @Suppress("UNCHECKED_CAST")
    private fun applyUserStyle(userStyle: UserStyle) {
        // Get color theme - default to gold
        var colorThemeId = "gold"
        
        // Use selectedOptions which returns Map<UserStyleSetting, UserStyleSetting.Option>
        val selectedOptions = userStyle.selectedOptions
        for ((setting, option) in selectedOptions) {
            when (setting.id.value) {
                "color_theme" -> {
                    (option as? UserStyleSetting.ListUserStyleSetting.ListOption)?.let {
                        colorThemeId = it.id.value.toString()
                    }
                }
                "show_seconds" -> {
                    (option as? UserStyleSetting.BooleanUserStyleSetting.BooleanOption)?.let {
                        showSeconds = it.value
                    }
                }
                "show_price" -> {
                    (option as? UserStyleSetting.BooleanUserStyleSetting.BooleanOption)?.let {
                        showPrice = it.value
                    }
                }
            }
        }

        when (colorThemeId) {
            "silver" -> {
                primaryColor = Color.parseColor("#C0C0C0")
                secondaryColor = Color.parseColor("#E8E8E8")
                accentColor = Color.parseColor("#A0A0B0")
                priceTextColor = Color.parseColor("#E0E0E0")
            }
            "green" -> {
                primaryColor = Color.parseColor("#00E676")
                secondaryColor = Color.parseColor("#76FF03")
                accentColor = Color.parseColor("#00C853")
                priceTextColor = Color.parseColor("#76FF03")
            }
            "blue" -> {
                primaryColor = Color.parseColor("#40C4FF")
                secondaryColor = Color.parseColor("#80D8FF")
                accentColor = Color.parseColor("#0091EA")
                priceTextColor = Color.parseColor("#80D8FF")
            }
            else -> {
                primaryColor = Color.parseColor("#F7931A")
                secondaryColor = Color.parseColor("#FFD700")
                accentColor = Color.parseColor("#FFAA00")
                priceTextColor = Color.parseColor("#FFD700")
            }
        }

        markerPaint.color = primaryColor
        hourPaint.color = primaryColor
        minutePaint.color = secondaryColor
        secondPaint.color = accentColor
        centerPaint.color = primaryColor
        priceWindowPaint.color = primaryColor
        priceTextPaint.color = priceTextColor
        priceLabelPaint.color = secondaryColor
    }

    fun onPriceUpdated() {
        invalidate()
    }
}
