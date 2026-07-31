package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NavyDark
import com.example.ui.theme.SlateCard
import com.example.util.BiometricAuthHelper

/**
 * Biyometrik (Parmak İzi / Yüz Tanıma) veya Cihaz PIN Şifresi ile Korunan Kilit Ekranı Komponenti.
 * Başarılı doğrulama gerçekleşmeden hassas içerik asla ekrana çizilmez.
 */
@Composable
fun BiometricSecurityLockOverlay(
    title: String,
    onUnlocked: () -> Unit = {},
    content: @Composable () -> Unit
) {
    var isUnlocked by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val triggerAuth = {
        BiometricAuthHelper.authenticate(
            context = context,
            title = "$title Güvenlik Doğrulaması",
            subtitle = "Gizli verilerinize erişmek için parmak izinizi okutun veya cihaz PIN şifrenizi girin",
            onSuccess = {
                isUnlocked = true
                onUnlocked()
            },
            onError = { err ->
                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
            }
        )
    }

    LaunchedEffect(Unit) {
        triggerAuth()
    }

    if (isUnlocked) {
        content()
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NavyDark),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(SlateCard)
                        .border(2.dp, GoldPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = GoldLight,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "$title Kilitli",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Bu alan biyometrik doğrulama veya cihaz PIN şifresi ile korunmaktadır. İçeriği görüntülemek için lütfen doğrulama yapın.",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = { triggerAuth() },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kilidi Aç (Biometric / PIN)", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
