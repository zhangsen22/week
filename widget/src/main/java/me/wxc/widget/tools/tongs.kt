package me.wxc.widget.tools

import android.content.res.Resources
import android.graphics.Point
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import com.nlf.calendar.Lunar
import me.wxc.widget.schedule.clockWidth
import java.util.Date
import kotlin.math.roundToInt
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty

val Int.dp: Int
    get() = (Resources.getSystem().displayMetrics.density * this).roundToInt()
val Float.dp: Float
    get() = Resources.getSystem().displayMetrics.density * this
val screenWidth: Int
    get() = Resources.getSystem().displayMetrics.widthPixels
val screenHeight: Int
    get() = Resources.getSystem().displayMetrics.heightPixels

fun MotionEvent.ifInRect(rectF: RectF?, padding: Int = 0): Boolean {
    if (rectF == null) return false
    return x > rectF.left - padding && x < rectF.right + padding && y > rectF.top - padding && y < rectF.bottom + padding
}

fun MotionEvent.ifAtRectTop(rectF: RectF?, padding: Int = 0): Boolean {
    if (rectF == null) return false
    return x > rectF.left - padding && x < rectF.right + padding && y > rectF.top - 2 * padding && y < rectF.top + padding
}

fun MotionEvent.ifAtRectBottom(rectF: RectF?, padding: Int = 0): Boolean {
    if (rectF == null) return false
    return x > rectF.left - padding && x < rectF.right + padding && y > rectF.bottom - padding && y < rectF.bottom + 2 * padding
}

fun RectF.ifVisible(view: View, leftClockWidth: Boolean = false): Boolean {
    return right >= view.left + (if (leftClockWidth) clockWidth.roundToInt() + 4.dp else 0) && left <= view.right && bottom >= view.top && top <= view.bottom
}

fun RectF.move(x: Int = 0, y: Int = 0) {
    left += x
    right += x
    top += y
    bottom += y
}

fun RectF?.topPoint(): Point? {
    return this?.run { Point(left.toInt(), top.toInt()) }
}

fun RectF?.bottomPoint(): Point? {
    return this?.run { Point(left.toInt(), bottom.toInt()) }
}

inline fun <T> setter(
    default: T,
    crossinline onSet: (old: T, new: T) -> Unit = { old, new -> }
): ReadWriteProperty<Any?, T> = Delegates.observable(default) { _, old, new ->
    onSet(old, new)
}

val Long.lunarText: String
    get() = run {
        val lunar = Lunar(Date(this))
        lunar.festivals.getOrNull(0) ?: run {
            lunar.qi.ifEmpty { null }
        } ?: run {
            lunar.jie.ifEmpty { null }
        } ?: run {
            if (lunar.dayInChinese == "初一") {
                "${lunar.monthInChinese}月"
            } else {
                lunar.dayInChinese
            }
        }
    }