package com.example.trecker

import java.util.Date

data class Habit(
    val id: Int,
    val name: String,
    val date: Date,
    val time: String,
    val repeatType: RepeatType,  // Используем существующий RepeatType
    val isCompleted: Boolean = false,
    val repeatDays: String = "",
    val endDate: Date? = null,
    val completedDates: List<String> = emptyList()
)