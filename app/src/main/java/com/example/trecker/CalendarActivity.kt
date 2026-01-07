package com.example.trecker

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
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
    private var selectedTime: String = "12:00"
    private val selectedDates = mutableListOf<Date>()
    private val displayDateFormat = SimpleDateFormat("d MMMM yyyy", Locale("ru"))
    private val dateFormatter = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("ru"))
    private val dayOfWeekFormatter = SimpleDateFormat("EEEE", Locale("ru"))

    // Цвета для выделения выбранных дней
    private val defaultButtonColor = Color.parseColor("#AF8482")
    private val selectedButtonColor = Color.parseColor("#4CAF50")
    private val weekendButtonColor = Color.parseColor("#2196F3")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("CalendarActivity", "=== СОЗДАНИЕ АКТИВНОСТИ ===")
        Log.d("CalendarActivity", "Размер экрана: ${resources.displayMetrics.heightPixels}px")

        try {
            binding = ActivityCalendarBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Проверяем видимость кнопки
            binding.root.post {
                Log.d("CalendarActivity", "Кнопка добавления видима: ${binding.addHabitButton.isShown}")
                Log.d("CalendarActivity", "Кнопка добавления координаты: ${binding.addHabitButton.top} - ${binding.addHabitButton.bottom}")
            }


            binding = ActivityCalendarBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d("CalendarActivity", "Layout загружен")

            habitManager = HabitManager(this)
            Log.d("CalendarActivity", "HabitManager создан")

            // Инициализируем UI
            initUI()

            Log.d("CalendarActivity", "=== АКТИВНОСТЬ СОЗДАНА УСПЕШНО ===")

        }

        catch (e: Exception) {
            Log.e("CalendarActivity", "КРИТИЧЕСКАЯ ОШИБКА в onCreate: ${e.message}", e)
            showErrorAndExit("Ошибка загрузки интерфейса")
        }
    }

    private fun initUI() {
        try {
            // Обновляем отображение времени
            updateTimeDisplay()

            // Настраиваем поле ввода
            setupHabitNameInput()

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

    private fun setupHabitNameInput() {
        binding.habitNameInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateAddButtonState()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Добавляем обработку кнопки "Готово" на клавиатуре
        binding.habitNameInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // Скрываем клавиатуру
                hideKeyboard()
                true
            } else {
                false
            }
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.habitNameInput.windowToken, 0)
    }

    private fun updateAddButtonState() {
        val hasHabitName = binding.habitNameInput.text.toString().trim().isNotEmpty()
        val hasDates = selectedDates.isNotEmpty()

        // Кнопка активна только если есть название и хотя бы одна дата
        binding.addHabitButton.isEnabled = hasHabitName && hasDates

        // Меняем цвет кнопки в зависимости от состояния
        if (binding.addHabitButton.isEnabled) {
            binding.addHabitButton.setBackgroundColor(Color.parseColor("#4CAF50")) // Зеленый
        } else {
            binding.addHabitButton.setBackgroundColor(Color.parseColor("#CCCCCC")) // Серый
        }
    }

    private fun updateTimeDisplay() {
        try {
            binding.selectedDateText.text = "Время: $selectedTime"
            Log.d("CalendarActivity", "Время обновлено: $selectedTime")
        } catch (e: Exception) {
            binding.selectedDateText.text = "Время: 12:00"
        }
    }

    private fun updateSelectOrAddButtonText() {
        val buttonText = if (selectedDates.isEmpty()) {
            "Добавить дату"
        } else {
            "Добавить еще дату"
        }

        binding.selectOrAddDateButton.text = buttonText
    }

    private fun setupButtons() {
        Log.d("CalendarActivity", "Настройка кнопок...")

        // Кнопка назад
        binding.backButton.setOnClickListener {
            Log.d("CalendarActivity", "Нажата кнопка Назад")
            finish()
        }

        // Кнопка добавления даты
        binding.selectOrAddDateButton.setOnClickListener {
            Log.d("CalendarActivity", "Нажата кнопка 'Добавить дату'")
            showMaterialDatePickerForAdding()
        }

        // Кнопка выбора времени
        binding.selectTimeButton.setOnClickListener {
            Log.d("CalendarActivity", "Нажата кнопка Выбрать время")
            showSimpleTimePicker()
        }

        // Кнопка добавить привычку
        binding.addHabitButton.setOnClickListener {
            Log.d("CalendarActivity", "=== НАЖАТА КНОПКА 'ДОБАВИТЬ ПРИВЫЧКУ' ===")
            addHabitToSelectedDates()
        }

        // Кнопка очистки дат
        binding.clearDatesButton.setOnClickListener {
            Log.d("CalendarActivity", "Нажата кнопка Очистить даты")
            clearAllDates()
        }

        // Кнопки быстрого выбора
        setupQuickSelectionButtons()

        Log.d("CalendarActivity", "Все кнопки настроены")
    }

    private fun setupQuickSelectionButtons() {
        // Рабочие дни (пн-пт)
        binding.btnWeekdays.setOnClickListener {
            Log.d("CalendarActivity", "Добавление рабочих дней")
            addWeekdays()
        }

        // Выходные (сб-вс)
        binding.btnWeekends.setOnClickListener {
            Log.d("CalendarActivity", "Добавление выходных")
            addWeekends()
        }

        // Сегодня
        binding.btnToday.setOnClickListener {
            Log.d("CalendarActivity", "Добавление сегодня")
            addToday()
        }

        // Дни недели
        binding.btnMonday.setOnClickListener { toggleDayOfWeek(Calendar.MONDAY, binding.btnMonday) }
        binding.btnTuesday.setOnClickListener { toggleDayOfWeek(Calendar.TUESDAY, binding.btnTuesday) }
        binding.btnWednesday.setOnClickListener { toggleDayOfWeek(Calendar.WEDNESDAY, binding.btnWednesday) }
        binding.btnThursday.setOnClickListener { toggleDayOfWeek(Calendar.THURSDAY, binding.btnThursday) }
        binding.btnFriday.setOnClickListener { toggleDayOfWeek(Calendar.FRIDAY, binding.btnFriday) }
        binding.btnSaturday.setOnClickListener { toggleDayOfWeek(Calendar.SATURDAY, binding.btnSaturday) }
        binding.btnSunday.setOnClickListener { toggleDayOfWeek(Calendar.SUNDAY, binding.btnSunday) }
    }

    private fun addWeekdays() {
        val daysOfWeek = listOf(
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY
        )

        addDaysOfWeek(daysOfWeek, "рабочие дни")
    }

    private fun addWeekends() {
        val daysOfWeek = listOf(
            Calendar.SATURDAY,
            Calendar.SUNDAY
        )

        addDaysOfWeek(daysOfWeek, "выходные")
    }

    private fun addToday() {
        val today = Calendar.getInstance()
        val date = today.time

        // Проверяем, есть ли уже такая дата
        val isAlreadyAdded = selectedDates.any { DateUtils.isSameDay(it, date) }

        if (!isAlreadyAdded) {
            selectedDates.add(date)
            updateDatesDisplay()
            Toast.makeText(this, "Добавлено: сегодня", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Сегодня уже есть в списке", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleDayOfWeek(dayOfWeek: Int, button: Button) {
        // Получаем все ближайшие даты для этого дня недели (на 4 недели вперед)
        val dates = getNextDatesForDayOfWeek(dayOfWeek, 4)

        // Проверяем, есть ли уже какие-то из этих дат в списке
        val newDates = dates.filter { date ->
            !selectedDates.any { DateUtils.isSameDay(it, date) }
        }

        if (newDates.isNotEmpty()) {
            selectedDates.addAll(newDates)
            updateDatesDisplay()

            // Подсвечиваем кнопку как выбранную
            button.setBackgroundColor(selectedButtonColor)

            val dayName = when (dayOfWeek) {
                Calendar.MONDAY -> "понедельники"
                Calendar.TUESDAY -> "вторники"
                Calendar.WEDNESDAY -> "среды"
                Calendar.THURSDAY -> "четверги"
                Calendar.FRIDAY -> "пятницы"
                Calendar.SATURDAY -> "субботы"
                Calendar.SUNDAY -> "воскресенья"
                else -> "дни"
            }

            Toast.makeText(this, "Добавлены $dayName на 4 недели", Toast.LENGTH_SHORT).show()
        } else {
            // Убираем все даты с этим днем недели
            val datesToRemove = selectedDates.filter { date ->
                val cal = Calendar.getInstance()
                cal.time = date
                cal.get(Calendar.DAY_OF_WEEK) == dayOfWeek
            }

            selectedDates.removeAll(datesToRemove)
            updateDatesDisplay()

            // Возвращаем стандартный цвет кнопки
            button.setBackgroundColor(defaultButtonColor)

            Toast.makeText(this, "Удалены все даты этого дня недели", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getNextDatesForDayOfWeek(targetDayOfWeek: Int, weeksCount: Int): List<Date> {
        val result = mutableListOf<Date>()
        val calendar = Calendar.getInstance()

        // Начинаем с сегодняшнего дня
        calendar.time = Date()

        // Находим ближайший день с нужным днем недели
        while (calendar.get(Calendar.DAY_OF_WEEK) != targetDayOfWeek) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Добавляем даты на указанное количество недель
        for (week in 0 until weeksCount) {
            val date = calendar.time
            result.add(date)
            calendar.add(Calendar.DAY_OF_MONTH, 7) // Переходим к следующей неделе
        }

        return result
    }

    private fun addDaysOfWeek(daysOfWeek: List<Int>, description: String) {
        val newDates = mutableListOf<Date>()

        // Для каждого дня недели получаем ближайшие 4 даты
        daysOfWeek.forEach { dayOfWeek ->
            val dates = getNextDatesForDayOfWeek(dayOfWeek, 4)
            dates.forEach { date ->
                // Проверяем, есть ли уже такая дата
                val isAlreadyAdded = selectedDates.any { DateUtils.isSameDay(it, date) }
                if (!isAlreadyAdded) {
                    newDates.add(date)
                }
            }
        }

        if (newDates.isNotEmpty()) {
            selectedDates.addAll(newDates)
            updateDatesDisplay()
            Toast.makeText(this, "Добавлены $description на 4 недели", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Все даты уже добавлены", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateDatesDisplay() {
        Log.d("CalendarActivity", "Обновление списка дат. Количество: ${selectedDates.size}")

        // Обновляем текст кнопки
        updateSelectOrAddButtonText()

        // Обновляем состояние кнопки добавления
        updateAddButtonState()

        if (selectedDates.isEmpty()) {
            // Нет выбранных дат - скрываем элементы
            binding.datesCountText.visibility = View.GONE
            binding.clearDatesButton.visibility = View.GONE
            binding.selectedDatesContainer.visibility = View.GONE

            // Сбрасываем подсветку кнопок дней недели
            resetDayOfWeekButtons()
        } else {
            // Есть выбранные даты - показываем
            binding.datesCountText.visibility = View.VISIBLE
            binding.clearDatesButton.visibility = View.VISIBLE
            binding.selectedDatesContainer.visibility = View.VISIBLE

            // Обновляем счетчик
            binding.datesCountText.text = "Выбрано дат: ${selectedDates.size}"

            // Очищаем контейнер
            binding.selectedDatesContainer.removeAllViews()

            // Добавляем каждую дату в хронологическом порядке
            selectedDates.sorted().forEachIndexed { index, date ->
                val dateView = createDateView(date, index)
                binding.selectedDatesContainer.addView(dateView)
            }

            // Подсвечиваем кнопки дней недели
            highlightDayOfWeekButtons()

            // Прокручиваем к кнопке добавления
            binding.root.post {
                binding.root.smoothScrollTo(0, binding.addHabitButton.bottom)
            }
        }
    }

    private fun resetDayOfWeekButtons() {
        val buttons = listOf(
            binding.btnMonday, binding.btnTuesday, binding.btnWednesday,
            binding.btnThursday, binding.btnFriday, binding.btnSaturday, binding.btnSunday
        )

        buttons.forEach { button ->
            button.setBackgroundColor(defaultButtonColor)
        }
    }

    private fun highlightDayOfWeekButtons() {
        val dayOfWeekButtons = mapOf(
            Calendar.MONDAY to binding.btnMonday,
            Calendar.TUESDAY to binding.btnTuesday,
            Calendar.WEDNESDAY to binding.btnWednesday,
            Calendar.THURSDAY to binding.btnThursday,
            Calendar.FRIDAY to binding.btnFriday,
            Calendar.SATURDAY to binding.btnSaturday,
            Calendar.SUNDAY to binding.btnSunday
        )

        // Собираем статистику по дням недели
        val daysCount = mutableMapOf<Int, Int>()

        selectedDates.forEach { date ->
            val calendar = Calendar.getInstance()
            calendar.time = date
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            daysCount[dayOfWeek] = daysCount.getOrDefault(dayOfWeek, 0) + 1
        }

        // Подсвечиваем кнопки, если есть даты с этим днем недели
        dayOfWeekButtons.forEach { (dayOfWeek, button) ->
            if (daysCount[dayOfWeek] ?: 0 > 0) {
                button.setBackgroundColor(selectedButtonColor)
            } else {
                button.setBackgroundColor(defaultButtonColor)
            }
        }
    }

    private fun createDateView(date: Date, index: Int): TextView {
        val dayOfWeek = dayOfWeekFormatter.format(date)
        val displayDate = displayDateFormat.format(date)

        return TextView(this).apply {
            text = "${index + 1}. $dayOfWeek, $displayDate в $selectedTime"
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
                    "Дата: $displayDate\nДень недели: $dayOfWeek\nВремя: $selectedTime",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showDeleteDateDialog(index: Int) {
        if (index in 0 until selectedDates.size) {
            val date = selectedDates[index]
            val dayOfWeek = dayOfWeekFormatter.format(date)

            AlertDialog.Builder(this)
                .setTitle("Удалить дату?")
                .setMessage("Удалить $dayOfWeek, ${displayDateFormat.format(date)} из списка?")
                .setPositiveButton("Удалить") { dialog, _ ->
                    selectedDates.removeAt(index)
                    updateDatesDisplay()
                    Toast.makeText(this, "Дата удалена из списка", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .setNegativeButton("Отмена") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
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
                    updateTimeDisplay()
                    updateDatesDisplay() // Обновляем все даты с новым временем
                    Toast.makeText(this, "Время изменено на $selectedTime", Toast.LENGTH_SHORT).show()
                    Log.d("CalendarActivity", "Новое время: $selectedTime")
                },
                currentHour,
                currentMinute,
                true
            )

            timePicker.setTitle("Выберите время для всех дат")
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
            // Используем сегодняшнюю дату как начальную
            val initialDate = if (selectedDates.isNotEmpty()) {
                selectedDates.last().time
            } else {
                Date().time
            }

            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Выберите дату для добавления")
                .setSelection(initialDate)
                .setTheme(R.style.MyMaterialDatePickerTheme)
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = selection
                val newDate = calendar.time

                // Проверяем, есть ли уже такая дата
                val isAlreadyAdded = selectedDates.any { DateUtils.isSameDay(it, newDate) }

                if (!isAlreadyAdded) {
                    selectedDates.add(newDate)
                    updateDatesDisplay()
                    Toast.makeText(this,
                        "Дата добавлена: ${displayDateFormat.format(newDate)}",
                        Toast.LENGTH_SHORT).show()
                    Log.d("CalendarActivity", "Дата добавлена, всего: ${selectedDates.size}")
                } else {
                    Toast.makeText(this, "Эта дата уже есть в списке", Toast.LENGTH_SHORT).show()
                }
            }

            datePicker.addOnNegativeButtonClickListener {
                Log.d("CalendarActivity", "Добавление даты отменено")
            }

            datePicker.show(supportFragmentManager, "ADD_DATE_PICKER")
            Log.d("CalendarActivity", "MaterialDatePicker показан")

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

    private fun addHabitToSelectedDates() {
        val habitName = binding.habitNameInput.text.toString().trim()

        if (habitName.isEmpty()) {
            Toast.makeText(this, "Введите название привычки", Toast.LENGTH_SHORT).show()
            binding.habitNameInput.requestFocus()
            return
        }

        if (selectedDates.isEmpty()) {
            Toast.makeText(this, "Добавьте хотя бы одну дату", Toast.LENGTH_SHORT).show()
            binding.selectOrAddDateButton.requestFocus()
            return
        }

        var successCount = 0

        selectedDates.forEach { date ->
            try {
                // Используем SimpleDateFormat для гарантированного правильного формата
                val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                Log.d("CALENDAR_ADD",
                    "Добавление привычки:\n" +
                            "  Название: $habitName\n" +
                            "  Дата: ${dbDateFormat.format(date)}\n" +
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

                Log.d("CALENDAR_ADD", "Привычка добавлена на дату: ${dbDateFormat.format(date)}")

            } catch (e: Exception) {
                Log.e("CALENDAR_ADD", "Ошибка: ${e.message}", e)
            }
        }

        // Показываем результат
        val message = if (successCount == 0) {
            "Ошибка добавления привычек"
        } else {
            "Привычка \"$habitName\" добавлена на $successCount дат!"
        }

        // Показать подробный отчет о добавленных датах
        if (successCount > 0) {
            val datesList = selectedDates.joinToString("\n") { date ->
                val dayOfWeek = dayOfWeekFormatter.format(date)
                "• $dayOfWeek, ${displayDateFormat.format(date)} в $selectedTime"
            }

            AlertDialog.Builder(this)
                .setTitle("✅ Привычка добавлена!")
                .setMessage("$message\n\nДобавлена на даты:\n$datesList")
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    // Возвращаемся на главный экран
                    Handler(Looper.getMainLooper()).postDelayed({
                        finish()
                    }, 500)
                }
                .setCancelable(false)
                .show()
        } else {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }

        // Отключаем кнопку после успешного добавления
        binding.addHabitButton.isEnabled = false
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