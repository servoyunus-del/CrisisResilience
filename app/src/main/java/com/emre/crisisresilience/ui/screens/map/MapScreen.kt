package com.emre.crisisresilience.ui.screens.map

import android.graphics.drawable.GradientDrawable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapScreen(
    onNavigateBack: () -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val userLat by viewModel.userLatitude.collectAsState()
    val userLon by viewModel.userLongitude.collectAsState()

    // OSMDroid konfigürasyonunu ayarla
    LaunchedEffect(Unit) {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            // Tile cache boyutunu ayarla (offline kullanım için)
            tileFileSystemCacheMaxBytes = 100L * 1024 * 1024 // 100 MB cache
            tileFileSystemCacheTrimBytes = 80L * 1024 * 1024
        }
    }

    // MapView referansını tut
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Üst Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Geri",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Çevrimdışı Harita",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                // Konumumu Bul butonu
                IconButton(onClick = {
                    viewModel.fetchCurrentLocation()
                    mapViewRef?.controller?.animateTo(GeoPoint(userLat, userLon))
                }) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Konumum",
                        tint = Color(0xFF2196F3)
                    )
                }
            }

            // Bilgi satırı
            Text(
                text = "📍 Mesajlar: ${messages.size} | Konum: ${"%.4f".format(userLat)}, ${"%.4f".format(userLon)}",
                color = Color.LightGray,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // OSMDroid Harita
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 4.dp),
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(15.0)
                        controller.setCenter(GeoPoint(userLat, userLon))

                        // Kullanıcı konumu marker'ı
                        val userMarker = Marker(this)
                        userMarker.position = GeoPoint(userLat, userLon)
                        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        userMarker.title = "Benim Konumum"
                        userMarker.snippet = "${"%.4f".format(userLat)}, ${"%.4f".format(userLon)}"

                        // Mavi yuvarlak ikon
                        val blueCircle = GradientDrawable()
                        blueCircle.shape = GradientDrawable.OVAL
                        blueCircle.setColor(android.graphics.Color.parseColor("#2196F3"))
                        blueCircle.setStroke(4, android.graphics.Color.WHITE)
                        blueCircle.setSize(48, 48)
                        userMarker.icon = blueCircle

                        overlays.add(userMarker)
                        mapViewRef = this
                    }
                },
                update = { mapView ->
                    // Mevcut marker'ları temizle (kullanıcı marker'ı hariç)
                    val userOverlay = mapView.overlays.firstOrNull()
                    mapView.overlays.clear()
                    if (userOverlay != null) {
                        // Kullanıcı marker'ını güncelle
                        if (userOverlay is Marker) {
                            userOverlay.position = GeoPoint(userLat, userLon)
                        }
                        mapView.overlays.add(userOverlay)
                    }

                    // Mesajlardan gelen konumları marker olarak ekle
                    for (msg in messages) {
                        if (msg.latitude != 0.0 || msg.longitude != 0.0) {
                            val marker = Marker(mapView)
                            marker.position = GeoPoint(msg.latitude, msg.longitude)
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            marker.title = msg.senderName
                            val details = if (msg.customText.isNotBlank()) "\n${msg.customText}" else ""
                            marker.snippet = "${msg.statusMessage}$details (Hop: ${msg.hopCount})"

                            // Duruma göre renk
                            val color = when {
                                msg.statusMessage.contains("GÜVENDEYİM", ignoreCase = true) -> android.graphics.Color.parseColor("#4CAF50")
                                msg.statusMessage.contains("YARDIM", ignoreCase = true) -> android.graphics.Color.parseColor("#FF9800")
                                msg.statusMessage.contains("ENKAZ", ignoreCase = true) -> android.graphics.Color.parseColor("#F44336")
                                else -> android.graphics.Color.parseColor("#9E9E9E")
                            }

                            val circle = GradientDrawable()
                            circle.shape = GradientDrawable.OVAL
                            circle.setColor(color)
                            circle.setStroke(3, android.graphics.Color.WHITE)
                            circle.setSize(40, 40)
                            marker.icon = circle

                            mapView.overlays.add(marker)
                        }
                    }

                    mapView.invalidate()
                }
            )
        }
    }

    // Lifecycle yönetimi
    DisposableEffect(Unit) {
        onDispose {
            mapViewRef?.onDetach()
        }
    }
}
