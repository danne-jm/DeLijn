package com.danieljm.delijn.domain.model

data class ServedLine(
    val lineId: String,
    val lineName: String,
    val omschrijving: String,
    val asFromTo: String,
    val entiteitnummer: String = "",
    val richting: String = ""
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
    val isScheduleOnly: Boolean = false,
    val vrtnum: String? = null, // Bus id, nullable for scheduled arrivals
    // New optional fields populated by ViewModel after calling the search API
    val lineNumberPublic: String? = null,
    val lineBackgroundColorHex: String? = null,
    val lineForegroundColorHex: String? = null,
    val lineForegroundRandColorHex: String? = null
)
