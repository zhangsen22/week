package me.wxc.todolist.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import me.wxc.todolist.tools.Preferences
import me.wxc.todolist.tools.newUserTasks
import me.wxc.widget.ScheduleConfig
import me.wxc.widget.base.*
import me.wxc.widget.base.DailyTaskModel
import me.wxc.widget.tools.*
import kotlinx.coroutines.launch
import me.wxc.todolist.databinding.FragmentMainBinding
import me.wxc.widget.base.ICalendarParent

class MainFragment : Fragment(), ICalendarParent {
    private val moreViewModel by viewModels<MainViewModel>()
    private lateinit var binding: FragmentMainBinding

    override val childRenders: List<ICalendarRender> by lazy {
        listOf(
            binding.scheduleGroup,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeNewUser()
        initializeScheduleConfig()
    }


    private fun initializeNewUser() {
        if (Preferences.getBoolean("newUser", true)) {
            lifecycleScope.launch {
                val list = mutableListOf<IScheduleModel>()
                newUserTasks.forEach {
                    moreViewModel.saveCreateDailyTask(it, list)
                }
                childRenders.onEach { it.reloadSchedulesFromProvider() }
            }
            Preferences.putBoolean("newUser", false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (!::binding.isInitialized) {
            binding = FragmentMainBinding.inflate(layoutInflater)
            onBindingInit()
        }
        return binding.root
    }

    private fun onBindingInit() {
        binding.yyyyM.text = beginOfDay().yyyyM
        binding.today.setOnClickListener {
            ScheduleConfig.selectedDayTime = nowMillis
            binding.yyyyM.text = nowMillis.yyyyM
            childRenders.filter { it.isVisible() }.onEach {
                it.selectedDayTime = nowMillis
            }
        }
        binding.fab.setOnClickListener {
            ScheduleConfig.onCreateTaskClickBlock(
                DailyTaskModel(
                    beginTime = beginOfDay(ScheduleConfig.selectedDayTime).timeInMillis + 10 * hourMillis,
                    duration = quarterMillis * 2,
                )
            )
        }
        binding.more.setOnClickListener {
            ScheduleConfig.lunarEnable = !ScheduleConfig.lunarEnable
            binding.scheduleGroup.lunarEnable = ScheduleConfig.lunarEnable
        }
        binding.scheduleGroup.selectedDayTime = nowMillis.calendar.firstDayOfWeekTime
    }

    private fun initializeScheduleConfig() {
        ScheduleConfig.run {
            onDateSelectedListener = {
                Log.i(TAG, "date select: $yyyyMMddHHmmss")
                binding.yyyyM.text = yyyyM
                selectedDayTime = timeInMillis
            }
            scheduleModelsProvider = { beginTime, endTime ->
                moreViewModel.getRangeDailyTask(beginTime, endTime)
            }
            lifecycleScope = this@MainFragment.lifecycleScope
            onDailyTaskClickBlock = { model ->
                DetailsFragment().apply {
                    taskModel = model.copy()
                    onDeleteBlock = { ids ->
                        childRenders.onEach { it.reloadSchedulesFromProvider() }
                    }
                    onSaveBlock = {
                        Log.i(TAG, "daily task added: ${model.title}")
                        childRenders.onEach { it.reloadSchedulesFromProvider() }
                    }
                }.show(childFragmentManager, "DetailsFragment")
            }
            onMoreTaskClickBlock = {
                Toast.makeText(requireContext(), it.models.joinToString { it.title }, Toast.LENGTH_SHORT).show()
            }
            onCreateTaskClickBlock = { model ->
                DetailsFragment().apply {
                    taskModel = model.copy()
                    onDeleteBlock = { ids ->
                        childRenders.onEach { it.reloadSchedulesFromProvider() }
                    }
                    onSaveBlock = {
                        Log.i(TAG, "daily task added: ${model.title}")
                        childRenders.onEach { it.reloadSchedulesFromProvider() }
                    }
                }.show(childFragmentManager, "DetailsFragment")
                Toast.makeText(requireContext(), "开始创建日程", Toast.LENGTH_SHORT).show()
            }
            onTaskDraggedBlock = { model ->
                lifecycleScope.launch {
                    moreViewModel.updateSingleDailyTask(model)
                    childRenders.onEach { it.reloadSchedulesFromProvider() }
                }
            }
            onWeekSelectedBlock = { time ->
                lifecycleScope.launch {
                    moreViewModel.mockRemoteData(time)
                    childRenders.onEach { it.reloadSchedulesFromProvider() }
                }
            }
        }
    }

    companion object {
        private const val TAG = "MoreFragment"
    }
}