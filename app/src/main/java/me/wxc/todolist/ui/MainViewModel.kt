package me.wxc.todolist.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import me.wxc.todolist.business.DailyTaskBusinessImpl
import me.wxc.widget.base.DailyTaskModel
import me.wxc.widget.base.IScheduleModel
import me.wxc.widget.tools.dayMillis
import me.wxc.widget.tools.hourMillis

class MainViewModel : ViewModel() {
    private val business by lazy { DailyTaskBusinessImpl() }
    private val mockRemoteData: MutableMap<Long, List<IScheduleModel>> = mutableMapOf()

    suspend fun getRangeDailyTask(
        beginTime: Long,
        endTime: Long
    ): List<IScheduleModel> = business.getDailyTasks(beginTime, endTime) + mockRemoteData.flatMap { it.value }

    suspend fun removeDailyTask(
        model: DailyTaskModel,
        deleteOption: DeleteOptionFragment.DeleteOption
    ): List<Long> = business.removeDailyTasks(model, deleteOption)

    suspend fun updateSingleDailyTask(
        model: DailyTaskModel
    ): List<Long> = business.updateDailyTasks(model, UpdateOptionFragment.UpdateOption.ONE)

    suspend fun saveCreateDailyTask(
        model: DailyTaskModel,
        adapterModels: MutableList<IScheduleModel>
    ) = business.saveCreateDailyTask(model) {
        adapterModels.addAll(it)
    }

    suspend fun mockRemoteData(weekTime: Long) {
        withContext(Dispatchers.IO) {
            mockRemoteData.getOrPut(weekTime) {
                delay(5000)
                weekTime.mockModels()
            }
        }
    }

    var id = 99L
    private fun Long.mockModels(): List<IScheduleModel> = listOf(
        DailyTaskModel(
            id = id ++,
            beginTime = this + dayMillis,
            duration = dayMillis,
            title = "整天$id"
        ),
        DailyTaskModel(
            id = id ++,
            beginTime = this + 3 * dayMillis + 6 * hourMillis,
            duration = 2 * hourMillis,
            title = "重合$id"
        ),
        DailyTaskModel(
            id = id ++,
            beginTime = this + 3 * dayMillis + 6 * hourMillis,
            duration = hourMillis,
            title = "重合$id"
        ),
        DailyTaskModel(
            id = id ++,
            beginTime = this + 4 * dayMillis + 10 * hourMillis,
            duration = 3 * hourMillis,
            title = "常规$id"
        ),
    )
}