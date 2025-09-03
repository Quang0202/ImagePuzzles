val bubbleSize = androidx.compose.ui.geometry.Size(bubbleW, bubbleH)
val bubbleTopLeft = Offset(bubbleLeft, bubbleTop)
val cr = 12.dp.toPx()

drawIntoCanvas { canvas ->
    val paint = AndroidPaint().apply {
        color = android.graphics.Color.WHITE
        setShadowLayer(12.3f, 0f, 1f, android.graphics.Color.parseColor("#19213D1F"))
    }

    canvas.nativeCanvas.drawRoundRect(
        bubbleTopLeft.x,
        bubbleTopLeft.y,
        bubbleTopLeft.x + bubbleSize.width,
        bubbleTopLeft.y + bubbleSize.height,
        cr,
        cr,
        paint
    )
}
