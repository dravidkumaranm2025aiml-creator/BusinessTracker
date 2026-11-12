package com.dravid.businesstracker.ui.screens.calendar

import com.dravid.businesstracker.data.model.BusinessEntity
import com.dravid.businesstracker.data.model.BusinessStatus
import com.dravid.businesstracker.data.model.BusinessSummary
import com.dravid.businesstracker.data.model.CalendarDashboardSummary
import com.dravid.businesstracker.data.model.CalendarEventEntity
import com.dravid.businesstracker.data.model.CalendarEventType
import com.dravid.businesstracker.data.model.CalendarEventWithPolicy
import com.dravid.businesstracker.data.repository.BusinessRepository
import com.dravid.businesstracker.data.repository.CalendarRepository
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeCalendarRepo: FakeCalendarRepoForViewModel
    private lateinit var fakeBusinessRepo: FakeBusinessRepoForViewModel
    private lateinit var viewModel: CalendarViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeCalendarRepo = FakeCalendarRepoForViewModel()
        fakeBusinessRepo = FakeBusinessRepoForViewModel()
        viewModel = CalendarViewModel(fakeCalendarRepo, fakeBusinessRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialLoad_populatesEventsAndCategorizesSelectedAndUpcoming() = runTest {
        val today = DateUtils.todayLocalDate()
        val tomorrow = today.plusDays(1)

        val eventToday = CalendarEventWithPolicy(
            event = CalendarEventEntity(
                id = 1L,
                title = "Today Renewal",
                eventType = CalendarEventType.RENEWAL,
                eventDate = DateUtils.toStartOfDayMillis(today),
            ),
            policyNumber = "POL-100",
        )
        val eventTomorrow = CalendarEventWithPolicy(
            event = CalendarEventEntity(
                id = 2L,
                title = "Tomorrow Follow-up",
                eventType = CalendarEventType.FOLLOW_UP,
                eventDate = DateUtils.toStartOfDayMillis(tomorrow),
            ),
            policyNumber = null,
        )

        fakeCalendarRepo.emitEvents(listOf(eventToday, eventTomorrow))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.allEvents.size)
        assertEquals(1, state.eventsForSelectedDate.size)
        assertEquals("Today Renewal", state.eventsForSelectedDate[0].event.title)
        assertEquals(2, state.upcomingEvents.size)
        assertTrue(state.monthEventDates.containsKey(today))
        assertTrue(state.monthEventDates.containsKey(tomorrow))
    }

    @Test
    fun monthNavigation_updatesCurrentMonthCorrectly() = runTest {
        advanceUntilIdle()
        val initialMonth = viewModel.uiState.value.currentMonth

        viewModel.nextMonth()
        assertEquals(initialMonth.plusMonths(1), viewModel.uiState.value.currentMonth)

        viewModel.previousMonth()
        assertEquals(initialMonth, viewModel.uiState.value.currentMonth)

        viewModel.previousMonth()
        assertEquals(initialMonth.minusMonths(1), viewModel.uiState.value.currentMonth)

        viewModel.goToToday()
        assertEquals(YearMonth.now(), viewModel.uiState.value.currentMonth)
        assertEquals(LocalDate.now(), viewModel.uiState.value.selectedDate)
    }

    @Test
    fun selectDate_filtersEventsForThatDate() = runTest {
        val today = DateUtils.todayLocalDate()
        val otherDate = today.plusDays(5)

        val eventOther = CalendarEventWithPolicy(
            event = CalendarEventEntity(
                id = 5L,
                title = "Future Inspection",
                eventType = CalendarEventType.FOLLOW_UP,
                eventDate = DateUtils.toStartOfDayMillis(otherDate),
            ),
            policyNumber = "POL-555",
        )

        fakeCalendarRepo.emitEvents(listOf(eventOther))
        advanceUntilIdle()

        // Today has 0 events
        assertEquals(0, viewModel.uiState.value.eventsForSelectedDate.size)

        // Select other date
        viewModel.selectDate(otherDate)
        assertEquals(otherDate, viewModel.uiState.value.selectedDate)
        assertEquals(1, viewModel.uiState.value.eventsForSelectedDate.size)
        assertEquals("Future Inspection", viewModel.uiState.value.eventsForSelectedDate[0].event.title)
    }

    @Test
    fun addEvent_validation_blankTitleShowsError() = runTest {
        viewModel.openAddForm()
        viewModel.updateFormTitle("   ")
        viewModel.saveEvent()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isFormOpen)
        assertEquals("Event title cannot be empty.", viewModel.uiState.value.formValidationMessage)
    }

    @Test
    fun addEvent_validForm_persistsEventAndClosesDialog() = runTest {
        viewModel.openAddForm()
        viewModel.updateFormTitle("Client Consultation")
        viewModel.updateFormDescription("Discuss term plan renewal")
        viewModel.updateFormEventType(CalendarEventType.RENEWAL)
        viewModel.updateFormEventTime("11:00 AM")

        viewModel.saveEvent()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isFormOpen)
        assertNull(viewModel.uiState.value.formValidationMessage)
        assertEquals("Event scheduled successfully.", viewModel.uiState.value.successMessage)
        assertEquals(1, fakeCalendarRepo.insertedEvents.size)
        assertEquals("Client Consultation", fakeCalendarRepo.insertedEvents[0].title)
        assertEquals("11:00 AM", fakeCalendarRepo.insertedEvents[0].eventTime)
    }

    @Test
    fun editEvent_prepopulatesFormAndUpdatesCorrectly() = runTest {
        val original = CalendarEventWithPolicy(
            event = CalendarEventEntity(
                id = 42L,
                title = "Original Meeting",
                description = "Notes",
                eventType = CalendarEventType.FOLLOW_UP,
                eventDate = DateUtils.todayMillis(),
                eventTime = "10:00 AM",
            ),
            policyNumber = null,
        )

        fakeCalendarRepo.emitEvents(listOf(original))
        advanceUntilIdle()

        viewModel.openEditForm(original)
        val formState = viewModel.uiState.value
        assertTrue(formState.isFormOpen)
        assertEquals(42L, formState.editingEventId)
        assertEquals("Original Meeting", formState.formTitle)
        assertEquals("10:00 AM", formState.formEventTime)

        viewModel.updateFormTitle("Updated Meeting Title")
        viewModel.saveEvent()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isFormOpen)
        assertEquals("Event updated successfully.", viewModel.uiState.value.successMessage)
        assertEquals(1, fakeCalendarRepo.updatedEvents.size)
        assertEquals("Updated Meeting Title", fakeCalendarRepo.updatedEvents[0].title)
    }

    @Test
    fun toggleCompletion_callsRepositoryAndShowsNotification() = runTest {
        val event = CalendarEventWithPolicy(
            event = CalendarEventEntity(
                id = 99L,
                title = "Sign Agreement",
                eventType = CalendarEventType.RENEWAL,
                eventDate = DateUtils.todayMillis(),
                isCompleted = false,
            ),
            policyNumber = null,
        )

        fakeCalendarRepo.emitEvents(listOf(event))
        advanceUntilIdle()

        viewModel.toggleEventCompletion(event)
        advanceUntilIdle()

        assertEquals(true, fakeCalendarRepo.completionMap[99L])
        assertEquals("Marked as completed: Sign Agreement", viewModel.uiState.value.successMessage)
    }

    @Test
    fun deleteEvent_workflowEnforcesConfirmationDialog() = runTest {
        val event = CalendarEventWithPolicy(
            event = CalendarEventEntity(
                id = 7L,
                title = "Cancelled Task",
                eventType = CalendarEventType.FOLLOW_UP,
                eventDate = DateUtils.todayMillis(),
            ),
            policyNumber = null,
        )

        viewModel.requestDelete(event)
        assertEquals(event, viewModel.uiState.value.eventToDelete)

        viewModel.cancelDelete()
        assertNull(viewModel.uiState.value.eventToDelete)

        viewModel.requestDelete(event)
        viewModel.confirmDelete()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.eventToDelete)
        assertEquals("Event deleted.", viewModel.uiState.value.successMessage)
        assertTrue(fakeCalendarRepo.deletedIds.contains(7L))
    }
}

class FakeCalendarRepoForViewModel : CalendarRepository {
    private val eventsFlow = MutableStateFlow<List<CalendarEventWithPolicy>>(emptyList())
    private val summaryFlow = MutableStateFlow(CalendarDashboardSummary())

    val insertedEvents = mutableListOf<CalendarEventEntity>()
    val updatedEvents = mutableListOf<CalendarEventEntity>()
    val deletedIds = mutableListOf<Long>()
    val completionMap = mutableMapOf<Long, Boolean>()

    fun emitEvents(events: List<CalendarEventWithPolicy>) {
        eventsFlow.value = events
    }

    override fun getAllEvents(): Flow<List<CalendarEventWithPolicy>> = eventsFlow.asStateFlow()
    override fun getEventsForDate(dateMillis: Long): Flow<List<CalendarEventWithPolicy>> = eventsFlow.asStateFlow()
    override fun getUpcomingEvents(): Flow<List<CalendarEventWithPolicy>> = eventsFlow.asStateFlow()
    override fun getOverdueEvents(): Flow<List<CalendarEventWithPolicy>> = eventsFlow.asStateFlow()
    override fun getCalendarDashboardSummary(): Flow<CalendarDashboardSummary> = summaryFlow.asStateFlow()

    override suspend fun getEventById(id: Long): CalendarEventEntity? {
        return eventsFlow.value.find { it.event.id == id }?.event
    }

    override suspend fun insertEvent(event: CalendarEventEntity): Result<Long> {
        insertedEvents.add(event)
        return Result.success(event.id.takeIf { it > 0 } ?: 100L)
    }

    override suspend fun updateEvent(event: CalendarEventEntity): Result<Unit> {
        updatedEvents.add(event)
        return Result.success(Unit)
    }

    override suspend fun deleteEvent(event: CalendarEventEntity): Result<Unit> {
        deletedIds.add(event.id)
        return Result.success(Unit)
    }

    override suspend fun deleteEventById(id: Long): Result<Unit> {
        deletedIds.add(id)
        return Result.success(Unit)
    }

    override suspend fun setEventCompleted(id: Long, isCompleted: Boolean): Result<Unit> {
        completionMap[id] = isCompleted
        return Result.success(Unit)
    }

    override fun validateEvent(title: String, eventDate: Long): ValidationResult {
        if (title.isBlank()) return ValidationResult.Error("Event title cannot be empty.")
        if (eventDate <= 0L) return ValidationResult.Error("Valid event date is required.")
        return ValidationResult.Success
    }
}

class FakeBusinessRepoForViewModel : BusinessRepository {
    private val businessesFlow = MutableStateFlow<List<BusinessEntity>>(emptyList())
    private val summaryFlow = MutableStateFlow(BusinessSummary())

    override fun getAllBusinesses(): Flow<List<BusinessEntity>> = businessesFlow.asStateFlow()
    override fun getBusinessSummary(): Flow<BusinessSummary> = summaryFlow.asStateFlow()
    override suspend fun getBusinessById(id: Long): BusinessEntity? = null
    override fun observeBusinessById(id: Long): Flow<BusinessEntity?> = MutableStateFlow(null)
    override suspend fun insertBusiness(business: BusinessEntity): Result<Long> = Result.success(1L)
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
