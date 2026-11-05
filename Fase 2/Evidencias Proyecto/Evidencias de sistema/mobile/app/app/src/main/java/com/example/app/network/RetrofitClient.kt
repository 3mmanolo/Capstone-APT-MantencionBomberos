package com.example.app.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // URL fija del túnel público HTTPS (funciona desde cualquier red 4G/5G/Wi-Fi):
    const val BASE_URL = "https://bomberos-melipilla.loca.lt/"

    // Variable global para guardar el Token de autenticación de la sesión
    var authToken: String? = null

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val requestBuilder = originalRequest.newBuilder()
            
            authToken?.let { token ->
                if (token.isNotBlank()) {
                    requestBuilder.addHeader("Authorization", "Token $token")
                }
            }
            
            chain.proceed(requestBuilder.build())
        }
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
