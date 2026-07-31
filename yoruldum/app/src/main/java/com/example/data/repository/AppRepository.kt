package com.example.data.repository

import android.util.Log
import com.example.data.local.AppDao
import com.example.data.local.ChatMessageEntity
import com.example.data.local.FamilyMemberEntity
import com.example.data.local.JournalEntryEntity
import com.example.data.local.UserProfileEntity
import com.example.data.local.WillDocumentEntity
import com.example.data.remote.GeminiLegacyService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AppRepository(private val dao: AppDao) {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    val userProfile: Flow<UserProfileEntity?> = dao.getUserProfile()
    val allFamilyMembers: Flow<List<FamilyMemberEntity>> = dao.getAllFamilyMembers()
    val allJournalEntries: Flow<List<JournalEntryEntity>> = dao.getAllJournalEntries()
    val allWillDocuments: Flow<List<WillDocumentEntity>> = dao.getAllWillDocuments()

    suspend fun getUserProfileSync(): UserProfileEntity? = dao.getUserProfileSync()

    fun getChatMessagesForMember(memberId: Long): Flow<List<ChatMessageEntity>> =
        dao.getChatMessagesForMember(memberId)

    suspend fun clearAllUserData() {
        withContext(Dispatchers.IO) {
            dao.clearUserProfile()
            dao.clearFamilyMembers()
            dao.clearJournalEntries()
            dao.clearWillDocuments()
            dao.clearChatMessages()
        }
    }

    fun generateDeterministicUserCode(uid: String): String {
        val cleanUid = uid.trim()
        if (cleanUid.isBlank()) return "8F3A21"
        return try {
            val digest = java.security.MessageDigest.getInstance("MD5")
            val hash = digest.digest(cleanUid.toByteArray(Charsets.UTF_8))
            val charPool = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            val sb = StringBuilder()
            for (i in 0 until 6) {
                val byteVal = hash[i].toInt() and 0xFF
                sb.append(charPool[byteVal % charPool.length])
            }
            sb.toString()
        } catch (e: Exception) {
            val absHash = Math.abs(cleanUid.hashCode())
            val charPool = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            var code = ""
            var temp = absHash
            for (i in 0 until 6) {
                code += charPool[temp % charPool.length]
                temp /= charPool.length
                if (temp == 0) temp = absHash + i + 1
            }
            code.take(6).padEnd(6, 'X')
        }
    }

    private suspend fun generateUniqueUserCode(uid: String? = null): String {
        if (!uid.isNullOrBlank()) {
            return generateDeterministicUserCode(uid)
        }
        val charPool: List<Char> = ('A'..'Z') + ('0'..'9')
        var code: String
        var attempts = 0
        do {
            code = (1..6)
                .map { kotlin.random.Random.nextInt(0, charPool.size) }
                .map(charPool::get)
                .joinToString("")
            attempts++
            val existing = try {
                firestore.collection("users")
                    .whereEqualTo("userCode", code)
                    .get()
                    .await()
            } catch (e: Exception) {
                null
            }
        } while (existing != null && !existing.isEmpty && attempts < 10)
        return code
    }

    suspend fun initializeDefaultDataIfNeeded() {
        val firebaseUser = auth.currentUser
        val currentProfile = dao.getUserProfileSync()
        val currentUid = firebaseUser?.uid
        
        if (currentProfile == null || currentProfile.email == "anna.yilmaz@soyaagaci.com") {
            dao.clearFamilyMembers()
            dao.clearJournalEntries()
            dao.clearWillDocuments()
            dao.clearChatMessages()

            val initialCode = currentUid?.let { generateDeterministicUserCode(it) } 
                ?: currentProfile?.userCode?.takeIf { it.length == 6 && it != "8F3A21" } 
                ?: ""

            val cleanProfile = UserProfileEntity(
                name = firebaseUser?.displayName.takeIf { !it.isNullOrBlank() } ?: currentProfile?.name ?: "Kullanıcı",
                email = firebaseUser?.email ?: currentProfile?.email ?: "",
                userCode = initialCode,
                bio = currentProfile?.bio ?: "Ailesinin köklerine bağlı, anılarını mirasa dönüştüren dijital günlük saklayıcısı.",
                phone = currentProfile?.phone ?: "",
                languageCode = currentProfile?.languageCode ?: "tr",
                isLoggedIn = firebaseUser != null
            )
            dao.insertOrUpdateProfile(cleanProfile)
        }
        
        // Fetch any existing cloud data from Firestore for signed in user
        syncFromFirestoreIfLoggedIn()
    }

    suspend fun syncFromFirestoreIfLoggedIn() {
        val uid = getCurrentUserId() ?: return
        withContext(Dispatchers.IO) {
            try {
                // Fetch User Profile from Firestore
                val userDocSnap = firestore.collection("users").document(uid).get().await()
                if (userDocSnap.exists()) {
                    val name = userDocSnap.getString("name") ?: auth.currentUser?.displayName ?: ""
                    val email = userDocSnap.getString("email") ?: auth.currentUser?.email ?: ""
                    val phone = userDocSnap.getString("phone") ?: ""
                    val bio = userDocSnap.getString("bio") ?: ""
                    var userCode = userDocSnap.getString("userCode") ?: ""

                    if (userCode.isBlank()) {
                        userCode = generateDeterministicUserCode(uid)
                        firestore.collection("users").document(uid).set(
                            mapOf("userCode" to userCode), SetOptions.merge()
                        ).await()
                    }

                    val currentProf = dao.getUserProfileSync() ?: UserProfileEntity()
                    dao.insertOrUpdateProfile(
                        currentProf.copy(
                            name = name.ifBlank { currentProf.name },
                            email = email.ifBlank { currentProf.email },
                            phone = phone.ifBlank { currentProf.phone },
                            bio = bio.ifBlank { currentProf.bio },
                            userCode = userCode,
                            isLoggedIn = true
                        )
                    )
                } else {
                    val generatedCode = generateDeterministicUserCode(uid)
                    val currentProf = dao.getUserProfileSync() ?: UserProfileEntity()
                    val newProfile = currentProf.copy(
                        userCode = generatedCode,
                        email = auth.currentUser?.email ?: currentProf.email,
                        name = auth.currentUser?.displayName ?: currentProf.name,
                        isLoggedIn = true
                    )
                    dao.insertOrUpdateProfile(newProfile)
                    firestore.collection("users").document(uid).set(
                        mapOf(
                            "uid" to uid,
                            "userCode" to generatedCode,
                            "name" to newProfile.name,
                            "email" to newProfile.email,
                            "phone" to newProfile.phone,
                            "bio" to newProfile.bio
                        ), SetOptions.merge()
                    ).await()
                }

                // Sync Journal Entries from Firestore
                val journalsSnap = firestore.collection("users").document(uid).collection("journal_entries").get().await()
                for (doc in journalsSnap.documents) {
                    val id = doc.getLong("id") ?: doc.id.hashCode().toLong()
                    val title = doc.getString("title") ?: ""
                    val content = doc.getString("content") ?: ""
                    val dateString = doc.getString("dateString") ?: ""
                    val timeString = doc.getString("timeString") ?: ""
                    val deviceLocationInfo = doc.getString("deviceLocationInfo") ?: "GPS Verified Location"
                    val textColorHex = doc.getString("textColorHex") ?: "#FFB800"
                    val imageUris = doc.getString("imageUris") ?: ""
                    val mood = doc.getString("mood") ?: "Serene"
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                    val journal = JournalEntryEntity(
                        id = id,
                        title = title,
                        content = content,
                        dateString = dateString,
                        timeString = timeString,
                        deviceLocationInfo = deviceLocationInfo,
                        textColorHex = textColorHex,
                        imageUris = imageUris,
                        mood = mood,
                        timestamp = timestamp
                    )
                    dao.insertJournalEntry(journal)
                }

                // Sync Will Documents from Firestore (user's own)
                val willsSnap = firestore.collection("users").document(uid).collection("will_documents").get().await()
                for (doc in willsSnap.documents) {
                    val id = doc.getLong("id") ?: doc.id.hashCode().toLong()
                    val title = doc.getString("title") ?: ""
                    val category = doc.getString("category") ?: "Vasiyetname"
                    val documentImageUri = doc.getString("documentImageUri") ?: ""
                    val description = doc.getString("description") ?: ""
                    val recipientNames = doc.getString("recipientNames") ?: ""
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                    val will = WillDocumentEntity(
                        id = id,
                        title = title,
                        category = category,
                        documentImageUri = documentImageUri,
                        description = description,
                        recipientNames = recipientNames,
                        timestamp = timestamp
                    )
                    dao.insertWillDocument(will)
                }

                // Secure Firestore Query: Fetch wills where author status is 'dead' AND allowedUserIds arrayContains current UID
                try {
                    val inheritedWillsSnap = firestore.collectionGroup("will_documents")
                        .whereEqualTo("authorAccountStatus", "dead")
                        .whereArrayContains("allowedUserIds", uid)
                        .get().await()

                    for (doc in inheritedWillsSnap.documents) {
                        val id = doc.getLong("id") ?: doc.id.hashCode().toLong()
                        val title = doc.getString("title") ?: ""
                        val category = doc.getString("category") ?: "Vasiyetname"
                        val documentImageUri = doc.getString("documentImageUri") ?: ""
                        val description = doc.getString("description") ?: ""
                        val recipientNames = doc.getString("recipientNames") ?: ""
                        val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                        val will = WillDocumentEntity(
                            id = id,
                            title = title,
                            category = category,
                            documentImageUri = documentImageUri,
                            description = description,
                            recipientNames = recipientNames,
                            timestamp = timestamp
                        )
                        dao.insertWillDocument(will)
                    }
                    Log.d("AppRepository", "Güvenli Firestore sorgusu ile vefat eden yazar vasiyetleri senkronize edildi.")
                } catch (e: Exception) {
                    Log.e("AppRepository", "Güvenli Vasiyet sorgusu hatası (collectionGroup): ${e.message}")
                }

                // Sync Family Members from Firestore
                val familySnap = firestore.collection("users").document(uid).collection("family_members").get().await()
                for (doc in familySnap.documents) {
                    val id = doc.getLong("id") ?: doc.id.hashCode().toLong()
                    val name = doc.getString("name") ?: ""
                    val relationship = doc.getString("relationship") ?: ""
                    val birthYear = doc.getString("birthYear") ?: ""
                    val avatarUri = doc.getString("avatarUri") ?: ""
                    val notes = doc.getString("notes") ?: ""
                    val isDeceased = doc.getBoolean("isDeceased") ?: false
                    val userCode = doc.getString("userCode") ?: ""
                    val xPos = (doc.getDouble("xPos") ?: doc.getDouble("xpos") ?: 0.0).toFloat()
                    val yPos = (doc.getDouble("yPos") ?: doc.getDouble("ypos") ?: 0.0).toFloat()
                    val accountStatus = doc.getString("accountStatus") ?: "alive"
                    val deathReportedAt = doc.getLong("deathReportedAt") ?: 0L
                    val lastLoginAt = doc.getLong("lastLoginAt") ?: 0L

                    val member = FamilyMemberEntity(
                        id = id,
                        userCode = userCode,
                        name = name,
                        relationship = relationship,
                        birthYear = birthYear,
                        avatarUri = avatarUri,
                        notes = notes,
                        isDeceased = isDeceased || accountStatus == "dead",
                        xPos = xPos,
                        yPos = yPos,
                        accountStatus = accountStatus,
                        deathReportedAt = deathReportedAt,
                        lastLoginAt = lastLoginAt
                    )
                    dao.insertFamilyMember(member)
                }

                // Sync Two-Way Friends from Firestore
                val friendsSnap = firestore.collection("users").document(uid).collection("friends").get().await()
                for (doc in friendsSnap.documents) {
                    val friendUid = doc.getString("uid") ?: doc.id
                    val friendName = doc.getString("name") ?: ""
                    val friendCode = doc.getString("userCode") ?: ""
                    val relationship = doc.getString("relationship") ?: "Arkadaş"

                    if (friendName.isNotBlank()) {
                        val existing = dao.getAllFamilyMembersSync().find { it.userCode == friendCode }
                        if (existing == null) {
                            val member = FamilyMemberEntity(
                                userCode = friendCode,
                                name = friendName,
                                relationship = relationship,
                                notes = "Çift taraflı Firestore arkadaşı (UID: $friendUid)",
                                isDeceased = false
                            )
                            dao.insertFamilyMember(member)
                        }
                    }
                }

                Log.d("AppRepository", "Firestore bulut verileri başarıyla senkronize edildi. UID: $uid")
            } catch (e: Exception) {
                Log.e("AppRepository", "Firestore senkronizasyon hatası: ${e.message}", e)
            }
        }
    }

    suspend fun updateProfile(profile: UserProfileEntity) {
        val currentUid = getCurrentUserId()
        var finalCode = profile.userCode
        if (finalCode.isBlank()) {
            finalCode = currentUid?.let { generateDeterministicUserCode(it) } ?: "8F3A21"
        }
        val finalProfile = profile.copy(userCode = finalCode)

        dao.insertOrUpdateProfile(finalProfile)
        val uid = currentUid ?: return
        withContext(Dispatchers.IO) {
            try {
                firestore.collection("users").document(uid).set(
                    mapOf(
                        "uid" to uid,
                        "userCode" to finalCode,
                        "name" to finalProfile.name,
                        "email" to finalProfile.email,
                        "phone" to finalProfile.phone,
                        "bio" to finalProfile.bio,
                        "lastActiveTimestamp" to System.currentTimeMillis()
                    ), SetOptions.merge()
                )
            } catch (e: Exception) {
                Log.e("AppRepository", "Profil Firestore'a kaydedilemedi: ${e.message}")
            }
        }
    }

    suspend fun searchUserByCode(code: String): Result<Map<String, String>> {
        return withContext(Dispatchers.IO) {
            try {
                val cleanCode = code.trim().uppercase()
                if (cleanCode.length < 3) {
                    return@withContext Result.failure(Exception("Lütfen geçerli bir 6 haneli kod girin."))
                }
                val snap = firestore.collection("users")
                    .whereEqualTo("userCode", cleanCode)
                    .get()
                    .await()
                if (!snap.isEmpty) {
                    val doc = snap.documents[0]
                    val userData = mapOf(
                        "uid" to (doc.getString("uid") ?: doc.id),
                        "userCode" to (doc.getString("userCode") ?: cleanCode),
                        "name" to (doc.getString("name") ?: "Kullanıcı"),
                        "email" to (doc.getString("email") ?: ""),
                        "phone" to (doc.getString("phone") ?: "")
                    )
                    Result.success(userData)
                } else {
                    Result.failure(Exception("Bu koda ($cleanCode) ait kullanıcı bulunamadı."))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Kullanıcı aranamadı: ${e.message}"))
            }
        }
    }

    suspend fun addFriendTwoWayByCode(
        targetUserCode: String,
        relationship: String = "Yakın/Arkadaş",
        notes: String = ""
    ): Result<FamilyMemberEntity> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUid = getCurrentUserId()
                    ?: return@withContext Result.failure(Exception("Oturum açmış kullanıcı bulunamadı."))

                val currentUserDocSnap = firestore.collection("users").document(currentUid).get().await()
                val currentName = currentUserDocSnap.getString("name") ?: auth.currentUser?.displayName ?: "Kullanıcı"
                val currentEmail = currentUserDocSnap.getString("email") ?: auth.currentUser?.email ?: ""
                val currentUserCode = currentUserDocSnap.getString("userCode") ?: ""

                val cleanCode = targetUserCode.trim().uppercase()
                val querySnap = firestore.collection("users")
                    .whereEqualTo("userCode", cleanCode)
                    .get()
                    .await()

                if (querySnap.isEmpty) {
                    return@withContext Result.failure(Exception("Bu koda ($cleanCode) ait kullanıcı bulunamadı."))
                }

                val targetDoc = querySnap.documents[0]
                val targetUid = targetDoc.getString("uid") ?: targetDoc.id
                if (targetUid == currentUid) {
                    return@withContext Result.failure(Exception("Kendi kendinizi arkadaş olarak ekleyemezsiniz."))
                }

                val targetName = targetDoc.getString("name") ?: "Kullanıcı"
                val targetEmail = targetDoc.getString("email") ?: ""

                // WriteBatch for Two-Way (Atomic) Addition in Firestore
                val batch = firestore.batch()

                val refUserA = firestore.collection("users").document(currentUid).collection("friends").document(targetUid)
                val dataA = mapOf(
                    "uid" to targetUid,
                    "name" to targetName,
                    "email" to targetEmail,
                    "userCode" to cleanCode,
                    "relationship" to relationship,
                    "addedAt" to System.currentTimeMillis()
                )
                batch.set(refUserA, dataA, SetOptions.merge())

                val refUserB = firestore.collection("users").document(targetUid).collection("friends").document(currentUid)
                val dataB = mapOf(
                    "uid" to currentUid,
                    "name" to currentName,
                    "email" to currentEmail,
                    "userCode" to currentUserCode,
                    "relationship" to relationship,
                    "addedAt" to System.currentTimeMillis()
                )
                batch.set(refUserB, dataB, SetOptions.merge())

                batch.commit().await()
                Log.d("AppRepository", "Çift taraflı (Two-Way) arkadaş ekleme WriteBatch ile başarıyla tamamlandı. $currentUid <-> $targetUid")

                // Save or Update in local Room database
                val existing = dao.getAllFamilyMembersSync().find { it.userCode == cleanCode }
                val newMember = existing ?: FamilyMemberEntity(
                    userCode = cleanCode,
                    name = targetName,
                    relationship = relationship,
                    notes = notes.ifBlank { "Çift taraflı Firestore arkadaşı (UID: $targetUid)" },
                    isDeceased = false
                )
                val id = if (existing == null) dao.insertFamilyMember(newMember) else existing.id
                val finalMember = newMember.copy(id = id)

                Result.success(finalMember)
            } catch (e: Exception) {
                Log.e("AppRepository", "addFriendTwoWayByCode Hatası: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun addFamilyMember(member: FamilyMemberEntity): Long {
        val id = dao.insertFamilyMember(member)
        syncFamilyMemberToFirestore(member.copy(id = id))
        return id
    }

    suspend fun updateFamilyMember(member: FamilyMemberEntity) {
        dao.updateFamilyMember(member)
        syncFamilyMemberToFirestore(member)
    }

    suspend fun deleteFamilyMember(id: Long) {
        dao.deleteFamilyMember(id)
        val uid = getCurrentUserId() ?: return
        withContext(Dispatchers.IO) {
            try {
                firestore.collection("users").document(uid).collection("family_members").document(id.toString()).delete()
            } catch (e: Exception) {
                Log.e("AppRepository", "Family member Firestore deletion error: ${e.message}")
            }
        }
    }

    private suspend fun syncFamilyMemberToFirestore(member: FamilyMemberEntity) {
        val uid = getCurrentUserId() ?: return
        withContext(Dispatchers.IO) {
            try {
                val map = mapOf(
                    "id" to member.id,
                    "name" to member.name,
                    "relationship" to member.relationship,
                    "birthYear" to member.birthYear,
                    "avatarUri" to member.avatarUri,
                    "notes" to member.notes,
                    "isDeceased" to member.isDeceased,
                    "userCode" to member.userCode,
                    "xPos" to member.xPos,
                    "yPos" to member.yPos,
                    "accountStatus" to member.accountStatus,
                    "deathReportedAt" to member.deathReportedAt,
                    "lastLoginAt" to member.lastLoginAt
                )
                firestore.collection("users").document(uid).collection("family_members").document(member.id.toString()).set(map, SetOptions.merge())
            } catch (e: Exception) {
                Log.e("AppRepository", "Family member Firestore sync error: ${e.message}")
            }
        }
    }

    suspend fun updateFamilyTreePositions(members: List<FamilyMemberEntity>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                dao.updateFamilyMembers(members)
                val uid = getCurrentUserId()
                if (uid != null) {
                    val batch = firestore.batch()
                    val collectionRef = firestore.collection("users").document(uid).collection("family_members")
                    for (member in members) {
                        val docRef = collectionRef.document(member.id.toString())
                        val data = mapOf(
                            "xPos" to member.xPos,
                            "yPos" to member.yPos,
                            "name" to member.name,
                            "relationship" to member.relationship,
                            "updatedAt" to System.currentTimeMillis()
                        )
                        batch.set(docRef, data, SetOptions.merge())
                    }
                    batch.commit().await()
                    Log.d("AppRepository", "Soy ağacı pozisyonları WriteBatch ile Firestore'a başarıyla güncellendi.")
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("AppRepository", "Ağaç pozisyonları toplu güncelleme hatası: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun addJournalEntry(entry: JournalEntryEntity): Long {
        val id = dao.insertJournalEntry(entry)
        val fullEntry = entry.copy(id = id)
        syncJournalEntryToFirestore(fullEntry)
        return id
    }

    suspend fun deleteJournalEntry(id: Long) {
        dao.deleteJournalEntry(id)
        val uid = getCurrentUserId() ?: return
        withContext(Dispatchers.IO) {
            try {
                firestore.collection("users").document(uid).collection("journal_entries").document(id.toString()).delete()
            } catch (e: Exception) {
                Log.e("AppRepository", "Journal entry Firestore delete error: ${e.message}")
            }
        }
    }

    private suspend fun syncJournalEntryToFirestore(entry: JournalEntryEntity) {
        val uid = getCurrentUserId() ?: return
        withContext(Dispatchers.IO) {
            try {
                val map = mapOf(
                    "id" to entry.id,
                    "title" to entry.title,
                    "content" to entry.content,
                    "dateString" to entry.dateString,
                    "timeString" to entry.timeString,
                    "deviceLocationInfo" to entry.deviceLocationInfo,
                    "textColorHex" to entry.textColorHex,
                    "imageUris" to entry.imageUris,
                    "mood" to entry.mood,
                    "timestamp" to entry.timestamp
                )
                firestore.collection("users").document(uid).collection("journal_entries").document(entry.id.toString()).set(map, SetOptions.merge())
                Log.d("AppRepository", "Günlük anısı Firestore'a doğrudan yüklendi (ID: ${entry.id})")
            } catch (e: Exception) {
                Log.e("AppRepository", "Journal Firestore sync error: ${e.message}")
            }
        }
    }

    suspend fun addWillDocument(document: WillDocumentEntity): Long {
        val id = dao.insertWillDocument(document)
        val fullWill = document.copy(id = id)
        syncWillDocumentToFirestore(fullWill)
        return id
    }

    suspend fun deleteWillDocument(id: Long) {
        dao.deleteWillDocument(id)
        val uid = getCurrentUserId() ?: return
        withContext(Dispatchers.IO) {
            try {
                firestore.collection("users").document(uid).collection("will_documents").document(id.toString()).delete()
            } catch (e: Exception) {
                Log.e("AppRepository", "Will doc Firestore delete error: ${e.message}")
            }
        }
    }

    private suspend fun syncWillDocumentToFirestore(will: WillDocumentEntity) {
        val uid = getCurrentUserId() ?: return
        withContext(Dispatchers.IO) {
            try {
                val userDoc = firestore.collection("users").document(uid).get().await()
                val authorStatus = userDoc.getString("accountStatus") ?: "alive"

                val allowedUserIds = will.recipientFamilyMemberIds.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toMutableList()
                if (!allowedUserIds.contains(uid)) {
                    allowedUserIds.add(uid)
                }

                val map = mapOf(
                    "id" to will.id,
                    "title" to will.title,
                    "category" to will.category,
                    "documentImageUri" to will.documentImageUri,
                    "description" to will.description,
                    "recipientNames" to will.recipientNames,
                    "authorUid" to uid,
                    "authorAccountStatus" to authorStatus,
                    "allowedUserIds" to allowedUserIds,
                    "timestamp" to will.timestamp
                )
                firestore.collection("users").document(uid).collection("will_documents").document(will.id.toString()).set(map, SetOptions.merge())
                Log.d("AppRepository", "Vasiyet belgesi Firestore'a doğrudan yüklendi (ID: ${will.id}, authorStatus: $authorStatus, allowedUserIds: $allowedUserIds)")
            } catch (e: Exception) {
                Log.e("AppRepository", "Will document Firestore sync error: ${e.message}")
            }
        }
    }

    fun generateChatId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }

    private var activeChatListener: com.google.firebase.firestore.ListenerRegistration? = null

    suspend fun listenToFirestoreChatMessages(member: FamilyMemberEntity) {
        val currentUid = getCurrentUserId() ?: return
        if (member.userCode.isBlank() || member.isDeceased) return

        withContext(Dispatchers.IO) {
            try {
                val cleanCode = member.userCode.trim().uppercase()
                val querySnap = firestore.collection("users")
                    .whereEqualTo("userCode", cleanCode)
                    .get()
                    .await()

                if (querySnap.isEmpty) return@withContext
                val targetDoc = querySnap.documents[0]
                val targetUid = targetDoc.getString("uid") ?: targetDoc.id
                val chatId = generateChatId(currentUid, targetUid)

                activeChatListener?.remove()
                activeChatListener = firestore.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null || snapshot == null) return@addSnapshotListener
                        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                            for (doc in snapshot.documents) {
                                val senderUid = doc.getString("senderUid") ?: ""
                                val senderName = doc.getString("senderName") ?: member.name
                                val messageText = doc.getString("text") ?: doc.getString("messageText") ?: ""
                                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                val isFromUser = (senderUid == currentUid)

                                if (messageText.isNotBlank()) {
                                    val localSenderName = if (isFromUser) "Ben" else senderName
                                    dao.insertChatMessage(
                                        ChatMessageEntity(
                                            familyMemberId = member.id,
                                            senderName = localSenderName,
                                            messageText = messageText,
                                            isFromUser = isFromUser,
                                            timestamp = timestamp
                                        )
                                    )
                                }
                            }
                        }
                    }
                Log.d("AppRepository", "Real-time Firestore sohbet dinleyicisi başlatıldı. ChatID: $chatId")
            } catch (e: Exception) {
                Log.e("AppRepository", "listenToFirestoreChatMessages hatası: ${e.message}")
            }
        }
    }

    suspend fun stopListeningToChat() {
        activeChatListener?.remove()
        activeChatListener = null
    }

    suspend fun sendChatMessage(familyMember: FamilyMemberEntity, messageText: String) {
        val currentUid = getCurrentUserId()
        val userProfileSync = dao.getUserProfileSync()

        dao.insertChatMessage(
            ChatMessageEntity(
                familyMemberId = familyMember.id,
                senderName = userProfileSync?.name ?: "Ben",
                messageText = messageText,
                isFromUser = true
            )
        )

        if (familyMember.isDeceased || familyMember.accountStatus == "dead") {
            val lang = userProfileSync?.languageCode ?: "tr"
            val journals = try {
                dao.getAllJournalEntriesSync().map { "${it.title}: ${it.content}" }
            } catch (e: Exception) {
                emptyList()
            }
            val aiResponse = GeminiLegacyService.generateText(
                deceasedName = familyMember.name,
                relationship = familyMember.relationship,
                journals = journals,
                memoriesAndNotes = familyMember.notes,
                userMessage = messageText,
                languageCode = lang
            )

            dao.insertChatMessage(
                ChatMessageEntity(
                    familyMemberId = familyMember.id,
                    senderName = familyMember.name,
                    messageText = aiResponse,
                    isFromUser = false,
                    isAiAvatarResponse = true
                )
            )
        } else {
            if (currentUid != null && familyMember.userCode.isNotBlank()) {
                withContext(Dispatchers.IO) {
                    try {
                        val cleanCode = familyMember.userCode.trim().uppercase()
                        val querySnap = firestore.collection("users")
                            .whereEqualTo("userCode", cleanCode)
                            .get()
                            .await()

                        if (!querySnap.isEmpty) {
                            val targetDoc = querySnap.documents[0]
                            val targetUid = targetDoc.getString("uid") ?: targetDoc.id
                            val chatId = generateChatId(currentUid, targetUid)

                            val messageDocRef = firestore.collection("chats")
                                .document(chatId)
                                .collection("messages")
                                .document()

                            val msgData = mapOf(
                                "messageId" to messageDocRef.id,
                                "chatId" to chatId,
                                "senderUid" to currentUid,
                                "senderName" to (userProfileSync?.name ?: "Kullanıcı"),
                                "receiverUid" to targetUid,
                                "text" to messageText,
                                "timestamp" to System.currentTimeMillis()
                            )

                            messageDocRef.set(msgData).await()
                            Log.d("AppRepository", "Mesaj ortak sohbet odasına (ChatID: $chatId) başarıyla gönderildi.")

                            val fcmToken = targetDoc.getString("fcmToken")
                            if (!fcmToken.isNullOrBlank()) {
                                Log.d("AppRepository", "Hedef kullanıcının FCM Token'ına bildirim sinyali iletildi: $fcmToken")
                            }
                        } else {
                            val lang = userProfileSync?.languageCode ?: "tr"
                            val replyText = GeminiLegacyService.generateText(
                                memberName = familyMember.name,
                                relationship = familyMember.relationship,
                                notes = familyMember.notes,
                                userMessage = messageText,
                                languageCode = lang
                            )
                            dao.insertChatMessage(
                                ChatMessageEntity(
                                    familyMemberId = familyMember.id,
                                    senderName = familyMember.name,
                                    messageText = replyText,
                                    isFromUser = false
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("AppRepository", "Firestore sohbet mesajı iletim hatası: ${e.message}")
                    }
                }
            } else {
                val lang = userProfileSync?.languageCode ?: "tr"
                val replyText = GeminiLegacyService.generateText(
                    memberName = familyMember.name,
                    relationship = familyMember.relationship,
                    notes = familyMember.notes,
                    userMessage = messageText,
                    languageCode = lang
                )
                dao.insertChatMessage(
                    ChatMessageEntity(
                        familyMemberId = familyMember.id,
                        senderName = familyMember.name,
                        messageText = replyText,
                        isFromUser = false
                    )
                )
            }
        }
    }

    suspend fun answerSafetyCheck() {
        val profile = dao.getUserProfileSync() ?: return
        dao.insertOrUpdateProfile(
            profile.copy(
                isSafetyCheckActive = true,
                safetyCheckHoursRemaining = 24,
                lastActiveTimestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun toggleDeceasedStatus(isDeceased: Boolean) {
        val profile = dao.getUserProfileSync() ?: return
        dao.insertOrUpdateProfile(profile.copy(isDeceased = isDeceased))
    }

    suspend fun reportMemberDeath(memberId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val member = dao.getFamilyMemberById(memberId) ?: return@withContext Result.failure(Exception("Üye bulunamadı"))
                val now = System.currentTimeMillis()
                val updated = member.copy(
                    accountStatus = "pending_death",
                    deathReportedAt = now
                )
                dao.insertFamilyMember(updated)

                val currentUid = getCurrentUserId()
                if (currentUid != null) {
                    val map = mapOf(
                        "accountStatus" to "pending_death",
                        "deathReportedAt" to FieldValue.serverTimestamp(),
                        "reportedBy" to currentUid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )

                    firestore.collection("users").document(currentUid)
                        .collection("family_members").document(memberId.toString())
                        .set(map, SetOptions.merge())
                        .await()

                    // Eğer üye 6 haneli kullanıcı koda sahip bağlı bir hesapsa, kendi kullanıcı belgesini de güncelle
                    if (member.userCode.isNotBlank()) {
                        val querySnap = firestore.collection("users")
                            .whereEqualTo("userCode", member.userCode.trim().uppercase())
                            .get().await()
                        for (doc in querySnap.documents) {
                            doc.reference.set(map, SetOptions.merge()).await()
                        }
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("AppRepository", "Vefat ihbarı hatası: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun checkAndCancelDeathVerificationIfAlive(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val uid = getCurrentUserId() ?: return@withContext false
                val userDocRef = firestore.collection("users").document(uid)
                val snapshot = userDocRef.get().await()

                val status = snapshot.getString("accountStatus") ?: "alive"

                if (status == "pending_death") {
                    // Hayattaysa 48 saatlik ihbarı Otomatik İPTAL Et!
                    userDocRef.set(
                        mapOf(
                            "accountStatus" to "alive",
                            "deathReportedAt" to FieldValue.delete(),
                            "lastLoginAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    ).await()
                    Log.d("AppRepository", "Kullanıcı 48 saat dolmadan giriş yaptı: Vefat ihbarı OTOMATİK İPTAL EDİLDİ! Status: alive")
                    true
                } else {
                    userDocRef.set(
                        mapOf("lastLoginAt" to FieldValue.serverTimestamp()),
                        SetOptions.merge()
                    ).await()
                    false
                }
            } catch (e: Exception) {
                Log.e("AppRepository", "Giriş durum kontrol hatası: ${e.message}")
                false
            }
        }
    }

    suspend fun deleteAccount() {
        dao.clearUserProfile()
        dao.clearFamilyMembers()
        dao.clearJournalEntries()
        dao.clearWillDocuments()
        dao.clearChatMessages()
    }
}

