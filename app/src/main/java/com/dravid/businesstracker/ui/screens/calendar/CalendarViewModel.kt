package com.dravid.businesstracker.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dravid.businesstracker.data.model.BusinessEntity
import com.dravid.businesstracker.data.model.CalendarEventEntity
import com.dravid.businesstracker.data.model.CalendarEventType
import com.dravid.businesstracker.data.model.CalendarEventWithPolicy
import com.dravid.businesstracker.data.repository.BusinessRepository
import com.dravid.businesstracker.data.repository.CalendarRepository
import com.dravid.businesstracker.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val allEvents: List<CalendarEventWithPolicy> = emptyList(),
    val eventsForSelectedDate: List<CalendarEventWithPolicy> = emptyList(),
    val upcomingEvents: List<CalendarEventWithPolicy> = emptyList(),
    val monthEventDates: Map<LocalDate, List<CalendarEventType>> = emptyMap(),
    val availableBusinesses: List<BusinessEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    // Add / Edit form dialog
    val isFormOpen: Boolean = false,
    val editingEventId: Long? = null,
    val formTitle: String = "",
    val formDescription: String = "",
    val formEventType: CalendarEventType = CalendarEventType.RENEWAL,
    val formEventDate: Long = DateUtils.todayMillis(),
    val formEventTime: String = "",
    val formBusinessRecordId: Long? = null,
    val formValidationMessage: String? = null,
    // Delete dialog
    val eventToDelete: CalendarEventWithPolicy? = null,
)

class CalendarViewModel(
    private val calendarRepository: CalendarRepository,
    private val businessRepository: BusinessRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState(isLoading = true))
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            calendarRepository.getAllEvents().collect { events ->
                _uiState.update { current ->
                    val selectedDayStart = DateUtils.toStartOfDayMillis(current.selectedDate)
                    val selectedEvents = events.filter {
                        DateUtils.startOfDay(it.event.eventDate) == selectedDayStart
                    }

                    val todayStart = DateUtils.startOfDay(DateUtils.todayMillis())
                    val upcoming = events.filter {
                        !it.event.isCompleted && it.event.eventDate >= todayStart
                    }.sortedWith(compareBy<CalendarEventWithPolicy> { it.event.eventDate }.thenBy { it.event.id })

                    // Map events by LocalDate for dot indicators on the month grid
                    val datesMap = events.groupBy(
                        keySelector = { DateUtils.toLocalDate(it.event.eventDate) },
                        valueTransform = { it.event.eventType },
                    )

                    current.copy(
                        allEvents = events,
                        eventsForSelectedDate = selectedEvents,
                        upcomingEvents = upcoming,
                        monthEventDates = datesMap,
                        isLoading = false,
                    )
                }
            }
        }

        viewModelScope.launch {
            businessRepository.getAllBusinesses().collect { businesses ->
                _uiState.update { it.copy(availableBusinesses = businesses) }
            }
        }
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { current ->
            val selectedDayStart = DateUtils.toStartOfDayMillis(date)
            val selectedEvents = current.allEvents.filter {
                DateUtils.startOfDay(it.event.eventDate) == selectedDayStart
            }
            current.copy(
                selectedDate = date,
                eventsForSelectedDate = selectedEvents,
            )
        }
    }

    fun previousMonth() {
        _uiState.update { it.copy(currentMonth = it.currentMonth.minusMonths(1)) }
    }

    fun nextMonth() {
        _uiState.update { it.copy(currentMonth = it.currentMonth.plusMonths(1)) }
    }

    fun goToToday() {
        val today = LocalDate.now()
        _uiState.update { current ->
            val todayStart = DateUtils.toStartOfDayMillis(today)
            current.copy(
                currentMonth = YearMonth.from(today),
                selectedDate = today,
                eventsForSelectedDate = current.allEvents.filter {
                    DateUtils.startOfDay(it.event.eventDate) == todayStart
                },
            )
        }
    }

    fun openAddForm(date: LocalDate? = null) {
        val targetDate = date ?: _uiState.value.selectedDate
        _uiState.update {
            it.copy(
                isFormOpen = true,
                editingEventId = null,
                formTitle = "",
                formDescription = "",
                formEventType = CalendarEventType.RENEWAL,
                formEventDate = DateUtils.toStartOfDayMillis(targetDate),
                formEventTime = "",
                formBusinessRecordId = null,
                formValidationMessage = null,
            )
        }
    }

    fun openEditForm(item: CalendarEventWithPolicy) {
        _uiState.update {
            it.copy(
                isFormOpen = true,
                editingEventId = item.event.id,
                formTitle = item.event.title,
                formDescription = item.event.description ?: "",
                formEventType = item.event.eventType,
                formEventDate = item.event.eventDate,
                formEventTime = item.event.eventTime ?: "",
                formBusinessRecordId = item.event.businessRecordId,
                formValidationMessage = null,
            )
        }
    }

    fun closeForm() {
        _uiState.update {
            it.copy(
                isFormOpen = false,
                formValidationMessage = null,
            )
        }
    }

    fun updateFormTitle(title: String) {
        _uiState.update { it.copy(formTitle = title, formValidationMessage = null) }
    }

    fun updateFormDescription(description: String) {
        _uiState.update { it.copy(formDescription = description, formValidationMessage = null) }
    }

    fun updateFormEventType(type: CalendarEventType) {
        _uiState.update { it.copy(formEventType = type, formValidationMessage = null) }
    }

    fun updateFormEventDate(dateMillis: Long) {
        _uiState.update { it.copy(formEventDate = dateMillis, formValidationMessage = null) }
    }

    fun updateFormEventTime(time: String) {
        _uiState.update { it.copy(formEventTime = time, formValidationMessage = null) }
    }

    fun updateFormBusinessRecordId(recordId: Long?) {
        _uiState.update { it.copy(formBusinessRecordId = recordId, formValidationMessage = null) }
    }

    fun saveEvent() {
        val current = _uiState.value
        val trimmedTitle = current.formTitle.trim()
        if (trimmedTitle.isEmpty()) {
            _uiState.update { it.copy(formValidationMessage = "Event title cannot be empty.") }
            return
        }

        if (current.formEventDate <= 0L) {
            _uiState.update { it.copy(formValidationMessage = "Valid event date is required.") }
            return
        }

        viewModelScope.launch {
            val result = if (current.editingEventId == null) {
                calendarRepository.insertEvent(
                    CalendarEventEntity(
                        title = trimmedTitle,
                        description = current.formDescription.trim().ifEmpty { null },
                        eventType = current.formEventType,
                        eventDate = DateUtils.startOfDay(current.formEventDate),
                        eventTime = current.formEventTime.trim().ifEmpty { null },
                        businessRecordId = current.formBusinessRecordId,
                        isCompleted = false,
                    ),
                )
            } else {
                calendarRepository.updateEvent(
                    CalendarEventEntity(
                        id = current.editingEventId,
                        title = trimmedTitle,
                        description = current.formDescription.trim().ifEmpty { null },
                        eventType = current.formEventType,
                        eventDate = DateUtils.startOfDay(current.formEventDate),
                        eventTime = current.formEventTime.trim().ifEmpty { null },
                        businessRecordId = current.formBusinessRecordId,
                        // Preserve completion status when editing metadata
                        isCompleted = current.allEvents.find { it.event.id == current.editingEventId }?.event?.isCompleted ?: false,
                    ),
                )
            }

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isFormOpen = false,
                        formValidationMessage = null,
                        successMessage = if (current.editingEventId == null) {
                            "Event scheduled successfully."
                        } else {
                            "Event updated successfully."
                        },
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        formValidationMessage = result.exceptionOrNull()?.message
                            ?: "Failed to save event.",
                    )
                }
            }
        }
    }

    fun toggleEventCompletion(item: CalendarEventWithPolicy) {
        viewModelScope.launch {
            val newStatus = !item.event.isCompleted
            val result = calendarRepository.setEventCompleted(item.event.id, newStatus)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        successMessage = if (newStatus) {
                            "Marked as completed: ${item.event.title}"
                        } else {
                            "Reopened: ${item.event.title}"
                        },
                    )
                }
            } else {
                _uiState.update {
                    it.copy(errorMessage = "Failed to update event status.")
                }
            }
        }
    }

    fun requestDelete(item: CalendarEventWithPolicy) {
        _uiState.update { it.copy(eventToDelete = item) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(eventToDelete = null) }
    }

    fun confirmDelete() {
        val item = _uiState.value.eventToDelete ?: return
        viewModelScope.launch {
            val result = calendarRepository.deleteEvent(item.event)
            _uiState.update { current ->
                current.copy(
                    eventToDelete = null,
                    successMessage = if (result.isSuccess) "Event deleted." else null,
                    errorMessage = if (result.isFailure) "Failed to delete event." else null,
                )
            }
        }
    }

    fun dismissMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
