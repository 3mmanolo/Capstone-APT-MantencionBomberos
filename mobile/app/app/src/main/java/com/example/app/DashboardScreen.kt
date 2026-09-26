package com.example.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.ui.theme.*

data class StatItem(val label: String, val value: String, val valueColor: Color)
data class Vehiculo(
    val id: String,
    val nombre: String,
    val patente: String,
    val compania: String,
    val estadoText: String,
    val estadoDetalle: String,
    val estadoColor: Color
)

@Composable
fun DashboardScreen(
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
    onExitApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    // Estado para manejar si hay un vehículo seleccionado desde el inicio
    var detailedVehicle by remember { mutableStateOf<Vehiculo?>(null) }
    
    // Estados para los paneles de administración y edición
    var isAdminVehiclesActive by remember { mutableStateOf(false) }
    var isAdminUsersActive by remember { mutableStateOf(false) }
    var isAddingVehicle by remember { mutableStateOf(false) }
    var isAddingUser by remember { mutableStateOf(false) }
    var isEditingVehicle by remember { mutableStateOf(false) }
    var isEditingUser by remember { mutableStateOf(false) }
    var editingVehicleData by remember { mutableStateOf<AdminVehicle?>(null) }
    var editingUserData by remember { mutableStateOf<AdminUser?>(null) }

    // Estado para el diálogo de salida
    var showExitDialog by remember { mutableStateOf(false) }

    // Manejador del gesto atrás/deslizar borde
    BackHandler(
        enabled = true // Siempre habilitado para controlar la salida
    ) {
        when {
            isAddingUser -> isAddingUser = false
            isEditingUser -> isEditingUser = false
            isAddingVehicle -> isAddingVehicle = false
            isEditingVehicle -> isEditingVehicle = false
            isAdminVehiclesActive -> isAdminVehiclesActive = false
            isAdminUsersActive -> isAdminUsersActive = false
            detailedVehicle != null -> detailedVehicle = null
            selectedTab != 0 -> selectedTab = 0
            else -> showExitDialog = true // Estamos en el inicio, mostramos el diálogo
        }
    }

    // Diálogo de Confirmación de Salida
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(text = "Cerrar Aplicación", fontWeight = FontWeight.Bold) },
            text = { Text(text = "¿Estás seguro de que deseas cerrar la aplicación de Bomberos?") },
            confirmButton = {
                Button(
                    onClick = onExitApp,
                    colors = ButtonDefaults.buttonColors(containerColor = BomberosRed)
                ) {
                    Text("Sí, salir", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = if (isDarkMode) Color(0xFF1C1C1E) else Color.White,
                tonalElevation = 0.dp
            ) {
                // Pestaña 1: Inicio
                NavigationBarItem(
                    selected = selectedTab == 0 && !isAdminVehiclesActive && !isAdminUsersActive && !isAddingVehicle && !isEditingVehicle && !isAddingUser && !isEditingUser,
                    onClick = { 
                        selectedTab = 0
                        detailedVehicle = null
                        isAdminVehiclesActive = false
                        isAdminUsersActive = false
                        isAddingVehicle = false
                        isEditingVehicle = false
                        isAddingUser = false
                        isEditingUser = false
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_dialog_dialer),
                            contentDescription = "Inicio",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text("Inicio", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BomberosRed,
                        selectedTextColor = BomberosRed,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = Color.Transparent
                    )
                )

                // Pestaña 2: Registrar
                NavigationBarItem(
                    selected = selectedTab == 1 && !isAdminVehiclesActive && !isAdminUsersActive && !isAddingVehicle && !isEditingVehicle && !isAddingUser && !isEditingUser,
                    onClick = { 
                        selectedTab = 1
                        detailedVehicle = null
                        isAdminVehiclesActive = false
                        isAdminUsersActive = false
                        isAddingVehicle = false
                        isEditingVehicle = false
                        isAddingUser = false
                        isEditingUser = false
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_input_add),
                            contentDescription = "Registrar",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text("Registrar", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BomberosRed,
                        selectedTextColor = BomberosRed,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = Color.Transparent
                    )
                )

                // Pestaña 3: Historial
                NavigationBarItem(
                    selected = selectedTab == 2 && !isAdminVehiclesActive && !isAdminUsersActive && !isAddingVehicle && !isEditingVehicle && !isAddingUser && !isEditingUser,
                    onClick = { 
                        selectedTab = 2
                        detailedVehicle = null
                        isAdminVehiclesActive = false
                        isAdminUsersActive = false
                        isAddingVehicle = false
                        isEditingVehicle = false
                        isAddingUser = false
                        isEditingUser = false
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_recent_history),
                            contentDescription = "Historial",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text("Historial", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BomberosRed,
                        selectedTextColor = BomberosRed,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = Color.Transparent
                    )
                )

                // Pestaña 4: Perfil
                NavigationBarItem(
                    selected = selectedTab == 3 || isAdminVehiclesActive || isAdminUsersActive || isAddingVehicle || isEditingVehicle || isAddingUser || isEditingUser,
                    onClick = { 
                        selectedTab = 3
                        detailedVehicle = null
                        isAdminVehiclesActive = false
                        isAdminUsersActive = false
                        isAddingVehicle = false
                        isEditingVehicle = false
                        isAddingUser = false
                        isEditingUser = false
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_perfil_usuario),
                            contentDescription = "Perfil",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text("Perfil", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BomberosRed,
                        selectedTextColor = BomberosRed,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when {
                isAddingUser -> AddUserScreen(onBack = { isAddingUser = false })
                isEditingUser && editingUserData != null -> EditUserScreen(
                    user = editingUserData!!,
                    onBack = { isEditingUser = false }
                )
                isAddingVehicle -> AddVehicleScreen(onBack = { isAddingVehicle = false })
                isEditingVehicle && editingVehicleData != null -> EditVehicleScreen(
                    vehicle = editingVehicleData!!,
                    onBack = { isEditingVehicle = false }
                )
                isAdminVehiclesActive -> AdminVehiclesScreen(
                    onBack = { isAdminVehiclesActive = false },
                    onAddVehicle = { isAddingVehicle = true },
                    onEditVehicle = { vehicle ->
                        editingVehicleData = vehicle
                        isEditingVehicle = true
                    }
                )
                isAdminUsersActive -> AdminUsersScreen(
                    onBack = { isAdminUsersActive = false },
                    onAddUser = { isAddingUser = true },
                    onEditUser = { user ->
                        editingUserData = user
                        isEditingUser = true
                    }
                )
                selectedTab == 0 -> {
                    if (detailedVehicle != null) {
                        VehicleDetailScreen(
                            vehicleId = detailedVehicle!!.id,
                            vehicleName = detailedVehicle!!.nombre,
                            patente = detailedVehicle!!.patente,
                            compania = detailedVehicle!!.compania,
                            estadoText = detailedVehicle!!.estadoText,
                            estadoDetalle = detailedVehicle!!.estadoDetalle,
                            estadoColor = detailedVehicle!!.estadoColor,
                            onBackClick = { detailedVehicle = null },
                            onVerHistorialClick = { 
                                detailedVehicle = null
                                selectedTab = 2 
                            },
                            onRegistrarMantencionClick = {
                                detailedVehicle = null
                                selectedTab = 1 
                            }
                        )
                    } else {
                        MainDashboardContent(onVehicleClick = { detailedVehicle = it })
                    }
                }
                selectedTab == 1 -> RegisterScreen()
                selectedTab == 2 -> HistoryScreen()
                selectedTab == 3 -> ProfileScreen(
                    isDarkMode = isDarkMode,
                    onDarkModeChange = onDarkModeChange,
                    onManageVehicles = { isAdminVehiclesActive = true },
                    onManageUsers = { isAdminUsersActive = true },
                    onLogout = onLogout
                )
            }
        }
    }
}

@Composable
fun MainDashboardContent(onVehicleClick: (Vehiculo) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header Superior
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BomberosBackground)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo Bomberos",
                modifier = Modifier
                    .size(45.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Control de mantención",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Bomberos Melipilla",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }
        }

        // Lista e Indicadores
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                val stats = listOf(
                    StatItem("Vehículos totales", "5", MaterialTheme.colorScheme.onBackground),
                    StatItem("Operativos", "2", StatusGreen),
                    StatItem("Por vencer", "2", StatusOrange),
                    StatItem("Alertas vencidas", "1", StatusRed)
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(stats[0], modifier = Modifier.weight(1f))
                        StatCard(stats[1], modifier = Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(stats[2], modifier = Modifier.weight(1f))
                        StatCard(stats[3], modifier = Modifier.weight(1f))
                    }
                }
            }

            item {
                Text(
                    text = "Flota de vehículos",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            val flota = listOf(
                Vehiculo("B-1", "Bomba Melipilla", "HXPL-21", "Compañía 1ª", "Mantención vencida", "Venció hace 6 días", StatusRed),
                Vehiculo("B-2", "Bomba Los Cerros", "FRWZ-88", "Compañía 2ª", "Vence en 5 días", "Programar mantención", StatusOrange),
                Vehiculo("R-1", "Rescute Vehicular", "KTLM-05", "Compañía 3ª", "Vence en 4 días", "Programar mantención", StatusOrange),
                Vehiculo("B-3", "Bomba Centro", "JNPX-47", "Compañía 4ª", "Operativo", "Próxima mantención en 79 días", StatusGreen),
                Vehiculo("B-4", "Bomba Forestal", "DGRT-63", "Compañía 1ª", "Operativo", "Próxima mantención en 95 días", StatusGreen)
            )

            items(flota) { vehiculo ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onVehicleClick(vehiculo) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height(90.dp)
                                .background(vehiculo.estadoColor, shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                        )
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.2f)) {
                                Text(
                                    text = "${vehiculo.id} • ${vehiculo.nombre}",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Patente ${vehiculo.patente} • ${vehiculo.compania}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                            
                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = vehiculo.estadoText,
                                    color = vehiculo.estadoColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.End
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = vehiculo.estadoDetalle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.End,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(stat: StatItem, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = stat.label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stat.value, color = stat.valueColor, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    AppTheme {
        DashboardScreen(
            isDarkMode = false,
            onDarkModeChange = {},
            onLogout = {},
            onExitApp = {}
        )
    }
}
