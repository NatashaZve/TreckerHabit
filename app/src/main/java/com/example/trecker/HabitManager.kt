package com.example.trecker

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class HabitManager(private val context: Context) {
    private val sharedPreferences: SharedPreferences
    private val gson = Gson()
    private val habitsKey = "habits"
    private var nextId = 1
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    init {
        sharedPreferences = context.getSharedPreferences("HabitPrefs", Context.MODE_PRIVATE)
        nextId = sharedPreferences.getInt("nextId", 1)
    }

    // Основной метод добавления привычки с поддержкой повторений
    fun addHabit(
        name: String,
        date: Date,
        time: String = "00:00",
        repeatType: RepeatType = RepeatType.ONCE,
        repeatDays: String = "",
        endDate: Date? = null
    ): Habit {
        val habit = Habit(
            id = nextId++,
            name = name,
            date = date,
            time = time,
            isCompleted = false,
            repeatType = repeatType,
            repeatDays = repeatDays,
            endDate = endDate,
            completedDates = emptyList()
        )

        val habits = getAllHabits().toMutableList()
        habits.add(habit)
        saveHabits(habits)
        saveNextId()

        Log.d("HabitManager", "Добавлена привычка: '$name' на ${dateFormat.format(date)}")
        return habit
    }

    // Метод, который запрашивается в MainActivity
    fun getAllHabits(): List<Habit> {
        val json = sharedPreferences.getString(habitsKey, "[]") ?: "[]"
        return try {
            val type = object : TypeToken<List<Habit>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // В методе getHabitsForDate добавьте отладку:
    fun getHabitsForDate(targetDate: Date): List<Habit> {
        val allHabits = getAllHabits()
        val result = mutableListOf<Habit>()
        val dateStr = dateFormat.format(targetDate)

        Log.d("HABIT_DEBUG", "=== ПОИСК ПРИВЫЧЕК НА ДАТУ: $dateStr ===")
        Log.d("HABIT_DEBUG", "Всего привычек в базе: ${allHabits.size}")

        // Выводим все привычки для отладки
        allHabits.forEachIndexed { index, habit ->
            Log.d("HABIT_DEBUG",
                "Привычка $index: '${habit.name}' " +
                        "дата: ${dateFormat.format(habit.date)} " +
                        "тип: ${habit.repeatType} " +
                        "repeatDays: '${habit.repeatDays}'"
            )
        }

        for (habit in allHabits) {
            val isActive = isHabitActiveOnDate(habit, targetDate)
            Log.d("HABIT_DEBUG",
                "Проверка '${habit.name}': активна на $dateStr? $isActive"
            )

            if (isActive) {
                val isCompletedOnDate = habit.completedDates.contains(dateStr)
                val habitForDate = habit.copy(
                    date = targetDate,
                    isCompleted = isCompletedOnDate
                )
                result.add(habitForDate)
                Log.d("HABIT_DEBUG", "  -> ДОБАВЛЕНО")
            }
        }

        Log.d("HABIT_DEBUG", "Итого найдено: ${result.size} привычек")
        return result.sortedBy { it.time }
    }

    internal fun isHabitActiveOnDate(habit: Habit, targetDate: Date): Boolean {
        val habitDateStr = dateFormat.format(habit.date)
        val targetDateStr = dateFormat.format(targetDate)

        Log.d("HABIT_CHECK",
            "Проверка '${habit.name}':\n" +
                    "  Дата привычки: $habitDateStr\n" +
                    "  Целевая дата: $targetDateStr\n" +
                    "  Тип: ${habit.repeatType}"
        )

        // Для ONCE просто сравниваем даты
        return when (habit.repeatType) {
            RepeatType.ONCE -> habitDateStr == targetDateStr
            else -> {
                // Для упрощения - показываем только ONCE привычки
                // После того как это заработает, добавим другие типы
                habitDateStr == targetDateStr
            }
        }
    }

    // Отметить привычку как выполненную на конкретную дату
    fun completeHabit(habitId: Int, completionDate: Date = Date()) {
        val habits = getAllHabits().toMutableList()
        val index = habits.indexOfFirst { it.id == habitId }

        if (index != -1) {
            val habit = habits[index]
            val dateStr = dateFormat.format(completionDate)

            val newCompletedDates = if (habit.completedDates.contains(dateStr)) {
                habit.completedDates
            } else {
                habit.completedDates + dateStr
            }

            habits[index] = habit.copy(completedDates = newCompletedDates)
            saveHabits(habits)
        }
    }

    // Обновить время выполнения привычки
    fun updateHabitTime(habitId: Int, newTime: String) {
        val habits = getAllHabits().toMutableList()
        val index = habits.indexOfFirst { it.id == habitId }

        if (index != -1 && DateUtils.isValidTime(newTime)) {
            val habit = habits[index]
            habits[index] = habit.copy(time = newTime)
            saveHabits(habits)
        }
    }

    // Удалить привычку
    fun deleteHabit(habitId: Int) {
        val habits = getAllHabits().toMutableList()
        habits.removeIf { it.id == habitId }
        saveHabits(habits)
    }

    // Получить привычки на сегодня
    fun getTodayHabits(): List<Habit> {
        return getHabitsForDate(Date())
    }

    // Получить ближайшие предстоящие привычки
    fun getUpcomingHabits(daysCount: Int = 7): List<Habit> {
        val result = mutableListOf<Habit>()
        val calendar = Calendar.getInstance()
        val today = Date()

        for (i in 0 until daysCount) {
            calendar.time = today
            calendar.add(Calendar.DAY_OF_MONTH, i)
            val date = calendar.time

            val habitsForDate = getHabitsForDate(date)
                .filter { !it.isCompleted }
                .map { it.copy(date = date) }

            result.addAll(habitsForDate)
        }

        return result.distinctBy { it.id to dateFormat.format(it.date) }
            .sortedWith(compareBy({ it.date }, { it.time }))
    }

    // Вспомогательные методы
    private fun saveHabits(habits: List<Habit>) {
        val json = gson.toJson(habits)
        sharedPreferences.edit().putString(habitsKey, json).apply()
    }

    private fun saveNextId() {
        sharedPreferences.edit().putInt("nextId", nextId).apply()
    }

    // Поиск привычки по ID
    fun findHabitById(habitId: Int): Habit? {
        return getAllHabits().find { it.id == habitId }
    }

    // Получить все уникальные привычки (без учета дат)
    fun getAllUniqueHabits(): List<Habit> {
        return getAllHabits().distinctBy { it.name }
    }

    // Получить привычки по типу повторения
    fun getHabitsByRepeatType(repeatType: RepeatType): List<Habit> {
        return getAllHabits().filter { it.repeatType == repeatType }
    }

    // Обновить информацию о привычке
    fun updateHabit(updatedHabit: Habit): Boolean {
        val habits = getAllHabits().toMutableList()
        val index = habits.indexOfFirst { it.id == updatedHabit.id }

        if (index != -1) {
            habits[index] = updatedHabit
            saveHabits(habits)
            return true
        }
        return false
    }

    // Очистить все привычки (для тестирования)
    fun clearAllHabits() {
        sharedPreferences.edit().remove(habitsKey).apply()
        saveNextId()
    }
}