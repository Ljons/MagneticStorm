package com.magneticstorm.app.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.magneticstorm.app.R
import com.magneticstorm.app.ui.viewmodel.MainViewModel
import android.graphics.Paint

private const val PADDING_LEFT = 56f
private const val PADDING_RIGHT = 24f
private const val PADDING_TOP = 24f
private const val PADDING_BOTTOM = 44f

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt().coerceIn(0, 255),
    (red * 255).toInt().coerceIn(0, 255),
    (green * 255).toInt().coerceIn(0, 255),
    (blue * 255).toInt().coerceIn(0, 255)
)

private fun formatKpValue(kp: Double): String =
    if (kp == kp.toInt().toDouble()) kp.toInt().toString() else "%.1f".format(kp)

@Composable
fun ChartScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsState()
    val data = state.monthChartData
    val location = state.location

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.chart_screen_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "${location.displayName} · поточний місяць",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        if (data.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.chart_no_data),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 16.dp)
            ) {
                LineChart(
                    data = data,
                    dotFillColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun LineChart(
    data: List<Pair<Int, Double>>,
    dotFillColor: Color,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val dotColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val chartLeft = PADDING_LEFT
        val chartRight = width - PADDING_RIGHT
        val chartTop = PADDING_TOP
        val chartBottom = height - PADDING_BOTTOM
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop

        if (chartWidth <= 0 || chartHeight <= 0 || data.isEmpty()) return@Canvas

        val days = data.map { it.first }
        val values = data.map { it.second }
        val minDay = 1
        val maxDay = days.maxOrNull()?.coerceIn(1, 31) ?: 31
        val dayRange = (maxDay - minDay).coerceAtLeast(1)
        val maxKp = (values.maxOrNull() ?: 9.0).coerceAtLeast(1.0)
        val minKp = 0.0

        fun dayToX(day: Int) =
            chartLeft + (day - minDay).toFloat() / dayRange * chartWidth

        fun kpToY(kp: Double) =
            chartBottom - (kp - minKp).toFloat() / (maxKp - minKp).coerceAtLeast(0.1).toFloat() * chartHeight

        // Сітка по вертикалі (Kp) та підписи осей
        val kpSteps = 5
        val labelPaint = Paint().apply {
            color = textColor.toArgb()
            textSize = 11.sp.toPx()
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        val xLabelPaint = Paint().apply {
            color = textColor.toArgb()
            textSize = 11.sp.toPx()
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        for (i in 0..kpSteps) {
            val kp = minKp + (maxKp - minKp) * i / kpSteps
            val y = kpToY(kp).toFloat()
            drawLine(
                color = gridColor,
                start = Offset(chartLeft, y),
                end = Offset(chartRight, y),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
                strokeWidth = 1f
            )
            // Підпис по вертикалі (Kp)
            val kpLabel = formatKpValue(kp)
            val labelY = y - (labelPaint.ascent() + labelPaint.descent()) / 2f
            drawContext.canvas.nativeCanvas.apply {
                drawText(kpLabel, chartLeft - 8f, labelY, labelPaint)
            }
        }

        // Підписи по горизонталі (дні)
        val dayStep = (dayRange / 8).coerceAtLeast(1)
        val daysToShow = (minDay..maxDay).filter { (it - minDay) % dayStep == 0 || it == minDay || it == maxDay }.distinct().sorted()
        for (day in daysToShow) {
            val x = dayToX(day)
            val labelY = chartBottom + 16f - xLabelPaint.descent()
            drawContext.canvas.nativeCanvas.apply {
                drawText(day.toString(), x, labelY, xLabelPaint)
            }
        }

        // Лінія графіка та точки
        val path = Path()
        data.forEachIndexed { index, (day, kp) ->
            val x = dayToX(day)
            val y = kpToY(kp).toFloat()
            if (index == 0) path.moveTo(x, y)
            else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3f)
        )

        // Точки (кружечки)
        data.forEach { (day, kp) ->
            val x = dayToX(day)
            val y = kpToY(kp).toFloat()
            drawCircle(
                color = dotColor,
                radius = 6f,
                center = Offset(x, y)
            )
            drawCircle(
                color = dotFillColor,
                radius = 3f,
                center = Offset(x, y)
            )
        }
    }
}
