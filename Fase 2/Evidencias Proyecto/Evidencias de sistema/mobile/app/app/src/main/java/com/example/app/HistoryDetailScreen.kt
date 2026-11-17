package com.example.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.network.RetrofitClient
import com.example.app.ui.theme.AppTheme
import com.example.app.ui.theme.BomberosBackground
import com.example.app.ui.theme.BomberosRed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LogMantencion(
    val fecha: String,
    val tipo: String,
    val responsable: String,
    val kilometraje: String,
    val observaciones: String
)

@Composable
fun HistoryDetailScreen(
    realVehicleId: Int = 1,
    vehicleName: String,
    vehicleId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var listaMantenciones by remember { mutableStateOf<List<LogMantencion>>(emptyList()) }
    var isDownloadingPdf by remember { mutableStateOf(false) }
    var showPdfResultDialog by remember { mutableStateOf(false) }
    var pdfResultMessage by remember { mutableStateOf("") }

    LaunchedEffect(realVehicleId) {
        try {
            val response = RetrofitClient.apiService.getHistorialDetalle(realVehicleId)
            if (response.isSuccessful && response.body()?.success == true) {
                listaMantenciones = response.body()!!.logs.map { dto ->
                    LogMantencion(
                        fecha = dto.fecha,
                        tipo = dto.tipo,
                        responsable = dto.responsable,
                        kilometraje = dto.kilometraje,
                        observaciones = dto.observaciones
                    )
                }
            }
        } catch (e: Exception) {
            listaMantenciones = emptyList()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header con botón de Descargar PDF
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BomberosBackground)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
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
                        text = "Registros de $vehicleName",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Historial técnico de mantenimientos",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }
            }

            // Botón Descargar PDF
            Button(
                onClick = {
                    if (isDownloadingPdf) return@Button
                    isDownloadingPdf = true
                    downloadPdfForHistory(realVehicleId, vehicleName, context) { msg ->
                        pdfResultMessage = msg
                        showPdfResultDialog = true
                        isDownloadingPdf = false
                    }
                },
                enabled = !isDownloadingPdf,
                colors = ButtonDefaults.buttonColors(containerColor = BomberosRed),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (isDownloadingPdf) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                } else {
                    Text(text = "Descargar PDF", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

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

    // Diálogo de Resultado Descarga PDF
    if (showPdfResultDialog) {
        AlertDialog(
            onDismissRequest = { showPdfResultDialog = false },
            title = { Text(text = "Informe PDF", fontWeight = FontWeight.Bold) },
            text = { Text(text = pdfResultMessage) },
            confirmButton = {
                Button(
                    onClick = { showPdfResultDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BomberosRed)
                ) {
                    Text("Aceptar", color = Color.White)
                }
            }
        )
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
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Responsable: ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(text = mantencion.responsable, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Kilometraje: ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(text = mantencion.kilometraje, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                }
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

private fun downloadPdfForHistory(realVehicleId: Int, patente: String, context: android.content.Context, onResult: (String) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitClient.apiService.descargarPdfVehiculo(realVehicleId)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val fileName = "Informe_${patente}_${System.currentTimeMillis()}.pdf"
                var savedUri: android.net.Uri? = null

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val contentValues = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = resolver.insert(android.provider.MediaStore.Files.getContentUri("external"), contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            body.byteStream().use { inputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                        savedUri = uri
                    }
                } else {
                    val targetDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    if (!targetDir.exists()) targetDir.mkdirs()
                    val targetFile = java.io.File(targetDir, fileName)
                    targetFile.outputStream().use { outputStream ->
                        body.byteStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    savedUri = android.net.Uri.fromFile(targetFile)
                }

                if (savedUri != null) {
                    com.example.app.network.NotificationHelper.showPdfDownloadNotification(context, savedUri, fileName)
                }

                withContext(Dispatchers.Main) {
                    onResult("PDF descargado exitosamente en la carpeta Descargas.")
                }
            } else {
                withContext(Dispatchers.Main) {
                    onResult("Error al generar el PDF (Código: ${response.code()})")
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onResult("Error de red: ${e.localizedMessage}")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HistoryDetailScreenPreview() {
    AppTheme {
        HistoryDetailScreen(realVehicleId = 1, vehicleName = "Bomba Melipilla", vehicleId = "B-1", onBackClick = {})
    }
}
