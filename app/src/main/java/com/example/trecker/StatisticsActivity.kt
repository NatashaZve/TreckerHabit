package com.example.trecker

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.trecker.databinding.ActivityStatisticsBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*

class StatisticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatisticsBinding
    private lateinit var habitManager: HabitManager
    private lateinit var statisticsManager: StatisticsManager

    private lateinit var monthlyChart: BarChart
    private lateinit var monthlyStatsRecyclerView: RecyclerView
    private lateinit var monthlyAdapter: MonthlyStatsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("StatisticsActivity", "=== НАЧАЛО СОЗДАНИЯ АКТИВНОСТИ ===")

        try {
            binding = ActivityStatisticsBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d("StatisticsActivity", "ViewBinding успешно инициализирован")

            habitManager = HabitManager(this)
            statisticsManager = StatisticsManager(this)
            Log.d("StatisticsActivity", "Managers созданы")

            setupUI()
            loadSimpleStatistics()

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

            // Настройка графиков
            Log.d("StatisticsActivity", "Настройка графиков...")
            setupPieChart(binding.completionPieChart)
            setupBarChart(binding.weeklyBarChart)

            // Настройка месячной статистики
            setupMonthlySection()

            Log.d("StatisticsActivity", "UI настроен успешно")

        } catch (e: Exception) {
            Log.e("StatisticsActivity", "Ошибка в setupUI: ${e.message}", e)
            Toast.makeText(this, "Ошибка настройки интерфейса", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupMonthlySection() {
        try {
            // Создаем карточку для месячной статистики
            val monthlyCard = com.google.android.material.card.MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 16.dpToPx())
                }
                radius = 12.dpToPx().toFloat()
                elevation = 4.dpToPx().toFloat()
            }

            val linearLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16.dpToPx(), 16.dpToPx(), 16.dpToPx(), 16.dpToPx())
            }

            // Заголовок
            TextView(this).apply {
                text = "Прогресс за месяц"
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(this@StatisticsActivity, R.color.black))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 16.dpToPx()
                }
                linearLayout.addView(this)
            }

            // График по месяцам
            monthlyChart = BarChart(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    200.dpToPx()
                )
                linearLayout.addView(this)
            }

            // Список месяцев
            monthlyStatsRecyclerView = RecyclerView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    300.dpToPx()
                ).apply {
                    topMargin = 16.dpToPx()
                }
                layoutManager = LinearLayoutManager(this@StatisticsActivity)
                linearLayout.addView(this)
            }

            monthlyCard.addView(linearLayout)

            // Добавляем карточку в основной контейнер
            val mainContainer = binding.root.findViewById<LinearLayout>(R.id.mainLinearLayout)
            mainContainer?.addView(monthlyCard)

        } catch (e: Exception) {
            Log.e("StatisticsActivity", "Ошибка настройки месячной секции: ${e.message}")
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

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

                    // 4. Серии
                    val currentStreak = calculateSimpleStreak()
                    binding.streakText.text = currentStreak.toString()

                    // Лучшая серия
                    val bestStreak = habits.maxOfOrNull { it.bestStreak } ?: currentStreak
                    binding.bestStreakText.text = bestStreak.toString()

                    // 5. Графики
                    Log.d("StatisticsActivity", "Обновление графиков...")
                    updateSimplePieChart(completedToday, totalToday)
                    updateSimpleBarChart()

                    // 6. Месячная статистика
                    updateMonthlyChart()
                    updateMonthlyStatsList()

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

    private fun calculateSimpleStreak(): Int {
        try {
            var streak = 0
            val calendar = Calendar.getInstance()

            // Проверяем последние 30 дней
            for (i in 0..29) {
                calendar.time = Date()
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

    private fun updateSimplePieChart(completedToday: Int, totalToday: Int) {
        try {
            val entries = ArrayList<PieEntry>()

            if (totalToday > 0) {
                entries.add(PieEntry(completedToday.toFloat()))
                entries.add(PieEntry((totalToday - completedToday).toFloat()))
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
            dataSet.valueTextColor = ContextCompat.getColor(this, R.color.black)

            val data = BarData(dataSet)
            data.barWidth = 0.5f

            binding.weeklyBarChart.data = data
            binding.weeklyBarChart.xAxis.textColor = ContextCompat.getColor(this, R.color.black)
            binding.weeklyBarChart.axisLeft.textColor = ContextCompat.getColor(this, R.color.black)
            binding.weeklyBarChart.invalidate()

        } catch (e: Exception) {
            Log.e("StatisticsActivity", "Ошибка в updateSimpleBarChart: ${e.message}")
        }
    }

    // ДОБАВЬТЕ ЭТИ МЕТОДЫ ДЛЯ СОРТИРОВКИ МЕСЯЦЕВ:

    private fun sortMonthlyStats(stats: List<StatisticsManager.MonthlyStat>): List<StatisticsManager.MonthlyStat> {
        // Карта для перевода названий месяцев в числа
        val monthOrder = mapOf(
            "Январь" to 1, "Февраль" to 2, "Март" to 3,
            "Апрель" to 4, "Май" to 5, "Июнь" to 6,
            "Июль" to 7, "Август" to 8, "Сентябрь" to 9,
            "Октябрь" to 10, "Ноябрь" to 11, "Декабрь" to 12
        )

        return stats.sortedWith(compareBy(
            // Сначала по году
            { stat ->
                try {
                    stat.month.split(" ")[1].toInt()
                } catch (e: Exception) {
                    0
                }
            },
            // Затем по месяцу
            { stat ->
                monthOrder[stat.month.split(" ")[0]] ?: 0
            }
        ))
    }

    private fun getShortMonthName(fullName: String): String {
        return when (fullName) {
            "Январь" -> "Янв"
            "Февраль" -> "Фев"
            "Март" -> "Мар"
            "Апрель" -> "Апр"
            "Май" -> "Май"
            "Июнь" -> "Июн"
            "Июль" -> "Июл"
            "Август" -> "Авг"
            "Сентябрь" -> "Сен"
            "Октябрь" -> "Окт"
            "Ноябрь" -> "Ноя"
            "Декабрь" -> "Дек"
            else -> fullName.take(3)
        }
    }

    private fun updateMonthlyChart() {
        try {
            val monthlyStats = statisticsManager.getMonthlyStats(6)

            // Сортируем статистику
            val sortedStats = sortMonthlyStats(monthlyStats)

            val entries = ArrayList<BarEntry>()
            val labels = ArrayList<String>()

            sortedStats.forEachIndexed { index, stat ->
                entries.add(BarEntry(index.toFloat(), stat.rate))

                // Создаем короткое название месяца
                val monthParts = stat.month.split(" ")
                if (monthParts.size == 2) {
                    val monthName = monthParts[0]
                    val year = monthParts[1]
                    labels.add("${getShortMonthName(monthName)}\n'${year.takeLast(2)}")
                } else {
                    labels.add(stat.month)
                }
            }

            val dataSet = BarDataSet(entries, "Выполнение по месяцам (%)")
            dataSet.color = ContextCompat.getColor(this, R.color.purple)
            dataSet.valueTextSize = 10f
            dataSet.valueTextColor = Color.BLACK

            val data = BarData(dataSet)
            data.barWidth = 0.4f

            monthlyChart.apply {
                this.data = data
                description.isEnabled = false

                xAxis.apply {
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return labels.getOrNull(value.toInt()) ?: ""
                        }
                    }
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    setDrawGridLines(false)
                    textColor = Color.BLACK
                    labelCount = labels.size
                }

                axisLeft.apply {
                    axisMinimum = 0f
                    axisMaximum = 100f
                    granularity = 20f
                    textColor = Color.BLACK
                }

                axisRight.isEnabled = false
                legend.isEnabled = false
                setTouchEnabled(true)
                setDragEnabled(true)
                setScaleEnabled(true)
                setPinchZoom(false)

                animateY(1000)
                invalidate()
            }

        } catch (e: Exception) {
            Log.e("StatisticsActivity", "Ошибка обновления месячного графика: ${e.message}")
        }
    }

    private fun updateMonthlyStatsList() {
        try {
            val monthlyStats = statisticsManager.getMonthlyStats(6)

            // Используем нашу функцию сортировки
            val sortedStats = sortMonthlyStats(monthlyStats)

            monthlyAdapter = MonthlyStatsAdapter(sortedStats)
            monthlyStatsRecyclerView.adapter = monthlyAdapter

        } catch (e: Exception) {
            Log.e("StatisticsActivity", "Ошибка обновления списка месяцев: ${e.message}")
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

// Класс адаптера для месячной статистики
class MonthlyStatsAdapter(
    private val monthlyStats: List<StatisticsManager.MonthlyStat>
) : RecyclerView.Adapter<MonthlyStatsAdapter.MonthlyViewHolder>() {

    class MonthlyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val monthName: TextView = itemView.findViewById(R.id.monthName)
        val completionRate: TextView = itemView.findViewById(R.id.completionRate)
        val totalCompleted: TextView = itemView.findViewById(R.id.totalCompleted)
        val daysWithHabits: TextView = itemView.findViewById(R.id.daysWithHabits)
        val bestDay: TextView = itemView.findViewById(R.id.bestDay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthlyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_monthly_stat, parent, false)
        return MonthlyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MonthlyViewHolder, position: Int) {
        val stat = monthlyStats[position]

        // ИСПРАВЛЕНО: используйте stat.month вместо stat.monthName
        holder.monthName.text = stat.month

        holder.completionRate.text = "Выполнено: ${"%.1f".format(stat.rate)}%"
        holder.totalCompleted.text = "${stat.completed}/${stat.total}"
        holder.daysWithHabits.text = "Дней с привычками: ${stat.daysWithHabits}"

        holder.bestDay.text = if (stat.bestDay != null) {
            "🏆 Лучший день: ${stat.bestDay.first} (${stat.bestDay.second})"
        } else {
            "🏆 Нет лучшего дня"
        }

        // Цвет в зависимости от процента выполнения
        val color = when {
            stat.rate >= 80 -> android.R.color.holo_green_dark
            stat.rate >= 50 -> android.R.color.holo_orange_dark
            else -> android.R.color.holo_red_dark
        }

        holder.completionRate.setTextColor(
            ContextCompat.getColor(holder.itemView.context, color)
        )
    }

    override fun getItemCount() = monthlyStats.size
}