package com.example.trecker

enum class RepeatType {
    NEVER,           // Никогда (эквивалент ONCE)
    ONCE,            // Один раз
    DAILY,           // Ежедневно
    WEEKLY,          // Еженедельно
    MONTHLY,         // Ежемесячно
    YEARLY,          // Ежегодно
    CUSTOM_INTERVAL, // Пользовательский интервал
    WEEKDAYS,        // Рабочие дни
    WEEKENDS,        // Выходные
    SPECIFIC_DAYS,   // Конкретные дни
    CUSTOM_DAYS      // Пользовательские дни
}