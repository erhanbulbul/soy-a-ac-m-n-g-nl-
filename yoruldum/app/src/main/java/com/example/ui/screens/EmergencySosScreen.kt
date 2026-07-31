package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.emergency.BleRadarManager
import com.example.emergency.DiscoveredBeacon
import com.example.emergency.EmergencySosForegroundService
import com.example.emergency.NearbyStation
import com.example.emergency.NearbyWalkieTalkieManager
import com.example.emergency.ShakeDetector
import kotlin.math.cos
import kotlin.math.sin

val DarkBg = Color(0xFF0F172A)
val SlateCard = Color(0xFF1E293B)
val GoldPrimary = Color(0xFFEAB308)
val RedEmergency = Color(0xFFDC2626)
val RedDark = Color(0xFF991B1B)
val GreenActive = Color(0xFF22C55E)
val BlueRescuer = Color(0xFF0284C7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencySosScreen(
    currentLanguageCode: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Donanım & Mod Yöneticileri
    val shakeDetector = remember { ShakeDetector(context) }
    val bleRadarManager = remember { BleRadarManager(context) }
    val nearbyWalkieTalkieManager = remember { NearbyWalkieTalkieManager(context) }

    // State'ler
    val shakeProgress by shakeDetector.shakeProgress.collectAsState()
    val isShakeMonitoring by shakeDetector.isMonitoring.collectAsState()

    val discoveredBeacons by bleRadarManager.discoveredBeacons.collectAsState()
    val isRadarScanning by bleRadarManager.isScanning.collectAsState()

    val discoveredStations by nearbyWalkieTalkieManager.discoveredStations.collectAsState()
    val isWalkieConnected by nearbyWalkieTalkieManager.isConnected.collectAsState()
    val connectedStationName by nearbyWalkieTalkieManager.connectedStationName.collectAsState()
    val isTransmittingAudio by nearbyWalkieTalkieManager.isTransmitting.collectAsState()
    val isHandsFreeStreaming by NearbyWalkieTalkieManager.globalIsHandsFreeStreaming.collectAsState()

    var isStationEmergencyActive by remember { mutableStateOf(false) }
    var isRescuerModeActive by remember { mutableStateOf(false) }

    // Gerekli İzinler
    val requiredPermissions = remember {
        val list = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(Manifest.permission.BLUETOOTH_SCAN)
            list.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            list.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        list.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            Toast.makeText(context, "İzinler onaylandı! Afet Mesh Ağı hazır.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Sinyal ve ses akışı için tüm izinleri vermelisiniz.", Toast.LENGTH_LONG).show()
        }
    }

    // İvmeölçer Şiddetli Sarsıntı Takibini Başlat
    LaunchedEffect(Unit) {
        shakeDetector.start {
            // 5 saniye şiddetli sarsıntı algılandı -> Otomatik Afet Modunu Aç!
            isStationEmergencyActive = true
            isRescuerModeActive = false
            val intent = Intent(context, EmergencySosForegroundService::class.java).apply {
                action = EmergencySosForegroundService.ACTION_START_EMERGENCY
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Toast.makeText(context, "🚨 OTM. TETİKLEME: 5 Saniyelik Sarsıntı Algılandı! S.O.S Başlatıldı!", Toast.LENGTH_LONG).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            shakeDetector.stop()
            bleRadarManager.stopScan()
            nearbyWalkieTalkieManager.stopDiscovery()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Afet Mesh & BLE Radar",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "İnternetsiz Eşleşmesiz Telsiz & S.O.S Ağı",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        containerColor = DarkBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. BAS-KONUŞ VEYA ELLER SERBEST TELSİZ ARAYÜZÜ (BAĞLANTI AKTİF OLDUĞUNDA)
            if (isWalkieConnected) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isHandsFreeStreaming) RedEmergency.copy(alpha = 0.25f) else GreenActive.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(2.dp, if (isHandsFreeStreaming) RedEmergency else GreenActive)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(20.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isHandsFreeStreaming) Icons.Default.VolumeUp else Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = if (isHandsFreeStreaming) RedEmergency else GreenActive
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (isHandsFreeStreaming) "🚨 ELLER SERBEST CANLI YAYIN AKTİF" else "Telsiz Bağlantısı Aktif (Wi-Fi Direct)",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                            if (connectedStationName.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Bağlı Cihaz: $connectedStationName",
                                    color = GoldPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            if (isHandsFreeStreaming) {
                                // AFET DURUMUNDAKİ KİŞİ İÇİN ELLER SERBEST KART AÇIKLAMASI
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = RedDark),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.GraphicEq,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Mikrofon ve Hoparlör Son Seste Sürekli Açık",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Afet durumunda herhangi bir butona basmanıza gerek yoktur. Ortam sesiniz canlı olarak kurtarıcıya iletilmekte ve gelen sesler duyurulmaktadır.",
                                            color = Color.LightGray,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                // BAS-KONUŞ DEVASAL BUTON
                                Box(
                                    modifier = Modifier
                                        .size(140.dp)
                                        .clip(CircleShape)
                                        .background(if (isTransmittingAudio) RedEmergency else GreenActive)
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onPress = {
                                                    nearbyWalkieTalkieManager.startTransmitting()
                                                    tryAwaitRelease()
                                                    nearbyWalkieTalkieManager.stopTransmitting()
                                                }
                                            )
                                        }
                                        .testTag("push_to_talk_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = if (isTransmittingAudio) Icons.Default.VolumeUp else Icons.Default.Mic,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(52.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            if (isTransmittingAudio) "KONUŞUYORSUN" else "BAS-KONUŞ",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    if (isTransmittingAudio) "Sesiniz karşı cihaza anlık 16kHz Byte Stream olarak iletiliyor..." else "Basılı tutarak konuşun, bıraktığınızda canlı sesi dinleyin.",
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = {
                                        val targetId = NearbyWalkieTalkieManager.globalConnectedEndpointId.value
                                        if (targetId != null) {
                                            NearbyWalkieTalkieManager.startHandsFreeAutoStreaming(context, targetId)
                                        }
                                    }
                                ) {
                                    Text("🎙️ Eller Serbest Moduna Geç (Kesintisiz Yayın)", color = GoldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { nearbyWalkieTalkieManager.disconnect() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text("Telsiz Bağlantısını Kes")
                            }
                        }
                    }
                }
            }

            // 2. İVMEÖLÇER (SENSÖR) DURUM KARTI
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (shakeProgress > 0f) RedEmergency else Color.Gray.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Vibration,
                                    contentDescription = null,
                                    tint = if (shakeProgress > 0f) RedEmergency else GoldPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Sensör Takibi (İvmeölçer)",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                            Badge(containerColor = if (isShakeMonitoring) GreenActive else Color.Gray) {
                                Text(if (isShakeMonitoring) "Aktif (5sn)" else "Kapalı", color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Telefon 5 saniye boyunca şiddetli sarsıntı algılarsa ekran kapalı olsa bile 'Afet Durumundayım' modu otomatik devreye girer.",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )

                        if (shakeProgress > 0f) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { shakeProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = RedEmergency,
                                trackColor = Color.DarkGray
                            )
                            Text(
                                "Şiddetli Sarsıntı Algılanıyor: %${(shakeProgress * 100).toInt()}",
                                color = RedEmergency,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // 2. ANA MOD SEÇİM BUTONLARI ("AFET DURUMUNDAYIM" vs "YARDIM ET")
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // AFET DURUMUNDAYIM (ENKAZ ALTI / İSTASYON MODU)
                    Button(
                        onClick = {
                            val hasPerms = requiredPermissions.all {
                                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                            }
                            if (!hasPerms) {
                                permissionLauncher.launch(requiredPermissions)
                                return@Button
                            }

                            if (!isStationEmergencyActive) {
                                isStationEmergencyActive = true
                                isRescuerModeActive = false
                                bleRadarManager.stopScan()
                                nearbyWalkieTalkieManager.stopDiscovery()

                                val intent = Intent(context, EmergencySosForegroundService::class.java).apply {
                                    action = EmergencySosForegroundService.ACTION_START_EMERGENCY
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(intent)
                                } else {
                                    context.startService(intent)
                                }
                                Toast.makeText(context, "🚨 S.O.S Yayın Servisi ve Hoparlör Başlatıldı!", Toast.LENGTH_SHORT).show()
                            } else {
                                isStationEmergencyActive = false
                                val intent = Intent(context, EmergencySosForegroundService::class.java).apply {
                                    action = EmergencySosForegroundService.ACTION_STOP_EMERGENCY
                                }
                                context.startService(intent)
                                Toast.makeText(context, "S.O.S Yayını Durduruldu.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(90.dp)
                            .testTag("sos_station_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isStationEmergencyActive) RedEmergency else RedDark
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (isStationEmergencyActive) Icons.Default.Warning else Icons.Default.Radio,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (isStationEmergencyActive) "🚨 S.O.S AKTİF" else "AFET DURUMUNDAYIM",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                if (isStationEmergencyActive) "Kapatmak İçin Dokun" else "(Enkaz Altı / İstasyon)",
                                color = Color.LightGray,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // YARDIM ET (KURTARICI / BLE RADAR MODU)
                    Button(
                        onClick = {
                            val hasPerms = requiredPermissions.all {
                                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                            }
                            if (!hasPerms) {
                                permissionLauncher.launch(requiredPermissions)
                                return@Button
                            }

                            if (!isRescuerModeActive) {
                                isRescuerModeActive = true
                                isStationEmergencyActive = false
                                // S.O.S Servisini kapat
                                val intent = Intent(context, EmergencySosForegroundService::class.java).apply {
                                    action = EmergencySosForegroundService.ACTION_STOP_EMERGENCY
                                }
                                context.startService(intent)

                                bleRadarManager.startScan()
                                nearbyWalkieTalkieManager.startDiscovery()
                                Toast.makeText(context, "📡 BLE Radar ve Arama Modu Aktif!", Toast.LENGTH_SHORT).show()
                            } else {
                                isRescuerModeActive = false
                                bleRadarManager.stopScan()
                                nearbyWalkieTalkieManager.stopDiscovery()
                                Toast.makeText(context, "Radar Taraması Durduruldu.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(90.dp)
                            .testTag("rescuer_radar_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRescuerModeActive) BlueRescuer else SlateCard
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BlueRescuer),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Radar,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (isRescuerModeActive) "📡 RADAR AKTİF" else "YARDIM ET",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                if (isRescuerModeActive) "Taramayı Durdur" else "(Kurtarıcı / Sinyal Ara)",
                                color = Color.LightGray,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // 3. ENKAZ ALTI S.O.S İSTASYONU YAYIN BİLGİ KARTI
            if (isStationEmergencyActive) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = RedEmergency.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, RedEmergency)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CellTower, contentDescription = null, tint = RedEmergency)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "BLE & Nearby S.O.S İstasyon Modu Aktif",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("• BLE Advertising ile etrafa eşleşmesiz S.O.S sinyali yayılıyor.", color = Color.LightGray, fontSize = 12.sp)
                            Text("• Nearby Connections kurtarıcı bağlantı taleplerini OTOMATİK kabul ediyor.", color = Color.LightGray, fontSize = 12.sp)
                            Text("• Hoparlör ve Mikrofon donanımsal olarak son seste açıldı.", color = Color.LightGray, fontSize = 12.sp)
                            Text("• Ekran kapansa dahi Ön Plan Servisi çalışmaya devam eder.", color = Color.LightGray, fontSize = 12.sp)
                        }
                    }
                }
            }

            // 4. BLE RADAR VE DİNAMİK RENK EKRANI (KURTARICI MODU)
            if (isRescuerModeActive) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BlueRescuer.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Canlı BLE Afet Radarı",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                "Mesafe Yaklaştıkça Renk Koyu Kan Kırmızısına Dönüşür",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // CANLI ÇİZİLEN COMPOSE RADAR CANVAS
                            AnimatedRadarCanvas(beacons = discoveredBeacons)

                            Spacer(modifier = Modifier.height(16.dp))

                            // RENK SKALASI EFSANESİ (LEGEND)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ColorLegendItem(color = Color(0xFFFF8A80), label = ">15m (Uzak)")
                                ColorLegendItem(color = Color(0xFFE53935), label = "5-15m (Orta)")
                                ColorLegendItem(color = Color(0xFFB71C1C), label = "<2m (Çok Yakın)")
                            }
                        }
                    }
                }

                // BULUNAN S.O.S CİHAZLARI VE TELSİZ BAĞLANTI LİSTESİ
                item {
                    Text(
                        "Algılanan S.O.S Cihazları (${discoveredStations.size.coerceAtLeast(discoveredBeacons.size)})",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                if (discoveredStations.isEmpty() && discoveredBeacons.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = BlueRescuer)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Çevredeki S.O.S cihazları taranıyor...", color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                    }
                } else if (discoveredStations.isNotEmpty()) {
                    items(discoveredStations, key = { it.endpointId }) { station ->
                        val matchingBeacon = discoveredBeacons.find {
                            it.name.contains(station.name, ignoreCase = true) || station.name.contains(it.name, ignoreCase = true)
                        }
                        StationListItem(
                            station = station,
                            beacon = matchingBeacon,
                            onConnect = {
                                nearbyWalkieTalkieManager.connectToStation(station)
                                Toast.makeText(context, "${station.name} cihazı ile telsiz tüneli kuruluyor...", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                } else {
                    items(discoveredBeacons, key = { it.id }) { beacon ->
                        BeaconListItem(
                            beacon = beacon,
                            onConnectAndTalk = {
                                val station = NearbyStation(beacon.id, beacon.name)
                                nearbyWalkieTalkieManager.connectToStation(station)
                                Toast.makeText(context, "${beacon.name} cihazına telsiz tüneli kuruluyor...", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Animated Sweeping Radar Canvas Composable
 */
@Composable
fun AnimatedRadarCanvas(beacons: List<DiscoveredBeacon>) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarSweepAngle"
    )

    Box(
        modifier = Modifier
            .size(260.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(240.dp)
                .background(Color(0xFF020617), CircleShape)
                .border(2.dp, BlueRescuer.copy(alpha = 0.6f), CircleShape)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.width / 2

            // Hedef Daireler (0.2, 0.4, 0.6, 0.8 yarıçap katları)
            for (i in 1..4) {
                drawCircle(
                    color = BlueRescuer.copy(alpha = 0.2f),
                    radius = maxRadius * (i * 0.25f),
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Radar Çapraz Eksen Çizgileri
            drawLine(
                color = BlueRescuer.copy(alpha = 0.25f),
                start = Offset(center.x, 0f),
                end = Offset(center.x, size.height),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = BlueRescuer.copy(alpha = 0.25f),
                start = Offset(0f, center.y),
                end = Offset(size.width, center.y),
                strokeWidth = 1.dp.toPx()
            )

            // Dönen Radar Döner Çizgisi
            val sweepRad = Math.toRadians(angle.toDouble())
            val sweepEnd = Offset(
                x = (center.x + maxRadius * cos(sweepRad)).toFloat(),
                y = (center.y + maxRadius * sin(sweepRad)).toFloat()
            )
            drawLine(
                color = BlueRescuer,
                start = center,
                end = sweepEnd,
                strokeWidth = 2.dp.toPx()
            )

            // Radar Üzerindeki S.O.S Cihaz Noktaları (Blips)
            beacons.forEach { beacon ->
                val rad = Math.toRadians(beacon.radarAngle.toDouble())
                val r = maxRadius * beacon.radarRadiusFactor
                val bx = (center.x + r * cos(rad)).toFloat()
                val by = (center.y + r * sin(rad)).toFloat()

                // Dış Parlama Halkası
                drawCircle(
                    color = beacon.color.copy(alpha = 0.4f),
                    radius = 11.dp.toPx(),
                    center = Offset(bx, by)
                )
                // Ana Nokta
                drawCircle(
                    color = beacon.color,
                    radius = 6.dp.toPx(),
                    center = Offset(bx, by)
                )
            }
        }

        // YÖN ETİKETLERİ (ÖN / SAĞ / SOL / ARKA)
        Text(
            "ÖN (12 Yönü)",
            color = BlueRescuer,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp)
        )
        Text(
            "SOL",
            color = Color.Gray,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp)
        )
        Text(
            "SAĞ",
            color = Color.Gray,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
        )
        Text(
            "ARKA",
            color = Color.Gray,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp)
        )
    }
}

@Composable
fun ColorLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = Color.Gray, fontSize = 10.sp)
    }
}

@Composable
fun BeaconListItem(
    beacon: DiscoveredBeacon,
    onConnectAndTalk: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, beacon.color.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Dinamik Kırmızı Renk Rozeti
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(beacon.color.copy(alpha = 0.2f), CircleShape)
                        .border(1.5.dp, beacon.color, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = beacon.color
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        beacon.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Mesafe: ~${beacon.distanceMeters}m (${beacon.rssi} dBm)",
                        color = beacon.color,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }

            Button(
                onClick = onConnectAndTalk,
                colors = ButtonDefaults.buttonColors(containerColor = BlueRescuer),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Telsiz Bağlan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StationListItem(
    station: NearbyStation,
    beacon: DiscoveredBeacon?,
    onConnect: () -> Unit
) {
    val displayColor = beacon?.color ?: GreenActive
    val distanceText = if (beacon != null) "Mesafe: ~${beacon.distanceMeters}m (${beacon.rssi} dBm)" else "Bağlantıya Hazır (S.O.S Uygulaması)"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, displayColor.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(displayColor.copy(alpha = 0.2f), CircleShape)
                        .border(1.5.dp, displayColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CellTower,
                        contentDescription = null,
                        tint = displayColor
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            station.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Badge(containerColor = GreenActive) {
                            Text("S.O.S Aktif", color = Color.White, fontSize = 9.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        distanceText,
                        color = displayColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onConnect,
                colors = ButtonDefaults.buttonColors(containerColor = BlueRescuer),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Telsiz Bağlan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
