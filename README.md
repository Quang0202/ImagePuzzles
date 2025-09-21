```
var layout by remember { mutableStateOf<TextLayoutResult?>(null) }

    BasicText(
        text = text,
        modifier = modifier.pointerInput(text) {
            detectTapGestures { pos ->
                val lr = layout ?: return@detectTapGestures
                val offset = lr.getOffsetForPosition(pos)
                val hit = text.getStringAnnotations(start = offset, end = offset)
                    .firstOrNull { it.tag == "ACTION" }
                if (hit != null) {
                    actions[hit.item]?.invoke()
                } else {
                    onBlankClick?.invoke()
                }
            }
        },
        onTextLayout = { layout = it }
    )
```
```
 var isFocused by remember { mutableStateOf(false) }

    BasicTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            }
            .border(
                width = 1.dp,
                color = if (isFocused) Color.Blue else Color.Gray,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp), // padding custom
        textStyle = LocalTextStyle.current.copy(
            fontSize = 16.sp,
            lineHeight = 20.sp,
            color = Color.Black
        ),
        singleLine = false,
        maxLines = Int.MAX_VALUE,
        cursorBrush = SolidColor(Color.Blue) // màu con trỏ
    )
```
