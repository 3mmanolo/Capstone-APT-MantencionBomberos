package com.example.app

import android.R
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.network.AdminVehiculoDto
import com.example.app.network.RegistrarMantencionRequest
import com.example.app.network.RetrofitClient
import com.example.app.ui.theme.AppTheme
import com.example.app.ui.theme.BomberosBackground
import com.example.app.ui.theme.BomberosRed
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RegisterScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var vehiculosList by remember { mutableStateOf<List<AdminVehiculoDto>>(emptyList()) }
    var selectedVehiculo by remember { mutableStateOf<AdminVehiculoDto?>(null) }
    var expandedVehiculoDropdown by remember { mutableStateOf(false) }

    val tiposList = listOf("Preventiva", "Correctiva", "Urgencia")
    var selectedTipo by remember { mutableStateOf(tiposList[0]) }
    var expandedTipoDropdown by remember { mutableStateOf(false) }

    val currentDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var fechaInicioText by remember { mutableStateOf(currentDate) }
    var fechaTerminoText by remember { mutableStateOf("") }
    var descripcionText by remember { mutableStateOf("") }
    var materialesText by remember { mutableStateOf("") }
    var costoText by remember { mutableStateOf("") }
    var responsableText by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    // Estados dinámicos para el usuario real autenticado
    var userName by remember { mutableStateOf("Cargando...") }
    var userRole by remember { mutableStateOf("Voluntario") }
    var userCompany by remember { mutableStateOf("Compañía") }
    var userInitials by remember { mutableStateOf("US") }

    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.apiService.getVehiculos()
            if (response.isSuccessful && response.body()?.success == true) {
                vehiculosList = response.body()!!.vehiculos
                if (vehiculosList.isNotEmpty()) {
                    selectedVehiculo = vehiculosList[0]
                }
            }
        } catch (e: Exception) {
            // Silencioso
        }

        // Cargar perfil del usuario real logueado (igual que en ProfileScreen)
        try {
            val profileResp = RetrofitClient.apiService.getPerfil()
            if (profileResp.isSuccessful && profileResp.body()?.success == true) {
                profileResp.body()?.perfil?.let { p ->
                    userName = p.nombre
                    userRole = p.rol
                    userCompany = p.compania
                    userInitials = p.initials
                }
            }
        } catch (e: Exception) {
            // Silencioso
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header / Selector de Vehículo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BomberosBackground)
                .padding(16.dp)
        ) {
            Column {
                Text(text = "Vehículo", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp))
                        .clickable { expandedVehiculoDropdown = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val label = selectedVehiculo?.let { "${it.nombre} — ${it.patente}" } ?: "Selecciona un vehículo"
                        Text(text = label, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text(text = "▼", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }

                    DropdownMenu(
                        expanded = expandedVehiculoDropdown,
                        onDismissRequest = { expandedVehiculoDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.9f).background(MaterialTheme.colorScheme.surface)
                    ) {
                        vehiculosList.forEach { vehiculo ->
                            DropdownMenuItem(
                                text = { Text("${vehiculo.nombre} — ${vehiculo.patente}", color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    selectedVehiculo = vehiculo
                                    expandedVehiculoDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Registrado por (Dinámico con el usuario real de sesión)
            Column {
                Text(text = "Registrado por", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(BomberosRed, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = userInitials, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = userName, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(text = "$userRole • $userCompany", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Tipo, Fecha inicio y Fecha término (3 Columnas)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Tipo de mantención", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp))
                            .clickable { expandedTipoDropdown = true }
                            .padding(horizontal = 12.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = selectedTipo, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                            Text(text = "▼", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        }
                        DropdownMenu(
                            expanded = expandedTipoDropdown,
                            onDismissRequest = { expandedTipoDropdown = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            tiposList.forEach { tipo ->
                                DropdownMenuItem(
                                    text = { Text(tipo, color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        selectedTipo = tipo
                                        expandedTipoDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Fecha inicio", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = fechaInicioText,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Fecha término", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = fechaTerminoText,
                        onValueChange = { fechaTerminoText = it },
                        placeholder = { Text("aaaa-mm-dd", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Descripción del trabajo
            Column {
                Text(text = "Descripción del trabajo", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = descripcionText,
                    onValueChange = { descripcionText = it },
                    placeholder = { Text("Ej: cambio de aceite y filtros, revisión de frenos...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Materiales e insumos utilizados
            Column {
                Text(text = "Materiales e insumos utilizados", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = materialesText,
                    onValueChange = { materialesText = it },
                    placeholder = { Text("Ej: Filtro de aceite, pastillas de freno...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Costos y Responsable (2 Columnas)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Otros costos (CLP)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = costoText,
                        onValueChange = { costoText = it },
                        placeholder = { Text("$ 0", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Responsable", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = responsableText,
                        onValueChange = { responsableText = it },
                        placeholder = { Text("Nombre del taller o mecánico", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Alerta
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = if (MaterialTheme.colorScheme.surface == Color.White) Color(0xFFFEF3D6) else Color(0xFF4A3C00))
            ) {
                Text(
                    text = "El vehículo pasará a \"en mantención\" hasta cerrar la orden.",
                    color = if (MaterialTheme.colorScheme.surface == Color.White) Color(0xFFB76E00) else Color(0xFFFFD54F),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }

            // Botones
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        descripcionText = ""
                        costoText = ""
                        responsableText = ""
                        materialesText = ""
                        fechaTerminoText = ""
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                ) {
                    Text(text = "Cancelar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        if (isSaving) return@Button
                        val vId = selectedVehiculo?.id ?: 1
                        isSaving = true
                        coroutineScope.launch {
                            try {
                                val req = RegistrarMantencionRequest(
                                    vehiculoId = vId,
                                    tipo = selectedTipo,
                                    descripcion = descripcionText,
                                    costo = costoText,
                                    taller = responsableText,
                                    materiales = materialesText,
                                    proximaMantencion = fechaTerminoText.ifBlank { "2026-12-31" }
                                )
                                val resp = RetrofitClient.apiService.registrarMantencion(req)
                                isSaving = false
                                if (resp.isSuccessful && resp.body()?.success == true) {
                                    Toast.makeText(context, "Mantención registrada con éxito", Toast.LENGTH_LONG).show()
                                    descripcionText = ""
                                    costoText = ""
                                    responsableText = ""
                                    materialesText = ""
                                    fechaTerminoText = ""
                                } else {
                                    val errorJson = try { resp.errorBody()?.string() } catch (e: Exception) { null }
                                    val msg = if (errorJson != null && errorJson.contains("message")) {
                                        try {
                                            com.google.gson.JsonParser.parseString(errorJson).asJsonObject.get("message").asString
                                        } catch (e: Exception) { null }
                                    } else null
                                    Toast.makeText(context, msg ?: "Error al registrar mantención (${resp.code()})", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                isSaving = false
                                Toast.makeText(context, "Error de red", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BomberosRed, contentColor = Color.White),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(text = "Guardar registro", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}