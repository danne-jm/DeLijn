package com.danieljm.delijn.domain.model

/** Domain model for a Route */
data class Route(
    val id: String,
    val name: String,
    val stopIds: List<String>
)

