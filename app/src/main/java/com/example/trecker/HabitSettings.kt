package com.example.trecker

import java.util.*

data class HabitSettings(
    val name: String = "",
    val description: String = "",
    val category: String = "Общие",
    val color: String = "#FF6B6B",
    val icon: String = "🎯",
    val priority: Int = 1,
    val time: String = "12:00",
    val repeatSettings: RepeatSettings = RepeatSettings(),
    val notificationSettings: NotificationSettings = NotificationSettings()
)

data class RepeatSettings(
    val repeatType: RepeatType = RepeatType.NEVER,
    val startDate: Date? = null,
    val endDate: Date? = null,
    val interval: Int = 1,
    val intervalUnit: IntervalUnit = IntervalUnit.DAYS,
    val daysOfWeek: List<Int> = emptyList(),
    val daysOfMonth: List<Int> = emptyList(),
    val specialDaysType: SpecialDaysType = SpecialDaysType.NONE
)

data class NotificationSettings(
    val enabled: Boolean = true,
    val reminderType: ReminderType = ReminderType.AT_TIME,
    val advanceMinutes: Int = 0,
    val soundEnabled: Boolean = true
)

enum class IntervalUnit {
    DAYS,
    WEEKS,
    MONTHS,
    YEARS
}

enum class ReminderType {
    AT_TIME,
    MINUTES_5,
    MINUTES_15,
    MINUTES_30,
    HOURS_1,
    HOURS_2,
    DAYS_1,
    CUSTOM
}

enum class SpecialDaysType {
    NONE,
    WEEKDAYS,
    WEEKENDS
}