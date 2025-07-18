package li.songe.gkd.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import li.songe.gkd.util.stopCoroutine
import li.songe.gkd.util.throttle
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class AlertDialogOptions(
    val title: @Composable (() -> Unit)? = null,
    val text: @Composable (() -> Unit)? = null,
    val onDismissRequest: (() -> Unit)? = null,
    val confirmButton: @Composable () -> Unit,
    val dismissButton: @Composable (() -> Unit)? = null,
)

private fun buildDialogOptions(
    title: @Composable (() -> Unit),
    text: @Composable (() -> Unit),
    confirmText: String,
    confirmAction: () -> Unit,
    dismissText: String? = null,
    dismissAction: (() -> Unit)? = null,
    onDismissRequest: (() -> Unit)? = null,
    error: Boolean = false,
): AlertDialogOptions {
    return AlertDialogOptions(
        title = title,
        text = text,
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = throttle(fn = confirmAction),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (error) MaterialTheme.colorScheme.error else Color.Unspecified
                )
            ) {
                Text(text = confirmText)
            }
        },
        dismissButton = if (dismissText != null && dismissAction != null) {
            {
                TextButton(
                    onClick = throttle(fn = dismissAction),
                ) {
                    Text(text = dismissText)
                }
            }
        } else {
            null
        },
    )
}

@Composable
fun BuildDialog(stateFlow: MutableStateFlow<AlertDialogOptions?>) {
    val options by stateFlow.collectAsState()
    options?.let {
        AlertDialog(
            text = it.text,
            title = it.title,
            onDismissRequest = it.onDismissRequest ?: { stateFlow.value = null },
            confirmButton = it.confirmButton,
            dismissButton = it.dismissButton,
        )
    }
}

fun MutableStateFlow<AlertDialogOptions?>.updateDialogOptions(
    title: String,
    text: String? = null,
    textContent: (@Composable (() -> Unit))? = null,
    confirmText: String = DEFAULT_IK_TEXT,
    confirmAction: (() -> Unit)? = null,
    dismissText: String? = null,
    dismissAction: (() -> Unit)? = null,
    onDismissRequest: (() -> Unit)? = null,
    error: Boolean = false,
) {
    value = buildDialogOptions(
        title = { Text(text = title) },
        text = textContent ?: { Text(text = text ?: error("miss text")) },
        confirmText = confirmText,
        confirmAction = confirmAction ?: { value = null },
        dismissText = dismissText,
        dismissAction = dismissAction ?: { value = null },
        onDismissRequest = onDismissRequest,
        error = error,
    )
}

private const val DEFAULT_IK_TEXT = "我知道了"
private const val DEFAULT_CONFIRM_TEXT = "确定"
private const val DEFAULT_DISMISS_TEXT = "取消"

suspend fun MutableStateFlow<AlertDialogOptions?>.getResult(
    title: String,
    text: String? = null,
    textContent: (@Composable (() -> Unit))? = null,
    dismissRequest: Boolean = false,
    confirmText: String = DEFAULT_CONFIRM_TEXT,
    dismissText: String = DEFAULT_DISMISS_TEXT,
    error: Boolean = false,
): Boolean {
    return suspendCoroutine { s ->
        val dismiss = {
            s.resume(false)
            this.value = null
        }
        updateDialogOptions(
            title = title,
            text = text,
            textContent = textContent,
            onDismissRequest = if (dismissRequest) dismiss else ({}),
            confirmText = confirmText,
            confirmAction = {
                s.resume(true)
                this.value = null
            },
            dismissText = dismissText,
            dismissAction = dismiss,
            error = error,
        )
    }
}

suspend fun MutableStateFlow<AlertDialogOptions?>.waitResult(
    title: String,
    text: String? = null,
    textContent: (@Composable (() -> Unit))? = null,
    dismissRequest: Boolean = false,
    confirmText: String = DEFAULT_CONFIRM_TEXT,
    dismissText: String = DEFAULT_DISMISS_TEXT,
    error: Boolean = false,
) {
    val r = getResult(
        title = title,
        text = text,
        textContent = textContent,
        dismissRequest = dismissRequest,
        confirmText = confirmText,
        dismissText = dismissText,
        error = error,
    )
    if (!r) {
        stopCoroutine()
    }
}