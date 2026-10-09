package com.example.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.network.RetrofitClient
import com.example.app.ui.theme.AppTheme
import com.example.app.ui.theme.BomberosBackground
import com.example.app.ui.theme.BomberosRed
import com.example.app.ui.theme.StatusGreen
import com.example.app.ui.theme.StatusOrange
import kotlinx.coroutines.launch

@Composable
fun VehicleDetailScreen(
    realVehicleId: Int = 1,
    vehicleId: String,
    vehicleName: String,
    patente: String,
    compania: String,
    estadoText: String,
    estadoDetalle: String,
    estadoColor: Color,
    onBackClick: () -> Unit,
    onVerHistorialClick: () -> Unit,
    onRegistrarMantencionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Diálogo 1: Confirmación
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!isLoading) showConfirmDialog = false },
            title = { Text(text = "Confirmar Cambio de Estado", fontWeight = FontWeight.Bold) },
            text = { Text(text = "¿Deseas cambiar el estado de este vehículo ($vehicleName - $patente) a OPERATIVO en la base de datos?") },
            confirmButton = {
                Button(
                    onClick = {
                        isLoading = true
                        coroutineScope.launch {
                            try {
                                val response = RetrofitClient.apiService.cambiarEstadoOperativo(realVehicleId)
                                isLoading = false
                                if (response.isSuccessful && response.body()?.success == true) {
                                    showConfirmDialog = false
                                    showSuccessDialog = true
                                } else {
                                    showConfirmDialog = false
                                }
                            } catch (e: Exception) {
                                isLoading = false
                                showConfirmDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
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

    // Diálogo 2: Estado Exitoso
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onBackClick()
            },
            title = { Text(text = "Marcado como operativo", fontWeight = FontWeight.Bold, color = StatusGreen) },
            text = { Text(text = "El vehículo $vehicleName ($patente) ha sido marcado como Operativo exitosamente.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen)
                ) {
                    Text("Confirmar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BomberosBackground)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onBackClick() }
                    .padding(end = 16.dp)
            )
            Column {
                Text(
                    text = "$vehicleId • $vehicleName",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Patente $patente · $compania",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = if (MaterialTheme.colorScheme.surface == Color.White) Color(0xFFFEF3D6) else Color(0xFF4A3C00)),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = estadoText,
                            color = if (MaterialTheme.colorScheme.surface == Color.White) estadoColor else Color(0xFFFFD54F),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Text(
                        text = estadoDetalle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoGridCard(label = "Compañía", value = compania, modifier = Modifier.weight(1f))
                    InfoGridCard(label = "Patente", value = patente, modifier = Modifier.weight(1f))
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onVerHistorialClick,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Text(text = "Ver historial", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = onRegistrarMantencionClick,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BomberosRed, contentColor = Color.White),
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Text(text = "Registrar\nmantención", fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 16.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { showConfirmDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusGreen),
                        border = BorderStroke(1.dp, StatusGreen),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text(text = "Marcar como operativo", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun InfoGridCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VehicleDetailScreenPreview() {
    AppTheme {
        VehicleDetailScreen(
            realVehicleId = 1,
            vehicleId = "B-2",
            vehicleName = "Bomba Los Cerros",
            patente = "FRWZ-88",
            compania = "Compañía 2ª",
            estadoText = "Vence en 5 días",
            estadoDetalle = "Programar mantención",
            estadoColor = StatusOrange,
            onBackClick = {},
            onVerHistorialClick = {},
            onRegistrarMantencionClick = {}
        )
    }
}
