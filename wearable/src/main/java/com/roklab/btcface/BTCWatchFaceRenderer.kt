package com.roklab.btcface

import android.content.Context
import android.graphics.*
import android.view.SurfaceHolder
import androidx.wear.watchface.*
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleSetting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.ZonedDateTime
import java.util.Locale
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
    private var primaryColor = Color.parseColor("#F7931A")     // Bitcoin Orange
    private var secondaryColor = Color.parseColor("#FFD700")   // Gold
    private var accentColor = Color.parseColor("#FFAA00")      // Amber
    private var bgCenterColor = Color.parseColor("#0E0E18")    // Dark
    private var bgEdgeColor = Color.parseColor("#06060C")      // Darker
    private var logoTintColor = Color.parseColor("#1A1510")    // Brown tint
    private var priceTextColor = Color.parseColor("#FFD700")   // Gold text

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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        scope.launch {
            currentUserStyleRepository.userStyle.collect { userStyle ->
                applyUserStyle(userStyle)
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun renderHighlightLayer(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        uid: String,
        complications: Map<Int, ComplicationData>,
        renderParameters: RenderParameters
    ) {
        // Not needed for this watch face
    }

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        uid: String,
        complications: Map<Int, ComplicationData>,
        renderParameters: RenderParameters
    ) {
        val isAmbientMode = renderParameters.watchFaceLayer == WatchFaceLayer.BASE &&
                renderParameters.drawMode == DrawMode.AMBIENT

        val centerX = bounds.exactCenterX()
        val centerY = bounds.exactCenterY()
        val radius = min(bounds.width(), bounds.height()) / 2f - 10f

        // Draw background
        drawBackground(canvas, bounds, isAmbientMode)

        // Draw hour markers
        if (!isAmbientMode) {
            drawMarkers(canvas, centerX, centerY, radius)
        }

        // Draw hands
        drawHands(canvas, centerX, centerY, radius, zonedDateTime, isAmbientMode)

        // Draw center dot
        val centerRadius = 8f
        if (isAmbientMode) {
            ambientOutlinePaint.apply { style = Paint.Style.STROKE; strokeWidth = 2f }
            canvas.drawCircle(centerX, centerY, centerRadius, ambientOutlinePaint)
        } else {
            canvas.drawCircle(centerX, centerY, centerRadius, centerPaint)
        }

        // Draw BTC price display
        if (showPrice) {
            drawPriceWindow(canvas, centerX, centerY, radius, isAmbientMode)
        }

        // Draw complications
        complicationSlotsManager.renderComplications(canvas, zonedDateTime, renderParameters)
    }

    private fun drawBackground(canvas: Canvas, bounds: Rect, isAmbientMode: Boolean) {
        if (isAmbientMode) {
            canvas.drawColor(Color.BLACK)
        } else {
            // Gradient background effect with radial paint
            bgPaint.apply {
                shader = RadialGradient(
                    bounds.exactCenterX(),
                    bounds.exactCenterY(),
                    min(bounds.width(), bounds.height()) / 2f,
                    intArrayOf(bgCenterColor, bgEdgeColor),
                    floatArrayOf(0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(bounds, bgPaint)
        }
    }

    private fun drawMarkers(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        val markerDistance = radius - 15f
        for (i in 0..11) {
            val angle = (i * 30 - 90) * Math.PI / 180
            val x = centerX + (markerDistance * cos(angle)).toFloat()
            val y = centerY + (markerDistance * sin(angle)).toFloat()
            
            // Draw large marker at 12, 3, 6, 9 o'clock
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

        // Hour hand
        val hourAngle = ((hour + minute / 60f) * 30 - 90) * Math.PI / 180
        drawHand(
            canvas, centerX, centerY,
            (radius * 0.5).toFloat(),
            hourAngle,
            if (isAmbientMode) ambientOutlinePaint else hourPaint,
            if (isAmbientMode) 4f else 10f
        )

        // Minute hand
        val minuteAngle = ((minute + second / 60f) * 6 - 90) * Math.PI / 180
        drawHand(
            canvas, centerX, centerY,
            (radius * 0.7).toFloat(),
            minuteAngle,
            if (isAmbientMode) ambientOutlinePaint else minutePaint,
            if (isAmbientMode) 3f else 7f
        )

        // Second hand
        if (showSeconds && !isAmbientMode) {
            val secondAngle = (second * 6 - 90) * Math.PI / 180
            drawHand(
                canvas, centerX, centerY,
                (radius * 0.8).toFloat(),
                secondAngle,
                secondPaint,
                4f
            )
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
        // Price window at 6 o'clock
        val windowTop = centerY + radius - 60f
        val windowBottom = centerY + radius - 10f
        val windowLeft = centerX - 50f
        val windowRight = centerX + 50f

        if (!isAmbientMode) {
            // Draw price window background
            canvas.drawRoundRect(
                RectF(windowLeft, windowTop, windowRight, windowBottom),
                8f, 8f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = bgCenterColor
                }
            )
            
            // Draw border
            canvas.drawRoundRect(
                RectF(windowLeft, windowTop, windowRight, windowBottom),
                8f, 8f,
                priceWindowPaint
            )

            // Get cached price from Data Layer
            val priceFormatted = BTCDataLayerListener.getCachedPriceFormatted(context)
            
            // Draw label
            canvas.drawText("BTC", centerX, windowTop + 18f, priceLabelPaint)
            
            // Draw price
            canvas.drawText(priceFormatted, centerX, windowBottom - 8f, priceTextPaint)
        }
    }

    private fun applyUserStyle(userStyle: UserStyle) {
        val colorThemeId = userStyle["color_theme"].toString()
        when (colorThemeId) {
            "silver" -> {
                primaryColor = Color.parseColor("#C0C0C0")
                secondaryColor = Color.parseColor("#E8E8E8")
                accentColor = Color.parseColor("#A0A0B0")
                priceTextColor = Color.parseColor("#E0E0E0")
                logoTintColor = Color.parseColor("#14141A")
            }
            "green" -> {
                primaryColor = Color.parseColor("#00E676")
                secondaryColor = Color.parseColor("#76FF03")
                accentColor = Color.parseColor("#00C853")
                priceTextColor = Color.parseColor("#76FF03")
                logoTintColor = Color.parseColor("#0C1A10")
            }
            "blue" -> {
                primaryColor = Color.parseColor("#40C4FF")
                secondaryColor = Color.parseColor("#80D8FF")
                accentColor = Color.parseColor("#0091EA")
                priceTextColor = Color.parseColor("#80D8FF")
                logoTintColor = Color.parseColor("#0C1420")
            }
            else -> { // gold
                primaryColor = Color.parseColor("#F7931A")
                secondaryColor = Color.parseColor("#FFD700")
                accentColor = Color.parseColor("#FFAA00")
                priceTextColor = Color.parseColor("#FFD700")
                logoTintColor = Color.parseColor("#1A1510")
            }
        }

        showSeconds = userStyle["show_seconds"].toString() != "false"
        showPrice = userStyle["show_price"].toString() != "false"

        // Update paint colors
        markerPaint.color = primaryColor
        hourPaint.color = primaryColor
        minutePaint.color = secondaryColor
        secondPaint.color = accentColor
        centerPaint.color = primaryColor
        priceWindowPaint.color = primaryColor
        priceTextPaint.color = priceTextColor
        priceLabelPaint.color = secondaryColor
    }
}
