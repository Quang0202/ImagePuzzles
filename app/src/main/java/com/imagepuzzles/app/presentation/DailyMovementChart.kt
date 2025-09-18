package com.imagepuzzles.app.presentation

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.io.path.Path
import kotlin.math.abs

// Assumes you already have your data class:
// Assumes you already have your data class:
data class BarDataPoint(val date: Long, val value: Double, val isHighlighted: Boolean = false)

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun BarChartView(
    data: List<BarDataPoint>,
    modifier: Modifier = Modifier
) {
    // ---- Config ----
    val chartHeight = 200.dp              // area for bars & grid
    val xLabelHeight = 24.dp              // area for bottom labels
    val totalHeight = 224.dp              // fixed total height (no outer vertical padding!)
    val yLabelWidth = 44.dp               // reserved width for Y labels (right side)
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 12f))

    // ---- Y range with step 0.5 and clamped drawing to bounds ----
    val values = data.map { it.value }
    val minRaw = values.minOrNull() ?: -1.0
    val maxRaw = values.maxOrNull() ?: 1.0
    val step = 0.5
    val minY = kotlin.math.floor(minRaw / step) * step
    val maxY = kotlin.math.ceil(maxRaw / step) * step
    val yRange = maxY - minY

    // Pre-compute Y ticks as (value, fraction-from-top)
    val ticksCount = (yRange / step).toInt() + 1
    val yTicks: List<Pair<Double, Float>> = List(ticksCount) { i ->
        val v = maxY - i * step
        val frac = ((maxY - v) / yRange).toFloat() // 0..1 from top
        v to frac
    }

    Column(
        modifier = modifier
            .height(totalHeight)
            .fillMaxWidth()
    ) {
        // Top row: Chart area + separate Y-label area (no overlap)
        Row(modifier = Modifier.height(chartHeight).fillMaxWidth()) {
            // Chart canvas (grid + bars)
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                val w = size.width
                val h = size.height

                // Grid lines
                yTicks.forEach { (value, frac) ->
                    val y = h * frac
                    drawLine(
                        color = Color.LightGray,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )
                }

                // Bars
                val barWidth = w / (data.size * 1.5f)
                data.forEachIndexed { index, point ->
                    val xCenter = barWidth * 1.5f * index + barWidth / 2
                    val clamped = point.value.coerceIn(minY, maxY)
                    val topY = h * ((maxY - clamped.coerceAtMost(0.0)) / yRange).toFloat()
                    val bottomY = h * ((maxY - clamped.coerceAtLeast(0.0)) / yRange).toFloat()

                    val color = when {
                        point.isHighlighted -> Color(0xFF7C4DFF)
                        point.value >= 0 -> Color(0xFF00C853)
                        else -> Color(0xFFD32F2F)
                    }

                    drawRoundRect(
                        color = color,
                        topLeft = Offset(xCenter, kotlin.math.min(topY, bottomY)),
                        size = Size(barWidth, kotlin.math.abs(bottomY - topY)),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }
            }

            // Separate canvas for Y labels (no overlap with bars)
            Canvas(
                modifier = Modifier
                    .width(yLabelWidth)
                    .fillMaxHeight()
            ) {
                val h = size.height
                val paint = android.graphics.Paint().apply { textSize = 30f;textAlign = android.graphics.Paint.Align.RIGHT }
                yTicks.forEach { (value, frac) ->
                    val y = h * frac
                    paint.color = when {
                        value > 0 -> android.graphics.Color.parseColor("#00A000")
                        value < 0 -> android.graphics.Color.parseColor("#E53935")
                        else -> android.graphics.Color.BLACK
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        String.format("%+.1f%%", value),
                        yLabelWidth.toPx(),
                        y + 10f,
                        paint
                    )
                }
            }
        }

        // Bottom X-axis labels (dates)
        Row(
            modifier = Modifier
                .height(xLabelHeight)
                .fillMaxWidth()
                .padding(top = 8.dp)
                .padding(end = 45.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val formatter = DateTimeFormatter.ofPattern("d/M")
            val indices = if (data.size <= 6) data.indices.toList() else listOf(0, data.size/4, data.size/2, (data.size*3)/4, data.lastIndex)
            indices.forEach { idx ->
                val point = data[idx]
                val label = Instant.ofEpochMilli(point.date)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .format(formatter)
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
// DEMO

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BarChartDemo() {
    val now = System.currentTimeMillis()
    val oneDay = 24 * 60 * 60 * 1000L

    val sampleData = List(10) { i ->
        val date = now - (9 - i) * oneDay
        val value = listOf(-1.2, -0.6, 0.3, 0.8, -0.4, 1.1, -1.5, -0.3, 0.6, 1.0)[i]
        BarDataPoint(date = date, value = value, isHighlighted = false)
    }
    MaterialTheme {
        Box(Modifier.fillMaxSize().padding(16.dp)) {
            BarChartView(data = sampleData)
        }
    }
}
