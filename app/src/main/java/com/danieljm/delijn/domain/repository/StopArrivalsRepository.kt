package com.danieljm.delijn.domain.repository

import com.danieljm.delijn.domain.model.ArrivalInfo
import com.danieljm.delijn.domain.model.ServedLine

interface StopArrivalsRepository {
    suspend fun getRealTimeArrivals(entiteitnummer: String, haltenummer: String, servedLines: List<ServedLine>): List<ArrivalInfo>
    suspend fun getScheduledArrivals(entiteitnummer: String, haltenummer: String, servedLines: List<ServedLine>): List<ArrivalInfo>
}

