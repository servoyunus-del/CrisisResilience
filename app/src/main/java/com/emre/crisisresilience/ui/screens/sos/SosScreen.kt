package com.emre.crisisresilience.ui.screens.sos

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SosScreen(
    onNavigateBack: () -> Unit,
    viewModel: SosViewModel = hiltViewModel()
) {
    val isFlashing by viewModel.isFlashing.collectAsState()

    // Ekrandan çıkıldığında flaşı zorla kapatmak için DisposableEffect
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopSos()
        }
    }

    val backgroundColor by animateColorAsState(
        targetValue = if (isFlashing) Color(0xFF8B0000) else Color(0xFF121212),
        animationSpec = tween(durationMillis = 500)
    )

    val buttonColor by animateColorAsState(
        targetValue = if (isFlashing) Color.Black else Color(0xFFF44336),
        animationSpec = tween(durationMillis = 300)
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Geri Butonu
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Geri",
                    tint = Color.White
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "ACİL DURUM\nFLAŞ SİNYALİ",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 64.dp)
                )

                // Dev SOS Butonu
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .clip(CircleShape)
                        .background(buttonColor)
                        .clickable { viewModel.toggleSos() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isFlashing) "DURDUR" else "SOS\nBAŞLAT",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = if (isFlashing) "Mors alfabesiyle (S-O-S) sinyal gönderiliyor...\nKapatmak için butona tekrar basın."
                           else "Bastığınızda arka planda telefon flaşı ve titreşimi ile Mors alfabesinde (S-O-S) sinyali gönderilecektir.",
                    color = Color.LightGray,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    }
}
