package me.wxc.widget.schedule.components

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.RectF
import android.view.MotionEvent
import me.wxc.widget.ScheduleConfig
import me.wxc.widget.base.DailyTaskModel
import me.wxc.widget.base.IScheduleComponent
import me.wxc.widget.schedule.clockWidth
import me.wxc.widget.schedule.dateLineHeight
import me.wxc.widget.schedule.dayWidth
import me.wxc.widget.schedule.fullDayBgColor
import me.wxc.widget.schedule.fullDayBgColorExpired
import me.wxc.widget.schedule.fullDayTextColor
import me.wxc.widget.schedule.fullDayTextColorExpired
import me.wxc.widget.schedule.fullDayTextSize
import me.wxc.widget.tools.dDays
import me.wxc.widget.tools.dp
import me.wxc.widget.tools.drawText
import me.wxc.widget.tools.nowMillis
import me.wxc.widget.tools.screenHeight
import me.wxc.widget.tools.screenWidth

/**
 * 全/跨天日程
 */
class FullDaySingleComponent(override val model: DailyTaskModel, val railIndex: Int) :
    IScheduleComponent<DailyTaskModel> {
    private val modelRectHeight = 20f.dp
    override val originRect: RectF = createOriginRect()
    override val drawingRect: RectF = createOriginRect()

    private fun createOriginRect() = run {
        val today = nowMillis.dDays
        val day = model.beginTime.dDays
        val left = clockWidth + (day - today) * dayWidth + paddingHorizontal
        val right = left + ((model.endTime - 1).dDays - day + 1) * dayWidth - paddingHorizontal
        val top = dateLineHeight
        val bottom = top + modelRectHeight
        RectF(left, top, right, bottom)
    }
    private val paddingVertical = 0.5f.dp
    private val paddingHorizontal = 0.5f.dp
    private val bgRadius = 2f.dp
    private val textSize = fullDayTextSize
    private val anchorPoint: Point = Point()
    private var fullDayHeight = 0f
    private val bgColor: Int
        get() = if (model.expired) {
            fullDayBgColorExpired
        } else {
            fullDayBgColor
        }
    private val textColor: Int
        get() = if (model.expired) {
            fullDayTextColorExpired
        } else {
            fullDayTextColor
        }

    private var parentWidth = screenWidth

    override fun onDraw(canvas: Canvas, paint: Paint) {
        if (railIndex >= ScheduleConfig.maxFullDayRailCount) return
        canvas.save()
        canvas.clipRect(
            drawingRect.left.coerceAtLeast(clockWidth),
            drawingRect.top,
            drawingRect.right,
            drawingRect.bottom.coerceAtMost(dateLineHeight + fullDayHeight),
        )
        if (model.id > 0) {
            model.drawExistsTask(canvas, paint)
        }
        canvas.restore()
    }

    private fun DailyTaskModel.drawExistsTask(canvas: Canvas, paint: Paint) {
        val drawRect = drawingRect
        if (drawingRect.bottom > dateLineHeight + fullDayHeight) return
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

        // 文字
        paint.textSize = textSize
        paint.color = textColor
        paint.textAlign = Paint.Align.CENTER
        val width = drawRect.right.coerceAtMost(parentWidth.toFloat()) + drawRect.left.coerceAtLeast(clockWidth)
        val maxWidth = drawRect.right.coerceAtMost(parentWidth.toFloat()) - drawRect.left.coerceAtLeast(
            clockWidth)
        canvas.drawText(
            title,
            width / 2,
            drawRect.centerY() + paint.textSize / 2 - 1f.dp,
            paint,
            maxWidth,
            autoFeed = false
        )
    }

    override fun updateDrawingRect(anchorPoint: Point, fullDayHeight: Float) {
        drawingRect.left = originRect.left + anchorPoint.x
        drawingRect.right = originRect.right + anchorPoint.x
        drawingRect.top = originRect.top + railIndex.coerceAtLeast(0) * modelRectHeight + marginVertical
        drawingRect.bottom = originRect.bottom + railIndex.coerceAtLeast(0) * modelRectHeight + marginVertical
        this.anchorPoint.x = anchorPoint.x
        this.anchorPoint.y = anchorPoint.y
        this.fullDayHeight = fullDayHeight
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        return true
    }

    companion object {
        val marginVertical = 6f.dp
    }
}