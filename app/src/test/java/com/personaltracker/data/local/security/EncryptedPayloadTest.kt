package com.personaltracker.data.local.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EncryptedPayloadTest {

    @Test
    fun testEncryptedPayloadEqualityAndHashCode() {
        val iv1 = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
        val iv2 = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
        val iv3 = byteArrayOf(9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9)

        val ct1 = byteArrayOf(100, 101, 102)
        val ct2 = byteArrayOf(100, 101, 102)
        val ct3 = byteArrayOf(20, 21, 22)

        val payload1 = EncryptedPayload(iv1, ct1)
        val payload2 = EncryptedPayload(iv2, ct2)
        val payloadDifferentIv = EncryptedPayload(iv3, ct1)
        val payloadDifferentCt = EncryptedPayload(iv1, ct3)

        assertEquals("Identical byte contents must be equal", payload1, payload2)
        assertEquals("Identical byte contents must produce identical hashCode", payload1.hashCode(), payload2.hashCode())

        assertNotEquals("Different IV must not be equal", payload1, payloadDifferentIv)
        assertNotEquals("Different Ciphertext must not be equal", payload1, payloadDifferentCt)
    }
}
