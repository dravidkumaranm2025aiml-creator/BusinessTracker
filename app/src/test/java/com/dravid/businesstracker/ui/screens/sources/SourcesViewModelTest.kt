package com.dravid.businesstracker.ui.screens.sources

import com.dravid.businesstracker.data.model.BusinessSummary
import com.dravid.businesstracker.data.model.SourceEntity
import com.dravid.businesstracker.data.model.SourceWithAnalytics
import com.dravid.businesstracker.data.model.UnassignedSourceAnalytics
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SourcesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeSourceRepoForViewModel
    private lateinit var viewModel: SourcesViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeSourceRepoForViewModel()
        viewModel = SourcesViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialLoad_populatesSourcesAndUnassigned() = runTest {
        val sources = listOf(
            SourceWithAnalytics(
                source = SourceEntity(id = 1L, name = "Direct Channel"),
                summary = BusinessSummary(totalIssuedPremiumPaise = 500000L, totalIssuedPolicies = 2, totalRecords = 2),
            ),
        )
        fakeRepository.emitAnalytics(sources)
        fakeRepository.emitUnassigned(
            UnassignedSourceAnalytics(
                summary = BusinessSummary(totalIssuedPremiumPaise = 100000L, totalIssuedPolicies = 1, totalRecords = 1),
            ),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.sources.size)
        assertEquals("Direct Channel", state.sources[0].source.name)
        assertEquals(500000L, state.sources[0].summary.totalIssuedPremiumPaise)
        assertEquals(1, state.unassignedAnalytics?.summary?.totalIssuedPolicies)
    }

    @Test
    fun addSource_validation_emptyNameShowsError() = runTest {
        viewModel.openAddForm()
        viewModel.updateFormName("   ")
        viewModel.saveSource()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isFormOpen)
        assertEquals("Source name cannot be empty.", viewModel.uiState.value.formValidationMessage)
    }

    @Test
    fun addSource_success_savesSourceAndShowsSuccessMessage() = runTest {
        viewModel.openAddForm()
        viewModel.updateFormName("Partner Agency")
        viewModel.updateFormNotes("Direct broker agreement")
        viewModel.saveSource()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isFormOpen)
        assertNull(viewModel.uiState.value.formValidationMessage)
        assertNotNull(viewModel.uiState.value.successMessage)
        assertTrue(viewModel.uiState.value.successMessage!!.contains("Partner Agency"))

        val saved = fakeRepository.sourcesList.firstOrNull { it.name == "Partner Agency" }
        assertNotNull(saved)
        assertEquals("Direct broker agreement", saved?.notes)
    }

    @Test
    fun addSource_duplicateName_showsValidationError() = runTest {
        fakeRepository.sourcesList.add(SourceEntity(id = 1L, name = "Direct"))

        viewModel.openAddForm()
        viewModel.updateFormName("  direct  ")
        viewModel.saveSource()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isFormOpen)
        assertEquals("A source with this name already exists.", viewModel.uiState.value.formValidationMessage)
    }

    @Test
    fun editSource_flow_populatesAndUpdatesSource() = runTest {
        val existing = SourceEntity(id = 5L, name = "Old Channel", notes = "Initial notes")
        fakeRepository.sourcesList.add(existing)

        viewModel.openEditForm(existing)
        assertEquals(5L, viewModel.uiState.value.editingSourceId)
        assertEquals("Old Channel", viewModel.uiState.value.formName)
        assertEquals("Initial notes", viewModel.uiState.value.formNotes)

        viewModel.updateFormName("New Channel")
        viewModel.updateFormNotes("Updated notes")
        viewModel.saveSource()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isFormOpen)
        assertEquals("Source 'New Channel' updated successfully.", viewModel.uiState.value.successMessage)

        val updated = fakeRepository.sourcesList.first { it.id == 5L }
        assertEquals("New Channel", updated.name)
        assertEquals("Updated notes", updated.notes)
    }

    @Test
    fun deleteSource_confirmation_deletesSourceAndShowsMessage() = runTest {
        val source = SourceEntity(id = 10L, name = "To Delete")
        fakeRepository.sourcesList.add(source)

        viewModel.requestDelete(source)
        assertEquals(source, viewModel.uiState.value.sourceToDelete)

        viewModel.confirmDelete()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.sourceToDelete)
        assertEquals("Source 'To Delete' deleted.", viewModel.uiState.value.successMessage)
        assertTrue(fakeRepository.sourcesList.none { it.id == 10L })
    }
}

class FakeSourceRepoForViewModel : SourceRepository {
    val sourcesList = mutableListOf<SourceEntity>()
    private val sourcesFlow = MutableStateFlow<List<SourceEntity>>(emptyList())
    private val analyticsFlow = MutableStateFlow<List<SourceWithAnalytics>>(emptyList())
    private val unassignedFlow = MutableStateFlow(UnassignedSourceAnalytics(summary = BusinessSummary()))

    fun emitAnalytics(analytics: List<SourceWithAnalytics>) {
        analyticsFlow.value = analytics
    }

    fun emitUnassigned(unassigned: UnassignedSourceAnalytics) {
        unassignedFlow.value = unassigned
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
