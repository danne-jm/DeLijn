package com.danieljm.delijn.domain.model

data class LineDirection(
    val entiteitnummer: String,
    val lijnnummer: String,
    val richting: String,
    val omschrijving: String,
    val from: String,
    val to: String
)

data class LineDirectionsResponse(
    val lijnrichtingen: List<LineDirection>
)
