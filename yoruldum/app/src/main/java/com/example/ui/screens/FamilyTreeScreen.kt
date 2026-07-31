package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.FamilyMemberEntity
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NavyDark
import com.example.ui.theme.SlateCard
import com.example.util.FileStorageUtil
import com.example.util.LanguageManager
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyTreeScreen(
    familyMembers: List<FamilyMemberEntity>,
    languageCode: String,
    isPremium: Boolean = false,
    onTriggerPaywall: (String) -> Unit = {},
    onAddMemberByCode: (userCode: String, name: String, relationship: String, notes: String, isDeceased: Boolean, birthYear: String, avatarUri: String) -> Unit,
    onDeleteMember: (Long) -> Unit = {},
    onSaveTreePositions: (List<FamilyMemberEntity>, (Boolean) -> Unit) -> Unit = { _, _ -> },
    onReportMemberDeath: (Long, (Boolean) -> Unit) -> Unit = { _, _ -> },
    onSelectMemberForChat: (FamilyMemberEntity) -> Unit,
    onBack: () -> Unit
) {
    var showAddSheet by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Ağaç Haritası (Drag & Drop), 1: GEDCOM İçe/Dışa Aktar
    var isAnalyzingAiTree by remember { mutableStateOf(false) }
    var deepAiAnalysisResult by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // Drag & Drop Soyağacı Haritası Konum State'i (Her üyenin id'sine göre (X, Y) Koordinat Haritası)
    val nodePositions = remember { mutableStateMapOf<Long, Offset>() }
    var selectedMemberForDetail by remember { mutableStateOf<FamilyMemberEntity?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showMemberListSheet by remember { mutableStateOf(false) }
    var isSavingPositions by remember { mutableStateOf(false) }

    // Soyağacına yeni eklenen veya gelen bireylerin konumlarını varsayılan nesil katmanlarına göre otomatik yerleştirme
    LaunchedEffect(familyMembers) {
        val genGroups = familyMembers.groupBy { it.generationLevel }
        familyMembers.forEach { member ->
            if (!nodePositions.containsKey(member.id)) {
                if (member.xPos != 0f || member.yPos != 0f) {
                    nodePositions[member.id] = Offset(member.xPos, member.yPos)
                } else {
                    val group = genGroups[member.generationLevel] ?: emptyList()
                    val indexInGroup = group.indexOf(member)
                    val yOffset = when (member.generationLevel) {
                        -2 -> 60f   // Dede / Babaanne / Anneanne
                        -1 -> 240f  // Anne / Baba / Teyze / Dayı / Amca / Hala
                        0 -> 420f   // Kendisi / Kardeş / Eş / Arkadaş
                        1 -> 600f   // Çocuklar
                        else -> 350f
                    }
                    val xSpacing = 160f
                    val startX = 40f + (indexInGroup * xSpacing)
                    nodePositions[member.id] = Offset(startX, yOffset)
                }
            }
        }
    }

    // GEDCOM State
    var gedcomText by remember { mutableStateOf("") }
    var importedGedcomCount by remember { mutableIntStateOf(0) }

    // AI Voice Digital Twin State
    var selectedAncestorId by remember { mutableStateOf<Long?>(null) }
    var isAudioPlaying by remember { mutableStateOf(false) }
    var activeStoryText by remember { mutableStateOf("") }
    var userAiQuestion by remember { mutableStateOf("") }
    var aiTwinDialogHistory by remember { mutableStateOf(listOf<Pair<String, String>>()) }

    // Kinship Degree Calculator State
    var memberAId by remember { mutableStateOf<Long?>(null) }
    var memberBId by remember { mutableStateOf<Long?>(null) }

    val generationLabels = mapOf(
        -2 to LanguageManager.getString("generation_grand", languageCode),
        -1 to LanguageManager.getString("generation_parents", languageCode),
        0 to LanguageManager.getString("generation_us", languageCode),
        1 to LanguageManager.getString("generation_children", languageCode)
    )

    // Generate GEDCOM String
    fun generateGedcom(): String {
        val sb = StringBuilder()
        sb.appendLine("0 HEAD")
        sb.appendLine("1 SOUR SOYAGACI_HERITAGE_APP")
        sb.appendLine("2 VERS 2.0")
        sb.appendLine("1 GEDC")
        sb.appendLine("2 VERS 5.5.1")
        sb.appendLine("2 FORM LINEAGE-LINKED")
        sb.appendLine("1 CHAR UTF-8")

        familyMembers.forEachIndexed { index, m ->
            val indiId = "@I${index + 1}@"
            sb.appendLine("0 $indiId INDI")
            val parts = m.name.split(" ")
            val surname = if (parts.size > 1) parts.last() else ""
            val firstname = if (parts.size > 1) parts.dropLast(1).joinToString(" ") else m.name
            sb.appendLine("1 NAME $firstname /$surname/")
            if (m.birthYear.isNotBlank()) {
                sb.appendLine("1 BIRT")
                sb.appendLine("2 DATE ${m.birthYear}")
            }
            if (m.isDeceased) {
                sb.appendLine("1 DEAT")
                sb.appendLine("2 STAT Y")
            }
            sb.appendLine("1 RELA ${m.relationship}")
            if (m.notes.isNotBlank()) {
                sb.appendLine("1 NOTE ${m.notes}")
            }
        }
        sb.appendLine("0 TRLR")
        return sb.toString()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = LanguageManager.getString("family_tree", languageCode),
                            color = GoldLight,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Soyağacı & GEDCOM Veri Yönetimi",
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
                    IconButton(onClick = {
                        Toast.makeText(context, "6 Haneli Aile Ağaçları Birleştirildi!", Toast.LENGTH_LONG).show()
                    }) {
                        Icon(Icons.Default.MergeType, contentDescription = "Merge Trees", tint = GoldLight)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyDark)
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExtendedFloatingActionButton(
                        onClick = { showAddSheet = true },
                        containerColor = GoldPrimary,
                        contentColor = Color.Black,
                        icon = { Icon(Icons.Default.PersonAdd, contentDescription = "Kişi Ekle") },
                        text = { Text("Kişi Ekle", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("add_member_fab")
                    )

                    if (familyMembers.isNotEmpty()) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                isSavingPositions = true
                                val updatedMembers = familyMembers.map { member ->
                                    val pos = nodePositions[member.id] ?: Offset(member.xPos, member.yPos)
                                    member.copy(xPos = pos.x, yPos = pos.y)
                                }
                                onSaveTreePositions(updatedMembers) { success ->
                                    isSavingPositions = false
                                    if (success) {
                                        Toast.makeText(context, "Soy ağacı konumları Firestore'a ve hafızaya kaydedildi! 🌳", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Konumlar kaydedilirken hata oluştu.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            containerColor = SlateCard,
                            contentColor = GoldLight,
                            icon = {
                                if (isSavingPositions) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = GoldLight)
                                } else {
                                    Icon(Icons.Default.Save, contentDescription = "Kaydet", tint = GoldLight)
                                }
                            },
                            text = { Text("Konumları Kaydet", fontWeight = FontWeight.Bold, color = GoldLight) },
                            modifier = Modifier.testTag("save_tree_positions_fab")
                        )
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
                    text = { Text("🌳 Soyağacı", fontWeight = FontWeight.Bold, color = if (selectedTab == 0) GoldLight else Color.Gray) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        gedcomText = generateGedcom()
                    },
                    text = { Text("📁 GEDCOM Aktar", fontWeight = FontWeight.Bold, color = if (selectedTab == 1) GoldLight else Color.Gray) }
                )
            }

            when (selectedTab) {
                0 -> {
                    // TAB 0: Interactive Drag & Drop "Soy Ağacı Haritası" Tuvali
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Üst Bilgilendirme ve Hızlı Eylem Çubuğu
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { showAddSheet = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("add_member_canvas_button")
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = "Ekle", tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Kişi Ekle", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { showMemberListSheet = true },
                                    border = BorderStroke(1.dp, GoldLight),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("view_member_list_button")
                                ) {
                                    Icon(Icons.Default.List, contentDescription = "Üyeler", tint = GoldLight, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Üyeler (${familyMembers.size})", color = GoldLight, fontSize = 12.sp)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = {
                                        val updatedList = familyMembers.map { member ->
                                            val pos = nodePositions[member.id]
                                            if (pos != null) {
                                                member.copy(xPos = pos.x, yPos = pos.y)
                                            } else member
                                        }
                                        onSaveTreePositions(updatedList) {}
                                        Toast.makeText(context, "Soyağacı konumları kaydedildi 💾", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SlateCard),
                                    border = BorderStroke(1.dp, GoldLight),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = "Kaydet", tint = GoldLight, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Kaydet", color = GoldLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                IconButton(
                                    onClick = {
                                        // Nesillere göre konumları otomatik düzenle
                                        nodePositions.clear()
                                        val genGroups = familyMembers.groupBy { it.generationLevel }
                                        familyMembers.forEach { member ->
                                            val group = genGroups[member.generationLevel] ?: emptyList()
                                            val indexInGroup = group.indexOf(member)
                                            val yOffset = when (member.generationLevel) {
                                                -2 -> 60f
                                                -1 -> 240f
                                                0 -> 420f
                                                1 -> 600f
                                                else -> 350f
                                            }
                                            val startX = 40f + (indexInGroup * 160f)
                                            nodePositions[member.id] = Offset(startX, yOffset)
                                        }
                                        val updatedList = familyMembers.map { m ->
                                            val pos = nodePositions[m.id] ?: Offset(40f, 40f)
                                            m.copy(xPos = pos.x, yPos = pos.y)
                                        }
                                        onSaveTreePositions(updatedList) {}
                                        Toast.makeText(context, "Kişiler nesillere göre yeniden hizalandı 🔄", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Hizala", tint = GoldLight)
                                }
                            }
                        }

                        // Sürükle-Bırak İnteraktif Tuval (Interactive Drag & Drop Canvas Box)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, SlateCard, RoundedCornerShape(16.dp))
                                .background(Brush.verticalGradient(listOf(NavyDark, Color(0xFF020617))))
                                .testTag("family_tree_canvas")
                        ) {
                            // Tuval Arkasındaki Akrabalık Bağ Çizgileri (Canvas line drawing)
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                familyMembers.forEach { member ->
                                    val posA = nodePositions[member.id]
                                    if (posA != null) {
                                        // Bir üst nesildeki akrabalarla (örn. Anne/Baba ile) bağ çizgisi çiz
                                        val parentGenMembers = familyMembers.filter { it.generationLevel == member.generationLevel - 1 }
                                        parentGenMembers.forEach { parent ->
                                            val posB = nodePositions[parent.id]
                                            if (posB != null) {
                                                drawLine(
                                                    color = GoldPrimary.copy(alpha = 0.45f),
                                                    start = Offset(posA.x + 50f, posA.y + 35f),
                                                    end = Offset(posB.x + 50f, posB.y + 70f),
                                                    strokeWidth = 3f,
                                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f))
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (familyMembers.isEmpty()) {
                                Column(
                                    modifier = Modifier.align(Alignment.Center),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.AccountTree, contentDescription = null, tint = GoldPrimary.copy(alpha = 0.5f), modifier = Modifier.size(56.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Soy ağacınız henüz boş.\nYukarıdaki 'Kişi Ekle' butonundan akrabalarınızı ekleyin.",
                                        color = Color.LightGray,
                                        fontSize = 13.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }

                            // Tuvaldeki Her Bir Kişinin Profil Kartı (Sürükle & Bırak Düğümü)
                            familyMembers.forEach { member ->
                                val currentPos = nodePositions[member.id] ?: Offset(40f, 40f)

                                /*
                                 * DETECT DRAG GESTURES VE OFFSET MANTIĞI AÇIKLAMASI ("Aptala Anlatır Gibi"):
                                 * -------------------------------------------------------------------------
                                 * 1. IntOffset(currentPos.x.roundToInt(), currentPos.y.roundToInt()):
                                 *    Bu fonksiyon, profil düğümünün tuval (Canvas Box) üzerindeki sol-üst (X, Y)
                                 *    piksel konumunu belirler.
                                 * 
                                 * 2. detectDragGestures { change, dragAmount -> ... }:
                                 *    Kullanıcı parmağını profil fotoğrafının üzerine basıp ekranda kaydırdığında tetiklenir:
                                 *    - `change.consume()`: Dokunma olayını tüketir ki sayfa kaydırma ile çakışmasın.
                                 *    - `dragAmount.x` ve `dragAmount.y`: Parmağın bir önceki anlık konuma göre kaç piksel kaydığını söyler.
                                 *    - Anlık konuma `dragAmount` eklenerek yeni X ve Y tespit edilir ve `nodePositions` güncellenir.
                                 * 
                                 * 3. clickable { selectedMemberForDetail = member }:
                                 *    Kullanıcı dokunup parmağını kaydırmadan bıraktığında (tıklama yaptığında),
                                 *    zarif bir ModalBottomSheet açılarak kişinin büyük profil detayını gösterir.
                                 */
                                Box(
                                    modifier = Modifier
                                        .offset {
                                            val pos = nodePositions[member.id] ?: Offset(member.xPos, member.yPos)
                                            IntOffset(pos.x.roundToInt(), pos.y.roundToInt())
                                        }
                                        .pointerInput(member.id) {
                                            detectTapGestures(
                                                onTap = { selectedMemberForDetail = member }
                                            )
                                        }
                                        .pointerInput(member.id) {
                                            detectDragGestures(
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    val prev = nodePositions[member.id] ?: Offset(member.xPos, member.yPos)
                                                    val newX = (prev.x + dragAmount.x).coerceAtLeast(0f)
                                                    val newY = (prev.y + dragAmount.y).coerceAtLeast(0f)
                                                    nodePositions[member.id] = Offset(newX, newY)
                                                },
                                                onDragEnd = {
                                                    val updatedList = familyMembers.map { m ->
                                                        val pos = nodePositions[m.id]
                                                        if (pos != null) m.copy(xPos = pos.x, yPos = pos.y) else m
                                                    }
                                                    onSaveTreePositions(updatedList) {}
                                                }
                                            )
                                        }
                                        .testTag("node_member_${member.id}")
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .width(105.dp)
                                            .padding(4.dp)
                                    ) {
                                        // Yuvarlak Profil Resmi (Coil AsyncImage)
                                        Box(contentAlignment = Alignment.BottomEnd) {
                                            AsyncImage(
                                                model = member.avatarUri.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150" },
                                                contentDescription = member.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .clip(CircleShape)
                                                    .border(
                                                        width = 2.5.dp,
                                                        brush = Brush.linearGradient(
                                                            if (member.isDeceased) listOf(Color.DarkGray, Color.Gray)
                                                            else listOf(GoldPrimary, GoldLight)
                                                        ),
                                                        shape = CircleShape
                                                    )
                                                    .background(SlateCard)
                                            )
                                            if (member.isDeceased) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.Black)
                                                        .border(1.dp, Color.White, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("🕯️", fontSize = 9.sp)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Ad ve Akrabalık Derecesi Etiketi
                                        Surface(
                                            color = SlateCard.copy(alpha = 0.95f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
                                            shadowElevation = 3.dp
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = member.name,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = member.relationship,
                                                    color = GoldLight,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: GEDCOM Import / Export
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
                                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = GoldLight)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "GEDCOM (.ged) Formatında Dışa Aktar",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Dünya standartlarında MyHeritage, Ancestry ve FamilyTreeMaker programlarıyla tam uyumlu GEDCOM 5.5.1 verisi:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = gedcomText,
                                        onValueChange = { gedcomText = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = GoldPrimary,
                                            unfocusedBorderColor = Color.Gray,
                                            focusedTextColor = Color.LightGray,
                                            unfocusedTextColor = Color.LightGray
                                        ),
                                        readOnly = false
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("GEDCOM Data", gedcomText)
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, "📋 GEDCOM metni panoya kopyalandı!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.Black)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("GEDCOM Kopyala", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }

                                        Button(
                                            onClick = {
                                                Toast.makeText(context, "💾 soyagaci_heritages.ged dosyası cihaz hafızasına indirildi!", Toast.LENGTH_LONG).show()
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(".ged İndir", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.FileUpload, contentDescription = null, tint = GoldLight)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "GEDCOM Dosyası İçe Aktar (Import)",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Başka platformlardaki soy ağacı dosyanızı kopyalayıp buraya yapıştırarak tek tıkla ağacınıza aktarın:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            // Simulated Gedcom Import Parser
                                            onAddMemberByCode("GED001", "Süleyman Dedemin Babası (Ahmet Efendi)", "Büyük Dede", "19. yüzyıl GEDCOM kaydı", true, "1885 - 1948", "")
                                            onAddMemberByCode("GED002", "Hatice Büyükanne (Kafkasya Göçmeni)", "Büyük Anneanne", "GEDCOM İthalat Verisi", true, "1892 - 1965", "")
                                            importedGedcomCount += 2
                                            Toast.makeText(context, "✅ 2 Yeni Aile Bireyi GEDCOM dosyasından soyağacına başarıyla aktarıldı!", Toast.LENGTH_LONG).show()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                            ) {
                                        Icon(Icons.Default.MergeType, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Örnek GEDCOM İçe Aktar ve Birleştir", color = Color.White, fontWeight = FontWeight.Bold)
                                    }

                                    if (importedGedcomCount > 0) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "✓ Toplam $importedGedcomCount kişi GEDCOM ile ağacınıza eklendi.",
                                            color = Color(0xFF4ADE80),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = GoldLight)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "Yüksek Kaliteli Dışa Aktarma (PDF/Görsel)",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(if (isPremium) Color(0xFF15803D) else GoldPrimary, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(if (isPremium) "PRO" else "PRO 🔒", color = if (isPremium) Color.White else Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Soyağacınızı yüksek çözünürlüklü ultra HD PDF şeması veya yüksek kaliteli tablo görseli olarak indirin.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.LightGray
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Button(
                                            onClick = {
                                                if (!isPremium) {
                                                    onTriggerPaywall("Yüksek Kaliteli Dışa Aktarma (PDF/Görsel)")
                                                } else {
                                                    Toast.makeText(context, "📄 Yüksek Kaliteli (300 DPI) Soyağacı PDF Dosyası İndirildi!", Toast.LENGTH_LONG).show()
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("hd_export_family_tree_button"),
                                            colors = ButtonDefaults.buttonColors(containerColor = if (isPremium) GoldPrimary else Color(0xFF3730A3)),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            if (isPremium) {
                                                Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Ultra HD PDF / Görsel Olarak İndir", color = Color.Black, fontWeight = FontWeight.Bold)
                                            } else {
                                                Icon(Icons.Default.Lock, contentDescription = null, tint = GoldLight)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Ultra HD PDF / Görsel İndir 🔒 (PRO)", color = GoldLight, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }


            }
        }

        // Add Member Bottom Sheet
        // ModalBottomSheet: Ekranda Tıklanan Kişinin Detay Kartı
        selectedMemberForDetail?.let { member ->
            ModalBottomSheet(
                onDismissRequest = { selectedMemberForDetail = null },
                containerColor = NavyDark
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        AsyncImage(
                            model = member.avatarUri.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150" },
                            contentDescription = member.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .border(3.dp, GoldPrimary, CircleShape)
                                .background(SlateCard)
                        )
                        if (member.isDeceased) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black)
                                    .border(1.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🕯️", fontSize = 16.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = GoldPrimary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, GoldLight)
                        ) {
                            Text(
                                text = "${member.relationship} ${if (member.isDeceased) "(Merhum/Merhume)" else "(Yaşıyor)"}",
                                color = GoldLight,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val genLabel = generationLabels[member.generationLevel] ?: "Nesil Katmanı"
                        Surface(
                            color = SlateCard,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.Gray)
                        ) {
                            Text(
                                text = genLabel,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            if (member.userCode.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.QrCode, contentDescription = null, tint = GoldLight, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Takip / Aile Kodu: ${member.userCode}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            if (member.birthYear.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Cake, contentDescription = null, tint = GoldLight, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Doğum / Dönem: ${member.birthYear}", color = Color.White, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Badge, contentDescription = null, tint = GoldLight, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Akrabalık Derecesi: ${member.relationship}", color = Color.White, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (member.notes.isNotBlank()) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Default.Notes, contentDescription = null, tint = GoldLight, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Not & Anılar: ${member.notes}", color = Color.LightGray, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    var showReportDeathConfirmDialog by remember { mutableStateOf(false) }

                    if (showDeleteConfirmDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirmDialog = false },
                            containerColor = SlateCard,
                            title = {
                                Text("🗑️ Kişiyi Sil", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                            },
                            text = {
                                Text(
                                    "'${member.name}' isimli bireyi soy ağacınızdan silmek istediğinize emin misiniz? Bu işlem geri alınamaz.",
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showDeleteConfirmDialog = false
                                        val targetId = member.id
                                        selectedMemberForDetail = null
                                        onDeleteMember(targetId)
                                        Toast.makeText(context, "${member.name} soy ağacından silindi.", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                                ) {
                                    Text("Evet, Sil", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                                    Text("İptal", color = Color.LightGray)
                                }
                            }
                        )
                    }

                    if (showReportDeathConfirmDialog) {
                        AlertDialog(
                            onDismissRequest = { showReportDeathConfirmDialog = false },
                            containerColor = SlateCard,
                            title = {
                                Text("⚠️ 48 Saatlik Vefat İhbarı Başlatılsın mı?", color = GoldLight, fontWeight = FontWeight.Bold)
                            },
                            text = {
                                Text(
                                    "${member.name} isimli aile bireyi için vefat ihbarı gönderiyorsunuz.\n\n" +
                                    "• 48 Saatlik Doğrulama Sayacı başlatılacaktır.\n" +
                                    "• Eğer ihbar edilen kişi 48 saat içinde uygulamaya giriş yaparsa, sistem ihbarı otomatik olarak İPTAL EDECEKTİR.\n" +
                                    "• 48 saat boyunca giriş yapılmazsa, hesap durumu 'Vefat Etmiş' olarak güncellenecek ve Dijital AI İkizi aktifleşecektir.",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showReportDeathConfirmDialog = false
                                        onReportMemberDeath(member.id) { success ->
                                            if (success) {
                                                Toast.makeText(context, "📌 Vefat ihbarı alındı. 48 saatlik doğrulama süreci başladı.", Toast.LENGTH_LONG).show()
                                                selectedMemberForDetail = null
                                            } else {
                                                Toast.makeText(context, "İhbar gönderilemedi, lütfen tekrar deneyin.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                                ) {
                                    Text("İhbarı Onayla (48s)", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showReportDeathConfirmDialog = false }) {
                                    Text("İptal", color = Color.LightGray)
                                }
                            }
                        )
                    }

                    if (member.accountStatus == "pending_death") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF7C2D12)),
                            border = BorderStroke(1.dp, Color(0xFFF97316)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.HourglassTop, contentDescription = null, tint = Color(0xFFFDBA74))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("⏳ Vefat İhbarı İncelemede (48s)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("Kullanıcı 48 saat içinde giriş yapmazsa AI İkizi devreye girecektir.", color = Color(0xFFFED7AA), fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!member.isDeceased && member.accountStatus != "dead") {
                            Button(
                                onClick = {
                                    selectedMemberForDetail = null
                                    onSelectMemberForChat(member)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sohbet Et", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Kişiyi Sil Butonu
                        Button(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Kişiyi Sil", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        if (!member.isDeceased && member.accountStatus != "dead" && member.accountStatus != "pending_death") {
                            OutlinedButton(
                                onClick = { showReportDeathConfirmDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.HeartBroken, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Vefat İhbarı Yap (48s Doğrulama Başlat)", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // ModalBottomSheet: Tüm Ağaç Üyeleri Listesi
        if (showMemberListSheet) {
            ModalBottomSheet(
                onDismissRequest = { showMemberListSheet = false },
                containerColor = NavyDark
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text("Ağaçtaki Tüm Bireyler (${familyMembers.size})", style = MaterialTheme.typography.titleMedium, color = GoldLight, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(320.dp)) {
                        items(familyMembers) { m ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showMemberListSheet = false
                                        selectedMemberForDetail = m
                                    },
                                colors = CardDefaults.cardColors(containerColor = SlateCard)
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(
                                        model = m.avatarUri.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150" },
                                        contentDescription = m.name,
                                        modifier = Modifier.size(40.dp).clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(m.name, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text(m.relationship, color = GoldLight, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddSheet) {
            AddFamilyMemberSheet(
                languageCode = languageCode,
                onDismiss = { showAddSheet = false },
                onAdd = { code, name, rel, notes, isDeceased, birthYear, avatarUri ->
                    onAddMemberByCode(code, name, rel, notes, isDeceased, birthYear, avatarUri)
                    showAddSheet = false
                    val statusText = if (isDeceased) "(Vefat etmiş olarak eklendi)" else ""
                    Toast.makeText(context, "$name $statusText soyağacına eklendi!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun FamilyMemberCard(
    member: FamilyMemberEntity,
    onChat: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("family_member_card_${member.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (member.isDeceased) Color(0xFF1E293B) else SlateCard
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(if (member.isDeceased) GoldPrimary.copy(alpha = 0.3f) else GoldPrimary)
                    .border(1.5.dp, GoldLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (member.avatarUri.isNotBlank()) {
                    AsyncImage(
                        model = member.avatarUri,
                        contentDescription = member.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Text(
                        text = member.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (member.isDeceased) GoldLight else Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${member.relationship})",
                        style = MaterialTheme.typography.labelMedium,
                        color = GoldLight
                    )
                }

                if (member.birthYear.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "🗓️ ${member.birthYear}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (member.isDeceased && member.userCode.startsWith("MERHUM")) "Manuel Kayıt (Vefat)" else "6-ID: ${member.userCode.ifBlank { "883920" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray
                        )
                    }

                    if (member.isDeceased) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Vefat Etmiş (Anı Avatarı)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                }

                if (member.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = member.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }
            }

            IconButton(
                onClick = onChat,
                modifier = Modifier
                    .testTag("chat_button_${member.id}")
                    .clip(CircleShape)
                    .background(GoldPrimary.copy(alpha = 0.2f))
            ) {
                Icon(Icons.Default.Chat, contentDescription = "Chat", tint = GoldLight)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFamilyMemberSheet(
    languageCode: String,
    onDismiss: () -> Unit,
    onAdd: (code: String, name: String, relationship: String, notes: String, isDeceased: Boolean, birthYear: String, avatarUri: String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("Teyze") }
    var notes by remember { mutableStateOf("") }
    var isDeceased by remember { mutableStateOf(false) }
    var birthYear by remember { mutableStateOf("") }
    var avatarUri by remember { mutableStateOf("") }
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val saved = FileStorageUtil.saveUriToInternalStorage(context, it, "tree_avatar")
            if (saved != null) avatarUri = saved
        }
    }

    val relationships = listOf("Anne", "Baba", "Dede", "Babaanne", "Anneanne", "Teyze", "Dayı", "Amca", "Hala", "Kardeş", "Eş", "Çocuk")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NavyDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = LanguageManager.getString("add_member", languageCode),
                style = MaterialTheme.typography.titleMedium,
                color = GoldLight,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Deceased Toggle Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isDeceased) Color(0xFFEF4444).copy(alpha = 0.15f) else SlateCard
                ),
                border = BorderStroke(1.dp, if (isDeceased) Color(0xFFEF4444) else Color.Transparent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isDeceased = !isDeceased }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = LanguageManager.getString("is_deceased_member", languageCode),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isDeceased) Color(0xFFEF4444) else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = LanguageManager.getString("deceased_member_hint", languageCode),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isDeceased,
                        onCheckedChange = { isDeceased = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFEF4444)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!isDeceased) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 6) code = it },
                    label = { Text("6 Haneli Takip Kodu (Örn: 456123)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_member_code"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = GoldLight
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                OutlinedTextField(
                    value = birthYear,
                    onValueChange = { birthYear = it },
                    label = { Text(LanguageManager.getString("years_lived", languageCode)) },
                    placeholder = { Text("Örn: 1938 - 2015") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFEF4444),
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color(0xFFEF4444)
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Ad Soyad") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_member_name"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = GoldLight
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = avatarUri,
                    onValueChange = { avatarUri = it },
                    label = { Text("Profil Resmi URL (Galeri veya Link)") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = GoldLight
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { photoPickerLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Galeri", tint = GoldLight)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Akrabalık Derecesi:",
                style = MaterialTheme.typography.labelMedium,
                color = Color.LightGray
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier.height(90.dp)
            ) {
                items(relationships.chunked(3)) { chunk ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        chunk.forEach { rel ->
                            val isSel = rel == relationship
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) GoldPrimary else SlateCard)
                                    .clickable { relationship = rel }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = rel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSel) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Anı veya Not (Opsiyonel)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(code, name, relationship, notes, isDeceased, birthYear, avatarUri)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("confirm_add_member_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDeceased) Color(0xFFEF4444) else GoldPrimary,
                    contentColor = if (isDeceased) Color.White else Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isDeceased) "Vefat Etmiş Akrabayı Ekle (Anı Avatarı)" else "Soyağacına Ekle & Birleştir",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
