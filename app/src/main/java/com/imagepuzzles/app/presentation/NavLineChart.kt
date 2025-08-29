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

// --------------------------- Public API -----------------------------

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PortfolioCard(
    modifier: Modifier = Modifier,
    currencyCode: String = "AUD",
    series: List<DataPoint>,
    selectedRange: RangeTab = RangeTab.Q3M
) {
    // NAV = giá trị cuối
    val nav = series.lastOrNull()?.value ?: 0f
    // % thay đổi so với đầu kỳ
    val change = if (series.size > 1) {
        val first = series.first().value
        if (first == 0f) 0f else (nav - first) / first * 100f
    } else 0f

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF7EFE5)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            // Header
            Text(
                "Net asset value",
                style = MaterialTheme.typography.labelLarge.copy(color = Color(0xFF6C6C6C))
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${nav.roundToInt().formatThousands()} $currencyCode",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111111),
                    lineHeight = 40.sp
                )
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val changeText = "${(nav - series.first().value).roundToInt().formatThousands()}"
                Text(
                    text = "+$changeText",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF18794E),
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "▲ ${"%.2f".format(change)}%",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF12A454),
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(Modifier.height(8.dp))

            // Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF7EFE5))
            ) {
                AreaLineChart(
                    data = series,
                    currencyCode = currencyCode
                )
            }

            Spacer(Modifier.height(12.dp))

            // Range tabs
            RangeTabs(
                current = selectedRange,
                onClick = { /* hook vào ViewModel nếu cần */ }
            )
        }
    }
}

// --------------------------- Chart -----------------------------

data class DataPoint(val date: LocalDate, val value: Float)
enum class DragMode { NONE, SCRUB, PILL }

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun AreaLineChart(
    data: List<DataPoint>,
    currencyCode: String
) {
    if (data.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val nf = remember { java.text.NumberFormat.getInstance(Locale.GERMANY) }
    val dateFmt = remember { DateTimeFormatter.ofPattern("dd, LLL yyyy", Locale.ENGLISH) }

    var selectedIndex by remember { mutableStateOf(data.lastIndex / 2) }

    var dragMode by remember { mutableStateOf(DragMode.NONE) }

    val animatedProgress by animateFloatAsState(targetValue = 1f, label = "drawProgress")

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { pos ->
                    // quyết định bấm pill (trái/phải) hay scrub
                    val w = size.width
                    val h = size.height

                    val left = 0f
                    val right = w
                    val top = 8.dp.toPx()
                    val pillH = 22.dp.toPx()
                    val bottom = h - 24.dp.toPx() // chừa chỗ cho pill
                    fun x(i: Int) =
                        left + (right - left) * (i / (data.lastIndex.coerceAtLeast(1)
                            .toFloat())) * animatedProgress

                    val pillW = 40.dp.toPx()
                    val cx = x(selectedIndex)
                    val pillLeft = (cx - pillW / 2)
                    val pillTop = bottom - pillH / 2    // tâm pill nằm đúng trên baseline
                    val hitPill = pos.x in pillLeft..(pillLeft + pillW) &&
                            pos.y in pillTop..(pillTop + pillH)

                    if (hitPill) {
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
                    onDragStart = { pos ->
                        val w = size.width
                        val h = size.height
                        val left = 0f
                        val right = w
                        val pillH = 22.dp.toPx()
                        val bottom = h - 24.dp.toPx()
                        fun x(i: Int) =
                            left + (right - left) * (i / (data.lastIndex.coerceAtLeast(1)
                                .toFloat())) * animatedProgress

                        val pillW = 40.dp.toPx()
                        val cx = x(selectedIndex)
                        val pillLeft = (cx - pillW / 2)
                        val pillTop = bottom - pillH / 2
                        dragMode = if (pos.x in pillLeft..(pillLeft + pillW) &&
                            pos.y in pillTop..(pillTop + pillH)
                        ) DragMode.PILL else DragMode.SCRUB
                    },
                    onDragEnd = { dragMode = DragMode.NONE },
                    onDragCancel = { dragMode = DragMode.NONE }
                ) { change, _ ->
                    when (dragMode) {
                        DragMode.PILL, DragMode.SCRUB ->
                            selectedIndex =
                                offsetToIndex(change.position.x, size.width.toFloat(), data.size)

                        else -> Unit
                    }
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // full width card
            val left = 0f
            val right = size.width
            val top = 8.dp.toPx()
            val bottom = size.height - 24.dp.toPx() // baseline nằm ở đây (chừa 24dp dưới)

            val values = data.map { it.value }
            val minY = (values.minOrNull() ?: 0f) * 0.98f
            val maxY = (values.maxOrNull() ?: 0f) * 1.02f
            val yRange = (maxY - minY).takeIf { it != 0f } ?: 1f

            fun x(i: Int) = left + (right - left) *
                    (i / (data.lastIndex.coerceAtLeast(1).toFloat())) * animatedProgress

            fun y(v: Float) = bottom - (v - minY) / yRange * (bottom - top)

            // line & area (full dải)
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

            // fill
            drawPath(
                path = area,
                brush = Brush.verticalGradient(
                    0f to Color(0xFFFFF6E9),
                    0.8f to Color(0xFFEFDCC3),
                    1f to Color(0x00EFDCC3)
                )
            )

            // baseline nét đứt
            drawLine(
                color = Color(0x66000000),
                start = Offset(left, bottom),
                end = Offset(right, bottom),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
            )

            // line chính
            drawPath(
                path = line,
                color = Color(0xFFE0A860),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // scrubber
            val idx = selectedIndex.coerceIn(0, data.lastIndex)
            val cx = x(idx)
            val cy = y(data[idx].value)

            // vạch dọc: đáy -> dot
            drawLine(
                color = Color(0x33D9A55A),
                start = Offset(cx, bottom),
                end = Offset(cx, cy),
                strokeWidth = 2.dp.toPx()
            )

            val innerRadius = 5.dp.toPx()        // bán kính hạt tròn bên trong
            val gap = 3.dp.toPx()                // khoảng trắng giữa hạt tròn và vòng ngoài
            val ringStroke = 3.dp.toPx()         // độ dày nét vòng ngoài
            val innerColor = Color(0xFFE0A860)   // màu hạt tròn
            val ringColor = Color(0xFFE0A860)    // màu vòng ngoài
            val holeBg =
                Color(0xFFF7EFE5)       // màu “nền” ngay dưới dot (để tạo cảm giác tách khỏi line)

// 1) Nền nhỏ dưới dot để đảm bảo khoảng trắng thật sự “trong suốt” so với line/area
//    (tùy nền card của bạn, nếu không cần thì có thể bỏ dòng này)
            drawCircle(
                color = holeBg,
                radius = innerRadius + gap + ringStroke, // to hơn một chút để che dưới
                center = Offset(cx, cy)
            )

// 2) Hạt tròn bên trong (filled)
            drawCircle(
                color = innerColor,
                radius = innerRadius,
                center = Offset(cx, cy)
            )

// 3) Vòng tròn bên ngoài (ring). Với style=Stroke, radius là tới tâm nét.
//    Muốn cách hạt trong một đoạn `gap`, thì:
//       ringRadius = innerRadius + gap + ringStroke/2
            val ringRadius = innerRadius + gap + ringStroke / 2f
            drawCircle(
                color = ringColor,
                radius = ringRadius,
                center = Offset(cx, cy),
                style = Stroke(width = ringStroke)
            )

            // tooltip: tự canh trái/phải
            val dateText = data[idx].date.format(dateFmt)
            val valueText = nf.format(data[idx].value.roundToInt())
            val padding = 10.dp.toPx()
            val gapTooltip = 8.dp.toPx() // khoảng cách so với dot theo phương ngang

            val titleLayout = textMeasurer.measure(
                text = dateText,
                style = TextStyle(fontSize = 12.sp, color = Color(0xFF8B8B8B))
            )
            val valueLayout = textMeasurer.measure(
                text = valueText,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111111)
                )
            )

            val bubbleW = maxOf(titleLayout.size.width, valueLayout.size.width) + padding * 2
            val bubbleH =
                titleLayout.size.height + valueLayout.size.height + padding * 2 + 4.dp.toPx()

            // nếu dot ở nửa phải → bubble bên trái; ngược lại → bên phải
            val placeLeft = cx > (left + right) / 2f
            val rawBubbleLeft = if (placeLeft) cx - gapTooltip - bubbleW else cx + gapTooltip
            val bubbleLeft = rawBubbleLeft
                .coerceIn(left, right - bubbleW)

            // luôn đặt bubble phía trên dot; nếu vượt mép trên thì hạ xuống
            val rawBubbleTop = cy - bubbleH - 12.dp.toPx()
            val bubbleTop = rawBubbleTop.coerceAtLeast(top)

            drawRoundRect(
                color = Color.White,
                topLeft = Offset(bubbleLeft, bubbleTop),
                size = androidx.compose.ui.geometry.Size(bubbleW, bubbleH),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
            )
            drawText(
                textLayoutResult = titleLayout,
                topLeft = Offset(bubbleLeft + padding, bubbleTop + padding)
            )
            drawText(
                textLayoutResult = valueLayout,
                topLeft = Offset(
                    bubbleLeft + padding,
                    bubbleTop + padding + titleLayout.size.height + 4.dp.toPx()
                )
            )

            // PILL 2 nút: tâm nằm trên baseline
            val pillW = 40.dp.toPx()
            val pillH = 22.dp.toPx()
            val pillCR = pillH / 2
            val pillTop = bottom - pillH / 2
            val pillLeft = (cx - pillW / 2)

// thân pill
            drawRoundRect(
                color = Color(0xFFE0A860),
                topLeft = Offset(pillLeft, pillTop),
                size = androidx.compose.ui.geometry.Size(pillW, pillH),
                cornerRadius = CornerRadius(pillCR, pillCR)
            )

// toạ độ chung
            val centerY = pillTop + pillH / 2
            val arrowSize = 5.dp.toPx()

// tam giác trái (◀)
            val leftCenterX = pillLeft + pillW / 2 - 8.dp.toPx()
            val leftArrow = Path().apply {
                moveTo(leftCenterX + arrowSize, centerY - arrowSize)  // trên phải
                lineTo(leftCenterX - arrowSize, centerY)              // trái
                lineTo(leftCenterX + arrowSize, centerY + arrowSize)  // dưới phải
                close()
            }
            drawPath(leftArrow, Color.White)

// tam giác phải (▶)
            val rightCenterX = pillLeft + pillW / 2 + 8.dp.toPx()
            val rightArrow = Path().apply {
                moveTo(rightCenterX - arrowSize, centerY - arrowSize) // trên trái
                lineTo(rightCenterX + arrowSize, centerY)             // phải
                lineTo(rightCenterX - arrowSize, centerY + arrowSize) // dưới trái
                close()
            }
            drawPath(rightArrow, Color.White)
        }
    }
}

// --------------------------- Range Tabs -----------------------------

enum class RangeTab(val label: String) {
    Q1D("1D"), Q1W("1W"), Q1M("1M"), Q3M("3M"), YTD("YTD"), ALL(
        "All"
    )
}

@Composable
private fun RangeTabs(
    current: RangeTab,
    onClick: (RangeTab) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        RangeTab.values().forEach { item ->
            val active = item == current
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) Color(0xFFEEDDC8) else Color(0xFFFFFFFF))
                    .clickable { onClick(item) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    item.label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        color = Color(0xFF4A4036)
                    )
                )
            }
        }
    }
}

// --------------------------- Helpers -----------------------------

private fun Int.formatThousands(): String {
    val nf = NumberFormat.getInstance(Locale.GERMANY)
    return nf.format(this)
}

private fun offsetToIndex(x: Float, width: Float, size: Int): Int {
    val clamped = x.coerceIn(0f, width)
    val frac = if (width == 0f) 0f else clamped / width
    return (frac * (size - 1)).roundToInt().coerceIn(0, size - 1)
}

@RequiresApi(Build.VERSION_CODES.O)
private fun demoSeries(
    days: Int = 90,
    start: LocalDate = LocalDate.now().minusDays((days - 1).toLong())
): List<DataPoint> {
    var v = 72_000f
    val rnd = Random(42)
    return (0 until days).map { i ->
        // random walk
        v += (rnd.nextFloat() - 0.45f) * 1200f
        val date = start.plusDays(i.toLong())
        DataPoint(date, v.coerceAtLeast(50_000f))
    } + listOf(
        // thêm “spike” giống hình
        DataPoint(LocalDate.now().minusDays(30), 78_232f)
    )
        .sortedBy { it.date }
}

// --------------------------- Preview -----------------------------

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, backgroundColor = 0xFFEFE7DD)
@Composable
private fun PortfolioCardPreview() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFEFE7DD))
                .padding(16.dp)
        ) {
            PortfolioCard(
                currencyCode = "AUD",
                series = demoSeries(95),
                selectedRange = RangeTab.Q3M
            )
        }
    }
}
