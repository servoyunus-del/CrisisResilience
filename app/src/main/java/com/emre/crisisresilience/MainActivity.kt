package com.emre.crisisresilience

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.emre.crisisresilience.ui.theme.CrisisResilienceTheme

import dagger.hilt.android.AndroidEntryPoint
import com.emre.crisisresilience.ui.navigation.CrisisNavGraph

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CrisisResilienceTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // İçerik boşluğu Scaffold ile korunarak Navigasyon Grafigi çağrılır
                    Box(modifier = Modifier.padding(innerPadding)) {
                        CrisisNavGraph()
                    }
                }
            }
        }
    }
}
