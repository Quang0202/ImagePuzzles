package com.imagepuzzles.app.presentation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import java.time.Instant
import java.time.ZoneId

enum class TimeFrame(val label: String, val days: Long?) {
    ONE_DAY("1D", 1),
    ONE_WEEK("1W", 7),
    ONE_MONTH("1M", 30),
    THREE_MONTHS("3M", 90),
    SIX_MONTHS("6M", 180),
    ONE_YEAR("1Y", 365),
    ALL("All", null)
}
data class LineDataPoint(val date: Long, val value: Double)

@Composable
fun MultiLineChartView(
    getFilteredData: (TimeFrame) -> List<List<LineDataPoint>>,
    modifier: Modifier = Modifier
) {
    var selectedTimeFrame by remember { mutableStateOf(TimeFrame.ONE_DAY) }

    // Get data based on selected tab
    val filteredData = getFilteredData(selectedTimeFrame)

    // Extract value ranges
    val flatValues = filteredData.flatten().map { it.value }
    val minY = flatValues.minOrNull() ?: -1.0
    val maxY = flatValues.maxOrNull() ?: 1.0

    var adjustedMinY = -1.0
    var adjustedMaxY = 1.0
    if (minY < adjustedMinY) adjustedMinY = minY
    if (maxY > adjustedMaxY) adjustedMaxY = maxY

    val yRange = adjustedMaxY - adjustedMinY
    val density = LocalDensity.current
    val labelPadding = with(density) { 4.dp.toPx()}

    Column(modifier = modifier.padding(16.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            val chartHeight = size.height
            val chartWidth = size.width - 80f - labelPadding

            // Draw grid and labels
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

            // Draw data lines
            val colors = listOf(Color.Red, Color.Blue, Color.Green, Color.Magenta)
            filteredData.forEachIndexed { index, line ->
                if (line.size < 2) return@forEachIndexed
                val sorted = line.sortedBy { it.date }
                val first = sorted.first().date
                val last = sorted.last().date
                val range = (last - first).toFloat()

                val path = Path()
                sorted.forEachIndexed { i, point ->
                    val x = chartWidth * ((point.date - first) / range)
                    val y = chartHeight * ((adjustedMaxY - point.value) / yRange).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(path, color = colors[index % colors.size], style = Stroke(3f))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // TimeFrame selection tabs
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            TimeFrame.values().forEach { frame ->
                val isSelected = selectedTimeFrame == frame
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .background(
                            if (isSelected) Color(0xFFFCEFD3) else Color(0xFFF2F2F2),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedTimeFrame = frame }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = frame.label,
                        color = if (isSelected) Color(0xFFB58900) else Color.Gray
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun filterDataByTimeFrame(
    allData: List<List<LineDataPoint>>,
    timeFrame: TimeFrame
): List<List<LineDataPoint>> {
    val grouped: (Long) -> Any = when (timeFrame) {
        TimeFrame.ONE_DAY -> { time -> time / (24 * 60 * 60 * 1000) } // by day
        TimeFrame.ONE_WEEK -> { time -> time / (7 * 24 * 60 * 60 * 1000) } // by week
        TimeFrame.ONE_MONTH -> { time ->
            val date = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate()
            date.year * 100 + date.monthValue  // e.g., 202409
        }
        TimeFrame.THREE_MONTHS -> { time ->
            val date = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate()
            val quarter = (date.monthValue - 1) / 3 + 1
            date.year * 10 + quarter  // e.g., 20243 (Q3)
        }
        TimeFrame.SIX_MONTHS, TimeFrame.ONE_YEAR, TimeFrame.ALL -> { time ->
            val date = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate()
            date.year  // aggregate by year
        }
    }

    return allData.map { series ->
        series
            .groupBy { grouped(it.date) }
            .map { (_, group) ->
                val avg = group.map { it.value }.average()
                val latest = group.maxByOrNull { it.date }!!
                LineDataPoint(date = latest.date, value = avg)
            }
            .sortedBy { it.date }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun MultiLineChartViewPreview() {
    val stockData: List<List<LineDataPoint>> = listOf(
        listOf(
            LineDataPoint(date = 1755317990699L, value = 0.45),
            LineDataPoint(date = 1755404390699L, value = 0.62),
            LineDataPoint(date = 1755490790699L, value = 0.66),
            LineDataPoint(date = 1755577190699L, value = 0.64),
            LineDataPoint(date = 1755663590699L, value = 0.39),
            LineDataPoint(date = 1755749990699L, value = 0.63),
            LineDataPoint(date = 1755836390699L, value = 0.53),
            LineDataPoint(date = 1755922790699L, value = 0.35),
            LineDataPoint(date = 1756009190699L, value = 0.56),
            LineDataPoint(date = 1756095590699L, value = 0.44),
            LineDataPoint(date = 1756181990699L, value = 0.7),
            LineDataPoint(date = 1756268390699L, value = 0.57),
            LineDataPoint(date = 1756354790699L, value = 0.67),
            LineDataPoint(date = 1756441190699L, value = 0.48),
            LineDataPoint(date = 1756527590699L, value = 0.36),
            LineDataPoint(date = 1756613990699L, value = 0.31),
            LineDataPoint(date = 1756700390699L, value = 0.34),
            LineDataPoint(date = 1756786790699L, value = 0.5),
            LineDataPoint(date = 1756873190699L, value = 0.52),
            LineDataPoint(date = 1756959590699L, value = 0.39)
        ),
        listOf(
            LineDataPoint(date = 1755317990699L, value = -0.42),
            LineDataPoint(date = 1755404390699L, value = -0.66),
            LineDataPoint(date = 1755490790699L, value = -0.59),
            LineDataPoint(date = 1755577190699L, value = -0.5),
            LineDataPoint(date = 1755663590699L, value = -0.36),
            LineDataPoint(date = 1755749990699L, value = -0.36),
            LineDataPoint(date = 1755836390699L, value = -0.51),
            LineDataPoint(date = 1755922790699L, value = -0.4),
            LineDataPoint(date = 1756009190699L, value = -0.36),
            LineDataPoint(date = 1756095590699L, value = -0.58),
            LineDataPoint(date = 1756181990699L, value = -0.32),
            LineDataPoint(date = 1756268390699L, value = -0.66),
            LineDataPoint(date = 1756354790699L, value = -0.48),
            LineDataPoint(date = 1756441190699L, value = -0.64),
            LineDataPoint(date = 1756527590699L, value = -0.41),
            LineDataPoint(date = 1756613990699L, value = -0.38),
            LineDataPoint(date = 1756700390699L, value = -0.45),
            LineDataPoint(date = 1756786790699L, value = -0.55),
            LineDataPoint(date = 1756873190699L, value = -0.65),
            LineDataPoint(date = 1756959590699L, value = -0.48)
        ),
        listOf(
            LineDataPoint(date = 1755317990699L, value = 0.93),
            LineDataPoint(date = 1755404390699L, value = 0.9),
            LineDataPoint(date = 1755490790699L, value = 0.94),
            LineDataPoint(date = 1755577190699L, value = 1.08),
            LineDataPoint(date = 1755663590699L, value = 0.81),
            LineDataPoint(date = 1755749990699L, value = 0.96),
            LineDataPoint(date = 1755836390699L, value = 0.97),
            LineDataPoint(date = 1755922790699L, value = 1.04),
            LineDataPoint(date = 1756009190699L, value = 1.19),
            LineDataPoint(date = 1756095590699L, value = 1.0),
            LineDataPoint(date = 1756181990699L, value = 1.14),
            LineDataPoint(date = 1756268390699L, value = 1.01),
            LineDataPoint(date = 1756354790699L, value = 0.82),
            LineDataPoint(date = 1756441190699L, value = 0.99),
            LineDataPoint(date = 1756527590699L, value = 1.02),
            LineDataPoint(date = 1756613990699L, value = 0.96),
            LineDataPoint(date = 1756700390699L, value = 1.05),
            LineDataPoint(date = 1756786790699L, value = 0.88),
            LineDataPoint(date = 1756873190699L, value = 0.97),
            LineDataPoint(date = 1756959590699L, value = 1.04)
        ),
        listOf(
            LineDataPoint(date = 1755317990699L, value = -0.87),
            LineDataPoint(date = 1755404390699L, value = -0.92),
            LineDataPoint(date = 1755490790699L, value = -1.14),
            LineDataPoint(date = 1755577190699L, value = -0.95),
            LineDataPoint(date = 1755663590699L, value = -1.13),
            LineDataPoint(date = 1755749990699L, value = -0.91),
            LineDataPoint(date = 1755836390699L, value = -0.85),
            LineDataPoint(date = 1755922790699L, value = -1.14),
            LineDataPoint(date = 1756009190699L, value = -0.86),
            LineDataPoint(date = 1756095590699L, value = -1.12),
            LineDataPoint(date = 1756181990699L, value = -0.84),
            LineDataPoint(date = 1756268390699L, value = -1.04),
            LineDataPoint(date = 1756354790699L, value = -0.95),
            LineDataPoint(date = 1756441190699L, value = -1.1),
            LineDataPoint(date = 1756527590699L, value = -0.97),
            LineDataPoint(date = 1756613990699L, value = -0.88),
            LineDataPoint(date = 1756700390699L, value = -0.98),
            LineDataPoint(date = 1756786790699L, value = -1.18),
            LineDataPoint(date = 1756873190699L, value = -1.17),
            LineDataPoint(date = 1756959590699L, value = -1.07)
        )
    )
    MultiLineChartView(getFilteredData = { timeFrame ->
        filterDataByTimeFrame(stockData, timeFrame)
    })
 }
