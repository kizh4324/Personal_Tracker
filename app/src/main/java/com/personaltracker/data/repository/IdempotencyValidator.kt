package com.personaltracker.data.repository

/**
 * Validates compound idempotency keys according to AD-4: {DOMAIN}_{ACTION}_{ID}.
 */
internal object IdempotencyValidator {

    fun validate(key: String) {
        val parts = key.split("_")
        require(parts.size >= 3 && parts.all { it.isNotBlank() && !it.any(Char::isWhitespace) }) {
            "Invalid idempotency key: '$key'. Expected compound format {DOMAIN}_{ACTION}_{ID} without whitespace."
        }
    }
}
