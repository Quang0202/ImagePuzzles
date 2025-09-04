```
@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.pdfreader

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.min

// ---------- Entry screen: chọn file hoặc nhập URL ----------
@Composable
fun PdfReaderScreen(
    defaultUrl: String? = null
) {
    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    var url by remember { mutableStateOf(defaultUrl ?: "") }

    val pickPdf = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> if (uri != null) pdfUri = uri }
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("Compose PDF Reader") }) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { pickPdf.launch(arrayOf("application/pdf")) }) {
                    Text("Open PDF (SAF)")
                }
                Spacer(Modifier.weight(1f))
            }

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("PDF URL (https://...)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                enabled = url.startsWith("http"),
                onClick = { pdfUri = null } // reset SAF để ưu tiên URL
            ) { Text("Open from URL") }

            Divider()

            Box(Modifier.fillMaxSize()) {
                when {
                    url.startsWith("http") -> PdfReaderFromUrl(pdfUrl = url)
                    pdfUri != null -> PdfReader(uri = pdfUri!!)
                    else -> HintEmpty()
                }
            }
        }
    }
}

@Composable private fun HintEmpty() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Chọn file PDF hoặc nhập URL.", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text("Hỗ trợ cuộn trang, zoom/pan, cache bitmap.", color = Color.Gray)
        }
    }
}

// ---------- Case: URL → tải về cache → đọc giống file local ----------
@Composable
fun PdfReaderFromUrl(pdfUrl: String, pageTargetWidthDp: Int = 1200) {
    val context = LocalContext.current
    var progress by remember { mutableFloatStateOf(0f) }
    var error by remember { mutableStateOf<String?>(null) }

    val pdfFile by produceState<File?>(initialValue = null, pdfUrl) {
        error = null
        progress = 0f
        value = try {
            downloadPdfToCache(
                url = pdfUrl,
                cacheDir = File(context.cacheDir, "pdf_cache").apply { mkdirs() },
                onProgress = { cur, total ->
                    if (total > 0L) progress = (cur.toFloat() / total).coerceIn(0f, 1f)
                }
            )
        } catch (e: Exception) {
            error = e.message ?: "Download error"
            null
        }
    }

    when {
        error != null -> ErrorBox("Không tải được PDF\n$error\n$url = $pdfUrl")
        pdfFile == null -> LoadingBox(progress)
        else -> PdfReader(uri = Uri.fromFile(pdfFile), pageTargetWidthDp = pageTargetWidthDp)
    }
}

@Composable private fun LoadingBox(progress: Float) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val p = if (progress in 0f..1f) progress else -1f
            CircularProgressIndicator(progress = p)
            Spacer(Modifier.height(8.dp))
            Text(
                if (progress in 0f..1f) "Đang tải ${(progress * 100).toInt()}%"
                else "Đang chuẩn bị…"
            )
        }
    }
}

@Composable private fun ErrorBox(msg: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(msg, color = Color.Red)
    }
}

// ---------- Case: File/Uri local (SAF hoặc cache) ----------
@Composable
fun PdfReader(uri: Uri, pageTargetWidthDp: Int = 1200) {
    val context = LocalContext.current
    val resolver = context.contentResolver

    // PdfRenderer cần file thật → copy content:// vào cache tạm.
    val pdfFile by produceState<File?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) { uriToTempPdfFile(resolver, uri) }
    }
    if (pdfFile == null) {
        LoadingBox(progress = -1f); return
    }

    val pdfRenderer = remember(pdfFile) { openPdfRenderer(pdfFile!!) }
    DisposableEffect(pdfRenderer) { onDispose { pdfRenderer?.close() } }
    if (pdfRenderer == null) { ErrorBox("Không mở được PDF"); return }

    val pageCount = pdfRenderer.pageCount
    val pagerState = rememberPagerState { pageCount }
    val cache = remember { PageBitmapCache(maxEntries = min(24, max(6, pageCount))) }

    HorizontalPager(
        state = pagerState,
        beyondBoundsPageCount = 1,
        key = { it },
        pageSpacing = 8.dp,
        modifier = Modifier.fillMaxSize().background(Color(0xFF101114)).padding(vertical = 8.dp)
    ) { pageIndex ->
        val targetWidthPx = with(LocalContext.current.resources.displayMetrics) {
            (pageTargetWidthDp * density).toInt()
        }
        val bitmap by produceState<Bitmap?>(initialValue = null, pdfRenderer, pageIndex) {
            value = cache[pageIndex] ?: withContext(Dispatchers.IO) {
                renderPdfPageToBitmap(pdfRenderer, pageIndex, targetWidthPx)?.also {
                    cache.put(pageIndex, it)
                }
            }
        }
        if (bitmap == null) LoadingBox(-1f)
        else ZoomableBitmap(bitmap, Modifier.fillMaxSize().padding(horizontal = 12.dp).clipToBounds())
    }
}

// ---------- Zoom/Pan 1 trang ----------
@Composable
private fun ZoomableBitmap(bitmap: Bitmap, modifier: Modifier = Modifier) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val minScale = 1f
    val maxScale = 5f

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(minScale, maxScale)
        val factor = newScale / scale
        scale = newScale
        offsetX += panChange.x * factor
        offsetY += panChange.y * factor
    }

    Box(
        modifier = modifier.background(Color(0xFF1A1B20)).transformable(transformState),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.graphicsLayer {
                scaleX = scale; scaleY = scale
                translationX = offsetX; translationY = offsetY
            }
        )
    }
}

// ---------- Helpers: PdfRenderer, render trang, cache ----------
private fun openPdfRenderer(file: File): PdfRenderer? = try {
    val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    PdfRenderer(pfd)
} catch (_: Throwable) { null }

private suspend fun uriToTempPdfFile(resolver: ContentResolver, uri: Uri): File? =
    withContext(Dispatchers.IO) {
        try {
            val input = resolver.openInputStream(uri) ?: return@withContext null
            val outFile = File.createTempFile("pdf_", ".pdf")
            input.use { ins -> FileOutputStream(outFile).use { outs -> ins.copyTo(outs) } }
            outFile
        } catch (_: Throwable) { null }
    }

private fun renderPdfPageToBitmap(
    renderer: PdfRenderer,
    pageIndex: Int,
    targetWidthPx: Int
): Bitmap? = try {
    renderer.openPage(pageIndex).use { page ->
        val ratio = targetWidthPx.toFloat() / page.width
        val targetHeightPx = (page.height * ratio).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(targetWidthPx, targetHeightPx, Bitmap.Config.ARGB_8888)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        bitmap
    }
} catch (_: Throwable) { null }

private class PageBitmapCache(maxEntries: Int) {
    private val cache = object : LruCache<Int, Bitmap>(maxEntries) {
        override fun sizeOf(key: Int, value: Bitmap): Int = 1
    }
    operator fun get(key: Int): Bitmap? = cache.get(key)
    fun put(key: Int, value: Bitmap) { cache.put(key, value) }
}

// ---------- HTTP download + cache file theo URL ----------
private val http by lazy {
    OkHttpClient.Builder().retryOnConnectionFailure(true).build()
}

@Throws(Exception::class)
private fun downloadPdfToCache(
    url: String,
    cacheDir: File,
    onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> }
): File {
    val outFile = File(cacheDir, "${sha1(url)}.pdf")
    if (outFile.exists() && outFile.length() > 0) return outFile

    val req = Request.Builder().url(url).header("Accept", "application/pdf").build()
    http.newCall(req).execute().use { resp ->
        if (!resp.isSuccessful) error("HTTP ${resp.code}")
        val body = resp.body ?: error("Empty body")
        val total = body.contentLength()
        outFile.outputStream().use { outs ->
            body.byteStream().use { ins ->
                val buf = ByteArray(DEFAULT_BUFFER_SIZE)
                var read: Int; var sum = 0L
                while (true) {
                    read = ins.read(buf)
                    if (read == -1) break
                    outs.write(buf, 0, read)
                    sum += read; onProgress(sum, total)
                }
                outs.flush()
            }
        }
    }
    return outFile
}

private fun sha1(text: String): String {
    val md = MessageDigest.getInstance("SHA-1")
    return md.digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
}
```
