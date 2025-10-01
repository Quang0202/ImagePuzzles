```
@file:OptIn(ExperimentalStdlibApi::class)

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@Composable
fun NumberAmountField(
    modifier: Modifier = Modifier,
    value: BigDecimal = BigDecimal.ZERO,
    onValueChange: (BigDecimal) -> Unit,
    textStyle: TextStyle = LocalTextStyle.current.copy(
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Start
    ),
    placeholder: String = "0.00",
    locale: Locale = Locale.getDefault(),
) {
    // Raw text dùng khi đang edit (không group separator)
    var raw by remember { mutableStateOf(normalizeRaw(value)) }
    var focused by remember { mutableStateOf(false) }

    // Formatter hiển thị khi chưa focus
    val fmt = remember(locale) {
        DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(locale))
    }

    // Text hiển thị:
    // - Focused: dùng raw để chỉnh từng số
    // - Unfocused: format chuẩn 0.00
    val display = remember(focused, raw, value) {
        if (focused) raw.ifEmpty { "" } else fmt.format(raw.toBigDecimalOrNull() ?: value)
    }

    BasicTextField(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged {
                val wasFocused = focused
                focused = it.isFocused

                if (wasFocused && !it.isFocused) {
                    // Mất focus → chốt giá trị về scale(2) và update ra ngoài
                    val bd = toBigDecimalSafe(raw)
                    val scaled = bd.setScale(2, BigDecimal.ROUND_HALF_UP)
                    raw = normalizeRaw(scaled)
                    onValueChange(scaled)
                }
            },
        value = display,
        onValueChange = { newText ->
            if (focused) {
                val sanitized = sanitizeDecimalInput(newText)
                // Giới hạn 2 chữ số thập phân trong lúc nhập
                val limited = limitFractionDigits(sanitized, 2)
                raw = limited
                toBigDecimalSafe(limited).let { onValueChange(it) }
            }
        },
        textStyle = textStyle.merge(MaterialTheme.typography.titleMedium),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                // Khi nhấn Done, ta giả lập mất focus bằng cách chuẩn hóa raw
                val bd = toBigDecimalSafe(raw).setScale(2, BigDecimal.ROUND_HALF_UP)
                raw = normalizeRaw(bd)
                onValueChange(bd)
            }
        ),
        singleLine = true,
        decorationBox = { inner ->
            Box(Modifier.padding(vertical = 4.dp)) {
                // Placeholder khi chưa focus và giá trị 0
                val isZeroOrEmpty = (!focused) && (toBigDecimalSafe(raw).compareTo(BigDecimal.ZERO) == 0)
                if (isZeroOrEmpty && display.isEmpty()) {
                    androidx.compose.material3.Text(
                        text = placeholder,
                        style = textStyle.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    )
                }
                // Không viền, nền trong suốt — chỉ render text
                inner()
            }
        },
        // Không có background/đường viền nên không set thêm gì
        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
    )
}

/* ----------------- Helpers ----------------- */

// Chuyển chuỗi nhập thành BigDecimal an toàn
private fun toBigDecimalSafe(text: String): BigDecimal {
    return text.trim()
        .replace(",", "") // đề phòng có dấu phẩy
        .let {
            if (it.isEmpty() || it == ".") "0" else it
        }
        .toBigDecimalOrNull() ?: BigDecimal.ZERO
}

// Giữ lại chỉ [0-9] và 1 dấu '.'; không cho nhiều dấu chấm
private fun sanitizeDecimalInput(s: String): String {
    val sb = StringBuilder()
    var seenDot = false
    s.forEach { ch ->
        when {
            ch.isDigit() -> sb.append(ch)
            ch == '.' && !seenDot -> {
                sb.append(ch)
                seenDot = true
            }
            else -> Unit
        }
    }
    return sb.toString()
}

// Giới hạn số chữ số phần thập phân
private fun limitFractionDigits(s: String, maxFraction: Int): String {
    val dot = s.indexOf('.')
    if (dot == -1) return s
    val fractionLen = s.length - dot - 1
    return if (fractionLen <= maxFraction) s else s.substring(0, dot + 1 + maxFraction)
}

// Chuẩn hóa raw string từ BigDecimal (không group, có tối đa 2 chữ số thập phân, bỏ trailing 0 không cần thiết khi đang edit)
private fun normalizeRaw(bd: BigDecimal): String {
    val scaled = bd.setScale(2, BigDecimal.ROUND_HALF_UP)
    // Khi chuyển về raw để edit: nếu là .00 thì bỏ phần .00 cho dễ gõ; nếu có phần thập phân khác 0 thì giữ tối đa 2 số
    val intPart = scaled.toBigInteger().toString()
    val frac = scaled.subtract(scaled.setScale(0)).abs().movePointRight(2).toPlainString().padStart(2, '0')
    return if (frac == "00") intPart else "$intPart.${frac.trimEnd('0')}"
}

/* ----------------- Usage ----------------- */

@Composable
fun AmountDemo() {
    var amount by remember { mutableStateOf(BigDecimal.ZERO) }
    Column(Modifier.padding(16.dp)) {
        NumberAmountField(
            value = amount,
            onValueChange = { amount = it },
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                textAlign = TextAlign.Start
            )
        )
        Spacer(Modifier.height(8.dp))
        androidx.compose.material3.Text(
            "Giá trị hiện tại: ${amount.setScale(2, BigDecimal.ROUND_HALF_UP)}",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

  ```
