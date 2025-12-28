package com.example.trecker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.Log

object NotificationHelper {

    // Идентификаторы каналов
    const val CHANNEL_REMINDERS_ID = "habit_reminders"
    const val CHANNEL_URGENT_ID = "habit_urgent"

    private const val CHANNEL_REMINDERS_NAME = "Напоминания о привычек"
    private const val CHANNEL_URGENT_NAME = "Срочные напоминания"

    /**
     * Создать каналы уведомлений
     */
    fun createAllChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("NotificationHelper", "Создание каналов уведомлений...")

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

                // Видимость на заблокированном экране (1 = PUBLIC)
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

                // Видимость на заблокированном экране
                lockscreenVisibility = NotificationManager.IMPORTANCE_MAX
            }

            val notificationManager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            notificationManager.createNotificationChannel(remindersChannel)
            notificationManager.createNotificationChannel(urgentChannel)

            Log.d("NotificationHelper", "Каналы уведомлений созданы")
        }
    }

    /**
     * Проверить, создан ли канал (добавьте этот метод!)
     */
    fun isChannelCreated(context: Context, channelId: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            return notificationManager.getNotificationChannel(channelId) != null
        }
        return true // Для версий ниже 8.0 каналы не используются
    }
}