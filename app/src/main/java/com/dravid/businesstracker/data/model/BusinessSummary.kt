package com.dravid.businesstracker.data.model

/**
 * Summary calculations for insurance business metrics using exact paise (Long).
 */
data class BusinessSummary(
    val totalIssuedPremiumPaise: Long = 0L,
    val totalIssuedPolicies: Int = 0,
    val pendingPremiumPaise: Long = 0L,
    val pendingPolicies: Int = 0,
    val cancelledPolicies: Int = 0,
    val totalRecords: Int = 0,
)
