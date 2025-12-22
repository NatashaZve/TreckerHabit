package com.example.trecker

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trecker.databinding.ActivityMainBinding
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var habitManager: HabitManager
    private lateinit var habitAdapter: HabitAdapter
    private var currentDate: Date = Date()
    private val dateFormatter = SimpleDateFormat("d MMMM yyyy", Locale("ru"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            enableEdgeToEdge()

            // 1. Инициализируем ViewBinding
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // 2. Инициализируем HabitManager
            habitManager = HabitManager(this)

            // 3. Настраиваем UI
            setupDateDisplay()
            setupDateNavigation()
            setupHabitsRecyclerView()
            setupSystemBars()
            setupMultiDateButton()

            Log.d("MainActivity", "Приложение успешно создано")

        } catch (e: Exception) {
            Log.e("MainActivity", "Ошибка при создании: ${e.message}", e)
            Toast.makeText(this, "Ошибка запуска: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupDateDisplay() {
        try {
            val dayOfMonth = DateUtils.getDayOfMonth(currentDate)
            val monthYear = DateUtils.getMonthYear(currentDate)
            val dayOfWeek = DateUtils.getDayOfWeek(currentDate)

            binding.dayOfMonthTextView.text = dayOfMonth.toString()
            binding.monthYearTextView.text = monthYear
            binding.dayOfWeekTextView.text = dayOfWeek.capitalize(Locale("ru"))

            val today = Date()
            val isToday = DateUtils.isSameDay(currentDate, today)

            loadHabitsForDate()

        } catch (e: Exception) {
            Log.e("MainActivity", "Ошибка в setupDateDisplay: ${e.message}")
            binding.dayOfMonthTextView.text = "?"
            binding.monthYearTextView.text = "Ошибка даты"
        }
    }

    private fun setupDateNavigation() {
        binding.prevDayButton.setOnClickListener {
            currentDate = DateUtils.addDays(currentDate, -1)
            setupDateDisplay()
        }

        binding.nextDayButton.setOnClickListener {
            currentDate = DateUtils.addDays(currentDate, 1)
            setupDateDisplay()
        }

        binding.todayButton.setOnClickListener {
            currentDate = Date()
            setupDateDisplay()
            Toast.makeText(this, "Перешли на сегодня", Toast.LENGTH_SHORT).show()
        }

        binding.calendarButton.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        try {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Выберите дату")
                .setSelection(currentDate.time)
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = selection
                currentDate = calendar.time

                setupDateDisplay()
                Toast.makeText(this, "Дата изменена", Toast.LENGTH_SHORT).show()
            }

            datePicker.show(supportFragmentManager, "MAIN_DATE_PICKER")

        } catch (e: Exception) {
            Log.e("MainActivity", "Ошибка в DatePicker: ${e.message}")
            Toast.makeText(this, "Ошибка открытия календаря", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showTimePickerForHabit(habitId: Int) {
        try {
            val habit = habitManager.getAllHabits().find { it.id == habitId }
            var initialHour = 12
            var initialMinute = 0

            if (habit != null && DateUtils.isValidTime(habit.time)) {
                val parts = habit.time.split(":")
                initialHour = parts[0].toInt()
                initialMinute = parts[1].toInt()
            } else {
                val calendar = Calendar.getInstance()
                initialHour = calendar.get(Calendar.HOUR_OF_DAY)
                initialMinute = calendar.get(Calendar.MINUTE)
            }

            val timePicker = TimePickerDialog(
                this,
                TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                    val newTime = String.format("%02d:%02d", hourOfDay, minute)
                    habitManager.updateHabitTime(habitId, newTime)
                    loadHabitsForDate()
                    Toast.makeText(this, "Время изменено на $newTime", Toast.LENGTH_SHORT).show()
                },
                initialHour,
                initialMinute,
                true
            )

            timePicker.setTitle("Изменить время привычки")
            timePicker.show()

        } catch (e: Exception) {
            Log.e("MainActivity", "Ошибка в showTimePickerForHabit: ${e.message}")
            Toast.makeText(this, "Ошибка изменения времени", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupHabitsRecyclerView() {
        try {
            habitAdapter = HabitAdapter(emptyList(),
                onCompleteClick = { habitId ->
                    habitManager.completeHabit(habitId)
                    loadHabitsForDate()
                    Toast.makeText(this, "Привычка выполнена!", Toast.LENGTH_SHORT).show()
                },
                onTimeClick = { habitId ->
                    showTimePickerForHabit(habitId)
                },
                onDeleteClick = { habitId ->
                    showDeleteConfirmationDialog(habitId)
                }
            )

            binding.habitsRecyclerView.apply {
                layoutManager = LinearLayoutManager(this@MainActivity)
                adapter = habitAdapter
                setHasFixedSize(true)
            }

            loadHabitsForDate()
        } catch (e: Exception) {
            Log.e("MainActivity", "Ошибка в setupHabitsRecyclerView: ${e.message}")
            binding.habitsRecyclerView.visibility = android.view.View.GONE
        }
    }

    private fun showDeleteConfirmationDialog(habitId: Int) {
        AlertDialog.Builder(this)
            .setTitle("Удаление привычки")
            .setMessage("Вы уверены, что хотите удалить эту привычку?")
            .setPositiveButton("Удалить") { dialog, _ ->
                habitManager.deleteHabit(habitId)
                loadHabitsForDate()
                Toast.makeText(this, "Привычка удалена!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun loadHabitsForDate() {
        try {
            val habits = habitManager.getHabitsForDate(currentDate)

            Log.d("MainActivity", "loadHabitsForDate: найдено ${habits.size} привычек на дату ${dateFormatter.format(currentDate)}")

            habitAdapter.updateHabits(habits)

            if (habits.isNotEmpty()) {
                binding.todayHabitsTitle.visibility = android.view.View.VISIBLE
                binding.habitsRecyclerView.visibility = android.view.View.VISIBLE
                binding.todayHabitsTitle.text = "Привычки на ${DateUtils.formatDate(currentDate, "d MMMM")}:"
                binding.emptyStateText.visibility = android.view.View.GONE
            } else {
                binding.todayHabitsTitle.visibility = android.view.View.GONE
                binding.habitsRecyclerView.visibility = android.view.View.GONE
                binding.emptyStateText.visibility = android.view.View.VISIBLE
                binding.emptyStateText.text = "На ${DateUtils.formatDate(currentDate, "d MMMM")} нет привычек"
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Ошибка в loadHabitsForDate: ${e.message}")
            binding.habitsRecyclerView.visibility = android.view.View.GONE
        }
    }

    private fun setupSystemBars() {
        try {
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Ошибка в setupSystemBars: ${e.message}")
        }
    }

    private fun setupMultiDateButton() {
        binding.multiDateButton.setOnClickListener {
            Log.d("MainActivity", "Кнопка 'Добавить привычку' нажата")

            try {
                val intent = Intent(this, CalendarActivity::class.java)
                intent.putExtra("current_date", currentDate.time)
                startActivity(intent)

                Toast.makeText(this, "Открываю добавление привычки...", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Log.e("MainActivity", "Ошибка: ${e.message}", e)
                Toast.makeText(this, "Ошибка открытия: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupDateDisplay()
    }
}