package com.emre.crisisresilience.ui.screens.chat

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emre.crisisresilience.data.local.dao.MessageDao
import com.emre.crisisresilience.data.local.entity.MessageEntity
import com.emre.crisisresilience.data.network.wifi.P2pSocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.emre.crisisresilience.data.location.LocationTracker

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageDao: MessageDao,
    private val p2pSocketManager: P2pSocketManager,
    private val locationTracker: LocationTracker
) : ViewModel() {

    // Veritabanındaki tüm mesajları akış olarak dinler
    val messagesFlow: Flow<List<MessageEntity>> = messageDao.getAllMessagesFlow()

    // Mesaj gönderme işlemi
    fun sendMessage(text: String, myStatus: String) {
        if (text.isBlank()) return

        val myDeviceName = Build.MODEL ?: "Bilinmeyen Cihaz"

        viewModelScope.launch {
            // Arka planda gerçek GPS konumunu çek
            val location = locationTracker.getCurrentLocation()
            val lat = location?.latitude ?: 0.0
            val lon = location?.longitude ?: 0.0

            val newMessage = MessageEntity(
                senderName = myDeviceName,
                statusMessage = myStatus,
                timestamp = System.currentTimeMillis(),
                latitude = lat,
                longitude = lon,
                customText = text,
                isIncoming = false
            )

            // 1. Önce kendi veritabanımıza kaydet (Ekranda görünsün)
            messageDao.insertMessage(newMessage)
            
            // 2. Ardından Wi-Fi P2P Soket motoru üzerinden karşı cihaza gönder
            p2pSocketManager.sendMessage(newMessage)
        }
    }
}
