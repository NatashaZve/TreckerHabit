package com.example.trecker

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    // Единый формат для всей базы данных
    private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun getCurrentDate(): Date {
        return Calendar.getInstance().time
    }

    fun getCurrentTime(): String {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return formatter.format(Date())
    }

    fun formatDate(date: Date, pattern: String = "d MMMM yyyy 'г.'"): String {
        val formatter = SimpleDateFormat(pattern, Locale("ru"))
        return formatter.format(date)
    }

    fun formatDateTime(date: Date, time: String): String {
        val dateStr = formatDate(date, "d MMMM yyyy")
        return "$dateStr в $time"
    }

    fun getDayOfMonth(date: Date): Int {
        val calendar = Calendar.getInstance()
        calendar.time = date
        return calendar.get(Calendar.DAY_OF_MONTH)
    }

    fun addDays(date: Date, days: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.DAY_OF_MONTH, days)
        return calendar.time
    }

    fun isSameDay(date1: Date, date2: Date): Boolean {
        // Используем единый формат для сравнения
        return dbDateFormat.format(date1) == dbDateFormat.format(date2)
    }

    fun getMonthYear(date: Date): String {
        return formatDate(date, "MMMM yyyy")
    }

    fun getDayOfWeek(date: Date): String {
        return formatDate(date, "EEEE")
    }

    // Проверка валидности времени
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

    // Сравнение времени
    fun compareTimes(time1: String, time2: String): Int {
        val parts1 = time1.split(":").map { it.toInt() }
        val parts2 = time2.split(":").map { it.toInt() }

        return when {
            parts1[0] != parts2[0] -> parts1[0] - parts2[0]
            else -> parts1[1] - parts2[1]
        }
    }

    // Новый метод для получения даты в формате БД
    fun toDbFormat(date: Date): String {
        return dbDateFormat.format(date)
    }

    // Парсинг из формата БД
    fun fromDbFormat(dateStr: String): Date {
        return dbDateFormat.parse(dateStr) ?: Date()
    }
}