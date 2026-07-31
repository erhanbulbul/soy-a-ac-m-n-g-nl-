package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.FamilyMemberEntity
import com.example.data.local.JournalEntryEntity
import com.example.data.local.UserProfileEntity
import com.example.data.local.WillDocumentEntity
import com.example.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository
    private val prefs = application.getSharedPreferences("soyaagaci_session", android.content.Context.MODE_PRIVATE)

    private val _isLoggedIn = MutableStateFlow<Boolean>(prefs.getBoolean("is_logged_in", false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isPremium = MutableStateFlow<Boolean>(prefs.getBoolean("is_premium", true))
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _showPaywallDialog = MutableStateFlow<Boolean>(false)
    val showPaywallDialog: StateFlow<Boolean> = _showPaywallDialog.asStateFlow()

    private val _paywallFeatureName = MutableStateFlow<String?>(null)
    val paywallFeatureName: StateFlow<String?> = _paywallFeatureName.asStateFlow()

    fun triggerPaywall(featureName: String? = null) {
        _paywallFeatureName.value = featureName
        _showPaywallDialog.value = true
    }

    fun dismissPaywall() {
        _showPaywallDialog.value = false
        _paywallFeatureName.value = null
    }

    fun setPremiumStatus(enabled: Boolean) {
        prefs.edit().putBoolean("is_premium", enabled).apply()
        _isPremium.value = enabled
        if (enabled) {
            _showPaywallDialog.value = false
        }
    }

    init {
        val database = AppDatabase.getInstance(application)
        repository = AppRepository(database.appDao())
        
        // Auto session restore check
        val firebaseUser = try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser } catch (e: Exception) { null }
        val isSavedLoggedIn = prefs.getBoolean("is_logged_in", false)
        if (isSavedLoggedIn || firebaseUser != null) {
            _isLoggedIn.value = true
            prefs.edit().putBoolean("is_logged_in", true).apply()
        }

        viewModelScope.launch {
            repository.initializeDefaultDataIfNeeded()
            val current = repository.getUserProfileSync()
            val savedName = prefs.getString("saved_name", "") ?: ""
            val savedEmail = prefs.getString("saved_email", "") ?: ""
            val currentUid = firebaseUser?.uid
            
            if (current != null && (isSavedLoggedIn || current.isLoggedIn || firebaseUser != null)) {
                _isLoggedIn.value = true
                repository.updateProfile(
                    current.copy(
                        name = if (savedName.isNotBlank()) savedName else current.name,
                        email = if (savedEmail.isNotBlank()) savedEmail else current.email,
                        userCode = current.userCode,
                        isLoggedIn = true
                    )
                )
            }
            // 48 saatlik vefat ihbarı iptal kontrolü (Kullanıcı hayattaysa ve giriş yaptıysa)
            val isRestored = repository.checkAndCancelDeathVerificationIfAlive()
            if (isRestored) {
                Log.d("MainViewModel", "Vefat ihbarı başarıyla iptal edildi! Kullanıcı aktif olarak kaydedildi.")
            }
        }
    }

    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val familyMembers: StateFlow<List<FamilyMemberEntity>> = repository.allFamilyMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val journalEntries: StateFlow<List<JournalEntryEntity>> = repository.allJournalEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val willDocuments: StateFlow<List<WillDocumentEntity>> = repository.allWillDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedMemberForChat = MutableStateFlow<FamilyMemberEntity?>(null)
    val selectedMemberForChat: StateFlow<FamilyMemberEntity?> = _selectedMemberForChat.asStateFlow()

    private val _isRecordingVoice = MutableStateFlow(false)
    val isRecordingVoice: StateFlow<Boolean> = _isRecordingVoice.asStateFlow()

    private val _currentLanguage = MutableStateFlow(prefs.getString("selected_language", "tr") ?: "tr")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    fun login(name: String, email: String) {
        val firebaseUser = try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser } catch (e: Exception) { null }
        val firebaseUid = firebaseUser?.uid
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("saved_name", name)
            .putString("saved_email", email)
            .apply()
        _isLoggedIn.value = true
        viewModelScope.launch {
            val current = userProfile.value ?: UserProfileEntity()
            repository.updateProfile(
                current.copy(
                    name = if (name.isNotBlank()) name else current.name,
                    email = if (email.isNotBlank()) email else current.email,
                    userCode = current.userCode,
                    isLoggedIn = true
                )
            )
            repository.syncFromFirestoreIfLoggedIn()
        }
    }

    fun logout() {
        prefs.edit()
            .putBoolean("is_logged_in", false)
            .remove("saved_name")
            .remove("saved_email")
            .apply()
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _isLoggedIn.value = false
        viewModelScope.launch {
            repository.clearAllUserData()
        }
    }

    fun selectMemberForChat(member: FamilyMemberEntity?) {
        _selectedMemberForChat.value = member
        if (member != null && !member.isDeceased && member.userCode.isNotBlank()) {
            viewModelScope.launch {
                repository.listenToFirestoreChatMessages(member)
            }
        } else {
            viewModelScope.launch {
                repository.stopListeningToChat()
            }
        }
    }

    fun setLanguage(langCode: String) {
        prefs.edit().putString("selected_language", langCode).apply()
        _currentLanguage.value = langCode
        try {
            val locale = Locale(langCode)
            Locale.setDefault(locale)
            val config = getApplication<Application>().resources.configuration
            config.setLocale(locale)
            getApplication<Application>().resources.updateConfiguration(config, getApplication<Application>().resources.displayMetrics)
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error setting locale: ${e.message}")
        }
        viewModelScope.launch {
            val current = userProfile.value ?: repository.getUserProfileSync() ?: UserProfileEntity()
            repository.updateProfile(current.copy(languageCode = langCode))
        }
    }

    fun updateProfileInfo(
        name: String,
        email: String,
        bio: String,
        phone: String,
        avatarUri: String,
        preferredColorHex: String,
        address: String = "",
        bloodType: String = ""
    ) {
        viewModelScope.launch {
            val current = userProfile.value ?: UserProfileEntity()
            repository.updateProfile(
                current.copy(
                    name = name,
                    email = email,
                    bio = bio,
                    phone = phone,
                    avatarUri = avatarUri,
                    preferredTextColorHex = preferredColorHex,
                    address = address,
                    bloodType = bloodType
                )
            )
        }
    }

    fun saveFamilyTreePositions(
        updatedMembers: List<FamilyMemberEntity>,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.updateFamilyTreePositions(updatedMembers)
            onComplete(result.isSuccess)
        }
    }

    fun addFamilyMemberByCode(
        userCode: String,
        name: String,
        relationship: String,
        notes: String,
        isDeceased: Boolean = false,
        birthYear: String = "",
        avatarUri: String = ""
    ) {
        viewModelScope.launch {
            if (!isDeceased && userCode.trim().length == 6) {
                val twoWayResult = repository.addFriendTwoWayByCode(
                    targetUserCode = userCode.trim().uppercase(),
                    relationship = relationship,
                    notes = notes
                )
                if (twoWayResult.isSuccess) {
                    return@launch
                }
            }

            val genLevel = when (relationship) {
                "Dede", "Babaanne", "Anneanne" -> -2
                "Anne", "Baba", "Teyze", "Amca", "Dayı", "Hala" -> -1
                "Çocuk" -> 1
                else -> 0
            }
            val defaultCode = if (isDeceased) "MERHUM-${(100..999).random()}" else (100000..999999).random().toString()
            
            // Default avatar image URL if none provided
            val finalAvatarUri = avatarUri.ifBlank {
                when (relationship) {
                    "Anne", "Anneanne", "Babaanne", "Teyze", "Hala", "Eş", "Kız Çocuk" -> "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150"
                    "Dede", "Baba", "Amca", "Dayı", "Erkek Çocuk" -> "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150"
                    else -> "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"
                }
            }

            val newMember = FamilyMemberEntity(
                userCode = if (userCode.isBlank()) defaultCode else userCode,
                name = name,
                relationship = relationship,
                generationLevel = genLevel,
                notes = notes,
                isDeceased = isDeceased,
                birthYear = birthYear,
                avatarUri = finalAvatarUri
            )
            repository.addFamilyMember(newMember)
        }
    }

    fun deleteFamilyMember(id: Long) {
        viewModelScope.launch {
            repository.deleteFamilyMember(id)
        }
    }

    fun addJournalEntry(
        title: String,
        content: String,
        textColorHex: String,
        imageUris: String = ""
    ) {
        viewModelScope.launch {
            val lang = userProfile.value?.languageCode ?: "tr"
            val locale = if (lang == "tr") Locale("tr", "TR") else Locale.ENGLISH
            val dateFormat = SimpleDateFormat("dd MMMM yyyy, EEEE", locale)
            val timeFormat = SimpleDateFormat("HH:mm", locale)
            val now = Date()

            val entry = JournalEntryEntity(
                title = title,
                content = content,
                dateString = dateFormat.format(now),
                timeString = timeFormat.format(now),
                textColorHex = textColorHex,
                imageUris = imageUris,
                deviceLocationInfo = "GPS Verified Location"
            )
            repository.addJournalEntry(entry)
        }
    }

    fun updateJournalEntry(
        id: Long,
        title: String,
        content: String,
        textColorHex: String,
        imageUris: String = ""
    ) {
        viewModelScope.launch {
            val existing = journalEntries.value.find { it.id == id } ?: return@launch
            val updated = existing.copy(
                title = title,
                content = content,
                textColorHex = textColorHex,
                imageUris = imageUris
            )
            repository.addJournalEntry(updated)
        }
    }

    fun deleteJournalEntry(id: Long) {
        viewModelScope.launch { repository.deleteJournalEntry(id) }
    }

    fun addWillDocument(
        title: String,
        category: String,
        documentUri: String,
        description: String,
        recipientNames: String
    ) {
        viewModelScope.launch {
            val doc = WillDocumentEntity(
                title = title,
                category = category,
                documentImageUri = documentUri,
                description = description,
                recipientNames = recipientNames
            )
            repository.addWillDocument(doc)
        }
    }

    fun deleteWillDocument(id: Long) {
        viewModelScope.launch { repository.deleteWillDocument(id) }
    }

    fun sendMessageToMember(member: FamilyMemberEntity, text: String) {
        viewModelScope.launch {
            repository.sendChatMessage(member, text)
        }
    }

    fun confirmSafetyCheck() {
        viewModelScope.launch {
            repository.answerSafetyCheck()
        }
    }

    fun reportMemberDeath(memberId: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val res = repository.reportMemberDeath(memberId)
            onResult(res.isSuccess)
        }
    }

    fun toggleDeceasedSimulation(isDeceased: Boolean) {
        viewModelScope.launch {
            repository.toggleDeceasedStatus(isDeceased)
        }
    }

    fun toggleVoiceRecording() {
        _isRecordingVoice.value = !_isRecordingVoice.value
    }

    fun deleteAccount() {
        prefs.edit().putBoolean("is_logged_in", false).apply()
        _isLoggedIn.value = false
        viewModelScope.launch {
            repository.deleteAccount()
        }
    }
}
