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

    data class MonthlyStat(
        val month: String, // "Январь 2024"
        val completed: Int,
        val total: Int,
        val rate: Float,
        val daysWithHabits: Int, // Дней с привычками в месяце
        val bestDay: Pair<String, Int>? // Лучший день (дата, выполнено)
    )

    fun getMonthlyStats(monthsCount: Int = 6): List<MonthlyStat> {
        val calendar = Calendar.getInstance()
        val stats = mutableListOf<MonthlyStat>()

        // Начинаем с месяцев назад и идем к текущему
        calendar.time = Date()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.add(Calendar.MONTH, -(monthsCount - 1)) // Начинаем с самого старого

        for (i in 0 until monthsCount) {
            val currentMonth = calendar.time
            val stat = getStatForMonth(currentMonth)
            stats.add(stat)

            // Переходим к следующему месяцу
            calendar.add(Calendar.MONTH, 1)
        }

        // Теперь stats содержат месяцы в порядке от старого к новому
        // Пример: [Июл 2025, Авг 2025, Сен 2025, ..., Янв 2026]
        return stats
    }

    /**
     * Получить статистику за конкретный месяц
     */
    private fun getStatForMonth(dateInMonth: Date): MonthlyStat {
        val calendar = Calendar.getInstance()
        calendar.time = dateInMonth

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)

        val monthDate = calendar.time

        // Устанавливаем на начало месяца
        calendar.set(year, month, 1, 0, 0, 0)
        val startOfMonth = calendar.timeInMillis

        // Переходим на конец месяца
        calendar.set(year, month, calendar.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
        val endOfMonth = calendar.timeInMillis

        val allHabits = habitManager.getAllHabits()
        var totalCompleted = 0
        var totalPossible = 0
        var daysWithHabits = 0
        var bestDay: Pair<String, Int>? = null

        // Форматирование для месяца
        val monthName = SimpleDateFormat("MMMM yyyy", Locale("ru")).format(dateInMonth)

        // Перебираем все дни месяца
        val dayCalendar = Calendar.getInstance()
        dayCalendar.set(year, month, 1)

        while (dayCalendar.get(Calendar.MONTH) == month) {
            val currentDate = dayCalendar.time
            val dateMillis = currentDate.time

            // Считаем привычки на этот день
            val dayHabits = allHabits.filter { habit ->
                val habitCalendar = Calendar.getInstance()
                habitCalendar.time = habit.date

                // Проверяем, активна ли привычка в этот день
                when (habit.repeatType) {
                    RepeatType.ONCE -> DateUtils.isSameDay(habit.date, currentDate)
                    RepeatType.DAILY -> true
                    RepeatType.WEEKLY -> habitCalendar.get(Calendar.DAY_OF_WEEK) == dayCalendar.get(Calendar.DAY_OF_WEEK)
                    RepeatType.WEEKDAYS -> {
                        val dayOfWeek = dayCalendar.get(Calendar.DAY_OF_WEEK)
                        dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY
                    }
                    RepeatType.WEEKENDS -> {
                        val dayOfWeek = dayCalendar.get(Calendar.DAY_OF_WEEK)
                        dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
                    }
                    else -> DateUtils.isSameDay(habit.date, currentDate)
                }
            }

            if (dayHabits.isNotEmpty()) {
                daysWithHabits++
                totalPossible += dayHabits.size

                // Считаем выполненные привычки
                val completed = dayHabits.count { habit ->
                    val dateStr = DateUtils.toDbFormat(currentDate)
                    habit.completedDates.contains(dateStr)
                }
                totalCompleted += completed

                // Обновляем лучший день
                if (completed > 0 && (bestDay == null || completed > bestDay.second)) {
                    val dayName = SimpleDateFormat("d MMMM", Locale("ru")).format(currentDate)
                    bestDay = Pair(dayName, completed)
                }
            }

            // Следующий день
            dayCalendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        val completionRate = if (totalPossible > 0) {
            (totalCompleted.toFloat() / totalPossible) * 100
        } else 0f

        return MonthlyStat(
            month = monthName,
            completed = totalCompleted,
            total = totalPossible,
            rate = completionRate,
            daysWithHabits = daysWithHabits,
            bestDay = bestDay
        )
    }

    /**
     * Получить прогресс за текущий месяц
     */
    fun getCurrentMonthProgress(): Pair<Int, Int> {
        val calendar = Calendar.getInstance()
        val monthStat = getStatForMonth(calendar.time)
        return Pair(monthStat.completed, monthStat.total)
    }

    /**
     * Получить самый продуктивный месяц
     */
    fun getMostProductiveMonth(): MonthlyStat? {
        val monthlyStats = getMonthlyStats(12) // За последний год
        return monthlyStats.maxByOrNull { it.rate }
    }
}