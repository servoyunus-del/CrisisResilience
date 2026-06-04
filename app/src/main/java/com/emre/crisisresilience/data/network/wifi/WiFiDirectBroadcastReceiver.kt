package com.emre.crisisresilience.data.network.wifi

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager

class WiFiDirectBroadcastReceiver(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val wifiDirectManager: WifiDirectManager
) : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                // Wi-Fi P2P açık mı kapalı mı? 
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                val isWifiP2pEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                // İhtiyaca göre bu durum wifiDirectManager üzerinden StateFlow'a aktarılabilir
            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                // Çevredeki cihaz listesi değişti
                manager.requestPeers(channel) { peerList ->
                    wifiDirectManager.updatePeers(peerList.deviceList)
                }
            }

            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                // Cihazın P2P ağ bağlantı durumu değişti (bağlandık veya koptuk)
                @Suppress("DEPRECATION")
                val networkInfo: NetworkInfo? = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO)
                
                if (networkInfo?.isConnected == true) {
                    wifiDirectManager.updateConnectionStatus(true)
                    
                    // Bağlantı kurulduğunda cihazın rolünü (Owner/Client) öğren ve Soketi başlat
                    manager.requestConnectionInfo(channel) { info ->
                        wifiDirectManager.handleConnectionInfo(info)
                    }
                } else {
                    wifiDirectManager.updateConnectionStatus(false)
                    wifiDirectManager.p2pSocketManager?.closeConnections()
                }
            }

            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                // Kendi cihazımızın durumu/ismi/bilgileri değişti
                @Suppress("DEPRECATION")
                val device: WifiP2pDevice? = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                device?.let {
                    wifiDirectManager.updateThisDevice(it)
                }
            }
        }
    }
}
