package com.danieljm.bussin.domain.model

/**
 * Pure domain model for a Stop (halte). No Android or networking dependencies here.
 */
data class Stop(
    val id: String,
    val name: String,
    val distance: Int? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)
