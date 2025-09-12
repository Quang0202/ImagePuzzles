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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.io.path.Path

@Composable
fun CompareLineChartFixedHeight(
    data1: List<Float>,
    data2: List<Float>,
    timeLabels: List<String> = listOf("13:00", "14:00", "15:00", "16:00", "17:00")
) {
    val maxY = 2f
    val minY = -2f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row {
            // 🎯 Biểu đồ với height cố định 224.dp
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(224.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val spacing = size.width / (data1.size - 1)
                    val height = size.height
                    val width = size.width

                    // 🧱 Grid ngang nét đứt
                    val ySteps = listOf(2f, 1f, 0f, -1f, -2f)
                    ySteps.forEach { step ->
                        val y = height * (1 - (step - minY) / (maxY - minY))
                        drawLine(
                            color = Color.LightGray,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                        )
                    }

                    // 🌄 Gradient Line 1 → bottom (trong Canvas, không đè timeLabels)
                    val points1 = data1.mapIndexed { index, value ->
                        val x = index * spacing
                        val y = height * (1 - (value - minY) / (maxY - minY))
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

                    // 🌄 Gradient Line 2 → bottom
                    val points2 = data2.mapIndexed { index, value ->
                        val x = index * spacing
                        val y = height * (1 - (value - minY) / (maxY - minY))
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

                    // 📈 Line 1
                    drawPath(
                        path = Path().apply {
                            moveTo(points1.first().x, points1.first().y)
                            for (point in points1.drop(1)) lineTo(point.x, point.y)
                        },
                        color = Color(0xFFFF9900),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 📈 Line 2
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

            // 🧾 Y Label ngoài cùng bên phải
            Column(
                modifier = Modifier
                    .height(224.dp)
                    .padding(start = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("+2.00%", "+1.00%", "0.00%", "-1.00%", "-2.00%").forEach {
                    Text(
                        text = it,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ⏰ Time Labels nằm dưới ngoài canvas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 32.dp), // chừa phần Y Label
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
    val symbol2Changes = listOf(0f, -0.3f, -0.2f, -0.5f, -0.7f, -1.1f, -1.0f)

    MaterialTheme {
        Box(Modifier.fillMaxSize().padding(16.dp).padding(top = 20.dp)) {
            CompareLineChartFixedHeight(data1 = symbol1Changes, data2 = symbol2Changes)
        }
    }
}
