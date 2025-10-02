```
val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(fontSize = 16.sp)

    // đo width của text hiện tại
    val textLayoutResult = remember(value) {
        textMeasurer.measure(
            text = AnnotatedString(value.ifEmpty { " " }),
            style = textStyle
        )
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = textStyle,
        modifier = Modifier.width(with(LocalDensity.current) { textLayoutResult.size.width.toDp() })
    )


  ```
