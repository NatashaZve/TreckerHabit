package com.example.trecker

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

class StatisticsManager(private val context: Context) {

    private val habitManager = HabitManager(context)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val weekFormat = SimpleDateFormat("yyyy-'W'w", Locale.getDefault())

    data class Statistics(
        val totalHabits: Int,
        val completedToday: Int,
        val totalToday: Int,
        val completionRate: Float, // 0-100%
        val streakDays: Int,
        val bestStreak: Int,
        val weeklyStats: List<WeeklyStat>  // ОСТАВЛЯЕМ недельную статистику
    )

    data class WeeklyStat(
        val week: String, // "Неделя 1"
        val completed: Int,
        val total: Int,
        val rate: Float
    )

    /**
     * Получить общую статистику
     */
    fun getOverallStatistics(): Statistics {
        val habits = habitManager.getAllHabits()
        val todayHabits = habitManager.getTodayHabits()

        return Statistics(
            totalHabits = habits.size,
            completedToday = todayHabits.count { it.isCompleted },
            totalToday = todayHabits.size,
            completionRate = if (todayHabits.isNotEmpty()) {
                (todayHabits.count { it.isCompleted }.toFloat() / todayHabits.size) * 100
            } else 0f,
            streakDays = calculateCurrentStreak(),
            bestStreak = calculateBestStreak(),
            weeklyStats = getWeeklyStats()  // ОСТАВЛЯЕМ
        )
    }

    /**
     * Рассчитать текущую серию (сколько дней подряд выполняются привычки)
     */
    private fun calculateCurrentStreak(): Int {
        val calendar = Calendar.getInstance()
        var streak = 0
        var currentDay = Date()

        while (true) {
            val dayHabits = habitManager.getHabitsForDate(currentDay)
            if (dayHabits.isEmpty()) break

            val completed = dayHabits.count { it.isCompleted }
            val total = dayHabits.size

            if (completed == total && completed > 0) {
                streak++
            } else {
                break
            }

            calendar.time = currentDay
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            currentDay = calendar.time
        }

        return streak
    }

    /**
     * Рассчитать лучшую серию
     */
    private fun calculateBestStreak(): Int {
        val habits = habitManager.getAllHabits()
        return habits.maxOfOrNull { it.bestStreak } ?: 0
    }

    /**
     * Статистика по неделям
     */
    private fun getWeeklyStats(): List<WeeklyStat> {
        val habits = habitManager.getAllHabits()
        val stats = mutableMapOf<String, Pair<Int, Int>>()

        habits.forEach { habit ->
            habit.completedDates.forEach { dateStr ->
                try {
                    val date = dateFormat.parse(dateStr)
                    val weekKey = weekFormat.format(date)
                    val current = stats[weekKey] ?: Pair(0, 0)
                    stats[weekKey] = Pair(current.first + 1, current.second + 1)
                } catch (e: Exception) {
                    // Игнорируем некорректные даты
                }
            }
        }

        return stats.map { (weekKey, pair) ->
            val weekNumber = weekKey.substringAfter("W").toIntOrNull() ?: 0
            WeeklyStat(
                week = "Неделя $weekNumber",
                completed = pair.first,
                total = pair.second,
                rate = if (pair.second > 0) (pair.first.toFloat() / pair.second) * 100 else 0f
            )
        }.sortedByDescending { it.week }
    }

    /**
     * Получить прогресс за день
     */
    fun getDailyProgress(date: Date = Date()): Pair<Int, Int> {
        val habits = habitManager.getHabitsForDate(date)
        val completed = habits.count { it.isCompleted }
        return Pair(completed, habits.size)
    }

    /**
     * Получить прогресс за неделю
     */
    fun getWeeklyProgress(): Pair<Int, Int> {
        val calendar = Calendar.getInstance()
        var totalCompleted = 0
        var totalHabits = 0

        // Считаем за последние 7 дней
        for (i in 0..6) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_MONTH, -i)
            val date = calendar.time
            val (completed, total) = getDailyProgress(date)
            totalCompleted += completed
            totalHabits += total
        }

        return Pair(totalCompleted, totalHabits)
    }
}