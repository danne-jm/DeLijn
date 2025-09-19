package com.danieljm.delijn.data.location

import android.location.Location
import com.google.android.gms.location.LocationRequest
import kotlinx.coroutines.flow.Flow

/**
 * Provides location updates as a Flow and allows fetching the last known location.
 *
 * Note: This interface returns android.location.Location to remain compatible with the
 * existing UI/ViewModel code in this project. If you prefer a platform-agnostic domain model,
 * replace the Location types here and map in the data layer.
 */
interface LocationProvider {
    fun locationUpdates(request: LocationRequest? = null): Flow<Location>
    suspend fun getLastKnownLocation(): Location?
}

