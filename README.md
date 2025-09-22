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
val context = LocalContext.current
    val mediaList = remember { mutableStateListOf<Uri>() }

    // Launcher cho phép chọn nhiều file ảnh/video
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            mediaList.addAll(uris)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text("Image/Video", color = Color.White)

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            // Nút thêm file
            item {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                        .clickable {
                            launcher.launch(arrayOf("image/*", "video/*"))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = "Add",
                        tint = Color.White
                    )
                }
            }

            // Hiển thị danh sách file đã chọn
            items(mediaList.size) { index ->
                val uri = mediaList[index]
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    AsyncImage( // từ coil-compose
                        model = uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )

                    // Icon xóa
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(Color.Red, CircleShape)
                            .padding(4.dp)
                            .clickable {
                                mediaList.remove(uri)
                            }
                    )
                }
            }
        }
    }
```
