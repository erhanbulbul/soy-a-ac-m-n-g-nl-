package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val dateString: String, // e.g. "23 Temmuz 2026, Perşembe"
    val timeString: String, // e.g. "14:30"
    val deviceLocationInfo: String = "İstanbul, Türkiye • Android Device",
    val textColorHex: String = "#FFB800", // Dynamic font color
    val imageUris: String = "", // Comma-separated list of up to 5 image URIs
    val audioNotePath: String? = null,
    val voiceIntervalSeconds: Int = 0,
    val mood: String = "Serene",
    val timestamp: Long = System.currentTimeMillis()
)
