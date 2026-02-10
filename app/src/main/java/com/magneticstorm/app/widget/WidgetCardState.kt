package com.magneticstorm.app.widget

/**
 * Повний стан для віджета «Рівень на сьогодні» (як у картці в додатку).
 * @param dateStr дата у форматі dd-MM-yyyy
 * @param locationDisplayName назва обраної локації (напр. «Київ, Україна»)
 * @param currentKp поточне значення Kp
 * @param currentTimeStr час у форматі HH:mm
 * @param circleCategoryOrdinal StormCategory.ordinal для кольору кола (0–5)
 * @param slots 8 слотів: пара (Kp або null, час або null)
 */
data class WidgetCardState(
    val dateStr: String,
    val locationDisplayName: String,
    val currentKp: Double,
    val currentTimeStr: String,
    val circleCategoryOrdinal: Int,
    val slots: List<Pair<Double?, String?>>
) {
    init {
        require(slots.size <= 8) { "slots size must be <= 8" }
    }
}
