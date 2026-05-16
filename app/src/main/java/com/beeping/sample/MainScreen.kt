package com.beeping.sample

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

/**
 * BEE-64 — Main screen.
 *
 * Layout (top → bottom):
 *
 * 1. App bar with title.
 * 2. Logo (5-tap target → toggles the debug console).
 * 3. Env segmented selector (LOCAL / DEV / PROD).
 * 4. Key text field (default `abc12`, validated by SDK on send).
 * 5. Send button — encodes + plays via `BeepingClient.send()`.
 * 6. Listen toggle — decodes mic via `BeepingClient.listen()`.
 * 7. Status panel (last decoded / last error).
 * 8. Debug console (overlay) when `consoleOpen`.
 *
 * `@Suppress("LongMethod")` is intentional: a Compose tree reads better inline
 * than split into helper composables that only get called once.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongMethod")
fun MainScreen(viewModel: SampleAppViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val micPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted -> viewModel.onListenToggle(granted) }

    fun toggleListen() {
        val granted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
        if (granted || state.listening) {
            viewModel.onListenToggle(granted)
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Beeping Sample") })
        },
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LogoTapTarget(onFiveTaps = { viewModel.toggleConsole() })

                EnvSelector(
                    selected = state.env,
                    onSelect = viewModel::onEnvChange,
                )

                OutlinedTextField(
                    value = state.key,
                    onValueChange = viewModel::onKeyChange,
                    label = { Text("Key (5-char base32)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Button(
                    onClick = { viewModel.onSendClick() },
                    enabled = !state.busy && state.key.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.size(8.dp))
                    }
                    Text("Send")
                }

                Button(
                    onClick = { viewModel.onSendScheduledClick() },
                    enabled = !state.busy && state.key.isNotBlank() && state.env == SampleEnv.LOCAL,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Send scheduled (10s @ 2.3s, LOCAL)")
                }

                Button(
                    onClick = { toggleListen() },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                if (state.listening) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                        ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (state.listening) "Stop listening" else "Listen")
                }

                StatusPanel(state = state, onClearError = viewModel::clearError)
            }

            if (state.consoleOpen) {
                DebugConsole(
                    logSource = viewModel.logs,
                    onDismiss = viewModel::toggleConsole,
                )
            }
        }
    }
}

@Composable
private fun LogoTapTarget(onFiveTaps: () -> Unit) {
    var taps by remember { mutableIntStateOf(0) }
    LaunchedEffect(taps) {
        if (taps in 1..4) {
            delay(LOGO_TAP_RESET_MS)
            taps = 0
        } else if (taps >= LOGO_TAP_THRESHOLD) {
            onFiveTaps()
            taps = 0
        }
    }
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier =
            Modifier
                .size(96.dp)
                .clickable { taps += 1 },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.GraphicEq,
                contentDescription = "Beeping logo (tap 5× for debug console)",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(48.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnvSelector(
    selected: SampleEnv,
    onSelect: (SampleEnv) -> Unit,
) {
    val options = SampleEnv.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, env ->
            SegmentedButton(
                selected = env == selected,
                onClick = { onSelect(env) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = { Text(env.name) },
            )
        }
    }
}

@Composable
private fun StatusPanel(
    state: SampleUiState,
    onClearError: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Env: ${state.env.name}", style = MaterialTheme.typography.labelMedium)
            Text(
                "Listening: ${if (state.listening) "ON" else "off"}",
                style = MaterialTheme.typography.labelMedium,
            )
            state.lastDecoded?.let {
                Text(
                    "Last decoded: $it",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            state.lastError?.let { err ->
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "✕",
                        color = MaterialTheme.colorScheme.error,
                        modifier =
                            Modifier
                                .background(
                                    MaterialTheme.colorScheme.errorContainer,
                                    RoundedCornerShape(8.dp),
                                ).clickable { onClearError() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

private const val LOGO_TAP_RESET_MS = 1_000L
private const val LOGO_TAP_THRESHOLD = 5
