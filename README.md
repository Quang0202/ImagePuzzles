```
implementation("io.coil-kt:coil-compose:2.5.0")
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
fun String.toTextRequestBody(): RequestBody =
    RequestBody.create("text/plain".toMediaTypeOrNull(), this)

suspend fun ContentResolver.uriToPart(
    uri: Uri,
    partName: String,
    fileName: String? = null
): MultipartBody.Part {
    val type = getType(uri) ?: "application/octet-stream"
    val name = fileName ?: queryDisplayName(this, uri) ?: "file.bin"
    val body = contentUriRequestBody(this, uri, type)
    return MultipartBody.Part.createFormData(partName, name, body)
}

// RequestBody đọc từ content:// Uri (không cần copy ra File)
fun contentUriRequestBody(
    resolver: ContentResolver,
    uri: Uri,
    mime: String
): RequestBody = object : RequestBody() {
    override fun contentType() = mime.toMediaTypeOrNull()
    override fun writeTo(sink: BufferedSink) {
        resolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                sink.write(buffer, 0, read)
            }
        }
    }
}

// Lấy display name từ Uri
fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
    val projection = arrayOf(android.provider.OpenableColumns.DISPLAY_NAME)
    resolver.query(uri, projection, null, null, null)?.use { c ->
        val idx = c.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME)
        if (c.moveToFirst()) return c.getString(idx)
    }
    return null
}


fun upload(
        resolver: ContentResolver,
        userId: String,
        note: String,
        avatarUri: Uri?,              // 1 ảnh đại diện
        attachmentUris: List<Uri>     // nhiều file
    ) {
        viewModelScope.launch {
            _state.value = UploadUiState.Uploading
            try {
                val avatarPart = avatarUri?.let {
                    resolver.uriToPart(it, partName = "avatar")
                }
                val attachParts: List<MultipartBody.Part> = attachmentUris.map { uri ->
                    resolver.uriToPart(uri, partName = "attachments")
                }

                val resp = ApiProvider.api.uploadProfile(
                    userId = userId.toTextRequestBody(),
                    note = note.toTextRequestBody(),
                    avatar = avatarPart,
                    attachments = attachParts
                )
                _state.value = UploadUiState.Success(resp.message)
            } catch (e: Exception) {
                _state.value = UploadUiState.Error(e.message ?: "Upload failed")
            }
        }
    }
```
