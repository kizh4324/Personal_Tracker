package com.personaltracker.data.local.security

/**
 * Container representing an AES-GCM encrypted payload.
 *
 * @property iv 12-byte initialization vector generated for this encryption operation.
 * @property ciphertext Encrypted bytes including the 128-bit authentication tag.
 */
data class EncryptedPayload(
    val iv: ByteArray,
    val ciphertext: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptedPayload

        if (!iv.contentEquals(other.iv)) return false
        if (!ciphertext.contentEquals(other.ciphertext)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = iv.contentHashCode()
        result = 31 * result + ciphertext.contentHashCode()
        return result
    }
}
