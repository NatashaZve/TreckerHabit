package com.example.trecker

enum class RepeatType {
    ONCE,           // Для одноразовых привычек
    NEVER,          // Никогда (эквивалент ONCE)
    DAILY,          // Ежедневно
    WEEKLY,         // Еженедельно
    MONTHLY,        // Ежемесячно
    YEARLY,         // Ежегодно
    CUSTOM_INTERVAL,// Пользовательский интервал
    WEEKDAYS,       // Рабочие дни
    WEEKENDS,       // Выходные
    SPECIFIC_DAYS,  // Конкретные дни
    CUSTOM_DAYS     // Пользовательские дни (для обратной совместимости)
}