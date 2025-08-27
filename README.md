import androidx.compose.ui.graphics.Color

data class Position(val volume: Int, val symbol: String, val exchange: String)
data class PieData(val value: Float, val label: String, val color: Color)

fun List<Position>.toPieData(
    maxSlices: Int = 7,
    minPercent: Float = 10f,
    palette: List<Color> = listOf(
        Color(0xFF7C4DFF), Color(0xFFFF7043), Color(0xFF29B6F6),
        Color(0xFFFFCA28), Color(0xFF66BB6A), Color(0xFFEC407A),
        Color(0xFF26A69A), Color(0xFFAB47BC), Color(0xFF8D6E63)
    ),
    othersColor: Color = Color(0xFFB0BEC5)
): List<PieData> {
    val total = this.sumOf { it.volume.toLong() }
    if (total <= 0L) return emptyList()

    // Tính % và sắp xếp giảm dần
    val withPercent = this.map { p ->
        val percent = (p.volume.toFloat() / total.toFloat()) * 100f
        Triple(percent, "${p.symbol}.${p.exchange}", p)
    }.sortedByDescending { it.first }

    // Tách các mục >= minPercent
    val bigOnes = withPercent.filter { it.first >= minPercent }.toMutableList()
    val smallOnes = withPercent.filter { it.first < minPercent }

    // Nếu quá số lượng cho phép (chừa chỗ cho Others), dồn bớt vào Others
    val maxBig = maxSlices - 1 // chừa 1 slot cho Others nếu cần
    if (bigOnes.size > maxBig) {
        val overflow = bigOnes.subList(maxBig, bigOnes.size).toList()
        bigOnes.subList(maxBig, bigOnes.size).clear()
        // gộp overflow vào smallOnes logic (vì cũng sẽ thành Others)
        val mergedSmall = smallOnes + overflow
        return buildFinal(bigOnes, mergedSmall, palette, othersColor)
    }

    return buildFinal(bigOnes, smallOnes, palette, othersColor, maxSlices)
}

private fun buildFinal(
    bigOnes: List<Triple<Float, String, Position>>,
    smallOnes: List<Triple<Float, String, Position>>,
    palette: List<Color>,
    othersColor: Color,
    maxSlices: Int = 7
): List<PieData> {
    val result = mutableListOf<PieData>()

    // Add big ones
    bigOnes.forEachIndexed { index, (percent, label, _) ->
        val color = palette[index % palette.size]
        result += PieData(value = percent, label = label, color = color)
    }

    // Sum smalls (and any overflow already merged)
    val othersPercent = smallOnes.sumOf { it.first.toDouble() }.toFloat()

    // Thêm Others nếu có phần nhỏ
    if (othersPercent > 0f && result.size < maxSlices) {
        result += PieData(value = othersPercent, label = "Others", color = othersColor)
    }

    // Trong trường hợp hiếm khi bigOnes rỗng và chỉ còn Others
    if (result.isEmpty() && othersPercent > 0f) {
        result += PieData(value = othersPercent, label = "Others", color = othersColor)
    }

    // Do tính toán số thực có thể lệch nhẹ, chuẩn hoá lại để tổng ≈ 100
    val sum = result.sumOf { it.value.toDouble() }.toFloat()
    return if (sum > 0f) {
        result.map { it.copy(value = it.value * (100f / sum)) }
    } else {
        result
    }
}
