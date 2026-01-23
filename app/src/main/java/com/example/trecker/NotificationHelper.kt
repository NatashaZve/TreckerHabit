package com.example.trecker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.Log

object NotificationHelper {

    // ==================== КАНАЛЫ ДЛЯ ПРИВЫЧЕК ====================

    // Идентификаторы каналов для привычек
    const val CHANNEL_REMINDERS_ID = "habit_reminders"
    const val CHANNEL_URGENT_ID = "habit_urgent"

    private const val CHANNEL_REMINDERS_NAME = "Напоминания о привычках"
    private const val CHANNEL_URGENT_NAME = "Срочные напоминания"

    // ==================== КАНАЛЫ ДЛЯ МОТИВАЦИИ ====================

    // Идентификаторы каналов для мотивации (совпадают с MotivationManager)
    const val CHANNEL_MOTIVATION_ID = "motivation_channel"
    const val CHANNEL_INSPIRATION_ID = "inspiration_channel"

    private const val CHANNEL_MOTIVATION_NAME = "Мотивация и поддержка"
    private const val CHANNEL_INSPIRATION_NAME = "Вдохновение"

    // ==================== МЕТОДЫ ДЛЯ ПРИВЫЧЕК ====================

    /**
     * Создать каналы для уведомлений о привычках
     */
    fun createReminderChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("NotificationHelper", "Создание каналов напоминаний...")

            try {
                // Канал для обычных напоминаний
                val remindersChannel = NotificationChannel(
                    CHANNEL_REMINDERS_ID,
                    CHANNEL_REMINDERS_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Напоминания о времени выполнения привычек"
                    enableLights(true)
                    lightColor = Color.RED
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500)

                    // Видимость на заблокированном экране
                    lockscreenVisibility = NotificationManager.IMPORTANCE_HIGH
                }

                // Канал для срочных напоминаний
                val urgentChannel = NotificationChannel(
                    CHANNEL_URGENT_ID,
                    CHANNEL_URGENT_NAME,
                    NotificationManager.IMPORTANCE_MAX
                ).apply {
                    description = "Срочные напоминания о пропущенных привычках"
                    enableLights(true)
                    lightColor = Color.RED
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 1000, 200, 1000, 200, 1000)
                    lockscreenVisibility = NotificationManager.IMPORTANCE_MAX
                }

                val notificationManager = context.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

                notificationManager.createNotificationChannel(remindersChannel)
                notificationManager.createNotificationChannel(urgentChannel)

                Log.d("NotificationHelper", "✅ Каналы напоминаний созданы")

            } catch (e: Exception) {
                Log.e("NotificationHelper", "Ошибка создания каналов напоминаний: ${e.message}", e)
            }
        } else {
            Log.d("NotificationHelper", "Каналы не требуются (API < 26)")
        }
    }

    /**
     * Проверить, создан ли канал для привычек
     */
    fun isReminderChannelCreated(context: Context, channelId: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            return notificationManager.getNotificationChannel(channelId) != null
        }
        return true // Для версий ниже 8.0 каналы не используются
    }

    // ==================== МЕТОДЫ ДЛЯ МОТИВАЦИИ ====================

    /**
     * Создать каналы для мотивационных уведомлений
     */
    fun createMotivationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("NotificationHelper", "Создание каналов мотивации...")

            try {
                // Канал для мотивационных уведомлений
                val motivationChannel = NotificationChannel(
                    CHANNEL_MOTIVATION_ID,
                    CHANNEL_MOTIVATION_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Мотивационные сообщения и поддержка в течение дня"
                    enableLights(true)
                    lightColor = Color.BLUE
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 250, 200, 250)
                    lockscreenVisibility = NotificationManager.IMPORTANCE_HIGH
                }

                // Канал для вдохновляющих уведомлений
                val inspirationChannel = NotificationChannel(
                    CHANNEL_INSPIRATION_ID,
                    CHANNEL_INSPIRATION_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Вдохновляющие сообщения и цитаты"
                    enableLights(true)
                    lightColor = Color.GREEN
                    enableVibration(false) // Тихие уведомления
                    lockscreenVisibility = NotificationManager.IMPORTANCE_LOW
                }

                val notificationManager = context.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

                notificationManager.createNotificationChannel(motivationChannel)
                notificationManager.createNotificationChannel(inspirationChannel)

                Log.d("NotificationHelper", "✅ Каналы мотивации созданы")

            } catch (e: Exception) {
                Log.e("NotificationHelper", "Ошибка создания каналов мотивации: ${e.message}", e)
            }
        } else {
            Log.d("NotificationHelper", "Каналы мотивации не требуются (API < 26)")
        }
    }

    /**
     * Проверить, создан ли мотивационный канал
     */
    fun isMotivationChannelCreated(context: Context, channelId: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            return notificationManager.getNotificationChannel(channelId) != null
        }
        return true
    }

    // ==================== ОБЩИЕ МЕТОДЫ ====================

    /**
     * Создать ВСЕ каналы уведомлений (привычки + мотивация)
     */
    fun createAllChannels(context: Context) {
        Log.d("NotificationHelper", "=== СОЗДАНИЕ ВСЕХ КАНАЛОВ УВЕДОМЛЕНИЙ ===")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val notificationManager = context.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

                // Канал для привычек
                val remindersChannel = NotificationChannel(
                    CHANNEL_REMINDERS_ID,
                    CHANNEL_REMINDERS_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Напоминания о времени выполнения привычек"
                    enableLights(true)
                    lightColor = Color.RED
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500)
                    lockscreenVisibility = NotificationManager.IMPORTANCE_HIGH
                    setShowBadge(true)
                }

                // Канал для срочных уведомлений
                val urgentChannel = NotificationChannel(
                    CHANNEL_URGENT_ID,
                    CHANNEL_URGENT_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Срочные напоминания о пропущенных привычках"
                    enableLights(true)
                    lightColor = Color.RED
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 1000, 200, 1000, 200, 1000)
                    lockscreenVisibility = NotificationManager.IMPORTANCE_HIGH
                    setShowBadge(true)
                }

                // Канал для мотивации
                val motivationChannel = NotificationChannel(
                    CHANNEL_MOTIVATION_ID,
                    CHANNEL_MOTIVATION_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Мотивационные сообщения и поддержка"
                    enableLights(true)
                    lightColor = Color.BLUE
                    enableVibration(false)
                    lockscreenVisibility = NotificationManager.IMPORTANCE_LOW
                }

                // Создаем каналы
                notificationManager.createNotificationChannel(remindersChannel)
                notificationManager.createNotificationChannel(urgentChannel)
                notificationManager.createNotificationChannel(motivationChannel)

                Log.d("NotificationHelper", "✅ Все каналы созданы успешно")

            } catch (e: Exception) {
                Log.e("NotificationHelper", "Ошибка создания каналов: ${e.message}", e)
            }
        } else {
            Log.d("NotificationHelper", "Каналы не требуются (API < 26)")
        }
    }

    /**
     * Удалить все каналы уведомлений (для отладки)
     */
    fun deleteAllChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val notificationManager = context.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

                notificationManager.notificationChannels.forEach { channel ->
                    notificationManager.deleteNotificationChannel(channel.id)
                }

                Log.d("NotificationHelper", "Все каналы удалены")

            } catch (e: Exception) {
                Log.e("NotificationHelper", "Ошибка удаления каналов: ${e.message}")
            }
        }
    }

    /**
     * Получить важность канала
     */
    fun getChannelImportance(context: Context, channelId: String): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            val channel = notificationManager.getNotificationChannel(channelId)
            return channel?.importance ?: NotificationManager.IMPORTANCE_DEFAULT
        }
        return NotificationManager.IMPORTANCE_DEFAULT
    }

    /**
     * Проверить, включены ли уведомления для канала
     */
    fun isChannelEnabled(context: Context, channelId: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            val channel = notificationManager.getNotificationChannel(channelId)
            return channel?.importance != NotificationManager.IMPORTANCE_NONE
        }
        return true
    }

    /**
     * Получить список всех созданных каналов
     */
    fun getAllChannels(context: Context): List<String> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            return notificationManager.notificationChannels.map { it.id }
        }
        return emptyList()
    }

    /**
     * Получить настройки вибрации для канала
     */
    fun getChannelVibrationPattern(context: Context, channelId: String): LongArray? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            val channel = notificationManager.getNotificationChannel(channelId)
            return channel?.vibrationPattern
        }
        return null
    }

    /**
     * Проверить, включен ли свет для канала
     */
    fun isChannelLightEnabled(context: Context, channelId: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            val channel = notificationManager.getNotificationChannel(channelId)
            return channel?.shouldShowLights() ?: false
        }
        return false
    }

    /**
     * Обновить настройки канала (только для отладки)
     */
    fun updateChannelSettings(context: Context, channelId: String, enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val notificationManager = context.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

                val channel = notificationManager.getNotificationChannel(channelId)
                channel?.let {
                    it.importance = if (enabled) {
                        NotificationManager.IMPORTANCE_HIGH
                    } else {
                        NotificationManager.IMPORTANCE_NONE
                    }
                    notificationManager.createNotificationChannel(it)
                    Log.d("NotificationHelper", "Канал $channelId обновлен")
                }

            } catch (e: Exception) {
                Log.e("NotificationHelper", "Ошибка обновления канала: ${e.message}")
            }
        }
    }
}