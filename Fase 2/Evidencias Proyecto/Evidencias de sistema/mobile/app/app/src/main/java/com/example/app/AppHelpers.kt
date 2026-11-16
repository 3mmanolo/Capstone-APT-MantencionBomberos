package com.example.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.app.network.CompaniaDto
import com.example.app.network.RetrofitClient
import retrofit2.Response

/** Carga la lista real de compañías desde el servidor (GET api/companias/). */
@Composable
fun rememberCompanias(): List<CompaniaDto> {
    var lista by remember { mutableStateOf<List<CompaniaDto>>(emptyList()) }
    LaunchedEffect(Unit) {
        try {
            val r = RetrofitClient.apiService.getCompanias()
            if (r.isSuccessful && r.body()?.success == true) lista = r.body()!!.companias
        } catch (e: Exception) {
            // Sin conexión: la lista queda vacía y el servidor asigna la compañía por defecto
        }
    }
    return lista
}

/** Id real (de la base de datos) de la compañía elegida por nombre, o null si no se encuentra. */
fun idCompania(companias: List<CompaniaDto>, nombre: String): Int? =
    companias.firstOrNull { it.nombre == nombre }?.id

/** Mensaje de error que devuelve el servidor (campo "message") o uno por defecto. */
fun mensajeDeError(response: Response<*>, porDefecto: String): String {
    val json = try { response.errorBody()?.string() } catch (e: Exception) { null }
    val msg = try {
        if (json != null) com.google.gson.JsonParser.parseString(json).asJsonObject.get("message")?.asString else null
    } catch (e: Exception) { null }
    return msg ?: response.body()?.let { (it as? com.example.app.network.GenericResponse)?.message } ?: porDefecto
}
