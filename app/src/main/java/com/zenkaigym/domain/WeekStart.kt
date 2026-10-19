package com.zenkaigym.domain

import com.zenkaigym.util.dayOfWeekFromEpochDay

/**
 * Primeiro dia da semana escolhido pelo usuário. Os dados continuam com 1 = segunda ... 7 = domingo;
 * isto só muda a ordem em que a semana é mostrada e onde cada semana começa nas métricas.
 */
enum class WeekStart(val pt: String) {
    MONDAY("Segunda"),
    SUNDAY("Domingo");

    val label: String get() = tr(pt)

    /** Dias (1 = segunda ... 7 = domingo) na ordem em que a semana é mostrada. */
    val days: List<Int> get() = if (this == MONDAY) (1..7).toList() else listOf(7) + (1..6)

    /** Posição do dia na semana (0 = primeiro dia). */
    fun position(dayOfWeek: Int): Int = days.indexOf(dayOfWeek)

    /** Primeiro dia (contado desde 1970-01-01) da semana que contém [epochDay]. */
    fun weekStartOf(epochDay: Long): Long = epochDay - position(dayOfWeekFromEpochDay(epochDay))

    companion object {
        fun fromName(name: String?): WeekStart = entries.firstOrNull { it.name == name } ?: MONDAY
    }
}
