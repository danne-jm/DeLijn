package com.danieljm.delijn.domain.model

data class ServedLine(
    val lineId: String,
    val lineName: String,
    val omschrijving: String,
    val asFromTo: String
)

data class ArrivalInfo(
    val lineId: String,
    val destination: String,
    val scheduledTime: String,
    val time: String,
    val remainingMinutes: Long,
    val omschrijving: String,
    val expectedArrivalTime: Long,
    val realArrivalTime: Long,
    val isScheduleOnly: Boolean = false
)

