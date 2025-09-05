@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
package com.imagepuzzles.app.presentation



import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max
import kotlin.math.roundToInt
@Composable
fun PdfVerticalViewerUrl(
    url: String,
    title: String = "PDF",
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var handle by remember { mutableStateOf<PdfHandle?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    // mở PdfRenderer từ URL (tải về cacheDir)
    LaunchedEffect(url) {
        runCatching {
            handle?.close()
            handle = openPdfRendererFromUrl(context, url)
            error = null
        }.onFailure { e -> error = e.message ?: e.toString() }
    }

    DisposableEffect(Unit) { onDispose { handle?.close() } }

    val listState = rememberLazyListState()
    val currentPage by remember { derivedStateOf { (listState.firstVisibleItemIndex + 1).coerceAtLeast(1) } }
    val totalPages = handle?.pageCount ?: 0

    BackHandler(enabled = onBack != null) { onBack?.invoke() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("$title • $currentPage/$totalPages", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { inner ->
        when {
            error != null -> Box(
                modifier = modifier.fillMaxSize().padding(inner),
                contentAlignment = Alignment.Center
            ) { Text("Lỗi: $error") }

            handle == null -> Box(
                modifier = modifier.fillMaxSize().padding(inner),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            else -> {
                val cache = remember { PageBitmapCache(64) } // 64MB
                var contentWidthPx by remember { mutableStateOf(0) }

                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(inner)
                        .onSizeChanged { contentWidthPx = it.width }
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(count = handle!!.pageCount, key = { it }) { pageIndex ->
                            PdfPageItemZoomable(
                                renderer = handle!!.renderer,
                                pageIndex = pageIndex,
                                targetWidthPx = contentWidthPx,
                                cache = cache
                            )
                        }
                    }
                }
            }
        }
    }
}

/* =========================
   Helpers: open, cache, render
   ========================= */

private class PdfHandle(
    val pfd: ParcelFileDescriptor,
    val renderer: PdfRenderer
) : AutoCloseable {
    val pageCount get() = renderer.pageCount
    override fun close() {
        runCatching { renderer.close() }
        runCatching { pfd.close() }
    }
}

private suspend fun openPdfRendererFromUrl(context: Context, url: String): PdfHandle =
    withContext(Dispatchers.IO) {
        val file = downloadToCache(context, url)
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        PdfHandle(pfd, PdfRenderer(pfd))
    }

private suspend fun downloadToCache(context: Context, urlString: String): File =
    withContext(Dispatchers.IO) {
        val url = URL(urlString)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 30000
        }
        conn.inputStream.use { input ->
            val out = File(context.cacheDir, "pdf_${System.currentTimeMillis()}.pdf")
            FileOutputStream(out).use { output -> input.copyTo(output) }
            out
        }
    }

private class PageBitmapCache(maxSizeInMb: Int) :
    LruCache<String, Bitmap>(maxSizeInMb * 1024 * 1024) {
    override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
}

private fun cacheKey(pageIndex: Int, targetWidthPx: Int) = "p=$pageIndex|w=$targetWidthPx"

/* =========================
   Page item with ZOOM
   ========================= */

// Saver cho Offset (Bundle-able)
private val offsetSaver: Saver<Offset, List<Float>> = Saver(
    save = { listOf(it.x, it.y) },
    restore = { list -> Offset(list[0], list[1]) }
)

@Composable
private fun PdfPageItemZoomable(
    renderer: PdfRenderer,
    pageIndex: Int,
    targetWidthPx: Int,
    cache: PageBitmapCache,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    if (targetWidthPx <= 0) {
        Box(modifier = modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Render bitmap (có cache)
    var bitmap by remember(pageIndex, targetWidthPx) { mutableStateOf<Bitmap?>(null) }
    var pageHeightPx by remember(pageIndex, targetWidthPx) { mutableStateOf(0) }
    val key = remember(pageIndex, targetWidthPx) { cacheKey(pageIndex, targetWidthPx) }

    LaunchedEffect(key) {
        cache.get(key)?.let {
            bitmap = it
            pageHeightPx = it.height
            return@LaunchedEffect
        }
        val bmp = withContext(Dispatchers.IO) {
            renderer.openPage(pageIndex).use { page ->
                val pageW = page.width
                val pageH = page.height
                val scale = targetWidthPx.toFloat() / pageW.toFloat()
                val targetH = (pageH * scale).roundToInt().coerceAtLeast(1)
                Bitmap.createBitmap(targetWidthPx.coerceAtLeast(1), targetH, Bitmap.Config.ARGB_8888).also {
                    page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                }
            }
        }
        cache.put(key, bmp)
        bitmap = bmp
        pageHeightPx = bmp.height
    }

    // Zoom/Pan state (saveable)
    var scale by rememberSaveable(pageIndex) { mutableStateOf(1f) }
    var offset by rememberSaveable(pageIndex, stateSaver = offsetSaver) { mutableStateOf(Offset.Zero) }
    val minScale = 1f
    val maxScale = 4f

    // Kích thước khung để clamp pan
    var containerW by remember { mutableStateOf(0) }
    var containerH by remember { mutableStateOf(0) }

    fun clampOffset(raw: Offset): Offset {
        val contentW = targetWidthPx * scale
        val contentH = pageHeightPx * scale
        val maxX = max(0f, (contentW - containerW) / 2f)
        val maxY = max(0f, (contentH - containerH) / 2f)
        return Offset(raw.x.coerceIn(-maxX, maxX), raw.y.coerceIn(-maxY, maxY))
    }

    // Pinch-to-zoom + pan (2 ngón) — không chặn scroll 1 ngón của LazyColumn
    val tfState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(minScale, maxScale)
        val scaleChange = if (scale == 0f) 1f else newScale / scale
        val newOffset = (offset + panChange) * scaleChange
        scale = newScale
        offset = if (scale == 1f) Offset.Zero else clampOffset(newOffset)
    }

    val pageHeightDp = remember(pageHeightPx) { with(density) { pageHeightPx.toDp() } }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(pageHeightDp)
            .clipToBounds()
            .onSizeChanged { sz ->
                containerW = sz.width
                containerH = sz.height
                offset = clampOffset(offset)
            }
            // Double-tap để toggle 1× ↔ 2×; KHÔNG tiêu thụ drag 1 ngón
            .pointerInput(pageIndex) {
                detectTapGestures(
                    onDoubleTap = { tap ->
                        val target = if (scale < 2f) 2f else 1f
                        val scaleChange = target / scale
                        val newOffset = (offset) + (tap - (tap + offset)) * (scaleChange - 1f)
                        scale = target
                        offset = if (scale == 1f) Offset.Zero else clampOffset(newOffset)
                    }
                )
            }
            // Pinch-to-zoom + pan (2 ngón) — hợp tác với scroll cha
            .transformable(state = tfState),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap == null) {
            CircularProgressIndicator()
        } else {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1}",
                modifier = Modifier
                    .graphicsLayer {
                        translationX = offset.x
                        translationY = offset.y
                        scaleX = scale
                        scaleY = scale
                    }
                    .fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun PdfScreenFormUrl(){
    PdfVerticalViewerUrl(
        url = "https://arxiv.org/pdf/1706.03762.pdf",
        title = "Tài liệu"
    )
}


