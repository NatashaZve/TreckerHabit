package com.example.trecker

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.trecker.databinding.ActivityAddHabitImprovedBinding
import java.text.SimpleDateFormat
import java.util.*

class AddHabitActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddHabitImprovedBinding

    // Календари для разных целей
    private val startCalendar = Calendar.getInstance()
    private val endCalendar = Calendar.getInstance()
    private val timeCalendar = Calendar.getInstance()

    private val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale("ru"))
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    // Текущие настройки
    private var repeatInterval = 1
    private var repeatUnit = IntervalUnit.DAYS
    private var repeatEnabled = false
    private var endDate: Date? = null
    private var startDate: Date = Date()
    private var selectedTime: String = "12:00"
    private var reminderMinutes = 0
    private var notificationEnabled = true

    // HabitManager
    private lateinit var habitManager: HabitManager

    // Настройки уведомлений
    private val reminderOptions = listOf(
        Pair(0, "В момент выполнения"),
        Pair(5, "5 минут"),
        Pair(15, "15 минут"),
        Pair(30, "30 минут"),
        Pair(60, "1 час"),
        Pair(120, "2 часа"),
        Pair(1440, "1 день")
    )

    companion object {
        private const val TAG = "AddHabitActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddHabitImprovedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализация HabitManager
        habitManager = HabitManager(this)

        setupUI()
        setupListeners()
        setupInitialValues()
    }

    private fun setupUI() {
        // Валидация названия в реальном времени
        binding.habitNameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.habitNameEditText.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupInitialValues() {
        // Устанавливаем текущую дату как дату начала
        startDate = Date()
        startCalendar.time = startDate

        val today = dateFormat.format(startDate)
        binding.startDateText.text = today

        // Устанавливаем время по умолчанию
        selectedTime = "12:00"
        binding.timeText.text = selectedTime

        // Инициализируем календарь конечной даты
        endCalendar.time = Date()
        endCalendar.add(Calendar.MONTH, 1)

        // Скрываем настройки повторения по умолчанию
        repeatEnabled = false
        binding.repeatSettingsLayout.isVisible = false

        // Обновляем UI
        updateRepeatSettingsUI()
        updateNotificationSettingsUI()
    }

    private fun setupListeners() {
        // Кнопка назад
        binding.backButton.setOnClickListener {
            finish()
        }

        // Кнопка сохранения
        binding.saveButton.setOnClickListener {
            saveHabit()
        }

        // Выбор даты начала
        binding.startDateLayout.setOnClickListener {
            showDatePicker(startCalendar) { selectedDate ->
                startDate = selectedDate
                startCalendar.time = selectedDate
                binding.startDateText.text = dateFormat.format(selectedDate)
                Log.d(TAG, "Установлена дата начала: $selectedDate")
            }
        }

        // Выбор времени
        binding.timeLayout.setOnClickListener {
            showTimePicker()
        }

        // Переключатель повторения
        binding.repeatSwitch.setOnCheckedChangeListener { _, isChecked ->
            repeatEnabled = isChecked
            updateRepeatSettingsUI()
        }

        // Увеличение интервала
        binding.increaseIntervalButton.setOnClickListener {
            if (repeatInterval < 365) {
                repeatInterval++
                updateRepeatIntervalUI()
            }
        }

        // Уменьшение интервала
        binding.decreaseIntervalButton.setOnClickListener {
            if (repeatInterval > 1) {
                repeatInterval--
                updateRepeatIntervalUI()
            }
        }

        // Выбор единицы времени
        binding.repeatUnitLayout.setOnClickListener {
            showRepeatUnitSelector()
        }

        // Выбор конечной даты
        binding.endDateLayout.setOnClickListener {
            showDatePicker(endCalendar) { selectedDate ->
                endDate = selectedDate
                endCalendar.time = selectedDate
                updateEndDateUI()
                Log.d(TAG, "Установлена конечная дата: $selectedDate")
            }
        }

        // Переключатель уведомлений
        binding.notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            notificationEnabled = isChecked
            updateNotificationSettingsUI()
        }

        // Выбор времени напоминания
        binding.reminderTimeLayout.setOnClickListener {
            showReminderTimeSelector()
        }
    }

    private fun updateRepeatSettingsUI() {
        // Показываем/скрываем настройки повторения
        binding.repeatSettingsLayout.isVisible = repeatEnabled

        if (repeatEnabled) {
            updateRepeatIntervalUI()
            updateRepeatUnitUI()
            updateEndDateUI()
        }
    }

    private fun updateRepeatIntervalUI() {
        binding.repeatIntervalText.text = repeatInterval.toString()
    }

    private fun updateRepeatUnitUI() {
        val unitText = when (repeatUnit) {
            IntervalUnit.DAYS -> "дней"
            IntervalUnit.WEEKS -> "недель"
            IntervalUnit.MONTHS -> "месяцев"
            IntervalUnit.YEARS -> "лет"
        }
        binding.repeatUnitText.text = unitText
    }

    private fun updateEndDateUI() {
        val endDateText = if (endDate != null) {
            dateFormat.format(endDate!!)
        } else {
            "Никогда"
        }
        binding.endDateText.text = endDateText
    }

    private fun updateNotificationSettingsUI() {
        binding.notificationSettingsLayout.isVisible = notificationEnabled

        if (notificationEnabled) {
            updateReminderTimeUI()
        }
    }

    private fun updateReminderTimeUI() {
        val reminderText = when {
            reminderMinutes == 0 -> "В момент выполнения"
            reminderMinutes < 60 -> "$reminderMinutes мин"
            reminderMinutes < 1440 -> "${reminderMinutes / 60} час"
            else -> "${reminderMinutes / 1440} день"
        }
        binding.reminderTimeText.text = reminderText
    }

    private fun showRepeatUnitSelector() {
        val units = arrayOf("дней", "недель", "месяцев", "лет")

        AlertDialog.Builder(this)
            .setTitle("Выберите период")
            .setItems(units) { _, which ->
                repeatUnit = when (which) {
                    0 -> IntervalUnit.DAYS
                    1 -> IntervalUnit.WEEKS
                    2 -> IntervalUnit.MONTHS
                    3 -> IntervalUnit.YEARS
                    else -> IntervalUnit.DAYS
                }
                updateRepeatUnitUI()
            }
            .show()
    }

    private fun showReminderTimeSelector() {
        val options = reminderOptions.map { it.second }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Напоминать за")
            .setItems(options) { _, which ->
                reminderMinutes = reminderOptions[which].first
                updateReminderTimeUI()
            }
            .show()
    }

    private fun showDatePicker(calendar: Calendar, onDateSelected: (Date) -> Unit) {
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                val date = calendar.time
                onDateSelected(date)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePicker.show()
    }

    private fun showTimePicker() {
        val currentTime = parseTimeString(selectedTime)
        timeCalendar.time = currentTime ?: Date()

        val timePicker = TimePickerDialog(
            this,
            { _, hour, minute ->
                selectedTime = String.format("%02d:%02d", hour, minute)
                binding.timeText.text = selectedTime
                Log.d(TAG, "Установлено время: $selectedTime")
            },
            timeCalendar.get(Calendar.HOUR_OF_DAY),
            timeCalendar.get(Calendar.MINUTE),
            true
        )

        timePicker.show()
    }

    private fun parseTimeString(timeStr: String): Date? {
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

    private fun combineDateAndTime(date: Date, timeString: String): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date

        return try {
            val parts = timeString.split(":")
            if (parts.size == 2) {
                calendar.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                calendar.set(Calendar.MINUTE, parts[1].toInt())
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            } else {
                calendar.set(Calendar.HOUR_OF_DAY, 12)
                calendar.set(Calendar.MINUTE, 0)
            }
            calendar.time
        } catch (e: Exception) {
            date
        }
    }

    private fun saveHabit() {
        // Проверяем валидность данных
        val name = binding.habitNameEditText.text.toString().trim()
        if (name.isEmpty()) {
            binding.habitNameEditText.error = "Введите название привычки"
            binding.habitNameEditText.requestFocus()
            return
        }

        // Проверяем время
        if (!DateUtils.isValidTime(selectedTime)) {
            binding.timeText.error = "Введите корректное время (HH:MM)"
            return
        }

        try {
            // Объединяем дату и время
            val combinedDate = combineDateAndTime(startDate, selectedTime)

            // Определяем тип повторения
            val habitRepeatType = if (repeatEnabled) {
                when {
                    repeatInterval > 1 -> RepeatType.CUSTOM_INTERVAL
                    else -> when (repeatUnit) {
                        IntervalUnit.DAYS -> RepeatType.DAILY
                        IntervalUnit.WEEKS -> RepeatType.WEEKLY
                        IntervalUnit.MONTHS -> RepeatType.MONTHLY
                        IntervalUnit.YEARS -> RepeatType.YEARLY
                    }
                }
            } else {
                RepeatType.ONCE
            }

            Log.d(TAG, "Сохранение привычки:")
            Log.d(TAG, "  Название: $name")
            Log.d(TAG, "  Дата: ${dateFormat.format(combinedDate)}")
            Log.d(TAG, "  Время: $selectedTime")
            Log.d(TAG, "  Тип повторения: $habitRepeatType")
            Log.d(TAG, "  Интервал: $repeatInterval $repeatUnit")
            Log.d(TAG, "  Конечная дата: ${endDate?.let { dateFormat.format(it) } ?: "нет"}")
            Log.d(TAG, "  Уведомления: $notificationEnabled")

            // Создаем настройки повторения
            val repeatSettings = RepeatSettings(
                repeatType = habitRepeatType,  // ← Используем локальную переменную
                startDate = combinedDate,
                endDate = endDate,
                interval = repeatInterval,
                intervalUnit = repeatUnit
            )

            // Создаем настройки уведомлений
            val notificationSettings = NotificationSettings(
                enabled = notificationEnabled,
                reminderType = when (reminderMinutes) {
                    0 -> ReminderType.AT_TIME
                    5 -> ReminderType.MINUTES_5
                    15 -> ReminderType.MINUTES_15
                    30 -> ReminderType.MINUTES_30
                    60 -> ReminderType.HOURS_1
                    120 -> ReminderType.HOURS_2
                    1440 -> ReminderType.DAYS_1
                    else -> ReminderType.CUSTOM
                },
                advanceMinutes = reminderMinutes
            )

            // Создаем общие настройки привычки
            val habitSettings = HabitSettings(
                name = name,
                repeatSettings = repeatSettings,
                notificationSettings = notificationSettings,
                color = "#FF6B6B",
                icon = "🎯",
                priority = 1,
                description = "",
                category = "Общие"
            )

            // Используем метод для создания одной привычки
            val savedHabit = habitManager.addSingleHabitWithSettings(habitSettings)

            if (savedHabit.id > 0) {
                showSuccessMessage("Привычка \"$name\" создана!")

                // Возвращаемся на главный экран
                binding.root.postDelayed({
                    finish()
                }, 1000)
            } else {
                showErrorMessage("Не удалось сохранить привычку")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка сохранения привычки", e)
            showErrorMessage("Ошибка: ${e.localizedMessage}")
        }
    }

    private fun showSuccessMessage(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Успешно!")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showErrorMessage(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Ошибка")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}