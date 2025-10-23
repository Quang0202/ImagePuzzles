class ImageDragShadow(private val imageView: ImageView) : View.DragShadowBuilder(imageView) {

    private val shadowSize = Point()
    private val touchPoint = Point()

    override fun onProvideShadowMetrics(outShadowSize: Point, outShadowTouchPoint: Point) {
        val w = imageView.width.coerceAtLeast(1)
        val h = imageView.height.coerceAtLeast(1)
        shadowSize.set(w, h)
        // điểm chạm ở giữa
        touchPoint.set(w / 2, h / 2)
        outShadowSize.set(shadowSize.x, shadowSize.y)
        outShadowTouchPoint.set(touchPoint.x, touchPoint.y)
    }

    override fun onDrawShadow(canvas: Canvas) {
        // vẽ “bóng mờ”: scale nhẹ + alpha
        canvas.save()
        canvas.scale(1.06f, 1.06f, (shadowSize.x / 2f), (shadowSize.y / 2f))
        imageView.draw(canvas)
        canvas.restore()

        // phủ 1 lớp mờ
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33000000 }
        canvas.drawRect(0f, 0f, shadowSize.x.toFloat(), shadowSize.y.toFloat(), paint)
    }
}
'''
