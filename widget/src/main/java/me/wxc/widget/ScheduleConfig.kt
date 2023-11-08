package me.wxc.widget

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import me.wxc.widget.base.CoincidentDailyTaskModel
import me.wxc.widget.base.DailyTaskModel
import me.wxc.widget.base.IScheduleModel
import me.wxc.widget.tools.beginOfDay
import me.wxc.widget.tools.nowMillis
import me.wxc.widget.tools.yyyyMd
import java.util.*

object ScheduleConfig {
    lateinit var app: Application
    var scheduleBeginTime: Long = 0L
    var scheduleEndTime: Long = beginOfDay().apply { add(Calendar.MONTH, 1200) }.timeInMillis
    var lifecycleScope: CoroutineScope = GlobalScope
    var selectedDayTime: Long = nowMillis
    var onDateSelectedListener: Calendar.() -> Unit = {}
    var scheduleModelsProvider: suspend (beginTime: Long, endTime: Long) -> List<IScheduleModel> =
        { _, _ ->
            emptyList()
        }
    var onDailyTaskClickBlock: (model: DailyTaskModel) -> Unit = {}
    var onMoreTaskClickBlock: (model: CoincidentDailyTaskModel) -> Unit = {}
    var onCreateTaskClickBlock: (model: DailyTaskModel) -> Unit = {}
    var onTaskDraggedBlock: (model: DailyTaskModel) -> Unit = {}
    var onWeekSelectedBlock: (Long) -> Unit = {}
    var lunarEnable: Boolean = true
    var nowLineEnable: Boolean = true
    val maxFullDayRailCount = 3
    val maxCoincidentRailCount = 5
}