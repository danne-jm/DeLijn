package com.danieljm.bussin.data.mapper

import com.danieljm.bussin.data.remote.dto.DoorkomstDto
import com.danieljm.bussin.data.remote.dto.HalteDoorkomstenDto
import com.danieljm.bussin.domain.model.Arrival
import com.danieljm.bussin.domain.model.HalteDoorkomsten

object ArrivalsMapper {
    fun fromDoorkomst(dto: DoorkomstDto): Arrival {
        return Arrival(
            doorkomstId = dto.doorkomstId,
            entiteitnummer = dto.entiteitnummer,
            lijnnummer = dto.lijnnummer,
            richting = dto.richting,
            ritnummer = dto.ritnummer,
            bestemming = dto.bestemming,
            plaatsBestemming = dto.plaatsBestemming,
            vias = dto.vias ?: emptyList(),
            dienstregelingTijdstip = dto.dienstregelingTijdstip,
            realTimeTijdstip = dto.realTimeTijdstip,
            vrtnum = dto.vrtnum,
            predictionStatussen = dto.predictionStatussen ?: emptyList()
        )
    }

    fun fromHalteDoorkomsten(dto: HalteDoorkomstenDto): HalteDoorkomsten {
        val arrivals = dto.doorkomsten?.map { fromDoorkomst(it) } ?: emptyList()
        return HalteDoorkomsten(haltenummer = dto.haltenummer, doorkomsten = arrivals)
    }
}

