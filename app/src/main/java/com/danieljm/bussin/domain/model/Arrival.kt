package com.danieljm.bussin.domain.model

/**
 * Domain model for an arrival/doorkomst at a stop
 */
data class Arrival(
    val doorkomstId: String?,
    val entiteitnummer: String?,
    val lijnnummer: String?,
    val richting: String?,
    val ritnummer: String?,
    val bestemming: String?,
    val plaatsBestemming: String?,
    val vias: List<String> = emptyList(),
    val dienstregelingTijdstip: String?,
    val realTimeTijdstip: String?,
    val vrtnum: String?,
    val predictionStatussen: List<String> = emptyList()
)

