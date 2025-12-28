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
    private val notificationSettingsKey = "notification_settings"
    private var nextId = 1
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Менеджер уведомлений
    private val notificationManager = HabitNotificationManager(context)

    // Настройки уведомлений
    private val notificationSettings: SharedPreferences

    init {
        sharedPreferences = context.getSharedPreferences("HabitPrefs", Context.MODE_PRIVATE)
        notificationSettings = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE)
        nextId = sharedPreferences.getInt("nextId", 1)

        // Инициализация настроек уведомлений
        initNotificationSettings()
    }

    // ==================== НАСТРОЙКИ УВЕДОМЛЕНИЙ ====================

    /**
     * Инициализация настроек уведомлений
     */
    private fun initNotificationSettings() {
        if (!notificationSettings.contains("notifications_enabled")) {
            notificationSettings.edit().putBoolean("notifications_enabled", true).apply()
        }
        if (!notificationSettings.contains("default_notification_time")) {
            notificationSettings.edit().putString("default_notification_time", "09:00").apply()
        }
    }

    /**
     * Включить/отключить все уведомления
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        notificationSettings.edit().putBoolean("notifications_enabled", enabled).apply()

        if (enabled) {
            // Включаем уведомления для всех привычек
            val habits = getAllHabits()
            habits.forEach { habit ->
                if (habit.notificationEnabled) {
                    notificationManager.scheduleNotification(habit)
                }
            }
            Log.d("HabitManager", "Все уведомления включены")
        } else {
            // Отключаем все уведомления
            notificationManager.cancelAllNotifications()
            Log.d("HabitManager", "Все уведомления отключены")
        }
    }

    /**
     * Проверить, включены ли уведомления
     */
    fun areNotificationsEnabled(): Boolean {
        return notificationSettings.getBoolean("notifications_enabled", true)
    }

    // ==================== ОСНОВНЫЕ МЕТОДЫ ДЛЯ ПРИВЫЧЕК ====================

    /**
     * Основной метод добавления привычки
     */
    fun addHabit(
        name: String,
        date: Date,
        time: String = "12:00",
        repeatType: RepeatType = RepeatType.ONCE,
        repeatDays: String = "",
        endDate: Date? = null,
        notificationEnabled: Boolean = true
    ): Habit {
        val habit = Habit(
            id = nextId++,
            name = name,
            date = date,
            time = time,
            repeatType = repeatType,
            repeatDays = repeatDays,
            endDate = endDate,
            notificationEnabled = notificationEnabled
        )

        val habits = getAllHabits().toMutableList()
        habits.add(habit)
        saveHabits(habits)
        saveNextId()

        Log.d("HabitManager", "Добавлена привычка: '$name' на ${dateFormat.format(date)} в $time")

        // Запланировать уведомление если включено
        if (notificationEnabled && areNotificationsEnabled()) {
            notificationManager.scheduleNotification(habit)
            Log.d("HabitManager", "Уведомление запланировано для '$name'")
        }

        return habit
    }

    /**
     * Получить все привычки
     */
    fun getAllHabits(): List<Habit> {
        val json = sharedPreferences.getString(habitsKey, "[]") ?: "[]"
        return try {
            val type = object : TypeToken<List<Habit>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e("HabitManager", "Ошибка чтения привычек: ${e.message}")
            emptyList()
        }
    }

    /**
     * Получить привычки на конкретную дату
     */
    fun getHabitsForDate(targetDate: Date): List<Habit> {
        val allHabits = getAllHabits()
        val result = mutableListOf<Habit>()
        val dateStr = dateFormat.format(targetDate)

        for (habit in allHabits) {
            val isActive = isHabitActiveOnDate(habit, targetDate)

            if (isActive) {
                val isCompletedOnDate = habit.completedDates.contains(dateStr)
                val habitForDate = habit.copy(
                    date = targetDate,
                    isCompleted = isCompletedOnDate
                )
                result.add(habitForDate)
            }
        }

        return result.sortedBy { it.time }
    }

    /**
     * Проверить, активна ли привычка на дату
     */
    internal fun isHabitActiveOnDate(habit: Habit, targetDate: Date): Boolean {
        val habitDateStr = dateFormat.format(habit.date)
        val targetDateStr = dateFormat.format(targetDate)

        return when (habit.repeatType) {
            RepeatType.ONCE -> habitDateStr == targetDateStr
            else -> habitDateStr == targetDateStr
        }
    }

    // ==================== УПРАВЛЕНИЕ УВЕДОМЛЕНИЯМИ ====================

    /**
     * Включить/отключить уведомления для привычки
     */
    fun toggleNotification(habitId: Int, enabled: Boolean): Boolean {
        val habits = getAllHabits().toMutableList()
        val index = habits.indexOfFirst { it.id == habitId }

        if (index != -1) {
            val habit = habits[index]

            // Обновляем привычку
            habits[index] = habit.copy(notificationEnabled = enabled)
            saveHabits(habits)

            // Управляем уведомлением
            if (enabled && areNotificationsEnabled()) {
                notificationManager.scheduleNotification(habits[index])
                Log.d("HabitManager", "Уведомления включены для привычки $habitId")
            } else {
                notificationManager.cancelNotification(habitId)
                Log.d("HabitManager", "Уведомления отключены для привычки $habitId")
            }

            return true
        }

        Log.w("HabitManager", "Привычка $habitId не найдена для переключения уведомлений")
        return false
    }

    /**
     * Перепланировать все уведомления
     */
    fun rescheduleAllNotifications() {
        if (!areNotificationsEnabled()) {
            Log.d("HabitManager", "Уведомления отключены, пропускаем перепланирование")
            return
        }

        val habits = getAllHabits()
            .filter { it.notificationEnabled }

        if (habits.isNotEmpty()) {
            notificationManager.rescheduleAllNotifications(habits)
            Log.d("HabitManager", "Перепланировано ${habits.size} уведомлений")
        } else {
            Log.d("HabitManager", "Нет привычек с включенными уведомлениями")
        }
    }

    // ==================== МЕТОДЫ ДЛЯ ВЫПОЛНЕНИЯ ПРИВЫЧЕК ====================

    /**
     * Отметить привычку как выполненную
     */
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

            habits[index] = habit.copy(
                completedDates = newCompletedDates,
                isCompleted = true
            )
            saveHabits(habits)

            // Отменить уведомление для выполненной привычки
            if (habit.notificationEnabled) {
                notificationManager.cancelNotification(habitId)
                Log.d("HabitManager", "Уведомление отменено для выполненной привычки $habitId")
            }

            Log.d("HabitManager", "Привычка $habitId отмечена выполненной на $dateStr")
        }
    }

    // ==================== ОБНОВЛЕНИЕ ПРИВЫЧЕК ====================

    /**
     * Обновить время выполнения привычки
     */
    fun updateHabitTime(habitId: Int, newTime: String): Boolean {
        val habits = getAllHabits().toMutableList()
        val index = habits.indexOfFirst { it.id == habitId }

        if (index != -1 && DateUtils.isValidTime(newTime)) {
            val habit = habits[index]
            habits[index] = habit.copy(time = newTime)
            saveHabits(habits)

            // Перепланировать уведомление с новым временем
            if (habit.notificationEnabled && areNotificationsEnabled()) {
                notificationManager.cancelNotification(habitId)
                notificationManager.scheduleNotification(habits[index])
            }

            return true
        }

        return false
    }

    /**
     * Обновить название привычки
     */
    fun updateHabitName(habitId: Int, newName: String): Boolean {
        val habits = getAllHabits().toMutableList()
        val index = habits.indexOfFirst { it.id == habitId }

        if (index != -1 && newName.isNotBlank()) {
            val habit = habits[index]
            habits[index] = habit.copy(name = newName)
            saveHabits(habits)
            return true
        }

        return false
    }

    // ==================== УДАЛЕНИЕ ПРИВЫЧЕК ====================

    /**
     * Удалить привычку
     */
    fun deleteHabit(habitId: Int) {
        val habits = getAllHabits().toMutableList()
        val habitToDelete = habits.find { it.id == habitId }

        habits.removeIf { it.id == habitId }
        saveHabits(habits)

        // Отменить уведомление
        habitToDelete?.let {
            if (it.notificationEnabled) {
                notificationManager.cancelNotification(habitId)
            }
        }
    }

    // ==================== ПОИСК И ФИЛЬТРАЦИЯ ====================

    /**
     * Найти привычку по ID
     */
    fun findHabitById(habitId: Int): Habit? {
        return getAllHabits().find { it.id == habitId }
    }

    /**
     * Получить привычки с уведомлениями
     */
    fun getHabitsWithNotifications(): List<Habit> {
        return getAllHabits()
            .filter { it.notificationEnabled }
    }

    /**
     * Получить просроченные привычки
     */
    fun getOverdueHabits(): List<Habit> {
        return getAllHabits()
            .filter { it.isOverdue && !it.isCompleted }
    }

    /**
     * Получить привычки на сегодня
     */
    fun getTodayHabits(): List<Habit> {
        return getHabitsForDate(Date())
    }

    // ==================== СТАТИСТИКА ====================

    /**
     * Получить статистику по привычкам
     */
    fun getHabitStats(): Map<String, Any> {
        val habits = getAllHabits()
        val total = habits.size
        val completed = habits.count { it.isCompleted }
        val withNotifications = habits.count { it.notificationEnabled }

        val overdue = habits.count { it.isOverdue && !it.isCompleted }

        return mapOf(
            "total" to total,
            "completed" to completed,
            "with_notifications" to withNotifications,
            "overdue" to overdue,
            "completion_rate" to if (total > 0) (completed * 100 / total) else 0
        )
    }

    /**
     * Получить статистику по уведомлениям
     */
    fun getNotificationStats(): Map<String, Any> {
        val habits = getAllHabits()
        val withNotifications = habits.count { it.notificationEnabled }
        val withoutNotifications = habits.count { !it.notificationEnabled }

        val overdue = habits.count { it.isOverdue && it.notificationEnabled }

        return mapOf(
            "total" to habits.size,
            "with_notifications" to withNotifications,
            "without_notifications" to withoutNotifications,
            "overdue" to overdue,
            "notification_percentage" to if (habits.isNotEmpty()) {
                (withNotifications * 100 / habits.size)
            } else 0
        )
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    fun saveHabits(habits: List<Habit>) {
        try {
            val json = gson.toJson(habits)
            sharedPreferences.edit().putString(habitsKey, json).apply()
        } catch (e: Exception) {
            Log.e("HabitManager", "Ошибка сохранения привычек: ${e.message}", e)
        }
    }

    fun saveNextId() {
        sharedPreferences.edit().putInt("nextId", nextId).apply()
    }

    /**
     * Получить количество выполненных привычек сегодня
     */
    fun getTodayCompletedCount(): Int {
        return getTodayHabits().count { it.isCompleted }
    }

    /**
     * Получить общее количество привычек сегодня
     */
    fun getTodayTotalCount(): Int {
        return getTodayHabits().size
    }

    /**
     * Получить прогресс выполнения на сегодня
     */
    fun getTodayProgress(): Float {
        val total = getTodayTotalCount()
        val completed = getTodayCompletedCount()

        return if (total > 0) completed.toFloat() / total else 0f
    }
}