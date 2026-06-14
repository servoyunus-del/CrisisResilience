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
    // Soket yönetimi (Dependency Injection ile inject edilebilir, şimdilik manuel ekliyoruz)
    var p2pSocketManager: P2pSocketManager? = null

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
        try {
            manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    android.util.Log.d("WifiDirect", "Keşif başarıyla başlatıldı.")
                }

                override fun onFailure(reasonCode: Int) {
                    val reason = when (reasonCode) {
                        WifiP2pManager.P2P_UNSUPPORTED -> "P2P Desteklenmiyor"
                        WifiP2pManager.ERROR -> "Dahili Hata"
                        WifiP2pManager.BUSY -> "Sistem Meşgul"
                        else -> "Bilinmeyen Hata ($reasonCode)"
                    }
                    android.util.Log.e("WifiDirect", "Keşif başarısız: $reason")
                }
            })
        } catch (e: SecurityException) {
            android.util.Log.e("WifiDirect", "İzin eksik! Keşif başlatılamadı.", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(device: WifiP2pDevice) {
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            // wps.setup ayarları da eklenebilir
        }

        try {
            manager.connect(channel, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    android.util.Log.d("WifiDirect", "Bağlantı isteği gönderildi: ${device.deviceAddress}")
                }

                override fun onFailure(reason: Int) {
                    android.util.Log.e("WifiDirect", "Bağlantı reddedildi veya hata oluştu. Kod: $reason")
                }
            })
        } catch (e: SecurityException) {
            android.util.Log.e("WifiDirect", "İzin eksik! Bağlantı kurulamadı.", e)
        }
    }
    
    // P2P ağından kopmak ve bağlantıyı kesmek için
    @SuppressLint("MissingPermission")
    fun disconnect() {
        try {
            manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    android.util.Log.d("WifiDirect", "Gruptan çıkıldı.")
                    p2pSocketManager?.closeConnections()
                    updateConnectionStatus(false)
                }

                override fun onFailure(reason: Int) {
                    android.util.Log.e("WifiDirect", "Gruptan çıkılamadı, bağlantı iptal ediliyor. Kod: $reason")
                    manager.cancelConnect(channel, null)
                    p2pSocketManager?.closeConnections()
                    updateConnectionStatus(false)
                }
            })
        } catch (e: SecurityException) {
            android.util.Log.e("WifiDirect", "İzin eksik! Bağlantı kesilemedi.", e)
        }
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

    // P2P Bağlantısı kurulduğunda cihazın rolüne göre soket başlatır
    fun handleConnectionInfo(info: android.net.wifi.p2p.WifiP2pInfo) {
        if (info.groupFormed && info.isGroupOwner) {
            // Cihaz Sunucu (Group Owner)
            p2pSocketManager?.startServer()
        } else if (info.groupFormed) {
            // Cihaz İstemci (Client)
            val ownerIp = info.groupOwnerAddress?.hostAddress
            ownerIp?.let {
                p2pSocketManager?.connectAsClient(it)
            }
        }
    }
}
