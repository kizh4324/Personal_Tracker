package com.personaltracker.platform.voice

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.Arrays
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Microphone audio recorder operating exclusively in RAM with zero disk writes (FR-7, AD-8, NFR-4).
 * Streams normalized RMS amplitudes (0.0..1.0) for live waveform visualization and unconditionally
 * zeroes audio memory buffers upon completion or cancellation.
 */
@Singleton
class QuickCaptureVoiceRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    /**
     * Checks if RECORD_AUDIO permission has been granted.
     */
    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Streams normalized RMS amplitudes (0.0f..1.0f) while recording from microphone.
     * Raw PCM buffers are kept strictly in RAM and unconditionally zeroed in a `finally` block.
     *
     * @param onAudioChunk Optional callback receiving in-memory PCM chunk before buffer is purged.
     * @throws SecurityException If RECORD_AUDIO permission is not granted.
     */
    @SuppressLint("MissingPermission")
    fun startRecording(
        onAudioChunk: ((ShortArray, Int) -> Unit)? = null
    ): Flow<Float> = flow {
        if (!hasRecordPermission()) {
            throw SecurityException("RECORD_AUDIO permission not granted")
        }

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )
        val bufferSize = maxOf(minBufferSize, BUFFER_SIZE_SAMPLES)
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize * 2
        )

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            throw IllegalStateException("AudioRecord failed to initialize")
        }

        val buffer = ShortArray(bufferSize)
        try {
            audioRecord.startRecording()
            _isRecording.value = true

            while (_isRecording.value) {
                val readSamples = audioRecord.read(buffer, 0, buffer.size)
                if (readSamples > 0) {
                    onAudioChunk?.invoke(buffer, readSamples)
                    val rms = calculateNormalizedRms(buffer, readSamples)
                    emit(rms)
                }
            }
        } finally {
            // Unconditionally zero in-memory buffer to prevent audio leaks (AD-8)
            purgeBuffer(buffer)
            runCatching {
                if (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop()
                }
            }
            runCatching { audioRecord.release() }
            _isRecording.value = false
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Halts active recording loop.
     */
    fun stopRecording() {
        _isRecording.value = false
    }

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val BUFFER_SIZE_SAMPLES = 2048

        /**
         * Calculates normalized RMS amplitude in range [0.0, 1.0] from 16-bit PCM audio samples.
         */
        fun calculateNormalizedRms(buffer: ShortArray, readSize: Int = buffer.size): Float {
            if (readSize <= 0) return 0.0f
            var sumOfSquares = 0.0
            val size = minOf(readSize, buffer.size)
            for (i in 0 until size) {
                val sample = buffer[i].toDouble()
                sumOfSquares += sample * sample
            }
            val rms = sqrt(sumOfSquares / size)
            // 16-bit signed PCM maximum amplitude is 32767.0
            val normalized = (rms / 32767.0).toFloat()
            return normalized.coerceIn(0.0f, 1.0f)
        }

        /**
         * Unconditionally zeroes out a ShortArray buffer in memory.
         */
        fun purgeBuffer(buffer: ShortArray) {
            Arrays.fill(buffer, 0.toShort())
        }

        /**
         * Unconditionally zeroes out a ByteArray buffer in memory.
         */
        fun purgeBuffer(buffer: ByteArray) {
            Arrays.fill(buffer, 0.toByte())
        }
    }
}
