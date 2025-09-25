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
settings.javaScriptCanOpenWindowsAutomatically = true
        settings.setSupportMultipleWindows(true)
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        WebView.setWebContentsDebuggingEnabled(true)

        webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                // mở challenge trong cùng WebView
                val transport = resultMsg?.obj as WebView.WebViewTransport
                transport.webView = this@apply
                resultMsg.sendToTarget()
                return true
            }
            override fun onConsoleMessage(cm: ConsoleMessage): Boolean {
                Log.d("ReCaptcha", "${cm.message()} @${cm.sourceId()}:${cm.lineNumber()}")
                return true
            }
        }
```
