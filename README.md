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
