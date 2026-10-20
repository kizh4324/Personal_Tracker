package com.personaltracker.platform

import com.personaltracker.platform.voice.QuickCaptureVoiceRecorder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class QuickCaptureVoiceRecorderTest {

    @Test
    fun `calculateNormalizedRms with silence returns zero`() {
        val buffer = ShortArray(1024) { 0 }
        val rms = QuickCaptureVoiceRecorder.calculateNormalizedRms(buffer)
        assertEquals(0.0f, rms, 0.0001f)
    }

    @Test
    fun `calculateNormalizedRms with max amplitude returns 1_0`() {
        val buffer = ShortArray(1024) { 32767.toShort() }
        val rms = QuickCaptureVoiceRecorder.calculateNormalizedRms(buffer)
        assertEquals(1.0f, rms, 0.001f)
    }

    @Test
    fun `calculateNormalizedRms with half amplitude returns approximately 0_5`() {
        val halfMax = (32767 / 2).toShort()
        val buffer = ShortArray(1024) { halfMax }
        val rms = QuickCaptureVoiceRecorder.calculateNormalizedRms(buffer)
        assertTrue("Expected ~0.5 but was $rms", abs(rms - 0.5f) < 0.01f)
    }

    @Test
    fun `calculateNormalizedRms with zero or negative readSize returns zero`() {
        val buffer = ShortArray(1024) { 1000.toShort() }
        assertEquals(0.0f, QuickCaptureVoiceRecorder.calculateNormalizedRms(buffer, readSize = 0), 0.0001f)
        assertEquals(0.0f, QuickCaptureVoiceRecorder.calculateNormalizedRms(buffer, readSize = -5), 0.0001f)
    }

    @Test
    fun `calculateNormalizedRms with alternating positive and negative samples`() {
        val buffer = ShortArray(1000) { index ->
            if (index % 2 == 0) 10000.toShort() else (-10000).toShort()
        }
        val rms = QuickCaptureVoiceRecorder.calculateNormalizedRms(buffer)
        val expected = 10000.0f / 32767.0f
        assertEquals(expected, rms, 0.001f)
    }

    @Test
    fun `purgeBuffer unconditionally zeroes ShortArray in memory`() {
        val buffer = ShortArray(512) { (it + 1).toShort() }
        // Verify buffer starts with non-zero values
        assertTrue(buffer.any { it != 0.toShort() })

        QuickCaptureVoiceRecorder.purgeBuffer(buffer)

        // Verify every single element is zeroed
        assertTrue(buffer.all { it == 0.toShort() })
    }

    @Test
    fun `purgeBuffer unconditionally zeroes ByteArray in memory`() {
        val buffer = ByteArray(256) { (it % 127 + 1).toByte() }
        // Verify buffer starts with non-zero values
        assertTrue(buffer.any { it != 0.toByte() })

        QuickCaptureVoiceRecorder.purgeBuffer(buffer)

        // Verify every single element is zeroed
        assertTrue(buffer.all { it == 0.toByte() })
    }
}
