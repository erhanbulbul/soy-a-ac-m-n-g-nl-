package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import coil.compose.rememberAsyncImagePainter
import com.example.data.local.UserProfileEntity
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NavyDark
import com.example.ui.theme.SlateCard
import com.example.util.AppLanguage
import com.example.util.LanguageManager

@Composable
fun HomeScreen(
    userProfile: UserProfileEntity?,
    languageCode: String,
    onConfirmSafetyCheck: () -> Unit,
    onToggleDeceasedSimulation: (Boolean) -> Unit,
    onNavigateToFamilyTree: () -> Unit,
    onNavigateToJournal: () -> Unit,
    onNavigateToWillVault: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToEmergencySos: () -> Unit,
    onNavigateToAdvancedFeatures: () -> Unit,
    onNavigateToAuth: () -> Unit,
    onSelectLanguage: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val profile = userProfile ?: UserProfileEntity()
    var showLanguageDialog by remember { mutableStateOf(false) }

    val currentLang = AppLanguage.entries.find { it.code == languageCode } ?: AppLanguage.TURKISH

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = SlateCard,
                tonalElevation = 8.dp,
                modifier = Modifier.height(72.dp)
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Already home */ },
                    icon = { Icon(Icons.Default.AccountTree, contentDescription = LanguageManager.getString("family_tree", languageCode), tint = GoldPrimary) },
                    label = { Text(LanguageManager.getString("family_tree", languageCode), color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = GoldPrimary.copy(alpha = 0.2f))
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToJournal,
                    icon = { Icon(Icons.Default.Book, contentDescription = LanguageManager.getString("journal", languageCode), tint = Color.Gray) },
                    label = { Text(LanguageManager.getString("journal", languageCode), color = Color.Gray, fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToChat,
                    icon = { Icon(Icons.Default.Forum, contentDescription = LanguageManager.getString("chat", languageCode), tint = Color.Gray) },
                    label = { Text(LanguageManager.getString("chat", languageCode), color = Color.Gray, fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToWillVault,
                    icon = { Icon(Icons.Default.Description, contentDescription = LanguageManager.getString("will_vault", languageCode), tint = Color.Gray) },
                    label = { Text(LanguageManager.getString("will_vault", languageCode), color = Color.Gray, fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToSettings,
                    icon = { Icon(Icons.Default.Person, contentDescription = LanguageManager.getString("profile", languageCode), tint = Color.Gray) },
                    label = { Text(LanguageManager.getString("profile", languageCode), color = Color.Gray, fontSize = 11.sp) }
                )
            }
        },
        containerColor = NavyDark,
        modifier = Modifier
            .statusBarsPadding()
            .navigationBarsPadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Top App Bar Header with Profile, ID & Quick Language Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onNavigateToSettings() }
                ) {
                    if (profile.avatarUri.isNotBlank()) {
                        Image(
                            painter = rememberAsyncImagePainter(profile.avatarUri),
                            contentDescription = "Profil Resmi",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(1.dp, GoldPrimary, CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = profile.name.take(1).ifEmpty { "K" },
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ID: ${profile.userCode}",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldLight
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy ID",
                                tint = GoldLight,
                                modifier = Modifier
                                    .size(12.dp)
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString(profile.userCode))
                                        Toast
                                            .makeText(
                                                context,
                                                "ID Kopyalandı: ${profile.userCode}",
                                                Toast.LENGTH_SHORT
                                            )
                                            .show()
                                    }
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Quick Language Switcher Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(SlateCard)
                            .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .clickable { showLanguageDialog = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = currentLang.flag, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentLang.code.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Settings / Profile Gear Icon
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SlateCard)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = GoldLight)
                    }
                }
            }



            // Offline Emergency SOS Card (Bluetooth Mesh)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable { onNavigateToEmergencySos() },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                shape = RoundedCornerShape(20.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFFEF4444), GoldPrimary)
                    )
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDC2626)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = LanguageManager.getString("sos_bluetooth_title", languageCode),
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = LanguageManager.getString("sos_bluetooth_sub", languageCode),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFECACA)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onNavigateToEmergencySos,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("SOS Aç", color = Color(0xFF7F1D1D), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Hero Legacy Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_legacy_hero_1784785118282),
                        contentDescription = "Legacy Vault",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = LanguageManager.getString("hero_title", languageCode),
                            style = MaterialTheme.typography.titleMedium,
                            color = GoldLight,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = LanguageManager.getString("hero_sub", languageCode),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                }
            }

            // AI & Advanced Features Spotlight Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable { onNavigateToAdvancedFeatures() },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF818CF8))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4F46E5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "✨ AI & İleri Seviye Özellikler",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Fotoğraf Onarımı, Miras Envanteri & Göç Haritası",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFC7D2FE)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Button(
                        onClick = onNavigateToAdvancedFeatures,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Keşfet", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Quick Feature Grid
            Text(
                text = LanguageManager.getString("dashboard_access", languageCode),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardCard(
                        title = LanguageManager.getString("family_tree", languageCode),
                        subtitle = LanguageManager.getString("tree_sub", languageCode),
                        icon = Icons.Default.AccountTree,
                        modifier = Modifier.weight(1f),
                        testTag = "nav_family_tree",
                        onClick = onNavigateToFamilyTree
                    )
                    DashboardCard(
                        title = LanguageManager.getString("journal", languageCode),
                        subtitle = LanguageManager.getString("journal_sub", languageCode),
                        icon = Icons.Default.Book,
                        modifier = Modifier.weight(1f),
                        testTag = "nav_journal",
                        onClick = onNavigateToJournal
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardCard(
                        title = LanguageManager.getString("will_vault", languageCode),
                        subtitle = LanguageManager.getString("will_sub", languageCode),
                        icon = Icons.Default.Description,
                        modifier = Modifier.weight(1f),
                        testTag = "nav_will_vault",
                        onClick = onNavigateToWillVault
                    )
                    DashboardCard(
                        title = LanguageManager.getString("chat", languageCode),
                        subtitle = LanguageManager.getString("chat_sub", languageCode),
                        icon = Icons.Default.Forum,
                        modifier = Modifier.weight(1f),
                        testTag = "nav_chat",
                        onClick = onNavigateToChat
                    )
                }
                DashboardCard(
                    title = LanguageManager.getString("settings", languageCode),
                    subtitle = LanguageManager.getString("settings_sub", languageCode),
                    icon = Icons.Default.Settings,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "nav_settings",
                    onClick = onNavigateToSettings
                )
            }
        }

        // Language Switcher Dialog (6 Languages)
        if (showLanguageDialog) {
            AlertDialog(
                onDismissRequest = { showLanguageDialog = false },
                title = { Text(LanguageManager.getString("select_language", languageCode), color = Color.White) },
                containerColor = SlateCard,
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLanguage.entries.forEach { lang ->
                            val isSelected = lang.code == languageCode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) GoldPrimary else Color(0xFF0F172A))
                                    .clickable {
                                        onSelectLanguage(lang.code)
                                        showLanguageDialog = false
                                        Toast.makeText(context, "${lang.displayName}", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = lang.flag, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = lang.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLanguageDialog = false }) {
                        Text(LanguageManager.getString("close", languageCode), color = GoldLight)
                    }
                }
            )
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .testTag(testTag)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(GoldPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = GoldLight)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                maxLines = 2
            )
        }
    }
}

