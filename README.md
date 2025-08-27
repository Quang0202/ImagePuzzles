import androidx.compose.ui.graphics.Color

data class Position(val volume: Int, val symbol: String, val exchange: String)
data class PieData(val value: Float, val label: String, val color: Color)

fun toPieData(
    positions: List<Position>,
    maxSlices: Int = 7,
    minShare: Float = 0.10f // ngưỡng 10% (dùng tỉ lệ để so sánh)
): List<PieData> {
    if (positions.isEmpty()) return emptyList()

    val total = positions.fold(0L) { acc, p -> acc + p.volume.toLong() }
    if (total <= 0L) return emptyList()

    val palette = listOf(
        Color(0xFFE57373),
        Color(0xFF64B5F6),
        Color(0xFF81C784),
        Color(0xFFFFB74D),
        Color(0xFFBA68C8),
        Color(0xFFFF8A65),
        Color(0xFF4DB6AC),
        Color(0xFFA1887F),
        Color(0xFF90A4AE)
    )

    // Tính tỉ lệ 0..1
    val shares = positions.map { p ->
        val share = p.volume.toFloat() / total.toFloat()
        p.symbol + "." + p.exchange to share
    }.sortedByDescending { it.second }

    val (majors, minors) = shares.partition { it.second >= minShare }
    val roomForMajors = maxSlices - 1
    val keptMajors = if (majors.size > roomForMajors) majors.take(roomForMajors) else majors
    val overflowMajors = if (majors.size > roomForMajors) majors.drop(roomForMajors) else emptyList()

    val othersValue = (minors + overflowMajors).sumOf { it.second.toDouble() }.toFloat()

    val rawResult = buildList<Pair<String, Float>> {
        addAll(keptMajors)
        if (othersValue > 0f) add("Others" to othersValue)
    }.take(maxSlices)

    // Chuyển sang % và scale sao cho tổng = 100
    val sum = rawResult.sumOf { it.second.toDouble() }.toFloat()
    val scale = if (sum == 0f) 100f else 100f / sum

    val scaled = rawResult.mapIndexed { index, (label, value) ->
        PieData(
            value = value * scale, // giá trị %
            label = label,
            color = palette[index % palette.size]
        )
    }.toMutableList()

    // Điều chỉnh sai số vào phần tử cuối
    if (scaled.isNotEmpty()) {
        val adjustedSum = scaled.dropLast(1).sumOf { it.value.toDouble() }.toFloat()
        val lastValue = 100f - adjustedSum
        scaled[scaled.lastIndex] = scaled.last().copy(value = lastValue.coerceAtLeast(0f))
    }

    return scaled
}
