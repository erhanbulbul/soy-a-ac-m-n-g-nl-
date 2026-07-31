package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val familyMemberId: Long, // Linked family member ID
    val senderName: String,
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFromUser: Boolean = true,
    val isAiAvatarResponse: Boolean = false,
    val audioNoteUrl: String? = null
)
