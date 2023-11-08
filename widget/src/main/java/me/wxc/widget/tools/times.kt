package me.wxc.widget.tools

import android.util.Log
import me.wxc.widget.ScheduleConfig
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt


private val sdf_HHmm = SimpleDateFormat("HH:mm", Locale.ROOT)
private val sdf_yyyyMMddHHmmss = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
private val sdf_yyyyM = SimpleDateFormat("yyyy年M月", Locale.ROOT)
private val sdf_M = SimpleDateFormat("M月", Locale.ROOT)
private val sdf_yyyyMd = SimpleDateFormat("yyyy年M月d日", Locale.ROOT)
private val sdf_Md = SimpleDateFormat("M月d日", Locale.ROOT)

val nowMillis
    get() = System.currentTimeMillis()
const val quarterMillis = 15 * 60 * 1000L
const val hourMillis = 60 * 60 * 1000L
const val dayMillis = 24 * hourMillis

val Long.hours: Int
    get() = run {
        Calendar.getInstance().apply {
            time = Date(this@run)
        }.get(Calendar.HOUR_OF_DAY)
    }
val Long.dDays: Long
    get() = (beginOfDay(this).timeInMillis - beginOfDay().timeInMillis) / dayMillis
val Long.dMonths: Int
    get() = (years - nowMillis.years) * 12 + monthOfYear - nowMillis.monthOfYear

val Long.years: Int
    get() = run {
        Calendar.getInstance().apply {
            time = Date(this@run)
        }.get(Calendar.YEAR)
    }
val Long.monthOfYear: Int
    get() = run {
        Calendar.getInstance().apply {
            time = Date(this@run)
        }.get(Calendar.MONTH) + 1
    }

val Long.calendar: Calendar
    get() = Calendar.getInstance().apply { timeInMillis = this@calendar }

val Long.dayOfWeek: Int
    get() = calendar.get(Calendar.DAY_OF_WEEK)

val Long.dayOfMonth: Int
    get() = calendar.get(Calendar.DAY_OF_MONTH)

val Long.dayOfYear: Int
    get() = calendar.get(Calendar.DAY_OF_YEAR)

val Long.dayOfWeekText: String
    get() = run {
        when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "日"
            Calendar.MONDAY -> "一"
            Calendar.TUESDAY -> "二"
            Calendar.WEDNESDAY -> "三"
            Calendar.THURSDAY -> "四"
            Calendar.FRIDAY -> "五"
            else -> "六"
        }
    }

val Long.dayOfWeekTextSimple: String
    get() = run {
        when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "日"
            Calendar.MONDAY -> "一"
            Calendar.TUESDAY -> "二"
            Calendar.WEDNESDAY -> "三"
            Calendar.THURSDAY -> "四"
            Calendar.FRIDAY -> "五"
            else -> "六"
        }
    }

val Long.HHmm: String
    get() = sdf_HHmm.format(this)

val Long.yyyyMMddHHmmss: String
    get() = sdf_yyyyMMddHHmmss.format(this)

val Long.yyyyMd: String
    get() = sdf_yyyyMd.format(this)

val Long.M: String
    get() = sdf_M.format(this)

val Long.yyyyM: String
    get() = sdf_yyyyM.format(this)

val Long.Md: String
    get() = sdf_Md.format(this)

val Calendar.HHmm: String
    get() = timeInMillis.HHmm

val Calendar.yyyyMMddHHmmss: String
    get() = timeInMillis.yyyyMMddHHmmss

val Calendar.yyyyMd: String
    get() = timeInMillis.yyyyMd

val Calendar.yyyyM: String
    get() = timeInMillis.yyyyM

val Calendar.M: String
    get() = timeInMillis.M

val Calendar.Md: String
    get() = timeInMillis.Md

val Calendar.firstDayOfMonthTime: Long
    get() = beginOfDay(timeInMillis).apply {
        set(Calendar.DAY_OF_MONTH, getActualMinimum(Calendar.DAY_OF_MONTH))
    }.timeInMillis

val Calendar.lastDayOfMonthTime: Long
    get() = beginOfDay(timeInMillis).apply {
        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
    }.timeInMillis

val Calendar.firstDayOfWeekTime: Long
    get() = beginOfDay(timeInMillis).apply {
        set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    }.timeInMillis

val Calendar.lastDayOfWeekTime: Long
    get() = beginOfDay(timeInMillis).apply {
        set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
    }.timeInMillis

fun beginOfDay(timestamp: Long = nowMillis): Calendar = Calendar.getInstance().apply {
    time = Date(timestamp)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

fun Long.adjustTimestamp(period: Long, roundTo: Boolean): Long {
    val zeroOfDay = beginOfDay(this).timeInMillis
    return if (!roundTo) {
        this - (this - zeroOfDay) % period
    } else {
        (zeroOfDay + (1f * (this - zeroOfDay) / period).roundToInt() * period)
    }
}

fun <R> wrapDurationLog(msg: String = "block cost: ", block: () -> R) : R {
    val t = System.currentTimeMillis()
    return block().apply {
        Log.i("wrapDurationLog", "$msg ${System.currentTimeMillis() - t} ms")
    }
}