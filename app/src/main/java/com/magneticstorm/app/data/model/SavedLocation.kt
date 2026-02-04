package com.magneticstorm.app.data.model

/**
 * Збережена локація користувача (місто + таймзона для відображення дат).
 */
data class SavedLocation(
    val displayName: String,
    val timeZoneId: String
) {
    companion object {
        val DEFAULT = SavedLocation(
            displayName = "Київ, Україна",
            timeZoneId = "Europe/Kyiv"
        )
    }
}
