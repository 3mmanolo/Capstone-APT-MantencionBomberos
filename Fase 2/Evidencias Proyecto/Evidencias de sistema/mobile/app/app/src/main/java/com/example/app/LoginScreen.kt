package com.example.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.network.LoginRequest
import com.example.app.network.Reenviar2FARequest
import com.example.app.network.RetrofitClient
import com.example.app.network.Verify2FARequest
import com.example.app.ui.theme.AppTheme
import com.example.app.ui.theme.BomberosBackground
import com.example.app.ui.theme.BomberosFieldBg
import com.example.app.ui.theme.BomberosFieldBorder
import com.example.app.ui.theme.BomberosOrange
import com.example.app.ui.theme.BomberosRed
import com.example.app.ui.theme.BomberosTextSecondary
import com.example.app.ui.theme.StatusGreen
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    // Estados para flujo 2FA (Código de Verificación por Correo)
    var step2FA by remember { mutableStateOf(false) }
    var userId2FA by remember { mutableStateOf<Int?>(null) }
    var codigo2FA by remember { mutableStateOf("") }
    var infoMessage by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BomberosBackground)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo Principal
        Image(
            painter = painterResource(id = R.drawable.logo2),
            contentDescription = "Logo Principal Bomberos",
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (!step2FA) {
            // ==========================================
            // PASO 1: Formulario de Usuario y Contraseña
            // ==========================================
            Text(
                text = "Control de mantención\nvehículos de emergencia",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Cuerpo de Bomberos de Talcahuano",
                color = BomberosTextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Campo: Usuario o correo
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Usuario o correo institucional",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMessage = ""
                    },
                    placeholder = { 
                        Text(
                            text = "nombre.apellido@bomberostalcahuano.cl",
                            color = BomberosTextSecondary,
                            fontSize = 13.sp
                        ) 
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = BomberosFieldBg,
                        unfocusedContainerColor = BomberosFieldBg,
                        focusedBorderColor = BomberosRed,
                        unfocusedBorderColor = BomberosFieldBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Campo: Contraseña
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Contraseña",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = ""
                    },
                    placeholder = { 
                        Text(
                            text = "••••••••",
                            color = BomberosTextSecondary,
                            fontSize = 14.sp
                        ) 
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = BomberosFieldBg,
                        unfocusedContainerColor = BomberosFieldBg,
                        focusedBorderColor = BomberosRed,
                        unfocusedBorderColor = BomberosFieldBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón: Iniciar sesión
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Por favor ingresa usuario/correo y contraseña."
                        return@Button
                    }

                    isLoading = true
                    errorMessage = ""

                    coroutineScope.launch {
                        try {
                            val response = RetrofitClient.apiService.login(LoginRequest(email.trim(), password.trim()))
                            isLoading = false
                            if (response.isSuccessful && response.body()?.success == true) {
                                val body = response.body()!!
                                if (body.requires2FA == true && body.userId != null) {
                                    userId2FA = body.userId
                                    step2FA = true
                                    infoMessage = "Hemos enviado un código de 6 dígitos a tu correo electrónico."
                                } else {
                                    RetrofitClient.authToken = body.token
                                    onLoginSuccess()
                                }
                            } else {
                                val msg = response.body()?.message ?: "Credenciales incorrectas. Intenta nuevamente."
                                errorMessage = msg
                            }
                        } catch (e: Exception) {
                            isLoading = false
                            errorMessage = "Error de conexión: No se pudo conectar al servidor de la base de datos."
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BomberosRed,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = "Iniciar sesión",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Enlaces inferiores
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "¿Olvidaste tu contraseña?",
                    color = BomberosOrange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Comp. 1ª - 2ª - 3ª - 4ª",
                    color = BomberosTextSecondary,
                    fontSize = 13.sp
                )
            }

        } else {
            // ==========================================
            // PASO 2: Verificación de Código 2FA por Correo
            // ==========================================
            Text(
                text = "Verificación de identidad",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Hemos enviado un código de 6 dígitos a tu correo electrónico. Ingrésalo para continuar.",
                color = BomberosTextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Campo de código 2FA
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Código de verificación (6 dígitos)",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = codigo2FA,
                    onValueChange = {
                        if (it.length <= 6) {
                            codigo2FA = it
                            errorMessage = ""
                        }
                    },
                    placeholder = { 
                        Text(
                            text = "123456",
                            color = BomberosTextSecondary,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) 
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = BomberosFieldBg,
                        unfocusedContainerColor = BomberosFieldBg,
                        focusedBorderColor = BomberosRed,
                        unfocusedBorderColor = BomberosFieldBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (infoMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = infoMessage,
                    color = StatusGreen,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón: Verificar Código
            Button(
                onClick = {
                    if (codigo2FA.isBlank() || codigo2FA.length < 6) {
                        errorMessage = "Por favor ingresa el código completo de 6 dígitos."
                        return@Button
                    }

                    isLoading = true
                    errorMessage = ""
                    infoMessage = ""

                    coroutineScope.launch {
                        try {
                            val response = RetrofitClient.apiService.verify2FA(
                                Verify2FARequest(userId2FA!!, codigo2FA.trim())
                            )
                            isLoading = false
                            if (response.isSuccessful && response.body()?.success == true) {
                                RetrofitClient.authToken = response.body()?.token
                                onLoginSuccess()
                            } else {
                                errorMessage = response.body()?.message ?: "Código de verificación incorrecto."
                            }
                        } catch (e: Exception) {
                            isLoading = false
                            errorMessage = "Error de conexión al validar código de verificación."
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BomberosRed,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = "Verificar código",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Enlace: Reenviar Código
            TextButton(
                onClick = {
                    if (userId2FA == null || isLoading) return@TextButton
                    isLoading = true
                    errorMessage = ""
                    infoMessage = ""
                    coroutineScope.launch {
                        try {
                            val resp = RetrofitClient.apiService.reenviar2FA(Reenviar2FARequest(userId2FA!!))
                            isLoading = false
                            if (resp.isSuccessful && resp.body()?.success == true) {
                                infoMessage = "Se ha enviado un nuevo código de 6 dígitos a tu correo."
                            } else {
                                errorMessage = resp.body()?.message ?: "Error al reenviar correo."
                            }
                        } catch (e: Exception) {
                            isLoading = false
                            errorMessage = "Error de conexión al reenviar correo."
                        }
                    }
                },
                enabled = !isLoading
            ) {
                Text(
                    text = "¿No llegó o expiró? Reenviar código",
                    color = BomberosOrange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Volver al Login inicial
            Text(
                text = "← Volver al login",
                color = BomberosTextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.clickable {
                    step2FA = false
                    errorMessage = ""
                    infoMessage = ""
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    AppTheme {
        LoginScreen(onLoginSuccess = {})
    }
}
