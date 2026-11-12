package com.dravid.businesstracker.data.model

import java.time.YearMonth

/**
 * Filter period options for business intelligence reports.
 */
enum class ReportPeriodFilter(val displayName: String) {
    ALL_TIME("All Time"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    THIS_YEAR("This Year"),
    CUSTOM("Custom"),
}

/**
 * Monthly performance aggregation of business metrics.
 */
data class MonthlyPerformance(
    val yearMonth: YearMonth,
    val summary: BusinessSummary,
)

/**
 * Source-wise performance aggregation of business metrics.
 */
data class SourcePerformance(
    val sourceId: Long?,
    val sourceName: String,
    val summary: BusinessSummary,
)

/**
 * Policy status breakdown item detailing counts, relevant premiums, and proportions.
 */
data class StatusBreakdownItem(
    val status: BusinessStatus,
    val policyCount: Int,
    val premiumPaise: Long,
    val percentageOfPolicies: Float,
)

/**
 * Comprehensive business intelligence report compiled from live Room database records.
 */
data class BusinessReport(
    val summary: BusinessSummary = BusinessSummary(),
    val averageIssuedPremiumPaise: Long = 0L,
    val cancelledPremiumPaise: Long = 0L,
    val monthlyPerformances: List<MonthlyPerformance> = emptyList(),
    val sourcePerformances: List<SourcePerformance> = emptyList(),
    val unassignedPerformance: SourcePerformance = SourcePerformance(
        sourceId = null,
        sourceName = "Unassigned",
        summary = BusinessSummary(),
    ),
    val statusBreakdown: List<StatusBreakdownItem> = emptyList(),
    val filteredRecordCount: Int = 0,
    val totalDatabaseRecordCount: Int = 0,
)
