package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.firebase.FirebaseAuthManager
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NavyDark
import com.example.ui.theme.SlateCard
import com.example.util.AppLanguage
import com.example.util.LanguageManager
import kotlinx.coroutines.launch

enum class AuthScreenMode {
    WELCOME,   // Karşılama Ekranı
    LOGIN,     // Giriş Yap Ekranı
    REGISTER   // Kayıt Ol Ekranı
}

@Composable
fun AuthScreen(
    currentLanguageCode: String,
    onLanguageSelected: (String) -> Unit,
    onLoginSuccess: (name: String, email: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val firebaseAuthManager = remember { FirebaseAuthManager(context) }

    val authPrefs = remember {
        context.getSharedPreferences("soyaagaci_registered_users", Context.MODE_PRIVATE)
    }

    var currentScreenMode by remember { mutableStateOf(AuthScreenMode.WELCOME) }

    // Form inputs
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // State indicators
    var isSubmitting by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }

    // Phone Auth Modal State
    var showPhoneAuthDialog by remember { mutableStateOf(false) }
    var phoneNumberInput by remember { mutableStateOf("+90 ") }
    var phoneNameInput by remember { mutableStateOf("") }
    var phoneSmsCodeInput by remember { mutableStateOf("") }
    var isPhoneCodeSent by remember { mutableStateOf(false) }
    var phoneVerificationId by remember { mutableStateOf("") }

    val languages = AppLanguage.entries

    fun isValidEmail(target: String): Boolean {
        val trimmed = target.trim()
        return trimmed.isNotBlank() &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches() &&
                trimmed.contains(".") &&
                trimmed.substringAfterLast(".").length >= 2
    }

    val handleGoogleSignIn: () -> Unit = {
        if (!isSubmitting) {
            isSubmitting = true
            coroutineScope.launch {
                val result = firebaseAuthManager.performGoogleSignIn(context, currentLanguageCode)
                isSubmitting = false
                result.fold(
                    onSuccess = { userProfile ->
                        val emailKey = userProfile.email.lowercase()
                        authPrefs.edit()
                            .putString("user_email_$emailKey", userProfile.email)
                            .putString("user_name_$emailKey", userProfile.name)
                            .putBoolean("is_logged_in", true)
                            .apply()
                        Toast.makeText(
                            context,
                            "✓ Google ile Giriş Başarılı! Hoş geldiniz ${userProfile.name}",
                            Toast.LENGTH_LONG
                        ).show()
                        onLoginSuccess(userProfile.name, userProfile.email)
                    },
                    onFailure = { err ->
                        Toast.makeText(context, err.message ?: "Google ile giriş başarısız.", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    val handleFacebookSignIn: () -> Unit = {
        Toast.makeText(
            context,
            "Facebook ile gerçek giriş yapılabilmesi için Facebook App ID ve Facebook SDK yapılandırması gereklidir.",
            Toast.LENGTH_LONG
        ).show()
    }

    val handleSocialClick: (String) -> Unit = { provider ->
        when (provider) {
            "Google" -> handleGoogleSignIn()
            "Facebook" -> handleFacebookSignIn()
            "Telefon" -> showPhoneAuthDialog = true
            else -> {
                Toast.makeText(context, "$provider ile giriş henüz desteklenmiyor.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Password Reset Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            containerColor = SlateCard,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = GoldLight)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Firebase Şifre Sıfırlama", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Hesabınıza ait e-posta adresinizi giriniz. Firebase Authentication üzerinden şifre sıfırlama bağlantısı gönderilecektir.",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("E-Posta Adresi") },
                        placeholder = { Text("ornek@soyaagaci.com") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = GoldLight) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = GoldLight,
                            unfocusedLabelColor = Color.LightGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isValidEmail(resetEmail)) {
                            Toast.makeText(context, "Lütfen geçerli bir e-posta adresi giriniz.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        coroutineScope.launch {
                            val res = firebaseAuthManager.sendPasswordResetEmail(resetEmail)
                            res.fold(
                                onSuccess = { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    showForgotPasswordDialog = false
                                },
                                onFailure = { err ->
                                    Toast.makeText(context, "Hata: ${err.message}", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black)
                ) {
                    Text("Sıfırlama Bağlantısı Gönder", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("İptal", color = Color.Gray)
                }
            }
        )
    }

    if (showPhoneAuthDialog) {
        AlertDialog(
            onDismissRequest = {
                showPhoneAuthDialog = false
                isPhoneCodeSent = false
            },
            containerColor = SlateCard,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = GoldLight)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("📱 Telefon No ile Giriş / Kayıt", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Telefon numaranızı girerek SMS doğrulama kodu ile hızlıca giriş yapın veya kaydolun.",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = phoneNumberInput,
                        onValueChange = { phoneNumberInput = it },
                        label = { Text("Telefon Numarası") },
                        placeholder = { Text("+90 5XX XXX XX XX") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = GoldLight) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = GoldLight,
                            unfocusedLabelColor = Color.LightGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = phoneNameInput,
                        onValueChange = { phoneNameInput = it },
                        label = { Text("Ad Soyad (İsteğe Bağlı)") },
                        placeholder = { Text("Örn: Ali Yılmaz") },
                        leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null, tint = GoldLight) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = GoldLight,
                            unfocusedLabelColor = Color.LightGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    if (isPhoneCodeSent) {
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF10B981)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "📩 SMS Doğrulama Kodu Gönderildi.\nLütfen telefonunuza gelen 6 haneli doğrulama kodunu giriniz.",
                                color = Color(0xFF6EE7B7),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        OutlinedTextField(
                            value = phoneSmsCodeInput,
                            onValueChange = { phoneSmsCodeInput = it },
                            label = { Text("6 Haneli SMS Doğrulama Kodu") },
                            placeholder = { Text("Örn: 482915") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = GoldLight) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = Color.Gray,
                                focusedLabelColor = GoldLight,
                                unfocusedLabelColor = Color.LightGray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        TextButton(
                            onClick = {
                                isPhoneCodeSent = false
                                phoneSmsCodeInput = ""
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Numarayı Değiştir / Tekrar Kod İste", color = GoldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanPhone = phoneNumberInput.trim()
                        if (cleanPhone.length < 10) {
                            Toast.makeText(context, "Lütfen geçerli bir telefon numarası giriniz.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (!isPhoneCodeSent) {
                            val activity = context as? android.app.Activity
                            if (activity != null) {
                                isSubmitting = true
                                firebaseAuthManager.sendPhoneVerificationCode(
                                    activity = activity,
                                    phoneNumber = cleanPhone,
                                    onCodeSent = { vId ->
                                        isSubmitting = false
                                        phoneVerificationId = vId
                                        isPhoneCodeSent = true
                                        phoneSmsCodeInput = ""
                                        Toast.makeText(context, "✓ SMS doğrulama kodu $cleanPhone numarasına gönderildi. Lütfen gelen kodu giriniz.", Toast.LENGTH_LONG).show()
                                    },
                                    onVerificationCompleted = { userProfile ->
                                        isSubmitting = false
                                        showPhoneAuthDialog = false
                                        isPhoneCodeSent = false
                                        Toast.makeText(context, "✓ Telefon Doğrulaması Başarılı! Hoş geldiniz ${userProfile.name}", Toast.LENGTH_LONG).show()
                                        onLoginSuccess(userProfile.name, userProfile.email)
                                    },
                                    onVerificationFailed = { errMsg ->
                                        isSubmitting = false
                                        Toast.makeText(context, "SMS Doğrulama Hatası: $errMsg", Toast.LENGTH_LONG).show()
                                    }
                                )
                            } else {
                                Toast.makeText(context, "Sistem hatası: Uygulama pencerisine erişilemedi.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            if (phoneSmsCodeInput.trim().isEmpty()) {
                                Toast.makeText(context, "Lütfen telefonunuza gelen SMS doğrulama kodunu giriniz.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isSubmitting = true
                            coroutineScope.launch {
                                val result = firebaseAuthManager.verifyPhoneCodeAndSignIn(
                                    verificationId = phoneVerificationId,
                                    smsCode = phoneSmsCodeInput.trim(),
                                    phoneNumber = cleanPhone,
                                    displayName = phoneNameInput.trim(),
                                    languageCode = currentLanguageCode
                                )
                                isSubmitting = false

                                result.fold(
                                    onSuccess = { userProfile ->
                                        showPhoneAuthDialog = false
                                        isPhoneCodeSent = false
                                        Toast.makeText(
                                            context,
                                            "✓ Telefon ile Giriş / Kayıt Başarılı! Hoş geldiniz ${userProfile.name}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        onLoginSuccess(userProfile.name, userProfile.email)
                                    },
                                    onFailure = { err ->
                                        Toast.makeText(context, err.message ?: "Giriş başarısız.", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black)
                ) {
                    Text(if (isPhoneCodeSent) "Doğrula ve Giriş Yap" else "SMS Kodu Gönder", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPhoneAuthDialog = false
                        isPhoneCodeSent = false
                    }
                ) {
                    Text("İptal", color = Color.Gray)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NavyDark, Color(0xFF1E293B), Color(0xFF0F172A))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Logo & Title
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(GoldLight, GoldPrimary)))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_family_app_icon_1784785105405),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = LanguageManager.getString("app_title", currentLanguageCode),
                style = MaterialTheme.typography.headlineSmall,
                color = GoldLight,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Firebase Authentication & Firestore Bulut Tabanlı Soyağacı Günlüğü",
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Language Selector Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = "Language",
                    tint = GoldLight,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Dil / Language:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(languages) { lang ->
                    val isSelected = lang.code == currentLanguageCode
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) GoldPrimary else SlateCard,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onLanguageSelected(lang.code) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(text = lang.flag, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = lang.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.Black else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Top Tab Navigation Bar (Karşılama / Giriş Yap / Kayıt Ol)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SlateCard)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AuthTabItem(
                    title = "✨ Karşılama",
                    isSelected = currentScreenMode == AuthScreenMode.WELCOME,
                    onClick = { currentScreenMode = AuthScreenMode.WELCOME }
                )
                AuthTabItem(
                    title = "🔑 Giriş Yap",
                    isSelected = currentScreenMode == AuthScreenMode.LOGIN,
                    onClick = { currentScreenMode = AuthScreenMode.LOGIN }
                )
                AuthTabItem(
                    title = "📝 Kayıt Ol",
                    isSelected = currentScreenMode == AuthScreenMode.REGISTER,
                    onClick = { currentScreenMode = AuthScreenMode.REGISTER }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Screen Mode Contents
            when (currentScreenMode) {
                AuthScreenMode.WELCOME -> {
                    WelcomeContent(
                        onNavigateToLogin = { currentScreenMode = AuthScreenMode.LOGIN },
                        onNavigateToRegister = { currentScreenMode = AuthScreenMode.REGISTER },
                        onGoogleSignInClick = handleGoogleSignIn,
                        onFacebookSignInClick = handleFacebookSignIn,
                        onPhoneSignInClick = { showPhoneAuthDialog = true }
                    )
                }

                AuthScreenMode.LOGIN -> {
                    LoginContent(
                        email = email,
                        onEmailChange = { email = it },
                        password = password,
                        onPasswordChange = { password = it },
                        isPasswordVisible = isPasswordVisible,
                        onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
                        isSubmitting = isSubmitting,
                        onForgotPasswordClick = {
                            resetEmail = email
                            showForgotPasswordDialog = true
                        },
                        onLoginClick = {
                            val trimmedEmail = email.trim()
                            if (!isValidEmail(trimmedEmail)) {
                                Toast.makeText(context, "Geçersiz e-posta formatı!", Toast.LENGTH_SHORT).show()
                                return@LoginContent
                            }
                            if (password.length < 6) {
                                Toast.makeText(context, "Şifreniz en az 6 karakter olmalıdır.", Toast.LENGTH_SHORT).show()
                                return@LoginContent
                            }

                            isSubmitting = true
                            coroutineScope.launch {
                                val result = firebaseAuthManager.loginUserWithEmail(trimmedEmail, password)
                                isSubmitting = false

                                result.fold(
                                    onSuccess = { userProfile ->
                                        Toast.makeText(
                                            context,
                                            "✓ Firebase ile Giriş Başarılı! Hoş geldiniz ${userProfile.name}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        onLoginSuccess(userProfile.name, userProfile.email)
                                    },
                                    onFailure = { err ->
                                        // If Firebase remote fails, check fallback local auth
                                        val emailKey = trimmedEmail.lowercase()
                                        if (authPrefs.contains("user_email_$emailKey")) {
                                            val savedPass = authPrefs.getString("user_password_$emailKey", "")
                                            if (savedPass == password || savedPass == "OAUTH_SOCIAL_PASS") {
                                                val savedName = authPrefs.getString("user_name_$emailKey", "Kullanıcı") ?: "Kullanıcı"
                                                Toast.makeText(context, "✓ Giriş Başarılı! Hoş geldiniz $savedName.", Toast.LENGTH_SHORT).show()
                                                onLoginSuccess(savedName, trimmedEmail)
                                                return@launch
                                            }
                                        }
                                        Toast.makeText(context, err.message ?: "Giriş başarısız.", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        },
                        onNavigateToRegister = { currentScreenMode = AuthScreenMode.REGISTER },
                        onSocialLoginClick = handleSocialClick
                    )
                }

                AuthScreenMode.REGISTER -> {
                    RegisterContent(
                        name = name,
                        onNameChange = { name = it },
                        email = email,
                        onEmailChange = { email = it },
                        password = password,
                        onPasswordChange = { password = it },
                        confirmPassword = confirmPassword,
                        onConfirmPasswordChange = { confirmPassword = it },
                        isPasswordVisible = isPasswordVisible,
                        onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
                        isSubmitting = isSubmitting,
                        onRegisterClick = {
                            val trimmedName = name.trim()
                            val trimmedEmail = email.trim()

                            if (trimmedName.length < 2) {
                                Toast.makeText(context, "Lütfen geçerli bir Ad Soyad giriniz.", Toast.LENGTH_SHORT).show()
                                return@RegisterContent
                            }
                            if (!isValidEmail(trimmedEmail)) {
                                Toast.makeText(context, "Lütfen geçerli bir e-posta adresi giriniz.", Toast.LENGTH_SHORT).show()
                                return@RegisterContent
                            }
                            if (password.length < 6) {
                                Toast.makeText(context, "Şifreniz en az 6 karakter olmalıdır.", Toast.LENGTH_SHORT).show()
                                return@RegisterContent
                            }
                            if (password != confirmPassword) {
                                Toast.makeText(context, "Şifreler uyuşmuyor! Lütfen kontrol edin.", Toast.LENGTH_SHORT).show()
                                return@RegisterContent
                            }

                            isSubmitting = true
                            coroutineScope.launch {
                                val result = firebaseAuthManager.registerUserWithEmail(
                                    name = trimmedName,
                                    email = trimmedEmail,
                                    password = password,
                                    languageCode = currentLanguageCode
                                )
                                isSubmitting = false

                                result.fold(
                                    onSuccess = { userProfile ->
                                        val emailKey = trimmedEmail.lowercase()
                                        authPrefs.edit()
                                            .putString("user_email_$emailKey", trimmedEmail)
                                            .putString("user_name_$emailKey", trimmedName)
                                            .putString("user_password_$emailKey", password)
                                            .apply()

                                        Toast.makeText(
                                            context,
                                            "🎉 Hesabınız Firebase Auth'a kaydedildi ve profiliniz Firestore veritabanına otomatik eklendi!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        onLoginSuccess(userProfile.name, userProfile.email)
                                    },
                                    onFailure = { err ->
                                        Toast.makeText(context, err.message ?: "Kayıt başarısız.", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        },
                        onNavigateToLogin = { currentScreenMode = AuthScreenMode.LOGIN },
                        onSocialLoginClick = handleSocialClick
                    )
                }
            }
        }
    }
}

@Composable
fun RowScope.AuthTabItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) GoldPrimary else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) Color.Black else Color.LightGray,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun WelcomeContent(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    onFacebookSignInClick: () -> Unit,
    onPhoneSignInClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCard.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Soyağacımın Günlüğüne Hoş Geldiniz! 👋",
                style = MaterialTheme.typography.titleMedium,
                color = GoldLight,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Aile köklerinizi keşfedin, geçmişinizi belgeleyin ve gelecek nesillere güvenli bulut altyapısı ile aktarın.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Feature Highlights
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FeatureHighlightItem(
                    icon = Icons.Default.FamilyRestroom,
                    title = "Interaktif Soyağacı & Kuşaklar",
                    description = "Atalarınızı ve akrabalarınızı interaktif ağaç yapısında düzenleyin."
                )
                FeatureHighlightItem(
                    icon = Icons.Default.CloudDone,
                    title = "Firebase Firestore Bulut Eşitleme",
                    description = "Verileriniz anında Firebase Firestore veritabanına yedeklenir."
                )
                FeatureHighlightItem(
                    icon = Icons.Default.MenuBook,
                    title = "Sesli & Görsel Günlük Kasası",
                    description = "Aile anılarınızı ses kayıtları ve fotoğraflarla dijitalleştirin."
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Buttons
            Button(
                onClick = onNavigateToRegister,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("welcome_register_button"),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("🚀 Normal E-Posta ile Kayıt Ol", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onNavigateToLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("welcome_login_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, GoldLight)
            ) {
                Text("🔑 E-Posta & Şifre ile Giriş Yap", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.weight(1f).height(1.dp).background(Color.DarkGray))
                Text("  VEYA SOSYAL HESAP / TELEFON ILE  ", color = Color.Gray, fontSize = 11.sp)
                Box(modifier = Modifier.weight(1f).height(1.dp).background(Color.DarkGray))
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onGoogleSignInClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("welcome_google_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    GoogleBrandIcon()
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Google", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = onFacebookSignInClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("welcome_facebook_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    FacebookBrandIcon()
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Facebook", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = onPhoneSignInClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("welcome_phone_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Telefon", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun FeatureHighlightItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NavyDark)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(GoldPrimary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = GoldLight, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(description, color = Color.LightGray, fontSize = 11.sp)
        }
    }
}

@Composable
fun LoginContent(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    isSubmitting: Boolean,
    onForgotPasswordClick: () -> Unit,
    onLoginClick: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onSocialLoginClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCard.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Firebase Oturum Açma 🔑",
                style = MaterialTheme.typography.titleMedium,
                color = GoldLight,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("E-Posta Adresi") },
                placeholder = { Text("ornek@soyaagaci.com") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = GoldLight) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_email_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = GoldLight,
                    unfocusedLabelColor = Color.LightGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Şifre") },
                placeholder = { Text("••••••••") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = GoldLight) },
                trailingIcon = {
                    IconButton(onClick = onTogglePasswordVisibility) {
                        Icon(
                            if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle password",
                            tint = Color.Gray
                        )
                    }
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_password_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = GoldLight,
                    unfocusedLabelColor = Color.LightGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onForgotPasswordClick) {
                    Text("Şifremi Unuttum?", color = GoldLight, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onLoginClick,
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("auth_submit_button"),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.Black, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Giriş Yapılıyor...", color = Color.Black, fontWeight = FontWeight.Bold)
                } else {
                    Text("Firebase ile Giriş Yap", style = MaterialTheme.typography.titleMedium, color = Color.Black, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Hesabınız yok mu? Hemen kayıt olun",
                style = MaterialTheme.typography.bodySmall,
                color = GoldLight,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onNavigateToRegister() }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text("Hızlı / Sosyal Hesap İle Giriş:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth()
            ) {
                SocialLoginChip(
                    provider = "Google",
                    brandColor = Color.White,
                    iconComposable = { GoogleBrandIcon() }
                ) { onSocialLoginClick("Google") }

                SocialLoginChip(
                    provider = "Facebook",
                    brandColor = Color(0xFF1877F2),
                    iconComposable = { FacebookBrandIcon() }
                ) { onSocialLoginClick("Facebook") }

                SocialLoginChip(
                    provider = "Telefon",
                    brandColor = Color(0xFF10B981),
                    iconComposable = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                ) { onSocialLoginClick("Telefon") }
            }
        }
    }
}

@Composable
fun RegisterContent(
    name: String,
    onNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    isSubmitting: Boolean,
    onRegisterClick: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onSocialLoginClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCard.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Yeni Kayıt & Firestore Veritabanı 📝",
                style = MaterialTheme.typography.titleMedium,
                color = GoldLight,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .padding(8.dp)
            ) {
                Icon(Icons.Default.CloudDone, contentDescription = null, tint = GoldLight, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Verileriniz otomatik olarak Firestore veritabanına kaydedilir.",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Ad Soyad") },
                placeholder = { Text("Örn: Ahmet Yılmaz") },
                leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null, tint = GoldLight) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_name_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = GoldLight,
                    unfocusedLabelColor = Color.LightGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("E-Posta Adresi") },
                placeholder = { Text("ornek@soyaagaci.com") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = GoldLight) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_email_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = GoldLight,
                    unfocusedLabelColor = Color.LightGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Şifre (En az 6 karakter)") },
                placeholder = { Text("••••••••") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = GoldLight) },
                trailingIcon = {
                    IconButton(onClick = onTogglePasswordVisibility) {
                        Icon(
                            if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle password",
                            tint = Color.Gray
                        )
                    }
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_password_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = GoldLight,
                    unfocusedLabelColor = Color.LightGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = { Text("Şifre Tekrarı") },
                placeholder = { Text("••••••••") },
                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GoldLight) },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = GoldLight,
                    unfocusedLabelColor = Color.LightGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onRegisterClick,
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("auth_submit_button"),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.Black, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Firestore'a Kaydediliyor...", color = Color.Black, fontWeight = FontWeight.Bold)
                } else {
                    Text("Kayıt Ol ve Firestore'a Kaydet", style = MaterialTheme.typography.titleMedium, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Zaten hesabınız var mı? Giriş yapın",
                style = MaterialTheme.typography.bodySmall,
                color = GoldLight,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onNavigateToLogin() }
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text("Hızlı / Sosyal Hesap İle Kaydolun:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth()
            ) {
                SocialLoginChip(
                    provider = "Google",
                    brandColor = Color.White,
                    iconComposable = { GoogleBrandIcon() }
                ) { onSocialLoginClick("Google") }

                SocialLoginChip(
                    provider = "Facebook",
                    brandColor = Color(0xFF1877F2),
                    iconComposable = { FacebookBrandIcon() }
                ) { onSocialLoginClick("Facebook") }

                SocialLoginChip(
                    provider = "Telefon",
                    brandColor = Color(0xFF10B981),
                    iconComposable = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                ) { onSocialLoginClick("Telefon") }
            }
        }
    }
}

@Composable
fun SocialLoginChip(
    provider: String,
    brandColor: Color,
    iconComposable: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(brandColor)
            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            .clickable { onClick() }
            .testTag("social_login_${provider.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        iconComposable()
    }
}

@Composable
fun GoogleBrandIcon() {
    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "G",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            color = Color(0xFF4285F4)
        )
    }
}

@Composable
fun FacebookBrandIcon() {
    Text(
        text = "f",
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        color = Color.White
    )
}

@Composable
fun XBrandIcon() {
    Text(
        text = "𝕏",
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = Color.White
    )
}

@Composable
fun LinkedInBrandIcon() {
    Text(
        text = "in",
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = Color.White
    )
}
