package com.emre.crisisresilience.data.network.wifi

import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WifiDirectManager(
    private val context: Context,
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel
) {
    // Çevredeki cihazların listesini UI'a StateFlow olarak sunar
    private val _peers = MutableStateFlow<List<WifiP2pDevice>>(emptyList())
    val peers: StateFlow<List<WifiP2pDevice>> = _peers.asStateFlow()

    // Cihazın herhangi bir Wi-Fi Direct ağına bağlı olup olmadığı bilgisi
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    // Kendi cihazımızın P2P üzerindeki durumu
    private val _thisDevice = MutableStateFlow<WifiP2pDevice?>(null)
    val thisDevice: StateFlow<WifiP2pDevice?> = _thisDevice.asStateFlow()

    private var receiver: WiFiDirectBroadcastReceiver? = null

    // Sadece bizi ilgilendiren Wi-Fi P2P Intent'lerini filtrelere ekliyoruz
    val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    fun registerReceiver() {
        if (receiver == null) {
            receiver = WiFiDirectBroadcastReceiver(manager, channel, this)
            context.registerReceiver(receiver, intentFilter)
        }
    }

    fun unregisterReceiver() {
        receiver?.let {
            context.unregisterReceiver(it)
            receiver = null
        }
    }

    @SuppressLint("MissingPermission")
    fun discoverPeers() {
        // Cihaz keşfini başlatıyoruz
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // Keşif başarıyla başladı, Receiver "PEERS_CHANGED" alacak
            }

            override fun onFailure(reasonCode: Int) {
                // Keşif başlatılamadı, hata yönetimi eklenebilir
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun connect(device: WifiP2pDevice) {
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            // wps.setup ayarları da eklenebilir
        }

        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // Bağlantı isteği gönderildi
            }

            override fun onFailure(reason: Int) {
                // Bağlantı isteği reddedildi veya hata oluştu
            }
        })
    }
    
    // Broadcast Receiver üzerinden tetiklenen güncelleyici fonksiyonlar
    fun updatePeers(deviceList: Collection<WifiP2pDevice>) {
        _peers.value = deviceList.toList()
    }

    fun updateConnectionStatus(connected: Boolean) {
        _isConnected.value = connected
    }
    
    fun updateThisDevice(device: WifiP2pDevice) {
        _thisDevice.value = device
    }
}
