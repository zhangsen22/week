package me.wxc.widget.base

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import me.wxc.widget.schedule.components.ExceedFullDayComponent

interface IScheduleModelHolder : ITimeRangeHolder {
    val schedules: List<IScheduleModel>
}

interface IScheduleWidget : ICalendarRender {
    val render: IScheduleRender
    var lunarEnable: Boolean
    val headerHeight: Float
    fun onTouchEvent(motionEvent: MotionEvent): Boolean
    fun onScroll(x: Int, y: Int)
    fun scrollTo(x: Int, y: Int, duration: Int = 250)
    fun isScrolling(): Boolean
}

interface IScheduleRender {
    var widget: IScheduleWidget
    val calendarPosition: Point
    val adapter: IScheduleRenderAdapter
    fun render(x: Int, y: Int)
}

interface IScheduleComponent<T : IScheduleModel> {
    val model: T
    val originRect: RectF
    val drawingRect: RectF
    fun onDraw(canvas: Canvas, paint: Paint)
    fun updateDrawingRect(anchorPoint: Point, fullDayHeight: Float)
    fun onTouchEvent(e: MotionEvent): Boolean = false
}

interface IScheduleRenderAdapter {
    var models: MutableList<IScheduleModel>
    var backgroundComponents: List<IScheduleComponent<*>>
    var foregroundComponents: List<IScheduleComponent<*>>
    val visibleComponents: List<IScheduleComponent<*>>
    val currentExceedFullDayComponent: ExceedFullDayComponent?
    val exceedFullDayComponents: List<ExceedFullDayComponent>
    fun onCreateComponent(model: IScheduleModel): IScheduleComponent<*>?
    fun notifyModelsChanged()
    fun onLunarEnableChange(enable: Boolean)
    val fullDayTotalHeight: Float
    fun changeFullDayTotalHeight(view: View)
    fun updateVisibleComponent()
}

interface IScheduleModel : ITimeRangeHolder, java.io.Serializable {
    val taskId: Long
        get() = (this as? DailyTaskModel)?.id ?: -1
}