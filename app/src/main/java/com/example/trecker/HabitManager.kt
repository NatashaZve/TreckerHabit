package com.example.trecker

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.threeten.bp.LocalDate

class HabitManager(private val context: Context) {
    private val sharedPreferences: SharedPreferences
    private val gson = Gson()
    private val habitsKey = "habits"
    private var nextId = 1

    init {
        sharedPreferences = context.getSharedPreferences("HabitPrefs", Context.MODE_PRIVATE)
        nextId = sharedPreferences.getInt("nextId", 1)
    }

    fun addHabit(name: String, date: LocalDate): Habit {
        val habit = Habit(nextId++, name, date)
        val habits = getAllHabits().toMutableList()
        habits.add(habit)
        saveHabits(habits)
        saveNextId()
        return habit
    }

    fun getHabitsForDate(date: LocalDate): List<Habit> {
        return getAllHabits().filter { it.date == date }
    }

    fun getTodayHabits(): List<Habit> {
        return getHabitsForDate(LocalDate.now())
    }

    fun completeHabit(habitId: Int) {
        val habits = getAllHabits().toMutableList()
        val index = habits.indexOfFirst { it.id == habitId }
        if (index != -1) {
            val habit = habits[index]
            habits[index] = habit.copy(isCompleted = true)
            saveHabits(habits)
        }
    }

    fun deleteHabit(habitId: Int) {
        val habits = getAllHabits().toMutableList()
        habits.removeIf { it.id == habitId }
        saveHabits(habits)
    }

    fun getAllHabits(): List<Habit> {
        val json = sharedPreferences.getString(habitsKey, "[]") ?: "[]"
        return try {
            val type = object : TypeToken<List<Habit>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveHabits(habits: List<Habit>) {
        val json = gson.toJson(habits)
        sharedPreferences.edit().putString(habitsKey, json).apply()
    }

    private fun saveNextId() {
        sharedPreferences.edit().putInt("nextId", nextId).apply()
    }
}