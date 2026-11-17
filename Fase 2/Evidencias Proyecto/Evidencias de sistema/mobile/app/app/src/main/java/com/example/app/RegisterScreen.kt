package com.example.app

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.network.AdminVehiculoDto
import com.example.app.network.NotificationHelper
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

    val currentDate = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date()) }
    var fechaInicioText by remember { mutableStateOf(currentDate) }
    var fechaTerminoText by remember { mutableStateOf("") }
    var descripcionText by remember { mutableStateOf("") }
    var materialesText by remember { mutableStateOf("") }
    var costoText by remember { mutableStateOf("") }
    var responsableText by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    // Usuario real autenticado (Dinámico)
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
        } catch (e: Exception) {}

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
        } catch (e: Exception) {}
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Topbar / Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BomberosBackground)
                .padding(16.dp)
        ) {
            Column {
                Text(text = "Registrar mantención", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(text = "Nueva entrada al historial del vehículo", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Vehículo Selector
            Column {
                Text(text = "Vehículo", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
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
                        val label = selectedVehiculo?.let { "${it.nombre} · ${it.compania} — ${it.patente}" } ?: "Selecciona un vehículo"
                        Text(text = label, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(text = "▼", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }

                    DropdownMenu(
                        expanded = expandedVehiculoDropdown,
                        onDismissRequest = { expandedVehiculoDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.9f).background(MaterialTheme.colorScheme.surface)
                    ) {
                        vehiculosList.forEach { vehiculo ->
                            DropdownMenuItem(
                                text = { Text("${vehiculo.nombre} · ${vehiculo.compania} — ${vehiculo.patente}", color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    selectedVehiculo = vehiculo
                                    expandedVehiculoDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // 2. Registrado por (Dinámico con datos de sesión)
            Column {
                Text(text = "Registrado por", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                                .size(36.dp)
                                .background(BomberosRed, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = userInitials, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = userName, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(text = "$userRole · $userCompany", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                }
            }

            // 3. Tipo de mantención, Fecha inicio y Fecha término (3 Columnas adaptadas)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Tipo de mantención", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp))
                            .clickable { expandedTipoDropdown = true }
                            .padding(horizontal = 10.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = selectedTipo, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
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
                    Text(text = "Fecha inicio", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                            focusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Fecha término", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = fechaTerminoText,
                        onValueChange = { fechaTerminoText = it },
                        placeholder = { Text("dd-mm-aaaa", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 11.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 4. Descripción del trabajo
            Column {
                Text(text = "Descripción del trabajo", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = descripcionText,
                    onValueChange = { descripcionText = it },
                    placeholder = { Text("Ej: cambio de aceite y filtros, revisión de sistema de frenos...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 13.sp) },
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 5. Materiales e insumos utilizados
            Column {
                Text(text = "Materiales e insumos utilizados", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = materialesText,
                    onValueChange = { materialesText = it },
                    placeholder = { Text("Selecciona un insumo o escribe los materiales...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 6. Otros costos y Responsable
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Otros costos (mano de obra, etc.)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = costoText,
                        onValueChange = { costoText = it },
                        placeholder = { Text("$ 0", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 13.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Responsable", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = responsableText,
                        onValueChange = { responsableText = it },
                        placeholder = { Text("Nombre del responsable o taller", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 7. Alerta / Aviso
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = if (MaterialTheme.colorScheme.surface == Color.White) Color(0xFFFEF3D6) else Color(0xFF4A3C00))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "⚠", color = Color(0xFFB76E00), fontSize = 16.sp)
                    Text(
                        text = "El vehículo pasará a \"en mantención\" hasta cerrar la orden.",
                        color = if (MaterialTheme.colorScheme.surface == Color.White) Color(0xFFB76E00) else Color(0xFFFFD54F),
                        fontSize = 13.sp
                    )
                }
            }

            // 8. Botones Cancelar y Guardar registro
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
                    Text(text = "Cancelar", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        if (isSaving) return@Button
                        val currentVehiculo = selectedVehiculo
                        if (currentVehiculo == null) {
                            Toast.makeText(context, "Selecciona un vehículo", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val vId = currentVehiculo.id
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
                                    fechaTer = fechaTerminoText.ifBlank { null },
                                    proximaMantencion = null
                                )
                                val resp = RetrofitClient.apiService.registrarMantencion(req)
                                isSaving = false
                                if (resp.isSuccessful && resp.body()?.success == true) {
                                    Toast.makeText(context, "Mantención registrada con éxito", Toast.LENGTH_LONG).show()
                                    
                                    try {
                                        val notifResp = RetrofitClient.apiService.getNotificaciones()
                                        if (notifResp.isSuccessful && notifResp.body()?.success == true) {
                                            notifResp.body()!!.notificaciones.forEachIndexed { index, notif ->
                                                NotificationHelper.showNotification(
                                                    context = context,
                                                    id = 4000 + index,
                                                    title = notif.titulo,
                                                    message = notif.mensaje
                                                )
                                            }
                                        }
                                    } catch (e: Exception) {}

                                    descripcionText = ""
                                    costoText = ""
                                    responsableText = ""
                                    materialesText = ""
                                    fechaTerminoText = ""
                                } else {
                                    Toast.makeText(context, "Error al registrar mantención", Toast.LENGTH_SHORT).show()
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