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
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<HabitAdapter.HabitViewHolder>() {

    class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val habitName: TextView = itemView.findViewById(R.id.habitName)
        val habitCheckbox: CheckBox = itemView.findViewById(R.id.habitCheckbox)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_habit, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = habits[position]

        holder.habitName.text = habit.name
        holder.habitCheckbox.isChecked = habit.isCompleted

        holder.habitCheckbox.setOnClickListener {
            if (!habit.isCompleted) {
                onCompleteClick(habit.id)
            }
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