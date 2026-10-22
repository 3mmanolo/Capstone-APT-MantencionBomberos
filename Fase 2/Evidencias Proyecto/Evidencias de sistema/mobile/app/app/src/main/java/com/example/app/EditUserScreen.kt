package com.example.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.network.GuardarUsuarioRequest
import com.example.app.network.RetrofitClient
import com.example.app.ui.theme.AppTheme
import com.example.app.ui.theme.BomberosBackground
import com.example.app.ui.theme.BomberosRed
import com.example.app.ui.theme.StatusGreen
import kotlinx.coroutines.launch

@Composable
fun EditUserScreen(
    user: AdminUser,
    onBack: () -> Unit
) {
    var nombre by remember { mutableStateOf(user.name) }
    var correo by remember { mutableStateOf(user.email) }
    var telefono by remember { mutableStateOf("+56 9 1234 5678") }

    val companias = listOf("Compañía 1ª", "Compañía 2ª", "Compañía 3ª", "Compañía 4ª")
    var selectedCompania by remember { mutableStateOf(user.compania) }
    var expandedCompania by remember { mutableStateOf(false) }

    val roles = listOf("Administrador", "Encargado de flota", "Voluntario", "Maquinista")
    var selectedRol by remember { mutableStateOf(user.role) }
    var expandedRol by remember { mutableStateOf(false) }

    var showConfirmDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    // Diálogo 1: Confirmar cambios
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!isLoading) showConfirmDialog = false },
            title = { Text(text = "Confirmar Edición de Usuario", fontWeight = FontWeight.Bold) },
            text = { Text(text = "¿Deseas guardar los cambios realizados al usuario \"$nombre\" en la base de datos?") },
            confirmButton = {
                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = ""
                        coroutineScope.launch {
                            try {
                                val compIndex = companias.indexOf(selectedCompania) + 1
                                val request = GuardarUsuarioRequest(
                                    id = user.realId,
                                    nombre = nombre,
                                    email = correo,
                                    rol = selectedRol,
                                    companiaId = if (compIndex > 0) compIndex else 1,
                                    telefono = telefono
                                )
                                val response = RetrofitClient.apiService.guardarUsuario(request)
                                isLoading = false
                                if (response.isSuccessful && response.body()?.success == true) {
                                    showConfirmDialog = false
                                    showSuccessDialog = true
                                } else {
                                    showConfirmDialog = false
                                    errorMessage = response.body()?.message ?: "Error al actualizar usuario."
                                }
                            } catch (e: Exception) {
                                isLoading = false
                                showConfirmDialog = false
                                errorMessage = "Error de conexión: ${e.localizedMessage}"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BomberosRed),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Confirmar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmDialog = false },
                    enabled = !isLoading
                ) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Diálogo 2: Éxito al actualizar
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onBack()
            },
            title = { Text(text = "Usuario Actualizado", fontWeight = FontWeight.Bold, color = StatusGreen) },
            text = { Text(text = "Los datos del usuario \"$nombre\" fueron actualizados exitosamente en la base de datos.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen)
                ) {
                    Text("Aceptar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BomberosBackground)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "‹", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Editar usuario",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = user.name,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Nombre completo
            FormField(
                label = "Nombre completo", 
                value = nombre, 
                onValueChange = { nombre = it }, 
                placeholder = "Ej: María González Díaz"
            )

            // Correo institucional
            FormField(
                label = "Correo institucional", 
                value = correo, 
                onValueChange = { correo = it }, 
                placeholder = "nombre.apellido@bomberosmelipilla.cl"
            )

            // Teléfono
            FormField(
                label = "Teléfono de contacto", 
                value = telefono, 
                onValueChange = { telefono = it }, 
                placeholder = "+56 9 1234 5678"
            )

            // Compañía
            Column {
                Text(text = "Compañía", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Box {
                    OutlinedTextField(
                        value = selectedCompania,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { 
                            Text(
                                text = "▼", 
                                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                fontSize = 10.sp
                            ) 
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = BomberosRed.copy(alpha = 0.5f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        )
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { expandedCompania = true })
                    DropdownMenu(
                        expanded = expandedCompania,
                        onDismissRequest = { expandedCompania = false },
                        modifier = Modifier.fillMaxWidth(0.9f).background(MaterialTheme.colorScheme.surface)
                    ) {
                        companias.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item, color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    selectedCompania = item
                                    expandedCompania = false
                                }
                            )
                        }
                    }
                }
            }

            // Rol
            Column {
                Text(text = "Rol", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Box {
                    OutlinedTextField(
                        value = selectedRol,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { 
                            Text(
                                text = "▼", 
                                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                fontSize = 10.sp
                            ) 
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = BomberosRed.copy(alpha = 0.5f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        )
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { expandedRol = true })
                    DropdownMenu(
                        expanded = expandedRol,
                        onDismissRequest = { expandedRol = false },
                        modifier = Modifier.fillMaxWidth(0.9f).background(MaterialTheme.colorScheme.surface)
                    ) {
                        roles.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item, color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    selectedRol = item
                                    expandedRol = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f).height(55.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)),
                    enabled = !isLoading
                ) {
                    Text(text = "Cancelar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        if (nombre.isBlank() || correo.isBlank()) {
                            errorMessage = "Por favor ingresa nombre y correo."
                            return@Button
                        }
                        errorMessage = ""
                        showConfirmDialog = true
                    },
                    modifier = Modifier.weight(1f).height(55.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BomberosRed, contentColor = Color.White),
                    enabled = !isLoading
                ) {
                    Text(text = "Guardar usuario", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditUserScreenPreview() {
    AppTheme {
        EditUserScreen(
            user = AdminUser("JP", 1, "Juan Pérez", "juan.perez@bomberosmelipilla.cl", "Compañía 1ª", "Administrador", Color.Red),
            onBack = {}
        )
    }
}
