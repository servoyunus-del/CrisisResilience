package com.emre.crisisresilience.data.location

import android.location.Location

interface LocationTracker {
    suspend fun getCurrentLocation(): Location?
}
