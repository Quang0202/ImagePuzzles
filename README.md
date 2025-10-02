```
// CustomToast.kt
package com.yourpkg.ui.toast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * API hiển thị toast (center overlay) với title + content + optional action button.
 */
class ToastController internal constructor(
    private val _state: MutableState<ToastData?>
) {
    fun show(
        title: String,
        message: String,
        actionText: String? = null,
        durationMillis: Long = 2500,
        onAction: (() -> Unit)? = null,
        onDismiss: (() -> Unit)? = null
    ) {
        _state.value = ToastData(
            title = title,
            message = message,
            actionText = actionText,
            durationMillis = durationMillis,
            onAction = onAction,
            onDismiss = onDismiss
        )
    }

    fun dismiss() {
        _state.value = null
    }
}

data class ToastData(
    val title: String,
    val message: String,
    val actionText: String? = null,
    val durationMillis: Long = 2500,
    val onAction: (() -> Unit)? = null,
    val onDismiss: (() -> Unit)? = null
)

/**
 * Gắn vào cây UI ở level cao (ví dụ trong Scaffold) để làm host cho toast.
 */
@Composable
fun rememberToastController(): ToastController {
    val state = remember { mutableStateOf<ToastData?>(null) }
    return remember { ToastController(state) }
}

@Composable
fun ToastHost(
    controller: ToastController,
    // Layout + style options:
    maxWidth: Dp = 360.dp,
    cornerRadius: Dp = 16.dp,
    innerPadding: Dp = 16.dp,
    verticalOffset: Dp = 0.dp, // chỉnh nếu muốn lệch trên/dưới
) {
    // lấy state internal
    val stateField = remember { mutableStateOf<ToastData?>(null) }
    // bridge controller -> host state
    LaunchedEffect(controller) {
        // reflect controller's internal state via reflection to keep it simple:
        // Ở trên ta giữ cùng instance state (truyền reference) nên không cần observe.
    }
    // Trick: controller giữ reference tới stateField
    SideEffect {
        // Hack nhỏ: gán đúng reference (chỉ chạy 1 lần)
        val field = ToastController::class.java.getDeclaredField("_state")
        field.isAccessible = true
        field.set(controller, stateField)
    }

    val toast = stateField.value
    val scope = rememberCoroutineScope()
    var autoDismissJob by remember { mutableStateOf<Job?>(null) }

    // Tự ẩn theo duration
    LaunchedEffect(toast) {
        autoDismissJob?.cancel()
        toast?.let {
            autoDismissJob = scope.launch {
                // Đợi một nhịp để animation in trước
                delay(50)
                delay(it.durationMillis)
                stateField.value = null
                it.onDismiss?.invoke()
            }
        }
    }

    // Overlay đặt cuối cây để phủ lên UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = toast != null,
            enter = fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.96f, animationSpec = tween(220)),
            exit = fadeOut(animationSpec = tween(180)) +
                    scaleOut(targetScale = 0.96f, animationSpec = tween(180))
        ) {
            if (toast != null) {
                ToastCard(
                    data = toast,
                    maxWidth = maxWidth,
                    cornerRadius = cornerRadius,
                    innerPadding = innerPadding,
                    modifier = Modifier
                        .offset(y = verticalOffset)
                ) {
                    // Action click
                    toast.onAction?.invoke()
                    // Thường toast đóng sau khi action
                    stateField.value = null
                    toast.onDismiss?.invoke()
                }
            }
        }
    }
}

@Composable
private fun ToastCard(
    data: ToastData,
    maxWidth: Dp,
    cornerRadius: Dp,
    innerPadding: Dp,
    modifier: Modifier = Modifier,
    onActionClick: () -> Unit
) {
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier
            .widthIn(max = maxWidth)
            .wrapContentWidth()
            .shadow(24.dp, shape = RoundedCornerShape(cornerRadius))
            .background(surface.copy(alpha = 0.98f), RoundedCornerShape(cornerRadius))
            .padding(innerPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = data.title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = onSurface
        )
        Text(
            text = data.message,
            style = MaterialTheme.typography.bodyMedium,
            color = onSurface.copy(alpha = 0.9f)
        )
        if (!data.actionText.isNullOrBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = onActionClick) {
                    Text(data.actionText!!)
                }
            }
        }
    }
}

@Composable
fun ScreenWithToast() {
    val toast = rememberToastController()

    Box(Modifier.fillMaxSize()) {
        // Nội dung màn hình
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(48.dp))
            Button(onClick = {
                toast.show(
                    title = "Đã lưu",
                    message = "Cấu hình của bạn đã được cập nhật.",
                    actionText = "Hoàn tác",
                    durationMillis = 3000,
                    onAction = { /* handle undo */ }
                )
            }) {
                Text("Show toast")
            }
        }

        // Host overlay – đặt cuối cùng để phủ lên
        ToastHost(controller = toast)
    }
}

  ```
