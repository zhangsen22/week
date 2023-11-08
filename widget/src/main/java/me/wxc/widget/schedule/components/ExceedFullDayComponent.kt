package me.wxc.widget.schedule.components

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.RectF
import android.view.MotionEvent
import me.wxc.widget.ScheduleConfig
import me.wxc.widget.base.CoincidentDailyTaskModel
import me.wxc.widget.base.IScheduleComponent
import me.wxc.widget.schedule.clockWidth
import me.wxc.widget.schedule.dateLineHeight
import me.wxc.widget.schedule.dayWidth
import me.wxc.widget.schedule.fullDayTextColorExpired
import me.wxc.widget.schedule.fullDayTextSize
import me.wxc.widget.schedule.scheduleBgColor
import me.wxc.widget.tools.dDays
import me.wxc.widget.tools.dp
import me.wxc.widget.tools.drawText
import me.wxc.widget.tools.nowMillis

/**
 * 超出最大行数限制的日程集
 */
class ExceedFullDayComponent(override val model: CoincidentDailyTaskModel) :
    IScheduleComponent<CoincidentDailyTaskModel> {
    private val modelRectHeight = 20f.dp
    override val originRect: RectF = createOriginRect()
    override val drawingRect: RectF = createOriginRect()
    private fun createOriginRect() = run {
        val today = nowMillis.dDays
        val day = model.beginTime.dDays
        val left = clockWidth + (day - today) * dayWidth
        val right = left + ((model.endTime - 1).dDays - day + 1) * dayWidth
        val top = dateLineHeight
        val bottom = top + modelRectHeight
        RectF(left, top, right, bottom)
    }
    private val textSize = fullDayTextSize
    private val anchorPoint: Point = Point()
    private var fullDayHeight = 0f
    private val textColor: Int = fullDayTextColorExpired

    override fun onDraw(canvas: Canvas, paint: Paint) {
        canvas.save()
        canvas.clipRect(
            drawingRect.left.coerceAtLeast(clockWidth),
            drawingRect.top,
            drawingRect.right,
            drawingRect.bottom.coerceAtMost(dateLineHeight + fullDayHeight),
        )
        model.drawExistsTask(canvas, paint)
        canvas.restore()
    }

    private fun CoincidentDailyTaskModel.drawExistsTask(canvas: Canvas, paint: Paint) {
        if (drawingRect.bottom > dateLineHeight + fullDayHeight) return
        paint.textSize = textSize
        paint.color = textColor
        paint.textAlign = Paint.Align.CENTER
        val count = models.size
        canvas.drawText(
            "还有其他${count}项 >",
            drawingRect,
            drawingRect.width() / 2,
            0f,
            paint
        )
    }

    override fun updateDrawingRect(anchorPoint: Point, fullDayHeight: Float) {
        drawingRect.left = originRect.left + anchorPoint.x
        drawingRect.right = originRect.right + anchorPoint.x
        drawingRect.top = originRect.top + fullDayHeight - rectHeight - marginVertical
        drawingRect.bottom = originRect.bottom + fullDayHeight - rectHeight - marginVertical
        this.anchorPoint.x = anchorPoint.x
        this.anchorPoint.y = anchorPoint.y
        this.fullDayHeight = fullDayHeight
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        return true
    }

    companion object {
        val marginVertical = 6f.dp
        val rectHeight = 20f.dp
    }
}