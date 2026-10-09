package com.example.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.network.GuardarVehiculoRequest
import com.example.app.network.RetrofitClient
import com.example.app.ui.theme.AppTheme
import com.example.app.ui.theme.BomberosBackground
import com.example.app.ui.theme.BomberosRed
import com.example.app.ui.theme.StatusGreen
import kotlinx.coroutines.launch

@Composable
fun AddVehicleScreen(onBack: () -> Unit) {
    var codigo by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var patente by remember { mutableStateOf("") }

    val companias = listOf("Compañía 1ª", "Compañía 2ª", "Compañía 3ª", "Compañía 4ª")
    var selectedCompania by remember { mutableStateOf(companias[0]) }
    var expandedCompania by remember { mutableStateOf(false) }

    val tipos = listOf("Carro bomba", "Rescate", "Hazmat", "Forestal", "Agua")
    var selectedTipo by remember { mutableStateOf(tipos[0]) }
    var expandedTipo by remember { mutableStateOf(false) }

    var anio by remember { mutableStateOf("") }
    var kilometraje by remember { mutableStateOf("") }
    var proximaMantencion by remember { mutableStateOf("") }

    var showConfirmDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    // Diálogo 1: Confirmación antes de guardar
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!isLoading) showConfirmDialog = false },
            title = { Text(text = "Confirmar Guardar Vehículo", fontWeight = FontWeight.Bold) },
            text = { Text(text = "¿Deseas registrar el vehículo \"$nombre\" (Patente: $patente) en la base de datos?") },
            confirmButton = {
                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = ""
                        coroutineScope.launch {
                            try {
                                val compIndex = companias.indexOf(selectedCompania) + 1
                                val km = kilometraje.toDoubleOrNull() ?: 0.0
                                val request = GuardarVehiculoRequest(
                                    nombre = nombre,
                                    patente = patente,
                                    tipo = selectedTipo,
                                    anio = anio.ifBlank { "2024" },
                                    kilometraje = km,
                                    companiaId = compIndex,
                                    proximaMantencion = proximaMantencion.ifBlank { null }
                                )
                                val response = RetrofitClient.apiService.guardarVehiculo(request)
                                isLoading = false
                                if (response.isSuccessful && response.body()?.success == true) {
                                    showConfirmDialog = false
                                    showSuccessDialog = true
                                } else {
                                    showConfirmDialog = false
                                    errorMessage = response.body()?.message ?: "Error al guardar vehículo en la base de datos."
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

    // Diálogo 2: Éxito al guardar
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onBack()
            },
            title = { Text(text = "Vehículo Guardado", fontWeight = FontWeight.Bold, color = StatusGreen) },
            text = { Text(text = "El vehículo \"$nombre\" ($patente) ha sido registrado exitosamente en la base de datos.") },
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
                    text = "Agregar vehículo",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Administrar la flota del cuartel",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Código interno
            FormField(label = "Código interno", value = codigo, onValueChange = { codigo = it }, placeholder = "Ej: B-5")

            // Nombre del vehículo
            FormField(label = "Nombre del vehículo", value = nombre, onValueChange = { nombre = it }, placeholder = "Ej: Bomba Nueva Estación")

            // Patente
            FormField(label = "Patente", value = patente, onValueChange = { patente = it }, placeholder = "Ej: ABCD-12")

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
                                fontSize = 10.sp,
                                modifier = Modifier.clickable { expandedCompania = true }
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

            // Tipo
            Column {
                Text(text = "Tipo", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Box {
                    OutlinedTextField(
                        value = selectedTipo,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { 
                            Text(
                                text = "▼", 
                                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                fontSize = 10.sp,
                                modifier = Modifier.clickable { expandedTipo = true }
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
                    Box(modifier = Modifier.matchParentSize().clickable { expandedTipo = true })
                    DropdownMenu(
                        expanded = expandedTipo,
                        onDismissRequest = { expandedTipo = false },
                        modifier = Modifier.fillMaxWidth(0.9f).background(MaterialTheme.colorScheme.surface)
                    ) {
                        tipos.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item, color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    selectedTipo = item
                                    expandedTipo = false
                                }
                            )
                        }
                    }
                }
            }

            // Año y Kilometraje
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FormField(
                    label = "Año", 
                    value = anio, 
                    onValueChange = { anio = it }, 
                    placeholder = "Ej: 2023",
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Number
                )
                FormField(
                    label = "Kilometraje", 
                    value = kilometraje, 
                    onValueChange = { kilometraje = it }, 
                    placeholder = "Ej: 0 km",
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Number
                )
            }

            // Próxima mantención programada
            Column {
                Text(text = "Próxima mantención programada", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = proximaMantencion,
                    onValueChange = { proximaMantencion = it },
                    placeholder = { Text("YYYY-MM-DD", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = BomberosRed.copy(alpha = 0.5f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                        if (nombre.isBlank() || patente.isBlank()) {
                            errorMessage = "Por favor ingresa nombre y patente del vehículo."
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
                    Text(text = "Guardar vehículo", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun FormField(
    label: String, 
    value: String, 
    onValueChange: (String) -> Unit, 
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier = modifier) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = BomberosRed.copy(alpha = 0.5f),
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AddVehicleScreenPreview() {
    AppTheme {
        AddVehicleScreen(onBack = {})
    }
}
