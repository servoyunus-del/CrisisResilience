package com.emre.crisisresilience.ui.screens.home

import android.net.wifi.p2p.WifiP2pDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun HomeScreen(
    onNavigateToChat: () -> Unit,
    onNavigateToSos: () -> Unit,
    onNavigateToMap: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val peers by viewModel.peers.collectAsState()
    val blePeers by viewModel.blePeers.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val myStatus by viewModel.myStatus.collectAsState()
    val isOfflineMode by viewModel.isOfflineMode.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val userBloodType by viewModel.userBloodType.collectAsState()
    val userEmergencyNote by viewModel.userEmergencyNote.collectAsState()
    val userLocationDesc by viewModel.userLocationDesc.collectAsState()
    val incomingMessages by viewModel.incomingMessages.collectAsState()
    var isProfileExpanded by remember { mutableStateOf(false) }

    // Otomatik Sohbet Ekranına Geçiş
    LaunchedEffect(isConnected) {
        if (isConnected) {
            onNavigateToChat()
        }
    }

    val context = LocalContext.current
    var showPermissionRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val nearbyWifiGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.NEARBY_WIFI_DEVICES] ?: false
        } else { true }
        
        val bluetoothScanGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            permissions[Manifest.permission.BLUETOOTH_SCAN] ?: false
        } else { true }

        if (!fineLocationGranted && !coarseLocationGranted) {
            Toast.makeText(context, "Konum izni reddedildi, mesajlar 0.0 koordinatıyla gidecek.", Toast.LENGTH_LONG).show()
        }
        
        if (!nearbyWifiGranted || !bluetoothScanGranted || (!fineLocationGranted && android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU)) {
            showPermissionRationale = true
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        } else {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { Text("İzinler Gerekli") },
            text = { Text("Afet anında cihazların birbirini bulabilmesi ve haberleşebilmesi için Yakındaki Cihazlar, Bluetooth ve Konum izinleri hayati önem taşır. Cihazların sizi bulabilmesi için bu izinleri vermelisiniz.") },
            confirmButton = {
                TextButton(onClick = { showPermissionRationale = false }) {
                    Text("Anladım")
                }
            }
        )
    }

    // Ekran Tasarımı (Dark Mode / Acil Durum Odaklı)
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212) // Koyu arka plan
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "CrisisResilience P2P",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)
            )

            // Çevrimdışı Mod (Offline Mode) Anahtarı
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Çevrimdışı Mod (Ağ Koruması)",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Switch(
                    checked = isOfflineMode,
                    onCheckedChange = { viewModel.toggleOfflineMode(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF4CAF50),
                        checkedTrackColor = Color(0xFF4CAF50).copy(alpha = 0.5f)
                    )
                )
            }

            // Kişisel Acil Profil Bilgileri Kartı
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isProfileExpanded = !isProfileExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "📋 Kişisel Acil Profil Bilgileri",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isProfileExpanded) "Gizle ▲" else "Düzenle ▼",
                            fontSize = 14.sp,
                            color = Color.Cyan
                        )
                    }

                    if (isProfileExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = userName,
                            onValueChange = { viewModel.updateProfile(it, userBloodType, userEmergencyNote, userLocationDesc) },
                            label = { Text("Ad Soyad", color = Color.Gray) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.Cyan,
                                unfocusedBorderColor = Color.DarkGray
                            ),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )

                        OutlinedTextField(
                            value = userBloodType,
                            onValueChange = { viewModel.updateProfile(userName, it, userEmergencyNote, userLocationDesc) },
                            label = { Text("Kan Grubu", color = Color.Gray) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.Cyan,
                                unfocusedBorderColor = Color.DarkGray
                            ),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )

                        OutlinedTextField(
                            value = userEmergencyNote,
                            onValueChange = { viewModel.updateProfile(userName, userBloodType, it, userLocationDesc) },
                            label = { Text("Acil Not (Sağlık Sorunu, İlaç vs.)", color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.Cyan,
                                unfocusedBorderColor = Color.DarkGray
                            ),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )

                        OutlinedTextField(
                            value = userLocationDesc,
                            onValueChange = { viewModel.updateProfile(userName, userBloodType, userEmergencyNote, it) },
                            label = { Text("Konum / Adres Açıklaması", color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.Cyan,
                                unfocusedBorderColor = Color.DarkGray
                            ),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                    } else {
                        if (userName.isNotBlank() || userBloodType.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${if (userName.isNotBlank()) userName else "İsimsiz"} | ${if (userBloodType.isNotBlank()) userBloodType else "Kan Grubu Belirtilmemiş"}",
                                fontSize = 14.sp,
                                color = Color.LightGray
                            )
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Afet durumunda diğer cihazlara gönderilecek bilgilerinizi buraya tıklayarak doldurun.",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // Durum Butonları (Büyük ve Belirgin)
            StatusButton(
                text = "GÜVENDEYİM",
                backgroundColor = Color(0xFF4CAF50), // Yeşil
                isSelected = myStatus == "GÜVENDEYİM"
            ) { viewModel.updateStatus("GÜVENDEYİM") }

            Spacer(modifier = Modifier.height(12.dp))

            StatusButton(
                text = "YARDIM LAZIM",
                backgroundColor = Color(0xFFFF9800), // Turuncu
                isSelected = myStatus == "YARDIM LAZIM"
            ) { viewModel.updateStatus("YARDIM LAZIM") }

            Spacer(modifier = Modifier.height(12.dp))

            StatusButton(
                text = "ENKAZ ALTINDAYIM",
                backgroundColor = Color(0xFFF44336), // Kırmızı
                isSelected = myStatus == "ENKAZ ALTINDAYIM"
            ) { viewModel.updateStatus("ENKAZ ALTINDAYIM") }

            Spacer(modifier = Modifier.height(24.dp))

            // Tarama Butonu
            Button(
                onClick = { viewModel.discoverPeers() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "ETRAFTAKİ CİHAZLARI TARA", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Alt butonlar: SOS ve Harita yan yana
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // SOS Flaş Butonu
                Button(
                    onClick = onNavigateToSos,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "🚨 SOS FLAŞ", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                // Harita Butonu
                Button(
                    onClick = onNavigateToMap,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "🗺️ HARİTA", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                // Acil Durum Bildirim Akışı (Yeni Gelen Bilgiler)
                val incomingOnly = incomingMessages.filter { it.isIncoming }
                if (incomingOnly.isNotEmpty()) {
                    item {
                        Text(
                            text = "📢 Yakındaki Acil Durum Bildirimleri (${incomingOnly.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    
                    items(incomingOnly.take(5)) { msg ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = msg.senderName,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.Cyan,
                                        fontSize = 14.sp
                                    )
                                    val timeString = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(msg.timestamp))
                                    Text(
                                        text = timeString,
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = msg.statusMessage,
                                    color = when {
                                        msg.statusMessage.contains("GÜVENDE", ignoreCase = true) -> Color(0xFF4CAF50)
                                        msg.statusMessage.contains("YARDIM", ignoreCase = true) -> Color(0xFFFF9800)
                                        else -> Color(0xFFF44336)
                                    },
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                                if (msg.customText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = msg.customText,
                                        color = Color.LightGray,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                item {
                    Text(
                        text = "Bulunan Wi-Fi Cihazlar (${peers.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.LightGray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(peers) { device ->
                    DeviceCard(device = device) {
                        viewModel.connectToDevice(device)
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Bulunan BLE Cihazlar (${blePeers.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.LightGray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(blePeers) { macAddress ->
                    BleDeviceCard(macAddress = macAddress) {
                        viewModel.connectToBleDevice(macAddress)
                    }
                }
            }
        }
    }
}

@Composable
fun BleDeviceCard(macAddress: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "BLE Cihazı",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Cyan
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "MAC: $macAddress",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun StatusButton(
    text: String,
    backgroundColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) backgroundColor else backgroundColor.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
    }
}

@Composable
fun DeviceCard(device: WifiP2pDevice, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = device.deviceName.ifEmpty { "İsimsiz Cihaz" },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "MAC: ${device.deviceAddress}",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}
