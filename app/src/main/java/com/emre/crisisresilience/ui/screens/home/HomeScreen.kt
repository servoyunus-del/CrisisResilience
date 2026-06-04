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
    viewModel: HomeViewModel = hiltViewModel()
) {
    val peers by viewModel.peers.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val myStatus by viewModel.myStatus.collectAsState()

    // Otomatik Sohbet Ekranına Geçiş
    LaunchedEffect(isConnected) {
        if (isConnected) {
            onNavigateToChat()
        }
    }

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (!fineLocationGranted && !coarseLocationGranted) {
            Toast.makeText(context, "Konum izni reddedildi, mesajlar 0.0 koordinatıyla gidecek.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
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
                modifier = Modifier.padding(bottom = 24.dp, top = 16.dp)
            )

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

            Spacer(modifier = Modifier.height(32.dp))

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

            Spacer(modifier = Modifier.height(16.dp))

            // SOS Flaş Butonu
            Button(
                onClick = onNavigateToSos,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "🚨 SOS FLAŞ / SİNYAL", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Cihaz Listesi
            Text(
                text = "Bulunan Cihazlar (${peers.size})",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.LightGray,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(peers) { device ->
                    DeviceCard(device = device) {
                        viewModel.connectToDevice(device)
                    }
                }
            }
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
