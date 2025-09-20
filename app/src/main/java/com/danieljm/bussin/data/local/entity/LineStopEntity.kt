package com.danieljm.bussin.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "line_stops")
data class LineStopEntity(
    @PrimaryKey
    val haltenummer: String,
    val entiteitnummer: String?,
    val omschrijving: String?,
    val omschrijvingLang: String?,
    val gemeentenummer: Int?,
    val omschrijvingGemeente: String?,
    val latitude: Double?,
    val longitude: Double?
) {
    companion object {
        fun fromDomain(domain: com.danieljm.bussin.domain.model.LineStop): LineStopEntity {
            return LineStopEntity(
                haltenummer = domain.haltenummer,
                entiteitnummer = domain.entiteitnummer,
                omschrijving = domain.omschrijving,
                omschrijvingLang = domain.omschrijvingLang,
                gemeentenummer = domain.gemeentenummer,
                omschrijvingGemeente = domain.omschrijvingGemeente,
                latitude = domain.latitude,
                longitude = domain.longitude
            )
        }

        fun toDomain(entity: LineStopEntity): com.danieljm.bussin.domain.model.LineStop {
            return com.danieljm.bussin.domain.model.LineStop(
                entiteitnummer = entity.entiteitnummer,
                haltenummer = entity.haltenummer,
                omschrijving = entity.omschrijving,
                omschrijvingLang = entity.omschrijvingLang,
                gemeentenummer = entity.gemeentenummer,
                omschrijvingGemeente = entity.omschrijvingGemeente,
                latitude = entity.latitude,
                longitude = entity.longitude
            )
        }
    }
}

