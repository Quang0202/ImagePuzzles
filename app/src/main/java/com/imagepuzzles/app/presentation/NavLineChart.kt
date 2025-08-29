package com.imagepuzzles.app.presentation

import android.icu.text.NumberFormat
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.roundToInt

// ====== Data & Enum ======
data class DataPoint(val date: LocalDate, val value: Float)

enum class RangeTab(val label: String) { Q1D("1D"), Q1W("1W"), Q1M("1M"), Q3M("3M"), YTD("YTD"), ALL("All") }

// ====== PortfolioCard demo container ======
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PortfolioCard(
    modifier: Modifier = Modifier,
    currencyCode: String = "AUD",
    series: List<DataPoint>
) {
    var selectedTab by remember { mutableStateOf(RangeTab.Q3M) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7EFE5)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            // Header
            Text("Net asset value", style = MaterialTheme.typography.labelLarge.copy(color = Color(0xFF6C6C6C)))
            Spacer(Modifier.height(6.dp))
            val nav = series.lastOrNull()?.value ?: 0f
            Text(
                text = "${nav.roundToInt()} $currencyCode",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(Modifier.height(8.dp))

            // Chart + Tabs
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(Color(0xFFF7EFE5))
            ) {
                AreaLineChart(
                    data = series,
                    currencyCode = currencyCode,
                    tabs = RangeTab.values().toList(),
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
        }
    }
}

// ====== AreaLineChart ======
enum class DragMode { NONE, SCRUB, PILL }
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AreaLineChart(
    data: List<DataPoint>,
    currencyCode: String,
    tabs: List<RangeTab>,
    selectedTab: RangeTab,
    onTabSelected: (RangeTab) -> Unit
) {
    if (data.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val numberFmt = remember { NumberFormat.getInstance(Locale.GERMANY) }
    val dateFmt = remember { DateTimeFormatter.ofPattern("dd, LLL yyyy", Locale.ENGLISH) }

    var selectedIndex by remember(data) { mutableStateOf((data.lastIndex / 2).coerceAtLeast(0)) }

    var dragMode by remember { mutableStateOf(DragMode.NONE) }

    val anim by animateFloatAsState(targetValue = 1f, label = "chartProgress")

    Box(Modifier.fillMaxSize()) {

        Canvas(Modifier.fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { pos ->
                    val w = size.width
                    val h = size.height
                    val tabsH = 40.dp.toPx()
                    val pillH = 22.dp.toPx()
                    val bottom = h - (tabsH + 8.dp.toPx())

                    fun x(i: Int) = (w * (i / (data.lastIndex.coerceAtLeast(1).toFloat()))) * anim
                    val pillW = 40.dp.toPx()
                    val cx = x(selectedIndex)
                    val pillLeft = cx - pillW / 2
                    val pillTop = bottom - pillH / 2
                    val inPill = pos.x in pillLeft..(pillLeft + pillW) && pos.y in pillTop..(pillTop + pillH)

                    if (inPill) {
                        if (pos.x < pillLeft + pillW / 2) {
                            selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                        } else {
                            selectedIndex = (selectedIndex + 1).coerceAtMost(data.lastIndex)
                        }
                    } else {
                        selectedIndex = offsetToIndex(pos.x, w.toFloat(), data.size)
                        dragMode = DragMode.SCRUB
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragMode = DragMode.SCRUB },
                    onDragEnd = { dragMode = DragMode.NONE },
                    onDragCancel = { dragMode = DragMode.NONE }
                ) { change, _ ->
                    if (dragMode != DragMode.NONE) {
                        selectedIndex = offsetToIndex(change.position.x, size.width.toFloat(), data.size)
                    }
                }
            }
        ) {
            val w = size.width
            val h = size.height
            val top = 8.dp.toPx()
            val tabsH = 40.dp.toPx()
            val pillH = 22.dp.toPx()
            val bottom = h - (tabsH + 8.dp.toPx())

            val values = data.map { it.value }
            val minY = (values.minOrNull() ?: 0f) * 0.98f
            val maxY = (values.maxOrNull() ?: 0f) * 1.02f
            val yRange = (maxY - minY).takeIf { it != 0f } ?: 1f

            fun x(i: Int) = (w * (i / (data.lastIndex.coerceAtLeast(1).toFloat()))) * anim
            fun y(v: Float) = bottom - (v - minY) / yRange * (bottom - top)

            clipRect(left = 0f, top = top, right = w, bottom = bottom) {
                val line = Path().apply {
                    moveTo(x(0), y(data[0].value))
                    for (i in 1..data.lastIndex) lineTo(x(i), y(data[i].value))
                }
                val area = Path().apply {
                    moveTo(x(0), y(data[0].value))
                    for (i in 1..data.lastIndex) lineTo(x(i), y(data[i].value))
                    lineTo(x(data.lastIndex), bottom)
                    lineTo(x(0), bottom)
                    close()
                }
                drawPath(area, Brush.verticalGradient(
                    0f to Color(0xFFFFF6E9),
                    0.8f to Color(0xFFEFDCC3),
                    1f to Color(0x00EFDCC3)
                ))
                drawPath(line, Color(0xFFE0A860), style = Stroke(width = 3.dp.toPx()))
            }

            // baseline nét đứt
            drawLine(
                Color(0x66000000), Offset(0f, bottom), Offset(w, bottom),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
            )

            // scrubber + dot
            val idx = selectedIndex.coerceIn(0, data.lastIndex)
            val cx = x(idx)
            val cy = y(data[idx].value)
            drawLine(Color(0x33D9A55A), Offset(cx, bottom), Offset(cx, cy), strokeWidth = 2.dp.toPx())

            // dot inner + ring
            val innerR = 5.dp.toPx()
            val gap = 3.dp.toPx()
            val ringStroke = 3.dp.toPx()
            val ringR = innerR + gap + ringStroke / 2f
            drawCircle(Color(0xFFF7EFE5), radius = ringR + 2.dp.toPx(), center = Offset(cx, cy)) // nền
            drawCircle(Color(0xFFE0A860), radius = innerR, center = Offset(cx, cy))
            drawCircle(Color(0xFFE0A860), radius = ringR, center = Offset(cx, cy), style = Stroke(ringStroke))

            // tooltip
            val dateText = data[idx].date.format(dateFmt)
            val valueText = numberFmt.format(data[idx].value.roundToInt())
            val padding = 8.dp.toPx()
            val titleLayout = textMeasurer.measure(dateText, TextStyle(fontSize = 12.sp, color = Color.Gray))
            val valueLayout = textMeasurer.measure(valueText, TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold))
            val bubbleW = maxOf(titleLayout.size.width, valueLayout.size.width) + padding * 2
            val bubbleH = titleLayout.size.height + valueLayout.size.height + padding * 2
            val placeLeft = cx > w / 2
            val bubbleLeft = if (placeLeft) cx - bubbleW - 8.dp.toPx() else cx + 8.dp.toPx()
            val bubbleTop = (cy - bubbleH - 12.dp.toPx()).coerceAtLeast(top)

            drawRoundRect(Color.White, Offset(bubbleLeft, bubbleTop),
                androidx.compose.ui.geometry.Size(bubbleW, bubbleH), CornerRadius(12.dp.toPx()))
            drawText(
                textMeasurer = textMeasurer,
                text = dateText,
                style = TextStyle(fontSize = 12.sp, color = Color(0xFF8B8B8B)),
                topLeft = Offset(bubbleLeft + padding, bubbleTop + padding)
            )

// vẽ dòng giá trị
            drawText(
                textMeasurer = textMeasurer,
                text = valueText,
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111111)),
                topLeft = Offset(
                    bubbleLeft + padding,
                    bubbleTop + padding + 4.dp.toPx() + // spacing giữa 2 dòng
                            textMeasurer.measure(
                                text = dateText,
                                style = TextStyle(fontSize = 12.sp)
                            ).size.height
                )
            )

            // pill + 2 tam giác
            val pillW = 40.dp.toPx()
            val pillTop = bottom - pillH / 2
            val pillLeft = cx - pillW / 2
            drawRoundRect(Color(0xFFE0A860), Offset(pillLeft, pillTop),
                androidx.compose.ui.geometry.Size(pillW, pillH), CornerRadius(pillH/2, pillH/2))
            val centerY = pillTop + pillH / 2
            val arrowSize = 5.dp.toPx()
            val leftArrow = Path().apply {
                moveTo(cx - 8.dp.toPx() + arrowSize, centerY - arrowSize)
                lineTo(cx - 8.dp.toPx() - arrowSize, centerY)
                lineTo(cx - 8.dp.toPx() + arrowSize, centerY + arrowSize)
                close()
            }
            val rightArrow = Path().apply {
                moveTo(cx + 8.dp.toPx() - arrowSize, centerY - arrowSize)
                lineTo(cx + 8.dp.toPx() + arrowSize, centerY)
                lineTo(cx + 8.dp.toPx() - arrowSize, centerY + arrowSize)
                close()
            }
            drawPath(leftArrow, Color.White)
            drawPath(rightArrow, Color.White)
        }

        // RangeTabs
        RangeTabs(
            current = selectedTab,
            onClick = onTabSelected,
            tabs = tabs,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp)
        )
    }
}

// ===== RangeTabs =====
@Composable
fun RangeTabs(
    current: RangeTab,
    onClick: (RangeTab) -> Unit,
    tabs: List<RangeTab>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEach { item ->
            val active = item == current
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .padding(horizontal = 2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (active) Color(0xFFEEDDC8) else Color.White.copy(alpha = 0.7f))
                    .clickable { onClick(item) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    item.label,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        color = Color(0xFF4A4036)
                    )
                )
            }
        }
    }
}

// ===== Helper =====
private fun offsetToIndex(x: Float, width: Float, size: Int): Int {
    val clamped = x.coerceIn(0f, width)
    val frac = if (width == 0f) 0f else clamped / width
    return (frac * (size - 1)).roundToInt().coerceIn(0, size - 1)
}

// ===== Fake data demo =====
@RequiresApi(Build.VERSION_CODES.O)
private fun demoSeries(days: Int = 90): List<DataPoint> {
    val start = LocalDate.now().minusDays(days.toLong())
    var v = 72000f
    val rnd = Random()
    return (0 until days).map { i ->
        v += (rnd.nextFloat() - 0.45f) * 1200f
        DataPoint(start.plusDays(i.toLong()), v.coerceAtLeast(50000f))
    }
}

// ===== Preview =====
@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun PortfolioCardPreview() {
    MaterialTheme {
        Box(Modifier.fillMaxSize().background(Color(0xFFEFE7DD)).padding(16.dp)) {
            PortfolioCard(series = demoSeries(100))
        }
    }
}
