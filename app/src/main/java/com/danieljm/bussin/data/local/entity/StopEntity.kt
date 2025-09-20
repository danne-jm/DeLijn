package com.danieljm.bussin.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stops")
data class StopEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val distance: Int?,
    val latitude: Double?,
    val longitude: Double?
) {
    companion object {
        fun fromDomain(stop: com.danieljm.bussin.domain.model.Stop): StopEntity {
            val domainId = stop.id
            val domainName = stop.name
            val domainDistance = stop.distance
            val domainLatitude = stop.latitude
            val domainLongitude = stop.longitude
            return StopEntity(
                id = domainId,
                name = domainName,
                distance = domainDistance,
                latitude = domainLatitude,
                longitude = domainLongitude
            )
        }

        fun toDomain(entity: StopEntity): com.danieljm.bussin.domain.model.Stop {
            return com.danieljm.bussin.domain.model.Stop(
                id = entity.id,
                name = entity.name,
                distance = entity.distance,
                latitude = entity.latitude,
                longitude = entity.longitude
            )
        }
    }
}
