package com.personaltracker.platform.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests validating [SpeechRecognitionState] models, [SpeechRecognitionError] mapping,
 * RMS normalization, and failover message resolution (FR-7, AD-8).
 */
class SpeechRecognitionStateTest {

    @Test
    fun testErrorCodeMappingToEnum() {
        assertEquals(SpeechRecognitionError.NETWORK_TIMEOUT, SpeechRecognitionError.fromErrorCode(1))
        assertEquals(SpeechRecognitionError.NETWORK, SpeechRecognitionError.fromErrorCode(2))
        assertEquals(SpeechRecognitionError.AUDIO, SpeechRecognitionError.fromErrorCode(3))
        assertEquals(SpeechRecognitionError.SERVER, SpeechRecognitionError.fromErrorCode(4))
        assertEquals(SpeechRecognitionError.CLIENT, SpeechRecognitionError.fromErrorCode(5))
        assertEquals(SpeechRecognitionError.SPEECH_TIMEOUT, SpeechRecognitionError.fromErrorCode(6))
        assertEquals(SpeechRecognitionError.NO_MATCH, SpeechRecognitionError.fromErrorCode(7))
        assertEquals(SpeechRecognitionError.RECOGNIZER_BUSY, SpeechRecognitionError.fromErrorCode(8))
        assertEquals(SpeechRecognitionError.INSUFFICIENT_PERMISSIONS, SpeechRecognitionError.fromErrorCode(9))
        assertEquals(SpeechRecognitionError.TOO_MANY_REQUESTS, SpeechRecognitionError.fromErrorCode(10))
        assertEquals(SpeechRecognitionError.SERVER_DISCONNECTED, SpeechRecognitionError.fromErrorCode(11))
        assertEquals(SpeechRecognitionError.LANGUAGE_NOT_SUPPORTED, SpeechRecognitionError.fromErrorCode(12))
        assertEquals(SpeechRecognitionError.LANGUAGE_UNAVAILABLE, SpeechRecognitionError.fromErrorCode(13))
        assertEquals(SpeechRecognitionError.CANNOT_CHECK_SUPPORT, SpeechRecognitionError.fromErrorCode(14))
        assertEquals(SpeechRecognitionError.UNKNOWN, SpeechRecognitionError.fromErrorCode(999))
    }

    @Test
    fun testIsNetworkErrorDetection() {
        assertTrue(SpeechRecognitionError.NETWORK.isNetworkError)
        assertTrue(SpeechRecognitionError.NETWORK_TIMEOUT.isNetworkError)
        assertTrue(SpeechRecognitionError.SERVER_DISCONNECTED.isNetworkError)
        assertFalse(SpeechRecognitionError.AUDIO.isNetworkError)
        assertFalse(SpeechRecognitionError.SPEECH_TIMEOUT.isNetworkError)
        assertFalse(SpeechRecognitionError.CLIENT.isNetworkError)
    }

    @Test
    fun testIsTimeoutOrNoMatchDetection() {
        assertTrue(SpeechRecognitionError.SPEECH_TIMEOUT.isTimeoutOrNoMatch)
        assertTrue(SpeechRecognitionError.NO_MATCH.isTimeoutOrNoMatch)
        assertFalse(SpeechRecognitionError.NETWORK.isTimeoutOrNoMatch)
        assertFalse(SpeechRecognitionError.AUDIO.isTimeoutOrNoMatch)
    }

    @Test
    fun testSpeechRecognitionStateInstances() {
        val idle = SpeechRecognitionState.Idle
        assertNotNull(idle)

        val listening = SpeechRecognitionState.Listening(rms = 0.65f)
        assertEquals(0.65f, listening.rms, 0.001f)

        val processing = SpeechRecognitionState.Processing
        assertNotNull(processing)

        val partialResult = SpeechRecognitionState.Result(text = "Buy groceries", isFinal = false)
        assertEquals("Buy groceries", partialResult.text)
        assertFalse(partialResult.isFinal)

        val finalResult = SpeechRecognitionState.Result(text = "Buy groceries tomorrow", isFinal = true)
        assertEquals("Buy groceries tomorrow", finalResult.text)
        assertTrue(finalResult.isFinal)

        val errorState = SpeechRecognitionState.Error(
            error = SpeechRecognitionError.NETWORK,
            message = "Network connection lost. Switched to offline mode."
        )
        assertEquals(SpeechRecognitionError.NETWORK, errorState.error)
        assertTrue(errorState.message.contains("offline", ignoreCase = true))
    }

    @Test
    fun testNormalizeRmsCalculations() {
        // NaN guard
        assertEquals(0f, SpeechRecognitionManager.normalizeRms(Float.NaN), 0.001f)

        // Below minimum threshold
        assertEquals(0f, SpeechRecognitionManager.normalizeRms(-5f), 0.001f)
        assertEquals(0f, SpeechRecognitionManager.normalizeRms(-2f), 0.001f)

        // Mid-point: (-2 + 2) / 12 = 0; (4 + 2) / 12 = 0.5
        assertEquals(0.5f, SpeechRecognitionManager.normalizeRms(4f), 0.001f)

        // Upper limit: (10 + 2) / 12 = 1.0
        assertEquals(1.0f, SpeechRecognitionManager.normalizeRms(10f), 0.001f)

        // Exceeding upper limit clamped to 1.0
        assertEquals(1.0f, SpeechRecognitionManager.normalizeRms(15f), 0.001f)
    }

    @Test
    fun testGetErrorMessageNonPunishingMessages() {
        val manager = SpeechRecognitionManager(context = null)

        val networkMsg = manager.getErrorMessage(SpeechRecognitionError.NETWORK)
        assertTrue(networkMsg.contains("offline", ignoreCase = true))

        val timeoutMsg = manager.getErrorMessage(SpeechRecognitionError.SPEECH_TIMEOUT)
        assertTrue(timeoutMsg.contains("retry", ignoreCase = true))

        val audioMsg = manager.getErrorMessage(SpeechRecognitionError.AUDIO)
        assertTrue(audioMsg.contains("microphone", ignoreCase = true))

        val permMsg = manager.getErrorMessage(SpeechRecognitionError.INSUFFICIENT_PERMISSIONS)
        assertTrue(permMsg.contains("permission", ignoreCase = true))

        val unknownMsg = manager.getErrorMessage(SpeechRecognitionError.UNKNOWN)
        assertTrue(unknownMsg.contains("typing", ignoreCase = true))
    }
}
