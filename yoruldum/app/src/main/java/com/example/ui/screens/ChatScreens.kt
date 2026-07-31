package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.data.local.ChatMessageEntity
import com.example.data.local.FamilyMemberEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NavyDark
import com.example.ui.theme.SlateCard
import com.example.util.LanguageManager

import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.mutableIntStateOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    familyMembers: List<FamilyMemberEntity>,
    languageCode: String,
    onSelectMember: (FamilyMemberEntity) -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Aile Bireyleri, 1: Dijital Kişiler

    val livingMembers = remember(familyMembers) {
        familyMembers.filter { !it.isDeceased && it.accountStatus != "dead" }
    }
    val digitalPersonas = remember(familyMembers) {
        familyMembers.filter { it.isDeceased || it.accountStatus == "dead" || it.accountStatus == "pending_death" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = LanguageManager.getString("chat", languageCode),
                        color = GoldLight,
                        fontWeight = FontWeight.Bold
                    )
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SlateCard,
                contentColor = GoldLight,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = GoldPrimary
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "💬 Aile Bireyleri (${livingMembers.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (selectedTab == 0) GoldLight else Color.Gray
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "🤖 Dijital Kişiler (${digitalPersonas.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (selectedTab == 1) GoldLight else Color.Gray
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val currentList = if (selectedTab == 0) livingMembers else digitalPersonas

            if (currentList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Default.Person else Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = GoldLight.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (selectedTab == 0)
                                "Sohbet edilebilecek aktif yakın bulunmuyor."
                            else
                                "Henüz vefat durumu onaylanan veya Dijital AI İkizi oluşturulan bir kişi bulunmuyor.\n\nSoy ağacından vefat ihbarı yapılan kişilerin 48 saat sonrasında günlük verilerinden Gemini AI Portörü otomatik oluşturulacaktır.",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(currentList) { member ->
                        val isDigitalPersona = member.isDeceased || member.accountStatus == "dead"
                        val isPendingDeath = member.accountStatus == "pending_death"

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chat_list_item_${member.id}")
                                .clickable { onSelectMember(member) },
                            colors = CardDefaults.cardColors(containerColor = SlateCard),
                            shape = RoundedCornerShape(16.dp),
                            border = if (isDigitalPersona) androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(if (isDigitalPersona) GoldPrimary.copy(alpha = 0.3f) else GoldPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isDigitalPersona) Icons.Default.SmartToy else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isDigitalPersona) GoldLight else Color.Black
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = member.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "(${member.relationship})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GoldLight
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    when {
                                        isPendingDeath -> {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.HourglassTop,
                                                    contentDescription = null,
                                                    tint = Color(0xFFF97316),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "⏳ 48s Vefat İhbarı İncelemede (Giriş Bekleniyor)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFFED7AA),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                        isDigitalPersona -> {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Psychology,
                                                    contentDescription = null,
                                                    tint = GoldLight,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "🤖 Dijital AI İkizi (Gemini RAG Günlük Yapısı)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = GoldLight,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        else -> {
                                            Text(
                                                text = "ID: ${member.userCode.ifBlank { "883920" }} • Canlı Mesaj Gönder",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    member: FamilyMemberEntity,
    viewModel: MainViewModel,
    languageCode: String,
    onBack: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val database = com.example.data.local.AppDatabase.getInstance(androidx.compose.ui.platform.LocalContext.current)
    val chatMessagesFlow = remember(member.id) { database.appDao().getChatMessagesForMember(member.id) }
    val messages by chatMessagesFlow.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary),
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
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    fontSize = 16.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = member.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            val isDigitalAvatar = member.isDeceased || member.accountStatus == "dead" || member.accountStatus == "pending_death"
                            Text(
                                text = if (isDigitalAvatar) "🤖 Dijital AI Portör (Gemini RAG)" else "💬 Eklediğiniz Aile Bireyi (${member.relationship})",
                                color = if (isDigitalAvatar) GoldLight else Color(0xFF4ADE80),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
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
        ) {
            val isDigitalAvatar = member.isDeceased || member.accountStatus == "dead" || member.accountStatus == "pending_death"
            if (isDigitalAvatar) {
                // Banner for Deceased Memory Avatar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B))
                        .border(1.dp, GoldPrimary.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = GoldLight)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🤖 ${member.name} Dijital AI Portör (RAG Günlük Yapısı): Geçmiş günlük verileri ve hatıra notları Gemini API'ye systemInstruction olarak aktarılarak onun üslubuyla sohbet eden bir AI karakter kurulmuştur.",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldLight
                        )
                    }
                }
            }

            // Message List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    MessageBubble(msg = msg, memberName = member.name)
                }
            }

            // Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlateCard)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    inputText = "Sesli mesaj kaydı simülasyonu..."
                }) {
                    Icon(Icons.Default.Mic, contentDescription = "Voice", tint = GoldLight)
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Mesajınızı yazın veya konuşun...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessageToMember(member, inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .testTag("send_chat_message_button")
                        .clip(CircleShape)
                        .background(GoldPrimary)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.Black)
                }
            }
        }
    }
}

@Composable
fun MessageBubble(msg: ChatMessageEntity, memberName: String) {
    val isUser = msg.isFromUser
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bgColor = if (isUser) GoldPrimary else SlateCard
    val textColor = if (isUser) Color.Black else Color.White

    Column(
        horizontalAlignment = alignment,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 0.dp,
                        bottomEnd = if (isUser) 0.dp else 16.dp
                    )
                )
                .background(bgColor)
                .padding(12.dp)
        ) {
            Column {
                if (!isUser) {
                    Text(
                        text = if (msg.isAiAvatarResponse) "$memberName (Anı Avatarı)" else memberName,
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldLight,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = msg.messageText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }
        }
    }
}
