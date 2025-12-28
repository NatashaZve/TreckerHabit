package com.example.trecker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.trecker.HabitNotificationManager

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        when (action) {
            HabitNotificationManager.ACTION_COMPLETE_HABIT -> {
                Log.d("NotificationActionReceiver", "Привычка отмечена выполненной")
            }

            HabitNotificationManager.ACTION_SNOOZE_HABIT -> {
                Log.d("NotificationActionReceiver", "Уведомление отложено")
            }

            else -> {
                Log.w("NotificationActionReceiver", "Неизвестное действие: $action")
            }
        }
    }
}