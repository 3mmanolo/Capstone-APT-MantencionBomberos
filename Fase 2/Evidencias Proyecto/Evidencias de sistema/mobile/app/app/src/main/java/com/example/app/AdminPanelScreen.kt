package com.example.app

import android.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.network.RetrofitClient
import com.example.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminVehiclesScreen(
    onBack: () -> Unit, 
    onAddVehicle: () -> Unit,
    onEditVehicle: (AdminVehicle) -> Unit
) {
    var vehicles by remember { mutableStateOf<List<AdminVehicle>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val fetchVehicles: () -> Unit = {
        coroutineScope.launch {
            isRefreshing = true
            try {
                val response = RetrofitClient.apiService.getVehiculos()
                if (response.isSuccessful && response.body()?.success == true) {
                    vehicles = response.body()!!.vehiculos.map { dto ->
                        val color = if (dto.statusBadge == "green") StatusGreen else if (dto.statusBadge == "amber") StatusOrange else StatusRed
                        AdminVehicle(
                            name = dto.nombre,
                            realId = dto.id,
                            patente = dto.patente,
                            compania = dto.compania,
                            tipo = dto.tipo,
                            anio = dto.anio,
                            estado = dto.estado,
                            color = color
                        )
                    }
                }
            } catch (e: Exception) {
                vehicles = emptyList()
            } finally {
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchVehicles()
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
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "←",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onBack() }
                        .padding(end = 16.dp)
                )
                Column {
                    Text(
                        text = "Vehículos",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${vehicles.size} vehículos en la flota",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(BomberosRed, CircleShape)
                    .clickable { onAddVehicle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_input_add),
                    contentDescription = null, 
                    tint = Color.White, 
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { fetchVehicles() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(vehicles) { vehicle ->
                    AdminCardUI(
                        title = vehicle.name,
                        subtitle = "Patente ${vehicle.patente} · ${vehicle.compania}",
                        extra = "${vehicle.tipo} · Año ${vehicle.anio}",
                        badgeText = vehicle.estado,
                        badgeColor = vehicle.color,
                        initials = vehicle.name.take(1),
                        onEdit = { onEditVehicle(vehicle) },
                        onDelete = {
                            coroutineScope.launch {
                                try {
                                    RetrofitClient.apiService.eliminarVehiculo(vehicle.realId)
                                    fetchVehicles()
                                } catch (e: Exception) {
                                    // Silencioso
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AdminUsersScreen(onBack: () -> Unit, onAddUser: () -> Unit, onEditUser: (AdminUser) -> Unit) {
    var users by remember { mutableStateOf<List<AdminUser>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.apiService.getUsuarios()
            if (response.isSuccessful && response.body()?.success == true) {
                users = response.body()!!.usuarios.map { dto ->
                    AdminUser(
                        initials = dto.initials,
                        realId = dto.id,
                        name = dto.nombre,
                        email = dto.email,
                        compania = dto.compania,
                        role = dto.rol,
                        color = if (dto.rol.lowercase().contains("admin")) StatusRed else StatusGreen
                    )
                }
            }
        } catch (e: Exception) {
            users = emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BomberosBackground)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "←",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onBack() }
                        .padding(end = 16.dp)
                )
                Column {
                    Text(
                        text = "Usuarios",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${users.size} usuarios registrados",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(BomberosRed, CircleShape)
                    .clickable { onAddUser() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_input_add),
                    contentDescription = null, 
                    tint = Color.White, 
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(users) { user ->
                AdminCardUI(
                    title = user.name,
                    subtitle = user.compania,
                    extra = user.email,
                    badgeText = user.role,
                    badgeColor = user.color,
                    initials = user.initials,
                    onEdit = { onEditUser(user) },
                    onDelete = { }
                )
            }
        }
    }
}

@Composable
fun SearchBarUI(placeholder: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(50.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
             Text("🔍", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = placeholder,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun AdminCardUI(
    title: String,
    subtitle: String,
    extra: String,
    badgeText: String,
    badgeColor: Color,
    initials: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(45.dp)
                    .background(BomberosRed, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = initials, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box {
                        Text(
                            text = "⋮", 
                            modifier = Modifier.clickable { expanded = true }.padding(horizontal = 8.dp), 
                            color = MaterialTheme.colorScheme.onSurfaceVariant, 
                            fontSize = 20.sp
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Editar", color = MaterialTheme.colorScheme.onSurface) },
                                onClick = { 
                                    expanded = false
                                    onEdit() 
                                },
                                leadingIcon = { Text("✎") }
                            )
                            DropdownMenuItem(
                                text = { Text("Eliminar", color = BomberosRed) },
                                onClick = { 
                                    expanded = false
                                    onDelete() 
                                },
                                leadingIcon = { Text("🗑") }
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                Text(
                    text = extra,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Surface(
                    color = badgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = badgeText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = badgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

data class AdminVehicle(val name: String, val realId: Int = 0, val patente: String, val compania: String, val tipo: String, val anio: String, val estado: String, val color: Color)
data class AdminUser(val initials: String, val realId: Int = 0, val name: String, val email: String, val compania: String, val role: String, val color: Color)
