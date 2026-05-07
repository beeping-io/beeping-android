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
        _state.update { it.copy(listening = true, lastError = null, lastDecoded = null) }
        listenJob =
            viewModelScope.launch {
                client.listen().collect { event ->
                    when (event) {
                        is BeepingEvent.Started -> {
                            Timber.tag("Beeping[trace=${client.traceId}]").d("listen started")
                        }
                        is BeepingEvent.Decoded -> {
                            _state.update { it.copy(lastDecoded = event.payload.payload) }
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
        }

    /**
     * Companion exposes the live JSON log stream from [BeepingTimberTree]
     * for the debug console composable.
     */
    val logs get() = BeepingTimberTree.logs
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
