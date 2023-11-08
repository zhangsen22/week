package me.wxc.widget.schedule.components

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.RectF
import android.view.MotionEvent
import me.wxc.widget.ScheduleConfig
import me.wxc.widget.base.CoincidentDailyTaskModel
import me.wxc.widget.base.DailyTaskModel
import me.wxc.widget.base.IScheduleComponent
import me.wxc.widget.schedule.clockWidth
import me.wxc.widget.schedule.dateLineHeight
import me.wxc.widget.schedule.dayWidth
import me.wxc.widget.schedule.originRect
import me.wxc.widget.schedule.scheduleBgColor
import me.wxc.widget.schedule.scheduleBgColorExpired
import me.wxc.widget.schedule.scheduleLineColor
import me.wxc.widget.schedule.scheduleLineColorExpired
import me.wxc.widget.schedule.scheduleTextColor
import me.wxc.widget.schedule.scheduleTextColorExpired
import me.wxc.widget.schedule.scheduleTextSize
import me.wxc.widget.tools.dp
import me.wxc.widget.tools.drawText
import me.wxc.widget.tools.quarterMillis
import me.wxc.widget.tools.screenHeight
import me.wxc.widget.tools.screenWidth

/**
 * 重叠的普通日程
 */
class CoincidentDailyTaskComponent(override val model: CoincidentDailyTaskModel) :
    IScheduleComponent<CoincidentDailyTaskModel> {
    override val originRect: RectF = originRect()
    override val drawingRect: RectF = originRect()
    private val paddingVertical = 0.5f.dp
    private val paddingHorizontal = 0.5f.dp
    private val bgRadius = 4f.dp
    private val strokeWidth = 2f.dp
    private val textSize = scheduleTextSize
    private val textPadding = 6f.dp
    private val anchorPoint: Point = Point()
    private var fullDayHeight = 0f
    private val DailyTaskModel.bgColor: Int
        get() = if (expired) {
            scheduleBgColorExpired
        } else {
            scheduleBgColor
        }
    private val DailyTaskModel.textColor: Int
        get() = if (expired) {
            scheduleTextColorExpired
        } else {
            scheduleTextColor
        }

    private val DailyTaskModel.lineColor: Int
        get() = if (expired) {
            scheduleLineColorExpired
        } else {
            scheduleLineColor
        }

    private var parentWidth = screenWidth
    private var parentHeight = screenHeight

    override fun onDraw(canvas: Canvas, paint: Paint) {
        canvas.save()
        canvas.clipRect(clockWidth, dateLineHeight + fullDayHeight, parentWidth.toFloat(), parentHeight.toFloat())
        model.models.forEach {
            if (it.id > 0) {
                it.drawExistsTask(canvas, paint)
            }
        }
        canvas.restore()
    }

    private fun DailyTaskModel.drawExistsTask(canvas: Canvas, paint: Paint) {
        val railIndex = model.rails.filter { it.value.contains(this) }.keys.first()
        val railCount = model.rails.count()
        val width = dayWidth / railCount
        parentWidth = canvas.width
        parentHeight = canvas.height
        if (railIndex >= ScheduleConfig.maxCoincidentRailCount) return
        val drawRect = originRect().apply {
            left += anchorPoint.x + railIndex * width
            right = left + width
            top += anchorPoint.y + fullDayHeight
            bottom += anchorPoint.y + fullDayHeight
        }
        if (drawRect.bottom < dateLineHeight) return
        // 背景
        paint.color = bgColor
        canvas.drawRoundRect(
            drawRect.left + paddingHorizontal,
            drawRect.top + paddingVertical,
            drawRect.right - paddingHorizontal,
            drawRect.bottom - paddingVertical,
            bgRadius,
            bgRadius,
            paint
        )
        // 标线
        paint.color = lineColor
        paint.style = Paint.Style.FILL
        paint.strokeWidth = strokeWidth
        val startX = drawRect.left + strokeWidth / 2 + paddingHorizontal
        canvas.drawLine(
            startX, drawRect.top + paddingVertical, startX, drawRect.bottom - paddingVertical, paint
        )

        // 文字
        paint.textSize = textSize
        paint.color = textColor
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(
            title,
            drawRect,
            textPadding,
            paddingHorizontal,
            paint,
            vertical = true
        )
    }

    override fun updateDrawingRect(anchorPoint: Point, fullDayHeight: Float) {
        drawingRect.left = originRect.left + anchorPoint.x
        drawingRect.right = originRect.right + anchorPoint.x
        drawingRect.top = originRect.top + anchorPoint.y + fullDayHeight
        drawingRect.bottom = originRect.bottom + anchorPoint.y + fullDayHeight
        this.anchorPoint.x = anchorPoint.x
        this.anchorPoint.y = anchorPoint.y
        this.fullDayHeight = fullDayHeight
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        return true
    }
}