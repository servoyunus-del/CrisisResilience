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

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageDao: MessageDao,
    private val p2pSocketManager: P2pSocketManager
) : ViewModel() {

    // Veritabanındaki tüm mesajları akış olarak dinler
    val messagesFlow: Flow<List<MessageEntity>> = messageDao.getAllMessagesFlow()

    // Mesaj gönderme işlemi
    fun sendMessage(text: String, myStatus: String) {
        if (text.isBlank()) return

        val myDeviceName = Build.MODEL ?: "Bilinmeyen Cihaz"

        val newMessage = MessageEntity(
            senderName = myDeviceName,
            statusMessage = myStatus,
            timestamp = System.currentTimeMillis(),
            latitude = 0.0, // GPS entegrasyonu Sprint 6'ya bırakıldı
            longitude = 0.0,
            customText = text,
            isIncoming = false
        )

        viewModelScope.launch {
            // 1. Önce kendi veritabanımıza kaydet (Ekranda görünsün)
            messageDao.insertMessage(newMessage)
            
            // 2. Ardından Wi-Fi P2P Soket motoru üzerinden karşı cihaza gönder
            p2pSocketManager.sendMessage(newMessage)
        }
    }
}
