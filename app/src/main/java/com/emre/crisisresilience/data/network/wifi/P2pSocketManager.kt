package com.emre.crisisresilience.data.network.wifi

import android.util.Log
import com.emre.crisisresilience.data.local.dao.MessageDao
import com.emre.crisisresilience.data.local.entity.MessageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class P2pSocketManager(
    private val messageDao: MessageDao
) {
    private var serverJob: Job? = null
    private var clientJob: Job? = null
    
    private var activeSocket: Socket? = null
    private var serverSocket: ServerSocket? = null
    
    private val scope = CoroutineScope(Dispatchers.IO)
    private val port = 8888

    // 1) Group Owner (Sunucu) Motoru
    fun startServer() {
        closeConnections()
        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(port)
                Log.d("P2pSocketManager", "Server (Group Owner) başlatıldı. Port: $port üzerinden bekleniyor...")
                
                while (isActive) {
                    val client = serverSocket?.accept() // Bloklayan I/O çağrısı (Dispatchers.IO'da güvenli)
                    client?.let {
                        Log.d("P2pSocketManager", "İstemci bağlandı: ${it.inetAddress.hostAddress}")
                        activeSocket = it
                        listenForMessages(it.getInputStream())
                    }
                }
            } catch (e: Exception) {
                Log.e("P2pSocketManager", "Server hatası: ${e.message}")
            }
        }
    }

    // 2) İstemci (Client) Motoru
    fun connectAsClient(hostAddress: String) {
        closeConnections()
        clientJob = scope.launch {
            try {
                val socket = Socket()
                socket.bind(null)
                Log.d("P2pSocketManager", "Group Owner'a bağlanılıyor: $hostAddress:$port")
                socket.connect(InetSocketAddress(hostAddress, port), 5000) // 5sn timeout
                Log.d("P2pSocketManager", "Bağlantı başarılı!")
                
                activeSocket = socket
                listenForMessages(socket.getInputStream())
                
            } catch (e: Exception) {
                Log.e("P2pSocketManager", "Client bağlantı hatası: ${e.message}")
            }
        }
    }

    // 3) Gelen Veri Akışını Dinleme ve Room DB Kaydı
    private suspend fun listenForMessages(inputStream: InputStream) {
        withContext(Dispatchers.IO) {
            try {
                val buffer = ByteArray(4096) // JSON paketleri için tampon
                var bytes: Int

                while (isActive) {
                    bytes = inputStream.read(buffer)
                    if (bytes == -1) {
                        Log.d("P2pSocketManager", "Soket bağlantısı koptu veya kapatıldı.")
                        break
                    }
                    
                    val messageString = String(buffer, 0, bytes)
                    Log.d("P2pSocketManager", "Soketten yeni veri ulaştı: $messageString")
                    
                    parseAndSaveMessage(messageString)
                }
            } catch (e: Exception) {
                Log.e("P2pSocketManager", "Mesaj dinleme esnasında hata: ${e.message}")
            }
        }
    }

    // JSON ayrıştırma ve Room Dao üzerinden kayıt
    private suspend fun parseAndSaveMessage(jsonString: String) {
        try {
            val jsonObject = JSONObject(jsonString)
            
            val entity = MessageEntity(
                senderName = jsonObject.optString("senderName", "Bilinmeyen Cihaz"),
                statusMessage = jsonObject.optString("statusMessage", ""),
                timestamp = jsonObject.optLong("timestamp", System.currentTimeMillis()),
                latitude = jsonObject.optDouble("latitude", 0.0),
                longitude = jsonObject.optDouble("longitude", 0.0),
                isRelayed = true // Mesh ağında dışarıdan geldiği için relayed true
            )
            
            messageDao.insertMessage(entity)
            Log.d("P2pSocketManager", "Gelen veri başarıyla Room DB'ye işlendi!")
            
        } catch (e: Exception) {
            Log.e("P2pSocketManager", "JSON ayrıştırma hatası: ${e.message}")
        }
    }

    // 4) Mesh Ağına Veri Fırlatma (Gönderme)
    fun sendMessage(messageEntity: MessageEntity) {
        scope.launch {
            try {
                val outputStream: OutputStream? = activeSocket?.getOutputStream()
                if (outputStream != null) {
                    
                    val jsonObject = JSONObject().apply {
                        put("senderName", messageEntity.senderName)
                        put("statusMessage", messageEntity.statusMessage)
                        put("timestamp", messageEntity.timestamp)
                        put("latitude", messageEntity.latitude)
                        put("longitude", messageEntity.longitude)
                        put("isRelayed", messageEntity.isRelayed)
                    }
                    
                    val bytes = jsonObject.toString().toByteArray()
                    outputStream.write(bytes)
                    Log.d("P2pSocketManager", "Mesaj soket üzerinden karşı tarafa iletildi!")
                    
                } else {
                    Log.e("P2pSocketManager", "Aktif soket bağlantısı bulunmadığı için mesaj gönderilemedi.")
                }
            } catch (e: Exception) {
                Log.e("P2pSocketManager", "Mesaj gönderme hatası: ${e.message}")
            }
        }
    }
    
    fun closeConnections() {
        serverJob?.cancel()
        clientJob?.cancel()
        try {
            activeSocket?.close()
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e("P2pSocketManager", "Bağlantıları kapatırken hata oluştu: ${e.message}")
        }
    }
}
