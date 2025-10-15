package com.example.todolist.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.R
import com.example.todolist.data.database.Task

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
        private val deleteButton: ImageButton = itemView.findViewById(R.id.btnDeleteTask)

        fun bind(task: Task) {
            checkBox.isChecked = task.isCompleted
            titleTextView.text = task.title
            descriptionTextView.text = task.description

            // 设置删除线效果
            if (task.isCompleted) {
                titleTextView.paintFlags = titleTextView.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                descriptionTextView.paintFlags = descriptionTextView.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                titleTextView.alpha = 0.6f
                descriptionTextView.alpha = 0.6f
            } else {
                titleTextView.paintFlags = titleTextView.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                descriptionTextView.paintFlags = descriptionTextView.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                titleTextView.alpha = 1.0f
                descriptionTextView.alpha = 1.0f
            }

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (checkBox.tag == null) {
                    checkBox.tag = true
                    onTaskCheckedChangeListener?.onTaskChanged(task, isChecked)
                    checkBox.tag = null
                }
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

    private var onTaskCheckedChangeListener: OnTaskCheckedChangeListener? = null
    private var onTaskDeleteListener: OnTaskDeleteListener? = null

    fun setOnTaskCheckedChangeListener(listener: OnTaskCheckedChangeListener) {
        this.onTaskCheckedChangeListener = listener
    }

    fun setOnTaskDeleteListener(listener: OnTaskDeleteListener) {
        this.onTaskDeleteListener = listener
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