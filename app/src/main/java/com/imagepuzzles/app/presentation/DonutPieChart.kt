package com.imagepuzzles.app.presentation

import android.graphics.CornerPathEffect
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
import androidx.compose.ui.geometry.Size
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.*
import kotlin.math.atan2
import kotlin.math.*

// ---------- Model ----------
data class PieSlice(val value: Float, val color: Color, val label: String = "")

// ---------- Public API ----------
@Composable
fun DonutPieRoundedOverlap(
    slices: List<PieSlice>,
    modifier: Modifier = Modifier,
    holeRatio: Float = 0.62f,     // 0f..1f
    roundRadius: Dp = 12.dp,      // bán kính bo cho 4 góc (lồi)
    // độ dày đường phân cách (vẽ sau cùng, luôn nhìn thấy)
    separatorWidth: Dp = 4.dp,
    // lượng chồng lên nhau ở mỗi đầu miếng, đo dọc theo cung tại bán kính giữa
    overlapWidth: Dp = 20.dp,
    startAngle: Float = -90f,
    separatorColor: Color = Color.White
) {
    if (slices.isEmpty()) return
    val total = slices.sumOf { it.value.toDouble() }.toFloat()
    if (total <= 0f) return

    Canvas(modifier = modifier) {
        val minSide = size.minDimension
        if (minSide <= 0f) return@Canvas

        val center = Offset(size.width / 2f, size.height / 2f)
        val outerR = minSide / 2f
        val innerR = (outerR * holeRatio).coerceIn(0f, outerR - 1f)
        val thickness = (outerR - innerR).coerceAtLeast(1f)
        val midR = innerR + thickness / 2f

        // đổi px -> độ ở bán kính giữa
        val sepPx = separatorWidth.toPx()
        val sepDeg = (sepPx / midR) * (180f / Math.PI.toFloat())
        val dashDeg = sepDeg * 1.15f // quét hơi dài hơn chút để “ăn” trọn mép

        val overlapPx = overlapWidth.toPx()
        val overlapDeg = (overlapPx / midR) * (180f / Math.PI.toFloat())

        // kẹp roundRadius
        val rCornerMax = thickness / 2f - 0.5f
        val rCornerBase = roundRadius.toPx().coerceIn(0f, rCornerMax)

        // Tính sẵn các ranh giới (góc cộng dồn) để vẽ separator sau cùng
        val boundaries = mutableListOf<Float>()
        var aCursor = startAngle
        slices.forEach { s ->
            val sweep = (s.value / total) * 360f
            boundaries += aCursor + sweep       // biên phải của miếng
            aCursor += sweep
        }

        // 1) Vẽ các miếng (mỗi miếng nới ra 2 đầu theo overlapDeg)
        aCursor = startAngle
        slices.forEach { s ->
            val sweep = (s.value / total) * 360f
            if (sweep <= 0f) { aCursor += sweep; return@forEach }

            // Nới 2 đầu: start lùi 1/2 overlap, sweep tăng đủ overlap
            val segStart = aCursor - overlapDeg / 2f
            val segSweep = max(0.4f, sweep + overlapDeg)

            // Kẹp bán kính bo theo sweep hiện tại
            var r = rCornerBase
            val phiOut = if (r > 0f) Math.toDegrees(atan2(r.toDouble(), (outerR - r).toDouble())).toFloat() else 0f
            val phiIn  = if (r > 0f) Math.toDegrees(atan2(r.toDouble(), (innerR + r).toDouble())).toFloat() else 0f
            val minNeeded = 2 * phiOut + 2 * phiIn
            if (segSweep <= minNeeded + 0.5f && minNeeded > 0f) {
                r *= ((segSweep * 0.9f) / minNeeded).toFloat()
            }

            val path = buildRoundedRingSegmentPrecise(
                center = center,
                innerR = innerR,
                outerR = outerR,
                startDeg = segStart,
                sweepDeg = segSweep,
                cornerR = r
            )
            drawPath(path = path, color = s.color, style = Fill)
            drawPath(path = path, color = Color.White, style = Stroke(width = 5f))
            aCursor += sweep
        }
    }
}

// ---------- Hình học chuẩn 4 góc lồi (dùng lại bản đúng) ----------
private fun buildRoundedRingSegmentPrecise(
    center: Offset,
    innerR: Float,
    outerR: Float,
    startDeg: Float,
    sweepDeg: Float,
    cornerR: Float
): Path {
    if (cornerR <= 0f) return buildRingSegmentPath(center, innerR, outerR, startDeg, sweepDeg)

    val a0 = startDeg
    val a1 = startDeg + sweepDeg

    fun u(angle: Float) = Offset(cosd(angle), sind(angle))
    fun vPerpCCW(angle: Float) = Offset(-sind(angle), cosd(angle))

    fun outerCornerCenter(angle: Float, signInside: Float): Offset {
        val uu = u(angle)
        val vv = vPerpCCW(angle)
        val t = sqrt(max(0.0, (outerR * outerR - 2.0 * outerR * cornerR).toDouble())).toFloat()
        return center + uu * t + vv * (signInside * cornerR)
    }
    fun innerCornerCenter(angle: Float, signInside: Float): Offset {
        val uu = u(angle)
        val vv = vPerpCCW(angle)
        val t = sqrt(((innerR * innerR) + (2.0 * innerR * cornerR)).toDouble()).toFloat()
        return center + uu * t + vv * (signInside * cornerR)
    }

    val Cos = outerCornerCenter(a0, +1f)
    val Coe = outerCornerCenter(a1, -1f)
    val Cis = innerCornerCenter(a0, +1f)
    val Cie = innerCornerCenter(a1, -1f)

    fun angO(p: Offset) = Math.toDegrees(atan2((p.y - center.y).toDouble(), (p.x - center.x).toDouble())).toFloat()
    val angOS = angO(Cos)
    val angOE = angO(Coe)
    val angIS = angO(Cis)
    val angIE = angO(Cie)

    val OS = polar(center, outerR, angOS)
    val OE = polar(center, outerR, angOE)
    val IS = polar(center, innerR, angIS)
    val IE = polar(center, innerR, angIE)

    fun lineContactAt(C: Offset, angle: Float, signInside: Float): Offset {
        val n = vPerpCCW(angle) * signInside
        return C - n * cornerR
    }
    val OSE = lineContactAt(Coe, a1, -1f)
    val IEE = lineContactAt(Cie, a1, -1f)
    val ISS = lineContactAt(Cis, a0, +1f)
    val OSS = lineContactAt(Cos, a0, +1f)

    fun circleRect(c: Offset, r: Float) = Rect(c.x - r, c.y - r, c.x + r, c.y + r)
    val rectOuter = circleRect(center, outerR)
    val rectInner = circleRect(center, innerR)
    val rectCos = circleRect(Cos, cornerR)
    val rectCoe = circleRect(Coe, cornerR)
    val rectCis = circleRect(Cis, cornerR)
    val rectCie = circleRect(Cie, cornerR)

    fun angleAt(c: Offset, p: Offset) =
        Math.toDegrees(atan2((p.y - c.y).toDouble(), (p.x - c.x).toDouble())).toFloat()

    fun cwSweep(from: Float, to: Float): Float {
        var s = to - from
        while (s < 0f) s += 360f
        while (s >= 360f) s -= 360f
        return s
    }
    fun ccwSweep(from: Float, to: Float): Float {
        var s = to - from
        while (s > 0f) s -= 360f
        while (s <= -360f) s += 360f
        return s
    }

    val path = Path()
    path.moveTo(OS.x, OS.y)
    path.arcTo(rectOuter, angOS, cwSweep(angOS, angOE), false)

    val aOE = angleAt(Coe, OE)
    val aOSE = angleAt(Coe, OSE)
    path.arcTo(rectCoe, aOE, cwSweep(aOE, aOSE), false)

    path.lineTo(IEE.x, IEE.y)

    val aIEE = angleAt(Cie, IEE)
    val aIE = angleAt(Cie, IE)
    path.arcTo(rectCie, aIEE, cwSweep(aIEE, aIE), false)

    path.arcTo(rectInner, angIE, ccwSweep(angIE, angIS), false)

    val aIS = angleAt(Cis, IS)
    val aISS = angleAt(Cis, ISS)
    path.arcTo(rectCis, aIS, cwSweep(aIS, aISS), false)

    path.lineTo(OSS.x, OSS.y)

    val aOSS = angleAt(Cos, OSS)
    val aOS = angleAt(Cos, OS)
    path.arcTo(rectCos, aOSS, cwSweep(aOSS, aOS), false)

    path.close()
    return path
}

private fun buildRingSegmentPath(
    center: Offset,
    innerR: Float,
    outerR: Float,
    startDeg: Float,
    sweepDeg: Float
): Path {
    val path = Path()
    val rectOuter = Rect(center.x - outerR, center.y - outerR, center.x + outerR, center.y + outerR)
    val rectInner = Rect(center.x - innerR, center.y - innerR, center.x + innerR, center.y + innerR)
    val pStart = polar(center, outerR, startDeg)
    path.moveTo(pStart.x, pStart.y)
    path.arcTo(rectOuter, startDeg, sweepDeg, false)
    val pEndInner = polar(center, innerR, startDeg + sweepDeg)
    path.lineTo(pEndInner.x, pEndInner.y)
    path.arcTo(rectInner, startDeg + sweepDeg, -sweepDeg, false)
    path.close()
    return path
}

// ---------- Math helpers ----------
private fun cosd(d: Float) = cos(Math.toRadians(d.toDouble())).toFloat()
private fun sind(d: Float) = sin(Math.toRadians(d.toDouble())).toFloat()
private operator fun Offset.plus(o: Offset) = Offset(x + o.x, y + o.y)
private operator fun Offset.minus(o: Offset) = Offset(x - o.x, y - o.y)
private operator fun Offset.times(k: Float) = Offset(x * k, y * k)

private fun polar(center: Offset, radius: Float, angleDeg: Float): Offset {
    val r = Math.toRadians(angleDeg.toDouble())
    return Offset(center.x + radius * cos(r).toFloat(), center.y + radius * sin(r).toFloat())
}

// ---------- Preview ----------
@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun DonutPieRoundedOverlapPreview() {
    val slices = listOf(
        PieSlice(14f, Color(0xFFB0BEC5)),   // xám nhạt
        PieSlice(22f, Color(0xFF00C853)),   // xanh đậm
        PieSlice(18f, Color(0xFF69F0AE)),   // xanh nhạt
        PieSlice(16f, Color(0xFFFF5252)),   // đỏ
        PieSlice(30f, Color(0xFFFFCDD2)),   // hồng
    )
    Box(Modifier.padding(20.dp).size(300.dp)) {
        DonutPieRoundedOverlap(
            modifier = Modifier.fillMaxSize(),
            slices = slices,
            holeRatio = 0.62f,
            roundRadius = 14.dp,
            separatorWidth = 4.dp,
            overlapWidth = 6.dp,   // tăng/giảm độ “đè lên nhau”
            startAngle = -90f,
            separatorColor = Color.White
        )
    }
}

