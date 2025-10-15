package com.example.todolist.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.todolist.data.database.Task
import com.example.todolist.data.database.TodoDatabase
import com.example.todolist.data.repository.TaskRepository
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskRepository

    val allTasks: LiveData<List<Task>>
    val activeTasks: LiveData<List<Task>>
    val completedTasks: LiveData<List<Task>>
    val activeTaskCount: LiveData<Int>
    val completedTaskCount: LiveData<Int>

    // 当前筛选状态
    private val _currentFilter = MutableLiveData<TaskFilter>(TaskFilter.ALL)
    val currentFilter: LiveData<TaskFilter> = _currentFilter

    // 根据筛选条件显示的任务
    private val _filteredTasks = MediatorLiveData<List<Task>>()
    val filteredTasks: LiveData<List<Task>> = _filteredTasks

    init {
        val taskDao = TodoDatabase.getDatabase(application).taskDao()
        repository = TaskRepository(taskDao)

        allTasks = repository.getAllTasks()
        activeTasks = repository.getActiveTasks()
        completedTasks = repository.getCompletedTasks()
        activeTaskCount = repository.getActiveTaskCount()
        completedTaskCount = repository.getCompletedTaskCount()

        setupFilteredTasks()
    }

    private fun setupFilteredTasks() {
        _filteredTasks.addSource(_currentFilter) { filter ->
            updateFilteredTasks(filter)
        }

        _filteredTasks.addSource(allTasks) { tasks ->
            if (_currentFilter.value == TaskFilter.ALL) {
                _filteredTasks.value = tasks
            }
        }

        _filteredTasks.addSource(activeTasks) { tasks ->
            if (_currentFilter.value == TaskFilter.ACTIVE) {
                _filteredTasks.value = tasks
            }
        }

        _filteredTasks.addSource(completedTasks) { tasks ->
            if (_currentFilter.value == TaskFilter.COMPLETED) {
                _filteredTasks.value = tasks
            }
        }
    }

    private fun updateFilteredTasks(filter: TaskFilter) {
        _filteredTasks.value = when (filter) {
            TaskFilter.ALL -> allTasks.value ?: emptyList()
            TaskFilter.ACTIVE -> activeTasks.value ?: emptyList()
            TaskFilter.COMPLETED -> completedTasks.value ?: emptyList()
        }
    }

    enum class TaskFilter {
        ALL, ACTIVE, COMPLETED
    }

    fun setFilter(filter: TaskFilter) {
        _currentFilter.value = filter
    }

    fun insertTask(title: String, description: String) = viewModelScope.launch {
        val task = Task(
            title = title,
            description = description,
            isCompleted = false
        )
        repository.insertTask(task)
    }

    fun updateTask(task: Task) = viewModelScope.launch {
        repository.updateTask(task)
    }

    fun deleteTask(task: Task) = viewModelScope.launch {
        repository.deleteTask(task)
    }

    fun toggleTaskCompletion(task: Task) = viewModelScope.launch {
        val updatedTask = task.copy(
            isCompleted = !task.isCompleted,
            completedAt = if (!task.isCompleted) System.currentTimeMillis() else null
        )
        repository.updateTask(updatedTask)
    }

    fun deleteTaskById(id: Long) = viewModelScope.launch {
        repository.deleteTaskById(id)
    }
}