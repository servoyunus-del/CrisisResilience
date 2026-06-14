package com.emre.crisisresilience.ui.screens.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emre.crisisresilience.data.local.dao.MessageDao
import com.emre.crisisresilience.data.local.entity.MessageEntity
import com.emre.crisisresilience.data.location.LocationTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val messageDao: MessageDao,
    private val locationTracker: LocationTracker
) : ViewModel() {

    // Room'daki tüm mesajları haritada marker olarak göstermek için
    val messages: StateFlow<List<MessageEntity>> = messageDao.getAllMessagesFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Kullanıcının anlık konumu
    private val _userLatitude = MutableStateFlow(39.9334)  // Varsayılan: Ankara
    val userLatitude: StateFlow<Double> = _userLatitude.asStateFlow()

    private val _userLongitude = MutableStateFlow(32.8597)
    val userLongitude: StateFlow<Double> = _userLongitude.asStateFlow()

    init {
        fetchCurrentLocation()
    }

    fun fetchCurrentLocation() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val location = locationTracker.getCurrentLocation()
                if (location != null) {
                    _userLatitude.value = location.latitude
                    _userLongitude.value = location.longitude
                    Log.d("MapViewModel", "Konum alındı: ${location.latitude}, ${location.longitude}")
                } else {
                    Log.d("MapViewModel", "Konum alınamadı, varsayılan kullanılıyor.")
                }
            } catch (e: Exception) {
                Log.e("MapViewModel", "Konum alınırken hata.", e)
            }
        }
    }
}
