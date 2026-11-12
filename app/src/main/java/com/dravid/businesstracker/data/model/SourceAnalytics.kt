package com.dravid.businesstracker.data.model

/**
 * Encapsulates a lead/referral source along with its calculated business metrics.
 */
data class SourceWithAnalytics(
    val source: SourceEntity,
    val summary: BusinessSummary,
)

/**
 * Encapsulates metrics for business records that have no assigned source (sourceId == null).
 */
data class UnassignedSourceAnalytics(
    val summary: BusinessSummary,
)
