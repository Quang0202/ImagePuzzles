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
 implementation "com.google.android.recaptcha:recaptcha:18.8.0"
 @HiltAndroidApp
class CustomApplication : Application() {
  lateinit var recaptchaClient: RecaptchaClient
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override fun onCreate() {
    super.onCreate()
    scope.launch {
      try {
        recaptchaClient = Recaptcha.fetchClient(
          application = this@CustomApplication,
          key = BuildConfig.RECAPTCHA_KEY_ID // đưa KEY_ID vào buildConfigField
        )
      } catch (e: RecaptchaException) {
        // TODO: log/telemetry
      }
    }
  }
}


  private val client get() = (app as CustomApplication).recaptchaClient
  var ui by mutableStateOf<UiState>(UiState.Idle); private set

  fun verifyLoginWithRecaptcha(username: String, password: String) {
    viewModelScope.launch(Dispatchers.IO) {
      ui = UiState.Loading
      try {
        // 1) Lấy token từ reCAPTCHA
        val tokenResult = client
          .execute(RecaptchaAction.LOGIN, timeout = 10_000L) // khuyến nghị ~10s
          .getOrThrow()

        // 2) Gọi backend của bạn để "assess" token và tiếp tục login
        val ok = myApi.loginWithRecaptcha(username, password, tokenResult)
        ui = if (ok) UiState.Success else UiState.Failed("Invalid credentials")
      } catch (e: Exception) {
        ui = UiState.Failed(e.message ?: "reCAPTCHA/Network error")
      }
    }



```
```
class ContentUriRequestBody(
    private val context: Context,
    private val uri: Uri,
    private val mime: String?
) : RequestBody() {
    override fun contentType() = mime?.toMediaTypeOrNull()
    override fun writeTo(sink: okio.BufferedSink) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            sink.writeAll(input.source())
        }
    }
}

private fun getFileName(resolver: ContentResolver, uri: Uri): String {
    var name = "upload"
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && c.moveToFirst()) name = c.getString(idx) ?: name
    }
    return name
}

fun Uri.toMultipart(context: Context, partName: String = "files[]"): MultipartBody.Part {
    val resolver = context.contentResolver
    val mime = resolver.getType(this) ?: "application/octet-stream"
    val fileName = getFileName(resolver, this)
    val body = ContentUriRequestBody(context, this, mime)
    return MultipartBody.Part.createFormData(partName, fileName, body)
}

fun String.toTextPlain(): RequestBody =
    RequestBody.create("text/plain".toMediaTypeOrNull(), this)
    ```
```
@Composable
fun ShrinkTextToWidthMultiLine(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 2,                          // số dòng cho phép
    style: TextStyle = LocalTextStyle.current,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val baseFontSize = style.fontSize.takeIf { it != TextUnit.Unspecified } ?: 16.sp
    var fittedFontSize by remember(text, baseFontSize, maxLines) { mutableStateOf(baseFontSize) }

    BoxWithConstraints(modifier = modifier) {
        val targetWidthPx = with(density) { maxWidth.toPx().toInt() }

        LaunchedEffect(text, targetWidthPx, baseFontSize, maxLines, style) {
            if (text.isEmpty() || targetWidthPx <= 0) {
                fittedFontSize = baseFontSize
                return@LaunchedEffect
            }

            fun fitsAt(sizeSp: Float): Boolean {
                val res = measurer.measure(
                    text = text,
                    style = style.copy(fontSize = sizeSp.sp),
                    softWrap = true,
                    maxLines = maxLines,
                    overflow = TextOverflow.Clip,
                    constraints = Constraints(maxWidth = targetWidthPx)
                )
                return !res.hasVisualOverflow && !res.didOverflowHeight
            }

            if (fitsAt(baseFontSize.value)) {
                fittedFontSize = baseFontSize
            } else {
                var lo = 1f
                var hi = baseFontSize.value
                var best = lo
                val epsilon = 0.25f
                while (hi - lo > epsilon) {
                    val mid = (lo + hi) / 2f
                    if (fitsAt(mid)) { best = mid; lo = mid } else hi = mid
                }
                fittedFontSize = best.sp
            }
        }

        Text(
            text = text,
            style = style.copy(fontSize = fittedFontSize),
            maxLines = maxLines,
            softWrap = true,
            overflow = TextOverflow.Clip,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
```
