package com.example.todolist.data.repository

import androidx.lifecycle.LiveData
import com.example.todolist.data.database.Task
import com.example.todolist.data.database.TaskDao

class TaskRepository(private val taskDao: TaskDao) {

    fun getAllTasks(): LiveData<List<Task>> = taskDao.getAllTasks()
    fun getActiveTasks(): LiveData<List<Task>> = taskDao.getActiveTasks()
    fun getCompletedTasks(): LiveData<List<Task>> = taskDao.getCompletedTasks()
    fun getActiveTaskCount(): LiveData<Int> = taskDao.getActiveTaskCount()
    fun getCompletedTaskCount(): LiveData<Int> = taskDao.getCompletedTaskCount()

    suspend fun insertTask(task: Task): Long {
        return taskDao.insertTask(task)
    }

    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }

    suspend fun deleteTaskById(id: Long) {
        taskDao.deleteTaskById(id)
    }

    suspend fun updateTaskCompletion(id: Long, completed: Boolean) {
        val completedAt = if (completed) System.currentTimeMillis() else null
        taskDao.updateTaskCompletion(id, completed, completedAt)
    }

    suspend fun getTaskById(id: Long): Task? {
        return taskDao.getTaskById(id)
    }
}