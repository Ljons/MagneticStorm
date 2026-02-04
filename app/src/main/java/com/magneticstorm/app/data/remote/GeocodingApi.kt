package com.magneticstorm.app.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * OpenStreetMap Nominatim — безкоштовний геокодінг.
 * Повертає lat, lon та display_name; таймзону отримаємо з timezoneapi або виведемо з координат на клієнті.
 */
data class NominatimPlace(
    val lat: String,
    val lon: String,
    val display_name: String,
    val type: String?,
    val address: NominatimAddress?
)

data class NominatimAddress(
    val country_code: String?,
    val state: String?,
    val city: String?,
    val town: String?,
    val village: String?
)

interface GeocodingApi {

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 5,
        @Query("addressdetails") addressdetails: Int = 1
    ): Response<List<NominatimPlace>>

    companion object {
        const val BASE_URL = "https://nominatim.openstreetmap.org/"
    }
}
