package com.emre.crisisresilience.data.model

import org.json.JSONObject
import java.util.UUID

/**
 * Mesh ağı üzerinden iletilen mesajların veri modeli.
 * Her mesaj benzersiz bir [messageId] ile tanımlanır ve [hopCount] ile
 * kaç kez sıçradığı takip edilir. Bu sayede sonsuz döngü engellenir.
 */
data class MeshMessage(
    val messageId: String = UUID.randomUUID().toString(),
    val originMac: String,      // Orijinal gönderen MAC adresi
    val senderMac: String,      // Son gönderen (relay yapan) MAC adresi
    val payload: String,        // Mesaj içeriği (GÜVENDEYİM, YARDIM LAZIM vs.)
    val timestamp: Long = System.currentTimeMillis(),
    val hopCount: Int = 0       // Kaç kez sıçradığı
) {
    companion object {
        const val MAX_HOP = 5

        fun fromJson(json: String): MeshMessage? {
            return try {
                val obj = JSONObject(json)
                MeshMessage(
                    messageId = obj.getString("messageId"),
                    originMac = obj.getString("originMac"),
                    senderMac = obj.getString("senderMac"),
                    payload = obj.getString("payload"),
                    timestamp = obj.getLong("timestamp"),
                    hopCount = obj.getInt("hopCount")
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    fun toJson(): String {
        return JSONObject().apply {
            put("messageId", messageId)
            put("originMac", originMac)
            put("senderMac", senderMac)
            put("payload", payload)
            put("timestamp", timestamp)
            put("hopCount", hopCount)
        }.toString()
    }

    /**
     * Relay (sıçratma) için yeni bir kopya oluşturur.
     * hopCount bir artar ve senderMac güncellenir.
     */
    fun forRelay(newSenderMac: String): MeshMessage {
        return copy(
            senderMac = newSenderMac,
            hopCount = hopCount + 1
        )
    }

    fun canRelay(): Boolean = hopCount < MAX_HOP
}
