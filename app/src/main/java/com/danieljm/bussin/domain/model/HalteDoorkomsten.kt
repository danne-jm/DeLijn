package com.danieljm.bussin.domain.model

data class HalteDoorkomsten(
    val haltenummer: String,
    val doorkomsten: List<Arrival> = emptyList()
)

