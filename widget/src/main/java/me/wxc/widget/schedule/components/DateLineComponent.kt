package me.wxc.widget.schedule.components

import android.graphics.*
import me.wxc.widget.ScheduleConfig
import me.wxc.widget.base.IScheduleComponent
import me.wxc.widget.base.IScheduleModel
import me.wxc.widget.schedule.*
import me.wxc.widget.tools.*

/**
 * 顶部周历
 */
class DateLineComponent : IScheduleComponent<DateLineModel> {
    override val model: DateLineModel = DateLineModel
    override val originRect: RectF = RectF(clockWidth, 0f, screenWidth.toFloat(), dateLineHeight)
    override val drawingRect: RectF = RectF(clockWidth, 0f, screenWidth.toFloat(), dateLineHeight)
    private val padding: Float = 6f.dp
    private val bgRadius = 6f.dp
    private val dayMargin = 56f.dp
    private val dayCircleMargin = 48f.dp
    private val dayCircleRadius = 15f.dp
    private val weekMargin = 26f.dp
    private val lunarMargin = 80f.dp
    private var parentWidth = screenWidth
    private var scrollX = 0f

    override fun onDraw(canvas: Canvas, paint: Paint) {
        parentWidth = canvas.width
        paint.color = dateBgColor
        canvas.drawRoundRect(
            padding,
            padding,
            parentWidth - padding,
            drawingRect.bottom - padding,
            bgRadius, bgRadius,
            paint
        )
        canvas.save()
        canvas.clipRect(drawingRect)
        var x = -dayWidth
        while (x >= -dayWidth && x < parentWidth) {
            val startX =
                (x + scrollX) / dayWidth * dayWidth + clockWidth - (x + scrollX) % dayWidth
            val beginTime = beginOfDay().timeInMillis + startX.xToDDays * dayMillis
            val dDays = startX.xToDDays - nowMillis.dDays

            paint.isFakeBoldText = true
            paint.textAlign = Paint.Align.CENTER
            paint.color = if (dDays == 0L) {
                dateTextColorSelected
            } else if (dDays < 0) {
                dateTextColorExpired
            } else {
                dateTextColor
            }
            // 周
            paint.textSize = dateWeekTextSize
            paint.isFakeBoldText = false
            paint.color = if (dDays < 0) {
                dateTextColorExpired
            } else {
                dateTextColor
            }
            canvas.drawText(
                beginTime.dayOfWeekText,
                startX - scrollX + dayWidth / 2,
                drawingRect.top + weekMargin,
                paint
            )
            // 日
            if (dDays == 0L) {
                paint.color = scheduleBgColor
                canvas.drawCircle(
                    startX - scrollX + dayWidth / 2,
                    drawingRect.top + dayCircleMargin,
                    dayCircleRadius,
                    paint
                )
            }
            val day = if (dDays == 0L) {
                "今"
            } else {
                beginTime.dayOfMonth.toString()
            }
            paint.textSize = dateDayTextSize
            paint.color = if (dDays == 0L) {
                dateTextColorSelected
            } else if (dDays < 0) {
                dateTextColorExpired
            } else {
                dateTextColor
            }
            canvas.drawText(
                day,
                startX - scrollX + dayWidth / 2,
                drawingRect.top + dayMargin,
                paint
            )
            // 农历
            if (ScheduleConfig.lunarEnable) {
                paint.textSize = dateLunarTextSize
                paint.isFakeBoldText = false
                canvas.drawText(
                    beginTime.lunarText,
                    startX - scrollX + dayWidth / 2,
                    drawingRect.top + lunarMargin,
                    paint
                )
            }
            x += dayWidth
        }
        canvas.restore()
    }

    override fun updateDrawingRect(anchorPoint: Point, fullDayHeight: Float) {
        scrollX = -anchorPoint.x.toFloat()
    }
}

object DateLineModel : IScheduleModel {
    override var beginTime: Long = 0
    override var endTime: Long = 0
}