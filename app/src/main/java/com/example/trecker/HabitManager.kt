package com.example.trecker

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class HabitManager(private val context: Context) {
    private val sharedPreferences: SharedPreferences
    private val gson = Gson()
    private val habitsKey = "habits"
    private val habitsSettingsKey = "habits_settings"
    private var nextId = 1

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val notificationManager = HabitNotificationManager(context)
    private val motivationManager = MotivationManager(context)

    init {
        sharedPreferences = context.getSharedPreferences("HabitPrefs", Context.MODE_PRIVATE)
        nextId = sharedPreferences.getInt("nextId", 1)
    }

    // ==================== НОВАЯ СИСТЕМА ДОБАВЛЕНИЯ ПРИВЫЧЕК ====================

    fun addHabitWithSettings(settings: HabitSettings): List<Habit> {
        Log.d("HabitManager", "Добавление привычки с настройками: ${settings.name}")

        // Генерируем даты
        val dates = generateDatesFromSettings(settings.repeatSettings)

        // Убираем дубликаты дат
        val uniqueDates = dates.distinctBy { dateFormat.format(it) }

        Log.d("HabitManager", "Было ${dates.size} дат, стало ${uniqueDates.size} уникальных дат")

        if (uniqueDates.isEmpty()) {
            Log.w("HabitManager", "Не сгенерировано ни одной даты")
            return emptyList()
        }

        val habits = mutableListOf<Habit>()
        var currentId = nextId

        // Создаем привычки только для уникальных дат
        uniqueDates.forEachIndexed { index, date ->
            val habit = createHabitForDate(settings, date, currentId + index)
            habits.add(habit)
            saveHabitSettingsMapping(habit.id, settings)

            Log.d("HabitManager", "Создана привычка на ${dateFormat.format(date)}")
        }

        // Обновляем счетчик ID
        nextId += habits.size
        saveNextId()

        // Сохраняем все привычки
        val allHabits = getAllHabits().toMutableList()
        allHabits.addAll(habits)
        saveHabits(allHabits)

        // Планируем уведомления
        habits.forEach { habit ->
            if (settings.notificationSettings.enabled) {
                scheduleNotificationsForHabit(habit, settings.notificationSettings)
            }
        }

        Log.d("HabitManager", "Добавлено ${habits.size} привычек")
        return habits
    }

    private fun createHabitForDate(
        settings: HabitSettings,
        date: Date,
        id: Int
    ): Habit {
        // Используем время из настроек
        val time = settings.time

        // Конвертируем RepeatType из настроек
        val habitRepeatType = when (settings.repeatSettings.repeatType) {
            RepeatType.NEVER -> RepeatType.ONCE
            else -> settings.repeatSettings.repeatType
        }

        return Habit(
            id = id,
            name = settings.name,
            date = date,
            time = time,
            repeatType = habitRepeatType,
            repeatDays = getRepeatDaysString(settings.repeatSettings),
            endDate = settings.repeatSettings.endDate,
            notificationEnabled = settings.notificationSettings.enabled,
            notificationId = Habit.generateNotificationId() + id,
            notificationChannel = NotificationHelper.CHANNEL_REMINDERS_ID,
            color = settings.color,
            icon = settings.icon,
            priority = settings.priority,
            notes = settings.description,
            category = settings.category,
            repeatInterval = settings.repeatSettings.interval, // ← ДОБАВЬТЕ
            repeatIntervalUnit = settings.repeatSettings.intervalUnit // ← ДОБАВЬТЕ
        )
    }

    /**
     * Создать объект Habit из настроек
     */
    private fun createHabitFromSettings(
        settings: HabitSettings,
        date: Date,
        id: Int
    ): Habit {
        val time = "12:00"

        // Конвертируем RepeatType из настроек в тот, который ожидает Habit
        val habitRepeatType = when (settings.repeatSettings.repeatType) {
            RepeatType.NEVER -> RepeatType.ONCE  // Для совместимости с Habit.kt
            else -> settings.repeatSettings.repeatType
        }

        return Habit(
            id = id,
            name = settings.name,
            date = date,
            time = time,
            repeatType = habitRepeatType,  // Используем конвертированный тип
            repeatDays = getRepeatDaysString(settings.repeatSettings),
            endDate = settings.repeatSettings.endDate,
            notificationEnabled = settings.notificationSettings.enabled,
            notificationId = Habit.generateNotificationId(),
            notificationChannel = NotificationHelper.CHANNEL_REMINDERS_ID,
            color = settings.color,
            icon = settings.icon,
            priority = settings.priority,
            notes = settings.description,
            category = settings.category,
            createdAt = System.currentTimeMillis()
        )
    }

    /**
     * Получить строку дней для повторения
     */
    private fun getRepeatDaysString(repeatSettings: RepeatSettings): String {
        return when (repeatSettings.repeatType) {
            RepeatType.SPECIFIC_DAYS, RepeatType.WEEKLY ->
                repeatSettings.daysOfWeek.joinToString(",")
            else -> ""
        }
    }

    /**
     * Генерация дат на основе настроек повторения
     */
    private fun generateDatesFromSettings(repeatSettings: RepeatSettings): List<Date> {
        val dates = mutableListOf<Date>()

        val startDate = repeatSettings.startDate ?: Date()
        val endDate = repeatSettings.endDate

        Log.d("HabitManager", "=== ГЕНЕРАЦИЯ ДАТ НАСТРОЙКИ ===")
        Log.d("HabitManager", "Тип: ${repeatSettings.repeatType}")
        Log.d("HabitManager", "Интервал: ${repeatSettings.interval} ${repeatSettings.intervalUnit}")
        Log.d("HabitManager", "От: ${dateFormat.format(startDate)}")
        Log.d("HabitManager", "До: ${endDate?.let { dateFormat.format(it) } ?: "нет"}")

        val calendar = Calendar.getInstance().apply {
            time = startDate
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        when (repeatSettings.repeatType) {
            RepeatType.ONCE, RepeatType.NEVER -> {
                // Один раз
                dates.add(calendar.time)
            }

            RepeatType.DAILY -> {
                // Ежедневно = интервал 1 день
                generateDatesWithCustomInterval(calendar, endDate, 1, dates)
            }

            RepeatType.WEEKLY -> {
                // Еженедельно по выбранным дням
                generateWeeklyDates(calendar, repeatSettings, dates)
            }

            RepeatType.MONTHLY -> {
                // Ежемесячно по выбранным дням
                generateMonthlyDates(calendar, repeatSettings, dates)
            }

            RepeatType.YEARLY -> {
                // Ежегодно
                generateYearlyDates(calendar, repeatSettings, dates)
            }

            RepeatType.CUSTOM_INTERVAL -> {
                // Пользовательский интервал
                when (repeatSettings.intervalUnit) {
                    IntervalUnit.DAYS -> {
                        // Каждые N дней
                        generateDatesWithCustomInterval(calendar, endDate, repeatSettings.interval, dates)
                    }
                    IntervalUnit.WEEKS -> {
                        // Каждые N недель (N * 7 дней)
                        generateDatesWithCustomInterval(calendar, endDate, repeatSettings.interval * 7, dates)
                    }
                    IntervalUnit.MONTHS -> {
                        // Каждые N месяцев - сложная логика
                        generateMonthlyIntervalDates(calendar, repeatSettings, dates)
                    }
                    IntervalUnit.YEARS -> {
                        // Каждые N лет
                        generateYearlyIntervalDates(calendar, repeatSettings, dates)
                    }
                }
            }

            RepeatType.WEEKDAYS -> {
                // Только рабочие дни
                generateWeekdayDates(calendar, repeatSettings, dates)
            }

            RepeatType.WEEKENDS -> {
                // Только выходные
                generateWeekendDates(calendar, repeatSettings, dates)
            }

            RepeatType.SPECIFIC_DAYS -> {
                // Конкретные дни недели
                generateSpecificDaysDates(calendar, repeatSettings, dates)
            }

            RepeatType.CUSTOM_DAYS -> {
                // Для CUSTOM_DAYS используем ту же логику, что и для SPECIFIC_DAYS
                generateSpecificDaysDates(calendar, repeatSettings, dates)
            }
        }

        Log.d("HabitManager", "Сгенерировано ${dates.size} дат")
        return dates
    }

    private fun generateMonthlyIntervalDates(
        calendar: Calendar,
        repeatSettings: RepeatSettings,
        dates: MutableList<Date>
    ) {
        val interval = repeatSettings.interval
        val endDate = repeatSettings.endDate

        Log.d("HabitManager", "Генерация: каждые $interval месяцев")

        var count = 0
        val currentCalendar = calendar.clone() as Calendar

        while (count < 100) {
            val currentDate = currentCalendar.time

            // Проверяем дату окончания
            if (endDate != null && currentDate.after(endDate)) {
                break
            }

            dates.add(currentDate)
            count++

            // Добавляем N месяцев
            currentCalendar.add(Calendar.MONTH, interval)
        }
    }

    private fun generateYearlyIntervalDates(
        calendar: Calendar,
        repeatSettings: RepeatSettings,
        dates: MutableList<Date>
    ) {
        val interval = repeatSettings.interval
        val endDate = repeatSettings.endDate

        Log.d("HabitManager", "Генерация: каждые $interval лет")

        var count = 0
        val currentCalendar = calendar.clone() as Calendar

        while (count < 50) { // максимум 50 лет
            val currentDate = currentCalendar.time

            // Проверяем дату окончания
            if (endDate != null && currentDate.after(endDate)) {
                Log.d("HabitManager", "Достигнута дата окончания для ежегодных интервалов")
                break
            }

            dates.add(currentDate)
            count++
            Log.d("HabitManager", "Добавлен год $count: ${dateFormat.format(currentDate)}")

            // Добавляем N лет
            currentCalendar.add(Calendar.YEAR, interval)
        }

        Log.d("HabitManager", "Сгенерировано $count ежегодных дат")
    }

    /**
     * Генерация ежедневных дат
     */
    private fun generateDatesWithCustomInterval(
        startCalendar: Calendar,
        endDate: Date?,
        intervalDays: Int, // Количество дней МЕЖДУ повторениями
        dates: MutableList<Date>
    ) {
        Log.d("HabitManager", "ГЕНЕРАЦИЯ ДАТ с интервалом: каждые $intervalDays дней")
        Log.d("HabitManager", "ПРИМЕЧАНИЕ: интервал $intervalDays дней = повторение через $intervalDays дней")

        // Создаем копию календаря
        val calendar = startCalendar.clone() as Calendar

        // Сбрасываем время на 12:00 для единообразия
        calendar.set(Calendar.HOUR_OF_DAY, 12)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        var count = 0
        val maxDays = 1000

        while (count < maxDays) {
            val currentDate = calendar.time

            // Проверяем дату окончания
            if (endDate != null) {
                val endCal = Calendar.getInstance().apply {
                    time = endDate
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                // Если текущая дата ПОСЛЕ окончания - СТОП
                if (calendar.after(endCal)) {
                    Log.d("HabitManager", "СТОП: дата после окончания")
                    break
                }
            }

            // Добавляем дату
            dates.add(currentDate)
            count++

            Log.d("HabitManager", "День ${count}: ${dateFormat.format(currentDate)} (интервал: $intervalDays)")

            // ВАЖНО: добавляем intervalDays для перехода к следующей дате
            // Если intervalDays = 3: сегодня, через 3 дня, через 6 дней и т.д.
            calendar.add(Calendar.DAY_OF_MONTH, intervalDays)
        }

        Log.d("HabitManager", "Итого сгенерировано: $count дат")
        Log.d("HabitManager", "Интервал между датами: $intervalDays дней")
    }

    /**
     * Генерация еженедельных дат
     */
    private fun generateWeeklyDates(
        calendar: Calendar,
        repeatSettings: RepeatSettings,
        dates: MutableList<Date>
    ) {
        val daysOfWeek = repeatSettings.daysOfWeek.toMutableList()
        val endDate = repeatSettings.endDate

        // Если дни не выбраны, используем день недели из даты начала
        if (daysOfWeek.isEmpty()) {
            val startDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            daysOfWeek.add(startDayOfWeek)
            Log.d("HabitManager", "Не выбраны дни недели, используем день начала: $startDayOfWeek")
        }

        Log.d("HabitManager", "Генерация еженедельных дат для дней: $daysOfWeek")

        var currentCalendar = calendar.clone() as Calendar
        var count = 0
        val maxWeeks = 52 // Максимум на год вперед

        for (week in 0 until maxWeeks) {
            for (dayOfWeek in daysOfWeek) {
                val weekCalendar = currentCalendar.clone() as Calendar
                weekCalendar.add(Calendar.WEEK_OF_YEAR, week)

                // Устанавливаем правильный день недели
                val currentDayOfWeek = weekCalendar.get(Calendar.DAY_OF_WEEK)
                val daysToAdd = (dayOfWeek - currentDayOfWeek + 7) % 7
                weekCalendar.add(Calendar.DAY_OF_MONTH, daysToAdd)

                val currentDate = weekCalendar.time

                // Проверяем, что дата не раньше начала
                if (currentDate.before(repeatSettings.startDate ?: Date())) {
                    continue
                }

                // Проверяем дату окончания
                if (endDate != null && currentDate.after(endDate)) {
                    Log.d("HabitManager", "Достигнута дата окончания для недельных дат")
                    return
                }

                dates.add(currentDate)
                count++

                if (count >= 1000) {
                    Log.w("HabitManager", "Достигнут лимит в 1000 недельных дат")
                    return
                }
            }
        }

        Log.d("HabitManager", "Сгенерировано $count еженедельных дат")
    }

    /**
     * Генерация ежемесячных дат
     */
    private fun generateMonthlyDates(
        calendar: Calendar,
        repeatSettings: RepeatSettings,
        dates: MutableList<Date>
    ) {
        val daysOfMonth = if (repeatSettings.daysOfMonth.isEmpty()) {
            // Если дни не выбраны, используем день начала
            listOf(calendar.get(Calendar.DAY_OF_MONTH))
        } else {
            repeatSettings.daysOfMonth
        }

        val startCalendar = calendar.clone() as Calendar
        var count = 0
        val maxMonths = 12 // Максимум на год вперед

        for (month in 0 until maxMonths) {
            // Для каждого месяца проверяем все выбранные дни
            daysOfMonth.forEach { dayOfMonth ->
                val monthCalendar = startCalendar.clone() as Calendar
                monthCalendar.add(Calendar.MONTH, month)

                // Устанавливаем день месяца (с проверкой на валидность)
                val maxDayInMonth = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                val actualDay = minOf(dayOfMonth, maxDayInMonth)
                monthCalendar.set(Calendar.DAY_OF_MONTH, actualDay)

                val currentDate = monthCalendar.time

                // Проверяем, что дата не раньше начала
                if (currentDate.before(repeatSettings.startDate)) {
                    return@forEach
                }

                // Проверяем дату окончания
                if (repeatSettings.endDate != null && currentDate.after(repeatSettings.endDate)) {
                    return
                }

                dates.add(currentDate)
                count++

                if (count >= 1000) {
                    return
                }
            }
        }
    }

    /**
     * Генерация ежегодных дат
     */
    private fun generateYearlyDates(
        calendar: Calendar,
        repeatSettings: RepeatSettings,
        dates: MutableList<Date>
    ) {
        val startCalendar = calendar.clone() as Calendar
        var count = 0
        val maxYears = 5 // Максимум на 5 лет вперед

        for (year in 0 until maxYears) {
            val yearCalendar = startCalendar.clone() as Calendar
            yearCalendar.add(Calendar.YEAR, year)

            val currentDate = yearCalendar.time

            // Проверяем дату окончания
            if (repeatSettings.endDate != null && currentDate.after(repeatSettings.endDate)) {
                break
            }

            dates.add(currentDate)
            count++
        }
    }

    /**
     * Генерация дат с пользовательским интервалом
     */
    /**
     * Генерация дат с пользовательским интервалом
     */
    private fun generateCustomIntervalDates(
        calendar: Calendar,
        repeatSettings: RepeatSettings,
        dates: MutableList<Date>
    ) {
        val interval = repeatSettings.interval
        val unit = repeatSettings.intervalUnit
        val endDate = repeatSettings.endDate

        Log.d("HabitManager", "Генерация кастомных дат: каждые $interval $unit")

        // Функция для получения даты БЕЗ времени
        fun getDateWithoutTime(date: Date): Long {
            val cal = Calendar.getInstance()
            cal.time = date
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        val endDateMillis = endDate?.let { getDateWithoutTime(it) }
        var currentCalendar = calendar.clone() as Calendar
        var count = 0
        val maxCount = 1000

        while (count < maxCount) {
            val currentDate = currentCalendar.time
            val currentDateMillis = getDateWithoutTime(currentDate)

            // Проверяем дату окончания
            if (endDateMillis != null && currentDateMillis > endDateMillis) {
                Log.d("HabitManager", "Прекращаем: дата после окончания")
                break
            }

            dates.add(currentDate)
            count++

            // Добавляем интервал
            when (unit) {
                IntervalUnit.DAYS -> currentCalendar.add(Calendar.DAY_OF_MONTH, interval)
                IntervalUnit.WEEKS -> currentCalendar.add(Calendar.WEEK_OF_YEAR, interval)
                IntervalUnit.MONTHS -> currentCalendar.add(Calendar.MONTH, interval)
                IntervalUnit.YEARS -> currentCalendar.add(Calendar.YEAR, interval)
            }
        }

        Log.d("HabitManager", "Сгенерировано $count дат с кастомным интервалом")
    }

    /**
     * Генерация дат для рабочих дней
     */
    private fun generateWeekdayDates(
        calendar: Calendar,
        repeatSettings: RepeatSettings,
        dates: MutableList<Date>
    ) {
        var currentDate = calendar.time
        var count = 0
        val maxDays = 365

        while (count < maxDays) {
            // Проверяем дату окончания
            if (repeatSettings.endDate != null && currentDate.after(repeatSettings.endDate)) {
                break
            }

            // Проверяем, что это рабочий день (пн-пт)
            calendar.time = currentDate
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

            if (dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY) {
                dates.add(currentDate)
            }

            // Следующий день
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            currentDate = calendar.time
            count++
        }
    }

    /**
     * Генерация дат для выходных
     */
    private fun generateWeekendDates(
        calendar: Calendar,
        repeatSettings: RepeatSettings,
        dates: MutableList<Date>
    ) {
        var currentDate = calendar.time
        var count = 0
        val maxDays = 365

        while (count < maxDays) {
            // Проверяем дату окончания
            if (repeatSettings.endDate != null && currentDate.after(repeatSettings.endDate)) {
                break
            }

            // Проверяем, что это выходной (сб-вс)
            calendar.time = currentDate
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

            if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                dates.add(currentDate)
            }

            // Следующий день
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            currentDate = calendar.time
            count++
        }
    }

    /**
     * Генерация дат для конкретных дней недели
     */
    private fun generateSpecificDaysDates(
        calendar: Calendar,
        repeatSettings: RepeatSettings,
        dates: MutableList<Date>
    ) {
        val daysOfWeek = repeatSettings.daysOfWeek
        val endDate = repeatSettings.endDate

        if (daysOfWeek.isEmpty()) {
            Log.w("HabitManager", "Не выбраны дни недели для SPECIFIC_DAYS")
            return
        }

        Log.d("HabitManager", "Генерация дат для дней недели: $daysOfWeek")

        var currentCalendar = calendar.clone() as Calendar
        var count = 0
        val maxDays = 365 // Максимум на год вперед

        // Устанавливаем на начало дня
        currentCalendar.set(Calendar.HOUR_OF_DAY, 0)
        currentCalendar.set(Calendar.MINUTE, 0)
        currentCalendar.set(Calendar.SECOND, 0)
        currentCalendar.set(Calendar.MILLISECOND, 0)

        while (count < maxDays) {
            val currentDate = currentCalendar.time

            // Проверяем дату окончания
            if (endDate != null && currentDate.after(endDate)) {
                Log.d("HabitManager", "Достигнута дата окончания для дней недели")
                break
            }

            // Проверяем, что это выбранный день недели
            val dayOfWeek = currentCalendar.get(Calendar.DAY_OF_WEEK)
            if (daysOfWeek.contains(dayOfWeek)) {
                dates.add(currentDate)
                count++
                Log.d("HabitManager", "Добавлена дата: ${dateFormat.format(currentDate)} (день недели: $dayOfWeek)")
            }

            // Следующий день
            currentCalendar.add(Calendar.DAY_OF_MONTH, 1)

            if (count >= 1000) {
                Log.w("HabitManager", "Достигнут лимит в 1000 дат для дней недели")
                break
            }
        }

        Log.d("HabitManager", "Сгенерировано $count дат для выбранных дней недели")
    }

    /**
     * Добавить ОДНУ привычку с настройками повторения
     */
    fun addSingleHabitWithRepeatSettings(settings: HabitSettings): Habit {
        Log.d("HabitManager", "Добавление ОДНОЙ привычки с настройками: ${settings.name}")

        // Создаем только ОДНУ привычку
        val habit = createSingleHabitFromSettings(settings)

        // Сохраняем связь с настройками
        saveHabitSettingsMapping(habit.id, settings)

        // Сохраняем привычку
        val allHabits = getAllHabits().toMutableList()
        allHabits.add(habit)
        saveHabits(allHabits)

        // Обновляем счетчик ID
        saveNextId()

        // Планируем уведомления
        if (settings.notificationSettings.enabled) {
            scheduleNotificationsForHabit(habit, settings.notificationSettings)
        }

        Log.d("HabitManager", "Добавлена ОДНА привычка ID: ${habit.id}")
        return habit
    }

    /**
     * Создать ОДНУ привычку из настроек
     */
    private fun createSingleHabitFromSettings(
        settings: HabitSettings,
        id: Int = nextId++
    ): Habit {
        val startDate = settings.repeatSettings.startDate ?: Date()

        // Создаем календарь для объединения даты и времени
        val calendar = Calendar.getInstance().apply {
            time = startDate

            if (settings.time.isNotBlank() && DateUtils.isValidTime(settings.time)) {
                val timeParts = settings.time.split(":")
                if (timeParts.size == 2) {
                    set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                    set(Calendar.MINUTE, timeParts[1].toInt())
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            }
        }

        return Habit(
            id = id,
            name = settings.name,
            date = calendar.time,
            time = if (settings.time.isNotBlank() && DateUtils.isValidTime(settings.time)) settings.time else "12:00",
            repeatType = settings.repeatSettings.repeatType,
            repeatDays = getRepeatDaysString(settings.repeatSettings),
            endDate = settings.repeatSettings.endDate,
            repeatInterval = settings.repeatSettings.interval,
            repeatIntervalUnit = settings.repeatSettings.intervalUnit,
            notificationEnabled = settings.notificationSettings.enabled,
            notificationId = Habit.generateNotificationId(),
            notificationChannel = NotificationHelper.CHANNEL_REMINDERS_ID,
            color = settings.color,
            icon = settings.icon,
            priority = settings.priority,
            notes = settings.description,
            category = settings.category,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun scheduleNotificationsForHabit(
        habit: Habit,
        notificationSettings: NotificationSettings
    ) {
        if (habit.notificationEnabled) {
            try {
                // Рассчитываем время уведомления на основе настроек
                val notificationTime = calculateNotificationTime(habit.time, notificationSettings)

                // Создаем привычку с настроенным временем уведомления
                val habitForNotification = habit.copy(time = notificationTime)

                // Планируем уведомление
                notificationManager.scheduleNotification(habitForNotification)

                Log.d("HabitManager", "Уведомление запланировано для привычки ${habit.id}")
            } catch (e: Exception) {
                Log.e("HabitManager", "Ошибка планирования уведомления: ${e.message}")
            }
        }
    }



    /**
     * Расчет времени уведомления на основе настроек
     */
    private fun calculateNotificationTime(
        habitTime: String,
        notificationSettings: NotificationSettings
    ): String {
        // Парсим время привычки
        val habitTimeParts = habitTime.split(":")
        if (habitTimeParts.size != 2) return habitTime

        var hour = habitTimeParts[0].toIntOrNull() ?: 12
        var minute = habitTimeParts[1].toIntOrNull() ?: 0

        // Корректируем время в зависимости от типа напоминания
        val advanceMinutes = notificationSettings.advanceMinutes

        // Вычитаем минуты напоминания
        minute -= advanceMinutes

        // Корректируем переполнения
        while (minute < 0) {
            minute += 60
            hour -= 1
        }

        while (hour < 0) {
            hour += 24
        }

        return String.format("%02d:%02d", hour, minute)
    }

    /**
     * Отменить уведомление на конкретную дату
     */
    private fun cancelNotificationForDate(habitId: Int, date: Date) {
        try {
            // Создаем уникальный ID уведомления для конкретной даты
            val dateStr = dateFormat.format(date)
            val uniqueNotificationId = habitId + dateStr.hashCode()

            notificationManager.cancelNotification(uniqueNotificationId)
            Log.d("HabitManager", "Уведомление отменено для привычки $habitId на дату $dateStr")
        } catch (e: Exception) {
            Log.e("HabitManager", "Ошибка отмены уведомления: ${e.message}")
        }
    }

    /**
     * Отменить все уведомления для привычки
     */
    private fun cancelAllNotificationsForHabit(habitId: Int) {
        try {
            notificationManager.cancelNotification(habitId)
            Log.d("HabitManager", "Все уведомления отменены для привычки $habitId")
        } catch (e: Exception) {
            Log.e("HabitManager", "Ошибка отмены всех уведомлений: ${e.message}")
        }
    }

    /**
     * Перепланировать уведомление для привычки (при изменении времени)
     */
    fun rescheduleNotification(habit: Habit) {
        try {
            // Получаем настройки привычки
            val settings = getHabitSettings(habit.id)

            if (settings != null && habit.notificationEnabled) {
                // Рассчитываем время уведомления
                val notificationTime = calculateNotificationTime(habit.time, settings.notificationSettings)
                val habitForNotification = habit.copy(time = notificationTime)

                // Отменяем старое уведомление и планируем новое
                cancelNotification(habit.id)
                notificationManager.scheduleNotification(habitForNotification)

                Log.d("HabitManager", "Уведомление перепланировано для привычки ${habit.id}")
            }
        } catch (e: Exception) {
            Log.e("HabitManager", "Ошибка перепланирования уведомления: ${e.message}")
        }
    }

    /**
     * Отменить уведомление (простая версия)
     */
    private fun cancelNotification(habitId: Int) {
        try {
            notificationManager.cancelNotification(habitId)
        } catch (e: Exception) {
            Log.e("HabitManager", "Ошибка отмены уведомления: ${e.message}")
        }
    }

    /**
     * Сохранение связи между привычкой и ее настройками
     */
    private fun saveHabitSettingsMapping(habitId: Int, settings: HabitSettings) {
        val mappings = getHabitSettingsMappings().toMutableMap()
        mappings[habitId] = settings
        saveHabitSettingsMappings(mappings)
    }

    /**
     * Получение настроек для конкретной привычки
     */
    fun getHabitSettings(habitId: Int): HabitSettings? {
        val mappings = getHabitSettingsMappings()
        return mappings[habitId]
    }

    // ==================== СТАРАЯ СИСТЕМА (для обратной совместимости) ====================

    /**
     * Старый метод добавления привычки (для обратной совместимости)
     */
    /**
     * Старый метод добавления привычки (для обратной совместимости)
     */
    fun addHabit(
        name: String,
        date: Date,
        time: String = "12:00",
        repeatType: RepeatType = RepeatType.ONCE,  // Изменил тип с RepeatTypeOld на RepeatType
        repeatDays: String = "",
        endDate: Date? = null,
        notificationEnabled: Boolean = true
    ): Habit {
        val habit = Habit(
            id = nextId++,
            name = name,
            date = date,
            time = time,
            repeatType = repeatType,  // Теперь совместимый тип
            repeatDays = repeatDays,
            endDate = endDate,
            notificationEnabled = notificationEnabled
        )

        val habits = getAllHabits().toMutableList()
        habits.add(habit)
        saveHabits(habits)
        saveNextId()

        Log.d("HabitManager", "Добавлена привычка: '$name' на ${dateFormat.format(date)} в $time")

        // Запланировать уведомление если включено
        if (notificationEnabled) {
            notificationManager.scheduleNotification(habit)
        }

        return habit
    }

    // ==================== ОБЩИЕ МЕТОДЫ ====================

    fun getAllHabits(): List<Habit> {
        val json = sharedPreferences.getString(habitsKey, "[]") ?: "[]"
        return try {
            val type = object : TypeToken<List<Habit>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e("HabitManager", "Ошибка чтения привычек: ${e.message}")
            emptyList()
        }
    }

    fun getHabitsForDate(targetDate: Date): List<Habit> {
        try {
            Log.d("HabitManager", "=== getHabitsForDate ===")
            Log.d("HabitManager", "Целевая дата: ${dateFormat.format(targetDate)}")

            val allHabits = getAllHabits()
            Log.d("HabitManager", "Всего привычек в базе: ${allHabits.size}")

            val result = mutableListOf<Habit>()

            for (habit in allHabits) {
                try {
                    val isActive = isHabitActiveOnDate(habit, targetDate)
                    Log.d("HabitManager", "Привычка '${habit.name}' (ID: ${habit.id}): активна = $isActive")

                    if (isActive) {
                        val dateStr = dateFormat.format(targetDate)
                        val isCompletedOnDate = habit.completedDates.contains(dateStr)

                        // Создаем копию привычки с ВСЕМИ полями
                        val habitForDate = habit.copy(
                            date = targetDate,
                            isCompleted = isCompletedOnDate,
                            time = habit.time,
                            // Явно указываем все поля, которые могут быть null
                            repeatInterval = habit.repeatInterval,
                            repeatIntervalUnit = habit.repeatIntervalUnit
                        )

                        result.add(habitForDate)
                        Log.d("HabitManager", "✓ Добавлена: ${habit.name} в ${habit.time}")
                    }
                } catch (e: Exception) {
                    Log.e("HabitManager", "Ошибка проверки привычки ${habit.id}: ${e.message}", e)
                }
            }

            Log.d("HabitManager", "Итого найдено: ${result.size} привычек")
            Log.d("HabitManager", "=== Конец getHabitsForDate ===")

            return result.sortedBy { it.time }

        } catch (e: Exception) {
            Log.e("HabitManager", "Ошибка в getHabitsForDate: ${e.message}", e)
            return emptyList()
        }
    }

    /**
     * Отметить привычку выполненной на конкретную дату
     */
    fun completeHabitOnDate(habitId: Int, completionDate: Date = Date()): Boolean {
        val habits = getAllHabits().toMutableList()
        val index = habits.indexOfFirst { it.id == habitId }

        if (index != -1) {
            val habit = habits[index]
            val dateStr = dateFormat.format(completionDate)

            val newCompletedDates = if (habit.completedDates.contains(dateStr)) {
                habit.completedDates
            } else {
                habit.completedDates + dateStr
            }

            habits[index] = habit.copy(
                completedDates = newCompletedDates,
                streak = habit.streak + 1,
                bestStreak = maxOf(habit.bestStreak, habit.streak + 1),
                totalCompletions = habit.totalCompletions + 1,
                updatedAt = System.currentTimeMillis()
            )

            saveHabits(habits)

            // Отменить уведомление на этот день
            if (habit.notificationEnabled) {
                cancelNotificationForDate(habitId, completionDate)
            }

            Log.d("HabitManager", "Привычка $habitId выполнена на $dateStr")
            return true
        }

        return false
    }

    fun isHabitActiveOnDate(habit: Habit, targetDate: Date): Boolean {
        try {
            val habitDateStr = dateFormat.format(habit.date)
            val targetDateStr = dateFormat.format(targetDate)

            Log.d("HabitManager", "Проверка привычки '${habit.name}':")
            Log.d("HabitManager", "  Дата привычки: $habitDateStr")
            Log.d("HabitManager", "  Целевая дата: $targetDateStr")
            Log.d("HabitManager", "  Тип повторения: ${habit.repeatType}")

            val result = when (habit.repeatType) {
                RepeatType.ONCE, RepeatType.NEVER -> {
                    // Один раз - только в указанную дату
                    val isActive = habitDateStr == targetDateStr
                    Log.d("HabitManager", "  ONCE/NEVER: $isActive")
                    isActive
                }

                RepeatType.DAILY -> {
                    // Ежедневно - всегда активна
                    Log.d("HabitManager", "  DAILY: true")
                    true
                }

                RepeatType.WEEKLY -> {
                    // Еженедельно - в тот же день недели
                    val calendarHabit = Calendar.getInstance().apply { time = habit.date }
                    val calendarTarget = Calendar.getInstance().apply { time = targetDate }
                    val isActive = calendarHabit.get(Calendar.DAY_OF_WEEK) == calendarTarget.get(Calendar.DAY_OF_WEEK)
                    Log.d("HabitManager", "  WEEKLY: $isActive (день недели: ${calendarHabit.get(Calendar.DAY_OF_WEEK)} == ${calendarTarget.get(Calendar.DAY_OF_WEEK)})")
                    isActive
                }

                RepeatType.WEEKDAYS -> {
                    // Рабочие дни (пн-пт)
                    val calendar = Calendar.getInstance().apply { time = targetDate }
                    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                    val isActive = dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY
                    Log.d("HabitManager", "  WEEKDAYS: $isActive (день недели: $dayOfWeek)")
                    isActive
                }

                RepeatType.WEEKENDS -> {
                    // Выходные (сб-вс)
                    val calendar = Calendar.getInstance().apply { time = targetDate }
                    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                    val isActive = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
                    Log.d("HabitManager", "  WEEKENDS: $isActive (день недели: $dayOfWeek)")
                    isActive
                }

                RepeatType.MONTHLY -> {
                    // Ежемесячно - в тот же день месяца
                    val calendarHabit = Calendar.getInstance().apply { time = habit.date }
                    val calendarTarget = Calendar.getInstance().apply { time = targetDate }
                    val isActive = calendarHabit.get(Calendar.DAY_OF_MONTH) == calendarTarget.get(Calendar.DAY_OF_MONTH)
                    Log.d("HabitManager", "  MONTHLY: $isActive (день месяца: ${calendarHabit.get(Calendar.DAY_OF_MONTH)} == ${calendarTarget.get(Calendar.DAY_OF_MONTH)})")
                    isActive
                }

                RepeatType.YEARLY -> {
                    // Ежегодно - в тот же день и месяц
                    val calendarHabit = Calendar.getInstance().apply { time = habit.date }
                    val calendarTarget = Calendar.getInstance().apply { time = targetDate }
                    val isActive = calendarHabit.get(Calendar.DAY_OF_MONTH) == calendarTarget.get(Calendar.DAY_OF_MONTH) &&
                            calendarHabit.get(Calendar.MONTH) == calendarTarget.get(Calendar.MONTH)
                    Log.d("HabitManager", "  YEARLY: $isActive")
                    isActive
                }

                RepeatType.CUSTOM_INTERVAL -> {
                    // Пользовательский интервал
                    val isActive = isHabitActiveOnCustomInterval(habit, targetDate)
                    Log.d("HabitManager", "  CUSTOM_INTERVAL: $isActive")
                    isActive
                }

                RepeatType.SPECIFIC_DAYS, RepeatType.CUSTOM_DAYS -> {
                    // Конкретные дни недели
                    val isActive = if (habit.repeatDays.isNotEmpty()) {
                        val calendar = Calendar.getInstance().apply { time = targetDate }
                        val targetDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

                        val daysList = habit.repeatDays.split(",")
                            .mapNotNull { it.trim().toIntOrNull() }

                        val result = daysList.contains(targetDayOfWeek)
                        Log.d("HabitManager", "  CUSTOM_DAYS: $result (дни: $daysList, сегодня: $targetDayOfWeek)")
                        result
                    } else {
                        // Если дни не указаны, проверяем по дате начала
                        val result = habitDateStr == targetDateStr
                        Log.d("HabitManager", "  CUSTOM_DAYS (без дней): $result")
                        result
                    }
                    isActive
                }

                else -> {
                    Log.d("HabitManager", "  Неизвестный тип: ${habit.repeatType}")
                    false
                }
            }

            Log.d("HabitManager", "  ИТОГ: $result")
            return result

        } catch (e: Exception) {
            Log.e("HabitManager", "Ошибка в isHabitActiveOnDate: ${e.message}", e)
            return false
        }
    }

    private fun isHabitActiveOnCustomInterval(habit: Habit, targetDate: Date): Boolean {
        // Получаем настройки привычки
        val settings = getHabitSettings(habit.id)

        return if (settings != null) {
            // Используем настройки для расчета
            val repeatSettings = settings.repeatSettings
            val interval = repeatSettings.interval.toLong() // ПРИВОДИМ К LONG

            when (repeatSettings.intervalUnit) {
                IntervalUnit.DAYS -> {
                    // Проверяем, проходит ли targetDate через interval дней от startDate
                    val daysBetween = daysBetween(repeatSettings.startDate ?: habit.date, targetDate)
                    daysBetween % interval == 0L // СРАВНИВАЕМ С 0L
                }
                IntervalUnit.WEEKS -> {
                    val weeksBetween = daysBetween(repeatSettings.startDate ?: habit.date, targetDate) / 7
                    weeksBetween % interval == 0L // СРАВНИВАЕМ С 0L
                }
                IntervalUnit.MONTHS -> {
                    val monthsBetween = monthsBetween(repeatSettings.startDate ?: habit.date, targetDate)
                    monthsBetween % interval == 0L // СРАВНИВАЕМ С 0L
                }
                IntervalUnit.YEARS -> {
                    val yearsBetween = yearsBetween(repeatSettings.startDate ?: habit.date, targetDate)
                    yearsBetween % interval == 0L // СРАВНИВАЕМ С 0L
                }
            }
        } else {
            // Если настроек нет, проверяем по дате начала
            dateFormat.format(habit.date) == dateFormat.format(targetDate)
        }
    }

    private fun daysBetween(date1: Date, date2: Date): Long {
        val diff = abs(date2.time - date1.time)
        return diff / (1000 * 60 * 60 * 24)
    }

    private fun monthsBetween(date1: Date, date2: Date): Long {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }

        val yearDiff = cal2.get(Calendar.YEAR) - cal1.get(Calendar.YEAR)
        val monthDiff = cal2.get(Calendar.MONTH) - cal1.get(Calendar.MONTH)

        return (yearDiff * 12 + monthDiff).toLong() // ПРИВОДИМ К LONG
    }

    private fun yearsBetween(date1: Date, date2: Date): Long {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }

        return (cal2.get(Calendar.YEAR) - cal1.get(Calendar.YEAR)).toLong() // ПРИВОДИМ К LONG
    }

    fun completeHabit(habitId: Int, completionDate: Date = Date()) {
        val habits = getAllHabits().toMutableList()
        val index = habits.indexOfFirst { it.id == habitId }

        if (index != -1) {
            val habit = habits[index]
            val dateStr = dateFormat.format(completionDate)

            val newCompletedDates = if (habit.completedDates.contains(dateStr)) {
                habit.completedDates
            } else {
                habit.completedDates + dateStr
            }

            habits[index] = habit.copy(
                completedDates = newCompletedDates,
                isCompleted = true,
                streak = habit.streak + 1,
                bestStreak = maxOf(habit.bestStreak, habit.streak + 1),
                totalCompletions = habit.totalCompletions + 1
            )
            saveHabits(habits)

            // Отменить уведомление для выполненной привычки
            if (habit.notificationEnabled) {
                notificationManager.cancelNotification(habitId)
            }

            Log.d("HabitManager", "Привычка $habitId отмечена выполненной на $dateStr")
        }
    }

    fun updateHabitTime(habitId: Int, newTime: String): Boolean {
        val habits = getAllHabits().toMutableList()
        val index = habits.indexOfFirst { it.id == habitId }

        if (index != -1 && DateUtils.isValidTime(newTime)) {
            val habit = habits[index]
            habits[index] = habit.copy(time = newTime)
            saveHabits(habits)

            // Перепланировать уведомление
            if (habit.notificationEnabled) {
                notificationManager.cancelNotification(habitId)
                notificationManager.scheduleNotification(habits[index])
            }

            return true
        }

        return false
    }

    /**
     * Удалить привычку (полностью, со всех дней)
     */
    fun deleteHabit(habitId: Int): Boolean {
        val habits = getAllHabits().toMutableList()
        val habitToDelete = habits.find { it.id == habitId }

        if (habitToDelete != null) {
            habits.removeIf { it.id == habitId }
            saveHabits(habits)

            // Отменить все уведомления для этой привычки
            if (habitToDelete.notificationEnabled) {
                cancelAllNotificationsForHabit(habitId)
            }

            // Удалить настройки
            deleteHabitSettings(habitId)

            Log.d("HabitManager", "Привычка $habitId удалена полностью")
            return true
        }

        return false
    }

    fun deleteHabitCompletion(habitId: Int, date: Date): Boolean {
        val habits = getAllHabits().toMutableList()
        val index = habits.indexOfFirst { it.id == habitId }

        if (index != -1) {
            val habit = habits[index]
            val dateStr = dateFormat.format(date)

            val newCompletedDates = habit.completedDates.filter { it != dateStr }

            habits[index] = habit.copy(
                completedDates = newCompletedDates,
                streak = maxOf(0, habit.streak - 1),
                totalCompletions = maxOf(0, habit.totalCompletions - 1),
                updatedAt = System.currentTimeMillis()
            )

            saveHabits(habits)
            Log.d("HabitManager", "Выполнение привычки $habitId удалено на $dateStr")
            return true
        }

        return false
    }

    fun findHabitById(habitId: Int): Habit? {
        return getAllHabits().find { it.id == habitId }
    }

    fun getTodayHabits(): List<Habit> {
        return getHabitsForDate(Date())
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    private fun saveHabits(habits: List<Habit>) {
        try {
            val json = gson.toJson(habits)
            sharedPreferences.edit().putString(habitsKey, json).apply()
        } catch (e: Exception) {
            Log.e("HabitManager", "Ошибка сохранения привычек: ${e.message}", e)
        }
    }

    private fun saveNextId() {
        sharedPreferences.edit().putInt("nextId", nextId).apply()
    }

    private fun getHabitSettingsMappings(): Map<Int, HabitSettings> {
        val json = sharedPreferences.getString(habitsSettingsKey, "{}") ?: "{}"
        return try {
            val type = object : TypeToken<Map<Int, HabitSettings>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            Log.e("HabitManager", "Ошибка чтения настроек привычек: ${e.message}")
            emptyMap()
        }
    }

    private fun saveHabitSettingsMappings(mappings: Map<Int, HabitSettings>) {
        try {
            val json = gson.toJson(mappings)
            sharedPreferences.edit().putString(habitsSettingsKey, json).apply()
        } catch (e: Exception) {
            Log.e("HabitManager", "Ошибка сохранения настроек привычек: ${e.message}")
        }
    }

    private fun deleteHabitSettings(habitId: Int) {
        val mappings = getHabitSettingsMappings().toMutableMap()
        mappings.remove(habitId)
        saveHabitSettingsMappings(mappings)
    }

    fun getHabitStats(): Map<String, Any> {
        val habits = getAllHabits()
        val total = habits.size
        val completed = habits.count { it.isCompleted }
        val withNotifications = habits.count { it.notificationEnabled }

        // Если в Habit нет isOverdue, убрать или заменить на другую логику
        val overdue = habits.count { habit ->
            // Проверка просроченности
            val today = Date()
            habit.date.before(today) && !habit.isCompleted
        }

        return mapOf(
            "total" to total,
            "completed" to completed,
            "with_notifications" to withNotifications,
            "overdue" to overdue,
            "completion_rate" to if (total > 0) (completed * 100 / total) else 0
        )
    }

    fun getTodayCompletedCount(): Int {
        return getTodayHabits().count { it.isCompleted }
    }

    fun getTodayTotalCount(): Int {
        return getTodayHabits().size
    }

    fun getTodayProgress(): Float {
        val total = getTodayTotalCount()
        val completed = getTodayCompletedCount()
        return if (total > 0) completed.toFloat() / total else 0f
    }

    /**
     * Добавить привычку с поддержкой интервалов
     */
    fun addHabitWithInterval(
        name: String,
        date: Date,
        time: String = "12:00",
        repeatType: RepeatType = RepeatType.ONCE,
        repeatInterval: Int = 1,
        intervalUnit: IntervalUnit = IntervalUnit.DAYS,
        endDate: Date? = null,
        notificationEnabled: Boolean = true,
        reminderMinutes: Int = 0
    ): Habit {

        Log.d("HabitManager", "Добавление привычки с интервалом: $repeatInterval $intervalUnit")

        // Создаем настройки
        val repeatSettings = RepeatSettings(
            repeatType = when {
                repeatInterval > 1 -> RepeatType.CUSTOM_INTERVAL
                else -> repeatType
            },
            startDate = date,
            endDate = endDate,
            interval = repeatInterval,
            intervalUnit = intervalUnit
        )

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

        val habitSettings = HabitSettings(
            name = name,
            repeatSettings = repeatSettings,
            notificationSettings = notificationSettings
        )

        // Используем метод для создания одной привычки
        return addSingleHabitWithRepeatSettings(habitSettings).copy(time = time)
    }

    /**
     * Простая функция для добавления привычки (обратная совместимость)
     */
    fun addSimpleHabit(
        name: String,
        date: Date,
        time: String = "12:00",
        repeatType: RepeatType = RepeatType.ONCE,
        endDate: Date? = null,
        notificationEnabled: Boolean = true
    ): Habit {
        return addHabit(
            name = name,
            date = date,
            time = time,
            repeatType = repeatType,
            endDate = endDate,
            notificationEnabled = notificationEnabled
        )
    }

    fun debugAllHabits() {
        try {
            val allHabits = getAllHabits()
            Log.d("HabitManager", "=== ВСЕ ПРИВЫЧКИ В БАЗЕ ===")
            Log.d("HabitManager", "Общее количество: ${allHabits.size}")

            if (allHabits.isEmpty()) {
                Log.d("HabitManager", "База данных пуста!")
                return
            }

            allHabits.forEachIndexed { index, habit ->
                Log.d("HabitManager", "\n--- Привычка #${index + 1} ---")
                Log.d("HabitManager", "ID: ${habit.id}")
                Log.d("HabitManager", "Название: '${habit.name}'")
                Log.d("HabitManager", "Дата начала: ${dateFormat.format(habit.date)}")
                Log.d("HabitManager", "Время: ${habit.time}")
                Log.d("HabitManager", "Тип повторения: ${habit.repeatType}")
                Log.d("HabitManager", "Дни повторения: ${habit.repeatDays}")
                Log.d("HabitManager", "Дата окончания: ${habit.endDate?.let { dateFormat.format(it) } ?: "нет"}")
                Log.d("HabitManager", "Уведомления: ${habit.notificationEnabled}")
                Log.d("HabitManager", "Выполнена: ${habit.isCompleted}")
                Log.d("HabitManager", "Даты выполнения: ${habit.completedDates}")
            }

            Log.d("HabitManager", "=== КОНЕЦ СПИСКА ПРИВЫЧЕК ===")

        } catch (e: Exception) {
            Log.e("HabitManager", "Ошибка в debugAllHabits: ${e.message}")
        }
    }
}