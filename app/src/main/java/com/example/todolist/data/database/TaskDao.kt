package com.example.todolist.data.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): LiveData<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getActiveTasks(): LiveData<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY completedAt DESC")
    fun getCompletedTasks(): LiveData<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): Task?

    @Query("SELECT * FROM tasks WHERE hasAlarm = 1 AND isCompleted = 0 ORDER BY alarmTime ASC")
    fun getTasksWithAlarm(): LiveData<List<Task>>

    @Insert
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("UPDATE tasks SET isCompleted = :completed, completedAt = :completedAt WHERE id = :id")
    suspend fun updateTaskCompletion(id: Long, completed: Boolean, completedAt: Long?)

    @Query("UPDATE tasks SET alarmTime = :alarmTime, hasAlarm = :hasAlarm WHERE id = :id")
    suspend fun updateTaskAlarm(id: Long, alarmTime: Long?, hasAlarm: Boolean)

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 0")
    fun getActiveTaskCount(): LiveData<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1")
    fun getCompletedTaskCount(): LiveData<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE hasAlarm = 1 AND isCompleted = 0")
    fun getAlarmTaskCount(): LiveData<Int>
}