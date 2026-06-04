package com.emre.crisisresilience.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.navigation.NavController
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.emre.crisisresilience.data.local.entity.MessageEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatScreen(
    navController: NavController,
    viewModel: ChatViewModel = hiltViewModel(),
    myStatus: String = "BİLİNMİYOR" // HomeViewModel'dan Navigation args ile gelebilir veya ayrı flow'dan beslenebilir
) {
    val messages by viewModel.messagesFlow.collectAsState(initial = emptyList())
    var inputText by remember { mutableStateOf("") }

    BackHandler {
        viewModel.disconnect()
        navController.popBackStack()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Üst Başlık
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        viewModel.disconnect()
                        navController.popBackStack()
                    },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Geri", tint = Color.White)
                }

                Text(
                    text = "P2P Güvenli Ağ",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = {
                        viewModel.disconnect()
                        navController.popBackStack()
                    },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Bağlantıyı Kes", tint = Color.Red)
                }
            }

            // Mesaj Listesi
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                reverseLayout = true // Yeni mesajlar altta görünmesi için listeyi tersine çeviriyoruz (Room'dan DESC geldiği için bu mantıklı)
            ) {
                items(messages) { msg ->
                    MessageBubble(message = msg)
                }
            }

            // Alt Bar (Mesaj Gönderme)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    placeholder = { Text("Mesaj yazın...", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF2196F3),
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    shape = RoundedCornerShape(24.dp)
                )

                IconButton(
                    onClick = {
                        viewModel.sendMessage(inputText, myStatus)
                        inputText = ""
                    },
                    modifier = Modifier
                        .background(Color(0xFF2196F3), shape = RoundedCornerShape(50))
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Gönder",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: MessageEntity) {
    val isMine = !message.isIncoming
    val alignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (isMine) Color(0xFF2196F3) else Color(0xFF333333)
    val shape = if (isMine) {
        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
    }

    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = sdf.format(Date(message.timestamp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .background(backgroundColor, shape)
                .padding(12.dp)
                .widthIn(max = 260.dp)
        ) {
            // Gönderen Adı
            if (!isMine) {
                Text(
                    text = message.senderName,
                    fontWeight = FontWeight.Bold,
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
            
            // Mesaj Metni
            Text(
                text = message.customText.ifBlank { message.statusMessage },
                color = Color.White,
                fontSize = 16.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Alt Bilgi (Statü + Zaman)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message.statusMessage,
                    color = Color(0xFFFF9800),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = timeString,
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
