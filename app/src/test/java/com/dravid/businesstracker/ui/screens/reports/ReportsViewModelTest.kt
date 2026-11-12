package com.dravid.businesstracker.ui.screens.reports

import com.dravid.businesstracker.data.model.BusinessEntity
import com.dravid.businesstracker.data.model.BusinessStatus
import com.dravid.businesstracker.data.model.BusinessSummary
import com.dravid.businesstracker.data.model.ReportPeriodFilter
import com.dravid.businesstracker.data.model.SourceEntity
import com.dravid.businesstracker.data.model.SourceWithAnalytics
import com.dravid.businesstracker.data.model.UnassignedSourceAnalytics
import com.dravid.businesstracker.data.repository.BusinessRepository
import com.dravid.businesstracker.data.repository.SourceRepository
import com.dravid.businesstracker.util.DateUtils
import com.dravid.businesstracker.util.ValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeBusinessRepo: FakeBusinessRepoForReports
    private lateinit var fakeSourceRepo: FakeSourceRepoForReports
    private lateinit var viewModel: ReportsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeBusinessRepo = FakeBusinessRepoForReports()
        fakeSourceRepo = FakeSourceRepoForReports()
        viewModel = ReportsViewModel(fakeBusinessRepo, fakeSourceRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialLoad_emptyDatabase_reportsZeroMetrics() = runTest {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(ReportPeriodFilter.ALL_TIME, state.selectedFilter)
        assertEquals(0, state.report.filteredRecordCount)
        assertEquals(0, state.report.totalDatabaseRecordCount)
        assertEquals(0L, state.report.summary.totalIssuedPremiumPaise)
        assertEquals(0, state.report.summary.totalIssuedPolicies)
        assertEquals(0L, state.report.averageIssuedPremiumPaise)
    }

    @Test
    fun initialLoad_populatedDatabase_calculatesAllMetricsAccurately() = runTest {
        val today = LocalDate.now(ZoneId.systemDefault())
        val todayMillis = DateUtils.toStartOfDayMillis(today)

        val sources = listOf(
            SourceEntity(id = 10L, name = "Direct Channel"),
            SourceEntity(id = 20L, name = "Partner Agency"),
        )
        fakeSourceRepo.emitSources(sources)

        val records = listOf(
            // Issued: ₹10,000.00 (1000000 paise)
            BusinessEntity(id = 1, policyNumber = "POL-01", premiumAmountPaise = 1000000L, status = BusinessStatus.ISSUED, businessDate = todayMillis, sourceId = 10L),
            // Issued: ₹20,000.00 (2000000 paise)
            BusinessEntity(id = 2, policyNumber = "POL-02", premiumAmountPaise = 2000000L, status = BusinessStatus.ISSUED, businessDate = todayMillis, sourceId = 20L),
            // Pending: ₹5,000.00 (500000 paise)
            BusinessEntity(id = 3, policyNumber = "POL-03", premiumAmountPaise = 500000L, status = BusinessStatus.PENDING, businessDate = todayMillis, sourceId = 10L),
            // Cancelled: ₹7,500.00 (750000 paise) - excluded from active revenue
            BusinessEntity(id = 4, policyNumber = "POL-04", premiumAmountPaise = 750000L, status = BusinessStatus.CANCELLED, businessDate = todayMillis, sourceId = null),
            // Unassigned Issued: ₹15,000.00 (1500000 paise)
            BusinessEntity(id = 5, policyNumber = "POL-05", premiumAmountPaise = 1500000L, status = BusinessStatus.ISSUED, businessDate = todayMillis, sourceId = null),
        )
        fakeBusinessRepo.emitBusinesses(records)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        val report = state.report

        assertEquals(5, report.totalDatabaseRecordCount)
        assertEquals(5, report.filteredRecordCount)

        // Total Issued: 10,000 + 20,000 + 15,000 = ₹45,000.00 (4500000 paise)
        assertEquals(4500000L, report.summary.totalIssuedPremiumPaise)
        assertEquals(3, report.summary.totalIssuedPolicies)

        // Average: 4500000 / 3 = 1500000 paise (₹15,000.00)
        assertEquals(1500000L, report.averageIssuedPremiumPaise)

        // Pending: ₹5,000.00 (500000 paise)
        assertEquals(500000L, report.summary.pendingPremiumPaise)
        assertEquals(1, report.summary.pendingPolicies)

        // Cancelled: 1 policy
        assertEquals(1, report.summary.cancelledPolicies)
        assertEquals(750000L, report.cancelledPremiumPaise)

        // Source performance
        assertEquals(2, report.sourcePerformances.size)
        val direct = report.sourcePerformances.find { it.sourceId == 10L }
        assertNotNull(direct)
        assertEquals(1000000L, direct?.summary?.totalIssuedPremiumPaise)
        assertEquals(500000L, direct?.summary?.pendingPremiumPaise)

        // Unassigned performance
        assertEquals(1500000L, report.unassignedPerformance.summary.totalIssuedPremiumPaise)
        assertEquals(1, report.unassignedPerformance.summary.cancelledPolicies)
    }

    @Test
    fun periodFilters_filterByDateBoundaries() = runTest {
        val today = LocalDate.now(ZoneId.systemDefault())
        val thisMonth = YearMonth.from(today)
        val lastMonth = thisMonth.minusMonths(1)
        val lastYear = today.year - 1

        val dateThisMonth = DateUtils.toStartOfDayMillis(today)
        val dateLastMonth = DateUtils.toStartOfDayMillis(lastMonth.atDay(10))
        val dateLastYear = DateUtils.toStartOfDayMillis(LocalDate.of(lastYear, 6, 15))

        val records = listOf(
            BusinessEntity(id = 1, policyNumber = "P-THIS-MONTH", premiumAmountPaise = 100000L, status = BusinessStatus.ISSUED, businessDate = dateThisMonth),
            BusinessEntity(id = 2, policyNumber = "P-LAST-MONTH", premiumAmountPaise = 200000L, status = BusinessStatus.ISSUED, businessDate = dateLastMonth),
            BusinessEntity(id = 3, policyNumber = "P-LAST-YEAR", premiumAmountPaise = 300000L, status = BusinessStatus.ISSUED, businessDate = dateLastYear),
        )
        fakeBusinessRepo.emitBusinesses(records)
        advanceUntilIdle()

        // 1. ALL TIME -> 3 records
        assertEquals(3, viewModel.uiState.value.report.filteredRecordCount)

        // 2. THIS MONTH -> 1 record
        viewModel.setPeriodFilter(ReportPeriodFilter.THIS_MONTH)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.report.filteredRecordCount)
        assertEquals(100000L, viewModel.uiState.value.report.summary.totalIssuedPremiumPaise)

        // 3. LAST MONTH -> 1 record
        viewModel.setPeriodFilter(ReportPeriodFilter.LAST_MONTH)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.report.filteredRecordCount)
        assertEquals(200000L, viewModel.uiState.value.report.summary.totalIssuedPremiumPaise)

        // 4. THIS YEAR -> 2 records (this month + last month, provided last month is in the same year, else at least this month)
        viewModel.setPeriodFilter(ReportPeriodFilter.THIS_YEAR)
        advanceUntilIdle()
        val expectedThisYear = records.filter {
            val recDate = DateUtils.toLocalDate(it.businessDate)
            recDate.year == today.year
        }.size
        assertEquals(expectedThisYear, viewModel.uiState.value.report.filteredRecordCount)

        // 5. CUSTOM RANGE -> exact boundaries
        viewModel.applyCustomDateRange(dateLastMonth, dateLastMonth)
        advanceUntilIdle()
        assertEquals(ReportPeriodFilter.CUSTOM, viewModel.uiState.value.selectedFilter)
        assertEquals(1, viewModel.uiState.value.report.filteredRecordCount)
        assertEquals("P-LAST-MONTH", records.find { it.businessDate == dateLastMonth }?.policyNumber)

        // 6. Reset to ALL TIME
        viewModel.resetFilterToAllTime()
        advanceUntilIdle()
        assertEquals(ReportPeriodFilter.ALL_TIME, viewModel.uiState.value.selectedFilter)
        assertEquals(3, viewModel.uiState.value.report.filteredRecordCount)
    }

    @Test
    fun liveUpdates_recalculatesAutomaticallyWhenRepositoryChanges() = runTest {
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.report.filteredRecordCount)

        // Step 1: Add initial record
        val initialRecord = BusinessEntity(
            id = 1,
            policyNumber = "POL-LIVE-1",
            premiumAmountPaise = 500000L,
            status = BusinessStatus.ISSUED,
            businessDate = DateUtils.todayMillis(),
        )
        fakeBusinessRepo.emitBusinesses(listOf(initialRecord))
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.report.filteredRecordCount)
        assertEquals(500000L, viewModel.uiState.value.report.summary.totalIssuedPremiumPaise)

        // Step 2: Edit record status to PENDING
        val editedRecord = initialRecord.copy(status = BusinessStatus.PENDING)
        fakeBusinessRepo.emitBusinesses(listOf(editedRecord))
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.report.filteredRecordCount)
        assertEquals(0L, viewModel.uiState.value.report.summary.totalIssuedPremiumPaise)
        assertEquals(500000L, viewModel.uiState.value.report.summary.pendingPremiumPaise)

        // Step 3: Delete record
        fakeBusinessRepo.emitBusinesses(emptyList())
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.report.filteredRecordCount)
        assertEquals(0L, viewModel.uiState.value.report.summary.pendingPremiumPaise)
    }

    @Test
    fun customDateDialog_handlesStateAndCandidateSelection() = runTest {
        viewModel.openCustomRangeDialog()
        assertTrue(viewModel.uiState.value.isCustomDatePickerOpen)

        val start = 1700000000000L
        val end = 1700500000000L
        viewModel.updateCustomStartCandidate(start)
        viewModel.updateCustomEndCandidate(end)

        assertEquals(start, viewModel.uiState.value.customStartCandidate)
        assertEquals(end, viewModel.uiState.value.customEndCandidate)

        viewModel.closeCustomRangeDialog()
        assertFalse(viewModel.uiState.value.isCustomDatePickerOpen)
    }
}

class FakeBusinessRepoForReports : BusinessRepository {
    private val recordsFlow = MutableStateFlow<List<BusinessEntity>>(emptyList())
    private val summaryFlow = MutableStateFlow(BusinessSummary())

    fun emitBusinesses(records: List<BusinessEntity>) {
        recordsFlow.value = records
    }

    override fun getAllBusinesses(): Flow<List<BusinessEntity>> = recordsFlow.asStateFlow()
    override fun getBusinessSummary(): Flow<BusinessSummary> = summaryFlow.asStateFlow()
    override suspend fun getBusinessById(id: Long): BusinessEntity? = recordsFlow.value.find { it.id == id }
    override fun observeBusinessById(id: Long): Flow<BusinessEntity?> = MutableStateFlow(recordsFlow.value.find { it.id == id })
    override suspend fun insertBusiness(business: BusinessEntity): Result<Long> = Result.success(business.id)
    override suspend fun updateBusiness(business: BusinessEntity): Result<Unit> = Result.success(Unit)
    override suspend fun deleteBusiness(business: BusinessEntity): Result<Unit> = Result.success(Unit)
    override suspend fun deleteBusinessById(id: Long): Result<Unit> = Result.success(Unit)
    override fun validateBusiness(
        policyNumber: String,
        premiumAmountPaise: Long,
        status: BusinessStatus,
        businessDate: Long,
        issueDate: Long?,
    ): ValidationResult = ValidationResult.Success
}

class FakeSourceRepoForReports : SourceRepository {
    private val sourcesFlow = MutableStateFlow<List<SourceEntity>>(emptyList())
    private val analyticsFlow = MutableStateFlow<List<SourceWithAnalytics>>(emptyList())
    private val unassignedFlow = MutableStateFlow(UnassignedSourceAnalytics(summary = BusinessSummary()))

    fun emitSources(sources: List<SourceEntity>) {
        sourcesFlow.value = sources
    }

    override fun getAllSources(): Flow<List<SourceEntity>> = sourcesFlow.asStateFlow()
    override fun getSourcesWithAnalytics(): Flow<List<SourceWithAnalytics>> = analyticsFlow.asStateFlow()
    override fun getUnassignedAnalytics(): Flow<UnassignedSourceAnalytics> = unassignedFlow.asStateFlow()
    override suspend fun getSourceById(id: Long): SourceEntity? = sourcesFlow.value.find { it.id == id }
    override suspend fun insertSource(source: SourceEntity): Result<Long> = Result.success(source.id)
    override suspend fun updateSource(source: SourceEntity): Result<Unit> = Result.success(Unit)
    override suspend fun deleteSource(sourceId: Long): Result<Unit> = Result.success(Unit)
    override suspend fun validateSource(name: String, currentId: Long?): ValidationResult = ValidationResult.Success
}
