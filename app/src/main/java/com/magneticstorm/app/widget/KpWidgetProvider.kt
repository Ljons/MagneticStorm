package com.magneticstorm.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.magneticstorm.app.MainActivity
import com.magneticstorm.app.R
import com.magneticstorm.app.data.model.KpScale
import com.magneticstorm.app.data.model.StormCategory
import com.magneticstorm.app.data.util.formatKp
import java.util.Locale

class KpWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateAppWidget(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        private val CIRCLE_DRAWABLE_IDS = intArrayOf(
            R.drawable.widget_kp_circle_quiet,
            R.drawable.widget_kp_circle_minor,
            R.drawable.widget_kp_circle_moderate,
            R.drawable.widget_kp_circle_strong,
            R.drawable.widget_kp_circle_severe,
            R.drawable.widget_kp_circle_extreme
        )

        private val SLOT_COLORS = intArrayOf(
            0xFF2E7D32.toInt(), 0xFFF9A825.toInt(), 0xFFEF6C00.toInt(),
            0xFFC62828.toInt(), 0xFF7B1FA2.toInt(), 0xFF212121.toInt()
        )

        private fun categoryToColorOrdinal(kp: Double): Int =
            KpScale.category(kp).ordinal.coerceIn(0, 5)

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val prefs = WidgetPreferences(context)
            val state = prefs.getCardState()

            for (id in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_kp)
                if (state != null) {
                    applyCardState(context, views, state)
                } else {
                    applyLegacyState(context, views, prefs.getLastKp(), prefs.getLastKpTimeMillis())
                }
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pending = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, pending)
                appWidgetManager.updateAppWidget(id, views)
            }
        }

        private fun applyCardState(context: Context, views: RemoteViews, state: WidgetCardState) {
            views.setTextViewText(R.id.widget_title, context.getString(R.string.today_level))
            views.setTextViewText(R.id.widget_date, state.dateStr)
            views.setTextViewText(R.id.widget_location, state.locationDisplayName.ifEmpty { " " })
            views.setTextViewText(R.id.widget_circle_kp, formatKp(state.currentKp))
            views.setTextViewText(R.id.widget_kp_text, "Kp ${formatKp(state.currentKp)}")
            views.setTextViewText(R.id.widget_time, state.currentTimeStr)

            val circleResId = CIRCLE_DRAWABLE_IDS.getOrElse(state.circleCategoryOrdinal.coerceIn(0, 5)) {
                R.drawable.widget_kp_circle_quiet
            }
            views.setInt(R.id.widget_circle, "setBackgroundResource", circleResId)

            val slotIds = listOf(
                R.id.widget_slot_0, R.id.widget_slot_1, R.id.widget_slot_2, R.id.widget_slot_3,
                R.id.widget_slot_4, R.id.widget_slot_5, R.id.widget_slot_6, R.id.widget_slot_7
            )
            val timeIds = listOf(
                R.id.widget_time_0, R.id.widget_time_1, R.id.widget_time_2, R.id.widget_time_3,
                R.id.widget_time_4, R.id.widget_time_5, R.id.widget_time_6, R.id.widget_time_7
            )
            state.slots.take(8).forEachIndexed { i, (kp, time) ->
                if (kp != null && time != null) {
                    views.setTextViewText(slotIds[i], formatKp(kp))
                    views.setInt(slotIds[i], "setBackgroundColor", SLOT_COLORS.getOrElse(categoryToColorOrdinal(kp)) { SLOT_COLORS[0] })
                    views.setTextViewText(timeIds[i], time)
                    views.setViewVisibility(slotIds[i], View.VISIBLE)
                    views.setViewVisibility(timeIds[i], View.VISIBLE)
                } else {
                    views.setTextViewText(slotIds[i], "—")
                    views.setInt(slotIds[i], "setBackgroundResource", R.drawable.widget_slot_bg)
                    views.setTextViewText(timeIds[i], "—")
                    views.setViewVisibility(slotIds[i], View.VISIBLE)
                    views.setViewVisibility(timeIds[i], View.VISIBLE)
                }
            }
            for (i in state.slots.size until 8) {
                views.setTextViewText(slotIds[i], "—")
                views.setInt(slotIds[i], "setBackgroundResource", R.drawable.widget_slot_bg)
                views.setTextViewText(timeIds[i], "—")
            }
        }

        private fun applyLegacyState(
            context: Context,
            views: RemoteViews,
            kp: Double,
            timeMillis: Long
        ) {
            val kpText = if (timeMillis > 0) formatKp(kp) else "—"
            val timeText = if (timeMillis > 0) {
                java.text.SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(java.util.Date(timeMillis))
            } else ""
            views.setTextViewText(R.id.widget_title, context.getString(R.string.today_level))
            views.setTextViewText(R.id.widget_date, "")
            views.setTextViewText(R.id.widget_location, "")
            views.setTextViewText(R.id.widget_circle_kp, kpText)
            views.setTextViewText(R.id.widget_kp_text, "Kp $kpText")
            views.setTextViewText(R.id.widget_time, timeText)
            views.setInt(R.id.widget_circle, "setBackgroundResource", CIRCLE_DRAWABLE_IDS[categoryToColorOrdinal(kp).coerceIn(0, 5)])
            listOf(R.id.widget_slot_0, R.id.widget_slot_1, R.id.widget_slot_2, R.id.widget_slot_3,
                R.id.widget_slot_4, R.id.widget_slot_5, R.id.widget_slot_6, R.id.widget_slot_7).forEach { id ->
                views.setTextViewText(id, "—")
                views.setInt(id, "setBackgroundResource", R.drawable.widget_slot_bg)
            }
            listOf(R.id.widget_time_0, R.id.widget_time_1, R.id.widget_time_2, R.id.widget_time_3,
                R.id.widget_time_4, R.id.widget_time_5, R.id.widget_time_6, R.id.widget_time_7).forEach { id ->
                views.setTextViewText(id, "—")
            }
        }
    }
}
