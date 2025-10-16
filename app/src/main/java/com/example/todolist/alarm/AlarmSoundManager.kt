package com.example.todolist.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log

class AlarmSoundManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var isPlaying = false
    private var retryCount = 0
    private val maxRetries = 3
    private var handler: Handler? = null
    private var stopSoundRunnable: Runnable? = null
    private val ALARM_DURATION_MS = 10 * 1000L // 10秒

    fun startAlarmSound() {
        if (isPlaying) {
            Log.d("AlarmSoundManager", "闹钟已经在播放中")
            return
        }

        try {
            // 初始化音频管理器
            audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            // 获取WakeLock确保设备不会休眠
            acquireWakeLock()

            // 停止之前的播放
            stopAlarmSound()

            // 初始化Handler
            handler = Handler(Looper.getMainLooper())

            // 请求音频焦点
            requestAudioFocus()

            // 使用内置的音频文件
            try {
                val resourceId = context.resources.getIdentifier("alarm_sound", "raw", context.packageName)
                if (resourceId != 0) {
                    Log.d("AlarmSoundManager", "使用内置音频文件")
                    mediaPlayer = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .setLegacyStreamType(AudioManager.STREAM_ALARM)
                                .build()
                        )
                        // 设置音频数据源（音频文件已经是10秒长度）
                        val assetFileDescriptor = context.resources.openRawResourceFd(resourceId)
                        setDataSource(assetFileDescriptor.fileDescriptor, assetFileDescriptor.startOffset, assetFileDescriptor.length)
                        assetFileDescriptor.close()
                        isLooping = true
                        setVolume(1.0f, 1.0f) // 设置最大音量

                        prepareAsync()
                        setOnPreparedListener {
                            try {
                                start()
                                startPlaybackSuccess()
                                Log.d("AlarmSoundManager", "内置音频开始播放，将在10秒后自动停止")

                                // 设置10秒后自动停止
                                stopSoundRunnable = Runnable {
                                    Log.d("AlarmSoundManager", "10秒时间到，自动停止铃声")
                                    stopAlarmSound()
                                }
                                handler?.postDelayed(stopSoundRunnable!!, ALARM_DURATION_MS)
                            } catch (e: Exception) {
                                Log.e("AlarmSoundManager", "启动播放失败", e)
                                startPlaybackFailure()
                            }
                        }
                        setOnErrorListener { _, what, extra ->
                            Log.e("AlarmSoundManager", "播放错误: what=$what, extra=$extra")
                            startPlaybackFailure()
                            true
                        }
                    }
                } else {
                    Log.e("AlarmSoundManager", "未找到内置音频文件")
                    isPlaying = false
                }
            } catch (e: Exception) {
                Log.e("AlarmSoundManager", "创建MediaPlayer失败", e)
                isPlaying = false
            }
        } catch (e: Exception) {
            Log.e("AlarmSoundManager", "启动闹钟声音失败", e)
            isPlaying = false
        }
    }

    fun startVibration() {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ 使用 VibratorManager
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                // Android 12以下使用 Vibrator
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            // 设置振动模式：振动1秒，停止0.3秒，重复（持续10秒）
            val vibrationPattern = longArrayOf(0, 1000, 300) // 振动1秒，停止0.3秒
            vibrator?.vibrate(vibrationPattern, 0) // 从索引0开始重复
            Log.d("AlarmSoundManager", "振动开始，将在10秒后自动停止，模式：${vibrationPattern.contentToString()}")

            // 设置10秒后自动停止振动
            handler?.postDelayed({
                Log.d("AlarmSoundManager", "10秒时间到，自动停止振动")
                stopVibration()
            }, ALARM_DURATION_MS)
        } catch (e: Exception) {
            Log.e("AlarmSoundManager", "启动振动失败", e)
        }
    }

    fun stopAlarmSound() {
        isPlaying = false

        // 清除自动停止的定时器
        stopSoundRunnable?.let { runnable ->
            handler?.removeCallbacks(runnable)
            stopSoundRunnable = null
        }

        try {
            mediaPlayer?.apply {
                if (this.isPlaying) {
                    stop()
                }
                reset()
                release()
            }
            mediaPlayer = null

            // 释放音频焦点
            audioManager?.abandonAudioFocus(null)

            Log.d("AlarmSoundManager", "闹钟声音停止")
        } catch (e: Exception) {
            Log.e("AlarmSoundManager", "停止声音失败", e)
        }
    }

    fun stopVibration() {
        try {
            vibrator?.cancel()
            vibrator = null
            Log.d("AlarmSoundManager", "振动停止")
        } catch (e: Exception) {
            Log.e("AlarmSoundManager", "停止振动失败", e)
        }
    }

    fun stopAll() {
        stopAlarmSound()
        stopVibration()

        // 释放WakeLock
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            wakeLock = null
            Log.d("AlarmSoundManager", "WakeLock释放")
        } catch (e: Exception) {
            Log.e("AlarmSoundManager", "释放WakeLock失败", e)
        }
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "TodoList:AlarmWakeLock"
            )
            wakeLock?.acquire(10 * 60 * 1000L) // 保持10分钟
            Log.d("AlarmSoundManager", "WakeLock获取成功")
        } catch (e: Exception) {
            Log.e("AlarmSoundManager", "获取WakeLock失败", e)
        }
    }

    private fun requestAudioFocus() {
        try {
            val result = audioManager?.requestAudioFocus(
                null,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
            Log.d("AlarmSoundManager", "音频焦点请求结果: $result")
        } catch (e: Exception) {
            Log.e("AlarmSoundManager", "请求音频焦点失败", e)
        }
    }

    private fun startPlaybackSuccess() {
        isPlaying = true
        retryCount = 0
    }

    private fun startPlaybackFailure() {
        isPlaying = false
    }

    }