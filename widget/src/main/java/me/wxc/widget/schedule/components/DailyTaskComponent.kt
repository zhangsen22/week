package me.wxc.widget.schedule.components

import android.graphics.*
import android.view.MotionEvent
import me.wxc.widget.base.*
import me.wxc.widget.schedule.*
import me.wxc.widget.tools.*

/**
 * 普通日程
 */
class DailyTaskComponent(override val model: DailyTaskModel) :
    IScheduleComponent<DailyTaskModel> {
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
    private val bgColor: Int
        get() = if (model.expired) {
            scheduleBgColorExpired
        } else {
            scheduleBgColor
        }
    private val textColor: Int
        get() = if (model.expired) {
            scheduleTextColorExpired
        } else {
            scheduleTextColor
        }

    private val lineColor: Int
        get() = if (model.expired) {
            scheduleLineColorExpired
        } else {
            scheduleLineColor
        }

    private var parentWidth = screenWidth
    private var parentHeight = screenHeight

    override fun onDraw(canvas: Canvas, paint: Paint) {
        if (model.id > 0) {
            drawExistsTask(canvas, paint)
        }
    }

    private fun drawExistsTask(canvas: Canvas, paint: Paint) {
        parentWidth = canvas.width
        parentHeight = canvas.height
        val drawRect = drawingRect
        if (drawRect.bottom < dateLineHeight) return
        canvas.save()
        canvas.clipRect(
            clockWidth,
            dateLineHeight + fullDayHeight,
            parentWidth.toFloat(),
            parentHeight.toFloat()
        )
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
            startX,
            drawRect.top + paddingVertical,
            startX,
            drawRect.bottom - paddingVertical,
            paint
        )

        // 文字
        paint.textSize = textSize
        paint.color = textColor
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(model.title, drawRect, textPadding, paddingHorizontal, paint)
        canvas.restore()
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

    companion object {
        private const val TAG = "DailyTaskComponent"
    }
}
