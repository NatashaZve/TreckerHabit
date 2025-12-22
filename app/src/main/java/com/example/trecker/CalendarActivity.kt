package com.example.trecker

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.trecker.databinding.ActivityCalendarBinding
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.*

class CalendarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarBinding
    private lateinit var habitManager: HabitManager
    private var selectedDate: Date = Date()
    private var selectedTime: String = "12:00"
    private val selectedDates = mutableListOf<Date>()
    private val displayDateFormat = SimpleDateFormat("d MMMM yyyy", Locale("ru"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("CalendarActivity", "=== СОЗДАНИЕ АКТИВНОСТИ ===")

        try {
            binding = ActivityCalendarBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d("CalendarActivity", "Layout загружен")

            habitManager = HabitManager(this)
            Log.d("CalendarActivity", "HabitManager создан")

            // Получаем дату из Intent
            val intentDate = intent.getLongExtra("current_date", -1)
            if (intentDate != -1L) {
                selectedDate = Date(intentDate)
                Log.d("CalendarActivity", "Дата из Intent: ${displayDateFormat.format(selectedDate)}")
            } else {
                Log.d("CalendarActivity", "Использую текущую дату")
            }

            // Инициализируем UI
            initUI()

            Log.d("CalendarActivity", "=== АКТИВНОСТЬ СОЗДАНА УСПЕШНО ===")

        } catch (e: Exception) {
            Log.e("CalendarActivity", "КРИТИЧЕСКАЯ ОШИБКА в onCreate: ${e.message}", e)
            showErrorAndExit("Ошибка загрузки интерфейса")
        }
    }

    private fun initUI() {
        try {
            // Обновляем отображение даты
            updateDateDisplay()

            // Настраиваем кнопки
            setupButtons()

            // Обновляем список дат
            updateDatesDisplay()

            Log.d("CalendarActivity", "UI инициализирован")

        } catch (e: Exception) {
            Log.e("CalendarActivity", "Ошибка в initUI: ${e.message}")
            throw e
        }
    }

    private fun updateDateDisplay() {
        try {
            binding.selectedDateText.text = "Дата: ${displayDateFormat.format(selectedDate)} в $selectedTime"
            Log.d("CalendarActivity", "Дата обновлена: ${binding.selectedDateText.text}")
        } catch (e: Exception) {
            binding.selectedDateText.text = "Дата: сегодня в $selectedTime"
        }
    }

    private fun setupButtons() {
        Log.d("CalendarActivity", "Настройка кнопок...")

        // Кнопка назад
        binding.backButton.setOnClickListener {
            Log.d("CalendarActivity", "Нажата кнопка Назад")
            finish()
        }

        // Кнопка выбора даты
        binding.selectDateButton.setOnClickListener {
            Log.d("CalendarActivity", "Нажата кнопка Выбрать дату")
            showMaterialDatePicker()
        }

        // Кнопка выбора времени
        binding.selectTimeButton.setOnClickListener {
            Log.d("CalendarActivity", "Нажата кнопка Выбрать время")
            showSimpleTimePicker()
        }

        // Кнопка добавить привычку
        binding.addHabitButton.setOnClickListener {
            Log.d("CalendarActivity", "=== НАЖАТА КНОПКА 'ДОБАВИТЬ ПРИВЫЧКУ' ===")
            showAddHabitDialog()
        }

        // Кнопка добавить еще дату
        binding.addAnotherDateButton.setOnClickListener {
            Log.d("CalendarActivity", "Нажата кнопка 'Добавить еще дату'")
            showMaterialDatePickerForAdding()
        }

        // Кнопка очистки дат
        binding.clearDatesButton.setOnClickListener {
            Log.d("CalendarActivity", "Нажата кнопка Очистить даты")
            clearAllDates()
        }

        Log.d("CalendarActivity", "Все кнопки настроены")
    }

    private fun updateDatesDisplay() {
        Log.d("CalendarActivity", "Обновление списка дат. Количество: ${selectedDates.size}")

        if (selectedDates.isEmpty()) {
            // Нет выбранных дат - скрываем элементы
            binding.datesCountText.visibility = View.GONE
            binding.clearDatesButton.visibility = View.GONE
            binding.selectedDatesScrollView.visibility = View.GONE
        } else {
            // Есть выбранные даты - показываем
            binding.datesCountText.visibility = View.VISIBLE
            binding.clearDatesButton.visibility = View.VISIBLE
            binding.selectedDatesScrollView.visibility = View.VISIBLE

            // Обновляем счетчик
            binding.datesCountText.text = "Выбрано дат: ${selectedDates.size}"

            // Очищаем контейнер
            binding.selectedDatesContainer.removeAllViews()

            // Добавляем каждую дату
            selectedDates.forEachIndexed { index, date ->
                val dateView = createDateView(date, index)
                binding.selectedDatesContainer.addView(dateView)
            }
        }
    }

    private fun createDateView(date: Date, index: Int): TextView {
        return TextView(this).apply {
            text = "${index + 1}. ${displayDateFormat.format(date)}"
            textSize = 16f
            setPadding(16, 12, 16, 12)
            background = resources.getDrawable(android.R.drawable.edit_text, null)

            // Добавляем возможность удалить по долгому нажатию
            setOnLongClickListener {
                showDeleteDateDialog(index)
                true
            }

            // Короткое нажатие - показываем детали
            setOnClickListener {
                Toast.makeText(
                    this@CalendarActivity,
                    "Дата: ${displayDateFormat.format(date)}\nНажмите и удерживайте для удаления",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showDeleteDateDialog(index: Int) {
        if (index in 0 until selectedDates.size) {
            val date = selectedDates[index]
            AlertDialog.Builder(this)
                .setTitle("Удалить дату?")
                .setMessage("Удалить ${displayDateFormat.format(date)}?")
                .setPositiveButton("Удалить") { dialog, _ ->
                    selectedDates.removeAt(index)
                    updateDatesDisplay()
                    Toast.makeText(this, "Дата удалена", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .setNegativeButton("Отмена") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun showMaterialDatePicker() {
        try {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Выберите дату")
                .setSelection(selectedDate.time)
                .setTheme(R.style.MyMaterialDatePickerTheme)
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = selection
                selectedDate = calendar.time
                updateDateDisplay()
                Toast.makeText(this, "Дата изменена", Toast.LENGTH_SHORT).show()
                Log.d("CalendarActivity", "Новая дата: ${displayDateFormat.format(selectedDate)}")
            }

            datePicker.addOnNegativeButtonClickListener {
                Log.d("CalendarActivity", "Выбор даты отменен")
            }

            datePicker.show(supportFragmentManager, "MAIN_DATE_PICKER")
            Log.d("CalendarActivity", "MaterialDatePicker показан")

        } catch (e: Exception) {
            Log.e("CalendarActivity", "Ошибка в MaterialDatePicker: ${e.message}", e)
            Toast.makeText(this, "Ошибка открытия календаря", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSimpleTimePicker() {
        try {
            // Парсим текущее время
            val parts = selectedTime.split(":")
            val currentHour = parts[0].toInt()
            val currentMinute = parts[1].toInt()

            val timePicker = TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    selectedTime = String.format("%02d:%02d", hourOfDay, minute)
                    updateDateDisplay()
                    Toast.makeText(this, "Время изменено на $selectedTime", Toast.LENGTH_SHORT).show()
                    Log.d("CalendarActivity", "Новое время: $selectedTime")
                },
                currentHour,
                currentMinute,
                true
            )

            timePicker.setTitle("Выберите время")
            timePicker.show()

            // Изменяем цвета кнопок TimePicker
            timePicker.setOnShowListener {
                val positiveButton = timePicker.getButton(TimePickerDialog.BUTTON_POSITIVE)
                val negativeButton = timePicker.getButton(TimePickerDialog.BUTTON_NEGATIVE)

                positiveButton?.setTextColor(Color.parseColor("#AF8482"))
                negativeButton?.setTextColor(Color.parseColor("#AF8482"))
            }

        } catch (e: Exception) {
            Log.e("CalendarActivity", "Ошибка в showSimpleTimePicker: ${e.message}", e)
            Toast.makeText(this, "Ошибка выбора времени", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showMaterialDatePickerForAdding() {
        try {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Добавить еще одну дату")
                .setSelection(selectedDate.time)
                .setTheme(R.style.MyMaterialDatePickerTheme)
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = selection
                val newDate = calendar.time

                // Простая проверка по timestamp
                val isAlreadyAdded = selectedDates.any { it.time == newDate.time }

                if (!isAlreadyAdded) {
                    selectedDates.add(newDate)
                    updateDatesDisplay()
                    Toast.makeText(this,
                        "Дата добавлена: ${displayDateFormat.format(newDate)}",
                        Toast.LENGTH_SHORT).show()
                    Log.d("CalendarActivity", "Дата добавлена, всего: ${selectedDates.size}")
                } else {
                    Toast.makeText(this, "Эта дата уже выбрана", Toast.LENGTH_SHORT).show()
                }
            }

            datePicker.addOnNegativeButtonClickListener {
                Log.d("CalendarActivity", "Добавление даты отменено")
            }

            datePicker.show(supportFragmentManager, "ADD_DATE_PICKER")
            Log.d("CalendarActivity", "MaterialDatePicker для добавления показан")

        } catch (e: Exception) {
            Log.e("CalendarActivity", "Ошибка в showMaterialDatePickerForAdding: ${e.message}", e)
            Toast.makeText(this, "Ошибка добавления даты: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearAllDates() {
        if (selectedDates.isNotEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Очистка дат")
                .setMessage("Удалить все выбранные даты? (${selectedDates.size} шт.)")
                .setPositiveButton("Удалить") { dialog, _ ->
                    selectedDates.clear()
                    updateDatesDisplay()
                    Toast.makeText(this, "Все даты удалены", Toast.LENGTH_SHORT).show()
                    Log.d("CalendarActivity", "Все даты очищены")
                    dialog.dismiss()
                }
                .setNegativeButton("Отмена") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        } else {
            Toast.makeText(this, "Нет дат для очистки", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAddHabitDialog() {
        try {
            if (selectedDates.isEmpty()) {
                // Добавляем текущую дату, если нет выбранных
                selectedDates.add(selectedDate)
                updateDatesDisplay()
                Log.d("CalendarActivity", "Добавлена текущая дата по умолчанию")
            }

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Добавить привычку")

            // Создаем layout для диалога
            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(50, 20, 50, 20)
            }

            // Поле для названия
            val nameLabel = TextView(this).apply {
                text = "Название привычки:"
                textSize = 16f
                setPadding(0, 0, 0, 10)
            }

            val nameInput = EditText(this).apply {
                hint = "Например: Выпить воды"
                setPadding(20, 15, 20, 15)
            }

            layout.addView(nameLabel)
            layout.addView(nameInput)

            // Показываем список выбранных дат
            if (selectedDates.isNotEmpty()) {
                val datesLabel = TextView(this).apply {
                    text = "Выбранные даты:"
                    textSize = 16f
                    setPadding(0, 20, 0, 10)
                }

                val datesText = TextView(this).apply {
                    text = selectedDates.joinToString("\n") { "• ${displayDateFormat.format(it)}" }
                    textSize = 14f
                    setPadding(20, 10, 20, 20)
                    maxLines = 5
                }

                layout.addView(datesLabel)
                layout.addView(datesText)
            }

            builder.setView(layout)

            builder.setPositiveButton("Добавить") { dialog, _ ->
                val habitName = nameInput.text.toString().trim()
                if (habitName.isNotEmpty()) {
                    addHabitToSelectedDates(habitName)
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Введите название привычки", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }

            builder.setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }

            builder.show()

        } catch (e: Exception) {
            Log.e("CalendarActivity", "Ошибка в диалоге: ${e.message}", e)
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addHabitToSelectedDates(habitName: String) {
        var successCount = 0

        selectedDates.forEach { date ->
            try {
                // Используем SimpleDateFormat для гарантированного правильного формата
                val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                Log.d("CALENDAR_ADD",
                    "Добавление привычки:\n" +
                            "  Название: $habitName\n" +
                            "  Дата (yyyy-MM-dd): ${dbDateFormat.format(date)}\n" +
                            "  Дата (пользовательская): ${displayDateFormat.format(date)}\n" +
                            "  Время: $selectedTime"
                )

                // Добавляем привычку как ONCE
                habitManager.addHabit(
                    name = habitName,
                    date = date,
                    time = selectedTime,
                    repeatType = RepeatType.ONCE
                )

                successCount++

                // Немедленная проверка
                val allHabits = habitManager.getAllHabits()

                Log.d("CALENDAR_ADD",
                    "Результат:\n" +
                            "  Всего привычек в базе: ${allHabits.size}"
                )

            } catch (e: Exception) {
                Log.e("CALENDAR_ADD", "Ошибка: ${e.message}", e)
            }
        }

        val message = if (successCount == 0) {
            "Ошибка добавления привычек"
        } else {
            "Добавлено $successCount привычек"
        }

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        // Возвращаемся на главный экран с небольшой задержкой
        Handler(Looper.getMainLooper()).postDelayed({
            finish()
        }, 1000)
    }

    private fun showErrorAndExit(message: String) {
        try {
            val textView = TextView(this).apply {
                text = "Ошибка:\n$message\n\nНажмите для выхода"
                textSize = 16f
                setPadding(50, 100, 50, 100)
                setOnClickListener { finish() }
            }
            setContentView(textView)
        } catch (e: Exception) {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("CalendarActivity", "=== АКТИВНОСТЬ УНИЧТОЖЕНА ===")
    }
}