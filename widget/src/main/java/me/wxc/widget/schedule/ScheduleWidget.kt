package me.wxc.widget.schedule

import android.animation.ValueAnimator
import android.graphics.PointF
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Scroller
import androidx.core.animation.doOnCancel
import androidx.core.graphics.toPoint
import androidx.core.view.doOnLayout
import me.wxc.widget.ScheduleConfig
import me.wxc.widget.base.*
import me.wxc.widget.schedule.components.DailyTaskComponent
import me.wxc.widget.schedule.components.CoincidentDailyTaskComponent
import me.wxc.widget.schedule.components.FullDaySingleComponent
import me.wxc.widget.tools.*
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class ScheduleWidget(override val render: IScheduleRender) : IScheduleWidget {
    private val MIN_SCROLL_Y = 0
    private val MAX_SCROLL_Y: Int
        get() = dayHeight.roundToInt() + headerHeight.roundToInt() + render.adapter.fullDayTotalHeight.toInt() - (render as View).height + (render as View).paddingTop + (render as View).paddingBottom
    private val MIN_SCROLL_X: Int
        get() = (dayWidth * (ScheduleConfig.scheduleBeginTime.dDays - nowMillis.dDays)).roundToInt()
    private val MAX_SCROLL_X: Int
        get() = (dayWidth * (ScheduleConfig.scheduleEndTime.dDays - nowMillis.dDays)).roundToInt()

    override var lunarEnable: Boolean = ScheduleConfig.lunarEnable
    override val headerHeight: Float
        get() = dateLineHeight
    override var selectedDayTime: Long by setter(nowMillis) { oldTime, newTime ->
        val time = newTime.calendar.firstDayOfWeekTime
        if (!scroller.isFinished || byDrag) {
            return@setter
        }
        val isNow = time % beginOfDay(time).timeInMillis != 0L
        if (isNow) {
            scrollTo((dayWidth * time.dDays).roundToInt(), initializedY(time))
            return@setter
        }
        if (time.dDays != oldTime.dDays) {
            scrollTo((dayWidth * time.dDays).roundToInt(), scrollY)
        }
    }

    override var focusedDayTime: Long by setter(-1) { _, time ->
    }

    override var scheduleModels: List<IScheduleModel> by setter(listOf()) { old, list ->
        if (old == list) return@setter
        render.adapter.models.clear()
        render.adapter.models.addAll(list)
        render.adapter.notifyModelsChanged()
        (render as? View)?.invalidate()
        if (loadingMore) {
            loadingMore = false
        }
    }

    override var beginTime: Long by setter(nowMillis) { _, time ->
        Log.i(TAG, "update beginTime: ${time.yyyyMMddHHmmss}")
    }
    override var endTime: Long by setter(nowMillis) { _, time ->
        Log.i(TAG, "update endTime: ${time.yyyyMMddHHmmss}")
    }

    private var scrollX: Int =
        (dayWidth * (selectedDayTime.dDays - selectedDayTime.calendar.firstDayOfWeekTime.dDays)).toInt()
    private var scrollY: Int = 0

    private val singleDayScroller: Scroller by lazy {
        Scroller((render as View).context) { ot ->
            var t = ot
            t -= 1.0f
            t * t * t * t * t + 1.0f
        }
    }
    private val threeDayScroller: Scroller by lazy {
        Scroller((render as View).context, DecelerateInterpolator(), false)
    }
    private val scroller: Scroller
        get() = singleDayScroller

    private val velocityTracker by lazy {
        VelocityTracker.obtain()
    }
    private val gestureDetector by lazy { createGestureDetector() }

    private var scrollHorizontal = false

    private var byDrag = false

    init {
        render.widget = this
        (render as View).doOnLayout {
            scrollY = initializedY()
            onScroll(scrollX, scrollY)
        }
        beginTime = beginOfDay().apply { add(Calendar.MONTH, -1) }.timeInMillis
        endTime = beginOfDay(beginTime).apply { add(Calendar.MONTH, 2) }.timeInMillis
        beginTime = beginTime
        endTime = endTime
        reloadSchedulesFromProvider()
    }

    private var downOnDateLine = false

    private fun createGestureDetector() =
        GestureDetector(
            (render as View).context,
            object : GestureDetector.SimpleOnGestureListener() {
                private var justDown = false
                override fun onDown(e: MotionEvent): Boolean {
                    justDown = true
                    if (!scroller.isFinished) {
                        scroller.abortAnimation()
                    }
                    return true
                }

                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    Log.i(TAG, "onSingleTapUp: ${e.action}")
                    if (e.y < headerHeight) return true
                    render.adapter.visibleComponents.mapNotNull { it as? FullDaySingleComponent }
                        .findLast { e.ifInRect(it.drawingRect) }?.let {
                            ScheduleConfig.onDailyTaskClickBlock(it.model)
                            return true
                        }
                    render.adapter.exceedFullDayComponents.findLast { e.ifInRect(it.drawingRect) }?.let {
                            ScheduleConfig.onMoreTaskClickBlock(it.model)
                            return true
                        }
                    if (e.y < headerHeight + render.adapter.fullDayTotalHeight) return true
                    render.adapter.visibleComponents.mapNotNull { it as? DailyTaskComponent }
                        .findLast { e.ifInRect(it.drawingRect) }?.let {
                            ScheduleConfig.onDailyTaskClickBlock(it.model)
                            return true
                        }
                    render.adapter.visibleComponents.mapNotNull { it as? CoincidentDailyTaskComponent }
                        .findLast { e.ifInRect(it.drawingRect) }?.let {
                            ScheduleConfig.onMoreTaskClickBlock(it.model)
                            return true
                        }
                    val downOnBody = e.x > clockWidth && e.y > headerHeight
                    if (downOnBody) {
                        ScheduleConfig.onCreateTaskClickBlock.invoke(
                            DailyTaskModel(
                                beginTime = PointF(e.x - dayWidth / 2, e.y)
                                    .toPoint()
                                    .positionToTime(
                                        scrollX,
                                        scrollY - render.adapter.fullDayTotalHeight.toInt()
                                    )
                                    .adjustTimestamp(quarterMillis, true),
                            )
                        )
                    }
                    return true
                }

                override fun onScroll(
                    e1: MotionEvent,
                    e2: MotionEvent,
                    distanceX: Float,
                    distanceY: Float
                ): Boolean {
                    if (justDown) {
                        scrollHorizontal = abs(distanceX) > abs(distanceY)
                    }
                    if (scrollHorizontal) {
                        scrollX += distanceX.toInt()
                        scrollX = scrollX.coerceAtMost(MAX_SCROLL_X).coerceAtLeast(MIN_SCROLL_X)
                        onScroll(scrollX, scrollY)
                    } else if (!downOnDateLine) {
                        scrollY += distanceY.toInt()
                        scrollY = scrollY.coerceAtMost(MAX_SCROLL_Y).coerceAtLeast(MIN_SCROLL_Y)
                        onScroll(scrollX, scrollY)
                    }
                    justDown = false
                    return true
                }
            })

    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        byDrag = motionEvent.action != MotionEvent.ACTION_UP
        velocityTracker.addMovement(motionEvent)
        val result: Boolean
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                downOnDateLine = motionEvent.y < headerHeight
                result = gestureDetector.onTouchEvent(motionEvent)
            }

            MotionEvent.ACTION_UP -> {
                result = gestureDetector.onTouchEvent(motionEvent)
                if (!(downOnDateLine && !scrollHorizontal) && !result) autoSnap()
                Log.i(TAG, "onTouchEvent: ${velocityTracker.xVelocity}")
            }

            else -> {
                result = gestureDetector.onTouchEvent(motionEvent)
            }
        }
        return result
    }

    private var loadingMore = false
    override fun onScroll(x: Int, y: Int) {
        render.render(-x, -y)
        val startDay = -(-x / dayWidth).toInt() - 1
        val endDay = startDay + 1 + (screenWidth / dayWidth).toInt()
        if (startDay < beginTime.dDays + 15) {
            beginTime = min(beginTime.calendar.apply {
                add(Calendar.MONTH, -1)
            }.timeInMillis.apply {
                Log.i(TAG, "onScroll: load previous month: $yyyyM")
            }, beginOfDay().timeInMillis + startDay * dayMillis)
            if (!loadingMore) {
                loadingMore = true
                reloadSchedulesFromProvider()
            }
        } else if (endDay > endTime.dDays - 15) {
            endTime = max(endTime.calendar.apply {
                add(Calendar.MONTH, 1)
            }.timeInMillis.apply {
                Log.i(TAG, "onScroll: load next month: $yyyyM")
            }, beginOfDay().timeInMillis + endDay * dayMillis)
            if (!loadingMore) {
                loadingMore = true
                reloadSchedulesFromProvider()
            }
        }
        val dDays = (scrollX / dayWidth).roundToInt()
        ScheduleConfig.onDateSelectedListener.invoke((beginOfDay().timeInMillis + dDays * dayMillis).calendar)
    }

    private fun autoSnap() {
        velocityTracker.computeCurrentVelocity(1000)
        if (scrollHorizontal) {
            // 自适应滑动结束位置
            autoSnapWeek()
        } else {
            scroller.fling(
                scrollX,
                scrollY,
                0,
                -velocityTracker.yVelocity.toInt(),
                Int.MIN_VALUE,
                Int.MAX_VALUE,
                Int.MIN_VALUE,
                Int.MAX_VALUE
            )
            scroller.finalX = ((scrollX / dayWidth).roundToInt() * dayWidth).roundToInt()
                .coerceAtMost(MAX_SCROLL_X).coerceAtLeast(MIN_SCROLL_X)
        }
        callOnScrolling(true, true)
    }

    // 周视图，滑动一页（七天）
    private fun autoSnapWeek() {
        val velocity = velocityTracker.xVelocity.toInt()
        if (!scroller.isFinished) {
            scroller.abortAnimation()
        }
        val currentDDays = selectedDayTime.dDays.toInt()
        val weekWidth = 7 * dayWidth
        val destDDays = if (velocity < -1000) { // 左滑
            currentDDays + 7
        } else if (velocity > 1000) { // 右滑
            currentDDays - 7
        } else {
            val s =
                (scrollX + (nowMillis.dDays - nowMillis.calendar.firstDayOfWeekTime.dDays) * dayWidth).toInt()
            (7 * ((1f * s / weekWidth).roundToInt()) - (nowMillis.dDays - nowMillis.calendar.firstDayOfWeekTime.dDays)).toInt()
        }
        val dx = (destDDays * dayWidth).roundToInt() - scrollX
        scroller.startScroll(
            scrollX,
            scrollY,
            dx,
            0,
            (abs(dx) - abs(velocity) / 100).coerceAtMost(400).coerceAtLeast(50)
        )
    }

    private fun callOnScrolling(byFling: Boolean, yUpdated: Boolean) {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 10_000
            val beginTime = nowMillis
            doOnCancel {
                notifySelectedDayTime()
            }
            addUpdateListener {
                if (!scroller.computeScrollOffset()) {
                    Log.i(
                        TAG,
                        "canceled in ${nowMillis - beginTime}, $scrollX"
                    )
                    cancel()
                    return@addUpdateListener
                }
                if (byFling) {
                    if (scrollHorizontal) {
                        scrollX = scroller.currX
                        onScroll(scrollX, scrollY)
                    } else {
                        scrollY =
                            scroller.currY.coerceAtMost(MAX_SCROLL_Y).coerceAtLeast(MIN_SCROLL_Y)
                        scrollX =
                            scroller.currX.coerceAtMost(MAX_SCROLL_X).coerceAtLeast(MIN_SCROLL_X)
                        onScroll(scrollX, scrollY)
                    }
                } else {
                    scrollX = scroller.currX.coerceAtMost(MAX_SCROLL_X).coerceAtLeast(MIN_SCROLL_X)
                    if (yUpdated) {
                        scrollY =
                            scroller.currY.coerceAtMost(MAX_SCROLL_Y).coerceAtLeast(MIN_SCROLL_Y)
                    }
                    onScroll(scrollX, scrollY)
                }
            }
        }.start()
    }

    override fun scrollTo(x: Int, y: Int, duration: Int) {
        if (!scroller.isFinished) {
            scroller.abortAnimation()
        }
        if (x == scrollX && y == scrollY) {
            (render as View).invalidate()
            return
        }
        val yUpdated = y != scrollY
        scroller.startScroll(scrollX, scrollY, x - scrollX, y - scrollY, duration)
        callOnScrolling(false, yUpdated)
    }

    override fun isScrolling(): Boolean {
        return !scroller.isFinished
    }

    override val parentRender: ICalendarRender?
        get() = (render as? View)?.parent as? ICalendarRender
    override val calendar: Calendar = beginOfDay()

    private fun initializedY(time: Long = nowMillis) = run {
        val millis = time - beginOfDay(time).timeInMillis
        val centerMillis =
            ((render as View).height / 2 - zeroClockY / 2 - (render as View).paddingTop) * hourMillis / clockHeight
        ((millis - centerMillis) * clockHeight / hourMillis).toInt().coerceAtMost(MAX_SCROLL_Y)
            .coerceAtLeast(MIN_SCROLL_Y)
    }

    private fun notifySelectedDayTime() {
        val dDays = (scrollX / dayWidth).roundToInt()
        if (rootCalendarRender?.selectedDayTime?.dDays?.toInt() != dDays) {
            rootCalendarRender?.selectedDayTime =
                beginOfDay().timeInMillis + dDays * dayMillis
            ScheduleConfig.onDateSelectedListener.invoke(beginOfDay(selectedDayTime))
        }
        val weedTime =
            (nowMillis + dDays * dayMillis).calendar.firstDayOfWeekTime
        ScheduleConfig.onWeekSelectedBlock(weedTime)
        render.adapter.changeFullDayTotalHeight(render as View)
        (render as View).invalidate()
    }

    companion object {
        private const val TAG = "ScheduleWidget"
    }
}

