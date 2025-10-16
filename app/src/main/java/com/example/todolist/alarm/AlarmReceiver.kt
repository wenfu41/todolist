package com.example.todolist.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.todolist.MainActivity
import com.example.todolist.R

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "闹钟触发")

        val taskId = intent.getLongExtra("task_id", 0L)
        val taskTitle = intent.getStringExtra("task_title") ?: "任务提醒"
        val taskDescription = intent.getStringExtra("task_description") ?: ""

        Log.d("AlarmReceiver", "任务ID: $taskId, 标题: $taskTitle, 描述: $taskDescription")

        showNotification(context, taskId, taskTitle, taskDescription)
    }

    private fun showNotification(context: Context, taskId: Long, title: String, description: String) {
        Log.d("AlarmReceiver", "显示通知开始")
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 创建通知渠道（Android 8.0+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "task_reminder",
                "任务提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                this.description = "任务闹钟提醒"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
            Log.d("AlarmReceiver", "通知渠道创建完成")
        }

        // 创建点击通知后的Intent
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            mainIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        // 创建关闭闹钟的Intent
        val dismissIntent = Intent(context, AlarmDismissReceiver::class.java).apply {
            putExtra("task_id", taskId)
        }

        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt() + 1000, // 避免与主通知冲突
            dismissIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        // 创建稍后提醒的Intent
        val snoozeIntent = Intent(context, AlarmSnoozeReceiver::class.java).apply {
            putExtra("task_id", taskId)
            putExtra("task_title", title)
            putExtra("task_description", description)
        }

        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt() + 2000,
            snoozeIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        // 构建通知
        val notification = NotificationCompat.Builder(context, "task_reminder")
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(title)
            .setContentText(if (description.isNotEmpty()) description else "任务时间到了！")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                if (description.isNotEmpty()) description else "任务时间到了！\n点击查看任务详情。"
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_delete, "关闭", dismissPendingIntent)
            .addAction(R.drawable.ic_add, "稍后提醒", snoozePendingIntent)
            .build()

        Log.d("AlarmReceiver", "通知构建完成，准备显示")

        // 显示通知
        notificationManager.notify(taskId.toInt(), notification)
        Log.d("AlarmReceiver", "通知已显示，ID: ${taskId.toInt()}")
    }
}