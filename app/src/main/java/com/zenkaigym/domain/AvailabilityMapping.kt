package com.zenkaigym.domain

import com.zenkaigym.data.entity.Availability

/** Janela de treino do dia; null se o dia não está disponível ou não tem horário utilizável. */
fun Availability.timeWindow(): TimeWindow? =
    if (available) TimeWindow.fromStored(startTime, endTime) else null

/** Dia da semana (1–7) -> janela, só para dias disponíveis com horário. */
fun List<Availability>.windowsByDay(): Map<Int, TimeWindow> =
    mapNotNull { row -> row.timeWindow()?.let { row.dayOfWeek to it } }.toMap()

/** Linha do banco para o dia: disponível se houver janela. */
fun availabilityOf(dayOfWeek: Int, window: TimeWindow?) =
    Availability(dayOfWeek, window != null, window?.start?.format(), window?.end?.format())
