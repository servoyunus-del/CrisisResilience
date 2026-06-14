package com.emre.crisisresilience.data.repository

import android.util.Log
import com.emre.crisisresilience.data.local.dao.MessageDao
import com.emre.crisisresilience.data.local.entity.MessageEntity
import com.emre.crisisresilience.data.model.MeshMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val messageDao: MessageDao
) {
    private val _isOfflineMode = MutableStateFlow(true)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    fun setOfflineMode(enabled: Boolean) {
        _isOfflineMode.value = enabled
    }

    /**
     * Mesh ağından gelen mesajı Room veritabanına kaydeder.
     * Deduplikasyon: Aynı messageId ile daha önce kaydedilmişse tekrar kaydetmez.
     * @return true eğer mesaj yeni ve kaydedildiyse, false eğer zaten mevcutsa.
     */
    suspend fun saveIncomingMeshMessage(meshMessage: MeshMessage): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Deduplikasyon kontrolü
                if (messageDao.isMessageExists(meshMessage.messageId)) {
                    Log.d("MessageRepository", "Duplike mesaj, atlanıyor: id=${meshMessage.messageId}")
                    return@withContext false
                }

                var senderName = meshMessage.originMac
                var statusMessage = meshMessage.payload
                var customText = ""

                try {
                    val obj = org.json.JSONObject(meshMessage.payload)
                    if (obj.has("status")) {
                        statusMessage = obj.getString("status")
                        val name = obj.optString("name", "")
                        if (name.isNotBlank()) {
                            senderName = "$name (${meshMessage.originMac})"
                        }
                        
                        val bloodType = obj.optString("bloodType", "")
                        val emergencyNote = obj.optString("emergencyNote", "")
                        val locationDesc = obj.optString("locationDesc", "")
                        
                        val sb = java.lang.StringBuilder()
                        if (bloodType.isNotBlank()) sb.append("Kan Grubu: $bloodType\n")
                        if (emergencyNote.isNotBlank()) sb.append("Acil Not: $emergencyNote\n")
                        if (locationDesc.isNotBlank()) sb.append("Konum Açıklaması: $locationDesc")
                        
                        customText = sb.toString().trim()
                    }
                } catch (e: Exception) {
                    // JSON formatında değilse legacy fallback
                }

                val entity = MessageEntity(
                    messageId = meshMessage.messageId,
                    senderName = senderName,
                    statusMessage = statusMessage,
                    timestamp = meshMessage.timestamp,
                    latitude = 0.0,
                    longitude = 0.0,
                    isRelayed = meshMessage.hopCount > 0,
                    customText = customText,
                    isIncoming = true,
                    hopCount = meshMessage.hopCount
                )
                messageDao.insertMessage(entity)
                Log.d("MessageRepository", "MeshMessage kaydedildi: Gönderen=$senderName, Hop=${meshMessage.hopCount}, Mesaj=$statusMessage")

                if (!isOfflineMode.value) {
                    // TODO: İnternet varsa senkronizasyon (Sync) yapılabilir.
                    Log.d("MessageRepository", "Ağ bağlantısı açık, uzak sunucuya senkronizasyon yapılabilir.")
                }

                return@withContext true
            } catch (e: Exception) {
                Log.e("MessageRepository", "MeshMessage kaydedilemedi!", e)
                return@withContext false
            }
        }
    }

    /**
     * Eski API uyumluluğu: Düz metin mesajlar için.
     */
    suspend fun saveIncomingMessage(senderMac: String, payload: String) {
        withContext(Dispatchers.IO) {
            try {
                val entity = MessageEntity(
                    senderName = senderMac,
                    statusMessage = payload,
                    timestamp = System.currentTimeMillis(),
                    latitude = 0.0,
                    longitude = 0.0,
                    isRelayed = true,
                    isIncoming = true
                )
                messageDao.insertMessage(entity)
                Log.d("MessageRepository", "Mesaj Room DB'ye kaydedildi: Gönderen=$senderMac, Mesaj=$payload")
            } catch (e: Exception) {
                Log.e("MessageRepository", "Mesaj kaydedilemedi!", e)
            }
        }
    }
}
