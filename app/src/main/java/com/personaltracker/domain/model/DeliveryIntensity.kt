package com.personaltracker.domain.model

/**
 * Task delivery intensity classifications driving reward payouts (FR-2.1, FR-2.2, FR-8.1).
 */
enum class DeliveryIntensity {
    ROUTINE,
    IMPORTANT,
    URGENT;

    companion object {
        fun fromString(value: String?): DeliveryIntensity = when (value?.uppercase()) {
            "URGENT" -> URGENT
            "IMPORTANT" -> IMPORTANT
            else -> ROUTINE
        }
    }
}
