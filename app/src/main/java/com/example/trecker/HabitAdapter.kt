package com.example.trecker

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class HabitAdapter(
    private var habits: List<Habit>,
    private val onCompleteClick: (Int) -> Unit,
    private val onTimeClick: (Int) -> Unit,  // Обработчик клика по времени
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<HabitAdapter.HabitViewHolder>() {

    class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val habitName: TextView = itemView.findViewById(R.id.habitName)
        val habitTime: TextView = itemView.findViewById(R.id.habitTime)  // Теперь это кнопка!
        val habitCheckbox: CheckBox = itemView.findViewById(R.id.habitCheckbox)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)

        // УДАЛИТЬ: val timeButton: Button = itemView.findViewById(R.id.timeButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_habit_with_time, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = habits[position]
        val context = holder.itemView.context

        holder.habitName.text = habit.name

        // 1. Устанавливаем текст времени
        holder.habitTime.text = habit.time

        // 2. Настраиваем кликабельность и внешний вид
        holder.habitTime.setOnClickListener {
            onTimeClick(habit.id)
        }

        // 3. Делаем время похожим на кнопку
        holder.habitTime.isClickable = true
        holder.habitTime.isFocusable = true

        // 4. Изменяем внешний вид в зависимости от статуса
        updateTimeAppearance(holder, habit)

        holder.habitCheckbox.isChecked = habit.isCompleted

        // 5. Обработка нажатия на чекбокс
        holder.habitCheckbox.setOnClickListener {
            if (!habit.isCompleted) {
                onCompleteClick(habit.id)
            }
        }

        // 6. Обработка нажатия на кнопку удаления
        holder.deleteButton.setOnClickListener {
            onDeleteClick(habit.id)
        }

        // 7. Дополнительно: можно сделать кликабельным всё view для деталей
        holder.itemView.setOnClickListener {
            // Можно добавить показ деталей привычки
        }
    }

    /**
     * Обновить внешний вид времени в зависимости от статуса привычки
     */
    private fun updateTimeAppearance(holder: HabitViewHolder, habit: Habit) {
        val context = holder.itemView.context

        when {
            habit.isCompleted -> {
                // Для выполненной привычки
                holder.habitTime.setTextColor(ContextCompat.getColor(context, R.color.green))
                holder.habitTime.setBackgroundResource(R.drawable.time_button_completed)
                holder.habitTime.text = habit.time
            }

            habit.isOverdue -> {
                // Для просроченной привычки
                holder.habitTime.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                holder.habitTime.setBackgroundResource(R.drawable.time_button_overdue)
                holder.habitTime.text = habit.time
            }

            else -> {
                // Для активной привычки
                holder.habitTime.setTextColor(ContextCompat.getColor(context, R.color.black))
                holder.habitTime.setBackgroundResource(R.drawable.time_button_normal)
                holder.habitTime.text = habit.time
            }
        }

        // Добавляем подсказку (для долгого нажатия)
        holder.habitTime.setOnLongClickListener {
            // Показываем подсказку
            android.widget.Toast.makeText(
                context,
                "Нажмите, чтобы изменить время выполнения",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            true
        }
    }

    override fun getItemCount() = habits.size

    fun updateHabits(newHabits: List<Habit>) {
        habits = newHabits
        notifyDataSetChanged()
    }
}