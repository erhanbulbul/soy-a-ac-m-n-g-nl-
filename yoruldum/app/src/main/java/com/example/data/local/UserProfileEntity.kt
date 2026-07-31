package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String = "primary_user",
    val name: String = "Anna Yılmaz",
    val email: String = "anna@soyaagaci.com",
    val userCode: String = "", // Dynamic Firebase Auth UID
    val avatarUri: String = "",
    val bio: String = "Ailesinin ve mirasının izinde bir anı biriktiricisi.",
    val birthDate: String = "15.05.1985",
    val phone: String = "+90 555 123 4567",
    val authProvider: String = "Google", // Google, Facebook, Twitter, LinkedIn, Email
    val languageCode: String = "tr", // tr, en, es, de, fr, ar
    val preferredTextColorHex: String = "#FFB800", // Gold writing text color default
    val address: String = "İstanbul, Türkiye",
    val bloodType: String = "A Rh+",
    val isDeceased: Boolean = false,
    val lastActiveTimestamp: Long = System.currentTimeMillis(),
    val isSafetyCheckActive: Boolean = true,
    val safetyCheckHoursRemaining: Int = 24,
    val isLoggedIn: Boolean = false
)
