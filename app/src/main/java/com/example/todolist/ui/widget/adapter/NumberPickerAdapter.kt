package com.example.todolist.ui.widget.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.R

class NumberPickerAdapter(
    private var items: List<Int>,
    private val onItemSelected: (Int) -> Unit
) : ListAdapter<Int, NumberPickerAdapter.NumberViewHolder>(NumberDiffCallback()) {

    private var selectedPosition = -1

    init {
        submitList(items)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NumberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_number_picker, parent, false)
        return NumberViewHolder(view)
    }

    override fun onBindViewHolder(holder: NumberViewHolder, position: Int) {
        val number = getItem(position)
        holder.bind(number, position == selectedPosition, onItemSelected) { newPos ->
            val oldPos = selectedPosition
            selectedPosition = newPos
            if (oldPos != -1) {
                notifyItemChanged(oldPos)
            }
            notifyItemChanged(newPos)
        }
    }

    fun updateItems(newItems: List<Int>) {
        items = newItems
        submitList(newItems)
    }

    fun setSelectedPosition(position: Int) {
        val oldPos = selectedPosition
        selectedPosition = position
        if (oldPos != -1) {
            notifyItemChanged(oldPos)
        }
        notifyItemChanged(position)
    }

    class NumberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val numberText: TextView = itemView.findViewById(R.id.tvNumber)

        fun bind(number: Int, isSelected: Boolean, onItemSelected: (Int) -> Unit, onSelectedChange: (Int) -> Unit) {
            numberText.text = String.format("%02d", number)

            // 设置选中状态的样式
            if (isSelected) {
                numberText.setTextColor(itemView.context.getColor(R.color.white))
                numberText.textSize = 36f
                numberText.alpha = 1.0f
                numberText.setTypeface(null, android.graphics.Typeface.BOLD)
                itemView.setBackgroundColor(itemView.context.getColor(R.color.purple_primary))
            } else {
                numberText.setTextColor(itemView.context.getColor(R.color.text_secondary))
                numberText.textSize = 32f
                numberText.alpha = 0.6f
                numberText.setTypeface(null, android.graphics.Typeface.NORMAL)
                itemView.setBackgroundColor(Color.TRANSPARENT)
            }

            itemView.setOnClickListener {
                onSelectedChange(bindingAdapterPosition)
                onItemSelected(number)
            }
        }
    }

    class NumberDiffCallback : DiffUtil.ItemCallback<Int>() {
        override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
            return oldItem == newItem
        }
    }
}