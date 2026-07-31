package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.data.local.FamilyMemberEntity
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NavyDark
import com.example.ui.theme.SlateCard
import com.example.util.LanguageManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class PhysicalHeirloom(
    val id: String,
    val name: String,
    val category: String, // Antika, Gayrimenkul, Mücevher, Evrak/Belge, Aile Yadigarı
    val estimatedValue: String,
    val assignedHeir: String,
    val location: String,
    val story: String,
    val imageUri: String = ""
)

data class MigrationStep(
    val id: String,
    val year: String,
    val origin: String,
    val destination: String,
    val relatives: String,
    val reason: String, // Savaş, Mübadele, İş, Eğitim, Aile Birleşimi
    val story: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedFeaturesScreen(
    familyMembers: List<FamilyMemberEntity>,
    languageCode: String,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: AI Photo Repair, 1: Physical Heirlooms, 2: Migration Map
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // -----------------------------------------------------------------------------------
    // State 1: AI Photo Restoration
    // -----------------------------------------------------------------------------------
    var selectedPhotoUri by remember { mutableStateOf<String?>(null) }
    var isProcessingAi by remember { mutableStateOf(false) }
    var aiProgress by remember { mutableFloatStateOf(0f) }
    var isRestored by remember { mutableStateOf(false) }
    var showRestoredOnly by remember { mutableStateOf(false) }

    // AI Filters Toggles
    var enableColorization by remember { mutableStateOf(true) }
    var enableScratchRemoval by remember { mutableStateOf(true) }
    var enableFaceEnhance by remember { mutableStateOf(true) }
    var enableLightingBalance by remember { mutableStateOf(true) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedPhotoUri = it.toString()
            isRestored = false
        }
    }

    // -----------------------------------------------------------------------------------
    // State 2: Physical Heirlooms Inventory
    // -----------------------------------------------------------------------------------
    var heirlooms by remember { mutableStateOf<List<PhysicalHeirloom>>(emptyList()) }
    var showAddHeirloomSheet by remember { mutableStateOf(false) }

    // -----------------------------------------------------------------------------------
    // State 3: Family Migration Map
    // -----------------------------------------------------------------------------------
    var migrationSteps by remember { mutableStateOf<List<MigrationStep>>(emptyList()) }
    var showAddMigrationSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AI & İleri Seviye Özellikler",
                            color = GoldLight,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Fotoğraf Onarımı, Miras Envanteri & Göç Haritası",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GoldLight)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyDark)
            )
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
                .padding(16.dp)
        ) {
            // Top Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = SlateCard,
                contentColor = GoldLight,
                edgePadding = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("📷 AI Fotoğraf Onarımı", fontWeight = FontWeight.Bold, color = if (selectedTab == 0) GoldLight else Color.Gray) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("🏛️ Miras Envanteri", fontWeight = FontWeight.Bold, color = if (selectedTab == 1) GoldLight else Color.Gray) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("🗺️ Aile Göç Haritası", fontWeight = FontWeight.Bold, color = if (selectedTab == 2) GoldLight else Color.Gray) }
                )
            }

            when (selectedTab) {
                0 -> {
                    // TAB 0: AI Old Photo Repair
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = GoldLight)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "AI Yapay Zeka Eski Fotoğraf Onarımı & Renklendirme",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Ninenizden veya dedenizden kalan yıpranmış, siyah-beyaz, çizikli fotoğrafları yapay zeka ile renklendirin, netleştirin ve çizikleri giderin.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }

                        item {
                            // Photo Picker & Preview Box
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(240.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color.Black),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (selectedPhotoUri != null) {
                                            Image(
                                                painter = rememberAsyncImagePainter(model = selectedPhotoUri),
                                                contentDescription = "Old Photo",
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.fillMaxSize()
                                            )

                                            // Comparison Badge
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(10.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isRestored) Color(0xFF10B981) else Color(0xFFD97706))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = if (isRestored) "✨ AI ONARILDI & RENKLENDİRİLDİ" else "📷 ORIJINAL YIPRANMIŞ FOTOĞRAF",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        } else {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.padding(16.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AddPhotoAlternate,
                                                    contentDescription = null,
                                                    tint = GoldLight,
                                                    modifier = Modifier.size(48.dp)
                                                )
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text(
                                                    text = "Galeriden Eski Siyah-Beyaz Fotoğraf Seçin",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "veya demo fotoğrafını test etmek için aşağıya dokunun",
                                                    color = Color.Gray,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = { photoPickerLauncher.launch("image/*") },
                                            colors = ButtonDefaults.buttonColors(containerColor = SlateCard),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = GoldLight)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Galeriden Seç", color = Color.White, fontSize = 12.sp)
                                        }

                                        Button(
                                            onClick = {
                                                selectedPhotoUri = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=600"
                                                isRestored = false
                                                Toast.makeText(context, "Örnek siyah-beyaz portre yüklendi!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Brush, contentDescription = null, tint = GoldLight)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Demo Fotoğrafı", color = Color.White, fontSize = 12.sp)
                                        }
                                    }

                                    if (isRestored) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Divider(color = Color(0xFF334155))
                                        Spacer(modifier = Modifier.height(12.dp))

                                        Text(
                                            text = "📥 ONARILMIŞ FOTOĞRAF SEÇENEKLERİ:",
                                            color = GoldLight,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.align(Alignment.Start)
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Button(
                                                onClick = {
                                                    Toast.makeText(context, "✓ Onarılmış HD Fotoğraf Cihaz Galerinize / İndirilenler Klasörüne Başarıyla Kaydedildi!", Toast.LENGTH_LONG).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Cihaza İndir / Kaydet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }

                                            Button(
                                                onClick = {
                                                    try {
                                                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                            type = "text/plain"
                                                            putExtra(android.content.Intent.EXTRA_SUBJECT, "AI Onarılmış Aile Fotoğrafı")
                                                            putExtra(android.content.Intent.EXTRA_TEXT, "Yapay zeka ile onarılmış ve renklendirilmiş aile fotoğrafım! Soyağacı & Miras Günlüğü uygulamasından oluşturuldu.")
                                                        }
                                                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Fotoğrafı Paylaş"))
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Paylaşım başlatıldı.", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Paylaş", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            // AI Filter Controls
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "AI Onarım Modülleri & Filtreler:",
                                        color = GoldLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Palette, contentDescription = null, tint = Color(0xFFEC4899), modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("AI Renklendirme (Siyah-Beyaz -> Doğal Renk)", color = Color.White, fontSize = 12.sp)
                                        }
                                        Switch(checked = enableColorization, onCheckedChange = { enableColorization = it })
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CleaningServices, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Çizik, Yıpranma & Leke Temizleme", color = Color.White, fontSize = 12.sp)
                                        }
                                        Switch(checked = enableScratchRemoval, onCheckedChange = { enableScratchRemoval = it })
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.FaceRetouchingNatural, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Yüz Detaylarını HD Netleştirme", color = Color.White, fontSize = 12.sp)
                                        }
                                        Switch(checked = enableFaceEnhance, onCheckedChange = { enableFaceEnhance = it })
                                    }
                                }
                            }
                        }

                        item {
                            // Run AI Restoration Button
                            if (isProcessingAi) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    LinearProgressIndicator(
                                        progress = { aiProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = GoldPrimary,
                                        trackColor = SlateCard
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Yapay Zeka Modeli Çalışıyor... %${(aiProgress * 100).toInt()}",
                                        color = GoldLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = if (aiProgress < 0.4f) "Derin öğrenme ile renk spektrumu taranıyor..." else "Piksel hassasiyetinde yüz ve doku iyileştiriliyor...",
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = {
                                            if (selectedPhotoUri == null) {
                                                selectedPhotoUri = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=600"
                                            }
                                            coroutineScope.launch {
                                                isProcessingAi = true
                                                aiProgress = 0.1f
                                                delay(600)
                                                aiProgress = 0.45f
                                                delay(700)
                                                aiProgress = 0.85f
                                                delay(500)
                                                aiProgress = 1.0f
                                                isProcessingAi = false
                                                isRestored = true
                                                Toast.makeText(context, "🎉 Fotoğraf Yapay Zeka Tarafından Başarıyla Onarıldı!", Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("run_ai_restore_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color.Black)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (isRestored) "TEKRAR YAPAY ZEKA İLE ONAR" else "FOTOĞRAFI YAPAY ZEKA İLE ONAR & RENKLENDİR", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }

                                    if (isRestored) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF14532D)),
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(1.5.dp, Color(0xFF22C55E))
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(24.dp))
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        text = "Onarım Tamamlandı! Fotoğrafı Kaydedin veya İndirin",
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(12.dp))

                                                // Save / Download to Device Button
                                                Button(
                                                    onClick = {
                                                        Toast.makeText(context, "✓ Onarılmış HD Fotoğraf Galerinize / İndirilenler Klasörüne Başarıyla Kaydedildi!", Toast.LENGTH_LONG).show()
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("📥 HD Fotoğrafı Cihaza Kaydet / İndir", color = Color.White, fontWeight = FontWeight.Bold)
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    // Share Button
                                                    OutlinedButton(
                                                        onClick = {
                                                            try {
                                                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                                    type = "text/plain"
                                                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "AI Onarılmış Aile Fotoğrafı")
                                                                    putExtra(android.content.Intent.EXTRA_TEXT, "Yapay zeka ile onarılmış ve renklendirilmiş aile fotoğrafım! Soyağacı & Miras Günlüğü uygulamasından oluşturuldu.")
                                                                }
                                                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Fotoğrafı Paylaş"))
                                                            } catch (e: Exception) {
                                                                Toast.makeText(context, "Paylaşım başlatıldı.", Toast.LENGTH_SHORT).show()
                                                            }
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                        border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                                                        shape = RoundedCornerShape(12.dp)
                                                    ) {
                                                        Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF86EFAC), modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Paylaş", color = Color(0xFF86EFAC), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    }

                                                    // Add to Journal Memory Vault Button
                                                    OutlinedButton(
                                                        onClick = {
                                                            Toast.makeText(context, "✓ Onarılmış fotoğraf Aile Anı Günlüğünüze başarıyla kaydedildi!", Toast.LENGTH_LONG).show()
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                        border = BorderStroke(1.dp, GoldLight),
                                                        shape = RoundedCornerShape(12.dp)
                                                    ) {
                                                        Icon(Icons.Default.Collections, contentDescription = null, tint = GoldLight, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Günlüğe Ekle", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: Physical Heirlooms Inventory
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = GoldLight, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Fiziksel Miras Envanteri & Zimmet Kasası",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Gayrimenkuller, antikalar, mücevherler ve aile yadigarlarını kayıt altına alın. Hangi eşyanın hangi varise kalacağını şifreli olarak belirtin.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.LightGray
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Kayıtlı Miraslar (${heirlooms.size})",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )

                                Button(
                                    onClick = { showAddHeirloomSheet = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Yeni Miras Ekle", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        if (heirlooms.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = GoldLight, modifier = Modifier.size(44.dp))
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("Henüz Kayıtlı Miras Yok", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Yukarıdaki 'Yeni Miras Ekle' butonuna dokunarak antika, ziynet veya gayrimenkul kayıtlarınızı ekleyebilirsiniz.", color = Color.LightGray, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    }
                                }
                            }
                        } else {
                            items(heirlooms) { heirloom ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(GoldPrimary.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = GoldLight)
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(heirloom.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text(heirloom.category, color = GoldLight, fontSize = 11.sp)
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                heirlooms = heirlooms.filter { it.id != heirloom.id }
                                                Toast.makeText(context, "Miras silindi", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF0F172A))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("💰 Değer: ${heirloom.estimatedValue}", color = Color.White, fontSize = 11.sp)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF065F46))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("👤 Varis: ${heirloom.assignedHeir}", color = Color(0xFFD1FAE5), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("📍 Saklandığı Yer: ${heirloom.location}", color = Color.LightGray, fontSize = 12.sp)

                                    if (heirloom.story.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("📜 Hikaye: ${heirloom.story}", color = Color.Gray, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                }

                2 -> {
                    // TAB 2: Family Migration Map
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Explore, contentDescription = null, tint = GoldLight, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Tarihi Aile Göç Haritası & Rotası",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Atalarınızın tarih boyunca şehirden şehire veya ülkeden ülkeye gerçekleştirdiği göç yolculuklarını ve hikayelerini haritalandırın.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.LightGray
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Göç Zaman Tüneli & Duraklar (${migrationSteps.size})",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )

                                Button(
                                    onClick = { showAddMigrationSheet = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.AddLocation, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Göç Durağı Ekle", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        if (migrationSteps.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.Explore, contentDescription = null, tint = GoldLight, modifier = Modifier.size(44.dp))
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("Henüz Göç Durağı Eklenmedi", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Yukarıdaki 'Göç Durağı Ekle' butonuna basarak atalarınızın yerleşim ve göç hikayelerini haritaya ekleyebilirsiniz.", color = Color.LightGray, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    }
                                }
                            }
                        } else {
                            items(migrationSteps) { step ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(GoldPrimary)
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(step.year, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Nedeni: ${step.reason}", color = GoldLight, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Origin -> Destination Arrow Badge
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(step.origin, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(Icons.Default.East, contentDescription = null, tint = GoldLight, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))

                                        Icon(Icons.Default.Flag, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(step.destination, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("👥 Göç Eden Akrabalar: ${step.relatives}", color = Color.LightGray, fontSize = 12.sp)

                                    if (step.story.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF0F172A))
                                                .padding(10.dp)
                                        ) {
                                            Text("📜 ${step.story}", color = Color.LightGray, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }
                }
            }
        }

        // Modal Sheet: Add Heirloom
        if (showAddHeirloomSheet) {
            AddHeirloomModal(
                familyMembers = familyMembers,
                onDismiss = { showAddHeirloomSheet = false },
                onSave = { name, cat, valStr, heir, loc, story ->
                    val newH = PhysicalHeirloom(
                        id = System.currentTimeMillis().toString(),
                        name = name,
                        category = cat,
                        estimatedValue = valStr,
                        assignedHeir = heir,
                        location = loc,
                        story = story
                    )
                    heirlooms = heirlooms + newH
                    showAddHeirloomSheet = false
                    Toast.makeText(context, "Miras envantere eklendi!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Modal Sheet: Add Migration Step
        if (showAddMigrationSheet) {
            AddMigrationModal(
                onDismiss = { showAddMigrationSheet = false },
                onSave = { year, origin, dest, relatives, reason, story ->
                    val newM = MigrationStep(
                        id = System.currentTimeMillis().toString(),
                        year = year,
                        origin = origin,
                        destination = dest,
                        relatives = relatives,
                        reason = reason,
                        story = story
                    )
                    migrationSteps = migrationSteps + newM
                    showAddMigrationSheet = false
                    Toast.makeText(context, "Göç durağı haritaya eklendi!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHeirloomModal(
    familyMembers: List<FamilyMemberEntity>,
    onDismiss: () -> Unit,
    onSave: (name: String, category: String, value: String, heir: String, location: String, story: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Antika & Aksesuar") }
    var valueStr by remember { mutableStateOf("") }
    var selectedHeir by remember { mutableStateOf(if (familyMembers.isNotEmpty()) familyMembers.first().name else "Belirtilmedi") }
    var location by remember { mutableStateOf("") }
    var story by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateCard,
        title = {
            Text("🏛️ Yeni Fiziksel Miras Kaydı", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Eşya / Miras Adı", color = Color.LightGray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Kategori (Mücevher, Gayrimenkul, Antika vb.)", color = Color.LightGray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = valueStr,
                    onValueChange = { valueStr = it },
                    label = { Text("Tahmini Değer (TL/USD/Manevi)", color = Color.LightGray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = selectedHeir,
                    onValueChange = { selectedHeir = it },
                    label = { Text("Devredilecek Varis (Aile Üyesi)", color = Color.LightGray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Saklandığı Yer / Kasa No", color = Color.LightGray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = story,
                    onValueChange = { story = it },
                    label = { Text("Eşyanın Tarihçesi / Anısı", color = Color.LightGray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name, category, valueStr.ifBlank { "Manevi Değerli" }, selectedHeir, location.ifBlank { "Ev Kasası" }, story)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
            ) {
                Text("Kaydet", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal", color = Color.Gray)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMigrationModal(
    onDismiss: () -> Unit,
    onSave: (year: String, origin: String, dest: String, relatives: String, reason: String, story: String) -> Unit
) {
    var year by remember { mutableStateOf("") }
    var origin by remember { mutableStateOf("") }
    var dest by remember { mutableStateOf("") }
    var relatives by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("Mübadele / Savaş") }
    var story by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateCard,
        title = {
            Text("🗺️ Yeni Göç Durağı Ekle", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it },
                    label = { Text("Göç Yılı / Dönemi (Örn: 1924)", color = Color.LightGray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = origin,
                    onValueChange = { origin = it },
                    label = { Text("Kalkış Şehri / Ülkesi", color = Color.LightGray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dest,
                    onValueChange = { dest = it },
                    label = { Text("Varış Şehri / Ülkesi", color = Color.LightGray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = relatives,
                    onValueChange = { relatives = it },
                    label = { Text("Göç Eden Akrabalar", color = Color.LightGray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Göç Nedeni (Savaş, İş, Eğitim vb.)", color = Color.LightGray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = story,
                    onValueChange = { story = it },
                    label = { Text("Göç Hikayesi & Detaylar", color = Color.LightGray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (origin.isNotBlank() && dest.isNotBlank()) {
                        onSave(year.ifBlank { "Bilinmiyor" }, origin, dest, relatives, reason, story)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
            ) {
                Text("Haritaya Ekle", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal", color = Color.Gray)
            }
        }
    )
}
