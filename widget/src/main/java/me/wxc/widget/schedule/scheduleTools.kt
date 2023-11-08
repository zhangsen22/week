package me.wxc.widget.schedule

import android.graphics.Color
import android.graphics.Point
import android.graphics.RectF
import android.util.Log
import me.wxc.widget.ScheduleConfig
import me.wxc.widget.base.CoincidentDailyTaskModel
import me.wxc.widget.base.DailyTaskModel
import me.wxc.widget.base.IScheduleComponent
import me.wxc.widget.base.IScheduleModel
import me.wxc.widget.tools.*
import java.util.*
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong

val dayWidth: Float
    get() = ((screenWidth - clockWidth) / 7).roundToInt().toFloat()
val dayHeight = 50f.dp * 24

val clockWidth = 56f.dp
val clockHeight = 50f.dp
val clockTextPadding = 4f.dp
val clockTextSize = 12f.dp
val clockLineColor = Color.parseColor("#BBBBBB")

val fullDayTextSize = 11f.dp
val fullDayTextColor = Color.parseColor("#43A1DD")
val fullDayTextColorExpired = Color.parseColor("#AEAEAE")
val fullDayBgColor = Color.parseColor("#DDECFE")
val fullDayBgColorExpired = Color.parseColor("#F6F6F6")

val scheduleTextSize = 11f.dp
val scheduleTextColor = Color.parseColor("#43A1DD")
val scheduleTextColorExpired = Color.parseColor("#AEAEAE")
val scheduleBgColor = Color.parseColor("#DDECFE")
val scheduleBgColorExpired = Color.parseColor("#F6F6F6")
val scheduleLineColor = Color.parseColor("#0889F2")
val scheduleLineColorExpired = Color.parseColor("#D9D9D9")

val dateTextColor = Color.parseColor("#333333")
val dateTextColorSelected = Color.parseColor("#43A1DD")
val dateTextColorExpired = Color.parseColor("#AEAEAE")
val dateBgColor = Color.parseColor("#F5F5F5")
val dateDayTextSize = 18f.dp
val dateWeekTextSize = 12f.dp
val dateLunarTextSize = 12f.dp
val dateLineHeight: Float
    get() = if (ScheduleConfig.lunarEnable) 98f.dp else 76f.dp

val canvasPadding = 0.dp
val zeroClockY = dateLineHeight + canvasPadding

val createTaskDuration = hourMillis

fun IScheduleModel.originRect(): RectF {
    // x轴： 与当天的间隔天数 * 一天的宽度
    // y轴： 当前分钟数 / 一天的分钟数 * 一天的高度
    val today = nowMillis.dDays
    val day = beginTime.dDays
    val left = clockWidth + (day - today) * dayWidth
    val right = left + dayWidth
    val zeroClock = beginOfDay(beginTime)
    val top = dateLineHeight + dayHeight * (beginTime - zeroClock.time.time) / (hourMillis * 24)
    val bottom = dateLineHeight + dayHeight * (endTime - zeroClock.time.time) / (hourMillis * 24)
    return RectF(left, top, right, bottom)
}

fun IScheduleComponent<*>.originRect(): RectF = model.originRect()

fun Point.positionToTime(scrollX: Int = 0, scrollY: Int = 0): Long {
    val days = ((x - clockWidth + scrollX) / dayWidth).roundToInt()
    val millis = ((y - dateLineHeight + scrollY) * hourMillis / clockHeight).roundToLong()
    val calendar = beginOfDay().apply {
        add(Calendar.DAY_OF_YEAR, days)
    }
    return calendar.timeInMillis + millis
}

val Float.yToMillis: Long
    get() = (this * hourMillis / clockHeight).roundToLong()

val Float.xToDDays: Int
    get() = ((this - clockWidth) / dayWidth).roundToInt()

val IScheduleModel.isFullDay: Boolean
    get() = (endTime - 1).dDays > beginTime.dDays || endTime - beginTime >= dayMillis

val List<IScheduleModel>.mapForRender: List<IScheduleModel>
    get() = run {
        val fullDayModels = filter { it.isFullDay }
        val sorted = sortedBy { it.beginTime } - fullDayModels.toSet()
        Log.i("mapForRender", "from: ${sorted.joinToString { it.beginTime.yyyyMMddHHmmss }}")
        var lastBeginTime = 0L
        var lastEndTime = 0L
        var lastModel: DailyTaskModel? = null
        val map = mutableMapOf<Long, MutableList<DailyTaskModel>>()
        sorted.forEach {
            if (it !is DailyTaskModel) return@forEach
            val beginTime = it.beginTime
            val endTime = it.endTime
            if (it.beginTime < lastEndTime) {
                map.getOrPut(lastBeginTime) {
                    mutableListOf<DailyTaskModel>().apply {
                        lastModel?.let { add(it) }
                    }
                }.add(it)
                lastEndTime = max(lastEndTime, endTime)
            } else {
                lastBeginTime = beginTime
                lastEndTime = endTime
                lastModel = it
            }
        }
        val result = sorted - map.flatMap { it.value }.toSet() + map.map {
            val maxEndTime = it.value.maxOfOrNull { it.endTime } ?: it.key
            CoincidentDailyTaskModel(it.key, maxEndTime, it.value.toList())
        } + fullDayModels
        result.sortedBy { it.beginTime }.apply {
            Log.i("mapForRender", "to: ${joinToString { it.beginTime.yyyyMMddHHmmss }}")
        }
    }