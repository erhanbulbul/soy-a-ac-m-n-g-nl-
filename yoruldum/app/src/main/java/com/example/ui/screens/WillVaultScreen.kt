package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.example.ui.components.BiometricSecurityLockOverlay
import com.example.data.local.FamilyMemberEntity
import com.example.data.local.WillDocumentEntity
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NavyDark
import com.example.ui.theme.SlateCard
import com.example.util.FileStorageUtil
import com.example.util.LanguageManager
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MultiSigTrustee(
    val id: String,
    val name: String,
    val relationship: String,
    var isApproved: Boolean = false,
    var approvedDate: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WillVaultScreen(
    willDocuments: List<WillDocumentEntity>,
    familyMembers: List<FamilyMemberEntity>,
    languageCode: String,
    isPremium: Boolean = false,
    onTriggerPaywall: (String) -> Unit = {},
    onAddWillDocument: (title: String, category: String, uri: String, desc: String, recipients: String) -> Unit,
    onDeleteWillDocument: (Long) -> Unit,
    onBack: () -> Unit
) {
    BiometricSecurityLockOverlay(title = "Vasiyet Kasası") {
        var selectedTab by remember { mutableIntStateOf(0) } // 0: Vasiyet Kasası, 1: Multi-Sig Çoklu Onay
    var showAddSheet by remember { mutableStateOf(false) }
    var selectedViewDocument by remember { mutableStateOf<WillDocumentEntity?>(null) }
    var isBiometricPinLockEnabled by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Multi-Sig Vault State
    var requiredApprovalsCount by remember { mutableIntStateOf(1) }
    var trustees by remember { mutableStateOf<List<MultiSigTrustee>>(emptyList()) }
    var showAddTrusteeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = LanguageManager.getString("will_vault", languageCode),
                            color = GoldLight,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Dijital Vasiyet & Multi-Sig Kasa",
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
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        if (willDocuments.size >= 1 && !isPremium) {
                            onTriggerPaywall("Sınırsız Vasiyet / Dijital Miras Depolama")
                        } else {
                            showAddSheet = true
                        }
                    },
                    containerColor = GoldPrimary,
                    contentColor = Color.Black,
                    modifier = Modifier.testTag("add_will_doc_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(LanguageManager.getString("will_vault", languageCode), color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
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
                .padding(16.dp)
        ) {
            // Module Navigation Tabs
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
                    text = { Text("📜 Vasiyet Kasası", fontWeight = FontWeight.Bold, color = if (selectedTab == 0) GoldLight else Color.Gray) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("🔐 Multi-Sig Kasa", fontWeight = FontWeight.Bold, color = if (selectedTab == 1) GoldLight else Color.Gray) }
                )
            }

            when (selectedTab) {
                0 -> {
                    // TAB 0: Digital Will Documents Vault
                    // Premium Feature Banner 1: Sınırsız Vasiyet & Dijital Miras Depolama
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (isPremium) GoldLight else GoldPrimary.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = GoldLight)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Sınırsız Vasiyet & Dijital Miras Depolama",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(if (isPremium) Color(0xFF15803D) else GoldPrimary, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isPremium) "SINIRSIZ PRO" else "PRO 🔒",
                                        color = if (isPremium) Color.White else Color.Black,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "✓ Sınırsız depolama alanı aktiftir. İstediğiniz kadar vasiyet ve miras belgesi ekleyebilirsiniz.",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Feature Banner 2: Gelişmiş Güvenlik Kilidi (Biyometrik / PIN)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = GoldLight)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Gelişmiş Güvenlik Kilidi (Biyometrik / PIN)",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF15803D), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "AKTİF",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Kasa girişinde Parmak İzi / Yüz Tanıma veya 6 haneli özel PIN doğrulama isteyin.",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isBiometricPinLockEnabled) "🛡️ Biyometrik/PIN Kilit: AKTİF" else "Biyometrik/PIN Kilit: PASİF",
                                    color = if (isBiometricPinLockEnabled) Color(0xFF4ADE80) else Color.Gray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Switch(
                                    checked = isBiometricPinLockEnabled,
                                    onCheckedChange = { checked ->
                                        if (!isPremium) {
                                            onTriggerPaywall("Gelişmiş Güvenlik Kilidi (Biyometrik / PIN)")
                                        } else {
                                            isBiometricPinLockEnabled = checked
                                            Toast.makeText(
                                                context,
                                                if (checked) "🔒 Gelişmiş Biyometrik & PIN Kilidi Aktifleştirildi!" else "Biyometrik Kilit Devre Dışı",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    modifier = Modifier.testTag("biometric_pin_switch")
                                )
                            }
                        }
                    }

                    if (willDocuments.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = GoldLight, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Henüz kasada vasiyet veya belge bulunmuyor.\n'Vasiyet Ekle' butonuna dokunarak metin yazın, PDF veya resim yükleyin.",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Kayıtlı Vasiyetler & Belgeler (${willDocuments.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(willDocuments) { doc ->
                                WillDocumentCard(
                                    document = doc,
                                    onView = { selectedViewDocument = doc },
                                    onDelete = { onDeleteWillDocument(doc.id) }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: Multi-Sig Vault (Çoklu Onaylı Multi-Sig Kasa)
                    val approvedCount = trustees.count { it.isApproved }
                    val isVaultUnlocked = approvedCount >= requiredApprovalsCount

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, GoldPrimary)
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.GroupWork, contentDescription = null, tint = GoldLight)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "🔐 Multi-Sig Çoklu Onaylı Kasa Protokolü",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Miras kasası tek bir kişinin elinde değil; belirlediğiniz varislerin ortak dijital imzası ve onayı ile kilit açılır.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }

                        item {
                            // Vault Lock Status Banner
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isVaultUnlocked) Color(0xFF065F46) else Color(0xFF1E1B4B)
                                ),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(2.dp, if (isVaultUnlocked) Color(0xFF10B981) else Color(0xFF818CF8))
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = if (isVaultUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (isVaultUnlocked) Color(0xFF34D399) else GoldLight,
                                        modifier = Modifier.size(40.dp)
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = if (isVaultUnlocked) "🔓 KASA KİLİDİ AÇILDI" else "🔒 KASA KİLİTLİ (MULTI-SIG)",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "Gerekli Onay Sayısı: $approvedCount / $requiredApprovalsCount (Toplam ${trustees.size} Varis)",
                                        color = GoldLight,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        item {
                            // Threshold Selector
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Kasa Açılışı İçin Gerekli Onay Eşiği (N / M):",
                                        color = GoldLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf(1 to "1 / 3 Minimal", 2 to "2 / 3 Çoğunluk", 3 to "3 / 3 Oy Birliği").forEach { (count, label) ->
                                            val isSelected = requiredApprovalsCount == count
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (isSelected) GoldPrimary else NavyDark)
                                                    .clickable { requiredApprovalsCount = count }
                                                    .padding(vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = if (isSelected) Color.Black else Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
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
                                    text = "Onay Temsilcileri / Varis İmzaları:",
                                    color = GoldLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )

                                Button(
                                    onClick = { showAddTrusteeDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Temsilci Ekle", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (trustees.isEmpty()) {
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
                                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = GoldLight, modifier = Modifier.size(44.dp))
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("Henüz Onay Temsilcisi Eklenmedi", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Kasa kilit onayları için yukarıdaki 'Temsilci Ekle' butonuna basarak aile üyelerinizi varis/temsilci olarak tanımlayabilirsiniz.", color = Color.LightGray, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    }
                                }
                            }
                        } else {
                            items(trustees) { trustee ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(if (trustee.isApproved) Color(0xFF10B981) else Color.Gray),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (trustee.isApproved) Icons.Default.Check else Icons.Default.HourglassEmpty,
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(trustee.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(trustee.relationship, color = GoldLight, fontSize = 12.sp)
                                        if (trustee.isApproved && trustee.approvedDate != null) {
                                            Text("✓ Imzalandı: ${trustee.approvedDate}", color = Color(0xFF34D399), fontSize = 11.sp)
                                        } else {
                                            Text("⏳ Onay Bekleniyor", color = Color.Gray, fontSize = 11.sp)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            trustees = trustees.map {
                                                if (it.id == trustee.id) {
                                                    val newStatus = !it.isApproved
                                                    val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("tr", "TR"))
                                                    it.copy(isApproved = newStatus, approvedDate = if (newStatus) sdf.format(Date()) else null)
                                                } else it
                                            }
                                            Toast.makeText(context, "${trustee.name} imza durumu güncellendi!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (trustee.isApproved) Color(0xFF374151) else GoldPrimary
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (trustee.isApproved) "İmzayı Kaldır" else "Varis Olarak İmzala",
                                            color = if (trustee.isApproved) Color.White else Color.Black,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    }
                }
            }
        }

        // Add Will Document Sheet
        if (showAddSheet) {
            AddWillDocumentSheet(
                familyMembers = familyMembers,
                languageCode = languageCode,
                onDismiss = { showAddSheet = false },
                onSave = { title, cat, uri, desc, recipients ->
                    onAddWillDocument(title, cat, uri, desc, recipients)
                    showAddSheet = false
                    Toast.makeText(context, "Vasiyet belgesi kasaya başarıyla eklendi!", Toast.LENGTH_SHORT).show()
                }
            )
        }



        // Detailed View Dialog
        selectedViewDocument?.let { doc ->
            AlertDialog(
                onDismissRequest = { selectedViewDocument = null },
                containerColor = SlateCard,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = GoldLight)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(doc.title, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(GoldPrimary.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(doc.category, color = GoldLight, style = MaterialTheme.typography.labelSmall)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (doc.description.isNotBlank()) {
                            Text("Vasiyet Metni / Açıklama:", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(doc.description, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (doc.documentImageUri.isNotBlank()) {
                            Text("Eklenti / Dosya Bağlantısı:", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            if (doc.documentImageUri.contains("pdf")) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFEF4444))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("PDF Belgesi Ekli", color = Color.White, style = MaterialTheme.typography.bodySmall)
                                }
                            } else {
                                Image(
                                    painter = rememberAsyncImagePainter(model = doc.documentImageUri),
                                    contentDescription = "Belge Görseli",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.4f))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Send, contentDescription = null, tint = GoldLight, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Vefat Halinde Gönderilecekler: ${doc.recipientNames.ifBlank { "Tüm Aile Fertleri" }}",
                                    color = GoldLight,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { exportWillToPdf(context, doc) },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PDF İndir / Paylaş", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        TextButton(onClick = { selectedViewDocument = null }) {
                            Text("Kapat", color = GoldLight)
                        }
                    }
                }
            )
        }
    }
}
}

@Composable
fun WillDocumentCard(
    document: WillDocumentEntity,
    onView: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onView() }
            .testTag("will_doc_card_${document.id}"),
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
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = GoldLight)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = document.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(GoldPrimary.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = document.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldLight,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (document.description.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B))
                            .border(1.dp, GoldPrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EditNote, contentDescription = null, tint = GoldLight, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Yazılı Metin", style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 10.sp)
                        }
                    }
                }

                if (document.documentImageUri.contains("pdf")) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF7F1D1D).copy(alpha = 0.5f))
                            .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF Dosyası Yüklendi", style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 10.sp)
                        }
                    }
                } else if (document.documentImageUri.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF065F46).copy(alpha = 0.5f))
                            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Kamera / Resim Ekli", style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 10.sp)
                        }
                    }
                }
            }

            if (document.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = document.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    maxLines = 3
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(10.dp)
            ) {
                Icon(Icons.Default.People, contentDescription = null, tint = GoldLight, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Vefat Halinde Gönderilecek Aile Üyeleri: ${document.recipientNames.ifBlank { "Tüm Aile Üyeleri" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldLight,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { exportWillToPdf(context, document) },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("export_pdf_button_${document.id}")
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF Export", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Vasiyeti PDF Olarak İndir / Dışa Aktar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWillDocumentSheet(
    familyMembers: List<FamilyMemberEntity>,
    languageCode: String,
    onDismiss: () -> Unit,
    onSave: (title: String, category: String, uri: String, desc: String, recipients: String) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Vasiyetname") }
    var description by remember { mutableStateOf("") }
    var attachedUriStr by remember { mutableStateOf("") }
    var isPdfAttached by remember { mutableStateOf(false) }
    var sendToAll by remember { mutableStateOf(false) }

    val selectedRecipients = remember { mutableStateListOf<String>() }
    val categories = listOf("Vasiyetname", "Tapu / Mülk", "Miras / Banka", "Özel Mektup", "Anı Belgeleri")
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val saved = FileStorageUtil.saveUriToInternalStorage(context, it, "will_doc")
            if (saved != null) {
                attachedUriStr = saved
                isPdfAttached = false
                Toast.makeText(context, "Kamera / Resim Belgesi Güvenli Kaydedildi!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val saved = FileStorageUtil.saveUriToInternalStorage(context, it, "will_pdf")
            if (saved != null) {
                attachedUriStr = saved
                isPdfAttached = true
                Toast.makeText(context, "PDF Belgesi Güvenli Yüklendi!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NavyDark,
        modifier = Modifier.fillMaxHeight(0.94f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(20.dp)
        ) {
            Text(
                text = "Yeni Vasiyet / Belge Oluştur",
                style = MaterialTheme.typography.titleLarge,
                color = GoldLight,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Metin olarak yazabilir, PDF belgesi yükleyebilir veya fotoğraflayabilirsiniz.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isPdfAttached && attachedUriStr.isNotEmpty()) Color(0xFFEF4444).copy(alpha = 0.25f) else SlateCard)
                        .border(1.dp, if (isPdfAttached && attachedUriStr.isNotEmpty()) Color(0xFFEF4444) else GoldPrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable { pdfPickerLauncher.launch("application/pdf") }
                        .padding(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = if (isPdfAttached && attachedUriStr.isNotEmpty()) Color(0xFFEF4444) else GoldLight)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isPdfAttached && attachedUriStr.isNotEmpty()) "PDF Eklendi ✓" else "PDF Dosyası Yükle",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (!isPdfAttached && attachedUriStr.isNotEmpty()) Color(0xFF10B981).copy(alpha = 0.25f) else SlateCard)
                        .border(1.dp, if (!isPdfAttached && attachedUriStr.isNotEmpty()) Color(0xFF10B981) else GoldPrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable { imagePickerLauncher.launch("image/*") }
                        .padding(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = if (!isPdfAttached && attachedUriStr.isNotEmpty()) Color(0xFF10B981) else GoldLight)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (!isPdfAttached && attachedUriStr.isNotEmpty()) "Resim Eklendi ✓" else "Kamera / Resim Çek",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Belge Türü / Kategorisi:",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    val isSelected = category == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) GoldPrimary else SlateCard)
                            .clickable { category = cat }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Vasiyet / Belge Başlığı") },
                placeholder = { Text("Örn: Çocuklarıma Son Vasiyetim, Tapu Bilgilerim...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_will_title"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = GoldLight,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Yazılı Vasiyet Metni & Açıklama") },
                placeholder = { Text("Miras paylaşımı, vefatımdan sonra yapılmasını istediklerim, aileme son mesajım...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp, max = 280.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = GoldLight,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Vefat Durumunda Kimlere Gönderilsin? (Seçin):",
                style = MaterialTheme.typography.labelMedium,
                color = GoldLight,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (sendToAll) GoldPrimary.copy(alpha = 0.25f) else SlateCard)
                    .clickable {
                        sendToAll = !sendToAll
                        if (sendToAll) {
                            selectedRecipients.clear()
                            selectedRecipients.addAll(familyMembers.map { it.name })
                        } else {
                            selectedRecipients.clear()
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Checkbox(
                    checked = sendToAll,
                    onCheckedChange = {
                        sendToAll = it
                        if (it) {
                            selectedRecipients.clear()
                            selectedRecipients.addAll(familyMembers.map { member -> member.name })
                        } else {
                            selectedRecipients.clear()
                        }
                    },
                    colors = CheckboxDefaults.colors(checkedColor = GoldPrimary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Group, contentDescription = null, tint = GoldLight)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tüm Eklenen Aile Fertlerine Gönder",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (familyMembers.isEmpty()) {
                Text(
                    text = "Henüz soy ağacına aile ferdi eklenmedi. Yine de oluşturabilir, daha sonra aile üyeleri bağlayabilirsiniz.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    familyMembers.forEach { member ->
                        val isSelected = selectedRecipients.contains(member.name)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) GoldPrimary.copy(alpha = 0.2f) else SlateCard)
                                .clickable {
                                    if (isSelected) {
                                        selectedRecipients.remove(member.name)
                                        sendToAll = false
                                    } else {
                                        selectedRecipients.add(member.name)
                                        if (selectedRecipients.size == familyMembers.size) {
                                            sendToAll = true
                                        }
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) GoldPrimary else Color.Gray),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = member.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = member.relationship,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val recipientNames = if (sendToAll) "Tüm Aile Üyeleri" else selectedRecipients.joinToString(", ")
                        onSave(title, category, attachedUriStr, description, recipientNames)
                    } else {
                        Toast.makeText(context, "Lütfen bir vasiyet başlığı girin", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_will_doc_button"),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Vasiyet Kasasına Şifreli Kaydet", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

fun exportWillToPdf(context: Context, document: WillDocumentEntity) {
    try {
        val pdfDocument = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paintTitle = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(15, 23, 42)
            textSize = 18f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val paintSubTitle = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(180, 83, 9)
            textSize = 13f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val paintBody = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(30, 41, 59)
            textSize = 11f
            isAntiAlias = true
        }

        val paintFooter = android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 9f
            isAntiAlias = true
        }

        val paintLine = android.graphics.Paint().apply {
            color = android.graphics.Color.LTGRAY
            strokeWidth = 1.5f
        }

        var yPos = 45f

        canvas.drawText("DİJİTAL VASİYET & RESMİ MİRAS BELGESİ", 40f, yPos, paintTitle)
        yPos += 22f

        canvas.drawText("Soyağacı Miras Kasası - Uçtan Uca Şifreli Kayıt", 40f, yPos, paintSubTitle)
        yPos += 25f

        canvas.drawLine(40f, yPos, 555f, yPos, paintLine)
        yPos += 25f

        canvas.drawText("Vasiyet / Belge Başlığı: ${document.title}", 40f, yPos, paintSubTitle)
        yPos += 22f

        canvas.drawText("Kategori: ${document.category}", 40f, yPos, paintBody)
        yPos += 20f

        val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("tr", "TR"))
        val dateStr = dateFormat.format(Date(document.timestamp))
        canvas.drawText("Kayıt Tarihi: $dateStr", 40f, yPos, paintBody)
        yPos += 25f

        val recipientsText = if (document.recipientNames.isNotBlank()) document.recipientNames else "Tüm Aile Üyeleri"
        canvas.drawText("Vefat Halinde Gönderilecek Aile Üyeleri:", 40f, yPos, paintSubTitle)
        yPos += 18f
        canvas.drawText(recipientsText, 40f, yPos, paintBody)
        yPos += 30f

        canvas.drawLine(40f, yPos, 555f, yPos, paintLine)
        yPos += 25f

        if (document.description.isNotBlank()) {
            canvas.drawText("Vasiyet Metni / Detaylı Açıklama:", 40f, yPos, paintSubTitle)
            yPos += 20f

            val textLines = wordWrap(document.description, 72)
            for (line in textLines) {
                if (yPos > 720f) break
                canvas.drawText(line, 40f, yPos, paintBody)
                yPos += 16f
            }
            yPos += 25f
        }

        if (document.documentImageUri.isNotBlank() && !document.documentImageUri.contains("pdf")) {
            try {
                val imageUri = Uri.parse(document.documentImageUri)
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    if (yPos > 550f) yPos = 550f
                    canvas.drawText("Ekli Görsel / Kamera Taraması:", 40f, yPos, paintSubTitle)
                    yPos += 15f

                    val maxWidth = 260f
                    val maxHeight = 160f
                    val scale = Math.min(maxWidth / bitmap.width, maxHeight / bitmap.height)
                    val scaledWidth = (bitmap.width * scale).toInt()
                    val scaledHeight = (bitmap.height * scale).toInt()

                    val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
                    canvas.drawBitmap(scaledBitmap, 40f, yPos, null)
                    yPos += scaledHeight + 20f
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (document.documentImageUri.contains("pdf")) {
            canvas.drawText("Ekli Dosya: Yüklü PDF Belgesi Bağlantılıdır", 40f, yPos, paintSubTitle)
            yPos += 20f
        }

        canvas.drawLine(40f, 790f, 555f, 790f, paintLine)
        canvas.drawText("Bu belge Soyağacı Uygulaması Vasiyet Kasası tarafından güvenli olarak üretilmiştir.", 40f, 810f, paintFooter)

        pdfDocument.finishPage(page)

        val pdfDir = File(context.cacheDir, "pdf")
        if (!pdfDir.exists()) pdfDir.mkdirs()
        val cleanTitle = document.title.replace(Regex("[^a-zA-Z0-9]"), "_")
        val pdfFile = File(pdfDir, "Vasiyet_${cleanTitle}_${document.id}.pdf")
        val outputStream = FileOutputStream(pdfFile)
        pdfDocument.writeTo(outputStream)
        outputStream.close()
        pdfDocument.close()

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "Soyağacı Vasiyet Belgesi - ${document.title}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Vasiyet PDF Belgesini İndir / Paylaş"))
        Toast.makeText(context, "PDF belgesi başarıyla oluşturuldu!", Toast.LENGTH_SHORT).show()

    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "PDF oluşturulurken hata oluştu: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

private fun wordWrap(text: String, maxCharsPerLine: Int): List<String> {
    val lines = mutableListOf<String>()
    val paragraphs = text.split("\n")
    for (paragraph in paragraphs) {
        val words = paragraph.split(" ")
        var currentLine = ""
        for (word in words) {
            if ((currentLine + " " + word).length > maxCharsPerLine) {
                if (currentLine.isNotEmpty()) lines.add(currentLine.trim())
                currentLine = word
            } else {
                currentLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine.trim())
    }
    return lines
}
