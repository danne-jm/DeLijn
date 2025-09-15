package com.danieljm.delijn.data.remote.dto

/** DTO for real-time arrival information */
data class RealTimeDto(
    val stopId: String,
    val line: String,
    val expectedArrivalEpochMs: Long
)

