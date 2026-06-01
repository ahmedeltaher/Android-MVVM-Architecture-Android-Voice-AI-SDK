package com.sdk.voiceai.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun VoiceSessionPermissionGate(
    onGranted: @Composable () -> Unit,
    rationale: String = "Microphone access is required for voice conversations.",
) {
    val context = LocalContext.current

    var permissionState by remember {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        mutableStateOf(
            if (granted) PermissionStatus.GRANTED else PermissionStatus.NOT_ASKED,
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        permissionState = if (isGranted) {
            PermissionStatus.GRANTED
        } else {
            PermissionStatus.PERMANENTLY_DENIED
        }
    }

    // Re-check on each composition in case the user granted via Settings and came back
    val currentlyGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED

    if (currentlyGranted && permissionState != PermissionStatus.GRANTED) {
        permissionState = PermissionStatus.GRANTED
    }

    when (permissionState) {
        PermissionStatus.GRANTED -> {
            onGranted()
        }

        PermissionStatus.NOT_ASKED -> {
            PermissionRationaleCard(
                message = rationale,
                buttonLabel = "Grant Permission",
                onButtonClick = {
                    launcher.launch(Manifest.permission.RECORD_AUDIO)
                },
            )
        }

        PermissionStatus.PERMANENTLY_DENIED -> {
            PermissionRationaleCard(
                message = "Permission permanently denied.",
                buttonLabel = "Open Settings",
                onButtonClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                },
            )
        }
    }
}

private enum class PermissionStatus {
    GRANTED,
    NOT_ASKED,
    PERMANENTLY_DENIED,
}

@Composable
private fun PermissionRationaleCard(
    message: String,
    buttonLabel: String,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onButtonClick) {
                Text(text = buttonLabel)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VoiceSessionPermissionGateRationalePreview() {
    MaterialTheme {
        PermissionRationaleCard(
            message = "Microphone access is required for voice conversations.",
            buttonLabel = "Grant Permission",
            onButtonClick = {},
        )
    }
}
