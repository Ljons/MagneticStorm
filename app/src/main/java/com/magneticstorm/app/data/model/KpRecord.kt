package com.magneticstorm.app.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Один запис Kp-індексу (3-годинний інтервал).
 * @param timeTag UTC час у форматі "yyyy-MM-dd HH:mm:ss"
 * @param kp значення Kp (0–9)
 * @param type observed / estimated / predicted
 * @param noaaScale G1, G2, G3, G4, G5 або null
 */
data class KpRecord(
    val timeTag: String,
    val kp: Double,
    val type: String,
    val noaaScale: String?
) {
    /** Час у заданій таймзоні для відображення */
    fun formatTime(timeZoneId: String, pattern: String = "dd-MM-yyyy, HH:mm"): String {
        return try {
            val utc = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply { this.timeZone = TimeZone.getTimeZone("UTC") }
            val date = utc.parse(timeTag.take(19)) ?: return timeTag
            val out = SimpleDateFormat(pattern, Locale.getDefault()).apply { this.timeZone = TimeZone.getTimeZone(timeZoneId) }
            out.format(date)
        } catch (_: Exception) {
            timeTag
        }
    }

    fun dateOnly(timeZoneId: String): String = formatTime(timeZoneId, "dd-MM-yyyy")

    /** Час запису в UTC у мілісекундах (для порівняння з поточним часом). */
    fun timeUtcMillis(): Long? = try {
        val utc = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        utc.parse(timeTag.take(19))?.time
    } catch (_: Exception) {
        null
    }
}

/** Рівень бурі за шкалою NOAA (G1–G5) та опис */
object KpScale {
    const val G1 = "G1"
    const val G2 = "G2"
    const val G3 = "G3"
    const val G4 = "G4"
    const val G5 = "G5"

    fun label(scale: String?): String = when (scale) {
        G1 -> "G1 (Слаба)"
        G2 -> "G2 (Помірна)"
        G3 -> "G3 (Сильна)"
        G4 -> "G4 (Дуже сильна)"
        G5 -> "G5 (Екстремальна)"
        else -> ""
    }

    /** Колір/категорія за Kp для UI */
    fun category(kp: Double): StormCategory = when {
        kp >= 9 -> StormCategory.EXTREME
        kp >= 7 -> StormCategory.SEVERE
        kp >= 6 -> StormCategory.STRONG
        kp >= 5 -> StormCategory.MODERATE
        kp >= 4 -> StormCategory.MINOR
        else -> StormCategory.QUIET
    }
}

enum class StormCategory(val labelKey: String, val emoji: String) {
    QUIET("quiet", "🟢"),
    MINOR("minor", "🟡"),
    MODERATE("moderate", "🟠"),
    STRONG("strong", "🔴"),
    SEVERE("severe", "🟣"),
    EXTREME("extreme", "⚫")
}
