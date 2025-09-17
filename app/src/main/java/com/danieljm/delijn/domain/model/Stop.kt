package com.danieljm.delijn.domain.model

data class Stop(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val entiteitnummer: String,
    val halteNummer: String
)
