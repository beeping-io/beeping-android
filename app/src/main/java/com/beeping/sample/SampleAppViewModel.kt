package com.beeping.sample

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beeping.AndroidBeepingCore.BeepingClient
import com.beeping.AndroidBeepingCore.BeepingError
import com.beeping.AndroidBeepingCore.BeepingEvent
import com.beeping.AndroidBeepingCore.BeepingException
import com.beeping.AndroidBeepingCore.BeepingMode
import com.beeping.AndroidBeepingCore.BeepingPayload
import com.beeping.AndroidBeepingCore.BeepingTimberTree
import com.beeping.AndroidBeepingCore.LogLevel
import com.beeping.AndroidBeepingCore.ReceptionMetrics
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * BEE-64 — Holds [BeepingClient] state for the sample app, exposes a
 * [StateFlow] of [SampleUiState], and coordinates env switching, encode
 * (Send button) + decode (Listen toggle).
 *
 * The `BeepingClient` is rebuilt whenever the env changes — keeps the
 * Builder DSL honest and ensures DEV/PROD swap clears any cached HTTP state.
 */
class SampleAppViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(SampleUiState())
    val state: StateFlow<SampleUiState> = _state.asStateFlow()

    private var client: BeepingClient = buildClient(_state.value.env)
    private var listenJob: Job? = null

    fun onEnvChange(env: SampleEnv) {
        if (env == _state.value.env) return
        listenJob?.cancel()
        listenJob = null
        runCatching { client.close() }
        client = buildClient(env)
        _state.update { it.copy(env = env, listening = false, lastError = null) }
        Timber.tag("Beeping[trace=${client.traceId}]").i("env switched to ${env.name}")
    }

    fun onKeyChange(key: String) {
        _state.update { it.copy(key = key) }
    }

    fun onSendClick() {
        val key = _state.value.key.trim()
        viewModelScope.launch {
            _state.update { it.copy(busy = true, lastError = null) }
            val result = client.send(BeepingPayload(payload = key))
            result.onFailure { e ->
                _state.update { it.copy(lastError = formatError(e)) }
            }
            _state.update { it.copy(busy = false) }
        }
    }

    /**
     * BEE-2240 — encode the current key as a scheduled transmission over
     * [SCHEDULE_DURATION] s with one beep every [SCHEDULE_INTERVAL] s and play
     * it back via the configured player. Only Local mode is supported — Cloud
     * surfaces a `SchedulingNotSupported` error in the status panel.
     */
    fun onSendScheduledClick() {
        val key = _state.value.key.trim()
        viewModelScope.launch {
            _state.update { it.copy(busy = true, lastError = null) }
            val result =
                client.sendScheduled(
                    payload = BeepingPayload(payload = key),
                    duration = SCHEDULE_DURATION,
                    startTime = 0f,
                    interval = SCHEDULE_INTERVAL,
                    // Sample app surfaces the audible band for QA — easier to
                    // hear the cadence than ultrasonic. Production callers
                    // typically omit this flag (defaults to inaudible).
                    audible = true,
                )
            result.onFailure { e ->
                _state.update { it.copy(lastError = formatError(e)) }
            }
            _state.update { it.copy(busy = false) }
        }
    }

    fun onListenToggle(hasMicPermission: Boolean) {
        if (_state.value.listening) {
            stopListening()
            return
        }
        if (!hasMicPermission) {
            _state.update {
                it.copy(lastError = "RECORD_AUDIO permission not granted — tap Listen again after granting.")
            }
            return
        }
        startListening()
    }

    fun toggleConsole() {
        _state.update { it.copy(consoleOpen = !it.consoleOpen) }
    }

    fun clearError() {
        _state.update { it.copy(lastError = null) }
    }

    private fun startListening() {
        listenJob?.cancel()
        _state.update { it.copy(listening = true, lastError = null, lastDecoded = null, lastMetrics = null) }
        listenJob =
            viewModelScope.launch {
                client.listen().collect { event ->
                    when (event) {
                        is BeepingEvent.Started -> {
                            Timber.tag("Beeping[trace=${client.traceId}]").d("listen started")
                        }
                        is BeepingEvent.Decoded -> {
                            // BEE-2313: surface reception metrics in the debug console + status panel.
                            val metricsLine = formatMetrics(event.payload.metrics)
                            Timber
                                .tag("Beeping[trace=${client.traceId}]")
                                .i("decoded '${event.payload.payload}' — $metricsLine")
                            _state.update {
                                it.copy(lastDecoded = event.payload.payload, lastMetrics = metricsLine)
                            }
                        }
                        is BeepingEvent.Failed -> {
                            _state.update {
                                it.copy(
                                    listening = false,
                                    lastError = formatBeepingError(event.reason),
                                )
                            }
                        }
                        BeepingEvent.Stopped -> {
                            _state.update { it.copy(listening = false) }
                        }
                    }
                }
            }
    }

    private fun stopListening() {
        listenJob?.cancel()
        listenJob = null
        _state.update { it.copy(listening = false) }
    }

    private fun buildClient(env: SampleEnv): BeepingClient =
        BeepingClient
            .Builder(getApplication<Application>())
            .mode(env.toMode())
            .logLevel(LogLevel.DEBUG)
            .build()

    override fun onCleared() {
        super.onCleared()
        listenJob?.cancel()
        runCatching { client.close() }
    }

    /** BEE-2313: compact one-line render of [ReceptionMetrics] for the console + status panel. */
    private fun formatMetrics(metrics: ReceptionMetrics?): String {
        if (metrics == null) return "no metrics"

        fun pct(value: Float) = "${(value * 100).toInt()}%"
        val vol = "%.2f".format(metrics.receivedBeepsVolume)
        return "conf=${pct(metrics.confidence)} err=${pct(metrics.confidenceError)} " +
            "noise=${pct(metrics.confidenceNoise)} vol=$vol mode=${metrics.decodedMode}"
    }

    private fun formatError(t: Throwable): String =
        when (t) {
            is BeepingException -> formatBeepingError(t.error)
            else -> "${t::class.simpleName}: ${t.message ?: "unknown"}"
        }

    private fun formatBeepingError(error: BeepingError): String =
        when (error) {
            is BeepingError.MissingMicPermission -> "Microphone permission denied."
            is BeepingError.AudioFocusLost -> "Audio focus lost — another app took the mic."
            is BeepingError.NativeLibraryNotLoaded -> "Native lib not loaded — LOCAL mode unavailable on this build."
            is BeepingError.AuthenticationFailed -> "Auth failed — check API key for the selected env."
            is BeepingError.RateLimited -> "Rate-limited (retry after ${error.retryAfterMs} ms)."
            is BeepingError.NetworkError -> "Network error: ${error.cause.message ?: error.cause::class.simpleName}"
            is BeepingError.DecoderInternal ->
                "Decoder internal: ${error.cause.message ?: error.cause::class.simpleName}"
            is BeepingError.SchedulingNotSupported ->
                "Scheduled send is local-only — switch to LOCAL mode to use it."
        }

    /**
     * Companion exposes the live JSON log stream from [BeepingTimberTree]
     * for the debug console composable.
     */
    val logs get() = BeepingTimberTree.logs

    private companion object {
        // BEE-2240: default schedule shown in the sample app's "Send scheduled"
        // button. 10 s with a 2.3 s gap yields 5 beeps — matches the upstream
        // BEE-2238 reference example.
        private const val SCHEDULE_DURATION = 10f
        private const val SCHEDULE_INTERVAL = 2.3f
    }
}

private fun SampleEnv.toMode(): BeepingMode =
    when (this) {
        SampleEnv.LOCAL -> BeepingMode.Local
        SampleEnv.DEV ->
            BeepingMode.Cloud(
                apiKey = com.beeping.sample.BuildConfig.BEEPBOX_DEV_API_KEY,
                endpoint = com.beeping.sample.BuildConfig.BEEPBOX_DEV_BASE_URL,
            )
        SampleEnv.PROD ->
            BeepingMode.Cloud(
                apiKey = com.beeping.sample.BuildConfig.BEEPBOX_PROD_API_KEY,
                endpoint = com.beeping.sample.BuildConfig.BEEPBOX_PROD_BASE_URL,
            )
    }
