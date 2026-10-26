package com.personaltracker.platform.voice

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Native Android [SpeechRecognizer] wrapper managing on-device and online speech recognition
 * with automatic network failover and RAM-only buffer purging (FR-7, AD-1, AD-8, NFR-2, NFR-4).
 *
 * Prioritizes on-device offline recognition via [SpeechRecognizer.createOnDeviceSpeechRecognizer]
 * on API 31+ and [RecognizerIntent.EXTRA_PREFER_OFFLINE] to preserve local-first privacy (AD-8).
 */
@Singleton
open class SpeechRecognitionManager @Inject constructor(
    @ApplicationContext internal val context: Context? = null,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) {
    private val _isListening = MutableStateFlow(false)
    open val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private var activeRecognizer: SpeechRecognizer? = null

    /**
     * Checks if RECORD_AUDIO permission has been granted.
     */
    open fun hasRecordPermission(): Boolean {
        val ctx = context ?: return false
        return ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if Android SpeechRecognizer service is available on the device.
     */
    open fun isRecognitionAvailable(): Boolean {
        val ctx = context ?: return false
        return runCatching { SpeechRecognizer.isRecognitionAvailable(ctx) }.getOrDefault(false)
    }

    /**
     * Checks if on-device speech recognition is supported (Android 12+ / API 31+).
     */
    open fun isOnDeviceRecognitionAvailable(): Boolean {
        val ctx = context ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { SpeechRecognizer.isOnDeviceRecognitionAvailable(ctx) }.getOrDefault(false)
        } else {
            false
        }
    }

    /**
     * Streams [SpeechRecognitionState] events during speech recognition.
     * Raw audio buffers received in callbacks are unconditionally zeroed in memory (AD-8).
     *
     * @param preferOffline When true, prefers on-device offline recognition engine.
     */
    @SuppressLint("MissingPermission")
    open fun startListening(
        preferOffline: Boolean = true
    ): Flow<SpeechRecognitionState> = callbackFlow {
        val ctx = context
        if (ctx == null) {
            trySend(SpeechRecognitionState.Error(SpeechRecognitionError.CLIENT, "Context not available"))
            close()
            return@callbackFlow
        }

        if (!hasRecordPermission()) {
            trySend(
                SpeechRecognitionState.Error(
                    SpeechRecognitionError.INSUFFICIENT_PERMISSIONS,
                    "Microphone permission required for voice capture."
                )
            )
            close()
            return@callbackFlow
        }

        var sessionRecognizer: SpeechRecognizer? = null

        withContext(mainDispatcher) {
            try {
                // Teardown any existing recognizer
                activeRecognizer?.runCatching {
                    stopListening()
                    cancel()
                    destroy()
                }

                val instance = createSpeechRecognizerInstance(ctx, preferOffline)
                sessionRecognizer = instance
                activeRecognizer = instance

                val listener = object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        trySend(SpeechRecognitionState.Listening(0f))
                    }

                    override fun onBeginningOfSpeech() {
                        trySend(SpeechRecognitionState.Listening(0.1f))
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        val normalized = normalizeRms(rmsdB)
                        trySend(SpeechRecognitionState.Listening(normalized))
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {
                        // Purge in-memory audio bytes immediately to prevent leaks (AD-8)
                        buffer?.let { QuickCaptureVoiceRecorder.purgeBuffer(it) }
                    }

                    override fun onEndOfSpeech() {
                        trySend(SpeechRecognitionState.Processing)
                    }

                    override fun onError(errorCode: Int) {
                        _isListening.value = false
                        val error = SpeechRecognitionError.fromErrorCode(errorCode)
                        val message = getErrorMessage(error)
                        trySend(SpeechRecognitionState.Error(error, message))
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull().orEmpty()
                        trySend(SpeechRecognitionState.Result(text = text, isFinal = true))
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partialText = matches?.firstOrNull().orEmpty()
                        if (partialText.isNotBlank()) {
                            trySend(SpeechRecognitionState.Result(text = partialText, isFinal = false))
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                }

                instance.setRecognitionListener(listener)
                val intent = createRecognizerIntent(preferOffline)
                instance.startListening(intent)
                _isListening.value = true
            } catch (e: Throwable) {
                _isListening.value = false
                trySend(
                    SpeechRecognitionState.Error(
                        SpeechRecognitionError.CLIENT,
                        e.message ?: "Failed to initialize speech recognizer"
                    )
                )
            }
        }

        awaitClose {
            _isListening.value = false
            CoroutineScope(mainDispatcher).launch {
                val current = sessionRecognizer
                if (activeRecognizer === current) {
                    activeRecognizer = null
                }
                runCatching {
                    current?.stopListening()
                    current?.cancel()
                    current?.destroy()
                }
            }
        }
    }

    /**
     * Halts speech recognition and requests results for audio captured so far.
     * Executes safely on [mainDispatcher].
     */
    open fun stopListening() {
        _isListening.value = false
        CoroutineScope(mainDispatcher).launch {
            runCatching {
                activeRecognizer?.stopListening()
            }
        }
    }

    /**
     * Cancels active speech recognition without processing remaining audio.
     * Executes safely on [mainDispatcher].
     */
    open fun cancelListening() {
        _isListening.value = false
        CoroutineScope(mainDispatcher).launch {
            runCatching {
                activeRecognizer?.cancel()
            }
        }
    }

    /**
     * Destroys active recognizer resources and zeroes state.
     * Executes safely on [mainDispatcher].
     */
    open fun destroy() {
        _isListening.value = false
        CoroutineScope(mainDispatcher).launch {
            val recognizer = activeRecognizer
            activeRecognizer = null
            runCatching {
                recognizer?.cancel()
                recognizer?.destroy()
            }
        }
    }

    /**
     * Factory method creating appropriate [SpeechRecognizer] instance.
     * Falls back to standard SpeechRecognizer if on-device creation fails.
     */
    protected open fun createSpeechRecognizerInstance(ctx: Context, preferOffline: Boolean): SpeechRecognizer {
        if (preferOffline && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val onDeviceAvailable = runCatching {
                SpeechRecognizer.isOnDeviceRecognitionAvailable(ctx)
            }.getOrDefault(false)

            if (onDeviceAvailable) {
                val onDeviceRecognizer = runCatching {
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(ctx)
                }.getOrNull()
                if (onDeviceRecognizer != null) {
                    return onDeviceRecognizer
                }
            }
        }
        return SpeechRecognizer.createSpeechRecognizer(ctx)
    }

    /**
     * Builds recognition [Intent] with offline preference flags and language settings.
     */
    protected open fun createRecognizerIntent(preferOffline: Boolean): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            if (preferOffline) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
        }
    }

    /**
     * Resolves human-friendly, non-punishing error messages for speech recognition errors.
     */
    open fun getErrorMessage(error: SpeechRecognitionError): String {
        return when (error) {
            SpeechRecognitionError.NETWORK, SpeechRecognitionError.NETWORK_TIMEOUT, SpeechRecognitionError.SERVER_DISCONNECTED ->
                "Network connection lost. Switched to offline mode."
            SpeechRecognitionError.SPEECH_TIMEOUT, SpeechRecognitionError.NO_MATCH ->
                "No speech detected. Tap mic to retry."
            SpeechRecognitionError.AUDIO ->
                "Audio recording error. Please check microphone."
            SpeechRecognitionError.INSUFFICIENT_PERMISSIONS ->
                "Microphone permission required for voice capture."
            SpeechRecognitionError.RECOGNIZER_BUSY, SpeechRecognitionError.TOO_MANY_REQUESTS ->
                "Speech recognizer is busy. Please try again."
            SpeechRecognitionError.LANGUAGE_NOT_SUPPORTED, SpeechRecognitionError.LANGUAGE_UNAVAILABLE ->
                "Selected language is unavailable offline. Try typing below."
            else ->
                "Speech recognition unavailable. Try typing below."
        }
    }

    companion object {
        /**
         * Normalizes Android SpeechRecognizer rmsdB values (typically -2dB..10dB) into [0.0, 1.0].
         * Safely handles NaN values returning 0f.
         */
        fun normalizeRms(rmsdB: Float): Float {
            if (rmsdB.isNaN()) return 0f
            return ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        }
    }
}
