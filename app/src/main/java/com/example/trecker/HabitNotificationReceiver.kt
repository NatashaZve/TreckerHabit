package com.example.trecker.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat

class HabitNotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "HabitNotificationReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive вызван с action: ${intent.action}")

        when (intent.action) {
            "com.example.trecker.SHOW_HABIT_REMINDER" -> {
                showHabitNotification(context, intent)
            }
            else -> {
                Log.w(TAG, "Неизвестный action: ${intent.action}")
            }
        }
    }

    private fun showHabitNotification(context: Context, intent: Intent) {
        try {
            val habitName = intent.getStringExtra("habit_name") ?: "Привычка"
            val notificationId = intent.getIntExtra("notification_id", 1000)

            Log.d(TAG, "Создаю уведомление для: $habitName")

            // Временное решение - используем системную иконку
            val builder = NotificationCompat.Builder(context, "habit_reminders")
                .setSmallIcon(android.R.drawable.ic_dialog_info) // ← Системная иконка
                .setContentTitle("Напоминание: $habitName")
                .setContentText("Время выполнить привычку")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

            notificationManager.notify(notificationId, builder.build())

            Log.d(TAG, "✅ Уведомление показано")

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при создании уведомления: ${e.message}", e)
        }
    }
}