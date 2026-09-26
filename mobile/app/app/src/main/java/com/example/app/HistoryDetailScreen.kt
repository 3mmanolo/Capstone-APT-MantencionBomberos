package com.example.app

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.ui.theme.AppTheme
import com.example.app.ui.theme.BomberosBackground
import com.example.app.ui.theme.BomberosRed

data class LogMantencion(
    val fecha: String,
    val tipo: String,
    val responsable: String,
    val kilometraje: String,
    val observaciones: String
)

@Composable
fun HistoryDetailScreen(
    vehicleName: String,
    vehicleId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                    text = "Registros de $vehicleId",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = vehicleName,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        }

        val listaMantenciones = listOf(
            LogMantencion("12 Feb 2025", "Mantención Preventiva", "Juan Pérez", "45.200 km", "Cambio de aceite de motor, filtros de aire y revisión de presión del sistema hidráulico de la bomba de agua."),
            LogMantencion("28 Ene 2025", "Reparación de Luces", "Carlos Gómez", "44.800 km", "Se reemplazaron ampolletas LED de balizas frontales y se reparó cableado del tablero de control."),
            LogMantencion("15 Dic 2024", "Revisión Técnica", "Planta Melipilla", "42.100 km", "Aprobada sin observaciones. Frenos y gases en óptimo estado."),
            LogMantencion("02 Nov 2024", "Cambio de Neumáticos", "Juan Pérez", "39.500 km", "Reemplazo de los dos neumáticos del eje delantero por desgaste severo.")
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(listaMantenciones) { mantencion ->
                MantencionItemCard(mantencion)
            }
        }
    }
}

@Composable
fun MantencionItemCard(mantencion: LogMantencion) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = mantencion.tipo,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = mantencion.fecha,
                    color = BomberosRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Responsable: ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(text = mantencion.responsable, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = "Kilometraje: ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(text = mantencion.kilometraje, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            Text(text = "Detalle y Observaciones:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = mantencion.observaciones,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HistoryDetailScreenPreview() {
    AppTheme {
        HistoryDetailScreen(vehicleName = "Bomba Melipilla", vehicleId = "B-1", onBackClick = {})
    }
}
