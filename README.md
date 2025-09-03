drawRoundRect(
    color = Color.Black.copy(alpha = 0.2f),   // bóng mờ
    topLeft = bubbleTopLeft + Offset(2.dp.toPx(), 2.dp.toPx()), // lệch 2dp
    size = bubbleSize,
    cornerRadius = cr
)

// ---- Tooltip trắng ----
drawRoundRect(
    color = Color.White,
    topLeft = bubbleTopLeft,
    size = bubbleSize,
    cornerRadius = cr
)
