package me.wxc.widget.schedule.components

import android.graphics.*
import me.wxc.widget.base.IScheduleComponent
import me.wxc.widget.base.IScheduleModel
import me.wxc.widget.schedule.clockTextPadding
import me.wxc.widget.schedule.clockTextSize
import me.wxc.widget.schedule.clockWidth
import me.wxc.widget.schedule.dateLineHeight
import me.wxc.widget.schedule.dayHeight
import me.wxc.widget.schedule.dayWidth
import me.wxc.widget.tools.HHmm
import me.wxc.widget.tools.beginOfDay
import me.wxc.widget.tools.dDays
import me.wxc.widget.tools.dp
import me.wxc.widget.tools.hourMillis
import me.wxc.widget.tools.nowMillis

/**
 * 当前时刻标线
 */
class NowLineComponent : IScheduleComponent<NowLineModel> {
    override val model: NowLineModel = NowLineModel
    override val originRect: RectF
        get() = run {
            val today = nowMillis.dDays
            val day = model.beginTime.dDays
            val left = clockWidth + (day - today) * dayWidth
            val right = left + 7 * dayWidth
            val zeroClock = beginOfDay(model.beginTime)
            val top =
                dateLineHeight + dayHeight * (model.beginTime - zeroClock.time.time) / (hourMillis * 24)
            val bottom =
                dateLineHeight + dayHeight * (model.endTime - zeroClock.time.time) / (hourMillis * 24)
            RectF(left, top, right, bottom)
        }
    override val drawingRect: RectF = run {
        val today = nowMillis.dDays
        val day = model.beginTime.dDays
        val left = clockWidth + (day - today) * dayWidth
        val right = left + 7 * dayWidth
        val zeroClock = beginOfDay(model.beginTime)
        val top =
            dateLineHeight + dayHeight * (model.beginTime - zeroClock.time.time) / (hourMillis * 24)
        val bottom =
            dateLineHeight + dayHeight * (model.endTime - zeroClock.time.time) / (hourMillis * 24)
        RectF(left, top, right, bottom)
    }
    private val circleRadius = 2f.dp
    private var extraPadding = 0f

    override fun onDraw(canvas: Canvas, paint: Paint) {
        if (drawingRect.centerY() - 4f.dp < dateLineHeight) return
        canvas.save()
        canvas.clipRect(
            0f,
            (drawingRect.top - 10f.dp).coerceAtLeast(dateLineHeight + extraPadding),
            drawingRect.right,
            (drawingRect.bottom + 10f.dp).coerceAtLeast(dateLineHeight + extraPadding),
        )
        paint.color = Color.RED
        paint.strokeWidth = 1f.dp
        val startX = clockWidth - clockTextPadding
        val y = drawingRect.top
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = clockTextSize
        canvas.drawText(model.showText, startX, y + 4f.dp, paint)
        canvas.drawLine(
            drawingRect.left + 4f.dp,
            drawingRect.centerY(),
            drawingRect.right - 2f.dp,
            drawingRect.centerY(),
            paint
        )
        canvas.drawCircle(
            drawingRect.left + circleRadius / 2,
            drawingRect.centerY(),
            circleRadius,
            paint
        )
        canvas.restore()
    }

    override fun updateDrawingRect(anchorPoint: Point, fullDayHeight: Float) {
        drawingRect.top = originRect.top + anchorPoint.y + fullDayHeight
        drawingRect.bottom = originRect.bottom + anchorPoint.y + fullDayHeight
        this.extraPadding = fullDayHeight
    }
}

object NowLineModel : IScheduleModel {
    override val beginTime: Long
        get() = nowMillis
    override val endTime: Long
        get() = nowMillis
    val showText: String
        get() = beginTime.HHmm
}