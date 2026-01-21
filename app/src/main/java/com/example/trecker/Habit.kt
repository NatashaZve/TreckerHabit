package com.example.trecker

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.*

@Parcelize
data class Habit(
    @SerializedName("id")
    val id: Int = 0,
    @SerializedName("name")
    val name: String,
    @SerializedName("date")
    val date: Date,
    @SerializedName("time")
    val time: String, // Формат: "HH:mm"
    @SerializedName("repeatType")
    val repeatType: RepeatType,
    @SerializedName("isCompleted")
    val isCompleted: Boolean = false,
    @SerializedName("repeatDays")
    val repeatDays: String = "", // Формат: "1,3,5" (пн, ср, пт) или "mon,wed,fri"
    @SerializedName("endDate")
    val endDate: Date? = null,
    @SerializedName("completedDates")
    val completedDates: List<String> = emptyList(), // Список дат в формате "yyyy-MM-dd"
    @SerializedName("notificationEnabled")
    val notificationEnabled: Boolean = true,
    @SerializedName("notificationId")
    val notificationId: Int = generateNotificationId(),
    @SerializedName("notificationChannel")
    val notificationChannel: String = NotificationHelper.CHANNEL_REMINDERS_ID,
    @SerializedName("snoozeCount")
    val snoozeCount: Int = 0, // Количество откладываний
    @SerializedName("lastNotificationTime")
    val lastNotificationTime: Long = 0, // Когда последний раз было уведомление (timestamp)
    @SerializedName("createdAt")
    val createdAt: Long = System.currentTimeMillis(), // Дата создания привычки
    @SerializedName("updatedAt")
    val updatedAt: Long = System.currentTimeMillis(), // Дата последнего обновления
    @SerializedName("color")
    val color: String = "#AF8482", // Цвет для отображения (HEX)

    @SerializedName("icon")
    val icon: String = "default", // Иконка привычки

    @SerializedName("priority")
    val priority: Int = 1, // Приоритет (1-5)

    @SerializedName("notes")
    val notes: String = "", // Заметки к привычке

    @SerializedName("streak")
    val streak: Int = 0, // Текущая серия выполнения

    @SerializedName("bestStreak")
    val bestStreak: Int = 0, // Лучшая серия выполнения

    @SerializedName("totalCompletions")
    val totalCompletions: Int = 0, // Общее количество выполнений

    @SerializedName("category")
    val category: String = "Общие", // Категория привычки

    @SerializedName("repeatInterval")
    val repeatInterval: Int = 1,

    @SerializedName("repeatIntervalUnit")
    val repeatIntervalUnit: IntervalUnit = IntervalUnit.DAYS,

    @SerializedName("displayId")
    val displayId: String = "" // Для временных ID при отображении
) : Parcelable {

    companion object {
        private val random = Random()
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        private val displayDateFormat = SimpleDateFormat("d MMMM yyyy", Locale("ru"))

        /**
         * Генерация уникального ID для уведомления
         */
        fun generateNotificationId(): Int {
            return 1000 + random.nextInt(8999) // Диапазон: 1000-9999
        }

        /**
         * Создать тестовую привычку
         */
        fun createTestHabit(): Habit {
            return Habit(
                id = 999,
                name = "Тестовая привычка",
                date = Date(),
                time = "12:00",
                repeatType = RepeatType.DAILY,
                notificationEnabled = true
            )
        }

        /**
         * Проверить валидность времени
         */
        fun isValidTime(time: String): Boolean {
            return try {
                val parts = time.split(":")
                if (parts.size != 2) return false
                val hours = parts[0].toInt()
                val minutes = parts[1].toInt()
                hours in 0..23 && minutes in 0..59
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Форматировать время для отображения
         */
        fun formatTimeForDisplay(time: String): String {
            return if (isValidTime(time)) {
                try {
                    val parts = time.split(":")
                    val hours = parts[0].toInt()
                    val minutes = parts[1].toInt()
                    String.format("%02d:%02d", hours, minutes)
                } catch (e: Exception) {
                    time
                }
            } else {
                "12:00"
            }
        }

        /**
         * Получить текущее время в формате HH:mm
         */
        fun getCurrentTime(): String {
            return timeFormat.format(Date())
        }

        /**
         * Получить текущую дату в формате yyyy-MM-dd
         */
        fun getCurrentDateString(): String {
            return dateFormat.format(Date())
        }

        /**
         * Сравнить два времени
         */
        fun compareTimes(time1: String, time2: String): Int {
            return try {
                val parts1 = time1.split(":").map { it.toInt() }
                val parts2 = time2.split(":").map { it.toInt() }

                when {
                    parts1[0] != parts2[0] -> parts1[0] - parts2[0]
                    else -> parts1[1] - parts2[1]
                }
            } catch (e: Exception) {
                0
            }
        }
    }

    // ВЫЧИСЛЯЕМЫЕ СВОЙСТВА!

    // Дата в строковом формате для отображения

    val displayDate: String
        get() = displayDateFormat.format(date)

    /**
     * Время в удобном формате для отображения
     */
    val displayTime: String
        get() = formatTimeForDisplay(time)

    /**
     * Дата и время вместе для отображения
     */
    val displayDateTime: String
        get() = "$displayDate в $displayTime"

    /**
     * Активна ли привычка сегодня
     */
    val isActiveToday: Boolean
        get() = DateUtils.isSameDay(date, Date())

    /**
     * Дата в формате для базы данных
     */
    val dbDate: String
        get() = dateFormat.format(date)

    /**
     * Дата окончания в формате для базы данных (или пустая строка)
     */
    val dbEndDate: String
        get() = endDate?.let { dateFormat.format(it) } ?: ""

    /**
     * Привычка просрочена (время прошло, но не выполнена)
     */
    val isOverdue: Boolean
        get() {
            if (isCompleted) return false

            return try {
                val now = Calendar.getInstance()
                val habitTime = Calendar.getInstance().apply {
                    time = date

                    // Правильный вызов split
                    val timeParts = this@Habit.time.split(":")
                    if (timeParts.size == 2) {
                        set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                        set(Calendar.MINUTE, timeParts[1].toInt())
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                }

                habitTime.before(now) && DateUtils.isSameDay(date, Date())
            } catch (e: Exception) {
                false
            }
        }
    /**
     * Время выполнения в миллисекундах
     */
    val timeInMillis: Long
        get() = DateUtils.getTimeInMillis(date, time)

    /**
     * Осталось минут до выполнения
     */
    val minutesRemaining: Long
        get() {
            val now = System.currentTimeMillis()
            val habitTime = timeInMillis
            return if (habitTime > now) {
                (habitTime - now) / (1000 * 60)
            } else {
                0
            }
        }

    /**
     * Форматированное оставшееся время
     */
    val formattedTimeRemaining: String
        get() {
            val minutes = minutesRemaining
            return when {
                minutes <= 0 -> "Просрочено"
                minutes < 60 -> "$minutes мин"
                else -> "${minutes / 60} ч ${minutes % 60} мин"
            }
        }

    /**
     * JSON список выполненных дат
     */
    val completedDatesJson: String
        get() = completedDates.joinToString(", ", "[", "]")

    /**
     * Процент выполнения (для прогресс-бара)
     */
    val completionPercentage: Int
        get() {
            val totalDays = calculateTotalDays()
            return if (totalDays > 0) {
                (completedDates.size * 100) / totalDays
            } else {
                0
            }
        }

    /**
     * Канал для уведомления в зависимости от статуса
     */
    val notificationChannelId: String
        get() = when {
            isOverdue -> NotificationHelper.CHANNEL_URGENT_ID
            repeatType == RepeatType.DAILY -> NotificationHelper.CHANNEL_REMINDERS_ID
            else -> notificationChannel
        }

    /**
     * Цвет для отображения статуса
     */
    val statusColor: String
        get() = when {
            isCompleted -> "#4CAF50" // Зеленый
            isOverdue -> "#F44336"   // Красный
            else -> "#2196F3"        // Синий
        }

    /**
     * Иконка для отображения статуса
     */
    val statusIcon: String
        get() = when {
            isCompleted -> "✅"
            isOverdue -> "🚨"
            else -> "⏰"
        }

    // ========== МЕТОДЫ ДЛЯ РАБОТЫ С ПРИВЫЧКОЙ ==========

    /**
     * Пометить как выполненную
     */
    fun markAsCompleted(completionDate: Date = Date()): Habit {
        val dateStr = dateFormat.format(completionDate)
        val newCompletedDates = if (completedDates.contains(dateStr)) {
            completedDates
        } else {
            completedDates + dateStr
        }

        val newStreak = streak + 1
        val newBestStreak = maxOf(bestStreak, newStreak)

        return this.copy(
            isCompleted = true,
            completedDates = newCompletedDates,
            streak = newStreak,
            bestStreak = newBestStreak,
            totalCompletions = totalCompletions + 1,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Пометить как невыполненную
     */
    fun markAsUncompleted(date: Date = Date()): Habit {
        val dateStr = dateFormat.format(date)
        val newCompletedDates = completedDates.filter { it != dateStr }
        val newStreak = maxOf(0, streak - 1)

        return this.copy(
            isCompleted = false,
            completedDates = newCompletedDates,
            streak = newStreak,
            totalCompletions = maxOf(0, totalCompletions - 1),
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Включить/отключить уведомления
     */
    fun withNotificationEnabled(enabled: Boolean): Habit {
        return this.copy(
            notificationEnabled = enabled,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Изменить время выполнения
     */
    fun withTime(newTime: String): Habit {
        return if (isValidTime(newTime)) {
            this.copy(
                time = newTime,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            this
        }
    }

    /**
     * Изменить название
     */
    fun withName(newName: String): Habit {
        return this.copy(
            name = newName,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Изменить тип повторения
     */
    fun withRepeatType(newRepeatType: RepeatType): Habit {
        return this.copy(
            repeatType = newRepeatType,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Изменить дату
     */
    fun withDate(newDate: Date): Habit {
        return this.copy(
            date = newDate,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Изменить цвет
     */
    fun withColor(newColor: String): Habit {
        return this.copy(
            color = newColor,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Изменить приоритет
     */
    fun withPriority(newPriority: Int): Habit {
        return this.copy(
            priority = newPriority.coerceIn(1, 5),
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Добавить заметку
     */
    fun withNotes(newNotes: String): Habit {
        return this.copy(
            notes = newNotes,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Отложить привычку (увеличить счетчик откладываний)
     */
    fun withSnooze(): Habit {
        return this.copy(
            snoozeCount = snoozeCount + 1,
            lastNotificationTime = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Обновить время последнего уведомления
     */
    fun withLastNotificationTime(timestamp: Long): Habit {
        return this.copy(
            lastNotificationTime = timestamp,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Обновить канал уведомлений
     */
    fun withNotificationChannel(channelId: String): Habit {
        return this.copy(
            notificationChannel = channelId,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Сбросить счетчик откладываний
     */
    fun resetSnoozeCount(): Habit {
        return this.copy(
            snoozeCount = 0,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Сбросить серию выполнения
     */
    fun resetStreak(): Habit {
        return this.copy(
            streak = 0,
            updatedAt = System.currentTimeMillis()
        )
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    /**
     * Рассчитать общее количество дней (с даты начала до конца или сегодня)
     */
    private fun calculateTotalDays(): Int {
        val start = date
        val end = endDate ?: Date()

        val diff = end.time - start.time
        return (diff / (1000 * 60 * 60 * 24)).toInt() + 1
    }

    /**
     * Получить текст для уведомления
     */
    fun getNotificationText(): String {
        return when {
            isOverdue -> "🚨 Просрочено: $name"
            isCompleted -> "✅ Выполнено: $name"
            else -> "⏰ Время для: $name"
        }
    }

    /**
     * Получить подробный текст для уведомления
     */
    fun getDetailedNotificationText(): String {
        return """
            $name
            Время: $displayTime
            ${if (isOverdue) "🚨 ПРОСРОЧЕНО" else ""}
            ${if (streak > 0) "Серия: $streak дней" else ""}
        """.trimIndent()
    }

    /**
     * Проверить, активна ли привычка на указанную дату
     */
    fun isActiveOnDate(targetDate: Date): Boolean {
        return when (repeatType) {
            RepeatType.ONCE -> DateUtils.isSameDay(date, targetDate)
            RepeatType.DAILY -> true // Активна каждый день
            RepeatType.WEEKLY -> {
                // Активна в тот же день недели
                val calendar = Calendar.getInstance()
                calendar.time = date
                val habitDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

                calendar.time = targetDate
                val targetDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

                habitDayOfWeek == targetDayOfWeek
            }
            RepeatType.WEEKDAYS -> {
                // Активна с понедельника по пятницу
                val calendar = Calendar.getInstance()
                calendar.time = targetDate
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

                dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY
            }
            RepeatType.WEEKENDS -> {
                // Активна в субботу и воскресенье
                val calendar = Calendar.getInstance()
                calendar.time = targetDate
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

                dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
            }
            RepeatType.CUSTOM_DAYS -> {
                // Проверка по пользовательским дням
                if (repeatDays.isEmpty()) return false

                val calendar = Calendar.getInstance()
                calendar.time = targetDate
                val targetDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

                val daysList = repeatDays.split(",").map { it.trim().toIntOrNull() }
                daysList.contains(targetDayOfWeek)
            }
            else -> DateUtils.isSameDay(date, targetDate) // По умолчанию для остальных типов
        }
    }

    /**
     * Получить следующую дату выполнения для повторяющихся привычек
     */
    fun getNextOccurrence(): Date? {
        if (repeatType == RepeatType.ONCE) return null

        val calendar = Calendar.getInstance()
        calendar.time = date

        return when (repeatType) {
            RepeatType.DAILY -> {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                calendar.time
            }
            RepeatType.WEEKLY -> {
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
                calendar.time
            }
            RepeatType.MONTHLY -> {
                calendar.add(Calendar.MONTH, 1)
                calendar.time
            }
            else -> null
        }
    }

    /**
     * Создать копию для следующего дня
     */
    fun copyForNextDay(): Habit {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.DAY_OF_MONTH, 1)

        return this.copy(
            date = calendar.time,
            isCompleted = false,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Проверить, является ли привычка валидной
     */
    fun isValid(): Boolean {
        return name.isNotBlank() &&
                isValidTime(time) &&
                repeatType != null
    }

    /**
     * Получить строковое представление для отладки
     */
    override fun toString(): String {
        return """
            Habit [
                id: $id,
                name: "$name",
                date: $displayDate,
                time: $time,
                repeatType: $repeatType,
                isCompleted: $isCompleted,
                notificationEnabled: $notificationEnabled,
                isOverdue: $isOverdue,
                streak: $streak
            ]
        """.trimIndent()
    }

    /**
     * Получить краткую информацию
     */
    fun toShortString(): String {
        return "$name ($displayTime)${if (isCompleted) " ✅" else ""}"
    }

    /**
     * Получить данные в формате для сохранения
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "date" to date.time,
            "time" to time,
            "repeatType" to repeatType.name,
            "isCompleted" to isCompleted,
            "repeatDays" to repeatDays,
            "endDate" to (endDate?.time ?: 0L),
            "completedDates" to completedDates,
            "notificationEnabled" to notificationEnabled,
            "notificationId" to notificationId,
            "notificationChannel" to notificationChannel,
            "snoozeCount" to snoozeCount,
            "lastNotificationTime" to lastNotificationTime,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "color" to color,
            "priority" to priority,
            "notes" to notes,
            "streak" to streak,
            "bestStreak" to bestStreak,
            "totalCompletions" to totalCompletions,
            "category" to category
        )
    }
}