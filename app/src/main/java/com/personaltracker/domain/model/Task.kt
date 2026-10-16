package com.personaltracker.domain.model

/**
 * Pure Kotlin domain model representing a task item (FR-2.2, FR-5.1, AD-1).
 */
data class Task(
    val id: String,
    val title: String,
    val description: String? = null,
    val scheduledDate: String, // "YYYY-MM-DD"
    val scheduledTime: String? = null, // "HH:mm"
    val estimatedDurationMinutes: Int = 0,
    val deliveryIntensity: String = "ROUTINE", // "ROUTINE", "IMPORTANT", "URGENT"
    val state: String = "PENDING", // "PENDING", "IN_PROGRESS", "INTERRUPTED", "COMPLETED", "CANCELLED"
    val isCarryForward: Boolean = false,
    val carryForwardCount: Int = 0,
    val completionTimestamp: Long? = null,
    val createdTimestamp: Long = System.currentTimeMillis()
)
