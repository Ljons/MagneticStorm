package com.magneticstorm.app.data.remote

import com.magneticstorm.app.data.model.KpRecord
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * NOAA повертає JSON як масив масивів: перший рядок — заголовки, далі — дані.
 * Forecast: ["time_tag","kp","observed","noaa_scale"], ["2026-02-04 00:00:00","0.67","observed",null], ...
 * Current: ["time_tag","Kp","a_running","station_count"], ...
 */
interface NoaaApi {

    @GET("noaa-planetary-k-index-forecast.json")
    suspend fun getKpForecast(): Response<List<List<Any?>>>

    @GET("noaa-planetary-k-index.json")
    suspend fun getKpCurrent(): Response<List<List<Any?>>>

    companion object {
        const val BASE_URL = "https://services.swpc.noaa.gov/products/"
    }
}

/** NOAA віддає Kp як Number або String у JSON — парсимо обидва варіанти. */
private fun parseKpValue(value: Any?): Double = when (value) {
    is Number -> value.toDouble()
    is String -> value.toDoubleOrNull() ?: 0.0
    else -> 0.0
}

fun parseForecastResponse(body: List<List<Any?>>): List<KpRecord> {
    if (body.size < 2) return emptyList()
    val list = mutableListOf<KpRecord>()
    for (i in 1 until body.size) {
        val row = body[i]
        if (row.size >= 4) {
            val timeTag = row[0]?.toString()?.replace(".000", "") ?: ""
            val kp = parseKpValue(row[1])
            val type = row[2]?.toString() ?: "observed"
            val noaaScale = (row[3] as? String)?.takeIf { it.isNotBlank() }
            list.add(KpRecord(timeTag = timeTag, kp = kp, type = type, noaaScale = noaaScale))
        }
    }
    return list
}

fun parseCurrentResponse(body: List<List<Any?>>): List<KpRecord> {
    if (body.size < 2) return emptyList()
    val list = mutableListOf<KpRecord>()
    for (i in 1 until body.size) {
        val row = body[i]
        if (row.size >= 2) {
            val timeTag = row[0]?.toString()?.replace(".000", "") ?: ""
            val kp = parseKpValue(row[1])
            list.add(KpRecord(timeTag = timeTag, kp = kp, type = "observed", noaaScale = null))
        }
    }
    return list
}
