val bubbleSize = androidx.compose.ui.geometry.Size(bubbleW, bubbleH)
val bubbleTL = Offset(bubbleLeft, bubbleTop)
val r = 12.dp.toPx()

// Tham số “xấp xỉ” box-shadow: 0px 1px 12.3px 6px #19213D1F
// -> ta giả lập bằng 3 lớp: offset Y ~1..3dp, "spread" tăng dần, alpha giảm dần
val layers = listOf(
    Triple(1.dp.toPx(), 6.dp.toPx(), 0.12f), // dy, spread, alpha
    Triple(2.dp.toPx(), 10.dp.toPx(), 0.08f),
    Triple(3.dp.toPx(), 14.dp.toPx(), 0.05f),
)

layers.forEach { (dy, spread, a) ->
    drawRoundRect(
        color = Color(0xFF19213D).copy(alpha = a),
        topLeft = bubbleTL + Offset(0f, dy) - Offset(spread / 2, spread / 2),
        size = androidx.compose.ui.geometry.Size(
            bubbleSize.width + spread,
            bubbleSize.height + spread
        ),
        cornerRadius = CornerRadius(r + spread / 2, r + spread / 2)
    )
}

// Tooltip trắng
drawRoundRect(
    color = Color.White,
    topLeft = bubbleTL,
    size = bubbleSize,
    cornerRadius = CornerRadius(r, r)
)
