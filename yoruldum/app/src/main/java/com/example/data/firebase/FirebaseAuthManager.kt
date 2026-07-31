package com.example.data.firebase

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.util.Date

data class FirestoreUser(
    val uid: String = "",
    val userCode: String = "",
    val name: String = "",
    val email: String = "",
    val authProvider: String = "email",
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis(),
    val languageCode: String = "tr",
    val role: String = "user"
)

class FirebaseAuthManager(private val context: Context) {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    val currentUser get() = auth.currentUser

    suspend fun registerUserWithEmail(
        name: String,
        email: String,
        password: String,
        languageCode: String = "tr"
    ): Result<FirestoreUser> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val firebaseUser = authResult.user ?: throw Exception("Kullanıcı oluşturulamadı.")

            // Update Auth Display Name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name.trim())
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            val uid = firebaseUser.uid
            val userProfile = FirestoreUser(
                uid = uid,
                name = name.trim(),
                email = email.trim(),
                authProvider = "email",
                createdAt = System.currentTimeMillis(),
                lastLoginAt = System.currentTimeMillis(),
                languageCode = languageCode,
                role = "user"
            )

            // Save automatically to Firestore "users" collection
            saveUserToFirestore(userProfile)
            fetchAndSaveFcmToken()

            Result.success(userProfile)
        } catch (e: Exception) {
            e.printStackTrace()
            val msg = translateFirebaseError(e)
            Result.failure(Exception(msg))
        }
    }

    suspend fun loginUserWithEmail(
        email: String,
        password: String
    ): Result<FirestoreUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val firebaseUser = authResult.user ?: throw Exception("Giriş başarısız oldu.")
            val uid = firebaseUser.uid

            // Update lastLoginAt in Firestore
            val updates = mapOf(
                "lastLoginAt" to System.currentTimeMillis()
            )
            try {
                firestore.collection("users").document(uid)
                    .set(updates, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Fetch user profile from Firestore or fallback to FirebaseUser info
            val profile = fetchUserProfileFromFirestore(uid) ?: FirestoreUser(
                uid = uid,
                name = firebaseUser.displayName ?: email.substringBefore("@"),
                email = firebaseUser.email ?: email,
                authProvider = "email",
                lastLoginAt = System.currentTimeMillis()
            )

            Result.success(profile)
        } catch (e: Exception) {
            e.printStackTrace()
            val msg = translateFirebaseError(e)
            Result.failure(Exception(msg))
        }
    }

    suspend fun performGoogleSignIn(
        context: Context,
        languageCode: String = "tr"
    ): Result<FirestoreUser> {
        return try {
            val webClientId = context.getString(R.string.default_web_client_id)
            if (webClientId.isBlank()) {
                return Result.failure(Exception("Google Web Client ID yapılandırılmamış."))
            }

            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val credentialResult = credentialManager.getCredential(request = request, context = context)
            val credential = credentialResult.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                val firebaseUser = authResult.user ?: throw Exception("Google kimlik doğrulama başarısız.")

                val userProfile = FirestoreUser(
                    uid = firebaseUser.uid,
                    name = firebaseUser.displayName ?: googleIdTokenCredential.displayName ?: "Google Kullanıcısı",
                    email = firebaseUser.email ?: googleIdTokenCredential.id,
                    authProvider = "google",
                    lastLoginAt = System.currentTimeMillis(),
                    languageCode = languageCode
                )
                saveUserToFirestore(userProfile)
                Result.success(userProfile)
            } else {
                Result.failure(Exception("Geçersiz Google kimlik bilgisi alındı."))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("Google ile giriş kullanıcı tarafından iptal edildi."))
        } catch (e: GetCredentialException) {
            Log.e("FirebaseAuthManager", "Google Credential Manager Hatası: ${e.message}", e)
            Result.failure(Exception("Google Hesabı seçimi başarısız: ${e.message}"))
        } catch (e: Exception) {
            Log.e("FirebaseAuthManager", "Google Sign In Hatası: ${e.message}", e)
            val msg = translateFirebaseError(e)
            Result.failure(Exception(msg))
        }
    }

    suspend fun signInWithGoogleCredential(
        idToken: String?,
        accountEmail: String,
        accountName: String,
        languageCode: String = "tr"
    ): Result<FirestoreUser> {
        return signInWithSocialProvider("Google", idToken, accountEmail, accountName, languageCode)
    }

    suspend fun signInWithSocialProvider(
        provider: String,
        idToken: String?,
        accountEmail: String,
        accountName: String,
        languageCode: String = "tr"
    ): Result<FirestoreUser> {
        return try {
            val providerKey = provider.lowercase().replace(" ", "_")
            val firebaseUser = if (!idToken.isNullOrBlank() && provider.equals("Google", ignoreCase = true)) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                authResult.user
            } else {
                auth.currentUser
            }

            if (firebaseUser == null && idToken.isNullOrBlank() && accountEmail.isBlank()) {
                return Result.failure(Exception("$provider kimlik doğrulama verisi alınamadı."))
            }

            val uid = firebaseUser?.uid ?: "${providerKey}_${accountEmail.lowercase().replace(".", "_")}"
            val finalName = firebaseUser?.displayName ?: accountName.ifBlank { "$provider Kullanıcısı" }
            val finalEmail = firebaseUser?.email ?: accountEmail

            val userProfile = FirestoreUser(
                uid = uid,
                name = finalName,
                email = finalEmail,
                authProvider = providerKey,
                lastLoginAt = System.currentTimeMillis(),
                languageCode = languageCode
            )

            saveUserToFirestore(userProfile)
            Result.success(userProfile)
        } catch (e: Exception) {
            e.printStackTrace()
            val msg = translateFirebaseError(e)
            Result.failure(Exception(msg))
        }
    }

    private fun formatPhoneNumberToE164(phoneNumber: String): String {
        val clean = phoneNumber.replace(Regex("[^0-9+]"), "")
        return when {
            clean.startsWith("+") -> clean
            clean.startsWith("05") -> "+90" + clean.substring(1)
            clean.startsWith("5") && clean.length == 10 -> "+90" + clean
            clean.startsWith("00") -> "+" + clean.substring(2)
            else -> "+$clean"
        }
    }

    fun sendPhoneVerificationCode(
        activity: android.app.Activity,
        phoneNumber: String,
        onCodeSent: (verificationId: String) -> Unit,
        onVerificationCompleted: (userProfile: FirestoreUser) -> Unit,
        onVerificationFailed: (errorMessage: String) -> Unit
    ) {
        val sanitized = formatPhoneNumberToE164(phoneNumber)
        Log.d("FirebaseAuthManager", "SMS kodu gönderimi başlatıldı. Numara: $sanitized (Orijinal girdi: $phoneNumber)")

        val options = com.google.firebase.auth.PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(sanitized)
            .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
                    Log.d("FirebaseAuthManager", "Otomatik SMS doğrulaması başarılı! Numara: $sanitized")
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        try {
                            val authResult = auth.signInWithCredential(credential).await()
                            val firebaseUser = authResult.user
                            val uid = firebaseUser?.uid ?: "phone_${sanitized.replace("+", "")}"
                            val userProfile = FirestoreUser(
                                uid = uid,
                                name = firebaseUser?.displayName ?: "Kullanıcı ($sanitized)",
                                email = firebaseUser?.email ?: "$sanitized@soyaagaci.com",
                                authProvider = "phone",
                                lastLoginAt = System.currentTimeMillis()
                            )
                            saveUserToFirestore(userProfile)
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                onVerificationCompleted(userProfile)
                            }
                        } catch (e: Exception) {
                            Log.e("FirebaseAuthManager", "Otomatik giriş sırasında hata oluştu. Numara: $sanitized", e)
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                onVerificationFailed(e.localizedMessage ?: "Otomatik giriş başarısız.")
                            }
                        }
                    }
                }

                override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                    Log.e("FirebaseAuthManager", "SMS Gönderim Hatası (onVerificationFailed). Numara: $sanitized, Mesaj: ${e.message}", e)
                    val friendlyMsg = when {
                        e.message?.contains("BILLING_NOT_ENABLED", ignoreCase = true) == true || e.message?.contains("billing", ignoreCase = true) == true ->
                            "Firebase SMS Hatası [BILLING_NOT_ENABLED]: Firebase Console'da Spark (ücretsiz) planındasınız. Gerçek SMS gönderimi için Firebase projesinde Blaze (Faturalandırma) planı aktif olmalıdır. VEYA Firebase Console -> Auth -> 'Test Telefon Numaraları' kısmına $sanitized numarasını ekleyip test koduyla giriş yapabilirsiniz."
                        e.message?.contains("SHA", ignoreCase = true) == true || e.message?.contains("appNotAuthorized", ignoreCase = true) == true ->
                            "Firebase Console'da bu uygulamanın SHA-1/SHA-256 parmak izi tanımlı değil. (Detay: ${e.localizedMessage})"
                        e.message?.contains("quota", ignoreCase = true) == true ->
                            "SMS kotası doldu. Detay: ${e.localizedMessage}"
                        e.message?.contains("invalid", ignoreCase = true) == true ->
                            "Geçersiz telefon numarası ($sanitized). Detay: ${e.localizedMessage}"
                        else -> e.localizedMessage ?: "SMS gönderilemedi. Numarayı ve internetinizi kontrol edin."
                    }
                    onVerificationFailed(friendlyMsg)
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
                ) {
                    Log.d("FirebaseAuthManager", "SMS Kodu Başarıyla Gönderildi! Numara: $sanitized, VerificationId: $verificationId")
                    onCodeSent(verificationId)
                }
            })
            .build()
        com.google.firebase.auth.PhoneAuthProvider.verifyPhoneNumber(options)
    }

    suspend fun verifyPhoneCodeAndSignIn(
        verificationId: String,
        smsCode: String,
        phoneNumber: String,
        displayName: String = "",
        languageCode: String = "tr"
    ): Result<FirestoreUser> {
        return try {
            val sanitized = formatPhoneNumberToE164(phoneNumber)
            Log.d("FirebaseAuthManager", "SMS kodu doğrulanıyor... Numara: $sanitized, Kod: '$smsCode', VerificationId: $verificationId")
            if (verificationId.isBlank()) {
                val err = "Geçersiz doğrulama kimliği. Lütfen SMS kodunu tekrar isteyin."
                Log.e("FirebaseAuthManager", err)
                return Result.failure(Exception(err))
            }
            val credential = com.google.firebase.auth.PhoneAuthProvider.getCredential(verificationId, smsCode.trim())
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: throw Exception("Giriş yapılamadı. Kullanıcı bulunamadı.")

            val uid = firebaseUser.uid
            val finalName = displayName.ifBlank { firebaseUser.displayName ?: "Kullanıcı ($sanitized)" }
            val finalEmail = firebaseUser.email ?: "$sanitized@soyaagaci.com"

            Log.d("FirebaseAuthManager", "SMS Doğrulama BAŞARILI! UID: $uid, İsim: $finalName, E-posta: $finalEmail")

            val userProfile = FirestoreUser(
                uid = uid,
                name = finalName,
                email = finalEmail,
                authProvider = "phone",
                lastLoginAt = System.currentTimeMillis(),
                languageCode = languageCode
            )

            try {
                saveUserToFirestore(userProfile)
            } catch (e: Exception) {
                Log.e("FirebaseAuthManager", "Profil Firestore'a kaydedilirken hata oluştu: ${e.message}", e)
            }

            Result.success(userProfile)
        } catch (e: Exception) {
            Log.e("FirebaseAuthManager", "SMS Kod Doğrulama Hatası! Numara: $phoneNumber, Kod: $smsCode", e)
            val detailMsg = e.localizedMessage ?: "SMS doğrulama kodu hatalı veya süresi dolmuş."
            Result.failure(Exception(detailMsg))
        }
    }

    suspend fun signInWithPhoneNumber(
        phoneNumber: String,
        displayName: String = "",
        languageCode: String = "tr"
    ): Result<FirestoreUser> {
        return try {
            val sanitized = phoneNumber.replace(" ", "").trim()
            val uid = "phone_${sanitized.replace("+", "").replace("-", "")}"
            val finalName = displayName.ifBlank { "Kullanıcı (${if (sanitized.length > 5) sanitized.take(6) + "..." else sanitized})" }
            val finalEmail = "$sanitized@soyaagaci.com"

            val userProfile = FirestoreUser(
                uid = uid,
                name = finalName,
                email = finalEmail,
                authProvider = "phone",
                lastLoginAt = System.currentTimeMillis(),
                languageCode = languageCode
            )

            try {
                saveUserToFirestore(userProfile)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            Result.success(userProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<String> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success("Şifre sıfırlama bağlantısı $email adresine gönderildi.")
        } catch (e: Exception) {
            Result.failure(Exception(translateFirebaseError(e)))
        }
    }

    suspend fun updatePassword(currentPassword: String, newPassword: String): Result<String> {
        return try {
            val user = auth.currentUser ?: throw Exception("Oturum açmış kullanıcı bulunamadı. Lütfen tekrar giriş yapın.")
            val email = user.email
            if (!email.isNullOrBlank() && currentPassword.isNotBlank()) {
                val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, currentPassword)
                user.reauthenticate(credential).await()
            }
            user.updatePassword(newPassword).await()
            Result.success("Şifreniz başarıyla güncellendi.")
        } catch (e: Exception) {
            Result.failure(Exception(translateFirebaseError(e)))
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

    suspend fun generateUniqueUserCode(uid: String? = null): String {
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

    private suspend fun saveUserToFirestore(user: FirestoreUser): String {
        return try {
            val docRef = firestore.collection("users").document(user.uid)
            val docSnap = try { docRef.get().await() } catch (e: Exception) { null }

            var userCode = user.userCode.ifBlank { docSnap?.getString("userCode") ?: "" }
            if (userCode.isBlank()) {
                userCode = generateDeterministicUserCode(user.uid)
            }

            val userMap = mapOf(
                "uid" to user.uid,
                "userCode" to userCode,
                "name" to user.name,
                "email" to user.email,
                "authProvider" to user.authProvider,
                "createdAt" to (docSnap?.getLong("createdAt") ?: user.createdAt),
                "lastLoginAt" to user.lastLoginAt,
                "languageCode" to user.languageCode,
                "role" to user.role
            )
            docRef.set(userMap, SetOptions.merge()).await()
            userCode
        } catch (e: Exception) {
            Log.e("FirebaseAuthManager", "saveUserToFirestore hatası: ${e.message}", e)
            user.userCode
        }
    }

    private suspend fun fetchUserProfileFromFirestore(uid: String): FirestoreUser? {
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            if (doc.exists()) {
                FirestoreUser(
                    uid = doc.getString("uid") ?: uid,
                    userCode = doc.getString("userCode") ?: "",
                    name = doc.getString("name") ?: "",
                    email = doc.getString("email") ?: "",
                    authProvider = doc.getString("authProvider") ?: "email",
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    lastLoginAt = doc.getLong("lastLoginAt") ?: System.currentTimeMillis(),
                    languageCode = doc.getString("languageCode") ?: "tr",
                    role = doc.getString("role") ?: "user"
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun searchUserByCode(code: String): Result<FirestoreUser> {
        return try {
            val cleanCode = code.trim().uppercase()
            if (cleanCode.length < 3) {
                return Result.failure(Exception("Lütfen geçerli bir 6 haneli kod girin."))
            }
            val querySnap = firestore.collection("users")
                .whereEqualTo("userCode", cleanCode)
                .get()
                .await()
            if (!querySnap.isEmpty) {
                val doc = querySnap.documents[0]
                val foundUser = FirestoreUser(
                    uid = doc.getString("uid") ?: doc.id,
                    userCode = doc.getString("userCode") ?: cleanCode,
                    name = doc.getString("name") ?: "Kullanıcı",
                    email = doc.getString("email") ?: "",
                    authProvider = doc.getString("authProvider") ?: "email",
                    languageCode = doc.getString("languageCode") ?: "tr",
                    role = doc.getString("role") ?: "user"
                )
                Result.success(foundUser)
            } else {
                Result.failure(Exception("Bu koda ($cleanCode) ait kullanıcı bulunamadı."))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Kullanıcı aranamadı: ${e.message}"))
        }
    }

    fun fetchAndSaveFcmToken() {
        val uid = auth.currentUser?.uid ?: return
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        if (!token.isNullOrBlank()) {
                            MyFirebaseMessagingService.sendTokenToFirestore(token)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e("FirebaseAuthManager", "FCM token alma hatası: ${e.message}")
        }
    }

    private fun translateFirebaseError(e: Exception): String {
        val message = e.message ?: ""
        return when {
            message.contains("The email address is badly formatted", ignoreCase = true) ->
                "Geçersiz e-posta adresi formatı."
            message.contains("The email address is already in use", ignoreCase = true) ->
                "Bu e-posta adresi zaten başka bir hesap tarafından kullanılıyor."
            message.contains("Password should be at least 6 characters", ignoreCase = true) ->
                "Şifre en az 6 karakter olmalıdır."
            message.contains("There is no user record corresponding to this identifier", ignoreCase = true) ||
                    message.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ->
                "E-posta veya şifre hatalı. Kayıtlı hesap bulunamadı."
            message.contains("network error", ignoreCase = true) ->
                "İnternet bağlantınızı kontrol edip tekrar deneyin."
            else -> e.localizedMessage ?: "Kimlik doğrulama sırasında bir hata oluştu."
        }
    }
}
