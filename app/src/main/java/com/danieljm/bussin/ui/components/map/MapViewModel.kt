package com.danieljm.bussin.ui.components.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel responsible for providing the device's live location as a StateFlow.
 * The ViewModel exposes a read-only flow `location` that emits the latest Location or null.
 * Start/stop location updates by calling the corresponding methods from the UI layer.
 */
@HiltViewModel
class MapViewModel @Inject constructor() : ViewModel() {
    private val _location = MutableStateFlow<Location?>(null)
    val location = _location.asStateFlow()

    private var fusedClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null

    /**
     * Starts location updates if the app has been granted location permission. This method
     * defensively checks permissions to avoid SecurityException and returns early otherwise.
     */
    @SuppressLint("MissingPermission")
    fun startLocationUpdates(context: Context) {
        // Check runtime permissions defensively
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) return

        if (locationCallback != null) return // already started

        val appCtx = context.applicationContext
        fusedClient = LocationServices.getFusedLocationProviderClient(appCtx)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                result.lastLocation?.let { loc ->
                    _location.value = loc
                }
            }
        }

        // Use the non-deprecated Builder API
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateIntervalMillis(2000L)
            .setMaxUpdateDelayMillis(5000L)
            .build()

        // Safe to call since we checked permissions above
        fusedClient?.requestLocationUpdates(request, locationCallback!!, Looper.getMainLooper())
    }

    fun stopLocationUpdates() {
        locationCallback?.let { cb ->
            fusedClient?.removeLocationUpdates(cb)
        }
        locationCallback = null
    }

    override fun onCleared() {
        stopLocationUpdates()
        super.onCleared()
    }
}
