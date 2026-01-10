package com.example.trecker.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.trecker.MotivationManager
import com.example.trecker.NotificationHelper
import com.example.trecker.HabitManager
import com.example.trecker.MotivationType

class MotivationNotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MotivationReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Получено мотивационное уведомление: ${intent.action}")

        try {
            val motivationManager = MotivationManager(context)
            val habitManager = HabitManager(context)

            val typeStr = intent.getStringExtra("motivation_type") ?: "RANDOM_ENCOURAGEMENT"

            // 👇 ИСПРАВЬТЕ: преобразуйте строку в enum
            val motivationType = MotivationType.valueOf(typeStr)

            var habitName: String? = null
            var habitId: Int? = null

            // Используйте enum для сравнения
            if (motivationType == MotivationType.HABIT_SPECIFIC) {
                habitName = intent.getStringExtra("habit_name")
                habitId = intent.getIntExtra("habit_id", -1)

                if (habitId != -1) {
                    val habit = habitManager.findHabitById(habitId)
                    if (habit?.isCompleted == true) {
                        Log.d(TAG, "Привычка уже выполнена, пропускаем уведомление")
                        return
                    }
                }
            }

            // Передавайте enum
            val (messageTitle, messageText) = motivationManager.getMotivationMessage(motivationType, habitName)

            // Используйте enum для сравнения
            val finalTitle = if (motivationType == MotivationType.DAY_PROGRESS) {
                val todayHabits = habitManager.getTodayHabits()
                val completed = todayHabits.count { it.isCompleted }
                val total = todayHabits.size

                if (total > 0) {
                    val progress = (completed * 100) / total
                    "$messageTitle (Выполнено: $completed/$total, $progress%)"
                } else {
                    messageTitle
                }
            } else {
                messageTitle
            }

            showMotivationNotification(context, finalTitle, messageText, motivationType)

            Log.d(TAG, "✅ Мотивационное уведомление показано: $finalTitle")

        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Неверный тип мотивации, используем RANDOM_ENCOURAGEMENT")
            // Можно добавить fallback логику
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка обработки мотивации: ${e.message}", e)
        }
    }

    private fun showMotivationNotification(
        context: Context,
        title: String,
        message: String,
        motivationType: MotivationType  // ← Принимаем enum
    ) {
        try {
            // Используйте enum для сравнения
            val channelId = when (motivationType) {
                MotivationType.STREAK_CELEBRATION -> NotificationHelper.CHANNEL_URGENT_ID
                else -> MotivationManager.CHANNEL_MOTIVATION_ID
            }

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                builder.setChannelId(channelId)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val notificationId = (System.currentTimeMillis() % 10000).toInt()
            notificationManager.notify(notificationId, builder.build())

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка создания уведомления: ${e.message}", e)
        }
    }
}