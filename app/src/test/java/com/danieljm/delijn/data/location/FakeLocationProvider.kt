package com.danieljm.delijn.data.location

import android.location.Location
import com.google.android.gms.location.LocationRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Simple fake LocationProvider for unit tests.
 * Tests can call [emitLocation] to push locations into the flow.
 */
class FakeLocationProvider : LocationProvider {
    private val _flow = MutableSharedFlow<Location>(replay = 1)
    val flow = _flow.asSharedFlow()

    override fun locationUpdates(request: LocationRequest?): Flow<Location> = flow

    override suspend fun getLastKnownLocation(): Location? {
        return _flow.replayCache.firstOrNull()
    }

    suspend fun emitLocation(location: Location) {
        _flow.emit(location)
    }
}

