package me.wxc.widget.tools

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

fun Canvas.drawText(
    text: String,
    x: Float,
    y: Float,
    paint: Paint,
    maxWidth: Float,
    autoFeed: Boolean = true
) {
    val start = 0
    var end = text.length
    while (end >= start && paint.measureText(text, start, end) > maxWidth) {
        end--
    }
    if (end <= 0) return
    drawText(text, 0, end, x, y, paint)
    if (autoFeed && end < text.length) {
        drawText(
            text.subSequence(end, text.length).toString(),
            x,
            y + paint.textSize + 2f.dp,
            paint,
            maxWidth
        )
    }
}

fun Canvas.drawText(text: String, rectF: RectF, startX: Float, paddingHorizontal: Float, paint: Paint, vertical: Boolean = false) {
    if (vertical) {
        drawTextVertical(text, rectF, startX, paddingHorizontal, paint)
        return
    }
    var start = 0
    val end = text.length
    val maxWidth = rectF.width() - startX - 2 * paddingHorizontal - 4f.dp
    val textHeight = paint.textSize
    val maxLines = rectF.height() / (textHeight + 2f.dp)
    val subStrings = mutableListOf<String>()
    var index = 0
    while (index < end && subStrings.size < maxLines) {
        if (paint.measureText(text, start, index) > maxWidth) {
            subStrings.add(text.substring(start, index))
            start = index
        }
        index ++
    }
    if (index > start && subStrings.size < maxLines) {
        subStrings.add(text.substring(start, index))
    }
    val centerY = rectF.top + rectF.height() / 2
    val fromY = centerY - subStrings.size * textHeight / 2
    subStrings.forEachIndexed { i, it ->
        drawText(it, rectF.left + startX, fromY + textHeight * (i + 1), paint)
    }
}


fun Canvas.drawTextVertical(text: String, rectF: RectF, startX: Float, paddingHorizontal: Float, paint: Paint) {
    val maxWidth = rectF.width() - startX - 2 * paddingHorizontal
    val textHeight = paint.textSize
    if (maxWidth < textHeight) return
    val maxLines = rectF.height() / (textHeight + 2f.dp)
    val subStrings = mutableListOf<String>()
    text.forEachIndexed { i, it ->
        if (i > maxLines) return@forEachIndexed
        subStrings.add(it.toString())
    }
    val centerY = rectF.top + rectF.height() / 2
    val fromY = centerY - subStrings.size * textHeight / 2
    subStrings.forEachIndexed { i, it ->
        drawText(it, rectF.left + startX, fromY + textHeight * (i + 1), paint)
    }
}

private fun getBaseLine(paint: Paint, centerY: Float): Float {
    return centerY - (paint.fontMetricsInt.bottom + paint.fontMetricsInt.top) / 2f
}
