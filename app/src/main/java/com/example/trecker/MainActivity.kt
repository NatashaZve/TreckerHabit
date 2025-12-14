package com.example.trecker

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trecker.databinding.ActivityMainBinding
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var habitManager: HabitManager
    private lateinit var habitAdapter: HabitAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Инициализируем ThreeTenABP (на всякий случай)
            com.jakewharton.threetenabp.AndroidThreeTen.init(this)

            enableEdgeToEdge()

            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Инициализируем менеджер привычек
            habitManager = HabitManager(this)

            setupDateDisplay()
            setupRefreshButton()
            setupHabitsRecyclerView()

            setupSystemBars()

            Log.d("MainActivity", "Приложение успешно создано")

        } catch (e: Exception) {
            Log.e("MainActivity", "Ошибка при создании: ${e.message}", e)
            Toast.makeText(this, "Ошибка запуска: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupDateDisplay() {
        try {
            val currentDate = LocalDate.now()

            binding.dayOfMonthTextView.text = currentDate.dayOfMonth.toString()

            val fullDateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy 'г.'", Locale("ru"))
            binding.fullDateTextView.text = currentDate.format(fullDateFormatter)
        } catch (e: Exception) {
            Log.e("MainActivity", "Ошибка в setupDateDisplay: ${e.message}")
            binding.dayOfMonthTextView.text = "?"
            binding.fullDateTextView.text = "Ошибка даты"
        }
    }

    private fun setupRefreshButton() {
        binding.refreshButton.setOnClickListener {
            setupDateDisplay()
            loadTodayHabits()
        }
    }

    private fun setupHabitsRecyclerView() {
        try {
            habitAdapter = HabitAdapter(emptyList(),
                onCompleteClick = { habitId ->
                    habitManager.completeHabit(habitId)
                    loadTodayHabits()
                    Toast.makeText(this, "Привычка выполнена!", Toast.LENGTH_SHORT).show()
                },
                onDeleteClick = { habitId ->
                    habitManager.deleteHabit(habitId)
                    loadTodayHabits()
                    Toast.makeText(this, "Привычка удалена!", Toast.LENGTH_SHORT).show()
                }
            )

            binding.habitsRecyclerView.apply {
                layoutManager = LinearLayoutManager(this@MainActivity)
                adapter = habitAdapter
                setHasFixedSize(true)
            }

            loadTodayHabits()
        } catch (e: Exception) {
            Log.e("MainActivity", "Ошибка в setupHabitsRecyclerView: ${e.message}")
            binding.habitsRecyclerView.visibility = View.GONE
            binding.todayHabitsTitle.visibility = View.GONE
        }
    }

    private fun loadTodayHabits() {
        try {
            val todayHabits = habitManager.getTodayHabits()

            Log.d("MainActivity", "Сегодняшних привычек: ${todayHabits.size}")

            habitAdapter.updateHabits(todayHabits)

            if (todayHabits.isNotEmpty()) {
                binding.todayHabitsTitle.visibility = View.VISIBLE
                binding.habitsRecyclerView.visibility = View.VISIBLE
                binding.todayHabitsTitle.text = "Сегодняшние привычки (${todayHabits.size}):"
            } else {
                binding.todayHabitsTitle.visibility = View.GONE
                binding.habitsRecyclerView.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Ошибка в loadTodayHabits: ${e.message}")
            binding.habitsRecyclerView.visibility = View.GONE
            binding.todayHabitsTitle.visibility = View.GONE
        }
    }

    private fun setupSystemBars() {
        try {
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Ошибка в setupSystemBars: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        setupDateDisplay()
        loadTodayHabits()
    }

    fun startNewActivity(view: View) {
        try {
            val intent = Intent(this, CalendarActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("MainActivity", "Ошибка при переходе: ${e.message}")
            Toast.makeText(this, "Ошибка открытия календаря", Toast.LENGTH_SHORT).show()
        }
    }
}