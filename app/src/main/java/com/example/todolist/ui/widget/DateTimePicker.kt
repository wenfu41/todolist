package com.example.todolist.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import androidx.recyclerview.widget.LinearSnapHelper
import com.example.todolist.R
import com.example.todolist.ui.widget.adapter.NumberPickerAdapter
import java.text.SimpleDateFormat
import java.util.*

class DateTimePicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var monthPicker: RecyclerView
    private lateinit var dayPicker: RecyclerView
    private lateinit var hourPicker: RecyclerView
    private lateinit var minutePicker: RecyclerView

    private lateinit var monthAdapter: NumberPickerAdapter
    private lateinit var dayAdapter: NumberPickerAdapter
    private lateinit var hourAdapter: NumberPickerAdapter
    private lateinit var minuteAdapter: NumberPickerAdapter

    private var currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private var currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
    private var currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    private var currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    private var currentMinute = Calendar.getInstance().get(Calendar.MINUTE)

    var onDateTimeChangedListener: ((year: Int, month: Int, day: Int, hour: Int, minute: Int) -> Unit)? = null

    // 添加标志防止初始化时触发监听器
    private var isInitializing = false

    init {
        LayoutInflater.from(context).inflate(R.layout.widget_date_time_picker, this, true)

        monthPicker = findViewById(R.id.monthPicker)
        dayPicker = findViewById(R.id.dayPicker)
        hourPicker = findViewById(R.id.hourPicker)
        minutePicker = findViewById(R.id.minutePicker)

        setupPickers()
    }

    private fun setupPickers() {
        // 月份选择器 (1-12)
        val months = (1..12).toList()
        monthAdapter = NumberPickerAdapter(months) { month ->
            currentMonth = month
            notifyDateTimeChanged()
        }
        monthPicker.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = monthAdapter
            val monthSnapHelper = LinearSnapHelper()
            monthSnapHelper.attachToRecyclerView(this)

            // 完全按照日选择器的滚动监听逻辑
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        val snappedView = monthSnapHelper.findSnapView(layoutManager)
                        val position = snappedView?.let { layoutManager?.getPosition(it) } ?: 0
                        monthAdapter.setSelectedPosition(position)
                        // 更新当前月份（和日选择器逻辑完全一致）
                        currentMonth = position + 1

                        notifyDateTimeChanged()
                    }
                }
            })
        }

        // 小时选择器 (0-23)
        val hours = (0..23).toList()
        hourAdapter = NumberPickerAdapter(hours) { hour ->
            currentHour = hour
            notifyDateTimeChanged()
        }
        hourPicker.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = hourAdapter
            val hourSnapHelper = LinearSnapHelper()
            hourSnapHelper.attachToRecyclerView(this)

            // 完全按照日选择器的滚动监听逻辑
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        val snappedView = hourSnapHelper.findSnapView(layoutManager)
                        val position = snappedView?.let { layoutManager?.getPosition(it) } ?: 0
                        hourAdapter.setSelectedPosition(position)
                        // 更新当前小时（和日选择器逻辑完全一致）
                        currentHour = position
                        notifyDateTimeChanged()
                    }
                }
            })
        }

        // 分钟选择器 (0-59)
        val minutes = (0..59).toList()
        minuteAdapter = NumberPickerAdapter(minutes) { minute ->
            currentMinute = minute
            notifyDateTimeChanged()
        }
        minutePicker.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = minuteAdapter
            val minuteSnapHelper = LinearSnapHelper()
            minuteSnapHelper.attachToRecyclerView(this)

            // 完全按照日选择器的滚动监听逻辑
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        val snappedView = minuteSnapHelper.findSnapView(layoutManager)
                        val position = snappedView?.let { layoutManager?.getPosition(it) } ?: 0
                        minuteAdapter.setSelectedPosition(position)
                        // 更新当前分钟（和日选择器逻辑完全一致）
                        currentMinute = position
                        notifyDateTimeChanged()
                    }
                }
            })
        }

        // 日期选择器
        dayAdapter = NumberPickerAdapter(emptyList()) { day ->
            currentDay = day
            notifyDateTimeChanged()
        }
        dayPicker.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = dayAdapter
            val daySnapHelper = LinearSnapHelper()
            daySnapHelper.attachToRecyclerView(this)

            // 添加滚动监听，和其他选择器完全一致
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        val snappedView = daySnapHelper.findSnapView(layoutManager)
                        val position = snappedView?.let { layoutManager?.getPosition(it) } ?: 0
                        dayAdapter.setSelectedPosition(position)
                        // 更新当前日期（position+1因为日期从1开始）
                        currentDay = position + 1
                        notifyDateTimeChanged()
                    }
                }
            })
        }

        // 设置初始值
        setCurrentDateTime()
        updateDayPicker()
    }

    private fun updateDayPicker() {
        val days = getDaysInMonth(currentYear, currentMonth)
        dayAdapter.updateItems(days)

        // 如果当前选中的日期超出了新月份的天数，调整到最后一天
        if (currentDay > days.size) {
            currentDay = days.size
        }

        // 滚动到当前选中的日期
        dayPicker.scrollToPosition(currentDay - 1)

        // 设置选中状态，确保居中显示
        post {
            dayAdapter.setSelectedPosition(currentDay - 1)
            // 再次滚动确保完全居中
            dayPicker.smoothScrollToPosition(currentDay - 1)
        }
    }

    private fun getDaysInMonth(year: Int, month: Int): List<Int> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1)
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        return (1..maxDay).toList()
    }

    private fun setCurrentDateTime() {
        isInitializing = true
        try {
            // 滚动到当前时间
            monthPicker.scrollToPosition(currentMonth - 1)
            hourPicker.scrollToPosition(currentHour)
            minutePicker.scrollToPosition(currentMinute)

            // 设置选中状态
            post {
                monthAdapter.setSelectedPosition(currentMonth - 1)
                hourAdapter.setSelectedPosition(currentHour)
                minuteAdapter.setSelectedPosition(currentMinute)

                // 再次滚动确保完全居中
                monthPicker.smoothScrollToPosition(currentMonth - 1)
                hourPicker.smoothScrollToPosition(currentHour)
                minutePicker.smoothScrollToPosition(currentMinute)
            }
        } finally {
            // 延迟重置标志，确保所有设置完成
            postDelayed({
                isInitializing = false
            }, 500)
        }
    }

    private fun notifyDateTimeChanged() {
        if (!isInitializing) {
            onDateTimeChangedListener?.invoke(currentYear, currentMonth, currentDay, currentHour, currentMinute)
        }
    }

    fun getSelectedDateTime(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(currentYear, currentMonth - 1, currentDay, currentHour, currentMinute, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun setDateTime(timeInMillis: Long) {
        isInitializing = true
        try {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = timeInMillis

            currentYear = calendar.get(Calendar.YEAR)
            currentMonth = calendar.get(Calendar.MONTH) + 1
            currentDay = calendar.get(Calendar.DAY_OF_MONTH)
            currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            currentMinute = calendar.get(Calendar.MINUTE)

            // 先滚动到目标位置
            monthPicker.scrollToPosition(currentMonth - 1)
            hourPicker.scrollToPosition(currentHour)
            minutePicker.scrollToPosition(currentMinute)

            // 设置选中状态并确保完全居中
            post {
                monthAdapter.setSelectedPosition(currentMonth - 1)
                hourAdapter.setSelectedPosition(currentHour)
                minuteAdapter.setSelectedPosition(currentMinute)

                // 再次滚动确保完全居中
                monthPicker.smoothScrollToPosition(currentMonth - 1)
                hourPicker.smoothScrollToPosition(currentHour)
                minutePicker.smoothScrollToPosition(currentMinute)
            }

            updateDayPicker()
        } finally {
            // 延迟重置标志，确保所有设置完成
            postDelayed({
                isInitializing = false
            }, 500)
        }
    }

    fun getFormattedDateTime(): String {
        val calendar = Calendar.getInstance()
        calendar.set(currentYear, currentMonth - 1, currentDay, currentHour, currentMinute, 0)
        val format = SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault())
        return format.format(calendar.time)
    }
}