package com.emre.crisisresilience.ui.screens.home

import android.content.Context
import android.net.wifi.p2p.WifiP2pDevice
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emre.crisisresilience.data.local.dao.MessageDao
import com.emre.crisisresilience.data.local.entity.MessageEntity
import com.emre.crisisresilience.data.network.wifi.WifiDirectManager
import com.emre.crisisresilience.data.network.bluetooth.BleManager
import com.emre.crisisresilience.data.model.MeshMessage
import com.emre.crisisresilience.data.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val wifiDirectManager: WifiDirectManager,
    private val bleManager: BleManager,
    private val messageRepository: MessageRepository,
    private val messageDao: MessageDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("user_profile", Context.MODE_PRIVATE)

    private val _toastEvent = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    val incomingMessages: StateFlow<List<MessageEntity>> = messageDao.getAllMessagesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _userName = MutableStateFlow(prefs.getString("user_name", "") ?: "")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userBloodType = MutableStateFlow(prefs.getString("user_blood_type", "") ?: "")
    val userBloodType: StateFlow<String> = _userBloodType.asStateFlow()

    private val _userEmergencyNote = MutableStateFlow(prefs.getString("user_emergency_note", "") ?: "")
    val userEmergencyNote: StateFlow<String> = _userEmergencyNote.asStateFlow()

    private val _userLocationDesc = MutableStateFlow(prefs.getString("user_location_desc", "") ?: "")
    val userLocationDesc: StateFlow<String> = _userLocationDesc.asStateFlow()

    fun updateProfile(name: String, bloodType: String, emergencyNote: String, locationDesc: String) {
        _userName.value = name
        _userBloodType.value = bloodType
        _userEmergencyNote.value = emergencyNote
        _userLocationDesc.value = locationDesc

        prefs.edit().apply {
            putString("user_name", name)
            putString("user_blood_type", bloodType)
            putString("user_emergency_note", emergencyNote)
            putString("user_location_desc", locationDesc)
            apply()
        }

        // Profil güncellendiğinde, mevcut tüm bulunan cihazlara yeni bilgileri otomatik gönder
        val currentPeers = bleManager.discoveredDevices.value
        viewModelScope.launch(Dispatchers.IO) {
            for (peerMac in currentPeers) {
                kotlinx.coroutines.delay(200)
                connectToBleDevice(peerMac)
            }
        }
    }

    private fun getPersonalPayload(status: String): String {
        return org.json.JSONObject().apply {
            put("status", status)
            put("name", userName.value)
            put("bloodType", userBloodType.value)
            put("emergencyNote", userEmergencyNote.value)
            put("locationDesc", userLocationDesc.value)
        }.toString()
    }

    // Çevredeki cihazlar
    val peers: StateFlow<List<WifiP2pDevice>> = wifiDirectManager.peers

    // BLE üzerinden bulunan cihazların listesi (MAC adresleri)
    val blePeers: StateFlow<List<String>> = bleManager.discoveredDevices

    // Offline Mode State
    val isOfflineMode: StateFlow<Boolean> = messageRepository.isOfflineMode

    init {
        // BLE üzerinden gelen MeshMessage'ları IO thread'de dinle, kaydet ve relay et
        viewModelScope.launch(Dispatchers.IO) {
            bleManager.receivedMessages.collect { messages ->
                val lastMessage = messages.lastOrNull()
                if (lastMessage != null) {
                    // 1. Veritabanına kaydet (deduplikasyon MessageRepository'de yapılır)
                    val isNew = messageRepository.saveIncomingMeshMessage(lastMessage)

                    // 2. Yeni mesaj ise, ekranda bildir ve diğer cihazlara sıçrat
                    if (isNew) {
                        var name = lastMessage.originMac
                        var status = lastMessage.payload
                        try {
                            val obj = org.json.JSONObject(lastMessage.payload)
                            if (obj.has("status")) {
                                status = obj.getString("status")
                                val profileName = obj.optString("name", "")
                                if (profileName.isNotBlank()) {
                                    name = profileName
                                }
                            }
                        } catch (e: Exception) {}
                        
                        _toastEvent.tryEmit("Yeni Mesaj: $name -> $status")

                        if (lastMessage.canRelay()) {
                            runCatching {
                                bleManager.relayMessageToAllPeers(lastMessage)
                            }.onFailure { e ->
                                Log.e("HomeViewModel", "Relay Hatası", e)
                            }
                        }
                    }
                }
            }
        }

        // Yeni cihaz keşfedildiğinde otomatik olarak acil durum ve profil bilgimizi gönder
        viewModelScope.launch(Dispatchers.IO) {
            var lastPeers = emptySet<String>()
            bleManager.discoveredDevices.collect { currentPeers ->
                val newPeers = currentPeers.filter { !lastPeers.contains(it) }
                lastPeers = currentPeers.toSet()
                for (peerMac in newPeers) {
                    Log.d("HomeViewModel", "Otomatik taramada yeni cihaz keşfedildi, bilgi gönderiliyor: $peerMac")
                    kotlinx.coroutines.delay(800)
                    connectToBleDevice(peerMac)
                }
            }
        }
    }

    // P2P Bağlantı durumu
    val isConnected: StateFlow<Boolean> = wifiDirectManager.isConnected

    // Kullanıcının anlık acil durum statüsü
    private val _myStatus = MutableStateFlow("BİLİNMİYOR")
    val myStatus: StateFlow<String> = _myStatus.asStateFlow()

    fun updateStatus(newStatus: String) {
        _myStatus.value = newStatus
        // Durum değiştiğinde, mevcut tüm bulunan cihazlara yeni bilgiyi otomatik gönder
        val currentPeers = bleManager.discoveredDevices.value
        viewModelScope.launch(Dispatchers.IO) {
            for (peerMac in currentPeers) {
                kotlinx.coroutines.delay(200)
                connectToBleDevice(peerMac)
            }
        }
    }

    fun toggleOfflineMode(enabled: Boolean) {
        messageRepository.setOfflineMode(enabled)
    }

    fun discoverPeers() {
        // Broadcast receiver'ı kaydet ve taramayı başlat
        wifiDirectManager.registerReceiver()
        wifiDirectManager.discoverPeers()

        // BLE Fallback mekanizmasını da başlat (Crash-proof)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                bleManager.startAdvertising()
                bleManager.startScanning()
            }.onFailure { e ->
                Log.e("HomeViewModel", "BLE Başlatma Hatası", e)
            }
        }
    }

    fun connectToDevice(device: WifiP2pDevice) {
        wifiDirectManager.connect(device)
        // Wi-Fi P2P meşgul olursa BLE fallback olarak da dene
        connectToBleDevice(device.deviceAddress)
    }

    fun connectToBleDevice(macAddress: String) {
        val payload = getPersonalPayload(_myStatus.value)
        viewModelScope.launch(Dispatchers.IO) {
            _toastEvent.tryEmit("Bilgiler gönderiliyor...")
            bleManager.connectToDeviceAndSendMessage(
                macAddress = macAddress,
                message = payload,
                onSuccess = {
                    _toastEvent.tryEmit("Bilgiler başarıyla gönderildi!")
                },
                onFailure = { e ->
                    Log.e("HomeViewModel", "BLE Bağlantı Hatası: $macAddress", e)
                    _toastEvent.tryEmit("Bağlantı başarısız oldu!")
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        // ViewModel ölürken receiver'ı temizle ve BLE'yi durdur
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                bleManager.stopAdvertising()
                bleManager.stopScanning()
            }.onFailure { e ->
                Log.e("HomeViewModel", "BLE Kapatma Hatası", e)
            }
        }
        // wifiDirectManager.unregisterReceiver()
    }
}
