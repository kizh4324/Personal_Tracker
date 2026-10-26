package com.personaltracker.platform.voice

/**
 * Speech recognition error categories mapped from Android SpeechRecognizer error codes (FR-7, AD-8).
 */
enum class SpeechRecognitionError {
    NETWORK,
    NETWORK_TIMEOUT,
    SERVER_DISCONNECTED,
    AUDIO,
    SERVER,
    CLIENT,
    SPEECH_TIMEOUT,
    NO_MATCH,
    RECOGNIZER_BUSY,
    INSUFFICIENT_PERMISSIONS,
    TOO_MANY_REQUESTS,
    LANGUAGE_NOT_SUPPORTED,
    LANGUAGE_UNAVAILABLE,
    CANNOT_CHECK_SUPPORT,
    UNKNOWN;

    val isNetworkError: Boolean
        get() = this == NETWORK || this == NETWORK_TIMEOUT || this == SERVER_DISCONNECTED

    val isTimeoutOrNoMatch: Boolean
        get() = this == SPEECH_TIMEOUT || this == NO_MATCH

    companion object {
        /**
         * Maps standard Android SpeechRecognizer ERROR_* integer codes to SpeechRecognitionError enum.
         */
        fun fromErrorCode(errorCode: Int): SpeechRecognitionError {
            return when (errorCode) {
                1 -> NETWORK_TIMEOUT // SpeechRecognizer.ERROR_NETWORK_TIMEOUT
                2 -> NETWORK         // SpeechRecognizer.ERROR_NETWORK
                3 -> AUDIO           // SpeechRecognizer.ERROR_AUDIO
                4 -> SERVER          // SpeechRecognizer.ERROR_SERVER
                5 -> CLIENT          // SpeechRecognizer.ERROR_CLIENT
                6 -> SPEECH_TIMEOUT  // SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                7 -> NO_MATCH        // SpeechRecognizer.ERROR_NO_MATCH
                8 -> RECOGNIZER_BUSY // SpeechRecognizer.ERROR_RECOGNIZER_BUSY
                9 -> INSUFFICIENT_PERMISSIONS // SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS
                10 -> TOO_MANY_REQUESTS // SpeechRecognizer.ERROR_TOO_MANY_REQUESTS
                11 -> SERVER_DISCONNECTED // SpeechRecognizer.ERROR_SERVER_DISCONNECTED
                12 -> LANGUAGE_NOT_SUPPORTED // SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED
                13 -> LANGUAGE_UNAVAILABLE // SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE
                14 -> CANNOT_CHECK_SUPPORT // SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Sealed interface representing the reactive lifecycle states of speech recognition (FR-7, AD-8).
 */
sealed interface SpeechRecognitionState {
    /**
     * Speech engine is idle, not capturing or listening.
     */
    object Idle : SpeechRecognitionState

    /**
     * Speech engine is active and capturing audio.
     * @param rms Normalized RMS amplitude in range [0.0, 1.0].
     */
    data class Listening(val rms: Float = 0f) : SpeechRecognitionState

    /**
     * Speech has completed and engine is actively computing final transcription.
     */
    object Processing : SpeechRecognitionState

    /**
     * A transcription result has been emitted.
     * @param text The transcribed speech text.
     * @param isFinal True if this is the final transcription; false if it is a partial draft.
     */
    data class Result(val text: String, val isFinal: Boolean) : SpeechRecognitionState

    /**
     * An error occurred during recognition.
     * @param error Categorized speech recognition error.
     * @param message Human-friendly, non-punishing error explanation.
     */
    data class Error(val error: SpeechRecognitionError, val message: String) : SpeechRecognitionState
}
