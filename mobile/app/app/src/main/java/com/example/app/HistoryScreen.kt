package com.example.app

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.ui.theme.AppTheme
import com.example.app.ui.theme.BomberosBackground
import com.example.app.ui.theme.StatusGreen
import com.example.app.ui.theme.StatusOrange

data class HistoryItem(
    val id: String,
    val nombre: String,
    val patente: String,
    val compania: String,
    val registros: Int,
    val color: Color
)

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    var selectedVehicle by remember { mutableStateOf<HistoryItem?>(null) }

    // Manejador del gesto atrás/deslizar borde en el historial
    BackHandler(enabled = selectedVehicle != null) {
        selectedVehicle = null
    }

    if (selectedVehicle != null) {
        HistoryDetailScreen(
            vehicleId = selectedVehicle!!.id,
            vehicleName = selectedVehicle!!.nombre,
            onBackClick = { selectedVehicle = null }
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BomberosBackground)
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                Text(
                    text = "Historial de mantenciones",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Selecciona un vehículo",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }

            val historyData = listOf(
                HistoryItem("B-1", "Bomba Melipilla", "HXPL-21", "Compañía 1ª", 4, StatusGreen),
                HistoryItem("B-2", "Bomba Los Cerros", "FRWZ-88", "Compañía 2ª", 2, StatusOrange),
                HistoryItem("R-1", "Rescute Vehicular", "KTLM-05", "Compañía 3ª", 1, StatusOrange),
                HistoryItem("B-3", "Bomba Centro", "JNPX-47", "Compañía 4ª", 1, StatusGreen),
                HistoryItem("B-4", "Bomba Forestal", "DGRT-63", "Compañía 1ª", 1, StatusGreen)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(historyData) { item ->
                    HistoryVehicleCard(item, onClick = { selectedVehicle = item })
                }
            }
        }
    }
}

@Composable
fun HistoryVehicleCard(item: HistoryItem, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Línea de estado lateral
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(90.dp)
                    .background(item.color, shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${item.id} • ${item.nombre}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Patente ${item.patente} • ${item.compania}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${item.registros} registros",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Ver historial",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview() {
    AppTheme {
        HistoryScreen()
    }
}
