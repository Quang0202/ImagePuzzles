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

<script src="https://www.google.com/recaptcha/api.js?onload=onloadRecaptcha&render=explicit&hl=en" async defer></script>
function onloadRecaptcha() {
    grecaptcha.render('captcha', {
      sitekey: '__SITE_KEY__',
      callback: onDataCallback,
      'expired-callback': onDataExpiredCallback,
      'error-callback': onDataErrorCallback
    });
  }
settings.javaScriptCanOpenWindowsAutomatically = true
settings.setSupportMultipleWindows(true)
// nếu trang có lẫn http/https khi test
settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

CookieManager.getInstance().setAcceptCookie(true)
CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)

                    // Lấy outerHTML của trang cha
                    fun capture() {
                        view.evaluateJavascript(
                            "(function(){return document.documentElement.outerHTML;})()"
                        ) { raw ->
                            // Kết quả trả về là chuỗi JSON (bọc quote + escape)
                            val html = raw
                                ?.removePrefix("\"")
                                ?.removeSuffix("\"")
                                ?.replace("\\u003C", "<")
                                ?.replace("\\n", "\n")
                                ?.replace("\\t", "\t")
                                ?.replace("\\\"", "\"")
                                ?.replace("\\\\", "\\")
                                ?: ""

                            onHtmlCaptured(html)
                        }
                    }

                    // Nhiều trang render thêm bằng JS → đợi 1 chút rồi chụp
                    view.postDelayed({ capture() }, 500)

                    // (Tuỳ chọn) lặp lại vài lần nếu chờ 1 selector xuất hiện
                    // ví dụ chờ badge reCAPTCHA
                    // retryUntil(".grecaptcha-badge" in DOM) rồi capture
                }
            }
```
