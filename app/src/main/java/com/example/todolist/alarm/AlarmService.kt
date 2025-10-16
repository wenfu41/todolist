package com.example.todolist.alarm

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class AlarmService : Service() {

    private var alarmSoundManager: AlarmSoundManager? = null
    private var currentTaskId: Long = 0L
    private var notificationManager: android.app.NotificationManager? = null

    override fun onCreate() {
        super.onCreate()
        alarmSoundManager = AlarmSoundManager(this)
        notificationManager = getSystemService(android.app.NotificationManager::class.java)
        Log.d("AlarmService", "闹钟服务创建")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        currentTaskId = intent?.getLongExtra("task_id", 0L) ?: 0L
        val taskTitle = intent?.getStringExtra("task_title") ?: "任务提醒"
        val taskDescription = intent?.getStringExtra("task_description") ?: ""

        Log.d("AlarmService", "闹钟服务启动，任务ID: $currentTaskId, 标题: $taskTitle")

        try {
            // 启动为前台服务（必须在启动声音之前）
            val notification = createNotification(currentTaskId, taskTitle, taskDescription)
            startForeground(currentTaskId.toInt(), notification)

            // 启动声音和振动
            alarmSoundManager?.startAlarmSound()
            alarmSoundManager?.startVibration()

            // 确保通知持续显示
            showNotification(currentTaskId, taskTitle, taskDescription)

            Log.d("AlarmService", "闹钟服务启动成功")
        } catch (e: Exception) {
            Log.e("AlarmService", "闹钟服务启动失败", e)
            // 即使启动失败，也尝试显示基本通知
            try {
                val notification = createSimpleNotification(currentTaskId, taskTitle)
                startForeground(currentTaskId.toInt(), notification)
            } catch (notificationException: Exception) {
                Log.e("AlarmService", "创建通知也失败", notificationException)
            }
        }

        return START_STICKY // 服务被杀死后自动重启
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("AlarmService", "闹钟服务即将销毁，停止声音和振动")
        alarmSoundManager?.stopAll()
        Log.d("AlarmService", "闹钟服务销毁完成")
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("AlarmService", "任务被移除，尝试重启服务")
        // 在任务被移除时尝试重启服务
        val restartServiceIntent = Intent(applicationContext, AlarmService::class.java)
        restartServiceIntent.putExtra("task_id", currentTaskId)
        restartServiceIntent.putExtra("task_title", "任务提醒")
        restartServiceIntent.putExtra("task_description", "")

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(restartServiceIntent)
        } else {
            startService(restartServiceIntent)
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun showNotification(taskId: Long, title: String, description: String) {
        try {
            // 更新前台服务通知
            val notification = createNotification(taskId, title, description)
            notificationManager?.notify(taskId.toInt(), notification)

            Log.d("AlarmService", "通知已更新，任务ID: $taskId")
        } catch (e: Exception) {
            Log.e("AlarmService", "更新通知失败", e)
        }
    }

    private fun createSimpleNotification(taskId: Long, title: String): android.app.Notification {
        val channelId = "alarm_service_simple"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "简单闹钟提醒",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                this.description = "闹钟提醒通知"
                setShowBadge(true)
            }
            notificationManager?.createNotificationChannel(channel)
        }

        return android.app.Notification.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText("任务时间到了！")
            .setSmallIcon(com.example.todolist.R.drawable.ic_alarm)
            .setPriority(android.app.Notification.PRIORITY_HIGH)
            .setAutoCancel(false)
            .build()
    }

    private fun createNotification(taskId: Long, title: String, description: String): android.app.Notification {
        val channelId = "alarm_service_channel"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "闹钟服务",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                this.description = "闹钟响铃服务"
                setShowBadge(true)
                enableVibration(true)
                enableLights(true)
                setSound(android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI,
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .build())
                // 设置是否可以绕过勿扰模式 (需要API级别28+)
                // 注意：bypassDnd 在某些版本中可能不可用，暂时注释掉
                // if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                //     bypassDnd = true
                // }
            }

            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        // 创建点击通知后的Intent
        val mainIntent = Intent(this, com.example.todolist.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = android.app.PendingIntent.getActivity(
            this,
            taskId.toInt(),
            mainIntent,
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            } else {
                android.app.PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        // 创建关闭闹钟的Intent
        val dismissIntent = Intent(this, AlarmDismissReceiver::class.java).apply {
            putExtra("task_id", taskId)
        }

        val dismissPendingIntent = android.app.PendingIntent.getBroadcast(
            this,
            taskId.toInt() + 1000,
            dismissIntent,
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            } else {
                android.app.PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        // 创建稍后提醒的Intent
        val snoozeIntent = Intent(this, AlarmSnoozeReceiver::class.java).apply {
            putExtra("task_id", taskId)
            putExtra("task_title", title)
            putExtra("task_description", description)
        }

        val snoozePendingIntent = android.app.PendingIntent.getBroadcast(
            this,
            taskId.toInt() + 2000,
            snoozeIntent,
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            } else {
                android.app.PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        return android.app.Notification.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(if (description.isNotEmpty()) description else "任务时间到了！")
            .setSmallIcon(com.example.todolist.R.drawable.ic_alarm)
            .setPriority(android.app.Notification.PRIORITY_HIGH)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .addAction(
                android.app.Notification.Action.Builder(
                    com.example.todolist.R.drawable.ic_delete,
                    "关闭闹钟",
                    dismissPendingIntent
                ).build()
            )
            .addAction(
                android.app.Notification.Action.Builder(
                    com.example.todolist.R.drawable.ic_add,
                    "稍后提醒",
                    snoozePendingIntent
                ).build()
            )
            .build()
    }

    fun stopAlarmService() {
        alarmSoundManager?.stopAll()
        stopSelf()
        Log.d("AlarmService", "闹钟服务停止")
    }
}