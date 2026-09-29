package com.example.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.app.ui.theme.AppTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Habilita el diseño de borde a borde (detrás de la barra de estado y navegación)
        enableEdgeToEdge()
        
        setContent {
            val systemInDarkTheme = isSystemInDarkTheme()
            var isDarkMode by remember { mutableStateOf(systemInDarkTheme) }
            
            AppTheme(darkTheme = isDarkMode) {
                var isLoggedIn by remember { mutableStateOf(false) }
                var isLoading by remember { mutableStateOf(false) }

                LaunchedEffect(isLoading) {
                    if (isLoading) {
                        delay(2000)
                        isLoading = false
                        isLoggedIn = true
                    }
                }

                // Usamos fillMaxSize() sin padding forzado para que el fondo llegue hasta los bordes del celular
                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        isLoading -> {
                            LoadingScreen()
                        }
                        isLoggedIn -> {
                            DashboardScreen(
                                isDarkMode = isDarkMode,
                                onDarkModeChange = { isDarkMode = it },
                                onLogout = { isLoggedIn = false },
                                onExitApp = { finish() },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        else -> {
                            LoginScreen(
                                onLoginSuccess = { isLoading = true },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}
