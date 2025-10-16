package com.example.todolist.alarm

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("task_id", 0L)

        Log.d("AlarmDismissReceiver", "关闭闹钟，任务ID: $taskId")

        // 创建声音管理器并停止所有声音和振动
        val soundManager = AlarmSoundManager(context)
        soundManager.stopAll()

        // 取消通知
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(taskId.toInt())

        Log.d("AlarmDismissReceiver", "闹钟已停止")
    }
}