package com.danieljm.delijn.data.location

import android.location.Location
import com.google.android.gms.location.LocationRequest
import kotlinx.coroutines.flow.Flow

/**
 * Platform boundary interface for providing location updates.
 * Placed under `platform` to keep the domain module free from platform-specific concerns
 * while allowing UI/ViewModels to depend on a stable contract.
 */
interface LocationProvider {
    fun locationUpdates(request: LocationRequest? = null): Flow<Location>
    suspend fun getLastKnownLocation(): Location?
}