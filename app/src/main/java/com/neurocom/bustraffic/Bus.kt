package com.neurocom.bustraffic

import java.time.LocalTime
import java.time.temporal.ChronoUnit

data class Bus(val schedule: List<LocalTime>, val durationTime: Int) {

    //вычисление времени в пути
    fun getTimeInTransit(currentTime: LocalTime): Long {
        val lastDepartureTime = schedule.filter { it <= currentTime }.maxOrNull() ?: return 0//фильтрует shedule и выбирает максимальное значение
        return ChronoUnit.MINUTES.between(lastDepartureTime, currentTime)
    }

    //вычисление времени до следующего рейса
    fun getTimeToNextDeparture(currentTime: LocalTime): Long {
        val nextDepartureTime = schedule.filter { it > currentTime }.minOrNull()?: schedule.first() // если текущее время после последнего рейса, берем первый рейс следующего дня
        return if (nextDepartureTime > currentTime) {
            ChronoUnit.SECONDS.between(currentTime, nextDepartureTime)
        } else {
            //если следующий рейс на следующий день
            val minutesToMidnight = ChronoUnit.SECONDS.between(currentTime, LocalTime.MAX) + 1
            val minutesFromMidnight = ChronoUnit.SECONDS.between(LocalTime.MIN, nextDepartureTime)
            minutesToMidnight + minutesFromMidnight
        }
    }

    //вычисление списка ближайших автобусов за последние ... минут
    fun getRecentBuses(currentTime: LocalTime): List<Pair<LocalTime, Double>> {
        val recentBuses = mutableListOf<Pair<LocalTime, Double>>()
        val durationTimeAgo = currentTime.minusMinutes(durationTime.toLong())

        for (departureTime in schedule) {
            if (departureTime > durationTimeAgo && departureTime <= currentTime) {
                val timeInTransit = ChronoUnit.SECONDS.between(departureTime, currentTime).toDouble() / 60.0
                recentBuses.add(Pair(departureTime, timeInTransit))
            }
        }
        return recentBuses
    }
}