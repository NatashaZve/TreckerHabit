package com.example.trecker

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.trecker.databinding.ActivityCalendarBinding
import com.google.android.material.datepicker.MaterialDatePicker
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import java.util.*

class CalendarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarBinding
    private lateinit var habitManager: HabitManager
    private var selectedDate: LocalDate = LocalDate.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        habitManager = HabitManager(this)

        updateDateDisplay()
        setupDatePicker()
        setupAddHabitButton()
        setupBackButton()
    }

    private fun updateDateDisplay() {
        val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))
        binding.selectedDateText.text = "Выбрана дата: ${selectedDate.format(formatter)}"
    }

    private fun setupDatePicker() {
        binding.selectDateButton.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Выберите дату")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                selectedDate = Instant.ofEpochMilli(selection)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                updateDateDisplay()
                Toast.makeText(this, "Дата изменена", Toast.LENGTH_SHORT).show()
            }

            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }
    }

    private fun setupAddHabitButton() {
        binding.addHabitButton.setOnClickListener {
            showAddHabitDialog()
        }
    }

    private fun showAddHabitDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_habit, null)
        val habitInput = dialogView.findViewById<android.widget.EditText>(R.id.habitInput)

        AlertDialog.Builder(this)
            .setTitle("Добавить привычку")
            .setView(dialogView)
            .setPositiveButton("Добавить") { dialog, _ ->
                val habitName = habitInput.text.toString().trim()
                if (habitName.isNotEmpty()) {
                    val habit = habitManager.addHabit(habitName, selectedDate)

                    Toast.makeText(
                        this,
                        "Привычка '${habit.name}' добавлена на ${selectedDate.dayOfMonth}.${selectedDate.monthValue}.${selectedDate.year}",
                        Toast.LENGTH_LONG
                    ).show()

                    updateDateDisplay()
                } else {
                    Toast.makeText(this, "Введите название привычки", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            finish()
        }
    }
}