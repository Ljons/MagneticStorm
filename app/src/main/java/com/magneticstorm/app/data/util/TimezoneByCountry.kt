package com.magneticstorm.app.data.util

import java.util.TimeZone

/**
 * Приблизний часовий пояс за кодом країни (ISO 3166-1 alpha-2).
 * Використовується після геокоду, коли окремого API для timezone немає.
 */
object TimezoneByCountry {

    private val countryToZone: Map<String, String> = mapOf(
        "ua" to "Europe/Kyiv",
        "pl" to "Europe/Warsaw",
        "de" to "Europe/Berlin",
        "fr" to "Europe/Paris",
        "gb" to "Europe/London",
        "uk" to "Europe/London",
        "it" to "Europe/Rome",
        "es" to "Europe/Madrid",
        "us" to "America/New_York",
        "ca" to "America/Toronto",
        "ru" to "Europe/Moscow",
        "by" to "Europe/Minsk",
        "md" to "Europe/Chisinau",
        "ro" to "Europe/Bucharest",
        "hu" to "Europe/Budapest",
        "sk" to "Europe/Bratislava",
        "cz" to "Europe/Prague",
        "at" to "Europe/Vienna",
        "ch" to "Europe/Zurich",
        "nl" to "Europe/Amsterdam",
        "be" to "Europe/Brussels",
        "tr" to "Europe/Istanbul",
        "gr" to "Europe/Athens",
        "bg" to "Europe/Sofia",
        "lt" to "Europe/Vilnius",
        "lv" to "Europe/Riga",
        "ee" to "Europe/Tallinn",
        "fi" to "Europe/Helsinki",
        "se" to "Europe/Stockholm",
        "no" to "Europe/Oslo",
        "jp" to "Asia/Tokyo",
        "cn" to "Asia/Shanghai",
        "in" to "Asia/Kolkata",
        "au" to "Australia/Sydney",
        "br" to "America/Sao_Paulo",
        "mx" to "America/Mexico_City",
        "ar" to "America/Argentina/Buenos_Aires",
        "il" to "Asia/Jerusalem",
        "eg" to "Africa/Cairo",
        "za" to "Africa/Johannesburg",
        "kz" to "Asia/Almaty",
        "ge" to "Asia/Tbilisi",
        "am" to "Asia/Yerevan",
        "az" to "Asia/Baku",
    )

    fun getTimeZoneId(countryCode: String?): String {
        if (countryCode.isNullOrBlank()) return TimeZone.getDefault().id
        return countryToZone[countryCode.lowercase()] ?: TimeZone.getDefault().id
    }
}
