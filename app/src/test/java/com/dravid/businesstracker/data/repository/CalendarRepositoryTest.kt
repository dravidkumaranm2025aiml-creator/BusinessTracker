package com.dravid.businesstracker.data.repository

import com.dravid.businesstracker.data.local.CalendarEventDao
import com.dravid.businesstracker.data.model.BusinessEntity
import com.dravid.businesstracker.data.model.BusinessStatus
import com.dravid.businesstracker.data.model.CalendarEventEntity
import com.dravid.businesstracker.data.model.CalendarEventType
import com.dravid.businesstracker.util.DateUtils
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

class CalendarRepositoryTest {

    private lateinit var fakeCalendarDao: FakeCalendarEventDao
    private lateinit var fakeBusinessDao: FakeBusinessDao
    private lateinit var repository: CalendarRepository

    @Before
    fun setup() {
        fakeCalendarDao = FakeCalendarEventDao()
        fakeBusinessDao = FakeBusinessDao()
        repository = CalendarRepositoryImpl(
            calendarEventDao = fakeCalendarDao,
            businessDao = fakeBusinessDao,
        )
    }

    @Test
    fun insertEvent_validData_persistsAndNormalizesDate() = runTest {
        val today = DateUtils.todayMillis()
        val event = CalendarEventEntity(
            title = "Renewal Notice for Pol 101",
            description = "Contact agent for renewal payment",
            eventType = CalendarEventType.RENEWAL,
            eventDate = today + 3600000L, // offset by 1 hour
            eventTime = "10:30 AM",
            businessRecordId = null,
        )

        val result = repository.insertEvent(event)
        assertTrue(result.isSuccess)
        val eventId = result.getOrThrow()
        assertTrue(eventId > 0)

        val retrieved = repository.getEventById(eventId)
        assertNotNull(retrieved)
        assertEquals("Renewal Notice for Pol 101", retrieved?.title)
        assertEquals("Contact agent for renewal payment", retrieved?.description)
        assertEquals(CalendarEventType.RENEWAL, retrieved?.eventType)
        assertEquals("10:30 AM", retrieved?.eventTime)
        assertEquals(DateUtils.startOfDay(today), retrieved?.eventDate) // Normalized to start of day
        assertFalse(retrieved?.isCompleted ?: true)
    }

    @Test
    fun insertEvent_blankTitle_failsValidation() = runTest {
        val invalidEvent = CalendarEventEntity(
            title = "   ",
            eventType = CalendarEventType.FOLLOW_UP,
            eventDate = DateUtils.todayMillis(),
        )

        val result = repository.insertEvent(invalidEvent)
        assertFalse(result.isSuccess)
        assertEquals("Event title cannot be empty.", result.exceptionOrNull()?.message)
    }

    @Test
    fun updateEvent_modifiesDataAndPreservesId() = runTest {
        val event = CalendarEventEntity(
            title = "Initial Follow-up",
            eventType = CalendarEventType.FOLLOW_UP,
            eventDate = DateUtils.todayMillis(),
        )
        val id = repository.insertEvent(event).getOrThrow()

        val updated = repository.getEventById(id)!!.copy(
            title = "Updated Client Call",
            description = "Rescheduled call",
            eventType = CalendarEventType.FOLLOW_UP,
            eventTime = "2:00 PM",
        )
        val updateResult = repository.updateEvent(updated)
        assertTrue(updateResult.isSuccess)

        val retrieved = repository.getEventById(id)
        assertNotNull(retrieved)
        assertEquals("Updated Client Call", retrieved?.title)
        assertEquals("Rescheduled call", retrieved?.description)
        assertEquals("2:00 PM", retrieved?.eventTime)
    }

    @Test
    fun deleteEvent_removesEventFromStorage() = runTest {
        val event = CalendarEventEntity(
            title = "To be deleted",
            eventType = CalendarEventType.RENEWAL,
            eventDate = DateUtils.todayMillis(),
        )
        val id = repository.insertEvent(event).getOrThrow()
        assertNotNull(repository.getEventById(id))

        val deleteResult = repository.deleteEventById(id)
        assertTrue(deleteResult.isSuccess)
        assertNull(repository.getEventById(id))
    }

    @Test
    fun setEventCompleted_togglesCompletionState() = runTest {
        val event = CalendarEventEntity(
            title = "Follow-up task",
            eventType = CalendarEventType.FOLLOW_UP,
            eventDate = DateUtils.todayMillis(),
        )
        val id = repository.insertEvent(event).getOrThrow()
        assertFalse(repository.getEventById(id)!!.isCompleted)

        // Mark completed
        repository.setEventCompleted(id, true)
        assertTrue(repository.getEventById(id)!!.isCompleted)

        // Reopen
        repository.setEventCompleted(id, false)
        assertFalse(repository.getEventById(id)!!.isCompleted)
    }

    @Test
    fun getAllEvents_combinesWithPolicyNumberFromBusiness() = runTest {
        val businessId = fakeBusinessDao.insertBusiness(
            BusinessEntity(
                policyNumber = "POL-9999",
                premiumAmountPaise = 500000L,
                status = BusinessStatus.ISSUED,
                businessDate = DateUtils.todayMillis(),
            ),
        )

        val eventWithPolicy = CalendarEventEntity(
            title = "Annual Renewal",
            eventType = CalendarEventType.RENEWAL,
            eventDate = DateUtils.todayMillis(),
            businessRecordId = businessId,
        )
        val standaloneEvent = CalendarEventEntity(
            title = "Direct Follow-up",
            eventType = CalendarEventType.FOLLOW_UP,
            eventDate = DateUtils.todayMillis(),
            businessRecordId = null,
        )

        repository.insertEvent(eventWithPolicy).getOrThrow()
        repository.insertEvent(standaloneEvent).getOrThrow()

        val allEventsWithPolicy = repository.getAllEvents().first()
        assertEquals(2, allEventsWithPolicy.size)

        val matchedEvent = allEventsWithPolicy.first { it.event.businessRecordId == businessId }
        assertEquals("POL-9999", matchedEvent.policyNumber)

        val unmatchedEvent = allEventsWithPolicy.first { it.event.businessRecordId == null }
        assertNull(unmatchedEvent.policyNumber)
    }

    @Test
    fun dashboardSummary_computesMetricsCorrectly() = runTest {
        val today = DateUtils.todayLocalDate()
        val yesterday = today.minusDays(1)
        val tomorrow = today.plusDays(1)
        val nextWeek = today.plusDays(7)

        // 1. Overdue incomplete follow-up (yesterday)
        repository.insertEvent(
            CalendarEventEntity(
                title = "Overdue Follow-up",
                eventType = CalendarEventType.FOLLOW_UP,
                eventDate = DateUtils.toStartOfDayMillis(yesterday),
                isCompleted = false,
            ),
        ).getOrThrow()

        // 2. Overdue completed renewal (yesterday) -> should NOT count as overdue
        repository.insertEvent(
            CalendarEventEntity(
                title = "Done Yesterday",
                eventType = CalendarEventType.RENEWAL,
                eventDate = DateUtils.toStartOfDayMillis(yesterday),
                isCompleted = true,
            ),
        ).getOrThrow()

        // 3. Today renewal (incomplete)
        repository.insertEvent(
            CalendarEventEntity(
                title = "Today Renewal",
                eventType = CalendarEventType.RENEWAL,
                eventDate = DateUtils.toStartOfDayMillis(today),
                isCompleted = false,
            ),
        ).getOrThrow()

        // 4. Today follow-up (completed)
        repository.insertEvent(
            CalendarEventEntity(
                title = "Today Follow-up Done",
                eventType = CalendarEventType.FOLLOW_UP,
                eventDate = DateUtils.toStartOfDayMillis(today),
                isCompleted = true,
            ),
        ).getOrThrow()

        // 5. Tomorrow upcoming renewal
        repository.insertEvent(
            CalendarEventEntity(
                title = "Tomorrow Renewal",
                eventType = CalendarEventType.RENEWAL,
                eventDate = DateUtils.toStartOfDayMillis(tomorrow),
                isCompleted = false,
            ),
        ).getOrThrow()

        // 6. Next week upcoming follow-up
        repository.insertEvent(
            CalendarEventEntity(
                title = "Next Week Follow-up",
                eventType = CalendarEventType.FOLLOW_UP,
                eventDate = DateUtils.toStartOfDayMillis(nextWeek),
                isCompleted = false,
            ),
        ).getOrThrow()

        val summary = repository.getCalendarDashboardSummary().first()

        assertEquals(2, summary.todayEventsCount) // Both incomplete and complete today
        assertEquals(2, summary.upcomingRenewalsCount) // Today incomplete renewal + Tomorrow incomplete renewal (>= today)
        assertEquals(1, summary.upcomingFollowUpsCount) // Next week incomplete follow-up
        assertEquals(1, summary.overdueCount) // 1 overdue incomplete
        assertEquals(2, summary.todayEvents.size)
    }

    @Test
    fun validateEvent_rulesEnforced() {
        assertFalse(repository.validateEvent("", DateUtils.todayMillis()).isSuccess)
        assertFalse(repository.validateEvent("   ", DateUtils.todayMillis()).isSuccess)
        assertFalse(repository.validateEvent("Valid Title", 0L).isSuccess)
        assertFalse(repository.validateEvent("Valid Title", -10L).isSuccess)
        assertTrue(repository.validateEvent("Valid Title", DateUtils.todayMillis()).isSuccess)
    }
}

class FakeCalendarEventDao : CalendarEventDao {
    private val events = mutableListOf<CalendarEventEntity>()
    private val eventsFlow = MutableStateFlow<List<CalendarEventEntity>>(emptyList())
    private var nextId = 1L

    private fun updateFlow() {
        eventsFlow.value = events.sortedWith(
            compareBy<CalendarEventEntity> { it.eventDate }.thenBy { it.id },
        )
    }

    override suspend fun insertEvent(event: CalendarEventEntity): Long {
        val id = if (event.id > 0) event.id else nextId++
        val entity = event.copy(id = id)
        events.add(entity)
        updateFlow()
        return id
    }

    override suspend fun updateEvent(event: CalendarEventEntity) {
        val index = events.indexOfFirst { it.id == event.id }
        if (index >= 0) {
            events[index] = event
            updateFlow()
        }
    }

    override suspend fun deleteEvent(event: CalendarEventEntity) {
        events.removeAll { it.id == event.id }
        updateFlow()
    }

    override suspend fun deleteEventById(id: Long) {
        events.removeAll { it.id == id }
        updateFlow()
    }

    override suspend fun getEventById(id: Long): CalendarEventEntity? {
        return events.find { it.id == id }
    }

    override fun observeEventById(id: Long): Flow<CalendarEventEntity?> {
        return MutableStateFlow(events.find { it.id == id })
    }

    override fun getAllEvents(): Flow<List<CalendarEventEntity>> {
        return eventsFlow.asStateFlow()
    }

    override fun getEventsForDate(eventDate: Long): Flow<List<CalendarEventEntity>> {
        val matching = events.filter { it.eventDate == eventDate }.sortedBy { it.id }
        return MutableStateFlow(matching)
    }

    override fun getEventsForDateRange(startDate: Long, endDate: Long): Flow<List<CalendarEventEntity>> {
        val matching = events.filter { it.eventDate in startDate..endDate }
            .sortedWith(compareBy<CalendarEventEntity> { it.eventDate }.thenBy { it.id })
        return MutableStateFlow(matching)
    }

    override suspend fun updateEventCompletion(id: Long, isCompleted: Boolean) {
        val index = events.indexOfFirst { it.id == id }
        if (index >= 0) {
            events[index] = events[index].copy(isCompleted = isCompleted)
            updateFlow()
        }
    }

    override suspend fun clearBusinessRecordId(businessRecordId: Long) {
        events.replaceAll { event ->
            if (event.businessRecordId == businessRecordId) event.copy(businessRecordId = null) else event
        }
        updateFlow()
    }

    override suspend fun getEventCount(): Int {
        return events.size
    }
}
