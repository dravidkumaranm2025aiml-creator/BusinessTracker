package com.dravid.businesstracker.data.repository

import com.dravid.businesstracker.data.local.BusinessDao
import com.dravid.businesstracker.data.model.BusinessEntity
import com.dravid.businesstracker.data.model.BusinessStatus
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

class BusinessRepositoryTest {

    private lateinit var fakeDao: FakeBusinessDao
    private lateinit var repository: BusinessRepository

    @Before
    fun setup() {
        fakeDao = FakeBusinessDao()
        repository = BusinessRepositoryImpl(fakeDao)
    }

    @Test
    fun addRecord_persistsExactPaiseAndCalculatesSummary() = runTest {
        // Record with exact value ₹1,250.50 (125050 paise)
        val record = BusinessEntity(
            policyNumber = "POL-1001",
            premiumAmountPaise = 125050L,
            status = BusinessStatus.ISSUED,
            businessDate = 1000L,
            issueDate = 1000L,
        )

        val result = repository.insertBusiness(record)
        assertTrue(result.isSuccess)
        val id = result.getOrThrow()
        assertTrue(id > 0)

        // Verify exact data persistence
        val persisted = repository.getBusinessById(id)
        assertNotNull(persisted)
        assertEquals("POL-1001", persisted?.policyNumber)
        assertEquals(125050L, persisted?.premiumAmountPaise)
        assertEquals(BusinessStatus.ISSUED, persisted?.status)

        // Verify calculation in paise
        val summary = repository.getBusinessSummary().first()
        assertEquals(125050L, summary.totalIssuedPremiumPaise)
        assertEquals(1, summary.totalIssuedPolicies)
    }

    @Test
    fun updateRecord_modifiesPersistedDataAndRecalculatesTotals() = runTest {
        val initial = BusinessEntity(
            policyNumber = "POL-UPDATE",
            premiumAmountPaise = 100000L, // ₹1,000.00
            status = BusinessStatus.PENDING,
            businessDate = 1000L,
        )
        val id = repository.insertBusiness(initial).getOrThrow()

        // Check pending calculation
        var summary = repository.getBusinessSummary().first()
        assertEquals(100000L, summary.pendingPremiumPaise)
        assertEquals(1, summary.pendingPolicies)
        assertEquals(0L, summary.totalIssuedPremiumPaise)
        assertEquals(0, summary.totalIssuedPolicies)

        // Update to ISSUED with new premium ₹2,500.50 (250050 paise)
        val updated = initial.copy(
            id = id,
            premiumAmountPaise = 250050L,
            status = BusinessStatus.ISSUED,
            issueDate = 1500L,
        )
        val updateResult = repository.updateBusiness(updated)
        assertTrue(updateResult.isSuccess)

        // Verify persisted data changed exactly
        val persisted = repository.getBusinessById(id)
        assertNotNull(persisted)
        assertEquals(250050L, persisted?.premiumAmountPaise)
        assertEquals(BusinessStatus.ISSUED, persisted?.status)
        assertEquals(1500L, persisted?.issueDate)

        // Verify calculations automatically updated
        summary = repository.getBusinessSummary().first()
        assertEquals(250050L, summary.totalIssuedPremiumPaise)
        assertEquals(1, summary.totalIssuedPolicies)
        assertEquals(0L, summary.pendingPremiumPaise)
        assertEquals(0, summary.pendingPolicies)
    }

    @Test
    fun deleteRecord_removesFromPersistenceAndRecalculatesTotals() = runTest {
        val record1 = BusinessEntity(
            policyNumber = "POL-DEL-1",
            premiumAmountPaise = 300000L,
            status = BusinessStatus.ISSUED,
            businessDate = 1000L,
            issueDate = 1000L,
        )
        val record2 = BusinessEntity(
            policyNumber = "POL-DEL-2",
            premiumAmountPaise = 150000L,
            status = BusinessStatus.PENDING,
            businessDate = 2000L,
        )

        val id1 = repository.insertBusiness(record1).getOrThrow()
        val id2 = repository.insertBusiness(record2).getOrThrow()

        var summary = repository.getBusinessSummary().first()
        assertEquals(300000L, summary.totalIssuedPremiumPaise)
        assertEquals(150000L, summary.pendingPremiumPaise)
        assertEquals(2, summary.totalRecords)

        // Delete record1
        val deleteResult = repository.deleteBusinessById(id1)
        assertTrue(deleteResult.isSuccess)

        // Verify calendar events are unlinked
        assertEquals(id1, fakeDao.clearedCalendarBusinessId)

        // Verify record1 is gone
        assertNull(repository.getBusinessById(id1))
        assertNotNull(repository.getBusinessById(id2))

        // Verify totals recalculate automatically
        summary = repository.getBusinessSummary().first()
        assertEquals(0L, summary.totalIssuedPremiumPaise)
        assertEquals(0, summary.totalIssuedPolicies)
        assertEquals(150000L, summary.pendingPremiumPaise)
        assertEquals(1, summary.pendingPolicies)
        assertEquals(1, summary.totalRecords)
    }

    @Test
    fun deleteBusiness_entityOverload_transactionallyUnlinksCalendarEventsAndDeletes() = runTest {
        val record = BusinessEntity(
            policyNumber = "POL-DEL-ENTITY",
            premiumAmountPaise = 200000L,
            status = BusinessStatus.ISSUED,
            businessDate = 1000L,
            issueDate = 1000L,
        )
        val id = repository.insertBusiness(record).getOrThrow()
        val persisted = repository.getBusinessById(id)
        assertNotNull(persisted)

        val deleteResult = repository.deleteBusiness(persisted!!)
        assertTrue(deleteResult.isSuccess)

        // Verify calendar events were unlinked for this business id
        assertEquals(id, fakeDao.clearedCalendarBusinessId)
        // Verify business record is deleted
        assertNull(repository.getBusinessById(id))
    }

    @Test
    fun cancelledPolicies_excludedFromPremiumAndPolicyTotals() = runTest {
        val cancelledRecord = BusinessEntity(
            policyNumber = "POL-CANCEL",
            premiumAmountPaise = 900000L,
            status = BusinessStatus.CANCELLED,
            businessDate = 1000L,
        )
        repository.insertBusiness(cancelledRecord).getOrThrow()

        val summary = repository.getBusinessSummary().first()
        // Must not contribute to premium or active policy totals
        assertEquals(0L, summary.totalIssuedPremiumPaise)
        assertEquals(0, summary.totalIssuedPolicies)
        assertEquals(0L, summary.pendingPremiumPaise)
        assertEquals(0, summary.pendingPolicies)
        assertEquals(1, summary.cancelledPolicies)
        assertEquals(1, summary.totalRecords)
    }

    @Test
    fun validation_rulesEnforced() {
        // Blank policy
        val blankResult = repository.validateBusiness(" ", 10000L, BusinessStatus.PENDING, 1000L, null)
        assertFalse(blankResult.isSuccess)
        assertEquals("Policy / Reference ID cannot be empty.", blankResult.errorMessage)

        // Non-positive premium
        val zeroPremium = repository.validateBusiness("POL-1", 0L, BusinessStatus.PENDING, 1000L, null)
        assertFalse(zeroPremium.isSuccess)

        val negPremium = repository.validateBusiness("POL-1", -100L, BusinessStatus.PENDING, 1000L, null)
        assertFalse(negPremium.isSuccess)

        // Issued without issue date
        val noIssueDate = repository.validateBusiness("POL-1", 10000L, BusinessStatus.ISSUED, 1000L, null)
        assertFalse(noIssueDate.isSuccess)

        // Issue date before business date
        val invalidDates = repository.validateBusiness("POL-1", 10000L, BusinessStatus.ISSUED, 1700086400000L, 1700000000000L)
        assertFalse(invalidDates.isSuccess)

        // Valid
        val valid = repository.validateBusiness("POL-1", 125050L, BusinessStatus.ISSUED, 1000L, 1000L)
        assertTrue(valid.isSuccess)
    }
}

class FakeBusinessDao : BusinessDao {
    private val records = mutableListOf<BusinessEntity>()
    private val recordsFlow = MutableStateFlow<List<BusinessEntity>>(emptyList())
    private var nextId = 1L

    private fun updateFlow() {
        recordsFlow.value = records.sortedWith(
            compareByDescending<BusinessEntity> { it.businessDate }.thenByDescending { it.id },
        )
    }

    override suspend fun insertBusiness(business: BusinessEntity): Long {
        val id = if (business.id > 0) business.id else nextId++
        val entity = business.copy(id = id)
        records.add(entity)
        updateFlow()
        return id
    }

    override suspend fun updateBusiness(business: BusinessEntity) {
        val index = records.indexOfFirst { it.id == business.id }
        if (index >= 0) {
            records[index] = business
            updateFlow()
        }
    }

    override suspend fun deleteBusiness(business: BusinessEntity) {
        records.removeAll { it.id == business.id }
        updateFlow()
    }

    override suspend fun deleteBusinessById(id: Long) {
        records.removeAll { it.id == id }
        updateFlow()
    }

    override suspend fun getBusinessById(id: Long): BusinessEntity? {
        return records.find { it.id == id }
    }

    override fun observeBusinessById(id: Long): Flow<BusinessEntity?> {
        return MutableStateFlow(records.find { it.id == id })
    }

    override fun getAllBusinesses(): Flow<List<BusinessEntity>> {
        return recordsFlow.asStateFlow()
    }

    override fun getAllBusinessesAsc(): Flow<List<BusinessEntity>> {
        return MutableStateFlow(
            records.sortedWith(compareBy<BusinessEntity> { it.businessDate }.thenBy { it.id }),
        )
    }

    override suspend fun getRecordCount(): Int {
        return records.size
    }

    override suspend fun clearSourceIdFromBusinesses(sourceId: Long) {
        records.replaceAll { record ->
            if (record.sourceId == sourceId) record.copy(sourceId = null) else record
        }
        updateFlow()
    }

    var clearedCalendarBusinessId: Long? = null
        private set

    override suspend fun clearBusinessRecordIdFromCalendarEvents(businessId: Long) {
        clearedCalendarBusinessId = businessId
    }
}
