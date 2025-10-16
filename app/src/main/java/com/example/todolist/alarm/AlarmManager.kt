package com.example.todolist.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.todolist.data.database.Task

class AlarmManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun setAlarm(task: Task) {
        if (task.alarmTime == null || !task.hasAlarm) return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("task_id", task.id)
            putExtra("task_title", task.title)
            putExtra("task_description", task.description)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    // Android 12+ 使用精确闹钟
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            task.alarmTime,
                            pendingIntent
                        )
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    // Android 6.0-11
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        task.alarmTime,
                        pendingIntent
                    )
                }
                else -> {
                    // Android 6.0以下
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        task.alarmTime,
                        pendingIntent
                    )
                }
            }
        } catch (e: SecurityException) {
            // 处理权限不足的情况
            e.printStackTrace()
        }
    }

    fun cancelAlarm(taskId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        alarmManager.cancel(pendingIntent)
    }

    fun updateAlarm(task: Task) {
        cancelAlarm(task.id)
        if (task.hasAlarm) {
            setAlarm(task)
        }
    }
}