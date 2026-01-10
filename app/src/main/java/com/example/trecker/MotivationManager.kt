package com.example.trecker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.trecker.receiver.MotivationNotificationReceiver
import java.util.*
import kotlin.random.Random

// Enum вынесен перед классом для простого импорта
enum class MotivationType {
    MORNING_MOTIVATION,    // Утренняя мотивация
    DAY_PROGRESS,          // Прогресс за день
    EVENING_REVIEW,        // Вечерний обзор
    STREAK_CELEBRATION,    // Празднование серии
    RANDOM_ENCOURAGEMENT,  // Случайное поощрение
    HABIT_SPECIFIC         // Специфичное для привычки
}

class MotivationManager(private val context: Context) {

    companion object {
        private const val TAG = "MotivationManager"
        private const val REQUEST_CODE_PREFIX = 2000

        // Каналы для мотивационных уведомлений
        const val CHANNEL_MOTIVATION_ID = "motivation_channel"
        const val CHANNEL_INSPIRATION_ID = "inspiration_channel"
    }

    private val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val random = Random
    // НЕ создаем HabitManager здесь, чтобы избежать циклической зависимости

    /**
     * Запланировать все мотивационные уведомления на день
     */
    fun scheduleDailyMotivations() {
        try {
            // Отменяем старые уведомления
            cancelAllMotivations()

            // Планируем различные типы уведомлений
            scheduleMorningMotivation()
            scheduleDayProgressChecks()
            scheduleEveningReview()
            scheduleRandomEncouragements()
            scheduleStreakCelebrations()

            Log.d(TAG, "✅ Все мотивационные уведомления запланированы")

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка планирования мотиваций: ${e.message}", e)
        }
    }

    /**
     * Утренняя мотивация (8:00)
     */
    private fun scheduleMorningMotivation() {
        try {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, 8)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)

                // Если время уже прошло, планируем на завтра
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            val intent = Intent(context, MotivationNotificationReceiver::class.java).apply {
                action = "MOTIVATION_MORNING"
                putExtra("motivation_type", MotivationType.MORNING_MOTIVATION.name)
                putExtra("title", "🌅 Доброе утро!")
            }

            scheduleMotivation(calendar.timeInMillis, intent, 1)

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка утренней мотивации: ${e.message}")
        }
    }

    /**
     * Проверка прогресса в течение дня (12:00, 15:00, 18:00)
     */
    private fun scheduleDayProgressChecks() {
        val times = listOf(12, 15, 18) // Часы для проверки прогресса

        times.forEachIndexed { index, hour ->
            try {
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = System.currentTimeMillis()
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, 30)
                    set(Calendar.SECOND, 0)

                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.DAY_OF_MONTH, 1)
                    }
                }

                val intent = Intent(context, MotivationNotificationReceiver::class.java).apply {
                    action = "MOTIVATION_PROGRESS"
                    putExtra("motivation_type", MotivationType.DAY_PROGRESS.name)
                    putExtra("title", "📊 Проверка прогресса")
                    putExtra("hour", hour)
                }

                scheduleMotivation(calendar.timeInMillis, intent, 2 + index)

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка планирования прогресса $hour:00: ${e.message}")
            }
        }
    }

    /**
     * Вечерний обзор (21:00)
     */
    private fun scheduleEveningReview() {
        try {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, 21)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)

                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            val intent = Intent(context, MotivationNotificationReceiver::class.java).apply {
                action = "MOTIVATION_EVENING"
                putExtra("motivation_type", MotivationType.EVENING_REVIEW.name)
                putExtra("title", "🌙 Вечерний обзор")
            }

            scheduleMotivation(calendar.timeInMillis, intent, 5)

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка вечернего обзора: ${e.message}")
        }
    }

    /**
     * Случайные поощрения (3 раза в день в случайное время)
     */
    private fun scheduleRandomEncouragements() {
        for (i in 0..2) {
            try {
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = System.currentTimeMillis()

                    // Случайное время между 9:00 и 20:00
                    val randomHour = (9..20).random()
                    val randomMinute = (0..59).random()

                    set(Calendar.HOUR_OF_DAY, randomHour)
                    set(Calendar.MINUTE, randomMinute)
                    set(Calendar.SECOND, 0)

                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.DAY_OF_MONTH, 1)
                    }
                }

                val intent = Intent(context, MotivationNotificationReceiver::class.java).apply {
                    action = "MOTIVATION_RANDOM"
                    putExtra("motivation_type", MotivationType.RANDOM_ENCOURAGEMENT.name)
                    putExtra("title", "💪 Поддержка!")
                }

                scheduleMotivation(calendar.timeInMillis, intent, 6 + i)

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка случайного поощрения: ${e.message}")
            }
        }
    }

    /**
     * Празднование серий (если есть активные серии)
     */
    private fun scheduleStreakCelebrations() {
        try {
            // Создаем HabitManager только когда нужно
            val habitManager = HabitManager(context)
            val habits = habitManager.getAllHabits()
            val habitsWithStreak = habits.filter { it.streak >= 3 } // Серия от 3 дней

            if (habitsWithStreak.isNotEmpty()) {
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = System.currentTimeMillis()
                    set(Calendar.HOUR_OF_DAY, 19)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)

                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.DAY_OF_MONTH, 1)
                    }
                }

                val intent = Intent(context, MotivationNotificationReceiver::class.java).apply {
                    action = "MOTIVATION_STREAK"
                    putExtra("motivation_type", MotivationType.STREAK_CELEBRATION.name)
                    putExtra("title", "🏆 Отличная серия!")
                    putExtra("habits_count", habitsWithStreak.size)
                }

                scheduleMotivation(calendar.timeInMillis, intent, 9)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка празднования серии: ${e.message}")
        }
    }

    /**
     * Уведомление для конкретной привычки
     */
    fun scheduleHabitSpecificMotivation(habit: Habit) {
        try {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()

                // За 30 минут до времени привычки
                val habitTime = habit.time.split(":")
                var hour = habitTime[0].toInt()
                var minute = habitTime[1].toInt() - 30

                if (minute < 0) {
                    hour -= 1
                    minute += 60
                }

                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)

                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            val intent = Intent(context, MotivationNotificationReceiver::class.java).apply {
                action = "MOTIVATION_HABIT_SPECIFIC"
                putExtra("motivation_type", MotivationType.HABIT_SPECIFIC.name)
                putExtra("title", "⏰ Напоминание о привычке")
                putExtra("habit_name", habit.name)
                putExtra("habit_id", habit.id)
            }

            scheduleMotivation(calendar.timeInMillis, intent, 1000 + habit.id)

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка специфичной мотивации: ${e.message}")
        }
    }

    /**
     * Общая функция планирования
     */
    private fun scheduleMotivation(triggerTime: Long, intent: Intent, requestCode: Int) {
        try {
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_PREFIX + requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }

            Log.d(TAG, "Мотивация запланирована на ${Date(triggerTime)}")

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка планирования: ${e.message}")
        }
    }

    /**
     * Отменить все мотивационные уведомления
     */
    fun cancelAllMotivations() {
        try {
            // Отменяем уведомления с кодами от 2000 до 3000
            for (i in 0..1000) {
                val intent = Intent(context, MotivationNotificationReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE_PREFIX + i,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingIntent)
            }

            Log.d(TAG, "Все мотивационные уведомления отменены")

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка отмены мотиваций: ${e.message}")
        }
    }

    /**
     * Получить мотивационное сообщение по типу
     */
    fun getMotivationMessage(type: MotivationType, habitName: String? = null): Pair<String, String> {
        val messages = when (type) {
            MotivationType.MORNING_MOTIVATION -> listOf(
                Pair("🌅 Доброе утро, чемпион!", "Сегодня отличный день для новых достижений! Начни свой день с маленькой победы."),
                Pair("☀️ Новый день - новые возможности!", "Каждая выполненная привычка делает тебя сильнее. Ты справишься!"),
                Pair("🚀 Время действовать!", "Сегодня ты станешь на шаг ближе к своей цели. Верь в себя!"),
                Pair("💫 Твой день начинается!", "Вспомни свои цели. Сегодня идеальный день для прогресса.")
            )

            MotivationType.DAY_PROGRESS -> listOf(
                Pair("📊 Как твой прогресс?", "Не забывай про свои привычки. Каждый маленький шаг ведет к большой цели!"),
                Pair("⏰ Проверка времени!", "Как дела с привычками сегодня? Помни, последовательность - ключ к успеху."),
                Pair("🎯 Не сбавляй темп!", "Ты уже прошел часть пути. Продолжай в том же духе!"),
                Pair("🔋 Заряд мотивации!", "Ты можешь больше, чем думаешь. Продолжай двигаться вперед!")
            )

            MotivationType.EVENING_REVIEW -> listOf(
                Pair("🌙 Вечерний обзор", "Посмотри на свой день. Гордись своими победами, даже самыми маленькими!"),
                Pair("⭐️ Ты молодец!", "Каждый день важен. Сегодня ты стал лучше, чем вчера."),
                Pair("📝 Подведем итоги", "Что получилось сегодня? Завтра будет еще лучше!"),
                Pair("🎊 Завершаем день с улыбкой", "Ты заслужил отдых. Завтра новый день для побед!")
            )

            MotivationType.STREAK_CELEBRATION -> listOf(
                Pair("🏆 Отличная серия!", "Ты держишь серию несколько дней подряд! Это впечатляет!"),
                Pair("🔥 Ты в ударе!", "Продолжающаяся серия - доказательство твоей силы воли. Так держать!"),
                Pair("💎 Невероятная последовательность!", "Твоя дисциплина восхищает. Продолжай в том же духе!"),
                Pair("🌟 Серия чемпиона!", "Каждый день ты становишься сильнее. Гордись своими достижениями!")
            )

            MotivationType.RANDOM_ENCOURAGEMENT -> listOf(
                Pair("💪 Ты можешь все!", "Помни: самая трудная часть - это начать. А ты уже начал!"),
                Pair("✨ Маленькие шаги - большие результаты", "Каждая привычка делает тебя лучше. Не сдавайся!"),
                Pair("🚀 Ты на правильном пути!", "Твои усилия не напрасны. Каждый день имеет значение."),
                Pair("🎯 Фокус на цели!", "Представь себе того, кем станешь благодаря своим привычкам."),
                Pair("🌈 У тебя все получится!", "Трудности временны, а результаты останутся навсегда."),
                Pair("⚡️ Заряд энергии!", "Ты сильнее, чем думаешь. Продолжай двигаться вперед!")
            )

            MotivationType.HABIT_SPECIFIC -> {
                val name = habitName ?: "привычка"
                listOf(
                    Pair("⏰ Время для \"$name\"!", "Не откладывай на потом. Сделай это сейчас и почувствуй удовлетворение!"),
                    Pair("🎯 Напоминание: \"$name\"", "Этот маленький шаг приблизит тебя к большой цели. Ты справишься!"),
                    Pair("💫 Пора выполнить \"$name\"", "Помни, зачем ты начал. Этот момент определяет твой успех!"),
                    Pair("🚀 \"$name\" ждет тебя!", "Сделай это ради того, кем станешь. Ты заслуживаешь этого результата!")
                )
            }
        }

        return messages.random()
    }

    /**
     * Включить/выключить мотивационные уведомления
     */
    fun setMotivationsEnabled(enabled: Boolean) {
        val prefs = context.getSharedPreferences("motivation_settings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("motivations_enabled", enabled).apply()

        if (enabled) {
            scheduleDailyMotivations()
            Log.d(TAG, "Мотивационные уведомления включены")
        } else {
            cancelAllMotivations()
            Log.d(TAG, "Мотивационные уведомления отключены")
        }
    }

    /**
     * Проверить, включены ли мотивационные уведомления
     */
    fun areMotivationsEnabled(): Boolean {
        val prefs = context.getSharedPreferences("motivation_settings", Context.MODE_PRIVATE)
        return prefs.getBoolean("motivations_enabled", true)
    }
}