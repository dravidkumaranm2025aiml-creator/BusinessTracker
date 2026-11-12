package com.dravid.businesstracker.ui.screens.business

import com.dravid.businesstracker.data.model.BusinessEntity
import com.dravid.businesstracker.data.model.BusinessStatus
import com.dravid.businesstracker.data.model.BusinessSummary
import com.dravid.businesstracker.data.model.SourceEntity
import com.dravid.businesstracker.data.model.SourceWithAnalytics
import com.dravid.businesstracker.data.model.UnassignedSourceAnalytics
import com.dravid.businesstracker.data.repository.BusinessRepository
import com.dravid.businesstracker.data.repository.SourceRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BusinessViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeBusinessRepository
    private lateinit var fakeSourceRepository: FakeSourceRepository
    private lateinit var viewModel: BusinessViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeBusinessRepository()
        fakeSourceRepository = FakeSourceRepository()
        viewModel = BusinessViewModel(fakeRepository, fakeSourceRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialLoad_populatesRecordsAndFilteredList() = runTest {
        val initialList = listOf(
            BusinessEntity(id = 1, policyNumber = "POL-A", premiumAmountPaise = 10000L, status = BusinessStatus.ISSUED, businessDate = 1000L),
            BusinessEntity(id = 2, policyNumber = "POL-B", premiumAmountPaise = 20000L, status = BusinessStatus.PENDING, businessDate = 2000L),
        )
        fakeRepository.emitRecords(initialList)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.records.size)
        assertEquals(2, state.filteredRecords.size)
    }

    @Test
    fun initialLoad_loadsAvailableSources() = runTest {
        val sources = listOf(
            SourceEntity(id = 1L, name = "Direct"),
            SourceEntity(id = 2L, name = "Agent Smith"),
        )
        fakeSourceRepository.emitSources(sources)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.availableSources.size)
        assertEquals("Direct", state.availableSources[0].name)
    }

    @Test
    fun searchFilter_filtersByPolicyNumber() = runTest {
        val initialList = listOf(
            BusinessEntity(id = 1, policyNumber = "POL-100", premiumAmountPaise = 10000L, status = BusinessStatus.ISSUED, businessDate = 1000L),
            BusinessEntity(id = 2, policyNumber = "POL-200", premiumAmountPaise = 20000L, status = BusinessStatus.PENDING, businessDate = 2000L),
        )
        fakeRepository.emitRecords(initialList)
        advanceUntilIdle()

        viewModel.setSearchQuery("200")
        assertEquals(1, viewModel.uiState.value.filteredRecords.size)
        assertEquals("POL-200", viewModel.uiState.value.filteredRecords.first().policyNumber)
    }

    @Test
    fun statusFilter_filtersByStatus() = runTest {
        val initialList = listOf(
            BusinessEntity(id = 1, policyNumber = "POL-A", premiumAmountPaise = 10000L, status = BusinessStatus.ISSUED, businessDate = 1000L),
            BusinessEntity(id = 2, policyNumber = "POL-B", premiumAmountPaise = 20000L, status = BusinessStatus.PENDING, businessDate = 2000L),
        )
        fakeRepository.emitRecords(initialList)
        advanceUntilIdle()

        viewModel.setStatusFilter(BusinessStatus.ISSUED)
        assertEquals(1, viewModel.uiState.value.filteredRecords.size)
        assertEquals("POL-A", viewModel.uiState.value.filteredRecords.first().policyNumber)
    }

    @Test
    fun addRecord_withSource_savesSourceIdCorrectly() = runTest {
        viewModel.openAddForm()
        viewModel.updateFormPolicyNumber("POL-NEW")
        viewModel.updateFormPremiumAmount("1500.50")
        viewModel.updateFormStatus(BusinessStatus.PENDING)
        viewModel.updateFormSourceId(10L)
        viewModel.saveRecord()
        advanceUntilIdle()

        val saved = fakeRepository.recordsList.firstOrNull { it.policyNumber == "POL-NEW" }
        assertNotNull(saved)
        assertEquals(150050L, saved?.premiumAmountPaise)
        assertEquals(10L, saved?.sourceId)
    }

    @Test
    fun editRecord_preservesAndUpdatesSourceId() = runTest {
        val existing = BusinessEntity(
            id = 5L,
            policyNumber = "POL-EDIT",
            premiumAmountPaise = 250000L,
            status = BusinessStatus.PENDING,
            businessDate = 1000L,
            sourceId = 2L,
        )
        fakeRepository.recordsList.add(existing)

        viewModel.openEditForm(existing)
        assertEquals(2L, viewModel.uiState.value.formSourceId)

        viewModel.updateFormSourceId(3L)
        viewModel.saveRecord()
        advanceUntilIdle()

        val updated = fakeRepository.recordsList.first { it.id == 5L }
        assertEquals(3L, updated.sourceId)
    }

    @Test
    fun deleteRecord_removesRecordAndShowsSuccessMessage() = runTest {
        val record = BusinessEntity(id = 1, policyNumber = "POL-DEL", premiumAmountPaise = 10000L, status = BusinessStatus.PENDING, businessDate = 1000L)
        fakeRepository.emitRecords(listOf(record))
        advanceUntilIdle()

        viewModel.requestDelete(record)
        assertEquals(record, viewModel.uiState.value.recordToDelete)

        viewModel.confirmDelete()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.recordToDelete)
        assertEquals("Record deleted.", viewModel.uiState.value.successMessage)
        assertEquals(0, fakeRepository.recordsList.size)
    }
}

class FakeBusinessRepository : BusinessRepository {
    val recordsList = mutableListOf<BusinessEntity>()
    private val flow = MutableStateFlow<List<BusinessEntity>>(emptyList())
    private val summaryFlow = MutableStateFlow(BusinessSummary())

    fun emitRecords(records: List<BusinessEntity>) {
        recordsList.clear()
        recordsList.addAll(records)
        flow.value = recordsList.toList()
    }

    override fun getAllBusinesses(): Flow<List<BusinessEntity>> = flow.asStateFlow()

    override fun getBusinessSummary(): Flow<BusinessSummary> = summaryFlow.asStateFlow()

    override suspend fun getBusinessById(id: Long): BusinessEntity? =
        recordsList.find { it.id == id }

    override fun observeBusinessById(id: Long): Flow<BusinessEntity?> =
        MutableStateFlow(recordsList.find { it.id == id })

    override suspend fun insertBusiness(business: BusinessEntity): Result<Long> {
        val newId = (recordsList.maxOfOrNull { it.id } ?: 0L) + 1
        val newEntity = business.copy(id = newId)
        recordsList.add(newEntity)
        flow.value = recordsList.toList()
        return Result.success(newId)
    }

    override suspend fun updateBusiness(business: BusinessEntity): Result<Unit> {
        val index = recordsList.indexOfFirst { it.id == business.id }
        if (index >= 0) {
            recordsList[index] = business
            flow.value = recordsList.toList()
        }
        return Result.success(Unit)
    }

    override suspend fun deleteBusiness(business: BusinessEntity): Result<Unit> {
        recordsList.removeAll { it.id == business.id }
        flow.value = recordsList.toList()
        return Result.success(Unit)
    }

    override suspend fun deleteBusinessById(id: Long): Result<Unit> {
        recordsList.removeAll { it.id == id }
        flow.value = recordsList.toList()
        return Result.success(Unit)
    }

    override fun validateBusiness(
        policyNumber: String,
        premiumAmountPaise: Long,
        status: BusinessStatus,
        businessDate: Long,
        issueDate: Long?,
    ): ValidationResult {
        if (policyNumber.isBlank()) return ValidationResult.Error("Policy / Reference ID cannot be empty.")
        if (premiumAmountPaise <= 0L) return ValidationResult.Error("Premium amount must be greater than zero.")
        return ValidationResult.Success
    }
}

class FakeSourceRepository : SourceRepository {
    val sourcesList = mutableListOf<SourceEntity>()
    private val sourcesFlow = MutableStateFlow<List<SourceEntity>>(emptyList())
    private val analyticsFlow = MutableStateFlow<List<SourceWithAnalytics>>(emptyList())
    private val unassignedFlow = MutableStateFlow(UnassignedSourceAnalytics(summary = BusinessSummary()))

    fun emitSources(sources: List<SourceEntity>) {
        sourcesList.clear()
        sourcesList.addAll(sources)
        sourcesFlow.value = sourcesList.toList()
    }

    override fun getAllSources(): Flow<List<SourceEntity>> = sourcesFlow.asStateFlow()
    override fun getSourcesWithAnalytics(): Flow<List<SourceWithAnalytics>> = analyticsFlow.asStateFlow()
    override fun getUnassignedAnalytics(): Flow<UnassignedSourceAnalytics> = unassignedFlow.asStateFlow()
    override suspend fun getSourceById(id: Long): SourceEntity? = sourcesList.find { it.id == id }

    override suspend fun insertSource(source: SourceEntity): Result<Long> {
        val newId = (sourcesList.maxOfOrNull { it.id } ?: 0L) + 1
        val newEntity = source.copy(id = newId)
        sourcesList.add(newEntity)
        sourcesFlow.value = sourcesList.toList()
        return Result.success(newId)
    }

    override suspend fun updateSource(source: SourceEntity): Result<Unit> {
        val index = sourcesList.indexOfFirst { it.id == source.id }
        if (index >= 0) {
            sourcesList[index] = source
            sourcesFlow.value = sourcesList.toList()
        }
        return Result.success(Unit)
    }

    override suspend fun deleteSource(sourceId: Long): Result<Unit> {
        sourcesList.removeAll { it.id == sourceId }
        sourcesFlow.value = sourcesList.toList()
        return Result.success(Unit)
    }

    override suspend fun validateSource(name: String, currentId: Long?): ValidationResult {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return ValidationResult.Error("Source name cannot be empty.")
        val duplicate = sourcesList.any { it.name.equals(trimmed, ignoreCase = true) && it.id != currentId }
        if (duplicate) return ValidationResult.Error("A source with this name already exists.")
        return ValidationResult.Success
    }
}
