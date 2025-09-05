package com.imagepuzzles.app.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun LineChartView() {
    val dataLines = listOf(
        listOf(0.6f, 0.55f, 0.58f, 0.57f, 0.6f, 0.7f, 0.65f, 0.8f, 0.7f, 0.68f),     // orange
        listOf(0.2f, 0.25f, 0.3f, 0.35f, 0.3f, 0.28f, 0.32f, 0.3f, 0.31f, 0.29f),     // purple
        listOf(0.0f, 0.05f, -0.05f, 0.02f, -0.08f, -0.1f, -0.12f, -0.15f, -0.18f, -0.16f), // blue
        listOf(-0.1f, 0.15f, -0.12f, -0.2f, -0.8f, -0.25f, -0.3f, -0.7f, -0.4f, -0.38f) // pink
    )

    val colors = listOf(
        Color(0xFFFF6D00), // orange
        Color(0xFF9C4DCC), // purple
        Color(0xFF00B0FF), // blue
        Color(0xFFF06292)  // pink
    )

    val flatData = dataLines.flatten()
    val actualMinY = flatData.minOrNull() ?: 0f
    val actualMaxY = flatData.maxOrNull() ?: 1f

// Start with fixed bounds
    var adjustedMinY = -1f
    var adjustedMaxY = 1f

// Only expand if data exceeds
    if (actualMinY < adjustedMinY) adjustedMinY = actualMinY
    if (actualMaxY > adjustedMaxY) adjustedMaxY = actualMaxY

    val yRange = adjustedMaxY - adjustedMinY

    val density = LocalDensity.current
    val labelPadding = with(density){4.dp.toPx()}

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.White)
        ) {
            val chartHeight = size.height
            val chartWidth = size.width - 80f - labelPadding

            // Grid lines (auto scale 5 levels)
            val steps = 5
            repeat(steps) { i ->
                val ratio = i.toFloat() / (steps - 1)
                val y = chartHeight * ratio
                val value = adjustedMaxY - ratio * yRange

                drawLine(
                    color = Color.LightGray,
                    start = Offset(0f, y),
                    end = Offset(chartWidth, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 12f))
                )

                drawContext.canvas.nativeCanvas.drawText(
                    "${String.format("%+.2f", value)}%",
                    chartWidth + labelPadding,
                    y + 10,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 30f
                    }
                )
            }

            // Draw each data line
            dataLines.forEachIndexed { index, data ->
                val path = Path()
                data.forEachIndexed { i, value ->
                    val x = chartWidth / (data.size - 1) * i
                    val y = chartHeight * ((adjustedMaxY - value) / yRange)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = colors[index],
                    style = Stroke(width = 4f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Timeframe selector
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            val timeFrames = listOf("1D", "1W", "1M", "3M", "6M", "1Y", "All")
            timeFrames.forEach { frame ->
                val isSelected = frame == "1D"
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .background(
                            if (isSelected) Color(0xFFFCEFD3) else Color(0xFFF2F2F2),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = frame,
                        color = if (isSelected) Color(0xFFB58900) else Color.Gray
                    )
                }
            }
        }
    }
}
