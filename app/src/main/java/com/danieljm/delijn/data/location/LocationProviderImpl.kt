package com.danieljm.delijn.data.location

import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Implementation of [LocationProvider] that uses FusedLocationProviderClient to stream location updates.
 * Emits the last known location immediately (if available) and then continuous updates.
 */
class LocationProviderImpl(
    private val fusedLocationClient: FusedLocationProviderClient
) : LocationProvider {

    @SuppressLint("MissingPermission")
    override fun locationUpdates(request: LocationRequest?): Flow<Location> = callbackFlow {
        val locRequest = request ?: LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .setMaxUpdateDelayMillis(5000)
            .setWaitForAccurateLocation(false)
            .build()

        // Try to emit last known location immediately using Task callbacks
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) trySend(location).isSuccess
                }
                .addOnFailureListener { ex ->
                    Log.w("LocationProviderImpl", "lastLocation failed: ${ex.message}")
                }
        } catch (e: SecurityException) {
            Log.w("LocationProviderImpl", "Permission missing for lastLocation: ${e.message}")
        } catch (e: Exception) {
            Log.w("LocationProviderImpl", "Exception while fetching lastLocation: ${e.message}")
        }

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                trySend(loc).isSuccess
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(locRequest, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.w("LocationProviderImpl", "Permission missing for requestLocationUpdates: ${e.message}")
        }

        awaitClose {
            try {
                fusedLocationClient.removeLocationUpdates(callback)
            } catch (e: Exception) {
                Log.w("LocationProviderImpl", "Failed to remove location updates: ${e.message}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun getLastKnownLocation(): Location? {
        return try {
            suspendCancellableCoroutine { cont ->
                val task = fusedLocationClient.lastLocation
                task.addOnCompleteListener { t ->
                    if (t.isSuccessful) {
                        cont.resume(t.result)
                    } else {
                        cont.resume(null)
                    }
                }
                task.addOnFailureListener { ex ->
                    if (!cont.isCompleted) cont.resume(null)
                }
                cont.invokeOnCancellation {
                    // No explicit cancellation on Task available here
                }
            }
        } catch (e: SecurityException) {
            Log.w("LocationProviderImpl", "Permission missing for getLastKnownLocation: ${e.message}")
            null
        } catch (e: Exception) {
            Log.w("LocationProviderImpl", "getLastKnownLocation failed: ${e.message}")
            null
        }
    }
}
