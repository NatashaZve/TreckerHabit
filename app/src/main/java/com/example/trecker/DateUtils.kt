package com.example.trecker

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object DateUtils {

    // ==================== ФОРМАТТЕРЫ ====================

    // Единый формат для всей базы данных
    private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val displayDateFormatter = SimpleDateFormat("d MMMM yyyy", Locale("ru"))

    // ==================== ОСНОВНЫЕ МЕТОДЫ ====================

    fun getCurrentDate(): Date = Calendar.getInstance().time

    fun getCurrentTime(): String = timeFormat.format(Date())

    fun formatDate(date: Date, pattern: String = "d MMMM yyyy"): String {
        return try {
            val formatter = SimpleDateFormat(pattern, Locale("ru"))
            formatter.format(date)
        } catch (e: Exception) {
            displayDateFormatter.format(date)
        }
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

    fun addMinutes(date: Date, minutes: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.MINUTE, minutes)
        return calendar.time
    }

    fun isSameDay(date1: Date, date2: Date): Boolean {
        return dbDateFormat.format(date1) == dbDateFormat.format(date2)
    }

    fun getMonthYear(date: Date): String {
        return formatDate(date, "MMMM yyyy")
    }

    fun getDayOfWeek(date: Date): String {
        return formatDate(date, "EEEE")
    }

    // ==================== МЕТОДЫ ДЛЯ УВЕДОМЛЕНИЙ ====================

    fun getTimeInMillis(date: Date, timeString: String): Long {
        return try {
            val calendar = Calendar.getInstance()
            calendar.time = date

            val parts = timeString.split(":")
            if (parts.size == 2) {
                calendar.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                calendar.set(Calendar.MINUTE, parts[1].toInt())
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }

            calendar.timeInMillis
        } catch (e: Exception) {
            date.time
        }
    }

    fun getTimeRemainingInMinutes(date: Date, timeString: String): Long {
        val targetTime = getTimeInMillis(date, timeString)
        val currentTime = System.currentTimeMillis()
        val millis = targetTime - currentTime
        return TimeUnit.MILLISECONDS.toMinutes(millis)
    }

    fun isTimePassed(date: Date, timeString: String): Boolean {
        return getTimeRemainingInMinutes(date, timeString) <= 0
    }

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

    fun toDbFormat(date: Date): String {
        return dbDateFormat.format(date)
    }

    fun fromDbFormat(dateStr: String): Date {
        return try {
            dbDateFormat.parse(dateStr) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    fun getFormattedTimeRemaining(date: Date, timeString: String): String {
        val minutes = getTimeRemainingInMinutes(date, timeString)

        return when {
            minutes <= 0 -> "Просрочено"
            minutes < 60 -> "$minutes мин"
            else -> {
                val hours = minutes / 60
                val remainingMinutes = minutes % 60
                if (remainingMinutes > 0) {
                    "$hours ч $remainingMinutes мин"
                } else {
                    "$hours ч"
                }
            }
        }
    }

    fun isToday(date: Date): Boolean {
        return isSameDay(date, Date())
    }

    fun isYesterday(date: Date): Boolean {
        return isSameDay(date, addDays(Date(), -1))
    }

    fun isTomorrow(date: Date): Boolean {
        return isSameDay(date, addDays(Date(), 1))
    }

    fun combineDateAndTime(date: Date, timeString: String): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date

        return try {
            val parts = timeString.split(":")
            if (parts.size == 2) {
                calendar.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                calendar.set(Calendar.MINUTE, parts[1].toInt())
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
            calendar.time
        } catch (e: Exception) {
            date
        }
    }

    fun getTimeFromDate(date: Date): String {
        val calendar = Calendar.getInstance().apply { time = date }
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        return String.format("%02d:%02d", hour, minute)
    }

    fun parseTimeString(timeStr: String): Date? {
        return try {
            val parts = timeStr.split(":")
            if (parts.size == 2) {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                calendar.set(Calendar.MINUTE, parts[1].toInt())
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.time
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}