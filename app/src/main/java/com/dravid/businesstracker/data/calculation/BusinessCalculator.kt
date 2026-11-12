package com.dravid.businesstracker.data.calculation

import com.dravid.businesstracker.data.model.BusinessEntity
import com.dravid.businesstracker.data.model.BusinessStatus
import com.dravid.businesstracker.data.model.BusinessSummary

/**
 * Dedicated calculation layer for business metrics ensuring single-source-of-truth
 * and exact integer arithmetic without floating-point errors.
 */
object BusinessCalculator {

    /**
     * Calculates summary metrics from a list of business records:
     * - Issued policies contribute to total issued count and total issued premium in paise.
     * - Pending policies and premiums are calculated and displayed separately in paise.
     * - Cancelled policies are tracked but do NOT contribute to premium or active policy totals.
     */
    fun calculateSummary(records: List<BusinessEntity>): BusinessSummary {
        var issuedPremiumPaise = 0L
        var issuedCount = 0
        var pendingPremiumPaise = 0L
        var pendingCount = 0
        var cancelledCount = 0

        for (record in records) {
            when (record.status) {
                BusinessStatus.ISSUED -> {
                    issuedPremiumPaise += record.premiumAmountPaise
                    issuedCount++
                }
                BusinessStatus.PENDING -> {
                    pendingPremiumPaise += record.premiumAmountPaise
                    pendingCount++
                }
                BusinessStatus.CANCELLED -> {
                    cancelledCount++
                }
            }
        }

        return BusinessSummary(
            totalIssuedPremiumPaise = issuedPremiumPaise,
            totalIssuedPolicies = issuedCount,
            pendingPremiumPaise = pendingPremiumPaise,
            pendingPolicies = pendingCount,
            cancelledPolicies = cancelledCount,
            totalRecords = records.size,
        )
    }

    /**
     * Calculates the average issued premium per issued policy in exact paise.
     * Returns 0L if there are no issued policies.
     */
    fun calculateAverageIssuedPremiumPaise(summary: BusinessSummary): Long {
        return if (summary.totalIssuedPolicies > 0) {
            summary.totalIssuedPremiumPaise / summary.totalIssuedPolicies
        } else {
            0L
        }
    }

    /**
     * Calculates total premium of cancelled policies in exact paise.
     */
    fun calculateCancelledPremiumPaise(records: List<BusinessEntity>): Long {
        return records.filter { it.status == BusinessStatus.CANCELLED }
            .sumOf { it.premiumAmountPaise }
    }

    /**
     * Filters business records by date range (inclusive).
     * Compares against the record's transaction businessDate.
     */
    fun filterByDateRange(
        records: List<BusinessEntity>,
        startDateMillis: Long?,
        endDateMillis: Long?,
    ): List<BusinessEntity> {
        if (startDateMillis == null && endDateMillis == null) {
            return records
        }
        return records.filter { record ->
            val meetsStart = startDateMillis == null || record.businessDate >= startDateMillis
            val meetsEnd = endDateMillis == null || record.businessDate <= endDateMillis
            meetsStart && meetsEnd
        }
    }

    /**
     * Groups records by YearMonth of businessDate and computes performance metrics for each month.
     * Ordered chronologically descending (most recent month first).
     */
    fun calculateMonthlyPerformance(records: List<BusinessEntity>): List<com.dravid.businesstracker.data.model.MonthlyPerformance> {
        val byMonth = records.groupBy {
            com.dravid.businesstracker.util.DateUtils.toYearMonth(it.businessDate)
        }
        return byMonth.map { (month, monthRecords) ->
            com.dravid.businesstracker.data.model.MonthlyPerformance(
                yearMonth = month,
                summary = calculateSummary(monthRecords),
            )
        }.sortedByDescending { it.yearMonth }
    }

    /**
     * Calculates performance for each recognized lead/referral source.
     */
    fun calculateSourcePerformance(
        records: List<BusinessEntity>,
        sources: List<com.dravid.businesstracker.data.model.SourceEntity>,
    ): List<com.dravid.businesstracker.data.model.SourcePerformance> {
        val recordsBySource = records.groupBy { it.sourceId }
        return sources.map { source ->
            val sourceRecords = recordsBySource[source.id].orEmpty()
            com.dravid.businesstracker.data.model.SourcePerformance(
                sourceId = source.id,
                sourceName = source.name,
                summary = calculateSummary(sourceRecords),
            )
        }
    }

    /**
     * Calculates performance for business records with no assigned source (sourceId == null).
     */
    fun calculateUnassignedPerformance(
        records: List<BusinessEntity>,
    ): com.dravid.businesstracker.data.model.SourcePerformance {
        val unassignedRecords = records.filter { it.sourceId == null }
        return com.dravid.businesstracker.data.model.SourcePerformance(
            sourceId = null,
            sourceName = "Unassigned",
            summary = calculateSummary(unassignedRecords),
        )
    }

    /**
     * Generates a status breakdown detailing policy counts, premiums, and percentage proportions.
     */
    fun calculateStatusBreakdown(records: List<BusinessEntity>): List<com.dravid.businesstracker.data.model.StatusBreakdownItem> {
        val total = records.size
        val issued = records.filter { it.status == BusinessStatus.ISSUED }
        val pending = records.filter { it.status == BusinessStatus.PENDING }
        val cancelled = records.filter { it.status == BusinessStatus.CANCELLED }

        val issuedItem = com.dravid.businesstracker.data.model.StatusBreakdownItem(
            status = BusinessStatus.ISSUED,
            policyCount = issued.size,
            premiumPaise = issued.sumOf { it.premiumAmountPaise },
            percentageOfPolicies = if (total > 0) (issued.size * 100f / total) else 0f,
        )
        val pendingItem = com.dravid.businesstracker.data.model.StatusBreakdownItem(
            status = BusinessStatus.PENDING,
            policyCount = pending.size,
            premiumPaise = pending.sumOf { it.premiumAmountPaise },
            percentageOfPolicies = if (total > 0) (pending.size * 100f / total) else 0f,
        )
        val cancelledItem = com.dravid.businesstracker.data.model.StatusBreakdownItem(
            status = BusinessStatus.CANCELLED,
            policyCount = cancelled.size,
            premiumPaise = cancelled.sumOf { it.premiumAmountPaise },
            percentageOfPolicies = if (total > 0) (cancelled.size * 100f / total) else 0f,
        )

        return listOf(issuedItem, pendingItem, cancelledItem)
    }

    /**
     * Compiles a comprehensive business report from filtered records and source list.
     */
    fun generateReport(
        filteredRecords: List<BusinessEntity>,
        allSources: List<com.dravid.businesstracker.data.model.SourceEntity>,
        totalDatabaseRecordCount: Int,
    ): com.dravid.businesstracker.data.model.BusinessReport {
        val summary = calculateSummary(filteredRecords)
        val avgIssuedPaise = calculateAverageIssuedPremiumPaise(summary)
        val cancelledPremiumPaise = calculateCancelledPremiumPaise(filteredRecords)
        val monthly = calculateMonthlyPerformance(filteredRecords)
        val sourcePerf = calculateSourcePerformance(filteredRecords, allSources)
        val unassignedPerf = calculateUnassignedPerformance(filteredRecords)
        val breakdown = calculateStatusBreakdown(filteredRecords)

        return com.dravid.businesstracker.data.model.BusinessReport(
            summary = summary,
            averageIssuedPremiumPaise = avgIssuedPaise,
            cancelledPremiumPaise = cancelledPremiumPaise,
            monthlyPerformances = monthly,
            sourcePerformances = sourcePerf,
            unassignedPerformance = unassignedPerf,
            statusBreakdown = breakdown,
            filteredRecordCount = filteredRecords.size,
            totalDatabaseRecordCount = totalDatabaseRecordCount,
        )
    }
}
