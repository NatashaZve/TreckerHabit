package com.example.trecker

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.threeten.bp.LocalDate

@Parcelize
data class Habit(
    val id: Int,
    val name: String,
    val date: LocalDate,
    val isCompleted: Boolean = false
) : Parcelable