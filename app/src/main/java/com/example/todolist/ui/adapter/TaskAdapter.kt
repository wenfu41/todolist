package com.example.todolist.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.R
import com.example.todolist.data.database.Task
import java.text.SimpleDateFormat
import java.util.*

class TaskAdapter : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkBox: CheckBox = itemView.findViewById(R.id.cbTaskComplete)
        private val titleTextView: TextView = itemView.findViewById(R.id.tvTaskTitle)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.tvTaskDescription)
        private val editButton: ImageButton = itemView.findViewById(R.id.btnEditTask)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.btnDeleteTask)
        private val alarmIcon: ImageView = itemView.findViewById(R.id.ivAlarmIcon)
        private val alarmTimeText: TextView = itemView.findViewById(R.id.tvAlarmTime)

        private val timeFormat = SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault())

        fun bind(task: Task) {
            checkBox.isChecked = task.isCompleted
            titleTextView.text = task.title
            descriptionTextView.text = task.description

            // 显示闹钟信息
            if (task.hasAlarm && task.alarmTime != null && !task.isCompleted) {
                alarmIcon.visibility = View.VISIBLE
                alarmTimeText.visibility = View.VISIBLE
                val alarmDate = Date(task.alarmTime)
                val formattedTime = timeFormat.format(alarmDate)
                alarmTimeText.text = formattedTime
            } else {
                alarmIcon.visibility = View.GONE
                alarmTimeText.visibility = View.GONE
            }

            // 设置删除线效果
            if (task.isCompleted) {
                titleTextView.paintFlags = titleTextView.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                descriptionTextView.paintFlags = descriptionTextView.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                titleTextView.alpha = 0.6f
                descriptionTextView.alpha = 0.6f
                alarmIcon.alpha = 0.6f
                alarmTimeText.alpha = 0.6f
            } else {
                titleTextView.paintFlags = titleTextView.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                descriptionTextView.paintFlags = descriptionTextView.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                titleTextView.alpha = 1.0f
                descriptionTextView.alpha = 1.0f
                alarmIcon.alpha = 1.0f
                alarmTimeText.alpha = 1.0f
            }

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (checkBox.tag == null) {
                    checkBox.tag = true
                    onTaskCheckedChangeListener?.onTaskChanged(task, isChecked)
                    checkBox.tag = null
                }
            }

            editButton.setOnClickListener {
                onTaskEditListener?.onTaskEdit(task)
            }

            deleteButton.setOnClickListener {
                onTaskDeleteListener?.onTaskDelete(task)
            }
        }
    }

    interface OnTaskCheckedChangeListener {
        fun onTaskChanged(task: Task, isCompleted: Boolean)
    }

    interface OnTaskDeleteListener {
        fun onTaskDelete(task: Task)
    }

    interface OnTaskEditListener {
        fun onTaskEdit(task: Task)
    }

    private var onTaskCheckedChangeListener: OnTaskCheckedChangeListener? = null
    private var onTaskDeleteListener: OnTaskDeleteListener? = null
    private var onTaskEditListener: OnTaskEditListener? = null

    fun setOnTaskCheckedChangeListener(listener: OnTaskCheckedChangeListener) {
        this.onTaskCheckedChangeListener = listener
    }

    fun setOnTaskDeleteListener(listener: OnTaskDeleteListener) {
        this.onTaskDeleteListener = listener
    }

    fun setOnTaskEditListener(listener: OnTaskEditListener) {
        this.onTaskEditListener = listener
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem
        }
    }
}