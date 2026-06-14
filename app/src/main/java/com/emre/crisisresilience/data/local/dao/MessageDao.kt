package com.emre.crisisresilience.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.emre.crisisresilience.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessage(message: MessageEntity)

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessagesFlow(): Flow<List<MessageEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM messages WHERE messageId = :messageId LIMIT 1)")
    fun isMessageExists(messageId: String): Boolean

    @Query("DELETE FROM messages")
    fun clearAllMessages()
}
