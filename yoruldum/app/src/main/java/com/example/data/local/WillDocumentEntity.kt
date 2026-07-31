package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "will_documents")
data class WillDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String = "Vasiyetname", // Vasiyetname, Tapu/Belge, Mektup, Banka/Miras Notu, Fotoğraf
    val documentImageUri: String,
    val description: String = "",
    val recipientFamilyMemberIds: String = "", // Comma-separated list of family member IDs
    val recipientNames: String = "", // Comma-separated display names
    val isEncrypted: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)
