package com.example.android.wearable.alpha

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.text.TextPaint
import android.util.Log
import android.view.SurfaceHolder
import androidx.core.graphics.withRotation
import androidx.core.graphics.withScale
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.WatchFaceLayer
import com.example.android.wearable.alpha.data.watchface.ColorStyleIdAndResourceIds
import com.example.android.wearable.alpha.data.watchface.WatchFaceColorPalette.Companion.convertToWatchFaceColorPalette
import com.example.android.wearable.alpha.data.watchface.WatchFaceData
import com.example.android.wearable.alpha.utils.COLOR_STYLE_SETTING
import com.example.android.wearable.alpha.utils.DRAW_HOUR_PIPS_STYLE_SETTING
import com.example.android.wearable.alpha.utils.WATCH_HAND_LENGTH_STYLE_SETTING
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

private const val FRAME_PERIOD_MS_DEFAULT: Long = 16L
private const val TAG = "AnalogWatchCanvasRenderer"
private const val GESTURE_THRESHOLD_GRAVITY = 1.2f
private const val GESTURE_SLOP_TIME_MS = 500
private const val GESTURE_COUNT_RESET_TIME_MS = 3000

class AnalogWatchCanvasRenderer(
    private val context: Context,
    surfaceHolder: SurfaceHolder,
    watchState: WatchState,
    private val complicationSlotsManager: ComplicationSlotsManager,
    currentUserStyleRepository: CurrentUserStyleRepository,
    canvasType: Int
) : Renderer.CanvasRenderer2<AnalogWatchCanvasRenderer.AnalogSharedAssets>(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    canvasType,
    FRAME_PERIOD_MS_DEFAULT,
    clearWithBackgroundTintBeforeRenderingHighlightLayer = false
) {
    class AnalogSharedAssets : SharedAssets {
        override fun onDestroy() {}
    }

    private val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var watchFaceData: WatchFaceData = WatchFaceData()

    private var watchFaceColors = convertToWatchFaceColorPalette(
        context,
        watchFaceData.activeColorStyle,
        watchFaceData.ambientColorStyle
    )

    private val clockHandPaint = Paint().apply {
        isAntiAlias = true
        strokeWidth =
            context.resources.getDimensionPixelSize(R.dimen.clock_hand_stroke_width).toFloat()
    }

    private val outerElementPaint = Paint().apply {
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        isAntiAlias = true
        textSize = context.resources.getDimensionPixelSize(R.dimen.hour_mark_size).toFloat()
    }

    private lateinit var hourHandFill: Path
    private lateinit var hourHandBorder: Path
    private lateinit var minuteHandFill: Path
    private lateinit var minuteHandBorder: Path
    private lateinit var secondHand: Path

    private var armLengthChangedRecalculateClockHands: Boolean = false

    private var currentWatchFaceSize = Rect(0, 0, 0, 0)

    private var showMenu = false
    private var gestureTime: Long = 0
    private var lastGestureTimestamp: Long = 0
    private var gestureCount: Int = 0

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val sensorListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val gForce = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH

                if (gForce > GESTURE_THRESHOLD_GRAVITY) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastGestureTimestamp > GESTURE_SLOP_TIME_MS) {
                        if (currentTime - lastGestureTimestamp > GESTURE_COUNT_RESET_TIME_MS) {
                            gestureCount = 0
                        }
                        lastGestureTimestamp = currentTime
                        gestureCount++

                        if (gestureCount >= 2) {
                            gestureTime = currentTime
                            showMenu = true
                            scope.launch {
                                delay(10000)
                                showMenu = false
                                invalidate()
                            }
                            gestureCount = 0
                        }
                    }
                }
            } else if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
                val rotationRateX = event.values[0]
                val rotationRateY = event.values[1]
                val rotationRateZ = event.values[2]

                val rotationRate = sqrt(rotationRateX * rotationRateX + rotationRateY * rotationRateY + rotationRateZ * rotationRateZ)

                if (rotationRate > GESTURE_THRESHOLD_GRAVITY) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastGestureTimestamp > GESTURE_SLOP_TIME_MS) {
                        if (currentTime - lastGestureTimestamp > GESTURE_COUNT_RESET_TIME_MS) {
                            gestureCount = 0
                        }
                        lastGestureTimestamp = currentTime
                        gestureCount++

                        if (gestureCount >= 2) {
                            gestureTime = currentTime
                            showMenu = true
                            scope.launch {
                                delay(10000)
                                showMenu = false
                                invalidate()
                            }
                            gestureCount = 0
                        }
                    }
                }
            }
        }
    }

    init {
        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(sensorListener, gyroscope, SensorManager.SENSOR_DELAY_UI)

        scope.launch {
            currentUserStyleRepository.userStyle.collect { userStyle ->
                updateWatchFaceData(userStyle)
            }
        }

        scope.launch {
            weeklyMenu = fetchWeeklyLunchMenu()
            invalidate()
        }

        scheduleMenuFetch()
    }

    override suspend fun createSharedAssets(): AnalogSharedAssets {
        return AnalogSharedAssets()
    }

    private fun updateWatchFaceData(userStyle: UserStyle) {
        Log.d(TAG, "updateWatchFace(): $userStyle")

        var newWatchFaceData: WatchFaceData = watchFaceData

        for (options in userStyle) {
            when (options.key.id.toString()) {
                COLOR_STYLE_SETTING -> {
                    val listOption = options.value as
                        UserStyleSetting.ListUserStyleSetting.ListOption

                    newWatchFaceData = newWatchFaceData.copy(
                        activeColorStyle = ColorStyleIdAndResourceIds.getColorStyleConfig(
                            listOption.id.toString()
                        )
                    )
                }
                DRAW_HOUR_PIPS_STYLE_SETTING -> {
                    val booleanValue = options.value as
                        UserStyleSetting.BooleanUserStyleSetting.BooleanOption

                    newWatchFaceData = newWatchFaceData.copy(
                        drawHourPips = booleanValue.value
                    )
                }
                WATCH_HAND_LENGTH_STYLE_SETTING -> {
                    val doubleValue = options.value as
                        UserStyleSetting.DoubleRangeUserStyleSetting.DoubleRangeOption

                    armLengthChangedRecalculateClockHands = true

                    val newMinuteHandDimensions = newWatchFaceData.minuteHandDimensions.copy(
                        lengthFraction = doubleValue.value.toFloat()
                    )

                    newWatchFaceData = newWatchFaceData.copy(
                        minuteHandDimensions = newMinuteHandDimensions
                    )
                }
            }
        }

        if (watchFaceData != newWatchFaceData) {
            watchFaceData = newWatchFaceData

            watchFaceColors = convertToWatchFaceColorPalette(
                context,
                watchFaceData.activeColorStyle,
                watchFaceData.ambientColorStyle
            )

            for ((_, complication) in complicationSlotsManager.complicationSlots) {
                ComplicationDrawable.getDrawable(
                    context,
                    watchFaceColors.complicationStyleDrawableId
                )?.let {
                    (complication.renderer as CanvasComplicationDrawable).drawable = it
                }
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        scope.cancel("AnalogWatchCanvasRenderer scope clear() request")
        sensorManager.unregisterListener(sensorListener)
        super.onDestroy()
    }

    override fun renderHighlightLayer(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: AnalogSharedAssets
    ) {
        canvas.drawColor(renderParameters.highlightLayer!!.backgroundTint)

        for ((_, complication) in complicationSlotsManager.complicationSlots) {
            if (complication.enabled) {
                complication.renderHighlightLayer(canvas, zonedDateTime, renderParameters)
            }
        }
    }

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: AnalogSharedAssets
    ) {
        val backgroundColor = if (renderParameters.drawMode == DrawMode.AMBIENT) {
            watchFaceColors.ambientBackgroundColor
        } else {
            watchFaceColors.activeBackgroundColor
        }

        canvas.drawColor(backgroundColor)

        drawComplications(canvas, zonedDateTime)

        if (renderParameters.watchFaceLayers.contains(WatchFaceLayer.COMPLICATIONS_OVERLAY)) {
            drawClockHands(canvas, bounds, zonedDateTime)
        }

        if (renderParameters.drawMode == DrawMode.INTERACTIVE &&
            renderParameters.watchFaceLayers.contains(WatchFaceLayer.BASE) &&
            watchFaceData.drawHourPips
        ) {
            drawNumberStyleOuterElement(
                canvas,
                bounds,
                watchFaceData.numberRadiusFraction,
                watchFaceData.numberStyleOuterCircleRadiusFraction,
                watchFaceColors.activeOuterElementColor,
                watchFaceData.numberStyleOuterCircleRadiusFraction,
                watchFaceData.gapBetweenOuterCircleAndBorderFraction
            )
        }

        canvas.drawText(
            context.getString(R.string.university_name),
            bounds.exactCenterX(),
            bounds.exactCenterY() - bounds.width() / 4 - 10,
            textUniversity
        )
        canvas.drawText(
            context.getString(R.string.department_name),
            bounds.exactCenterX(),
            bounds.exactCenterY() - bounds.width() / 4 + 20,
            textUniversity
        )

        val currentDate = ZonedDateTime.now()
        val currentHour = currentDate.hour
        val dayOfWeek = currentDate.dayOfWeek.value - 1
        val maxWidth = bounds.width() * 0.8f

        if (dayOfWeek in weeklyMenu.indices) {
            val (breakfastMenu, lunchMenu) = weeklyMenu[dayOfWeek]
            val (breakfastMain, breakfastDetail) = splitMenu(breakfastMenu)
            val (lunchMain, lunchDetail) = splitMenu(lunchMenu)

            when (currentHour) {
                in 6..9 -> {
                    drawMultilineText(canvas, breakfastMain, textLunchMenu, maxWidth, bounds.exactCenterX(), bounds.exactCenterY() + bounds.width() / 6)
                    if (showMenu) {
                        drawMultilineText(canvas, breakfastDetail, textLunchMenu, maxWidth, bounds.exactCenterX(), bounds.exactCenterY() + bounds.width() / 4)
                    }
                }
                in 10..12 -> {
                    drawMultilineText(canvas, lunchMain, textLunchMenu, maxWidth, bounds.exactCenterX(), bounds.exactCenterY() + bounds.width() / 6)
                    if (showMenu) {
                        drawMultilineText(canvas, lunchDetail, textLunchMenu, maxWidth, bounds.exactCenterX(), bounds.exactCenterY() + bounds.width() / 4)
                    }
                }
                else -> drawMultilineText(canvas, "오늘은 좋은 날.", textLunchMenu, maxWidth, bounds.exactCenterX(), bounds.exactCenterY() + bounds.width() / 6)
            }
        } else {
            drawMultilineText(canvas, "메뉴를 불러오는 중...", textLunchMenu, maxWidth, bounds.exactCenterX(), bounds.exactCenterY() + bounds.width() / 6)
        }
    }

    private fun drawComplications(canvas: Canvas, zonedDateTime: ZonedDateTime) {
        for ((_, complication) in complicationSlotsManager.complicationSlots) {
            if (complication.enabled) {
                complication.render(canvas, zonedDateTime, renderParameters)
            }
        }
    }

    private fun drawClockHands(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime
    ) {
        if (currentWatchFaceSize != bounds || armLengthChangedRecalculateClockHands) {
            armLengthChangedRecalculateClockHands = false
            currentWatchFaceSize = bounds
            recalculateClockHands(bounds)
        }

        val secondOfDay = zonedDateTime.toLocalTime().toSecondOfDay()

        val secondsPerHourHandRotation = Duration.ofHours(12).seconds
        val secondsPerMinuteHandRotation = Duration.ofHours(1).seconds

        val hourRotation = secondOfDay.rem(secondsPerHourHandRotation) * 360.0f /
            secondsPerHourHandRotation
        val minuteRotation = secondOfDay.rem(secondsPerMinuteHandRotation) * 360.0f /
            secondsPerMinuteHandRotation

        canvas.withScale(
            x = WATCH_HAND_SCALE,
            y = WATCH_HAND_SCALE,
            pivotX = bounds.exactCenterX(),
            pivotY = bounds.exactCenterY()
        ) {
            val drawAmbient = renderParameters.drawMode == DrawMode.AMBIENT

            clockHandPaint.style = if (drawAmbient) Paint.Style.STROKE else Paint.Style.FILL
            clockHandPaint.color = if (drawAmbient) {
                watchFaceColors.ambientPrimaryColor
            } else {
                watchFaceColors.activePrimaryColor
            }

            withRotation(hourRotation, bounds.exactCenterX(), bounds.exactCenterY()) {
                drawPath(hourHandBorder, clockHandPaint)
            }

            withRotation(minuteRotation, bounds.exactCenterX(), bounds.exactCenterY()) {
                drawPath(minuteHandBorder, clockHandPaint)
            }

            if (!drawAmbient) {
                clockHandPaint.color = watchFaceColors.activeSecondaryColor

                val secondsPerSecondHandRotation = Duration.ofMinutes(1).seconds
                val secondsRotation = secondOfDay.rem(secondsPerSecondHandRotation) * 360.0f /
                    secondsPerSecondHandRotation
                clockHandPaint.color = watchFaceColors.activeSecondaryColor

                withRotation(secondsRotation, bounds.exactCenterX(), bounds.exactCenterY()) {
                    drawPath(secondHand, clockHandPaint)
                }
            }
        }
    }

    private fun recalculateClockHands(bounds: Rect) {
        Log.d(TAG, "recalculateClockHands()")
        hourHandBorder =
            createClockHand(
                bounds,
                watchFaceData.hourHandDimensions.lengthFraction,
                watchFaceData.hourHandDimensions.widthFraction,
                watchFaceData.gapBetweenHandAndCenterFraction,
                watchFaceData.hourHandDimensions.xRadiusRoundedCorners,
                watchFaceData.hourHandDimensions.yRadiusRoundedCorners
            )
        hourHandFill = hourHandBorder

        minuteHandBorder =
            createClockHand(
                bounds,
                watchFaceData.minuteHandDimensions.lengthFraction,
                watchFaceData.minuteHandDimensions.widthFraction,
                watchFaceData.gapBetweenHandAndCenterFraction,
                watchFaceData.minuteHandDimensions.xRadiusRoundedCorners,
                watchFaceData.minuteHandDimensions.yRadiusRoundedCorners
            )
        minuteHandFill = minuteHandBorder

        secondHand =
            createClockHand(
                bounds,
                watchFaceData.secondHandDimensions.lengthFraction,
                watchFaceData.secondHandDimensions.widthFraction,
                watchFaceData.gapBetweenHandAndCenterFraction,
                watchFaceData.secondHandDimensions.xRadiusRoundedCorners,
                watchFaceData.secondHandDimensions.yRadiusRoundedCorners
            )
    }

    private fun createClockHand(
        bounds: Rect,
        length: Float,
        thickness: Float,
        gapBetweenHandAndCenter: Float,
        roundedCornerXRadius: Float,
        roundedCornerYRadius: Float
    ): Path {
        val width = bounds.width()
        val centerX = bounds.exactCenterX()
        val centerY = bounds.exactCenterY()
        val left = centerX - thickness / 2 * width
        val top = centerY - (gapBetweenHandAndCenter + length) * width
        val right = centerX + thickness / 2 * width
        val bottom = centerY - gapBetweenHandAndCenter * width
        val path = Path()

        if (roundedCornerXRadius != 0.0f || roundedCornerYRadius != 0.0f) {
            path.addRoundRect(
                left,
                top,
                right,
                bottom,
                roundedCornerXRadius,
                roundedCornerYRadius,
                Path.Direction.CW
            )
        } else {
            path.addRect(
                left,
                top,
                right,
                bottom,
                Path.Direction.CW
            )
        }
        return path
    }

    private fun drawNumberStyleOuterElement(
        canvas: Canvas,
        bounds: Rect,
        numberRadiusFraction: Float,
        outerCircleStokeWidthFraction: Float,
        outerElementColor: Int,
        numberStyleOuterCircleRadiusFraction: Float,
        gapBetweenOuterCircleAndBorderFraction: Float
    ) {
        val textBounds = Rect()
        textPaint.color = outerElementColor
        for (i in 0 until 4) {
            val rotation = 0.5f * (i + 1).toFloat() * Math.PI
            val dx = sin(rotation).toFloat() * numberRadiusFraction * bounds.width().toFloat()
            val dy = -cos(rotation).toFloat() * numberRadiusFraction * bounds.width().toFloat()
            textPaint.getTextBounds(HOUR_MARKS[i], 0, HOUR_MARKS[i].length, textBounds)
            canvas.drawText(
                HOUR_MARKS[i],
                bounds.exactCenterX() + dx - textBounds.width() / 2.0f,
                bounds.exactCenterY() + dy + textBounds.height() / 2.0f,
                textPaint
            )
        }

        outerElementPaint.strokeWidth = outerCircleStokeWidthFraction * bounds.width()
        outerElementPaint.color = outerElementColor
        canvas.save()
        for (i in 0 until 12) {
            if (i % 3 != 0) {
                drawTopMiddleCircle(
                    canvas,
                    bounds,
                    numberStyleOuterCircleRadiusFraction,
                    gapBetweenOuterCircleAndBorderFraction
                )
            }
            canvas.rotate(360.0f / 12.0f, bounds.exactCenterX(), bounds.exactCenterY())
        }
        canvas.restore()
    }

    private fun drawTopMiddleCircle(
        canvas: Canvas,
        bounds: Rect,
        radiusFraction: Float,
        gapBetweenOuterCircleAndBorderFraction: Float
    ) {
        outerElementPaint.style = Paint.Style.FILL_AND_STROKE

        val centerX = 0.5f * bounds.width().toFloat()
        val centerY = bounds.width() * (gapBetweenOuterCircleAndBorderFraction + radiusFraction)

        canvas.drawCircle(
            centerX,
            centerY,
            radiusFraction * bounds.width(),
            outerElementPaint
        )
    }

    companion object {
        private val HOUR_MARKS = arrayOf("3", "6", "9", "12")
        private const val WATCH_HAND_SCALE = 1.0f
        var weeklyMenu: List<Pair<String, String>> = listOf(Pair("메뉴를 불러오는 중...", "메뉴를 불러오는 중..."))
    }

    val textUniversity = Paint().apply {
        isAntiAlias = true
        textSize = context.resources.getDimension(R.dimen.mylunch_univeristy_size)
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }

    val textLunchMenu = Paint().apply {
        isAntiAlias = true
        textSize = context.resources.getDimension(R.dimen.mylunch_message_size)
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }

    private fun scheduleMenuFetch() {
        scheduleDailyTaskAt(8)
    }

    private fun scheduleDailyTaskAt(hour: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        var initialDelay = calendar.timeInMillis - System.currentTimeMillis()
        if (initialDelay < 0) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            initialDelay = calendar.timeInMillis - System.currentTimeMillis()
        }

        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(
            {
                scope.launch {
                    Log.d(TAG, "Fetching weekly menu at $hour:00")
                    weeklyMenu = fetchWeeklyLunchMenu()
                    Log.d(TAG, "Fetched weekly menu: $weeklyMenu")
                    invalidate()
                }
            },
            initialDelay,
            TimeUnit.DAYS.toMillis(1),
            TimeUnit.MILLISECONDS
        )
    }

    private suspend fun fetchWeeklyLunchMenu(): List<Pair<String, String>> {
        return withContext(Dispatchers.IO) {
            val formatter = DateTimeFormatter.ofPattern("MM.dd")
            val weeklyMenu = mutableListOf<Pair<String, String>>()

            for (i in 0 until 7) {
                val date = LocalDate.now().plusDays(i.toLong())
                val dateStr = date.format(formatter)

                try {
                    Log.d(TAG, "Fetching lunch menu for date: $dateStr")

                    val url = "https://www.mokpo.ac.kr/www/275/subview.do"
                    val document: Document = Jsoup.connect(url).get()

                    Log.d(TAG, "Fetched document")

                    val dlElements = document.select("dl:has(span.date:contains($dateStr))")

                    Log.d(TAG, "Found ${dlElements.size} dl elements for date: $dateStr")

                    var breakfastMenu = "휴일"
                    var lunchMenu = "휴일"

                    for (dlElement in dlElements) {
                        val contWrapElements = dlElement.select("dd .contWrap")
                        Log.d(TAG, "Found ${contWrapElements.size} contWrap elements in a dl element")
                        if (contWrapElements.size > 0) {
                            val mainDish = contWrapElements[0].select("div.main").text()
                            val menu = contWrapElements[0].select("div.menu").text()
                            Log.d(TAG, "Found first menu for $dateStr: $mainDish, $menu")
                            breakfastMenu = "아침 $mainDish, $menu"
                        }
                        if (contWrapElements.size > 1) {
                            val mainDish = contWrapElements[1].select("div.main").text()
                            val menu = contWrapElements[1].select("div.menu").text()
                            Log.d(TAG, "Found second menu for $dateStr: $mainDish, $menu")
                            lunchMenu = "점심 $mainDish, $menu"
                        }
                    }

                    weeklyMenu.add(Pair(breakfastMenu, lunchMenu))
                } catch (e: Exception) {
                    Log.e(TAG, "메뉴를 가져올 수 없습니다 for date: $dateStr. 1시간 후에 다시 시도합니다.", e)
                    delay(3600000)
                    return@withContext fetchWeeklyLunchMenu()
                }
            }
            return@withContext weeklyMenu
        }
    }

    private fun splitMenu(menu: String): Pair<String, String> {
        val parts = menu.split(", ", limit = 2)
        return if (parts.size == 2) {
            parts[0] to parts[1]
        } else {
            menu to ""
        }
    }

    private fun drawMultilineText(
        canvas: Canvas,
        text: String,
        textPaint: Paint,
        maxWidth: Float,
        startX: Float,
        startY: Float
    ) {
        val textPaint = TextPaint(textPaint).apply {
            textSize = context.resources.getDimension(R.dimen.mylunch_message_size)
        }
        val lines = ArrayList<String>()

        var start = 0
        var end: Int

        while (start < text.length) {
            end = textPaint.breakText(text, start, text.length, true, maxWidth, null)
            lines.add(text.substring(start, start + end))
            start += end
        }

        var y = startY
        for (line in lines) {
            canvas.drawText(line, startX, y, textPaint)
            y += textPaint.descent() - textPaint.ascent()
        }
    }
}
