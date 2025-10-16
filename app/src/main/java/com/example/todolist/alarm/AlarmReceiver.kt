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

        // 直接播放声音和振动，不使用服务
        val soundManager = AlarmSoundManager(context)
        soundManager.startAlarmSound()
        soundManager.startVibration()

        // 显示简单通知
        showSimpleNotification(context, taskId, taskTitle, taskDescription, soundManager)

        Log.d("AlarmReceiver", "闹钟提醒已启动")
    }

    private fun showSimpleNotification(context: Context, taskId: Long, title: String, description: String, soundManager: AlarmSoundManager) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "simple_task_reminder",
                "任务提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setDescription("简单任务闹钟提醒")
                enableVibration(false) // 由我们自己的振动管理
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 创建关闭闹钟的Intent
        val dismissIntent = Intent(context, AlarmDismissReceiver::class.java).apply {
            putExtra("task_id", taskId)
        }

        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt() + 1000,
            dismissIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        // 构建简单通知
        val notification = NotificationCompat.Builder(context, "simple_task_reminder")
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(title)
            .setContentText(if (description.isNotEmpty()) description else "任务时间到了！")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS) // 只使用灯光，不使用默认声音和振动
            .addAction(R.drawable.ic_delete, "关闭", dismissPendingIntent)
            .build()

        notificationManager.notify(taskId.toInt(), notification)
        Log.d("AlarmReceiver", "简单通知已显示")
    }
}