package com.personaltracker.data.local.db.converters

import com.personaltracker.data.local.security.EncryptedPayload
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ConvertersTest {

    private val payloadConverter = EncryptedPayloadConverter()
    private val dateConverters = DateConverters()

    @Test
    fun testEncryptedPayloadRoundTrip() {
        val iv = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
        // GCM ciphertext requires at least 16-byte authentication tag
        val ciphertext = ByteArray(16) { (it + 1).toByte() }
        val original = EncryptedPayload(iv = iv, ciphertext = ciphertext)

        val serialized = payloadConverter.fromEncryptedPayload(original)
        assertNotNull("Serialized byte array must not be null", serialized)
        assertEquals("Total bytes must be IV length (12) + ciphertext tag length (16) = 28", 28, serialized?.size)

        val deserialized = payloadConverter.toEncryptedPayload(serialized)
        assertNotNull("Deserialized payload must not be null", deserialized)
        assertEquals("Payload must equal original after round-trip", original, deserialized)
        assertArrayEquals("IV must match exactly", original.iv, deserialized?.iv)
        assertArrayEquals("Ciphertext must match exactly", original.ciphertext, deserialized?.ciphertext)
    }

    @Test
    fun testEncryptedPayloadNullHandling() {
        assertNull("Null payload input must produce null ByteArray", payloadConverter.fromEncryptedPayload(null))
        assertNull("Null ByteArray input must produce null EncryptedPayload", payloadConverter.toEncryptedPayload(null))
    }

    @Test
    fun testEncryptedPayloadTruncatedBytesHandling() {
        // Less than 28 bytes cannot contain 12-byte IV + 16-byte GCM authentication tag
        val truncated27 = ByteArray(27) { 1 }
        assertNull("ByteArray shorter than 28 bytes must return null", payloadConverter.toEncryptedPayload(truncated27))

        val empty = ByteArray(0)
        assertNull("Empty ByteArray must return null", payloadConverter.toEncryptedPayload(empty))

        val exactly28 = ByteArray(28) { 1 }
        assertNotNull("ByteArray of exactly 28 bytes must successfully deserialize", payloadConverter.toEncryptedPayload(exactly28))
    }

    @Test
    fun testEncryptedPayloadCiphertextMinimumTagBoundary() {
        val validIv = ByteArray(12) { 1 }

        // Ciphertext with 15 bytes (< 16-byte tag) must be rejected
        val tooShortCiphertext = ByteArray(15) { 2 }
        val invalidPayload = EncryptedPayload(iv = validIv, ciphertext = tooShortCiphertext)
        assertNull("Ciphertext shorter than 16 bytes must return null from converter", payloadConverter.fromEncryptedPayload(invalidPayload))

        // Ciphertext with 16 bytes (minimum GCM tag) must be accepted
        val minTagCiphertext = ByteArray(16) { 2 }
        val validMinPayload = EncryptedPayload(iv = validIv, ciphertext = minTagCiphertext)
        assertNotNull("Ciphertext with 16 bytes must serialize successfully", payloadConverter.fromEncryptedPayload(validMinPayload))
    }

    @Test
    fun testEncryptedPayloadInvalidIvSize() {
        val invalidIv = byteArrayOf(1, 2, 3)
        val ct = ByteArray(16) { 10 }
        val invalidPayload = EncryptedPayload(iv = invalidIv, ciphertext = ct)
        assertNull("Payload with non-12-byte IV must return null", payloadConverter.fromEncryptedPayload(invalidPayload))
    }

    @Test
    fun testLocalDateRoundTrip() {
        val date = LocalDate.of(2026, 10, 15)
        val serialized = dateConverters.fromLocalDate(date)
        assertEquals("2026-10-15", serialized)

        val deserialized = dateConverters.toLocalDate(serialized)
        assertEquals(date, deserialized)
    }

    @Test
    fun testLocalDateNullAndInvalidHandling() {
        assertNull(dateConverters.fromLocalDate(null))
        assertNull(dateConverters.toLocalDate(null))
        assertNull("Invalid ISO date string must parse to null", dateConverters.toLocalDate("not-a-date"))
    }

    @Test
    fun testInstantRoundTrip() {
        val now = Instant.ofEpochMilli(1792051200000L)
        val serialized = dateConverters.fromInstant(now)
        assertEquals(1792051200000L, serialized)

        val deserialized = dateConverters.toInstant(serialized)
        assertEquals(now, deserialized)
    }

    @Test
    fun testInstantNullHandling() {
        assertNull(dateConverters.fromInstant(null))
        assertNull(dateConverters.toInstant(null))
    }

    @Test
    fun testInstantOutOfRangeHandling() {
        // Instant.MAX has seconds beyond epoch milli and throws ArithmeticException if unhandled
        assertNull("Instant.MAX must return null instead of throwing", dateConverters.fromInstant(Instant.MAX))
        assertNull("Instant.MIN must return null instead of throwing", dateConverters.fromInstant(Instant.MIN))
    }
}
