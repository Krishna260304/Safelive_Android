package com.safelive.app.presentation.main

import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.safelive.app.data.websocket.SocketEvent
import com.safelive.app.ui.theme.*
import kotlinx.coroutines.flow.SharedFlow

// ---- Domain model ----

data class AppWarning(
    val warningId: String,
    val title: String,
    val message: String,
    /** Severity assigned EXCLUSIVELY by backend AI. Android does not modify this. */
    val severity: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionLabel: String? = null,
    val actionRoute: String? = null
)

private fun SocketEvent.BackendWarning.toAppWarning() = AppWarning(
    warningId = warningId,
    title = title,
    message = message,
    severity = severity,
    timestamp = timestamp,
    actionLabel = actionLabel,
    actionRoute = actionRoute
)

// ---- Host composable ----

/**
 * Place this composable once at the root of the app (inside MainActivity's setContent).
 * It observes [socketEvents] for BackendWarning events and shows appropriate UI based on
 * backend-assigned severity — LOW, MEDIUM, or HIGH.
 *
 * Severity interpretation (display only — NOT determined by Android):
 * - LOW: Material AlertDialog (dismissible)
 * - MEDIUM: ModalBottomSheet + haptic feedback
 * - HIGH: Full-screen dialog + sound + vibration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarningPopupHost(
    socketEvents: SharedFlow<SocketEvent>,
    onNavigateTo: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var currentWarning by remember { mutableStateOf<AppWarning?>(null) }

    // Collect warnings from socket
    LaunchedEffect(socketEvents) {
        socketEvents.collect { event ->
            if (event is SocketEvent.BackendWarning) {
                currentWarning = event.toAppWarning()
                triggerSeverityFeedback(context, event.severity)
            }
        }
    }

    val warning = currentWarning ?: return

    when (warning.severity) {
        "HIGH" -> HighSeverityWarningDialog(
            warning = warning,
            onDismiss = { currentWarning = null },
            onAction = { route ->
                currentWarning = null
                onNavigateTo(route)
            }
        )
        "MEDIUM" -> MediumSeverityWarningSheet(
            warning = warning,
            onDismiss = { currentWarning = null },
            onAction = { route ->
                currentWarning = null
                onNavigateTo(route)
            }
        )
        else -> LowSeverityWarningDialog(
            warning = warning,
            onDismiss = { currentWarning = null }
        )
    }
}

private fun triggerSeverityFeedback(context: Context, severity: String) {
    @Suppress("DEPRECATION")
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    when (severity) {
        "HIGH" -> {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200, 100, 400), -1))
            try {
                val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                val ringtone = RingtoneManager.getRingtone(context, notification)
                ringtone?.play()
            } catch (_: Exception) {}
        }
        "MEDIUM" -> {
            vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        }
        else -> { /* LOW — no feedback */ }
    }
}

// ---- HIGH severity: Full-screen dialog ----

@Composable
private fun HighSeverityWarningDialog(
    warning: AppWarning,
    onDismiss: () -> Unit,
    onAction: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = {},  // Not dismissible by back press for HIGH severity
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        containerColor = DangerRed.copy(alpha = 0.97f),
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        },
        title = {
            Text(
                text = warning.title,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Surface(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "⚠️ CRITICAL ALERT",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = warning.message,
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            if (warning.actionLabel != null && warning.actionRoute != null) {
                Button(
                    onClick = { onAction(warning.actionRoute) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = DangerRed
                    )
                ) { Text(warning.actionLabel, fontWeight = FontWeight.Bold) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Acknowledge", color = Color.White)
            }
        }
    )
}

// ---- MEDIUM severity: Bottom sheet ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediumSeverityWarningSheet(
    warning: AppWarning,
    onDismiss: () -> Unit,
    onAction: (String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(WarningOrange.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.NotificationImportant, null, tint = WarningOrange, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(warning.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Surface(
                color = WarningOrange.copy(alpha = 0.1f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    "MEDIUM PRIORITY",
                    color = WarningOrange,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                warning.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            if (warning.actionLabel != null && warning.actionRoute != null) {
                Button(
                    onClick = { onAction(warning.actionRoute) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = WarningOrange)
                ) { Text(warning.actionLabel, color = Color.White) }
                Spacer(Modifier.height(8.dp))
            }
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Dismiss") }
        }
    }
}

// ---- LOW severity: Standard AlertDialog ----

@Composable
private fun LowSeverityWarningDialog(
    warning: AppWarning,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Info, null, tint = PrimaryBlue)
        },
        title = { Text(warning.title, fontWeight = FontWeight.Bold) },
        text = { Text(warning.message) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}
