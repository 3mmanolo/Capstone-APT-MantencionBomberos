package com.example.app.network

import android.content.Context
import android.content.SharedPreferences
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val PREFS_NAME = "retrofit_prefs"
    private const val KEY_SERVER_URL = "server_url"
    const val DEFAULT_EMULATOR_URL = "http://10.0.2.2:8080/"
    const val DEFAULT_LOCAL_URL = "http://10.202.157.203:8080/"

    private var sharedPreferences: SharedPreferences? = null

    var baseUrl: String = DEFAULT_LOCAL_URL
        private set

    // Variable global para guardar el Token de autenticación de la sesión
    var authToken: String? = null

    fun init(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences = prefs
        val savedUrl = prefs.getString(KEY_SERVER_URL, null)
        val initialUrl = when {
            !savedUrl.isNullOrBlank() -> savedUrl
            isEmulator() -> DEFAULT_EMULATOR_URL
            else -> DEFAULT_LOCAL_URL
        }
        updateBaseUrl(initialUrl, saveToPrefs = false)
    }

    fun updateBaseUrl(newUrl: String, saveToPrefs: Boolean = true) {
        val formatted = formatUrl(newUrl)
        baseUrl = formatted
        if (saveToPrefs) {
            sharedPreferences?.edit()?.putString(KEY_SERVER_URL, formatted)?.apply()
        }
        rebuildApiService()
    }

    fun formatUrl(url: String): String {
        var trimmed = url.trim()
        if (trimmed.isEmpty()) return DEFAULT_EMULATOR_URL
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "http://$trimmed"
        }
        if (!trimmed.endsWith("/")) {
            trimmed = "$trimmed/"
        }
        return trimmed
    }

    private fun isEmulator(): Boolean {
        return (android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
                || "google_sdk" == android.os.Build.PRODUCT)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
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
    }

    private var _apiService: ApiService = createRetrofit(baseUrl).create(ApiService::class.java)

    val apiService: ApiService
        get() = _apiService

    private fun rebuildApiService() {
        _apiService = createRetrofit(baseUrl).create(ApiService::class.java)
    }

    private fun createRetrofit(url: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(url)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}

