```
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NumberFieldSimple(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "0.00",
    textStyle: TextStyle = LocalTextStyle.current.copy(
        fontSize = 20.sp,
        textAlign = TextAlign.Start
    ),
    sanitize: Boolean = true, // nếu muốn cho nhập tự do thì set false
) {
    var focused by remember { mutableStateOf(false) }

    BasicTextField(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused },
        value = value,
        onValueChange = { new ->
            val next = if (!sanitize) new else sanitizeDecimalFree(new)
            onValueChange(next)
        },
        textStyle = textStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { /* không auto-format */ }),
        singleLine = true,
        decorationBox = { inner ->
            Box(Modifier.padding(vertical = 4.dp)) {
                if (!focused && value.isBlank()) {
                    Text(
                        text = placeholder,
                        style = textStyle.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    )
                }
                inner() // không viền, nền trong suốt
            }
        }
    )
}

/** Giữ số và một dấu '.' (không giới hạn số chữ số thập phân) */
private fun sanitizeDecimalFree(s: String): String {
    val sb = StringBuilder()
    var seenDot = false
    s.forEach { ch ->
        when {
            ch.isDigit() -> sb.append(ch)
            ch == '.' && !seenDot -> { sb.append(ch); seenDot = true }
            ch == ',' -> { /* bỏ dấu phẩy nếu user dán từ nơi có grouping */ }
            else -> Unit
        }
    }
    return sb.toString()
}

/* ------------ Demo sử dụng ------------ */
@Composable
fun NumberFieldSimpleDemo() {
    var amount by remember { mutableStateOf("") } // rỗng -> hiển thị 0.00 khi chưa focus
    Column(Modifier.padding(16.dp)) {
        NumberFieldSimple(
            value = amount,
            onValueChange = { amount = it },
            textStyle = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(8.dp))
        Text("Raw: ${amount.ifBlank { "∅" }}", style = MaterialTheme.typography.bodyMedium)
    }
}


  ```
