package me.wxc.todolist.tools

import me.wxc.widget.base.DailyTaskModel
import me.wxc.widget.tools.*
import java.util.Calendar

val newUserTasks = listOf(
    DailyTaskModel(
        beginTime = nowMillis.adjustTimestamp(
            hourMillis, true
        ) - 2 * hourMillis, title = "测试1", duration = hourMillis
    ),
    DailyTaskModel(
        beginTime = nowMillis.adjustTimestamp(
            hourMillis, true
        ) - 1 * hourMillis, title = "测试2", duration = hourMillis
    ),
    DailyTaskModel(
        beginTime = nowMillis.adjustTimestamp(
            hourMillis, true
        ) - 2 * hourMillis, title = "测试3", duration = 2 * hourMillis
    ),
    DailyTaskModel(
        beginTime = nowMillis.adjustTimestamp(
            hourMillis, true
        ), title = "测试4", duration = 2 * dayMillis
    ),
    DailyTaskModel(
        beginTime = nowMillis.adjustTimestamp(
            hourMillis, true
        ) + dayMillis, title = "测试5", duration = 7 * dayMillis
    ),
)