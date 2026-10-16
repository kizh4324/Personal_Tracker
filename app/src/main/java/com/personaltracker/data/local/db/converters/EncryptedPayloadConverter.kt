package com.personaltracker.data.local.db.converters

import androidx.room.TypeConverter
import com.personaltracker.data.local.security.EncryptedPayload

/**
 * Room TypeConverter converting [EncryptedPayload] to/from a binary BLOB (ByteArray).
 * Serializes as [12 bytes IV][Ciphertext + 128-bit (16-byte) GCM tag].
 * Minimum valid serialized payload is 28 bytes (12-byte IV + 16-byte authentication tag).
 */
class EncryptedPayloadConverter {

    @TypeConverter
    fun fromEncryptedPayload(payload: EncryptedPayload?): ByteArray? {
        if (payload == null || payload.iv.size != 12 || payload.ciphertext.size < 16) return null
        return payload.iv + payload.ciphertext
    }

    @TypeConverter
    fun toEncryptedPayload(bytes: ByteArray?): EncryptedPayload? {
        if (bytes == null || bytes.size < 28) return null
        val iv = bytes.copyOfRange(0, 12)
        val ciphertext = bytes.copyOfRange(12, bytes.size)
        return EncryptedPayload(iv = iv, ciphertext = ciphertext)
    }
}
