package com.personaltracker.data.local.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Unit tests verifying AES-256-GCM encryption mechanics, 12-byte IV handling,
 * authentication tag integrity, and tamper resistance (ARCH-2, NFR-4).
 */
class SecurityCryptoUnitTest {

    private fun generateTestAes256Key(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        return keyGen.generateKey()
    }

    @Test
    fun testAesGcmEncryptionDecryptionRoundTrip() {
        val key = generateTestAes256Key()
        val originalPassphrase = ByteArray(32).apply { SecureRandom().nextBytes(this) }

        // Encrypt
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val ciphertext = cipher.doFinal(originalPassphrase)
        val iv = cipher.iv

        assertEquals("GCM IV length must be 12 bytes", 12, iv.size)
        assertTrue("Ciphertext must include 16-byte (128-bit) auth tag", ciphertext.size >= originalPassphrase.size + 16)

        // Decrypt
        val decryptCipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        decryptCipher.init(Cipher.DECRYPT_MODE, key, spec)
        val decryptedBytes = decryptCipher.doFinal(ciphertext)

        assertArrayEquals("Decrypted bytes must match original 32-byte passphrase", originalPassphrase, decryptedBytes)
    }

    @Test
    fun testUniqueIvPerEncryptionPreventsDeterministicCiphertext() {
        val key = generateTestAes256Key()
        val plaintext = ByteArray(32).apply { SecureRandom().nextBytes(this) }

        val cipher1 = Cipher.getInstance("AES/GCM/NoPadding")
        cipher1.init(Cipher.ENCRYPT_MODE, key)
        val ct1 = cipher1.doFinal(plaintext)
        val iv1 = cipher1.iv

        val cipher2 = Cipher.getInstance("AES/GCM/NoPadding")
        cipher2.init(Cipher.ENCRYPT_MODE, key)
        val ct2 = cipher2.doFinal(plaintext)
        val iv2 = cipher2.iv

        assertFalse("Subsequent encryptions must have distinct IVs", iv1.contentEquals(iv2))
        assertFalse("Identical plaintext with distinct IVs must produce distinct ciphertexts", ct1.contentEquals(ct2))
    }

    @Test
    fun testTamperedCiphertextFailsAuthenticationTag() {
        val key = generateTestAes256Key()
        val plaintext = "test_passphrase_bytes".toByteArray()

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val ciphertext = cipher.doFinal(plaintext)
        val iv = cipher.iv

        // Tamper with a byte in the ciphertext
        ciphertext[0] = (ciphertext[0].toInt() xor 0xFF).toByte()

        val decryptCipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        decryptCipher.init(Cipher.DECRYPT_MODE, key, spec)

        try {
            decryptCipher.doFinal(ciphertext)
            fail("Decryption of tampered ciphertext must throw AEADBadTagException")
        } catch (e: Exception) {
            assertTrue("Expected AEADBadTagException or bad padding exception", e is AEADBadTagException || e.cause is AEADBadTagException)
        }
    }
}
