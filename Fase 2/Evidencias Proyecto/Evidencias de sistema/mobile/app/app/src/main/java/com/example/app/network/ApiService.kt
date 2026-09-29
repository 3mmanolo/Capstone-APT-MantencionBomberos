package com.example.app.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @POST("api/login/")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/dashboard/")
    suspend fun getDashboard(): Response<DashboardResponse>

    @GET("api/vehiculos/")
    suspend fun getVehiculos(): Response<VehiculosListResponse>

    @POST("api/vehiculos/")
    suspend fun guardarVehiculo(@Body request: GuardarVehiculoRequest): Response<GenericResponse>

    @DELETE("api/vehiculos/{id}/eliminar/")
    suspend fun eliminarVehiculo(@Path("id") id: Int): Response<GenericResponse>

    @POST("api/registrar-mantencion/")
    suspend fun registrarMantencion(@Body request: RegistrarMantencionRequest): Response<GenericResponse>

    @GET("api/historial/")
    suspend fun getHistorial(): Response<HistorialResponse>

    @GET("api/historial/{id}/")
    suspend fun getHistorialDetalle(@Path("id") id: Int): Response<HistorialDetalleResponse>

    @GET("api/notificaciones/")
    suspend fun getNotificaciones(): Response<NotificacionesResponse>

    @GET("api/usuarios/")
    suspend fun getUsuarios(): Response<UsuariosListResponse>

    @POST("api/usuarios/")
    suspend fun guardarUsuario(@Body request: GuardarUsuarioRequest): Response<GenericResponse>

    @GET("api/perfil/")
    suspend fun getPerfil(): Response<GenericResponse>
}
