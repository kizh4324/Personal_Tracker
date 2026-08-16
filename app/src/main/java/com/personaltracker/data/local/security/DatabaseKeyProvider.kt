package com.personaltracker.data.local.security

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the generation, persistence, and recovery of the 256-bit database passphrase
 * used to encrypt the SQLCipher database.
 *
 * Implements ARCH-2 and Story 1.2 specifications.
 */
@Singleton
class DatabaseKeyProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keyStoreManager: SecurityKeyStoreManager
) {

    companion object {
        const val VAULT_FILE_NAME = "pt_db_vault.bin"
        private const val FORMAT_VERSION: Byte = 0x01
        private const val PASSPHRASE_LENGTH_BYTES = 32 // 256 bits entropy
        private const val GCM_IV_LENGTH_BYTES = 12
    }

    private val vaultFile: File
        get() = File(context.filesDir, VAULT_FILE_NAME)

    /**
     * Retrieves the decrypted 32-byte database passphrase.
     * Generates and encrypts a new passphrase if running for the first time.
     */
    @Synchronized
    fun getDatabasePassphrase(): ByteArray {
        return if (vaultFile.exists()) {
            loadAndDecryptPassphrase()
        } else {
            generateAndPersistPassphrase()
        }
    }

    /**
     * Generates a 32-byte cryptographic random passphrase, encrypts it with the
     * Keystore Master Key, writes the vault file to private app storage, and returns the plaintext bytes.
     */
    private fun generateAndPersistPassphrase(): ByteArray {
        val passphrase = ByteArray(PASSPHRASE_LENGTH_BYTES)
        SecureRandom().nextBytes(passphrase)

        val encrypted = keyStoreManager.encrypt(passphrase)

        // Structure: [1 byte version][12 bytes IV][ciphertext + tag]
        FileOutputStream(vaultFile).use { fos ->
            fos.write(byteArrayOf(FORMAT_VERSION))
            fos.write(encrypted.iv)
            fos.write(encrypted.ciphertext)
            fos.flush()
        }

        return passphrase
    }

    /**
     * Reads the encrypted vault file from private app storage, parses the IV and ciphertext,
     * and decrypts the 32-byte passphrase via the Keystore Master Key.
     */
    private fun loadAndDecryptPassphrase(): ByteArray {
        val fileBytes = FileInputStream(vaultFile).use { fis ->
            fis.readBytes()
        }

        require(fileBytes.size > 1 + GCM_IV_LENGTH_BYTES) {
            "Database vault file corrupted or truncated."
        }

        val version = fileBytes[0]
        require(version == FORMAT_VERSION) {
            "Unsupported database vault format version: $version"
        }

        val iv = fileBytes.copyOfRange(1, 1 + GCM_IV_LENGTH_BYTES)
        val ciphertext = fileBytes.copyOfRange(1 + GCM_IV_LENGTH_BYTES, fileBytes.size)

        val payload = EncryptedPayload(iv = iv, ciphertext = ciphertext)
        return keyStoreManager.decrypt(payload)
    }
}
