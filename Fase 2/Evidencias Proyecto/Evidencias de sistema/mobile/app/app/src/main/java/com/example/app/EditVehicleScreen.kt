package com.example.app

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.app.network.GuardarVehiculoRequest
import com.example.app.network.RetrofitClient
import kotlinx.coroutines.launch
import android.R
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.ui.theme.AppTheme
import com.example.app.ui.theme.BomberosBackground
import com.example.app.ui.theme.BomberosRed

@Composable
fun EditVehicleScreen(
    vehicle: AdminVehicle,
    onBack: () -> Unit
) {
    var codigo by remember { mutableStateOf(vehicle.name.split(" • ").first()) }
    var nombre by remember { mutableStateOf(vehicle.name.split(" • ").last()) }
    var patente by remember { mutableStateOf(vehicle.patente) }
    var anio by remember { mutableStateOf(vehicle.anio) }
    var kilometraje by remember { mutableStateOf(if (vehicle.kilometraje % 1.0 == 0.0) vehicle.kilometraje.toLong().toString() else vehicle.kilometraje.toString()) }
    var proximaMantencion by remember { mutableStateOf(vehicle.proximaMantencion) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var guardando by remember { mutableStateOf(false) }

    val companiasApi = rememberCompanias()
    val companias = companiasApi.map { it.nombre }
    var selectedCompania by remember { mutableStateOf(vehicle.compania) }
    var expandedCompania by remember { mutableStateOf(false) }

    val tipos = listOf("Carro bomba", "Rescate", "Forestal", "Escala")
    var selectedTipo by remember { mutableStateOf(vehicle.tipo) }
    var expandedTipo by remember { mutableStateOf(false) }

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
                    text = "Editar vehículo",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = vehicle.name,
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
            FormField(label = "Código interno", value = codigo, onValueChange = { codigo = it }, placeholder = "Ej: B-5")
            FormField(label = "Nombre del vehículo", value = nombre, onValueChange = { nombre = it }, placeholder = "Ej: Bomba Nueva Estación")
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

            // Próxima mantención
            Column {
                Text(text = "Próxima mantención programada", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = proximaMantencion,
                    onValueChange = { proximaMantencion = it },
                    placeholder = { Text("dd-mm-aaaa", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = { 
                        Icon(
                            painter = painterResource(id = R.drawable.ic_menu_my_calendar),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
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
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                ) {
                    Text(text = "Cancelar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        if (nombre.isBlank()) {
                            Toast.makeText(context, "Ingresa el nombre del vehículo.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        guardando = true
                        coroutineScope.launch {
                            try {
                                val km = kilometraje.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
                                val response = RetrofitClient.apiService.guardarVehiculo(
                                    GuardarVehiculoRequest(
                                        id = vehicle.realId,
                                        nombre = nombre,
                                        patente = patente,
                                        tipo = selectedTipo,
                                        anio = anio,
                                        kilometraje = km,
                                        companiaId = idCompania(companiasApi, selectedCompania),
                                        proximaMantencion = proximaMantencion.ifBlank { null }
                                    )
                                )
                                guardando = false
                                if (response.isSuccessful && response.body()?.success == true) {
                                    Toast.makeText(context, "Vehículo actualizado correctamente.", Toast.LENGTH_LONG).show()
                                    onBack()
                                } else {
                                    Toast.makeText(context, mensajeDeError(response, "No se pudo guardar el vehículo."), Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                guardando = false
                                Toast.makeText(context, "Error de conexión: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !guardando,
                    modifier = Modifier.weight(1f).height(55.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BomberosRed, contentColor = Color.White)
                ) {
                    Text(text = "Guardar vehículo", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
