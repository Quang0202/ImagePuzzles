package com.imagepuzzles.app.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.io.path.Path

@Composable
fun CompareLineChartDynamic(
    data1: List<Float>,
    data2: List<Float>,
    timeLabels: List<String> = listOf("13:00", "14:00", "15:00", "16:00", "17:00")
) {
    val density = LocalDensity.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(224.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val spacing = size.width / (data1.size - 1)
                    val height = size.height
                    val width = size.width

                    // 📊 Tính yRange động từ data
                    val flatValues = (data1 + data2)
                    val minValue = flatValues.minOrNull() ?: -1f
                    val maxValue = flatValues.maxOrNull() ?: 1f
                    var adjustedMinY = minOf(minValue, -1f)
                    var adjustedMaxY = maxOf(maxValue, 1f)
                    val yRange = adjustedMaxY - adjustedMinY

                    val steps = 5
                    repeat(steps) { i ->
                        val ratio = i / (steps - 1).toFloat()
                        val y = height * ratio
                        val value = adjustedMaxY - ratio * yRange

                        // 🔹 Vẽ baseline nét đứt
                        drawLine(
                            color = Color.LightGray,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                        )

                        // 🔹 Vẽ yLabel bằng nativeCanvas
                        drawContext.canvas.nativeCanvas.drawText(
                            String.format("%+.2f", value) + "%",
                            width + 8.dp.toPx(),
                            y + 10,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.GRAY
                                textSize = 30f
                            }
                        )
                    }

                    // 🌄 Vẽ gradient từ line1 → bottom
                    val points1 = data1.mapIndexed { index, value ->
                        val x = index * spacing
                        val y = height * ((adjustedMaxY - value) / yRange)
                        Offset(x, y)
                    }
                    val path1 = Path().apply {
                        moveTo(points1.first().x, points1.first().y)
                        for (point in points1.drop(1)) lineTo(point.x, point.y)
                        lineTo(points1.last().x, height)
                        lineTo(points1.first().x, height)
                        close()
                    }
                    drawPath(
                        path = path1,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0x33FF9900), Color.Transparent)
                        )
                    )

                    // 🌄 Vẽ gradient từ line2 → bottom
                    val points2 = data2.mapIndexed { index, value ->
                        val x = index * spacing
                        val y = height * ((adjustedMaxY - value) / yRange)
                        Offset(x, y)
                    }
                    val path2 = Path().apply {
                        moveTo(points2.first().x, points2.first().y)
                        for (point in points2.drop(1)) lineTo(point.x, point.y)
                        lineTo(points2.last().x, height)
                        lineTo(points2.first().x, height)
                        close()
                    }
                    drawPath(
                        path = path2,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0x33000000), Color.Transparent)
                        )
                    )

                    // 📈 Vẽ line 1
                    drawPath(
                        path = Path().apply {
                            moveTo(points1.first().x, points1.first().y)
                            for (point in points1.drop(1)) lineTo(point.x, point.y)
                        },
                        color = Color(0xFFFF9900),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 📈 Vẽ line 2
                    drawPath(
                        path = Path().apply {
                            moveTo(points2.first().x, points2.first().y)
                            for (point in points2.drop(1)) lineTo(point.x, point.y)
                        },
                        color = Color.Gray,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ⏰ Time labels (ngoài cùng dưới)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            timeLabels.forEach {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun CompareLineChartPreview() {
    val symbol1Changes = listOf(1.3f, 1.2f, 1.1f, 1.4f, 2.0f, 1.7f, 1.6f) // ví dụ
    val symbol2Changes = listOf(0f, -0.3f, -0.2f, -0.5f, -0.7f, -0.9f, -0.6f)

    MaterialTheme {
        Box(Modifier.fillMaxSize().padding(16.dp).padding(top = 20.dp)) {
            CompareLineChartDynamic(data1 = symbol1Changes, data2 = symbol2Changes)
        }
    }
}
