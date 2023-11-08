package me.wxc.widget.schedule

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.view.isVisible
import me.wxc.widget.R
import me.wxc.widget.ScheduleConfig
import me.wxc.widget.base.*
import me.wxc.widget.tools.beginOfDay
import me.wxc.widget.tools.calendar
import me.wxc.widget.tools.firstDayOfWeekTime
import me.wxc.widget.tools.setter
import java.util.*

class ScheduleGroup @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs), ICalendarRender, ICalendarParent, IScheduleWidget {
    private val scheduleView: ScheduleView
    private val calendarWidget: ScheduleWidget

    init {
        inflate(context, R.layout.schedule_group, this)
        scheduleView = findViewById(R.id.scheduleView)
        calendarWidget = ScheduleWidget(scheduleView)
    }

    override val parentRender: ICalendarRender? = null
    override val calendar: Calendar = beginOfDay()
    override var focusedDayTime: Long by setter(-1) { _, time ->
        childRenders.forEach { it.focusedDayTime = time }
    }
    override var selectedDayTime: Long by setter(-1) { _, time ->
        if (!isVisible) return@setter
        childRenders.forEach { it.selectedDayTime = time.calendar.firstDayOfWeekTime }
    }
    override var scheduleModels: List<IScheduleModel> by setter(emptyList()) { _, list ->
        childRenders.forEach { it.scheduleModels = list }
    }
    override val beginTime: Long
        get() = ScheduleConfig.scheduleBeginTime
    override val endTime: Long
        get() = ScheduleConfig.scheduleEndTime
    override val childRenders: List<ICalendarRender>
        get() = listOf(calendarWidget)

    override val render: IScheduleRender
        get() = scheduleView
    override var lunarEnable: Boolean by setter(ScheduleConfig.lunarEnable) { _, enable ->
        render.adapter.onLunarEnableChange(enable)
        scheduleView.invalidate()
    }
    override val headerHeight: Float by setter(dateLineHeight) { _, height ->
        scheduleView.invalidate()
    }
    override fun onScroll(x: Int, y: Int) {
        scheduleView.widget.onScroll(x, y)
    }

    override fun scrollTo(x: Int, y: Int, duration: Int) {
        scheduleView.widget.scrollTo(x, y, duration)
    }

    override fun isScrolling(): Boolean = scheduleView.widget.isScrolling()
}