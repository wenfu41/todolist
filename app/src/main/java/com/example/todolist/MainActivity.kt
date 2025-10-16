package com.example.todolist

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.todolist.databinding.ActivityMainBinding
import com.example.todolist.databinding.DialogAddTaskBinding
import com.example.todolist.ui.adapter.TaskAdapter
import com.example.todolist.ui.viewmodel.TaskViewModel
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var taskViewModel: TaskViewModel
    private lateinit var taskAdapter: TaskAdapter

  
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 不在启动时请求权限，只在用户需要闹钟功能时才请求
        // requestAlarmPermissions()

        initViewModel()
        initRecyclerView()
        initListeners()
        updateDate()
        observeViewModel()
    }

    
    private fun initViewModel() {
        taskViewModel = ViewModelProvider(this)[TaskViewModel::class.java]
    }

    private fun initRecyclerView() {
        taskAdapter = TaskAdapter()
        binding.rvTasks.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = taskAdapter
        }

        taskAdapter.setOnTaskCheckedChangeListener(object : TaskAdapter.OnTaskCheckedChangeListener {
            override fun onTaskChanged(task: com.example.todolist.data.database.Task, isCompleted: Boolean) {
                taskViewModel.toggleTaskCompletion(task)
            }
        })

        taskAdapter.setOnTaskDeleteListener(object : TaskAdapter.OnTaskDeleteListener {
            override fun onTaskDelete(task: com.example.todolist.data.database.Task) {
                showDeleteConfirmationDialog(task)
            }
        })

        taskAdapter.setOnTaskEditListener(object : TaskAdapter.OnTaskEditListener {
            override fun onTaskEdit(task: com.example.todolist.data.database.Task) {
                showEditTaskDialog(task)
            }
        })
    }

    private fun initListeners() {
        binding.fabAddTask.setOnClickListener {
            showAddTaskDialog()
        }

        // 筛选标签点击事件
        binding.tvAllTasks.setOnClickListener {
            updateFilterUI(TaskViewModel.TaskFilter.ALL)
            taskViewModel.setFilter(TaskViewModel.TaskFilter.ALL)
        }

        binding.tvActiveTasks.setOnClickListener {
            updateFilterUI(TaskViewModel.TaskFilter.ACTIVE)
            taskViewModel.setFilter(TaskViewModel.TaskFilter.ACTIVE)
        }

        binding.tvCompletedTasks.setOnClickListener {
            updateFilterUI(TaskViewModel.TaskFilter.COMPLETED)
            taskViewModel.setFilter(TaskViewModel.TaskFilter.COMPLETED)
        }
    }

    private fun updateDate() {
        val dateFormat = SimpleDateFormat("MM月dd日", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        binding.tvDate.text = "今天，$currentDate"
    }

    private fun observeViewModel() {
        // 观察筛选后的任务列表
        taskViewModel.filteredTasks.observe(this) { tasks ->
            taskAdapter.submitList(tasks)
            updateEmptyState(tasks.isEmpty())
        }

        taskViewModel.activeTaskCount.observe(this) { count ->
            binding.tvActiveCount.text = count.toString()
        }

        taskViewModel.completedTaskCount.observe(this) { count ->
            binding.tvCompletedCount.text = count.toString()
        }

        // 观察全部任务数量
        taskViewModel.allTasks.observe(this) { tasks ->
            binding.tvAllCount.text = tasks.size.toString()
        }

        // 初始化筛选状态
        updateFilterUI(TaskViewModel.TaskFilter.ALL)
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        val currentFilter = taskViewModel.currentFilter.value ?: TaskViewModel.TaskFilter.ALL

        if (isEmpty) {
            binding.emptyState.visibility = View.VISIBLE
            binding.rvTasks.visibility = View.GONE

            // 根据筛选状态显示不同的空状态提示
            val emptyText = when (currentFilter) {
                TaskViewModel.TaskFilter.ALL -> "还没有任务"
                TaskViewModel.TaskFilter.ACTIVE -> "没有进行中的任务"
                TaskViewModel.TaskFilter.COMPLETED -> "没有已完成的任务"
            }

            val hintText = when (currentFilter) {
                TaskViewModel.TaskFilter.ALL -> "点击右下角按钮添加第一个任务"
                TaskViewModel.TaskFilter.ACTIVE -> "所有任务都已完成！"
                TaskViewModel.TaskFilter.COMPLETED -> "还没有完成任何任务"
            }

            // 更新空状态文字
            (binding.emptyState.getChildAt(1) as TextView).text = emptyText
            (binding.emptyState.getChildAt(2) as TextView).text = hintText
        } else {
            binding.emptyState.visibility = View.GONE
            binding.rvTasks.visibility = View.VISIBLE
        }
    }

    private fun updateFilterUI(filter: TaskViewModel.TaskFilter) {
        // 重置所有标签样式
        binding.tvAllTasks.setBackgroundResource(R.drawable.filter_tab_unselected)
        binding.tvActiveTasks.setBackgroundResource(R.drawable.filter_tab_unselected)
        binding.tvCompletedTasks.setBackgroundResource(R.drawable.filter_tab_unselected)

        binding.tvAllTasks.alpha = 0.7f
        binding.tvActiveTasks.alpha = 0.7f
        binding.tvCompletedTasks.alpha = 0.7f

        // 设置选中标签样式
        when (filter) {
            TaskViewModel.TaskFilter.ALL -> {
                binding.tvAllTasks.setBackgroundResource(R.drawable.filter_tab_selected)
                binding.tvAllTasks.alpha = 1.0f
            }
            TaskViewModel.TaskFilter.ACTIVE -> {
                binding.tvActiveTasks.setBackgroundResource(R.drawable.filter_tab_selected)
                binding.tvActiveTasks.alpha = 1.0f
            }
            TaskViewModel.TaskFilter.COMPLETED -> {
                binding.tvCompletedTasks.setBackgroundResource(R.drawable.filter_tab_selected)
                binding.tvCompletedTasks.alpha = 1.0f
            }
        }
    }

    private fun showAddTaskDialog() {
        val dialogBinding = DialogAddTaskBinding.inflate(LayoutInflater.from(this))

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // 闹钟相关变量
        var hasAlarm = false
        var selectedAlarmTime: Long? = null
        val titleText = dialogBinding.root.findViewById<TextView>(R.id.title_text)

        // 闹钟开关点击事件
        dialogBinding.switchAlarm.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // 直接开启闹钟功能，不检查权限
                hasAlarm = true
                showAlarmTimePicker(dialogBinding, titleText) { time ->
                    selectedAlarmTime = time
                }
            } else {
                hasAlarm = false
                // 隐藏时间选择器
                dialogBinding.alarmTimeContainer.visibility = View.GONE
                selectedAlarmTime = null
                // 恢复原始标题
                titleText.text = "添加新任务"
            }
        }

        // 请求标题输入框的焦点
        dialogBinding.etTaskTitle.requestFocus()

        // 显示软键盘
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        dialogBinding.btnAdd.setOnClickListener {
            val title = dialogBinding.etTaskTitle.text?.toString()?.trim() ?: ""
            val description = dialogBinding.etTaskDescription.text?.toString()?.trim() ?: ""

            if (title.isNotEmpty()) {
                // 如果设置了闹钟但还没选择时间，使用当前时间
                if (hasAlarm && selectedAlarmTime == null) {
                    val calendar = Calendar.getInstance()
                    selectedAlarmTime = calendar.timeInMillis
                }

                val alarmTime = selectedAlarmTime
                if (alarmTime != null) {
                    val format = SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault())
                    val formattedTime = format.format(Date(alarmTime))
                    Log.d("MainActivity", "插入任务: 标题=$title, 闹钟时间=$formattedTime, 有闹钟=$hasAlarm")
                } else {
                    Log.d("MainActivity", "插入任务: 标题=$title, 无闹钟时间, 有闹钟=$hasAlarm")
                }

                taskViewModel.insertTask(title, description, selectedAlarmTime, hasAlarm)
                dialog.dismiss()
            } else {
                dialogBinding.etTaskTitle.error = "请输入任务标题"
                // 保持焦点在标题输入框
                dialogBinding.etTaskTitle.requestFocus()
            }
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()

        // 确保在对话框显示后再次请求焦点
        dialogBinding.etTaskTitle.post {
            dialogBinding.etTaskTitle.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(dialogBinding.etTaskTitle, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun showEditTaskDialog(task: com.example.todolist.data.database.Task) {
        val dialogBinding = DialogAddTaskBinding.inflate(LayoutInflater.from(this))

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // 填充现有数据
        dialogBinding.etTaskTitle.setText(task.title)
        dialogBinding.etTaskDescription.setText(task.description)

        // 闹钟相关变量
        var hasAlarm = task.hasAlarm
        var selectedAlarmTime = task.alarmTime
        val titleText = dialogBinding.root.findViewById<TextView>(R.id.title_text)

        // 设置对话框标题为编辑模式
        titleText.text = "编辑任务"

        // 设置闹钟开关状态
        dialogBinding.switchAlarm.isChecked = hasAlarm

        // 设置时间变化监听（先设置，避免后续设置时间时触发）
        dialogBinding.dateTimePicker.onDateTimeChangedListener = { year, month, day, hour, minute ->
            val timeCalendar = Calendar.getInstance()
            timeCalendar.set(year, month - 1, day, hour, minute, 0)
            selectedAlarmTime = timeCalendar.timeInMillis

            // 直接更新标题
            val format = SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault())
            titleText.text = "编辑任务 - $format"

            Log.d("MainActivity", "编辑时间选择器更新: $format")
        }

        // 如果原有闹钟，显示时间选择器并更新标题
        if (hasAlarm && task.alarmTime != null) {
            dialogBinding.alarmTimeContainer.visibility = View.VISIBLE
            dialogBinding.dateTimePicker.setDateTime(task.alarmTime)

            // 更新标题显示原有时间
            val format = SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault())
            titleText.text = "编辑任务 - $format"
        }

        // 闹钟开关点击事件
        dialogBinding.switchAlarm.setOnCheckedChangeListener { _, isChecked ->
            hasAlarm = isChecked
            if (isChecked) {
                // 显示时间选择器
                dialogBinding.alarmTimeContainer.visibility = View.VISIBLE

                // 如果没有原有的闹钟时间，设置为当前时间
                val currentTime = selectedAlarmTime
                if (currentTime == null) {
                    val calendar = Calendar.getInstance()
                    selectedAlarmTime = calendar.timeInMillis
                    dialogBinding.dateTimePicker.setDateTime(calendar.timeInMillis)

                    // 更新标题显示新时间
                    val format = SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault())
                    titleText.text = "编辑任务 - $format"
                } else {
                    // 使用原有时间
                    dialogBinding.dateTimePicker.setDateTime(currentTime)

                    // 更新标题显示原有时间
                    val format = SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault())
                    titleText.text = "编辑任务 - $format"
                }

            } else {
                // 隐藏时间选择器
                dialogBinding.alarmTimeContainer.visibility = View.GONE
                selectedAlarmTime = null

                // 恢复原始标题
                titleText.text = "编辑任务"
            }
        }

        // 更改按钮文本为"更新"
        dialogBinding.btnAdd.text = "更新"

        // 请求标题输入框的焦点
        dialogBinding.etTaskTitle.requestFocus()

        // 显示软键盘
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        dialogBinding.btnAdd.setOnClickListener {
            val title = dialogBinding.etTaskTitle.text?.toString()?.trim() ?: ""
            val description = dialogBinding.etTaskDescription.text?.toString()?.trim() ?: ""

            if (title.isNotEmpty()) {
                // 如果设置了闹钟但还没选择时间，使用当前时间
                if (hasAlarm && selectedAlarmTime == null) {
                    val calendar = Calendar.getInstance()
                    selectedAlarmTime = calendar.timeInMillis
                }

                // 创建更新后的任务对象
                val updatedTask = task.copy(
                    title = title,
                    description = description,
                    alarmTime = selectedAlarmTime,
                    hasAlarm = hasAlarm
                )

                Log.d("MainActivity", "更新任务: 标题=$title, 闹钟时间=$selectedAlarmTime, 有闹钟=$hasAlarm")

                taskViewModel.updateTask(updatedTask)
                dialog.dismiss()
            } else {
                dialogBinding.etTaskTitle.error = "请输入任务标题"
                // 保持焦点在标题输入框
                dialogBinding.etTaskTitle.requestFocus()
            }
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()

        // 确保在对话框显示后再次请求焦点
        dialogBinding.etTaskTitle.post {
            dialogBinding.etTaskTitle.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(dialogBinding.etTaskTitle, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun showDeleteConfirmationDialog(task: com.example.todolist.data.database.Task) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("删除任务")
            .setMessage("确定要删除任务「${task.title}」吗？\n${task.description}")
            .setPositiveButton("删除") { _, _ ->
                taskViewModel.deleteTask(task)
            }
            .setNegativeButton("取消", null)
            .create()

        dialog.show()

        // 将删除按钮设置为红色
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.delete_red))
    }

    private fun showAlarmTimePicker(dialogBinding: DialogAddTaskBinding, titleText: TextView, onTimeSelected: (Long) -> Unit) {
        // 显示时间选择器
        dialogBinding.alarmTimeContainer.visibility = View.VISIBLE

        // 设置默认时间为当前时间
        val calendar = Calendar.getInstance()

        // 设置时间选择器的时间（先设置时间，避免监听器被触发）
        dialogBinding.dateTimePicker.setDateTime(calendar.timeInMillis)

        // 初始化并更新标题
        onTimeSelected(calendar.timeInMillis)
        val format = SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault())
        titleText.text = "添加新任务 - $format"

        // 设置时间变化监听（在时间设置之后）
        dialogBinding.dateTimePicker.onDateTimeChangedListener = { year, month, day, hour, minute ->
            val timeCalendar = Calendar.getInstance()
            timeCalendar.set(year, month - 1, day, hour, minute, 0)
            val selectedTime = timeCalendar.timeInMillis
            onTimeSelected(selectedTime)

            // 直接更新标题
            val format = SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault())
            titleText.text = "添加新任务 - $format"

            Log.d("MainActivity", "时间选择器更新: $format")
        }
    }
}