package com.roklab.btcface

import android.content.Context
import android.graphics.*
import android.view.SurfaceHolder
import androidx.wear.watchface.*
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.BooleanUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ListUserStyleSetting
import com.roklab.btcface.theme.WatchFaceColorScheme
import com.roklab.btcface.theme.WatchFaceColors
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

class BTCCanvasRenderer(
    private val context: Context,
    surfaceHolder: SurfaceHolder,
    watchState: WatchState,
    private val complicationSlotsManager: ComplicationSlotsManager,
    currentUserStyleRepository: CurrentUserStyleRepository
) : Renderer.CanvasRenderer2<BTCCanvasRenderer.Assets>(
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

    // ── State ──────────────────────────────────────────────
    private var colorScheme: WatchFaceColorScheme = WatchFaceColors.BITCOIN_GOLD
    private var showSeconds = true
    private var showPrice = true
    private var showMarkers = true
    private var btcPrice = 0.0
    private var lastPriceRefresh = 0L

    // ── Paints ─────────────────────────────────────────────
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val markerBigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val hourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL; strokeCap = Paint.Cap.ROUND
    }
    private val minutePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL; strokeCap = Paint.Cap.ROUND
    }
    private val secondPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL; strokeCap = Paint.Cap.ROUND
    }
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val priceWindowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val priceTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }
    private val priceLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
    }
    private val ambientOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
    }

    // ── Reusable objects ───────────────────────────────────
    private val handRect = RectF()
    private val priceRect = RectF()
    private val priceFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }

    private val bitcoinLogo = context.getDrawable(R.drawable.ic_bitcoin_logo)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // ── Init ───────────────────────────────────────────────
    init {
        BTCPriceWorker.schedule(context)
        refreshPrice()
        applyThemeColors()

        scope.launch {
            currentUserStyleRepository.userStyle.collect { onStyleChanged(it) }
        }
    }

    // ── Style handling ─────────────────────────────────────
    private fun onStyleChanged(style: UserStyle) {
        val themeOpt = style[UserStyleSetting.Id("color_theme")]
        if (themeOpt is ListUserStyleSetting.ListOption) {
            colorScheme = WatchFaceColors.fromId(themeOpt.id.toString())
        }
        val secOpt = style[UserStyleSetting.Id("show_seconds")]
        if (secOpt is BooleanUserStyleSetting.BooleanOption) showSeconds = secOpt.value

        val priceOpt = style[UserStyleSetting.Id("show_price")]
        if (priceOpt is BooleanUserStyleSetting.BooleanOption) showPrice = priceOpt.value

        val markOpt = style[UserStyleSetting.Id("show_markers")]
        if (markOpt is BooleanUserStyleSetting.BooleanOption) showMarkers = markOpt.value

        applyThemeColors()
    }

    private fun applyThemeColors() {
        markerPaint.color = colorScheme.primary
        markerBigPaint.color = colorScheme.secondary
        hourPaint.color = colorScheme.primary
        minutePaint.color = colorScheme.secondary
        secondPaint.color = colorScheme.accent
        centerPaint.color = Color.WHITE
        priceWindowPaint.color = Color.argb(140, 0, 0, 0)
        priceTextPaint.color = colorScheme.priceText
        priceLabelPaint.color = Color.argb(160, 255, 255, 255)
        ambientOutlinePaint.color = Color.argb(180, 180, 180, 180)
    }

    private fun refreshPrice() {
        btcPrice = BTCPriceFetcher.getCachedPrice(context)
        lastPriceRefresh = System.currentTimeMillis()
    }

    // ── Rendering ──────────────────────────────────────────
    override suspend fun createSharedAssets(): Assets = Assets()

    override fun render(
        canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime, sharedAssets: Assets
    ) {
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val r = min(cx, cy)
        val ambient = renderParameters.drawMode == DrawMode.AMBIENT

        // Refresh cached price every 5 min
        if (System.currentTimeMillis() - lastPriceRefresh > 300_000) refreshPrice()

        drawBackground(canvas, cx, cy, r, ambient)
        drawBitcoinLogo(canvas, cx, cy, r, ambient)
        if (showMarkers) drawHourMarkers(canvas, cx, cy, r, ambient)
        drawComplications(canvas, bounds, zonedDateTime)
        if (showPrice && btcPrice > 0) drawPriceWindow(canvas, cx, cy, r, ambient)

        val hr = zonedDateTime.hour % 12
        val mn = zonedDateTime.minute
        val sc = zonedDateTime.second
        val ns = zonedDateTime.nano

        drawHourHand(canvas, cx, cy, r, hr, mn, ambient)
        drawMinuteHand(canvas, cx, cy, r, mn, sc, ambient)
        if (showSeconds && !ambient) drawSecondHand(canvas, cx, cy, r, sc, ns)
        drawCenterCap(canvas, cx, cy, r, ambient)
    }

    override fun renderHighlightLayer(
        canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime, sharedAssets: Assets
    ) {
        val highlightLayer = renderParameters.highlightLayer ?: return
        canvas.drawColor(highlightLayer.backgroundTint)
        for ((_, slot) in complicationSlotsManager.complicationSlots) {
            if (slot.enabled) {
                slot.renderer.drawHighlight(
                    canvas,
                    slot.computeBounds(bounds),
                    slot.boundsType,
                    zonedDateTime,
                    highlightLayer.highlightTint
                )
            }
        }
    }

    // ── Drawing helpers ────────────────────────────────────

    private fun drawBackground(canvas: Canvas, cx: Float, cy: Float, r: Float, ambient: Boolean) {
        if (ambient) {
            canvas.drawColor(Color.BLACK)
            return
        }
        bgPaint.shader = RadialGradient(
            cx, cy, r,
            intArrayOf(colorScheme.bgCenter, colorScheme.bgEdge),
            floatArrayOf(0.3f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, cx * 2, cy * 2, bgPaint)
    }

    private fun drawBitcoinLogo(canvas: Canvas, cx: Float, cy: Float, r: Float, ambient: Boolean) {
        bitcoinLogo?.let { logo ->
            val size = (r * 1.3f).toInt()
            val half = size / 2
            logo.setBounds(
                (cx - half).toInt(), (cy - half).toInt(),
                (cx + half).toInt(), (cy + half).toInt()
            )
            logo.alpha = if (ambient) 12 else 22
            logo.setTint(colorScheme.primary)

            canvas.save()
            canvas.rotate(14f, cx, cy)
            logo.draw(canvas)
            canvas.restore()

            // Reset tint so it doesn't leak
            logo.setTintList(null)
        }
    }

    private fun drawHourMarkers(canvas: Canvas, cx: Float, cy: Float, r: Float, ambient: Boolean) {
        val markerR = r * 0.88f
        for (i in 0 until 12) {
            val angle = Math.toRadians((i * 30 - 90).toDouble())
            val x = cx + markerR * cos(angle).toFloat()
            val y = cy + markerR * sin(angle).toFloat()

            if (i == 0) {
                // 12 o'clock — slightly larger
                markerBigPaint.alpha = if (ambient) 120 else 255
                canvas.drawCircle(x, y, r * 0.028f, markerBigPaint)
            } else {
                markerPaint.alpha = if (ambient) 80 else 200
                canvas.drawCircle(x, y, r * 0.018f, markerPaint)
            }
        }
    }

    private fun drawComplications(canvas: Canvas, screenBounds: Rect, zonedDateTime: ZonedDateTime) {
        for ((_, slot) in complicationSlotsManager.complicationSlots) {
            if (slot.enabled) {
                slot.renderer.render(
                    canvas,
                    slot.computeBounds(screenBounds),
                    zonedDateTime,
                    renderParameters,
                    slot.id
                )
            }
        }
    }

    private fun drawPriceWindow(canvas: Canvas, cx: Float, cy: Float, r: Float, ambient: Boolean) {
        val winW = r * 0.48f
        val winH = r * 0.18f
        val winY = cy + r * 0.48f

        priceRect.set(cx - winW / 2, winY - winH / 2, cx + winW / 2, winY + winH / 2)
        priceWindowPaint.alpha = if (ambient) 100 else 140
        canvas.drawRoundRect(priceRect, r * 0.04f, r * 0.04f, priceWindowPaint)

        val formattedPrice = "$${priceFormat.format(btcPrice)}"
        priceTextPaint.textSize = r * 0.095f
        priceTextPaint.alpha = if (ambient) 140 else 255
        canvas.drawText(formattedPrice, cx, winY + r * 0.035f, priceTextPaint)

        // Small "BTC" label above the price window
        priceLabelPaint.textSize = r * 0.055f
        priceLabelPaint.alpha = if (ambient) 80 else 160
        canvas.drawText("BTC", cx, winY - winH / 2 - r * 0.02f, priceLabelPaint)
    }

    // ── Hands ──────────────────────────────────────────────

    private fun drawHourHand(
        canvas: Canvas, cx: Float, cy: Float, r: Float,
        hour: Int, minute: Int, ambient: Boolean
    ) {
        val angle = (hour + minute / 60f) * 30f
        val length = r * 0.45f
        val width = r * 0.055f
        val tail = r * 0.10f

        canvas.save()
        canvas.rotate(angle, cx, cy)

        if (ambient) {
            ambientOutlinePaint.strokeWidth = r * 0.015f
            handRect.set(cx - width / 2, cy - length, cx + width / 2, cy + tail)
            canvas.drawRoundRect(handRect, width / 2, width / 2, ambientOutlinePaint)
        } else {
            hourPaint.setShadowLayer(r * 0.02f, 0f, r * 0.01f, Color.argb(80, 0, 0, 0))
            handRect.set(cx - width / 2, cy - length, cx + width / 2, cy + tail)
            canvas.drawRoundRect(handRect, width / 2, width / 2, hourPaint)
            hourPaint.clearShadowLayer()
        }
        canvas.restore()
    }

    private fun drawMinuteHand(
        canvas: Canvas, cx: Float, cy: Float, r: Float,
        minute: Int, second: Int, ambient: Boolean
    ) {
        val angle = (minute + second / 60f) * 6f
        val length = r * 0.68f
        val width = r * 0.038f
        val tail = r * 0.12f

        canvas.save()
        canvas.rotate(angle, cx, cy)

        if (ambient) {
            ambientOutlinePaint.strokeWidth = r * 0.012f
            handRect.set(cx - width / 2, cy - length, cx + width / 2, cy + tail)
            canvas.drawRoundRect(handRect, width / 2, width / 2, ambientOutlinePaint)
        } else {
            minutePaint.setShadowLayer(r * 0.015f, 0f, r * 0.008f, Color.argb(60, 0, 0, 0))
            handRect.set(cx - width / 2, cy - length, cx + width / 2, cy + tail)
            canvas.drawRoundRect(handRect, width / 2, width / 2, minutePaint)
            minutePaint.clearShadowLayer()
        }
        canvas.restore()
    }

    private fun drawSecondHand(
        canvas: Canvas, cx: Float, cy: Float, r: Float,
        second: Int, nano: Int
    ) {
        val angle = (second + nano / 1_000_000_000f) * 6f
        val length = r * 0.78f
        val width = r * 0.014f
        val tail = r * 0.18f

        canvas.save()
        canvas.rotate(angle, cx, cy)

        handRect.set(cx - width / 2, cy - length, cx + width / 2, cy + tail)
        canvas.drawRoundRect(handRect, width / 2, width / 2, secondPaint)

        canvas.restore()
    }

    private fun drawCenterCap(canvas: Canvas, cx: Float, cy: Float, r: Float, ambient: Boolean) {
        if (ambient) {
            ambientOutlinePaint.strokeWidth = r * 0.01f
            canvas.drawCircle(cx, cy, r * 0.03f, ambientOutlinePaint)
        } else {
            centerPaint.setShadowLayer(r * 0.01f, 0f, 0f, Color.argb(100, 0, 0, 0))
            canvas.drawCircle(cx, cy, r * 0.035f, centerPaint)
            centerPaint.clearShadowLayer()
        }
    }
}
