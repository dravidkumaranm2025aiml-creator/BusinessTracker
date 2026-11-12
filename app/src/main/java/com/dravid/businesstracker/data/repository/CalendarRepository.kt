package com.dravid.businesstracker.data.repository

import com.dravid.businesstracker.data.model.CalendarDashboardSummary
import com.dravid.businesstracker.data.model.CalendarEventEntity
import com.dravid.businesstracker.data.model.CalendarEventWithPolicy
import com.dravid.businesstracker.util.ValidationResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface defining calendar event scheduling, retrieval, validation, and dashboard summary.
 */
interface CalendarRepository {

    /**
     * Observes all calendar events paired with their linked policy number (if any).
     */
    fun getAllEvents(): Flow<List<CalendarEventWithPolicy>>

    /**
     * Observes events scheduled for a specific date (day-normalized).
     */
    fun getEventsForDate(dateMillis: Long): Flow<List<CalendarEventWithPolicy>>

    /**
     * Observes upcoming events (incomplete events scheduled from today onwards, chronological).
     */
    fun getUpcomingEvents(): Flow<List<CalendarEventWithPolicy>>

    /**
     * Observes overdue events (incomplete events scheduled prior to today, chronological).
     */
    fun getOverdueEvents(): Flow<List<CalendarEventWithPolicy>>

    /**
     * Observes aggregated calendar summary metrics for the home dashboard.
     */
    fun getCalendarDashboardSummary(): Flow<CalendarDashboardSummary>

    /**
     * Retrieves a calendar event entity by ID.
     */
    suspend fun getEventById(id: Long): CalendarEventEntity?

    /**
     * Inserts a calendar event after validation.
     */
    suspend fun insertEvent(event: CalendarEventEntity): Result<Long>

    /**
     * Updates an existing calendar event after validation.
     */
    suspend fun updateEvent(event: CalendarEventEntity): Result<Unit>

    /**
     * Deletes a calendar event.
     */
    suspend fun deleteEvent(event: CalendarEventEntity): Result<Unit>

    /**
     * Deletes a calendar event by ID.
     */
    suspend fun deleteEventById(id: Long): Result<Unit>

    /**
     * Marks an event as completed or reopens it.
     */
    suspend fun setEventCompleted(id: Long, isCompleted: Boolean): Result<Unit>

    /**
     * Validates event fields before persistence.
     */
    fun validateEvent(
        title: String,
        eventDate: Long,
    ): ValidationResult
}
