package com.imagepuzzles.app.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun BarChart(
    data: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier
) {
    val barWidth = 20.dp
    val spacing = 20.dp
    val chartHeight = 200.dp
    val baseLineCount = 4

    Column {
        Box(modifier = modifier.height(chartHeight + 40.dp)) {

            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .padding(bottom = 40.dp)
            ) {
                val lineSpacing = size.height / baseLineCount
                repeat(baseLineCount + 1) { i ->
                    val y = i * lineSpacing
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
                        strokeWidth = 2f
                    )
                }
            }

            // 2. Vẽ từng cột
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEachIndexed { index, value ->
                    val isHighlighted = index == data.lastIndex
                    val filledRatio = value.coerceIn(0f, 1f)
                    val filledHeight = chartHeight * filledRatio

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        // Cột mờ full height
                        Box(
                            modifier = Modifier
                                .width(barWidth)
                                .height(chartHeight)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(barWidth)
                                    .height(chartHeight)
                                    .background(
                                        color = Color.White,
                                        shape = RoundedCornerShape(50)
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .width(barWidth)
                                    .height(chartHeight)
                                    .background(
                                        color = Color.LightGray.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(50)
                                    )
                            ) {
                                // Phần dữ liệu thật (đè lên)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(filledHeight)
                                        .align(Alignment.BottomCenter)
                                        .background(
                                            brush = if (isHighlighted) {
                                                SolidColor(Color(0xFFD0933F))
                                            } else {
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.LightGray.copy(alpha = 0.2f),
                                                        Color.Gray
                                                    )
                                                )
                                            },
                                            shape = RoundedCornerShape(50)
                                        )
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = labels[index],
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun BarChartPreview() {
    val hours = listOf("13:00", "14:00", "15:00", "16:00", "17:00", "18:00")
    val values = listOf(0.3f, 0.7f, 0.6f, 0.6f, 0.7f, 0.9f) // Normalized 0..1

    MaterialTheme {
        Box(Modifier.fillMaxSize().padding(16.dp).padding(top = 20.dp)) {
            BarChart(
                data = values,
                labels = hours,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}