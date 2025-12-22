package com.example.trecker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HabitAdapter(
    private var habits: List<Habit>,
    private val onCompleteClick: (Int) -> Unit,
    private val onTimeClick: (Int) -> Unit,  // Новый обработчик для времени
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<HabitAdapter.HabitViewHolder>() {

    class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val habitName: TextView = itemView.findViewById(R.id.habitName)
        val habitTime: TextView = itemView.findViewById(R.id.habitTime)  // Новый TextView для времени
        val habitCheckbox: CheckBox = itemView.findViewById(R.id.habitCheckbox)
        val timeButton: Button = itemView.findViewById(R.id.timeButton)  // Кнопка изменения времени
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_habit_with_time, parent, false)  // Новый layout
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = habits[position]

        holder.habitName.text = habit.name
        holder.habitTime.text = habit.time
        holder.habitCheckbox.isChecked = habit.isCompleted

        // Подсветка если время прошло, но привычка не выполнена
        if (!habit.isCompleted) {
            val currentTime = DateUtils.getCurrentTime()
            if (DateUtils.compareTimes(habit.time, currentTime) < 0) {
                holder.habitTime.setTextColor(holder.itemView.context.getColor(android.R.color.holo_red_dark))
            } else {
                holder.habitTime.setTextColor(holder.itemView.context.getColor(android.R.color.black))
            }
        }

        holder.habitCheckbox.setOnClickListener {
            if (!habit.isCompleted) {
                onCompleteClick(habit.id)
            }
        }

        holder.timeButton.setOnClickListener {
            onTimeClick(habit.id)
        }

        holder.deleteButton.setOnClickListener {
            onDeleteClick(habit.id)
        }
    }

    override fun getItemCount() = habits.size

    fun updateHabits(newHabits: List<Habit>) {
        habits = newHabits
        notifyDataSetChanged()
    }
}