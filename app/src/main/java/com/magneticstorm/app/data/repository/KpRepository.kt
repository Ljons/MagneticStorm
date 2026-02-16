package com.magneticstorm.app.data.repository

import com.magneticstorm.app.data.model.KpRecord
import com.magneticstorm.app.data.remote.NoaaApi
import com.magneticstorm.app.data.remote.parseCurrentResponse
import com.magneticstorm.app.data.remote.parseForecastResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.TimeZone

class KpRepository {

    private val noaaApi: NoaaApi = Retrofit.Builder()
        .baseUrl(NoaaApi.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(NoaaApi::class.java)

    suspend fun getForecast(): Result<List<KpRecord>> = withContext(Dispatchers.IO) {
        try {
            val response = noaaApi.getKpForecast()
            if (response.isSuccessful) {
                val body = response.body() ?: return@withContext Result.failure(Exception("Empty body"))
                Result.success(parseForecastResponse(body))
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrent(): Result<List<KpRecord>> = withContext(Dispatchers.IO) {
        try {
            val response = noaaApi.getKpCurrent()
            if (response.isSuccessful) {
                val body = response.body() ?: return@withContext Result.failure(Exception("Empty body"))
                Result.success(parseCurrentResponse(body))
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * «Сьогодні» у заданій таймзоні (yyyy-MM-dd).
     */
    fun getTodayString(timeZoneId: String): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply { timeZone = TimeZone.getTimeZone(timeZoneId) }
        return sdf.format(System.currentTimeMillis())
    }

    /**
     * Групує записи по днях у заданій таймзоні. Якщо задано todayStr — повертає тільки дні в діапазоні [today−daysBack, today+daysForward].
     */
    fun groupByDay(
        records: List<KpRecord>,
        timeZoneId: String,
        todayStr: String? = null,
        daysBack: Int = 4,
        daysForward: Int = 3
    ): Map<String, List<KpRecord>> {
        val utc = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val out = SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply { timeZone = TimeZone.getTimeZone(timeZoneId) }
        val byDay = records.groupBy { record ->
            try {
                val date = utc.parse(record.timeTag.take(19)) ?: record.timeTag.take(10)
                out.format(date)
            } catch (_: Exception) {
                record.timeTag.take(10)
            }
        }
        val keys = if (todayStr != null) {
            val cal = Calendar.getInstance(TimeZone.getTimeZone(timeZoneId)).apply {
                val (y, m, d) = todayStr.split("-").map { it.toIntOrNull() ?: 0 }
                set(y, m - 1, d)
            }
            val start = Calendar.getInstance(TimeZone.getTimeZone(timeZoneId)).apply {
                timeInMillis = cal.timeInMillis
                add(Calendar.DAY_OF_MONTH, -daysBack)
            }
            val end = Calendar.getInstance(TimeZone.getTimeZone(timeZoneId)).apply {
                timeInMillis = cal.timeInMillis
                add(Calendar.DAY_OF_MONTH, daysForward)
            }
            val sdf = SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val startStr = sdf.format(start.time)
            val endStr = sdf.format(end.time)
            byDay.keys.filter { it in startStr..endStr }.sorted()
        } else {
            byDay.keys.sorted()
        }
        return keys.associateWith { byDay[it]!! }
    }

    /**
     * Запис Kp, найближчий до поточного моменту і не пізніший за нього (UTC).
     * Для блоку «Рівень на сьогодні» — щоб дата відповідала справжньому «сьогодні».
     */
    fun currentKpForNow(records: List<KpRecord>): KpRecord? {
        val nowUtc = System.currentTimeMillis()
        return records
            .mapNotNull { r -> r.timeUtcMillis()?.let { ms -> r to ms } }
            .filter { (_, ms) -> ms <= nowUtc }
            .maxByOrNull { it.second }
            ?.first
            ?: records.minByOrNull { it.timeTag } // якщо всі в майбутньому — показуємо найраніший
    }

    /**
     * Середній Kp по днях поточного місяця в заданій таймзоні.
     * Повертає список (день місяця 1..31, середній Kp), відсортований за днем.
     */
    fun getCurrentMonthDailyAverages(
        records: List<KpRecord>,
        timeZoneId: String
    ): List<Pair<Int, Double>> {
        val tz = TimeZone.getTimeZone(timeZoneId)
        val out = SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply { timeZone = tz }
        val utc = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val utcDateOnly = SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val nowCal = Calendar.getInstance(tz)
        val currentYear = nowCal.get(Calendar.YEAR)
        val currentMonth = nowCal.get(Calendar.MONTH)
        val byDayStr = records.groupBy { record ->
            try {
                val date = utc.parse(record.timeTag.take(19))
                if (date != null) out.format(date)
                else utcDateOnly.parse(record.timeTag.take(10))?.let { out.format(it) } ?: record.timeTag.take(10)
            } catch (_: Exception) {
                utcDateOnly.parse(record.timeTag.take(10))?.let { out.format(it) } ?: record.timeTag.take(10)
            }
        }
        return byDayStr
            .filter { (key, _) ->
                val parts = key.split("-")
                if (parts.size != 3) return@filter false
                val y = parts[0].toIntOrNull() ?: return@filter false
                val m = parts[1].toIntOrNull() ?: return@filter false
                y == currentYear && (m - 1) == currentMonth
            }
            .map { (key, dayRecords) ->
                val dayOfMonth = key.split("-").getOrNull(2)?.toIntOrNull() ?: 0
                val kpValues = dayRecords.map { it.kp }.filter { it > 0 }
                val avg = if (kpValues.isEmpty()) dayRecords.map { it.kp }.average() else kpValues.average()
                dayOfMonth to avg
            }
            .filter { it.first in 1..31 }
            .sortedBy { it.first }
    }
}
