package com.dravid.businesstracker.data.repository

import com.dravid.businesstracker.data.local.SourceDao
import com.dravid.businesstracker.data.model.BusinessEntity
import com.dravid.businesstracker.data.model.BusinessStatus
import com.dravid.businesstracker.data.model.SourceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SourceRepositoryTest {

    private lateinit var fakeSourceDao: FakeSourceDao
    private lateinit var fakeBusinessDao: FakeBusinessDao
    private lateinit var repository: SourceRepository

    @Before
    fun setup() {
        fakeSourceDao = FakeSourceDao()
        fakeBusinessDao = FakeBusinessDao()
        fakeSourceDao.onClearBusinesses = { sourceId ->
            fakeBusinessDao.clearSourceIdFromBusinesses(sourceId)
        }
        repository = SourceRepositoryImpl(
            sourceDao = fakeSourceDao,
            businessDao = fakeBusinessDao,
        )
    }

    @Test
    fun insertSource_validName_persistsSuccessfully() = runTest {
        val source = SourceEntity(name = "  Direct Channel  ", notes = "Corporate direct")
        val result = repository.insertSource(source)

        assertTrue(result.isSuccess)
        val id = result.getOrThrow()
        assertTrue(id > 0)

        val retrieved = repository.getSourceById(id)
        assertNotNull(retrieved)
        assertEquals("Direct Channel", retrieved?.name)
        assertEquals("Corporate direct", retrieved?.notes)
    }

    @Test
    fun insertSource_emptyName_failsValidation() = runTest {
        val blankSource = SourceEntity(name = "   ")
        val result = repository.insertSource(blankSource)

        assertFalse(result.isSuccess)
        assertEquals("Source name cannot be empty.", result.exceptionOrNull()?.message)
    }

    @Test
    fun insertSource_duplicateNameCaseInsensitive_failsValidation() = runTest {
        val first = SourceEntity(name = "Agency Alpha")
        assertTrue(repository.insertSource(first).isSuccess)

        val duplicate = SourceEntity(name = "  agency alpha  ")
        val result = repository.insertSource(duplicate)

        assertFalse(result.isSuccess)
        assertEquals("A source with this name already exists.", result.exceptionOrNull()?.message)
    }

    @Test
    fun updateSource_modifiesNameAndNotes() = runTest {
        val sourceId = repository.insertSource(SourceEntity(name = "Old Name")).getOrThrow()
        val updateResult = repository.updateSource(
            SourceEntity(id = sourceId, name = "New Name", notes = "Updated notes"),
        )

        assertTrue(updateResult.isSuccess)
        val updated = repository.getSourceById(sourceId)
        assertEquals("New Name", updated?.name)
        assertEquals("Updated notes", updated?.notes)
    }

    @Test
    fun updateSource_duplicateName_failsValidation() = runTest {
        repository.insertSource(SourceEntity(name = "Source One")).getOrThrow()
        val id2 = repository.insertSource(SourceEntity(name = "Source Two")).getOrThrow()

        val renameResult = repository.updateSource(
            SourceEntity(id = id2, name = "source one"),
        )

        assertFalse(renameResult.isSuccess)
        assertEquals("A source with this name already exists.", renameResult.exceptionOrNull()?.message)
    }

    @Test
    fun deleteSource_clearsSourceIdInBusinessRecords_doesNotDeleteBusinessRecords() = runTest {
        val sourceId = repository.insertSource(SourceEntity(name = "Referral Partner")).getOrThrow()

        // Insert business records associated with this source
        val b1 = fakeBusinessDao.insertBusiness(
            BusinessEntity(policyNumber = "POL-1", premiumAmountPaise = 100000L, status = BusinessStatus.ISSUED, businessDate = 1000L, sourceId = sourceId),
        )
        val b2 = fakeBusinessDao.insertBusiness(
            BusinessEntity(policyNumber = "POL-2", premiumAmountPaise = 50000L, status = BusinessStatus.PENDING, businessDate = 1000L, sourceId = sourceId),
        )

        // Delete the source
        val deleteResult = repository.deleteSource(sourceId)
        assertTrue(deleteResult.isSuccess)

        // Source entity is deleted
        assertNull(repository.getSourceById(sourceId))

        // Business records STILL EXIST, but sourceId is cleared to null
        val bus1 = fakeBusinessDao.getBusinessById(b1)
        val bus2 = fakeBusinessDao.getBusinessById(b2)
        assertNotNull(bus1)
        assertNotNull(bus2)
        assertNull(bus1?.sourceId)
        assertNull(bus2?.sourceId)
        assertEquals(2, fakeBusinessDao.getRecordCount())
    }

    @Test
    fun getSourcesWithAnalytics_calculatesAccurateMetricsInPaise() = runTest {
        val sourceId = repository.insertSource(SourceEntity(name = "Direct")).getOrThrow()

        // 1 ISSUED (₹1,000.50 = 100050 paise)
        fakeBusinessDao.insertBusiness(
            BusinessEntity(policyNumber = "P-1", premiumAmountPaise = 100050L, status = BusinessStatus.ISSUED, businessDate = 1000L, sourceId = sourceId),
        )
        // 1 PENDING (₹500.25 = 50025 paise)
        fakeBusinessDao.insertBusiness(
            BusinessEntity(policyNumber = "P-2", premiumAmountPaise = 50025L, status = BusinessStatus.PENDING, businessDate = 1000L, sourceId = sourceId),
        )
        // 1 CANCELLED
        fakeBusinessDao.insertBusiness(
            BusinessEntity(policyNumber = "P-3", premiumAmountPaise = 80000L, status = BusinessStatus.CANCELLED, businessDate = 1000L, sourceId = sourceId),
        )

        val analyticsList = repository.getSourcesWithAnalytics().first()
        assertEquals(1, analyticsList.size)

        val directAnalytics = analyticsList.first()
        assertEquals("Direct", directAnalytics.source.name)
        assertEquals(100050L, directAnalytics.summary.totalIssuedPremiumPaise)
        assertEquals(1, directAnalytics.summary.totalIssuedPolicies)
        assertEquals(50025L, directAnalytics.summary.pendingPremiumPaise)
        assertEquals(1, directAnalytics.summary.pendingPolicies)
        assertEquals(1, directAnalytics.summary.cancelledPolicies)
        assertEquals(3, directAnalytics.summary.totalRecords)
    }

    @Test
    fun getUnassignedAnalytics_calculatesMetricsForRecordsWithoutSource() = runTest {
        val assignedSourceId = repository.insertSource(SourceEntity(name = "Broker")).getOrThrow()

        // Assigned record
        fakeBusinessDao.insertBusiness(
            BusinessEntity(policyNumber = "P-ASSIGNED", premiumAmountPaise = 999900L, status = BusinessStatus.ISSUED, businessDate = 1000L, sourceId = assignedSourceId),
        )

        // Unassigned records
        fakeBusinessDao.insertBusiness(
            BusinessEntity(policyNumber = "P-UNASSIGNED-1", premiumAmountPaise = 200000L, status = BusinessStatus.ISSUED, businessDate = 1000L, sourceId = null),
        )
        fakeBusinessDao.insertBusiness(
            BusinessEntity(policyNumber = "P-UNASSIGNED-2", premiumAmountPaise = 150000L, status = BusinessStatus.PENDING, businessDate = 1000L, sourceId = null),
        )

        val unassigned = repository.getUnassignedAnalytics().first()
        assertEquals(200000L, unassigned.summary.totalIssuedPremiumPaise)
        assertEquals(1, unassigned.summary.totalIssuedPolicies)
        assertEquals(150000L, unassigned.summary.pendingPremiumPaise)
        assertEquals(1, unassigned.summary.pendingPolicies)
        assertEquals(2, unassigned.summary.totalRecords)
    }
}

class FakeSourceDao : SourceDao {
    private val sources = mutableListOf<SourceEntity>()
    private val flow = MutableStateFlow<List<SourceEntity>>(emptyList())
    private var nextId = 1L
    var onClearBusinesses: (suspend (Long) -> Unit)? = null

    override suspend fun clearSourceIdFromBusinesses(sourceId: Long) {
        onClearBusinesses?.invoke(sourceId)
    }

    private fun updateFlow() {
        flow.value = sources.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }

    override suspend fun insertSource(source: SourceEntity): Long {
        val id = if (source.id > 0) source.id else nextId++
        val entity = source.copy(id = id)
        sources.add(entity)
        updateFlow()
        return id
    }

    override suspend fun updateSource(source: SourceEntity) {
        val index = sources.indexOfFirst { it.id == source.id }
        if (index >= 0) {
            sources[index] = source
            updateFlow()
        }
    }

    override suspend fun deleteSource(source: SourceEntity) {
        sources.removeAll { it.id == source.id }
        updateFlow()
    }

    override suspend fun deleteSourceById(id: Long) {
        sources.removeAll { it.id == id }
        updateFlow()
    }

    override suspend fun getSourceById(id: Long): SourceEntity? {
        return sources.find { it.id == id }
    }

    override fun observeSourceById(id: Long): Flow<SourceEntity?> {
        return MutableStateFlow(sources.find { it.id == id })
    }

    override suspend fun getSourceByName(name: String): SourceEntity? {
        val trimmed = name.trim()
        return sources.firstOrNull { it.name.trim().equals(trimmed, ignoreCase = true) }
    }

    override fun getAllSources(): Flow<List<SourceEntity>> {
        return flow.asStateFlow()
    }

    override suspend fun getAllSourcesList(): List<SourceEntity> {
        return sources.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }

    override suspend fun getSourceCount(): Int {
        return sources.size
    }
}
