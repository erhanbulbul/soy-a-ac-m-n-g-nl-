package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "family_members")
data class FamilyMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userCode: String = "", // 6-digit code if linked
    val name: String,
    val relationship: String, // Anne, Baba, Dede, Babaanne, Anneanne, Teyze, Haluk/Dayı, Amca, Kardeş, Eş, Çocuk, vb.
    val generationLevel: Int = 0, // -2 (Dede/Nene), -1 (Anne/Baba/Teyze/Amca), 0 (Self/Kardeş), 1 (Çocuk)
    val avatarUri: String = "",
    val phone: String = "",
    val email: String = "",
    val isDeceased: Boolean = false,
    val birthYear: String = "",
    val notes: String = "",
    val isFollowed: Boolean = true,
    val xPos: Float = 0f,
    val yPos: Float = 0f,
    val accountStatus: String = "alive", // "alive", "pending_death", "dead"
    val deathReportedAt: Long = 0L,
    val lastLoginAt: Long = 0L
)
