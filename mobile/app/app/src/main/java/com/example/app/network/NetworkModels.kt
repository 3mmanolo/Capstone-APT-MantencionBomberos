package com.example.app.network

import com.google.gson.annotations.SerializedName

// Modelos de Petición (Request)
data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class RegistrarMantencionRequest(
    @SerializedName("vehiculo_id") val vehiculoId: Int,
    @SerializedName("tipo") val tipo: String,
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("costo") val costo: String,
    @SerializedName("taller") val taller: String,
    @SerializedName("materiales") val materiales: String,
    @SerializedName("proxima_mantencion") val proximaMantencion: String
)

data class GuardarVehiculoRequest(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("patente") val patente: String,
    @SerializedName("tipo") val tipo: String,
    @SerializedName("anio") val anio: String,
    @SerializedName("kilometraje") val kilometraje: Double,
    @SerializedName("compania_id") val companiaId: Int?,
    @SerializedName("proxima_mantencion") val proximaMantencion: String?
)

data class GuardarUsuarioRequest(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("email") val email: String,
    @SerializedName("rol") val rol: String,
    @SerializedName("compania_id") val companiaId: Int?,
    @SerializedName("telefono") val telefono: String
)

// Modelos de Respuesta (Response)
data class LoginResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("user") val user: UserDto?
)

data class UserDto(
    @SerializedName("id") val id: Int,
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("rol") val rol: String,
    @SerializedName("compania") val compania: String,
    @SerializedName("telefono") val telefono: String,
    @SerializedName("initials") val initials: String,
    @SerializedName("isAdmin") val isAdmin: Boolean
)

data class DashboardResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("stats") val stats: DashboardStatsDto,
    @SerializedName("flota") val flota: List<VehiculoDto>
)

data class DashboardStatsDto(
    @SerializedName("totales") val totales: Int,
    @SerializedName("operativos") val operativos: Int,
    @SerializedName("por_vencer") val porVencer: Int,
    @SerializedName("vencidos") val vencidos: Int
)

data class VehiculoDto(
    @SerializedName("id") val id: String,
    @SerializedName("real_id") val realId: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("patente") val patente: String,
    @SerializedName("compania") val compania: String,
    @SerializedName("estadoText") val estadoText: String,
    @SerializedName("estadoDetalle") val estadoDetalle: String,
    @SerializedName("colorHex") val colorHex: String,
    @SerializedName("kilometraje") val kilometraje: Double,
    @SerializedName("tipo_v") val tipoV: String?
)

data class VehiculosListResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("vehiculos") val vehiculos: List<AdminVehiculoDto>
)

data class AdminVehiculoDto(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("patente") val patente: String,
    @SerializedName("tipo") val tipo: String,
    @SerializedName("anio") val anio: String,
    @SerializedName("compania") val compania: String,
    @SerializedName("compania_id") val companiaId: Int?,
    @SerializedName("kilometraje") val kilometraje: Double,
    @SerializedName("proxima_mantencion") val proximaMantencion: String,
    @SerializedName("estado") val estado: String,
    @SerializedName("status_badge") val statusBadge: String
)

data class HistorialResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("historial") val historial: List<HistorialItemDto>
)

data class HistorialItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("real_id") val realId: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("patente") val patente: String,
    @SerializedName("compania") val compania: String,
    @SerializedName("registros") val registros: Int,
    @SerializedName("colorHex") val colorHex: String
)

data class HistorialDetalleResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("vehiculo") val vehiculo: VehiculoSimpleDto,
    @SerializedName("logs") val logs: List<LogMantencionDto>
)

data class VehiculoSimpleDto(
    @SerializedName("id") val id: String,
    @SerializedName("real_id") val realId: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("patente") val patente: String
)

data class LogMantencionDto(
    @SerializedName("id") val id: Int,
    @SerializedName("fecha") val fecha: String,
    @SerializedName("tipo") val tipo: String,
    @SerializedName("responsable") val responsable: String,
    @SerializedName("kilometraje") val kilometraje: String,
    @SerializedName("costo") val costo: Double,
    @SerializedName("observaciones") val observaciones: String
)

data class NotificacionesResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("notificaciones") val notificaciones: List<NotificacionDto>
)

data class NotificacionDto(
    @SerializedName("id") val id: String,
    @SerializedName("titulo") val titulo: String,
    @SerializedName("mensaje") val mensaje: String,
    @SerializedName("tipo") val tipo: String,
    @SerializedName("fecha") val fecha: String
)

data class UsuariosListResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("usuarios") val usuarios: List<UsuarioAdminDto>
)

data class UsuarioAdminDto(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("email") val email: String,
    @SerializedName("compania") val compania: String,
    @SerializedName("compania_id") val companiaId: Int?,
    @SerializedName("rol") val rol: String,
    @SerializedName("telefono") val telefono: String,
    @SerializedName("initials") val initials: String
)

data class GenericResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("id") val id: Int? = null
)
