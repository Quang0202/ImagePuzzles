package com.imagepuzzles.app.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
@Composable
fun RoundedPieChart(
    data: List<PieChartData>,
    modifier: Modifier = Modifier,
    centerText: String = "",
    centerSubText: String = "",
    strokeWidth: Dp = 60.dp,
    gapAngle: Float = 8f,
    cornerRadius: Dp = 8.dp
) {
    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val radius = minOf(canvasWidth, canvasHeight) / 2f
            val strokeWidthPx = strokeWidth.toPx()
            val cornerRadiusPx = cornerRadius.toPx()
            val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
            val outerRadius = radius - 20.dp.toPx() // Add some padding
            val innerRadius = outerRadius - strokeWidthPx

            // Calculate total value
            val totalValue = data.sumOf { it.value.toDouble() }.toFloat()
            if (totalValue <= 0f) return@Canvas

            var currentAngle = -90f // Start from top

            data.forEach { pieData ->
                val sweepAngle = (pieData.value / totalValue) * (360f - (data.size * gapAngle))

                if (sweepAngle > 0f) {
                    // Create path for rounded segment
                    val path = createRoundedPieSegment(
                        center = center,
                        innerRadius = innerRadius,
                        outerRadius = outerRadius,
                        startAngle = currentAngle,
                        sweepAngle = sweepAngle,
                        cornerRadius = cornerRadiusPx
                    )

                    // Draw the segment
                    drawPath(
                        path = path,
                        color = pieData.color
                    )
                }

                currentAngle += sweepAngle + gapAngle
            }
        }

        // Center content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

        }
    }
}

private fun createRoundedPieSegment(
    center: Offset,
    innerRadius: Float,
    outerRadius: Float,
    startAngle: Float,
    sweepAngle: Float,
    cornerRadius: Float
): Path {
    val path = Path()

    // Convert angles to radians
    val startRad = Math.toRadians(startAngle.toDouble()).toFloat()
    val endRad = Math.toRadians((startAngle + sweepAngle).toDouble()).toFloat()

    // Calculate corner points
    val innerStart = Offset(
        center.x + innerRadius * cos(startRad),
        center.y + innerRadius * sin(startRad)
    )
    val innerEnd = Offset(
        center.x + innerRadius * cos(endRad),
        center.y + innerRadius * sin(endRad)
    )
    val outerStart = Offset(
        center.x + outerRadius * cos(startRad),
        center.y + outerRadius * sin(startRad)
    )
    val outerEnd = Offset(
        center.x + outerRadius * cos(endRad),
        center.y + outerRadius * sin(endRad)
    )

    if (cornerRadius <= 0f) {
        // No rounding - simple path
        path.moveTo(innerStart.x, innerStart.y)
        path.arcTo(
            rect = Rect(center, innerRadius),
            startAngleDegrees = startAngle,
            sweepAngleDegrees = sweepAngle,
            forceMoveTo = false
        )
        path.lineTo(outerEnd.x, outerEnd.y)
        path.arcTo(
            rect = Rect(center, outerRadius),
            startAngleDegrees = startAngle + sweepAngle,
            sweepAngleDegrees = -sweepAngle,
            forceMoveTo = false
        )
        path.close()
    } else {
        // Create rounded corners
        val adjustedCornerRadius = minOf(cornerRadius, (outerRadius - innerRadius) / 2f)

        // Start at inner radius, offset by corner radius
        val innerStartOffset = getOffsetPoint(innerStart, outerStart, adjustedCornerRadius)
        path.moveTo(innerStartOffset.x, innerStartOffset.y)

        // Inner arc
        val innerArcStartAngle = startAngle + Math.toDegrees(
            adjustedCornerRadius / innerRadius.toDouble()
        ).toFloat()
        val innerArcSweepAngle = sweepAngle - 2f * Math.toDegrees(
            adjustedCornerRadius / innerRadius.toDouble()
        ).toFloat()

        if (innerArcSweepAngle > 0f) {
            path.arcTo(
                rect = Rect(center, innerRadius),
                startAngleDegrees = innerArcStartAngle,
                sweepAngleDegrees = innerArcSweepAngle,
                forceMoveTo = false
            )
        }

        // Rounded corner at inner end
        val innerEndOffset = getOffsetPoint(innerEnd, outerEnd, adjustedCornerRadius)
        path.quadraticBezierTo(
            innerEnd.x, innerEnd.y,
            innerEndOffset.x, innerEndOffset.y
        )

        // Side line to outer arc
        val outerEndOffset = getOffsetPoint(outerEnd, innerEnd, adjustedCornerRadius)
        path.lineTo(outerEndOffset.x, outerEndOffset.y)

        // Rounded corner at outer end
        path.quadraticBezierTo(
            outerEnd.x, outerEnd.y,
            outerEnd.x + adjustedCornerRadius * cos(endRad + Math.PI.toFloat() / 2f),
            outerEnd.y + adjustedCornerRadius * sin(endRad + Math.PI.toFloat() / 2f)
        )

        // Outer arc (reverse)
        val outerArcStartAngle = startAngle + sweepAngle - Math.toDegrees(
            adjustedCornerRadius / outerRadius.toDouble()
        ).toFloat()
        val outerArcSweepAngle = -(sweepAngle - 2f * Math.toDegrees(
            adjustedCornerRadius / outerRadius.toDouble()
        ).toFloat())

        if (outerArcSweepAngle < 0f) {
            path.arcTo(
                rect = Rect(center, outerRadius),
                startAngleDegrees = outerArcStartAngle,
                sweepAngleDegrees = outerArcSweepAngle,
                forceMoveTo = false
            )
        }

        // Rounded corner at outer start
        val outerStartOffset = getOffsetPoint(outerStart, innerStart, adjustedCornerRadius)
        path.quadraticBezierTo(
            outerStart.x, outerStart.y,
            outerStartOffset.x, outerStartOffset.y
        )

        path.close()
    }

    return path
}

private fun getOffsetPoint(from: Offset, to: Offset, distance: Float): Offset {
    val direction = (to - from)
    val length = direction.getDistance()
    return if (length > 0) {
        from + (direction / length) * distance
    } else {
        from
    }
}

data class PieChartData(
    val value: Float,
    val color: Color,
    val label: String = ""
)

@Preview
@Composable
fun RoundedPieChartPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val sampleData = listOf(
                    PieChartData(40f, Color(0xFF2E7D32), "Large"),
                    PieChartData(25f, Color(0xFF66BB6A), "Medium"),
                    PieChartData(20f, Color(0xFF81C784), "Small"),
                    PieChartData(15f, Color(0xFFEF5350), "Tiny")
                )


                RoundedPieChart(
                    data = sampleData,
                    modifier = Modifier.size(280.dp),
                    centerText = "$328K",
                    centerSubText = "Size\nDistribution",
                    strokeWidth = 50.dp,
                    gapAngle = 8f,
                    cornerRadius = 20.dp
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Corner Radius: 0dp (Sharp)",
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                RoundedPieChart(
                    data = sampleData,
                    modifier = Modifier.size(280.dp),
                    centerText = "$328K",
                    centerSubText = "Size\nDistribution",
                    strokeWidth = 50.dp,
                    gapAngle = 8f,
                    cornerRadius = 0.dp
                )
            }
        }
    }
}