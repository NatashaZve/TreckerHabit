package com.example.trecker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.trecker.HabitManager
import com.example.trecker.HabitNotificationManager

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "BootReceiver вызван: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                Log.d(TAG, "Перезагрузка устройства, перепланируем уведомления")
                rescheduleNotifications(context)
            }
        }
    }

    private fun rescheduleNotifications(context: Context) {
        Log.d(TAG, "=== ПЕРЕПЛАНИРОВАНИЕ ПОСЛЕ ПЕРЕЗАГРУЗКИ ===")

        val habitManager = HabitManager(context)
        val notificationManager = HabitNotificationManager(context)

        val habits = habitManager.getAllHabits()
            .filter { it.notificationEnabled }

        Log.d(TAG, "Найдено привычек с уведомлениями: ${habits.size}")

        if (habits.isNotEmpty()) {
            // Перепланируем каждую привычку
            habits.forEach { habit ->
                Log.d(TAG, "Перепланирование: ${habit.name}")
                notificationManager.scheduleNotification(habit)
            }
            Log.d(TAG, "✅ Все уведомления перепланированы")
        } else {
            Log.d(TAG, "Нет привычек для перепланирования")
        }
    }
}