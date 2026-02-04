package com.magneticstorm.app.data.repository

import com.magneticstorm.app.data.model.SavedLocation
import com.magneticstorm.app.data.remote.GeocodingApi
import com.magneticstorm.app.data.util.TimezoneByCountry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class LocationRepository {

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("User-Agent", "MagneticStorm/1.0 (Android)")
                .build()
            chain.proceed(request)
        }
        .build()

    private val geocodingApi: GeocodingApi = Retrofit.Builder()
        .baseUrl(GeocodingApi.BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GeocodingApi::class.java)

    suspend fun searchLocation(query: String): Result<List<SavedLocation>> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext Result.success(emptyList())
        try {
            val response = geocodingApi.search(query = query, limit = 8)
            if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code()}"))
            val body = response.body() ?: return@withContext Result.success(emptyList())
            val list = body.map { place ->
                val tzId = TimezoneByCountry.getTimeZoneId(place.address?.country_code)
                SavedLocation(displayName = place.display_name, timeZoneId = tzId)
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
