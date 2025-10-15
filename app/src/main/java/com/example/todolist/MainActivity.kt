package com.example.todolist

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
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

        // 请求标题输入框的焦点
        dialogBinding.etTaskTitle.requestFocus()

        // 显示软键盘
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        dialogBinding.btnAdd.setOnClickListener {
            val title = dialogBinding.etTaskTitle.text?.toString()?.trim() ?: ""
            val description = dialogBinding.etTaskDescription.text?.toString()?.trim() ?: ""

            if (title.isNotEmpty()) {
                taskViewModel.insertTask(title, description)
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
        AlertDialog.Builder(this)
            .setTitle("删除任务")
            .setMessage("确定要删除任务「${task.title}」吗？\n${task.description}")
            .setPositiveButton("删除") { _, _ ->
                taskViewModel.deleteTask(task)
            }
            .setNegativeButton("取消", null)
            .show()
    }
}