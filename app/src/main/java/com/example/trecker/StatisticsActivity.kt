package com.example.trecker

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trecker.databinding.ActivityStatisticsBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import java.util.*

class StatisticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatisticsBinding
    private lateinit var habitManager: HabitManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("StatisticsActivity", "=== НАЧАЛО СОЗДАНИЯ АКТИВНОСТИ ===")

        try {
            binding = ActivityStatisticsBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d("StatisticsActivity", "ViewBinding успешно инициализирован")

            habitManager = HabitManager(this)
            Log.d("StatisticsActivity", "HabitManager создан")

            setupUI()
            loadSimpleStatistics() // ← ИСПОЛЬЗУЕМ УПРОЩЁННУЮ ВЕРСИЮ

            Log.d("StatisticsActivity", "=== АКТИВНОСТЬ УСПЕШНО СОЗДАНА ===")

        } catch (e: Exception) {
            Log.e("StatisticsActivity", "КРИТИЧЕСКАЯ ОШИБКА в onCreate: ${e.message}", e)
            showErrorDialog("Ошибка создания экрана статистики: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("StatisticsActivity", "onResume - обновляем статистику")
        loadSimpleStatistics()
    }

    private fun setupUI() {
        try {
            Log.d("StatisticsActivity", "Настройка UI...")

            // Кнопка назад
            binding.backButton.setOnClickListener {
                finish()
            }

            // Кнопка обновления
            binding.refreshButton.setOnClickListener {
                Log.d("StatisticsActivity", "Ручное обновление статистики")
                loadSimpleStatistics()
                Toast.makeText(this, "Статистика обновлена", Toast.LENGTH_SHORT).show()
            }

            // Настройка RecyclerViews (ВРЕМЕННО отключены)
            Log.d("StatisticsActivity", "Инициализация адаптеров...")

            // Настройка графиков
            Log.d("StatisticsActivity", "Настройка графиков...")
            setupPieChart(binding.completionPieChart)
            setupBarChart(binding.weeklyBarChart)

            Log.d("StatisticsActivity", "UI настроен успешно")

        } catch (e: Exception) {
            Log.e("StatisticsActivity", "Ошибка в setupUI: ${e.message}", e)
            Toast.makeText(this, "Ошибка настройки интерфейса", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * УПРОЩЁННАЯ ЗАГРУЗКА СТАТИСТИКИ (с адаптерами)
     */
    private fun loadSimpleStatistics() {
        try {
            Log.d("StatisticsActivity", "=== ЗАГРУЗКА УПРОЩЁННОЙ СТАТИСТИКИ ===")

            val habits = habitManager.getAllHabits()
            val todayHabits = habitManager.getTodayHabits()

            Log.d("StatisticsActivity", "Всего привычек в базе: ${habits.size}")
            Log.d("StatisticsActivity", "Привычек на сегодня: ${todayHabits.size}")

            val completedToday = todayHabits.count { it.isCompleted }
            val totalToday = todayHabits.size

            runOnUiThread {
                try {
                    // 1. Основные показатели
                    Log.d("StatisticsActivity", "Обновление текстовых полей...")
                    binding.totalHabitsText.text = habits.size.toString()
                    binding.completedTodayText.text = "$completedToday/$totalToday"

                    // 2. Процент выполнения
                    val completionRate = if (totalToday > 0) {
                        (completedToday.toFloat() / totalToday) * 100
                    } else 0f
                    binding.completionRateText.text = "%.1f%%".format(completionRate)

                    // 3. Прогресс-бар
                    Log.d("StatisticsActivity", "Обновление прогресс-бара...")
                    binding.todayProgressBar.max = totalToday
                    binding.todayProgressBar.progress = completedToday

                    // 4. Серии (упрощённо)
                    val currentStreak = calculateSimpleStreak()
                    binding.streakText.text = currentStreak.toString()
                    binding.bestStreakText.text = currentStreak.toString() // временно

                    // 5. Графики
                    Log.d("StatisticsActivity", "Обновление графиков...")
                    updateSimplePieChart(completedToday, totalToday)
                    updateSimpleBarChart()

                    // 6. АДАПТЕРЫ - ВКЛЮЧАЕМ!
                    Log.d("StatisticsActivity", "Обновление адаптеров...")

                    Log.d("StatisticsActivity", "Упрощённая статистика успешно загружена")

                } catch (uiException: Exception) {
                    Log.e("StatisticsActivity", "ОШИБКА UI: ${uiException.message}", uiException)
                    Toast.makeText(this, "Ошибка отображения: ${uiException.message}", Toast.LENGTH_SHORT).show()
                }
            }

        } catch (e: Exception) {
            Log.e("StatisticsActivity", "ОШИБКА загрузки статистики: ${e.message}", e)
            runOnUiThread {
                Toast.makeText(this, "Ошибка загрузки: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Упрощённый расчёт серии
     */
    private fun calculateSimpleStreak(): Int {
        try {
            var streak = 0
            val calendar = Calendar.getInstance()

            // Проверяем последние 30 дней
            for (i in 0..29) {
                calendar.time = Date() // сбрасываем на сегодня
                calendar.add(Calendar.DAY_OF_MONTH, -i)
                val date = calendar.time
                val dayHabits = habitManager.getHabitsForDate(date)

                Log.d("StatisticsActivity", "День -$i: привычек ${dayHabits.size}")

                if (dayHabits.isNotEmpty()) {
                    val completed = dayHabits.count { it.isCompleted }
                    val total = dayHabits.size

                    Log.d("StatisticsActivity", "  Выполнено: $completed из $total")

                    if (completed == total && completed > 0) {
                        streak++
                        Log.d("StatisticsActivity", "  ✅ Серия продолжается: $streak")
                    } else {
                        Log.d("StatisticsActivity", "  ❌ Серия прервана")
                        break
                    }
                } else {
                    Log.d("StatisticsActivity", "  ⏸ Нет привычек, серия прервана")
                    break
                }
            }

            Log.d("StatisticsActivity", "Итоговая серия: $streak дней")
            return streak

        } catch (e: Exception) {
            Log.e("StatisticsActivity", "Ошибка расчёта серии: ${e.message}")
            return 0
        }
    }

    private fun setupPieChart(pieChart: PieChart) {
        try {
            pieChart.apply {
                setUsePercentValues(true)
                description.isEnabled = false
                setExtraOffsets(5f, 10f, 5f, 5f)
                dragDecelerationFrictionCoef = 0.95f
                isDrawHoleEnabled = true
                setHoleColor(Color.WHITE)
                setTransparentCircleColor(Color.WHITE)
                setTransparentCircleAlpha(110)
                holeRadius = 58f
                transparentCircleRadius = 61f
                setDrawCenterText(true)
                setCenterText("Сегодня")
                rotationAngle = 0f
                isRotationEnabled = true
                isHighlightPerTapEnabled = true
                legend.isEnabled = false
                setEntryLabelColor(Color.WHITE)
                setEntryLabelTextSize(12f)
            }
        } catch (e: Exception) {
            Log.e("StatisticsActivity", "Ошибка в setupPieChart: ${e.message}")
        }
    }

    /**
     * Упрощённый круговой график
     */
    private fun updateSimplePieChart(completedToday: Int, totalToday: Int) {
        try {
            val entries = ArrayList<PieEntry>()

            if (totalToday > 0) {
                entries.add(PieEntry(completedToday.toFloat(), "Выполнено"))
                entries.add(PieEntry((totalToday - completedToday).toFloat(), "Осталось"))
            } else {
                entries.add(PieEntry(1f, "Нет привычек"))
                binding.completionPieChart.centerText = "Нет данных"
            }

            val dataSet = PieDataSet(entries, "")
            dataSet.apply {
                sliceSpace = 3f
                selectionShift = 5f
                colors = listOf(
                    ContextCompat.getColor(this@StatisticsActivity, R.color.green),
                    ContextCompat.getColor(this@StatisticsActivity, R.color.RedMy)
                )
                valueTextSize = 14f
                valueTextColor = Color.WHITE
            }

            val data = PieData(dataSet)
            data.setValueTextSize(11f)
            data.setValueTextColor(Color.WHITE)

            binding.completionPieChart.data = data
            binding.completionPieChart.invalidate()

        } catch (e: Exception) {
            Log.e("StatisticsActivity", "Ошибка в updateSimplePieChart: ${e.message}")
        }
    }

    private fun setupBarChart(barChart: BarChart) {
        try {
            barChart.apply {
                description.isEnabled = false
                setDrawGridBackground(false)
                setDrawBarShadow(false)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(false)

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 1f
                    labelCount = 7
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return when (value.toInt()) {
                                0 -> "Пн"
                                1 -> "Вт"
                                2 -> "Ср"
                                3 -> "Чт"
                                4 -> "Пт"
                                5 -> "Сб"
                                6 -> "Вс"
                                else -> "День ${value.toInt()}"
                            }
                        }
                    }
                }

                axisLeft.apply {
                    setDrawGridLines(true)
                    axisMinimum = 0f
                    axisMaximum = 100f
                }

                axisRight.isEnabled = false
                legend.isEnabled = false
            }
        } catch (e: Exception) {
            Log.e("StatisticsActivity", "Ошибка в setupBarChart: ${e.message}")
        }
    }

    private fun updateSimpleBarChart() {
        try {
            val entries = ArrayList<BarEntry>()
            val calendar = Calendar.getInstance()

            // Данные за последние 7 дней
            for (i in 0..6) {
                calendar.time = Date()
                calendar.add(Calendar.DAY_OF_MONTH, -i)
                val date = calendar.time
                val dayHabits = habitManager.getHabitsForDate(date)

                val completed = dayHabits.count { it.isCompleted }
                val total = dayHabits.size

                val rate = if (total > 0) {
                    (completed.toFloat() / total) * 100
                } else 0f

                entries.add(BarEntry((6 - i).toFloat(), rate)) // обратный порядок
            }

            val dataSet = BarDataSet(entries, "Прогресс по дням")

            dataSet.color = ContextCompat.getColor(this, R.color.RedMy)

            dataSet.valueTextSize = 10f
            dataSet.valueTextColor = ContextCompat.getColor(this, R.color.black) // цвет значений

            val data = BarData(dataSet)
            data.barWidth = 0.5f

            binding.weeklyBarChart.data = data

            // Опционально: Настройка осей в purple
            binding.weeklyBarChart.xAxis.textColor = ContextCompat.getColor(this, R.color.black)
            binding.weeklyBarChart.axisLeft.textColor = ContextCompat.getColor(this, R.color.black)

            binding.weeklyBarChart.invalidate()

        } catch (e: Exception) {
            Log.e("StatisticsActivity", "Ошибка в updateSimpleBarChart: ${e.message}")
        }
    }

    private fun showErrorDialog(message: String) {
        try {
            AlertDialog.Builder(this)
                .setTitle("Ошибка")
                .setMessage("$message\n\nПриложение будет закрыто.")
                .setPositiveButton("OK") { _, _ -> finish() }
                .setCancelable(false)
                .show()
        } catch (e: Exception) {
            Log.e("StatisticsActivity", "Ошибка показа диалога: ${e.message}")
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("StatisticsActivity", "Активити уничтожена")
    }
}