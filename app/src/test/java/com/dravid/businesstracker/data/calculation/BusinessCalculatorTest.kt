package com.dravid.businesstracker.data.calculation

import com.dravid.businesstracker.data.model.BusinessEntity
import com.dravid.businesstracker.data.model.BusinessStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class BusinessCalculatorTest {

    @Test
    fun calculateSummary_emptyList_returnsZeros() {
        val summary = BusinessCalculator.calculateSummary(emptyList())

        assertEquals(0L, summary.totalIssuedPremiumPaise)
        assertEquals(0, summary.totalIssuedPolicies)
        assertEquals(0L, summary.pendingPremiumPaise)
        assertEquals(0, summary.pendingPolicies)
        assertEquals(0, summary.cancelledPolicies)
        assertEquals(0, summary.totalRecords)
    }

    @Test
    fun calculateSummary_issuedPolicies_calculatedWithExactPaise() {
        val records = listOf(
            BusinessEntity(
                id = 1,
                policyNumber = "POL-001",
                premiumAmountPaise = 125050L, // ₹1,250.50
                status = BusinessStatus.ISSUED,
                businessDate = 1000L,
                issueDate = 1000L,
            ),
            BusinessEntity(
                id = 2,
                policyNumber = "POL-002",
                premiumAmountPaise = 274950L, // ₹2,749.50
                status = BusinessStatus.ISSUED,
                businessDate = 2000L,
                issueDate = 2100L,
            ),
        )

        val summary = BusinessCalculator.calculateSummary(records)

        // ₹1,250.50 + ₹2,749.50 = ₹4,000.00 (400000 paise exactly)
        assertEquals(400000L, summary.totalIssuedPremiumPaise)
        assertEquals(2, summary.totalIssuedPolicies)
        assertEquals(0L, summary.pendingPremiumPaise)
        assertEquals(0, summary.pendingPolicies)
        assertEquals(0, summary.cancelledPolicies)
        assertEquals(2, summary.totalRecords)
    }

    @Test
    fun calculateSummary_pendingPolicies_calculatedSeparately() {
        val records = listOf(
            BusinessEntity(
                id = 1,
                policyNumber = "POL-PEND-01",
                premiumAmountPaise = 85075L, // ₹850.75
                status = BusinessStatus.PENDING,
                businessDate = 1000L,
            ),
            BusinessEntity(
                id = 2,
                policyNumber = "POL-PEND-02",
                premiumAmountPaise = 64925L, // ₹649.25
                status = BusinessStatus.PENDING,
                businessDate = 2000L,
            ),
        )

        val summary = BusinessCalculator.calculateSummary(records)

        assertEquals(0L, summary.totalIssuedPremiumPaise)
        assertEquals(0, summary.totalIssuedPolicies)
        assertEquals(150000L, summary.pendingPremiumPaise) // ₹1,500.00 exactly
        assertEquals(2, summary.pendingPolicies)
        assertEquals(0, summary.cancelledPolicies)
        assertEquals(2, summary.totalRecords)
    }

    @Test
    fun calculateSummary_cancelledPolicies_excludedFromTotals() {
        val records = listOf(
            BusinessEntity(
                id = 1,
                policyNumber = "POL-CANCEL-01",
                premiumAmountPaise = 500000L,
                status = BusinessStatus.CANCELLED,
                businessDate = 1000L,
            ),
            BusinessEntity(
                id = 2,
                policyNumber = "POL-CANCEL-02",
                premiumAmountPaise = 300000L,
                status = BusinessStatus.CANCELLED,
                businessDate = 2000L,
            ),
        )

        val summary = BusinessCalculator.calculateSummary(records)

        // Cancelled records do NOT contribute to premium or policy totals
        assertEquals(0L, summary.totalIssuedPremiumPaise)
        assertEquals(0, summary.totalIssuedPolicies)
        assertEquals(0L, summary.pendingPremiumPaise)
        assertEquals(0, summary.pendingPolicies)
        assertEquals(2, summary.cancelledPolicies)
        assertEquals(2, summary.totalRecords)
    }

    @Test
    fun calculateSummary_mixedPortfolio_calculatesAccurately() {
        val records = listOf(
            BusinessEntity(
                id = 1,
                policyNumber = "POL-ISS-01",
                premiumAmountPaise = 1000000L, // ₹10,000.00
                status = BusinessStatus.ISSUED,
                businessDate = 1000L,
                issueDate = 1050L,
            ),
            BusinessEntity(
                id = 2,
                policyNumber = "POL-PEND-01",
                premiumAmountPaise = 450050L, // ₹4,500.50
                status = BusinessStatus.PENDING,
                businessDate = 2000L,
            ),
            BusinessEntity(
                id = 3,
                policyNumber = "POL-CANC-01",
                premiumAmountPaise = 750000L, // ₹7,500.00
                status = BusinessStatus.CANCELLED,
                businessDate = 3000L,
            ),
            BusinessEntity(
                id = 4,
                policyNumber = "POL-ISS-02",
                premiumAmountPaise = 500025L, // ₹5,000.25
                status = BusinessStatus.ISSUED,
                businessDate = 4000L,
                issueDate = 4000L,
            ),
        )

        val summary = BusinessCalculator.calculateSummary(records)

        assertEquals(1500025L, summary.totalIssuedPremiumPaise) // ₹15,000.25 exactly
        assertEquals(2, summary.totalIssuedPolicies)
        assertEquals(450050L, summary.pendingPremiumPaise) // ₹4,500.50 exactly
        assertEquals(1, summary.pendingPolicies)
        assertEquals(1, summary.cancelledPolicies)
        assertEquals(4, summary.totalRecords)
    }

    @Test
    fun calculateAverageIssuedPremiumPaise_exactIntegerPaise() {
        // 2 policies with total ₹30,000.50 (3000050 paise) -> 1500025 paise (₹15,000.25)
        val summary1 = com.dravid.businesstracker.data.model.BusinessSummary(
            totalIssuedPremiumPaise = 3000050L,
            totalIssuedPolicies = 2,
        )
        assertEquals(1500025L, BusinessCalculator.calculateAverageIssuedPremiumPaise(summary1))

        // Zero issued policies -> 0 paise
        val summaryZero = com.dravid.businesstracker.data.model.BusinessSummary(
            totalIssuedPremiumPaise = 0L,
            totalIssuedPolicies = 0,
        )
        assertEquals(0L, BusinessCalculator.calculateAverageIssuedPremiumPaise(summaryZero))
    }

    @Test
    fun calculateCancelledPremiumPaise_calculatesOnlyCancelled() {
        val records = listOf(
            BusinessEntity(
                id = 1,
                policyNumber = "POL-1",
                premiumAmountPaise = 100000L,
                status = BusinessStatus.ISSUED,
                businessDate = 1000L,
            ),
            BusinessEntity(
                id = 2,
                policyNumber = "POL-2",
                premiumAmountPaise = 250000L,
                status = BusinessStatus.CANCELLED,
                businessDate = 1000L,
            ),
            BusinessEntity(
                id = 3,
                policyNumber = "POL-3",
                premiumAmountPaise = 350000L,
                status = BusinessStatus.CANCELLED,
                businessDate = 1000L,
            ),
        )

        assertEquals(600000L, BusinessCalculator.calculateCancelledPremiumPaise(records))
    }

    @Test
    fun filterByDateRange_inclusiveBoundaries() {
        val startMillis = 1700000000000L
        val endMillis = 1700100000000L

        val records = listOf(
            BusinessEntity(id = 1, policyNumber = "BEFORE", premiumAmountPaise = 1000L, status = BusinessStatus.ISSUED, businessDate = startMillis - 1000L),
            BusinessEntity(id = 2, policyNumber = "EXACT_START", premiumAmountPaise = 2000L, status = BusinessStatus.ISSUED, businessDate = startMillis),
            BusinessEntity(id = 3, policyNumber = "INSIDE", premiumAmountPaise = 3000L, status = BusinessStatus.ISSUED, businessDate = startMillis + 50000L),
            BusinessEntity(id = 4, policyNumber = "EXACT_END", premiumAmountPaise = 4000L, status = BusinessStatus.ISSUED, businessDate = endMillis),
            BusinessEntity(id = 5, policyNumber = "AFTER", premiumAmountPaise = 5000L, status = BusinessStatus.ISSUED, businessDate = endMillis + 1000L),
        )

        val filtered = BusinessCalculator.filterByDateRange(records, startMillis, endMillis)
        assertEquals(3, filtered.size)
        assertEquals(listOf("EXACT_START", "INSIDE", "EXACT_END"), filtered.map { it.policyNumber })

        // Null bounds -> all records
        assertEquals(5, BusinessCalculator.filterByDateRange(records, null, null).size)
    }

    @Test
    fun calculateMonthlyPerformance_groupsByBusinessDateMonth() {
        // Month 1: Nov 2026 (epoch ~ 1794000000000L)
        val dateNov = com.dravid.businesstracker.util.DateUtils.toStartOfDayMillis(java.time.LocalDate.of(2026, 11, 15))
        // Month 2: Oct 2026
        val dateOct = com.dravid.businesstracker.util.DateUtils.toStartOfDayMillis(java.time.LocalDate.of(2026, 10, 20))

        val records = listOf(
            BusinessEntity(id = 1, policyNumber = "NOV-1", premiumAmountPaise = 100000L, status = BusinessStatus.ISSUED, businessDate = dateNov),
            BusinessEntity(id = 2, policyNumber = "NOV-2", premiumAmountPaise = 50000L, status = BusinessStatus.PENDING, businessDate = dateNov),
            BusinessEntity(id = 3, policyNumber = "OCT-1", premiumAmountPaise = 200000L, status = BusinessStatus.ISSUED, businessDate = dateOct),
        )

        val monthly = BusinessCalculator.calculateMonthlyPerformance(records)
        assertEquals(2, monthly.size)

        val novPerf = monthly.find { it.yearMonth == java.time.YearMonth.of(2026, 11) }
        org.junit.Assert.assertNotNull(novPerf)
        assertEquals(100000L, novPerf?.summary?.totalIssuedPremiumPaise)
        assertEquals(1, novPerf?.summary?.totalIssuedPolicies)
        assertEquals(50000L, novPerf?.summary?.pendingPremiumPaise)
        assertEquals(1, novPerf?.summary?.pendingPolicies)

        val octPerf = monthly.find { it.yearMonth == java.time.YearMonth.of(2026, 10) }
        org.junit.Assert.assertNotNull(octPerf)
        assertEquals(200000L, octPerf?.summary?.totalIssuedPremiumPaise)
        assertEquals(1, octPerf?.summary?.totalIssuedPolicies)
    }

    @Test
    fun calculateSourcePerformance_aggregatesBySourceAndUnassigned() {
        val sources = listOf(
            com.dravid.businesstracker.data.model.SourceEntity(id = 10L, name = "Direct Channel"),
            com.dravid.businesstracker.data.model.SourceEntity(id = 20L, name = "Agent Referral"),
        )

        val records = listOf(
            BusinessEntity(id = 1, policyNumber = "DIR-1", premiumAmountPaise = 500000L, status = BusinessStatus.ISSUED, businessDate = 1000L, sourceId = 10L),
            BusinessEntity(id = 2, policyNumber = "DIR-2", premiumAmountPaise = 100000L, status = BusinessStatus.PENDING, businessDate = 1000L, sourceId = 10L),
            BusinessEntity(id = 3, policyNumber = "REF-1", premiumAmountPaise = 300000L, status = BusinessStatus.ISSUED, businessDate = 1000L, sourceId = 20L),
            BusinessEntity(id = 4, policyNumber = "UNAS-1", premiumAmountPaise = 200000L, status = BusinessStatus.ISSUED, businessDate = 1000L, sourceId = null),
        )

        val sourcePerf = BusinessCalculator.calculateSourcePerformance(records, sources)
        assertEquals(2, sourcePerf.size)

        val direct = sourcePerf.find { it.sourceId == 10L }
        org.junit.Assert.assertNotNull(direct)
        assertEquals("Direct Channel", direct?.sourceName)
        assertEquals(500000L, direct?.summary?.totalIssuedPremiumPaise)
        assertEquals(100000L, direct?.summary?.pendingPremiumPaise)

        val unassigned = BusinessCalculator.calculateUnassignedPerformance(records)
        assertEquals(null, unassigned.sourceId)
        assertEquals("Unassigned", unassigned.sourceName)
        assertEquals(200000L, unassigned.summary.totalIssuedPremiumPaise)
    }

    @Test
    fun calculateStatusBreakdown_calculatesProportionsAndCounts() {
        val records = listOf(
            BusinessEntity(id = 1, policyNumber = "ISS-1", premiumAmountPaise = 400000L, status = BusinessStatus.ISSUED, businessDate = 1000L),
            BusinessEntity(id = 2, policyNumber = "ISS-2", premiumAmountPaise = 600000L, status = BusinessStatus.ISSUED, businessDate = 1000L),
            BusinessEntity(id = 3, policyNumber = "PEND-1", premiumAmountPaise = 300000L, status = BusinessStatus.PENDING, businessDate = 1000L),
            BusinessEntity(id = 4, policyNumber = "CANC-1", premiumAmountPaise = 100000L, status = BusinessStatus.CANCELLED, businessDate = 1000L),
        )

        val breakdown = BusinessCalculator.calculateStatusBreakdown(records)
        assertEquals(3, breakdown.size)

        val issued = breakdown.find { it.status == BusinessStatus.ISSUED }!!
        assertEquals(2, issued.policyCount)
        assertEquals(1000000L, issued.premiumPaise)
        assertEquals(50.0f, issued.percentageOfPolicies, 0.01f)

        val pending = breakdown.find { it.status == BusinessStatus.PENDING }!!
        assertEquals(1, pending.policyCount)
        assertEquals(300000L, pending.premiumPaise)
        assertEquals(25.0f, pending.percentageOfPolicies, 0.01f)

        val cancelled = breakdown.find { it.status == BusinessStatus.CANCELLED }!!
        assertEquals(1, cancelled.policyCount)
        assertEquals(100000L, cancelled.premiumPaise)
        assertEquals(25.0f, cancelled.percentageOfPolicies, 0.01f)
    }
}
