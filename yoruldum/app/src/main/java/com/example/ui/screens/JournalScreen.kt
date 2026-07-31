package com.example.ui.screens

import android.content.Context
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.data.local.JournalEntryEntity
import com.example.ui.components.BiometricSecurityLockOverlay
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NavyDark
import com.example.ui.theme.SlateCard
import com.example.util.FileStorageUtil
import com.example.util.LanguageManager
import com.example.util.PdfExporter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data Models for Family Interactions
data class FamilyCalendarEvent(
    val id: String,
    val title: String,
    val dateString: String,
    val timeString: String,
    val category: String, // Doğum Günü, Yıldönümü, Buluşma, Görev
    val assignedTo: String,
    var isCompleted: Boolean = false,
    val reminderEnabled: Boolean = true
)

data class TimeCapsuleMemory(
    val id: String,
    val title: String,
    val createdDate: String,
    val unlockDate: String,
    val recipient: String,
    val messageContent: String,
    val isLocked: Boolean,
    val mediaType: String // Fotoğraf, Mektup, Ses Kaydı
)

data class VoiceMemoryItem(
    val id: String,
    val title: String,
    val narratorName: String,
    val relationship: String,
    val durationText: String,
    val recordedDate: String,
    val storyTopic: String,
    var isPlaying: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    journalEntries: List<JournalEntryEntity>,
    languageCode: String,
    userName: String,
    userCode: String,
    onAddEntry: (title: String, content: String, colorHex: String, imageUris: String) -> Unit,
    onUpdateEntry: (id: Long, title: String, content: String, colorHex: String, imageUris: String) -> Unit,
    onDeleteEntry: (Long) -> Unit,
    onBack: () -> Unit
) {
    BiometricSecurityLockOverlay(title = "Günlük & Anılar") {
        var selectedTab by remember { mutableIntStateOf(0) } // 0: Anılar & Günlük, 1: Ortak Takvim & Görevler, 2: Zaman Kapsülü, 3: Sesli Hafıza
    var showCreateSheet by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<JournalEntryEntity?>(null) }
    var previewImageUri by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // Family Calendar State
    var calendarEvents by remember { mutableStateOf<List<FamilyCalendarEvent>>(emptyList()) }
    var showAddEventDialog by remember { mutableStateOf(false) }

    // Time Capsule State
    var timeCapsules by remember { mutableStateOf<List<TimeCapsuleMemory>>(emptyList()) }
    var showAddTimeCapsuleDialog by remember { mutableStateOf(false) }

    // Voice Memory State
    var voiceMemories by remember { mutableStateOf<List<VoiceMemoryItem>>(emptyList()) }
    var showRecordVoiceDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = LanguageManager.getString("journal", languageCode),
                            color = GoldLight,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Anılar, Takvim, Zaman Kapsülü & Sesli Hafıza",
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
                actions = {
                    if (selectedTab == 0) {
                        Button(
                            onClick = {
                                val pdf = PdfExporter.generateAndShareJournalPdf(context, userName, userCode, journalEntries)
                                if (pdf != null) {
                                    PdfExporter.sharePdf(context, pdf)
                                } else {
                                    Toast.makeText(context, "PDF Error", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("export_pdf_button")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(LanguageManager.getString("export_pdf", languageCode), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyDark)
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        editingEntry = null
                        showCreateSheet = true
                    },
                    containerColor = GoldPrimary,
                    contentColor = Color.Black,
                    modifier = Modifier.testTag("add_journal_entry_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Entry")
                }
            } else if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { showAddEventDialog = true },
                    containerColor = GoldPrimary,
                    contentColor = Color.Black
                ) {
                    Icon(Icons.Default.Event, contentDescription = "New Event")
                }
            } else if (selectedTab == 2) {
                FloatingActionButton(
                    onClick = { showAddTimeCapsuleDialog = true },
                    containerColor = GoldPrimary,
                    contentColor = Color.Black
                ) {
                    Icon(Icons.Default.LockClock, contentDescription = "New Capsule")
                }
            } else if (selectedTab == 3) {
                FloatingActionButton(
                    onClick = { showRecordVoiceDialog = true },
                    containerColor = Color(0xFFDC2626),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Record Voice")
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
                    text = { Text("📖 Anı Günlüğü", fontWeight = FontWeight.Bold, color = if (selectedTab == 0) GoldLight else Color.Gray) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("📅 Aile Takvimi & Görev", fontWeight = FontWeight.Bold, color = if (selectedTab == 1) GoldLight else Color.Gray) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("⏳ Zaman Kapsülü", fontWeight = FontWeight.Bold, color = if (selectedTab == 2) GoldLight else Color.Gray) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("🎙️ Sesli Hafıza", fontWeight = FontWeight.Bold, color = if (selectedTab == 3) GoldLight else Color.Gray) }
                )
            }

            when (selectedTab) {
                0 -> {
                    // TAB 0: Journal & Memories
                    if (journalEntries.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = LanguageManager.getString("no_journal_entries", languageCode),
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(journalEntries) { entry ->
                                JournalEntryCard(
                                    entry = entry,
                                    onEdit = {
                                        editingEntry = entry
                                        showCreateSheet = true
                                    },
                                    onDelete = { onDeleteEntry(entry.id) },
                                    onImageClick = { previewImageUri = it }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: Family Calendar & Shared Tasks
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, GoldPrimary)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = GoldLight)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Ortak Aile Takvimi & Özel Gün Hatırlatıcı",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Doğum günleri, evlilik yıldönümleri, aile buluşmaları ve ortak miras görevlerini takvim üzerinden kolayca takip edin.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }

                        if (calendarEvents.isEmpty()) {
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
                                        Icon(Icons.Default.Event, contentDescription = null, tint = GoldLight, modifier = Modifier.size(44.dp))
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("Henüz Aile Etkinliği Yok", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Sağ alttaki '+' butonuna dokunarak yeni etkinlik veya özel gün ekleyebilirsiniz.", color = Color.LightGray, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    }
                                }
                            }
                        } else {
                            items(calendarEvents) { event ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (event.isCompleted) Color(0xFF1E293B) else SlateCard
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(
                                                    checked = event.isCompleted,
                                                    onCheckedChange = { checked ->
                                                        calendarEvents = calendarEvents.map {
                                                            if (it.id == event.id) it.copy(isCompleted = checked) else it
                                                        }
                                                    },
                                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF22C55E))
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = event.title,
                                                    color = if (event.isCompleted) Color.Gray else Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .background(GoldPrimary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(event.category, color = GoldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = GoldLight, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("${event.dateString} - ${event.timeString}", color = Color.LightGray, fontSize = 12.sp)
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Sorumlu: ${event.assignedTo}", color = Color.Gray, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: Time Locked Memories (Time Capsules)
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, GoldPrimary)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LockClock, contentDescription = null, tint = GoldLight)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "⏳ Kapsül (Zaman Kilidi) Anılar",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Çocuklarınızın 18. yaş günü, evlilik yıldönümleriniz veya gelecekteki aile buluşmaları için zaman kilitli dijital mektup ve anılar bırakın.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }

                        if (timeCapsules.isEmpty()) {
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
                                        Icon(Icons.Default.LockClock, contentDescription = null, tint = GoldLight, modifier = Modifier.size(44.dp))
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("Henüz Kilitli Zaman Kapsülü Yok", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Sağ alttaki '+' butonuna dokunarak geleceğe mektup veya anı bırakabilirsiniz.", color = Color.LightGray, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    }
                                }
                            }
                        } else {
                            items(timeCapsules) { capsule ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (capsule.isLocked) Color(0xFF1E1B4B) else Color(0xFF14532D)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, if (capsule.isLocked) Color(0xFF818CF8) else Color(0xFF22C55E))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (capsule.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                                    contentDescription = null,
                                                    tint = if (capsule.isLocked) GoldLight else Color(0xFF4ADE80)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = capsule.title,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .background(if (capsule.isLocked) Color.Red.copy(alpha = 0.3f) else Color.Green.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = if (capsule.isLocked) "🔒 KİLİTLİ" else "🔓 KİLİT AÇILDI",
                                                    color = if (capsule.isLocked) Color(0xFFFCA5A5) else Color(0xFF86EFAC),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Alıcı: ${capsule.recipient}", color = GoldLight, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            Text("Tür: ${capsule.mediaType}", color = Color.LightGray, fontSize = 12.sp)
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        if (capsule.isLocked) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(NavyDark, RoundedCornerShape(10.dp))
                                                    .padding(12.dp)
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "Açılış Tarihi: ${capsule.unlockDate}",
                                                        color = GoldPrimary,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "Bu zaman kapsülündeki içerikler kilit açılma tarihine kadar gizli kalacaktır.",
                                                        color = Color.Gray,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        } else {
                                            Text(
                                                text = capsule.messageContent,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(NavyDark, RoundedCornerShape(10.dp))
                                                    .padding(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // TAB 3: Voice Memory Corner
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color(0xFFEF4444))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFFEF4444))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "🎙️ \"Sesli Hafıza\" Köşesi",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Aile büyüklerimizin kendi seslerinden canlı anıları, öğütleri ve ses biyografileri gelecek nesillere miras kalsın.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }

                        if (voiceMemories.isEmpty()) {
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
                                        Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(44.dp))
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("Henüz Sesli Anı Kaydı Yok", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Sağ alttaki kırmızı mikrofon butonuna dokunarak ilk sesli anınızı kaydedebilirsiniz.", color = Color.LightGray, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    }
                                }
                            }
                        } else {
                            items(voiceMemories) { voice ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFDC2626)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color.White)
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(voice.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                Text("${voice.narratorName} (${voice.relationship})", color = GoldLight, fontSize = 12.sp)
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                voiceMemories = voiceMemories.map {
                                                    if (it.id == voice.id) it.copy(isPlaying = !it.isPlaying) else it.copy(isPlaying = false)
                                                }
                                                Toast.makeText(
                                                    context,
                                                    if (!voice.isPlaying) "🔊 ${voice.narratorName} ses kaydı çalınıyor..." else "Ses durduruldu",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (voice.isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                                contentDescription = "Play/Pause",
                                                tint = GoldPrimary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = "Konu: ${voice.storyTopic}",
                                        color = Color.LightGray,
                                        fontSize = 12.sp
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(NavyDark, RoundedCornerShape(8.dp))
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Süre: ${voice.durationText}", color = Color.Gray, fontSize = 11.sp)
                                        Text("Kayıt Tarihi: ${voice.recordedDate}", color = Color.Gray, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                    }
                }
            }
        }

        // Add Journal Entry Sheet
        if (showCreateSheet || editingEntry != null) {
            CreateOrEditJournalSheet(
                initialEntry = editingEntry,
                languageCode = languageCode,
                onDismiss = {
                    showCreateSheet = false
                    editingEntry = null
                },
                onSave = { title, content, colorHex, imageUris ->
                    if (editingEntry != null) {
                        onUpdateEntry(editingEntry!!.id, title, content, colorHex, imageUris)
                        Toast.makeText(context, "Günlük anısı güncellendi!", Toast.LENGTH_SHORT).show()
                    } else {
                        onAddEntry(title, content, colorHex, imageUris)
                        Toast.makeText(context, "Günlük anısı kaydedildi!", Toast.LENGTH_SHORT).show()
                    }
                    showCreateSheet = false
                    editingEntry = null
                }
            )
        }

        // Add Family Event Dialog
        if (showAddEventDialog) {
            var eventTitle by remember { mutableStateOf("") }
            var eventDate by remember { mutableStateOf("20 Ağustos 2026") }
            var eventCategory by remember { mutableStateOf("🎂 Doğum Günü") }
            var assignedPerson by remember { mutableStateOf("Tüm Aile") }

            AlertDialog(
                onDismissRequest = { showAddEventDialog = false },
                containerColor = NavyDark,
                title = { Text("Yeni Aile Etkinliği / Görevi Ekle", color = GoldLight) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = eventTitle,
                            onValueChange = { eventTitle = it },
                            label = { Text("Etkinlik Başlığı") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = eventDate,
                            onValueChange = { eventDate = it },
                            label = { Text("Tarih ve Saat") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = eventCategory,
                            onValueChange = { eventCategory = it },
                            label = { Text("Kategori (Doğum Günü, Yıldönümü, Buluşma)") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (eventTitle.isNotBlank()) {
                                calendarEvents = calendarEvents + FamilyCalendarEvent(
                                    id = System.currentTimeMillis().toString(),
                                    title = eventTitle,
                                    dateString = eventDate,
                                    timeString = "18:00",
                                    category = eventCategory,
                                    assignedTo = assignedPerson
                                )
                                showAddEventDialog = false
                                Toast.makeText(context, "Etkinlik aile takvimine eklendi!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                    ) {
                        Text("Kaydet", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddEventDialog = false }) {
                        Text("İptal", color = Color.Gray)
                    }
                }
            )
        }

        // Add Time Capsule Dialog
        if (showAddTimeCapsuleDialog) {
            var capsuleTitle by remember { mutableStateOf("") }
            var unlockDateStr by remember { mutableStateOf("15 Mayıs 2030") }
            var recipientStr by remember { mutableStateOf("Çocuklarım") }
            var messageContentStr by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showAddTimeCapsuleDialog = false },
                containerColor = NavyDark,
                title = { Text("Yeni Zaman Kapsülü Oluştur", color = GoldLight) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = capsuleTitle,
                            onValueChange = { capsuleTitle = it },
                            label = { Text("Kapsül Başlığı") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = unlockDateStr,
                            onValueChange = { unlockDateStr = it },
                            label = { Text("Kilit Açılma Tarihi (Gelecek)") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = recipientStr,
                            onValueChange = { recipientStr = it },
                            label = { Text("Hedef Alıcı") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = messageContentStr,
                            onValueChange = { messageContentStr = it },
                            label = { Text("Gizli Mektup / Mesaj") },
                            modifier = Modifier.height(100.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (capsuleTitle.isNotBlank()) {
                                timeCapsules = timeCapsules + TimeCapsuleMemory(
                                    id = System.currentTimeMillis().toString(),
                                    title = capsuleTitle,
                                    createdDate = "Bugün",
                                    unlockDate = unlockDateStr,
                                    recipient = recipientStr,
                                    messageContent = messageContentStr,
                                    isLocked = true,
                                    mediaType = "✉️ Mektup"
                                )
                                showAddTimeCapsuleDialog = false
                                Toast.makeText(context, "🔒 Zaman kapsülü kilitlendi ve kaydedildi!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                    ) {
                        Text("Kapsülü Kilitle", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddTimeCapsuleDialog = false }) {
                        Text("İptal", color = Color.Gray)
                    }
                }
            )
        }

        // Record Voice Memory Dialog
        if (showRecordVoiceDialog) {
            var voiceTitle by remember { mutableStateOf("") }
            var narratorName by remember { mutableStateOf("Büyük Baba") }
            var storyTopic by remember { mutableStateOf("Gençlik Hatıraları") }
            var isRecordingActive by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showRecordVoiceDialog = false },
                containerColor = NavyDark,
                title = { Text("Sesli Hafıza Kaydet", color = GoldLight) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = voiceTitle,
                            onValueChange = { voiceTitle = it },
                            label = { Text("Anı Başlığı") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = narratorName,
                            onValueChange = { narratorName = it },
                            label = { Text("Anlatan Aile Büyüğü") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = storyTopic,
                            onValueChange = { storyTopic = it },
                            label = { Text("Anı Konusu / Özet") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                isRecordingActive = !isRecordingActive
                                Toast.makeText(
                                    context,
                                    if (isRecordingActive) "🎙️ Mikrofon aktif — Aile büyüğünün sesli anısı kaydediliyor..." else "Kayıt tamamlandı!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isRecordingActive) Color.Red else Color(0xFFDC2626))
                        ) {
                            Icon(if (isRecordingActive) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isRecordingActive) "Kaydı Durdur" else "Mikrofonla Kayda Başla", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (voiceTitle.isNotBlank()) {
                                voiceMemories = voiceMemories + VoiceMemoryItem(
                                    id = System.currentTimeMillis().toString(),
                                    title = voiceTitle,
                                    narratorName = narratorName,
                                    relationship = "Aile Büyüğü",
                                    durationText = "01:45",
                                    recordedDate = "Bugün",
                                    storyTopic = storyTopic
                                )
                                showRecordVoiceDialog = false
                                Toast.makeText(context, "🎙️ Sesli hafıza profiline kaydedildi!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                    ) {
                        Text("Kütüphaneye Ekle", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRecordVoiceDialog = false }) {
                        Text("İptal", color = Color.Gray)
                    }
                }
            )
        }

        // Full Image Preview Dialog
        if (previewImageUri != null) {
            AlertDialog(
                onDismissRequest = { previewImageUri = null },
                containerColor = NavyDark,
                title = { Text("Fotoğraf Önizleme", color = GoldLight) },
                text = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Image(
                            painter = rememberAsyncImagePainter(previewImageUri),
                            contentDescription = "Full Preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 350.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { previewImageUri = null }) {
                        Text("Kapat", color = GoldLight, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}
}

@Composable
fun JournalEntryCard(
    entry: JournalEntryEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onImageClick: (String) -> Unit
) {
    val parseColor = try {
        Color(android.graphics.Color.parseColor(entry.textColorHex))
    } catch (e: Exception) {
        GoldLight
    }

    val imageList = remember(entry.imageUris) {
        if (entry.imageUris.isBlank()) emptyList()
        else entry.imageUris.split(",").filter { it.isNotBlank() }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("journal_card_${entry.id}"),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Info Bar (Date, Time, Location, Edit & Delete)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = GoldLight, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${entry.dateString} ${entry.timeString}",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldLight
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = GoldLight, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = entry.deviceLocationInfo,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Entry Title with Dynamic Color
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium,
                color = parseColor,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Entry Content with Dynamic Color
            Text(
                text = entry.content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.95f)
            )

            // Attached Images Thumbnails
            if (imageList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Eklenen Fotoğraflar (${imageList.size}/5):",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldLight,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(imageList) { uriStr ->
                        Image(
                            painter = rememberAsyncImagePainter(uriStr),
                            contentDescription = "Journal Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(70.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .clickable { onImageClick(uriStr) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrEditJournalSheet(
    initialEntry: JournalEntryEntity? = null,
    languageCode: String,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, colorHex: String, imageUris: String) -> Unit
) {
    var title by remember { mutableStateOf(initialEntry?.title ?: "") }
    var content by remember { mutableStateOf(initialEntry?.content ?: "") }
    var selectedColorHex by remember { mutableStateOf(initialEntry?.textColorHex ?: "#FFB800") }
    var isRecording by remember { mutableStateOf(false) }

    var imageList by remember {
        mutableStateOf(
            if (initialEntry?.imageUris.isNullOrBlank()) emptyList<String>()
            else initialEntry!!.imageUris.split(",").filter { it.isNotBlank() }
        )
    }

    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val currentCount = imageList.size
            val remainingSlots = 5 - currentCount
            if (remainingSlots <= 0) {
                Toast.makeText(context, "En fazla 5 adet fotoğraf ekleyebilirsiniz.", Toast.LENGTH_SHORT).show()
            } else {
                val newUris = uris.take(remainingSlots).mapNotNull { uri ->
                    FileStorageUtil.saveUriToInternalStorage(context, uri, "journal_photo")
                }
                imageList = imageList + newUris
                Toast.makeText(context, "${newUris.size} adet fotoğraf güvenli kaydedildi.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val colorOptions = listOf(
        "#FFB800" to "Altın",
        "#EF4444" to "Mercan",
        "#10B981" to "Zümrüt",
        "#3B82F6" to "Safir",
        "#8B5CF6" to "Menekşe",
        "#F59E0B" to "Kehribar",
        "#E2E8F0" to "Gümüş"
    )

    val dateFormat = SimpleDateFormat("dd MMMM yyyy, EEEE • HH:mm", Locale("tr", "TR"))
    val currentHeader = dateFormat.format(Date())

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val activeTextColor = try {
        Color(android.graphics.Color.parseColor(selectedColorHex))
    } catch (e: Exception) {
        GoldLight
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NavyDark,
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = if (initialEntry != null) "Günlük Anısını Düzenle" else LanguageManager.getString("write_entry", languageCode),
                style = MaterialTheme.typography.titleLarge,
                color = GoldLight,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Tarih & Saat: $currentHeader",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Dynamic Font Color Selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = LanguageManager.getString("font_color", languageCode) + ":",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = colorOptions.find { it.first == selectedColorHex }?.second ?: "Seçildi",
                    style = MaterialTheme.typography.labelSmall,
                    color = activeTextColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(colorOptions) { (hex, _) ->
                    val isSelected = selectedColorHex == hex
                    val colorVal = Color(android.graphics.Color.parseColor(hex))
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(colorVal)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedColorHex = hex }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Photo Attachment Section (Max 5 photos)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SlateCard)
                    .border(1.dp, GoldPrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Add Photos", tint = GoldLight)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Anı Fotoğrafları (${imageList.size}/5)",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (imageList.size < 5) {
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Fotoğraf Ekle", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (imageList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(imageList) { uriStr ->
                            Box(modifier = Modifier.size(76.dp)) {
                                Image(
                                    painter = rememberAsyncImagePainter(uriStr),
                                    contentDescription = "Photo preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                        .clickable { imageList = imageList - uriStr },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove Photo", tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Voice Input Trigger
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isRecording) Color(0xFFEF4444).copy(alpha = 0.25f) else SlateCard)
                    .border(1.dp, if (isRecording) Color(0xFFEF4444) else GoldPrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .clickable {
                        isRecording = !isRecording
                        if (isRecording) {
                            val voiceText = "\n[${LanguageManager.getString("voice_input", languageCode)}]"
                            content += voiceText
                        }
                    }
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Mic",
                    tint = if (isRecording) Color.Red else GoldLight,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = LanguageManager.getString("voice_dictation", languageCode),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = LanguageManager.getString("voice_input", languageCode),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Günlük / Anı Başlığı") },
                placeholder = { Text("Örn: Güzel Bir Aile Günü, Çocukluğum...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_journal_title"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = GoldLight,
                    focusedTextColor = activeTextColor,
                    unfocusedTextColor = activeTextColor
                ),
                textStyle = TextStyle(color = activeTextColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Large, spacious, scrollable writing area
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Anılarınızı ve Geleceğe Notlarınızı Yazın...") },
                placeholder = { Text("Bütün hislerinizi, tecrübelerinizi ve sevdiklerinize söylemek istediklerinizi buraya rahatça kaleme alın...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp, max = 360.dp)
                    .testTag("input_journal_content"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = Color.Gray,
                    focusedTextColor = activeTextColor,
                    unfocusedTextColor = activeTextColor
                ),
                textStyle = TextStyle(color = activeTextColor, fontSize = 15.sp, lineHeight = 22.sp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        onSave(title, content, selectedColorHex, imageList.joinToString(","))
                    } else {
                        Toast.makeText(context, "Lütfen başlık ve içerik yazınız.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_journal_entry_button"),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (initialEntry != null) LanguageManager.getString("update_entry", languageCode) else LanguageManager.getString("save_entry", languageCode),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
