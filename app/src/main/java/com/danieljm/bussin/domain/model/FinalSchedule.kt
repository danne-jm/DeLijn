package com.danieljm.bussin.domain.model

data class FinalSchedule(
    val halteDoorkomsten: List<HalteDoorkomsten> = emptyList(),
    val doorkomstNotas: List<String> = emptyList(),
    val ritNotas: List<String> = emptyList(),
    val omleidingen: List<String> = emptyList()
)

