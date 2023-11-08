package me.wxc.widget.schedule.components

import android.graphics.*
import me.wxc.widget.base.DailyTaskModel
import me.wxc.widget.base.IScheduleComponent
import me.wxc.widget.base.IScheduleModel
import me.wxc.widget.schedule.*
import me.wxc.widget.tools.*

/**
 * 钟点刻度线
 */
class ClockLineComponent(override val model: ClockLineModel) : IScheduleComponent<ClockLineModel> {
    override val originRect: RectF = originRect().apply {
        left = 0f
        right = parentWidth.toFloat()
        if (model.clock == 24) {
            top += dayHeight
            bottom += dayHeight
        }
    }
    override val drawingRect: RectF = originRect().apply {
        left = 0f
        right = parentWidth.toFloat()
        if (model.clock == 24) {
            top += dayHeight
            bottom += dayHeight
        }
    }
    private val anchorPoint: Point = Point()
    private var fullDayHeight = 0f
    private var parentWidth = screenWidth
    private var parentHeight = screenHeight

    override fun onDraw(canvas: Canvas, paint: Paint) {
        parentWidth = canvas.width
        parentHeight = canvas.height
        canvas.save()
        canvas.clipRect(0f, dateLineHeight + fullDayHeight, parentWidth.toFloat(), parentHeight.toFloat())
        paint.color = clockLineColor
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = clockTextSize
        val startX = clockWidth - clockTextPadding
        val y = drawingRect.top
        canvas.drawText(model.showText, startX, y + 4f.dp, paint)
        paint.strokeWidth = 1f
        val stopX = parentWidth.toFloat()
        canvas.drawLine(drawingRect.left + clockWidth, y, stopX, y, paint)
        paint.pathEffect = null
        canvas.restore()
    }

    override fun updateDrawingRect(anchorPoint: Point, fullDayHeight: Float) {
        this.fullDayHeight = fullDayHeight
        this.anchorPoint.x = anchorPoint.x
        this.anchorPoint.y = anchorPoint.y
        drawingRect.top = originRect.top + anchorPoint.y + fullDayHeight
        drawingRect.bottom = originRect.bottom + anchorPoint.y + fullDayHeight
    }
}

data class ClockLineModel(
    val clock: Int,
    var createTaskModel: DailyTaskModel? = null
) : IScheduleModel {
    private val zeroClock = beginOfDay()
    override val beginTime: Long = zeroClock.timeInMillis + clock * hourMillis
    override val endTime: Long = zeroClock.timeInMillis + (clock + 1) * hourMillis
    val showText: String
        get() = if (clock == 24) "24:00" else beginTime.HHmm
}