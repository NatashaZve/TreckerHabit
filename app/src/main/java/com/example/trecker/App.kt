package com.example.trecker

import android.app.Application
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeFormatterBuilder
import org.threeten.bp.format.ResolverStyle
import java.util.*

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // Инициализируем ThreeTenABP
        com.jakewharton.threetenabp.AndroidThreeTen.init(this)

        // Настраиваем форматтер для русского языка
        setupDateTimeFormatters()
    }

    private fun setupDateTimeFormatters() {
        // Можно настроить кастомные форматеры здесь если нужно
    }
}