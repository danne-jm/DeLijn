package com.danieljm.delijn.domain.repository

import com.danieljm.delijn.domain.model.Bus

interface BusRepository {
    suspend fun getBusDetails(busId: String): Bus?
}

