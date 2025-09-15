package com.danieljm.delijn.data.remote.dto

/** DTO representing a Route from the network */
data class RouteDto(
    val id: String,
    val name: String,
    val stopIds: List<String>
)

