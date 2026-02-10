package com.magneticstorm.app.data.util

import java.util.Locale

/** Єдине форматування Kp для віджета, екрану та сповіщень (одна десяткова). */
fun formatKp(kp: Double): String = String.format(Locale.US, "%.1f", kp)
