package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // User Profile
    @Query("SELECT * FROM user_profile WHERE id = 'primary_user'")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 'primary_user'")
    suspend fun getUserProfileSync(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)

    // Family Members
    @Query("SELECT * FROM family_members ORDER BY generationLevel ASC, name ASC")
    fun getAllFamilyMembers(): Flow<List<FamilyMemberEntity>>

    @Query("SELECT * FROM family_members")
    suspend fun getAllFamilyMembersSync(): List<FamilyMemberEntity>

    @Query("SELECT * FROM family_members WHERE userCode = :userCode LIMIT 1")
    suspend fun getFamilyMemberByUserCode(userCode: String): FamilyMemberEntity?

    @Query("SELECT * FROM family_members WHERE id = :id")
    suspend fun getFamilyMemberById(id: Long): FamilyMemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamilyMember(member: FamilyMemberEntity): Long

    @Update
    suspend fun updateFamilyMember(member: FamilyMemberEntity)

    @Update
    suspend fun updateFamilyMembers(members: List<FamilyMemberEntity>)

    @Query("DELETE FROM family_members WHERE id = :id")
    suspend fun deleteFamilyMember(id: Long)

    // Journal Entries
    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC")
    fun getAllJournalEntries(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC")
    suspend fun getAllJournalEntriesSync(): List<JournalEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(entry: JournalEntryEntity): Long

    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun deleteJournalEntry(id: Long)

    // Will Documents
    @Query("SELECT * FROM will_documents ORDER BY timestamp DESC")
    fun getAllWillDocuments(): Flow<List<WillDocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWillDocument(document: WillDocumentEntity): Long

    @Query("DELETE FROM will_documents WHERE id = :id")
    suspend fun deleteWillDocument(id: Long)

    // Chat Messages
    @Query("SELECT * FROM chat_messages WHERE familyMemberId = :memberId ORDER BY timestamp ASC")
    fun getChatMessagesForMember(memberId: Long): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity): Long

    // Clear all data (for Account Deletion "Hesabı Sil")
    @Query("DELETE FROM user_profile")
    suspend fun clearUserProfile()

    @Query("DELETE FROM family_members")
    suspend fun clearFamilyMembers()

    @Query("DELETE FROM journal_entries")
    suspend fun clearJournalEntries()

    @Query("DELETE FROM will_documents")
    suspend fun clearWillDocuments()

    @Query("DELETE FROM chat_messages")
    suspend fun clearChatMessages()
}
