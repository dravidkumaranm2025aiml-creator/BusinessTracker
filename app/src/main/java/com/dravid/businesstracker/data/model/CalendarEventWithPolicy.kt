package com.dravid.businesstracker.data.model

/**
 * Encapsulates a calendar event along with its linked policy/reference number (if any).
 */
data class CalendarEventWithPolicy(
    val event: CalendarEventEntity,
    val policyNumber: String? = null,
)
