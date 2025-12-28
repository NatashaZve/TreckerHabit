package com.example.trecker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.trecker.receiver.HabitNotificationReceiver
import android.os.Build
import android.util.Log
import java.util.*

class HabitNotificationManager(private val context: Context) {

    companion object {
        private const val TAG = "HabitNotificationManager"
        private const val REQUEST_CODE_PREFIX = 1000

        // Действия для Intent
        const val ACTION_SHOW_HABIT_REMINDER = "com.example.trecker.SHOW_HABIT_REMINDER"
        const val ACTION_COMPLETE_HABIT = "com.example.trecker.COMPLETE_HABIT"
        const val ACTION_SNOOZE_HABIT = "com.example.trecker.SNOOZE_HABIT"
    }

    private val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Запланировать уведомление для привычки
     */
    fun scheduleNotification(habit: Habit) {
        if (!habit.notificationEnabled) {
            Log.d(TAG, "Уведомления отключены для привычки: ${habit.name}")
            return
        }

        try {
            // 1. Рассчитать время срабатывания
            val triggerTime = calculateTriggerTime(habit)

            Log.d(TAG, "Планирование уведомления для '${habit.name}' на ${Date(triggerTime)}")

            // 2. Если время уже прошло - планируем на завтра
            if (triggerTime <= System.currentTimeMillis()) {
                Log.d(TAG, "Время уже прошло, планирую на завтра")
                scheduleForTomorrow(habit)
                return
            }

            // 3. Создать Intent
            val intent = createNotificationIntent(habit)

            // 4. Создать PendingIntent
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_PREFIX + habit.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 5. Запланировать через AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ с проверкой разрешения
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    // Fallback для старых версий или без разрешения
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 6.0+
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                // Android < 6.0
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }

            Log.d(TAG, "✅ Уведомление запланировано: '${habit.name}' в ${habit.time}")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка планирования уведомления: ${e.message}", e)
        }
    }

    /**
     * Создать Intent для уведомления
     */
    private fun createNotificationIntent(habit: Habit): Intent {
        val intent = Intent(context, HabitNotificationReceiver::class.java)

        // Устанавливаем action
        intent.action = ACTION_SHOW_HABIT_REMINDER

        // Добавляем данные о привычке
        intent.putExtra("habit_id", habit.id)
        intent.putExtra("habit_name", habit.name)
        intent.putExtra("notification_id", habit.notificationId)
        intent.putExtra("time", habit.time)

        // Проверяем, просрочена ли привычка
        val isOverdue = try {
            DateUtils.isTimePassed(habit.date, habit.time) && !habit.isCompleted
        } catch (e: Exception) {
            false
        }
        intent.putExtra("is_overdue", isOverdue)

        // Определяем канал для уведомления
        val channel = if (isOverdue) {
            NotificationHelper.CHANNEL_URGENT_ID
        } else {
            NotificationHelper.CHANNEL_REMINDERS_ID
        }
        intent.putExtra("channel_id", channel)

        return intent
    }

    /**
     * Рассчитать время срабатывания уведомления
     */
    private fun calculateTriggerTime(habit: Habit): Long {
        val calendar = Calendar.getInstance().apply {
            time = habit.date

            // Установить время из строки "HH:mm"
            val parts = habit.time.split(":")
            if (parts.size == 2) {
                set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                set(Calendar.MINUTE, parts[1].toInt())
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Если время уже прошло сегодня, планируем на завтра
            if (timeInMillis <= System.currentTimeMillis() &&
                DateUtils.isSameDay(habit.date, Date())) {
                add(Calendar.DAY_OF_MONTH, 1)
                Log.d(TAG, "Время прошло, переношу на завтра")
            }
        }

        return calendar.timeInMillis
    }

    /**
     * Запланировать на завтра
     */
    private fun scheduleForTomorrow(habit: Habit) {
        try {
            val calendar = Calendar.getInstance().apply {
                time = habit.date
                add(Calendar.DAY_OF_MONTH, 1)
            }

            val tomorrowHabit = habit.copy(date = calendar.time)
            scheduleNotification(tomorrowHabit)

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка переноса уведомления: ${e.message}", e)
        }
    }

    /**
     * Отменить уведомление для привычки
     */
    fun cancelNotification(habitId: Int) {
        try {
            val intent = Intent(context, HabitNotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_PREFIX + habitId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()

            Log.d(TAG, "Уведомление отменено для habitId: $habitId")

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка отмены уведомления: ${e.message}", e)
        }
    }

    /**
     * Отменить все уведомления
     */
    fun cancelAllNotifications() {
        Log.d(TAG, "Отмена всех уведомлений - требуется ручная отмена по habitId")
        // Здесь можно добавить логику для отмены всех уведомлений
        // Но обычно лучше отменять по конкретным ID
    }

    /**
     * Перепланировать все уведомления
     */
    fun rescheduleAllNotifications(habits: List<Habit>) {
        try {
            habits.forEach { habit ->
                cancelNotification(habit.id)
            }

            habits.filter { it.notificationEnabled }.forEach { habit ->
                scheduleNotification(habit)
            }

            Log.d(TAG, "Перепланировано ${habits.size} уведомлений")

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка перепланирования: ${e.message}", e)
        }
    }

    /**
     * Отложить уведомление на 10 минут
     */
    fun snoozeNotification(habit: Habit) {
        try {
            // Отменяем текущее уведомление
            cancelNotification(habit.id)

            // Планируем на 10 минут позже
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                add(Calendar.MINUTE, 10)
            }

            val snoozedHabit = habit.copy(
                date = calendar.time,
                snoozeCount = habit.snoozeCount + 1
            )

            scheduleNotification(snoozedHabit)

            Log.d(TAG, "Уведомление отложено на 10 минут для: ${habit.name}")

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка откладывания уведомления: ${e.message}", e)
        }
    }
}