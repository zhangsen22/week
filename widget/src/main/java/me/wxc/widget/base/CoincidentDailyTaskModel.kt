package me.wxc.widget.base

import me.wxc.widget.tools.dDays

data class CoincidentDailyTaskModel(
    override val beginTime: Long,
    override val endTime: Long,
    val models: List<DailyTaskModel>,
    val saveRailIndex: Boolean = false,
) : IScheduleModel {
    val rails: Map<Int, MutableList<DailyTaskModel>> = run {
        val result = sortedMapOf<Int, MutableList<DailyTaskModel>>()
        var railIndex = 0
        models.forEach {
            var hitIndex = -1
            for (i in result.keys) {
                val dailyTaskModels = result[i]
                val maxEndTime = dailyTaskModels?.maxOfOrNull { it.endTime } ?: 0L
                if (!saveRailIndex && it.beginTime >= maxEndTime) {
                    hitIndex = i
                    break
                }
                if (saveRailIndex && it.beginTime.dDays > (maxEndTime - 1).dDays) {
                    hitIndex = i
                    break
                }
            }
            if (hitIndex == -1) {
                val list = result.getOrPut(railIndex ++) {
                    mutableListOf()
                }
                list += it.apply { if (saveRailIndex) this.railIndex = railIndex - 1 }
            } else {
                val list = result.getOrPut(hitIndex) {
                    mutableListOf()
                }
                list += it.apply { if (saveRailIndex) this.railIndex = hitIndex }
            }
        }
        result.toMap()
    }
}