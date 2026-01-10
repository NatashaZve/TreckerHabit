package com.example.trecker

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trecker.databinding.ActivityMainBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var habitManager: HabitManager
    private lateinit var habitAdapter: HabitAdapter

    private lateinit var motivationManager: MotivationManager
    private var currentDate: Date = Date()
    private val dateFormatter = SimpleDateFormat("d MMMM yyyy", Locale("ru"))

    // Для запроса разрешений (Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        handleNotificationPermissionResult(isGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MainActivity", "=== СОЗДАНИЕ АКТИВНОСТИ ===")

        try {
            // ========== 1. НАСТРОЙКА УВЕДОМЛЕНИЙ (самое первое!) ==========
            setupNotifications()

            // Инициализируем мотивационный менеджер
            motivationManager = MotivationManager(this)

            // Создаем каналы для мотивационных уведомлений
            NotificationHelper.createMotivationChannels(this)

            // Планируем мотивационные уведомления (если включены)
            if (motivationManager.areMotivationsEnabled()) {
                motivationManager.scheduleDailyMotivations()
            }

            // ========== 2. ОСНОВНОЙ КОД ==========
            enableEdgeToEdge()

            // Инициализируем ViewBinding
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Инициализируем HabitManager
            habitManager = HabitManager(this)

            // Настраиваем UI
            setupDateDisplay()
            setupDateNavigation()
            setupHabitsRecyclerView()
            setupSystemBars()
            setupMultiDateButton()

            // ========== 3. ОБРАБОТКА ВХОДЯЩИХ УВЕДОМЛЕНИЙ ==========
            handleIncomingNotification(intent)

            Log.d("MainActivity", "Приложение успешно создано")

        } catch (e: Exception) {
            Log.e("MainActivity", "Ошибка при создании: ${e.message}", e)
            Toast.makeText(this, "Ошибка запуска: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }

        binding.statisticsButton.setOnClickListener {
            val intent = Intent(this, StatisticsActivity::class.java)
            startActivity(intent)
        }
    }

    // ==================== МЕТОДЫ ДЛЯ УВЕДОМЛЕНИЙ ====================

    /**
     * Настройка системы уведомлений при запуске приложения
     */
    private fun setupNotifications() {
        // 1. Создаем каналы уведомлений
        createNotificationChannels()

        // 2. Проверяем и запрашиваем разрешения для Android 13+
        checkNotificationPermission()

        // 3. Перепланируем уведомления после обновления приложения
        rescheduleNotificationsIfNeeded()
    }

    /**
     * Создает каналы уведомлений для разных типов уведомлений
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("MainActivity", "Создание всех каналов уведомлений...")

            try {
                // Используйте этот метод для создания всех каналов
                NotificationHelper.createAllChannels(this)

                // Проверяем создание
                if (NotificationHelper.isReminderChannelCreated(this, NotificationHelper.CHANNEL_REMINDERS_ID)) {
                    Log.d("MainActivity", "✅ Каналы уведомлений успешно созданы")
                } else {
                    Log.w("MainActivity", "⚠️ Каналы уведомлений не созданы")
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "Ошибка создания каналов: ${e.message}", e)
            }
        } else {
            Log.d("MainActivity", "Каналы не требуются (API < 26)")
        }
    }

    /**
     * Проверяет и запрашивает разрешение на уведомления для Android 13+
     */
    private fun checkNotificationPermission() {
        // Только для Android 13 (API 33) и выше
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d("MainActivity", "Проверка разрешения на уведомления...")

            val permission = Manifest.permission.POST_NOTIFICATIONS

            when {
                // Разрешение уже предоставлено
                ContextCompat.checkSelfPermission(this, permission) ==
                        PackageManager.PERMISSION_GRANTED -> {
                    Log.d("MainActivity", "Разрешение на уведомления уже предоставлено")
                    onNotificationPermissionGranted()
                }

                // Нужно показать объяснение (пользователь уже отказывал)
                shouldShowRequestPermissionRationale(permission) -> {
                    Log.d("MainActivity", "Показываем объяснение для разрешения")
                    showPermissionExplanationDialog()
                }

                // Первый запрос или "больше не спрашивать"
                else -> {
                    Log.d("MainActivity", "Запрашиваем разрешение впервые")
                    requestPermissionLauncher.launch(permission)
                }
            }
        } else {
            // Для Android < 13 разрешение не требуется
            Log.d("MainActivity", "Разрешение не требуется (API < 33)")
            onNotificationPermissionGranted()
        }
    }

    /**
     * Обработка результата запроса разрешения
     */
    private fun handleNotificationPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            Log.d("MainActivity", "Пользователь разрешил уведомления")
            onNotificationPermissionGranted()

            Toast.makeText(
                this,
                "Уведомления включены ✅",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Log.d("MainActivity", "Пользователь отказал в уведомлениях")
            onNotificationPermissionDenied()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Проверяем, выбрал ли пользователь "Больше не спрашивать"
                val shouldShowRationale = shouldShowRequestPermissionRationale(
                    Manifest.permission.POST_NOTIFICATIONS
                )

                if (!shouldShowRationale) {
                    // Пользователь выбрал "Больше не спрашивать"
                    showEnableNotificationsGuide()
                } else {
                    // Просто отказал, можно будет спросить снова
                    Toast.makeText(
                        this,
                        "Уведомления отключены. Можно включить в настройках.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * Действия при предоставлении разрешения
     */
    private fun onNotificationPermissionGranted() {
        // 1. Включаем уведомления в настройках приложения
        enableNotificationsInSettings(true)

        // 2. Перепланируем все уведомления
        rescheduleAllNotifications()

        // 3. Обновляем UI (если есть переключатель уведомлений)
        updateNotificationUI(true)
    }

    /**
     * Действия при отказе в разрешении
     */
    private fun onNotificationPermissionDenied() {
        // 1. Отключаем уведомления в настройках
        enableNotificationsInSettings(false)

        // 2. Отменяем все запланированные уведомления
        cancelAllScheduledNotifications()

        // 3. Обновляем UI
        updateNotificationUI(false)
    }

    /**
     * Диалог объяснения необходимости уведомлений
     */
    private fun showPermissionExplanationDialog() {
        AlertDialog.Builder(this)
            .setTitle("🔔 Уведомления нужны для напоминаний")
            .setMessage("""
                Приложение использует уведомления для:
                
                • Напоминаний о времени выполнения привычек
                • Ежедневных отчетов о вашем прогрессе
                • Мотивации и достижений
                
                Без разрешения вы не получите напоминаний.
                """.trimIndent())
            .setPositiveButton("Разрешить") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton("Позже") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(
                    this,
                    "Вы можете включить уведомления позже в настройках",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setIcon(android.R.drawable.ic_dialog_info)
            .show()
    }

    /**
     * Показывает инструкцию по включению уведомлений в настройках
     */
    private fun showEnableNotificationsGuide() {
        AlertDialog.Builder(this)
            .setTitle("Как включить уведомления позже")
            .setMessage("""
                1. Откройте Настройки телефона
                2. Перейдите в "Приложения" → "Трекер привычек"
                3. Выберите "Уведомления"
                4. Включите уведомления
                
                Или нажмите "Открыть настройки" для быстрого перехода.
                """.trimIndent())
            .setPositiveButton("Открыть настройки") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Понятно") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Открывает настройки приложения
     */
    private fun openAppSettings() {
        val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
        }

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            // Альтернативный способ для старых устройств
            val fallbackIntent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:$packageName")
            }
            startActivity(fallbackIntent)
        }
    }

    /**
     * Перепланирует уведомления после обновления приложения
     */
    private fun rescheduleNotificationsIfNeeded() {
        // Проверяем, нужно ли перепланировать уведомления
        // (например, после обновления приложения или перезагрузки)
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val lastVersion = prefs.getInt("last_version_code", 0)
        val currentVersion = packageManager.getPackageInfo(packageName, 0).versionCode

        if (lastVersion != currentVersion) {
            Log.d("MainActivity", "Обновление приложения, перепланируем уведомления")
            rescheduleAllNotifications()

            // Сохраняем текущую версию
            prefs.edit().putInt("last_version_code", currentVersion).apply()
        }
    }

    /**
     * Перепланирует все уведомления для активных привычек
     */
    private fun rescheduleAllNotifications() {
        try {
            val habits = habitManager.getAllHabits()
                .filter { it.notificationEnabled }

            if (habits.isNotEmpty()) {
                Log.d("MainActivity", "Перепланирование ${habits.size} уведомлений...")

                // Здесь будет вызов HabitNotificationManager
                // notificationManager.rescheduleAllNotifications(habits)

                Toast.makeText(
                    this,
                    "Уведомления запланированы для ${habits.size} привычек",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // Перепланируем мотивационные уведомления
            if (motivationManager.areMotivationsEnabled()) {
                motivationManager.scheduleDailyMotivations()
            }

        } catch (e: Exception) {
            Log.e("MainActivity", "Ошибка перепланирования уведомлений: ${e.message}")
        }
    }

    /**
     * Отменяет все запланированные уведомления
     */
    private fun cancelAllScheduledNotifications() {
        try {
            Log.d("MainActivity", "Отмена всех запланированных уведомлений")

            // Здесь будет вызов HabitNotificationManager
            // notificationManager.cancelAllNotifications()

        } catch (e: Exception) {
            Log.e("MainActivity", "Ошибка отмены уведомлений: ${e.message}")
        }
    }

    /**
     * Включает/отключает уведомления в настройках приложения
     */
    private fun enableNotificationsInSettings(enabled: Boolean) {
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()

        Log.d("MainActivity", "Уведомления в настройках: ${if (enabled) "ВКЛ" else "ВЫКЛ"}")
    }

    /**
     * Проверяет, включены ли уведомления в настройках
     */
    private fun areNotificationsEnabledInSettings(): Boolean {
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        return prefs.getBoolean("notifications_enabled", true)
    }

    /**
     * Обновляет UI в зависимости от состояния уведомлений
     */
    private fun updateNotificationUI(enabled: Boolean) {
        // Здесь можно обновить иконки, текст и т.д.
        // Например, показать/скрыть переключатель уведомлений

        runOnUiThread {
            // Обновление UI, если нужно
            if (enabled) {
                // Можно показать значок, что уведомления включены
                // binding.notificationStatusIcon.setImageResource(R.drawable.ic_notifications_on)
            } else {
                // binding.notificationStatusIcon.setImageResource(R.drawable.ic_notifications_off)
            }
        }
    }

    /**
     * Обрабатывает входящие уведомления (если приложение было открыто по уведомлению)
     */
    private fun handleIncomingNotification(intent: Intent?) {
        val fromNotification = intent?.getBooleanExtra("from_notification", false) ?: false

        if (fromNotification) {
            Log.d("MainActivity", "Приложение открыто из уведомления")

            val habitId = intent.getIntExtra("habit_id", -1)
            val notificationType = intent.getStringExtra("notification_type")

            if (habitId != -1) {
                // Показать конкретную привычку или выполнить действие
                showHabitFromNotification(habitId)
            }

            // Показать тост
            Toast.makeText(
                this,
                "Напоминание о привычке",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Показывает привычку, на которую пришло уведомление
     */
    private fun showHabitFromNotification(habitId: Int) {
        val habit = habitManager.findHabitById(habitId)
        habit?.let {
            // Можно выделить привычку в списке или показать диалог
            AlertDialog.Builder(this)
                .setTitle("⏰ Напоминание")
                .setMessage("Время выполнить привычку: \"${it.name}\"")
                .setPositiveButton("Выполнено") { dialog, _ ->
                    habitManager.completeHabit(it.id)
                    loadHabitsForDate()
                    dialog.dismiss()
                }
                .setNegativeButton("Позже") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    // ==================== СУЩЕСТВУЮЩИЕ МЕТОДЫ (с небольшими улучшениями) ====================

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

                    // Перепланировать уведомление при изменении времени
                    habit?.let {
                        if (it.notificationEnabled) {
                            // notificationManager.rescheduleNotification(it.copy(time = newTime))
                        }
                    }
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

                    // Можно отменить уведомление для выполненной привычки
                    habitManager.findHabitById(habitId)?.let { habit ->
                        if (habit.notificationEnabled) {
                            // notificationManager.cancelNotification(habitId)
                        }
                    }
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

    /**
     * Включает/отключает уведомления для конкретной привычки
     */
    private fun toggleHabitNotification(habitId: Int, enabled: Boolean) {
        try {
            habitManager.toggleNotification(habitId, enabled)
            loadHabitsForDate()

            val message = if (enabled) {
                "Уведомления включены для привычки"
            } else {
                "Уведомления отключены для привычки"
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e("MainActivity", "Ошибка переключения уведомлений: ${e.message}")
            Toast.makeText(this, "Ошибка изменения уведомлений", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmationDialog(habitId: Int) {
        AlertDialog.Builder(this)
            .setTitle("Удаление привычки")
            .setMessage("Вы уверены, что хотите удалить эту привычку?")
            .setPositiveButton("Удалить") { dialog, _ ->
                // Отменить уведомление перед удалением
                habitManager.findHabitById(habitId)?.let {
                    if (it.notificationEnabled) {
                        // notificationManager.cancelNotification(habitId)
                    }
                }

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

    /**
     * Открывает настройки уведомлений приложения
     */
    private fun openNotificationSettings() {
        try {
            // Прямой переход к настройкам уведомлений приложения
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
                }
                startActivity(intent)
            } else {
                openAppSettings()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Ошибка открытия настроек уведомлений: ${e.message}")
            Toast.makeText(this, "Не удалось открыть настройки", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Проверяет, можно ли отправлять уведомления (разрешение + настройки)
     */
    private fun canSendNotifications(): Boolean {
        // Проверяем системное разрешение (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                return false
            }
        }

        // Проверяем настройки канала (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_REMINDERS_ID)

            if (channel?.importance == NotificationManager.IMPORTANCE_NONE) {
                return false // Пользователь отключил канал
            }
        }

        // Проверяем настройки приложения
        if (!areNotificationsEnabledInSettings()) {
            return false
        }

        return true
    }

    /**
     * Показывает текущий статус уведомлений
     */
    private fun showNotificationStatus() {
        val canSend = canSendNotifications()
        val statusMessage = if (canSend) {
            "✅ Уведомления активны\nВы будете получать напоминания о привычках"
        } else {
            "🔕 Уведомления отключены\nВключите их для получения напоминаний"
        }

        AlertDialog.Builder(this)
            .setTitle("Статус уведомлений")
            .setMessage(statusMessage)
            .setPositiveButton("Настройки") { _, _ ->
                openNotificationSettings()
            }
            .setNegativeButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()

        // При возвращении в приложение обновляем данные
        setupDateDisplay()

        // Проверяем статус уведомлений (могли измениться в настройках системы)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            checkNotificationChannelStatus()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            val hasPermission = ContextCompat.checkSelfPermission(this, permission) ==
                    PackageManager.PERMISSION_GRANTED

            Log.d("MainActivity", "Разрешение на уведомления: $hasPermission")

            if (!hasPermission) {
                Log.w("MainActivity", "⚠️ Уведомления не будут работать без разрешения!")
                // Можно показать предупреждение пользователю
            }
        }
    }

    /**
     * Проверяет статус каналов уведомлений
     */
    private fun checkNotificationChannelStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_REMINDERS_ID)


            channel?.let {
                if (it.importance == NotificationManager.IMPORTANCE_NONE) {
                    // Пользователь отключил уведомления в настройках системы
                    Log.d("MainActivity", "Пользователь отключил уведомления в настройках системы")
                    updateNotificationUI(false)
                }

            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        // Обрабатываем уведомления, если приложение уже было открыто
        handleIncomingNotification(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "=== АКТИВНОСТЬ ЗАКРЫТА ===")
    }

    private fun setupMotivationSwitch() {
        val motivationSwitch = findViewById<SwitchMaterial>(R.id.motivationSwitch)
        motivationSwitch.isChecked = motivationManager.areMotivationsEnabled()

        motivationSwitch.setOnCheckedChangeListener { _, isChecked ->
            motivationManager.setMotivationsEnabled(isChecked)
            Toast.makeText(
                this,
                if (isChecked) "Мотивационные уведомления включены"
                else "Мотивационные уведомления выключены",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}