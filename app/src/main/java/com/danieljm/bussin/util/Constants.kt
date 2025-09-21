package com.danieljm.bussin.util

object Constants {
    // Use local proxy base URL. All De Lijn API requests will be routed through this proxy.
    // The proxy exposes multiple downstream APIs under /api/delijn/... as described in the task.

    const val BASE_URL = "https://choice-sweet-tiger.ngrok-free.app/"

    // For Core and Search APIs
    const val API_KEY = "f74c8e50b3364c6487355ce677d4a857"

    // For Realtime APIs (GTFS)
    const val REALTIME_API_KEY = "5eacdcf7e85c4637a14f4d627403935a"
}