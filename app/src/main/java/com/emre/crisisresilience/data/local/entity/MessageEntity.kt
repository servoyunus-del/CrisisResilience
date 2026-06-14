package com.emre.crisisresilience.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val messageId: String = "",
    val senderName: String,
    val statusMessage: String,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val isRelayed: Boolean = false,
    val customText: String = "",
    val isIncoming: Boolean = true,
    val hopCount: Int = 0
)
