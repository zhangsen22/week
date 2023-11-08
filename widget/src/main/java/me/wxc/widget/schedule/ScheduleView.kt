package me.wxc.widget.schedule

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.util.SparseArray
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.updatePadding
import me.wxc.widget.R
import me.wxc.widget.ScheduleConfig
import me.wxc.widget.base.*
import me.wxc.widget.schedule.components.*
import me.wxc.widget.tools.*
import me.wxc.widget.schedule.components.ClockLineComponent
import me.wxc.widget.schedule.components.ClockLineModel

class ScheduleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), IScheduleRender {

    init {
        updatePadding(top = canvasPadding, bottom = canvasPadding)
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(
            ResourcesCompat.getFont(context, R.font.product_sans_regular2),
            Typeface.NORMAL
        )
    }
    override lateinit var widget: IScheduleWidget
    override val calendarPosition: Point = Point()
    override val adapter: IScheduleRenderAdapter = ScheduleAdapter(this)

    private val fullDayHeight: Float
        get() = adapter.fullDayTotalHeight

    override fun render(x: Int, y: Int) {
        calendarPosition.x = x
        calendarPosition.y = y + paddingTop
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        adapter.backgroundComponents.forEach {
            it.updateDrawingRect(calendarPosition, fullDayHeight)
            if (it.drawingRect.ifVisible(this)) {
                it.onDraw(canvas, paint)
            }
        }
        adapter.updateVisibleComponent()
        adapter.visibleComponents.filterIsInstance<FullDaySingleComponent>()
            .forEach { component ->
                component.updateDrawingRect(calendarPosition, fullDayHeight)
                if (component.drawingRect.ifVisible(this, true)) {
                    component.onDraw(canvas, paint)
                }
            }

        // 分割线
        if (adapter.fullDayTotalHeight > 0) {
            paint.color = Color.parseColor("#F5F5F5")
            canvas.drawRect(
                0f,
                dateLineHeight + adapter.fullDayTotalHeight - dividerHeight,
                canvas.width.toFloat(),
                dateLineHeight + adapter.fullDayTotalHeight,
                paint
            )

            adapter.exceedFullDayComponents.forEach { component ->
                component.updateDrawingRect(calendarPosition, fullDayHeight)
                if (component.drawingRect.ifVisible(this)) {
                    component.onDraw(canvas, paint)
                }
            }
        }
        adapter.visibleComponents
            .filterNot { it is FullDaySingleComponent }
            .forEach { component ->
                component.updateDrawingRect(calendarPosition, fullDayHeight)
                if (component.drawingRect.ifVisible(this)) {
                    component.onDraw(canvas, paint)
                }
            }
        adapter.foregroundComponents.forEach {
            it.updateDrawingRect(calendarPosition, fullDayHeight)
            if (it.drawingRect.ifVisible(this)) {
                it.onDraw(canvas, paint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return widget.onTouchEvent(event)
    }

    companion object {
        private const val TAG = "ScheduleView"
        val dividerHeight = 6f.dp
    }
}

class ScheduleAdapter(private val view: ScheduleView) : IScheduleRenderAdapter {
    override var models: MutableList<IScheduleModel> = mutableListOf()
    override var backgroundComponents: List<IScheduleComponent<*>> = listOf()
    override var foregroundComponents: List<IScheduleComponent<*>> = listOf()
    override var visibleComponents: List<IScheduleComponent<*>> = listOf()
    private val _taskComponentCache = SparseArray<IScheduleComponent<*>>()
    override var exceedFullDayComponents: List<ExceedFullDayComponent> = listOf()
    private val _fullDayRailCountGroupByWeek: MutableMap<Long, Int> = mutableMapOf()

    init {
        initialStableComponents()
    }

    override val currentExceedFullDayComponent: ExceedFullDayComponent?
        get() = exceedFullDayComponents.find { it.model.beginTime == view.widget.selectedDayTime.calendar.firstDayOfWeekTime }

    override fun onCreateComponent(model: IScheduleModel): IScheduleComponent<*> {
        return when (model) {
            is DailyTaskModel -> {
                _taskComponentCache.get(model.id.toInt()) ?: run {
                    if (model.isFullDay) {
                        val rails = allFullDayCoincidentModel?.rails ?: emptyMap()
                        var railIndex = 0
                        rails.forEach { (i, dailyTaskModels) ->
                            if (dailyTaskModels.contains(model)) {
                                railIndex = i
                                return@forEach
                            }
                        }
                        FullDaySingleComponent(model, railIndex)
                    } else {
                        DailyTaskComponent(model)
                    }
                }.apply {
                    _taskComponentCache.append(model.id.toInt(), this)
                }
            }

            is CoincidentDailyTaskModel -> {
                CoincidentDailyTaskComponent(model)
            }

            else -> throw IllegalArgumentException("invalid model: $model")
        }
    }

    private var allFullDayCoincidentModel: CoincidentDailyTaskModel? = null
    private val fullDayModels: List<DailyTaskModel>
        get() = models.filterIsInstance<DailyTaskModel>().filter { it.isFullDay }
    private val fullDayBeginWeekTime: Long
        get() = if (fullDayModels.isEmpty()) 0 else fullDayModels.map { it.beginTime }.min().calendar.firstDayOfWeekTime
    private val fullDayEndWeekTime: Long
        get() = if (fullDayModels.isEmpty()) 0 else fullDayModels.map { it.endTime }.max().calendar.firstDayOfWeekTime

    override fun notifyModelsChanged() {
        _fullDayRailCountGroupByWeek.clear()
        _taskComponentCache.clear()
        calculateFullDayComponents()
        updateVisibleComponent()
        changeFullDayTotalHeight(view)
    }

    private fun calculateFullDayComponents() {
        exceedFullDayComponents = emptyList()
        allFullDayCoincidentModel = fullDayModels.toCoincidentModel().apply {
            // 计算超出最大行数限制的日程
            val models = rails.filter { it.key >= ScheduleConfig.maxFullDayRailCount }
                .flatMap { it.value }
            if (models.isNotEmpty()) {
                val result = mutableMapOf<Long, MutableList<DailyTaskModel>>()
                for (time in fullDayBeginWeekTime until fullDayEndWeekTime + 7 * dayMillis step 7 * dayMillis) {
                    result[time] = mutableListOf<DailyTaskModel>().apply {
                        addAll(
                            models.filter {
                                (it.beginTime >= time && it.beginTime < time + 7 * dayMillis)
                                        || (it.endTime >= time && it.endTime < time + 7 * dayMillis)
                                        || (it.beginTime < time && it.endTime > time + 7 * dayMillis)
                            }
                        )
                    }
                }
                result.keys.map {
                    CoincidentDailyTaskModel(
                        it,
                        it + 7 * dayMillis,
                        result.get(it)?.toList() ?: emptyList()
                    )
                }.filter {
                    it.models.isNotEmpty()
                }.map {
                    ExceedFullDayComponent(it)
                }.apply {
                    exceedFullDayComponents = this
                }
            }
        }

        // 计算每周的最大railIndex
        val result = mutableMapOf<Long, MutableList<DailyTaskModel>>()
        for (time in fullDayBeginWeekTime until fullDayEndWeekTime + 7 * dayMillis step 7 * dayMillis) {
            result[time] = mutableListOf<DailyTaskModel>().apply {
                addAll(
                    fullDayModels.filter {
                        (it.beginTime >= time && it.beginTime < time + 7 * dayMillis)
                                || (it.endTime >= time && it.endTime < time + 7 * dayMillis)
                                || (it.beginTime < time && it.endTime > time + 7 * dayMillis)
                    }
                )
            }
        }
        result.filter { it.value.isNotEmpty() }.keys.forEach {
            val count =
                result[it]?.map { it.railIndex }?.filter { it < ScheduleConfig.maxFullDayRailCount }
            if (count.isNullOrEmpty()) {
                _fullDayRailCountGroupByWeek[it] = 0
            } else {
                _fullDayRailCountGroupByWeek[it] = count.max()
            }
        }
        Log.i(
            "ScheduleView",
            "calculateFullDayComponents: ${result.values.joinToString { "\n" + it.joinToString { it.title } }}${_fullDayRailCountGroupByWeek.values.joinToString { it.toString() }}"
        )
    }

    private fun List<DailyTaskModel>.toCoincidentModel(): CoincidentDailyTaskModel {
        val beginTime = ScheduleConfig.scheduleBeginTime
        val endTime = ScheduleConfig.scheduleEndTime
        return CoincidentDailyTaskModel(
            beginTime, endTime, this, saveRailIndex = true
        )
    }

    override fun onLunarEnableChange(enable: Boolean) {
        initialStableComponents()
        notifyModelsChanged()
    }

    private fun initialStableComponents() {
        backgroundComponents = mutableListOf<IScheduleComponent<*>>().apply {
            if (ScheduleConfig.nowLineEnable) {
                add(NowLineComponent())
            }
            for (i in 1..23) {
                add(ClockLineComponent(ClockLineModel(i)))
            }
        }.toList()
        foregroundComponents = listOf(
            DateLineComponent(),
        )
    }

    override var fullDayTotalHeight: Float = 0f

    private var animator: ValueAnimator? = null
    override fun changeFullDayTotalHeight(view: View) {
        val from = fullDayTotalHeight
        val ifHasExceed = currentExceedFullDayComponent != null

        val time = this.view.widget.selectedDayTime.calendar.firstDayOfWeekTime
        val railCount = _fullDayRailCountGroupByWeek[time]?.let { it + 1 } ?: 0
        val to = if (railCount > 0) {
            val fullDayHeight =
                railCount.coerceAtMost(ScheduleConfig.maxFullDayRailCount) * ExceedFullDayComponent.rectHeight
            val exceedHeight = if (ifHasExceed) ExceedFullDayComponent.rectHeight else 0f
            fullDayHeight + ScheduleView.dividerHeight + 2 * FullDaySingleComponent.marginVertical + exceedHeight
        } else {
            0f
        }
        Log.i("ScheduleView", "changeFullDayTotalHeight: ${time.yyyyMd} ${railCount} $from -> $to")
        if (from == to) return
        fullDayTotalHeight = to
        animator?.cancel()
        animator = ValueAnimator.ofFloat(from, to).apply {
            addUpdateListener {
                duration = 100
                fullDayTotalHeight = it.animatedValue as Float
                view.invalidate()
            }
            start()
        }
    }

    override fun updateVisibleComponent() {
        val time = view.widget.selectedDayTime
        val startDay = time.dDays.toInt() - 28
        val endDay = startDay + 56
        visibleComponents = models.mapForRender.filter {
            it.isFullDay || it.beginTime.dDays in startDay..endDay
        }
            .map { onCreateComponent(it) }
            .filterNot {
                ((it as? FullDaySingleComponent)?.railIndex
                    ?: 0) >= ScheduleConfig.maxFullDayRailCount
            }
        visibleComponents
            .forEach { component ->
                component.updateDrawingRect(view.calendarPosition, fullDayTotalHeight)
            }
        exceedFullDayComponents.forEach { component ->
            component.updateDrawingRect(view.calendarPosition, fullDayTotalHeight)
        }
    }
}