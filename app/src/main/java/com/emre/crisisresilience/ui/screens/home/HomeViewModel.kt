package com.emre.crisisresilience.ui.screens.home

import android.net.wifi.p2p.WifiP2pDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emre.crisisresilience.data.network.wifi.WifiDirectManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val wifiDirectManager: WifiDirectManager
) : ViewModel() {

    // Çevredeki cihazlar
    val peers: StateFlow<List<WifiP2pDevice>> = wifiDirectManager.peers
    
    // P2P Bağlantı durumu
    val isConnected: StateFlow<Boolean> = wifiDirectManager.isConnected

    // Kullanıcının anlık acil durum statüsü
    private val _myStatus = MutableStateFlow("BİLİNMİYOR")
    val myStatus: StateFlow<String> = _myStatus.asStateFlow()

    fun updateStatus(newStatus: String) {
        _myStatus.value = newStatus
    }

    fun discoverPeers() {
        // Broadcast receiver'ı kaydet ve taramayı başlat
        wifiDirectManager.registerReceiver()
        wifiDirectManager.discoverPeers()
    }

    fun connectToDevice(device: WifiP2pDevice) {
        wifiDirectManager.connect(device)
    }

    override fun onCleared() {
        super.onCleared()
        // ViewModel ölürken receiver'ı temizle (opsiyonel, genelde MainActivity'de yapılır)
        // wifiDirectManager.unregisterReceiver()
    }
}
