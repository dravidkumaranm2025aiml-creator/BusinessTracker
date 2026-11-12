package com.dravid.businesstracker.data.repository

import com.dravid.businesstracker.data.local.BusinessDao
import com.dravid.businesstracker.data.local.CalendarEventDao
import com.dravid.businesstracker.data.model.CalendarDashboardSummary
import com.dravid.businesstracker.data.model.CalendarEventEntity
import com.dravid.businesstracker.data.model.CalendarEventType
import com.dravid.businesstracker.data.model.CalendarEventWithPolicy
import com.dravid.businesstracker.util.DateUtils
import com.dravid.businesstracker.util.ValidationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Concrete implementation of CalendarRepository combining CalendarEventDao and BusinessDao.
 */
class CalendarRepositoryImpl(
    private val calendarEventDao: CalendarEventDao,
    private val businessDao: BusinessDao,
) : CalendarRepository {

    override fun getAllEvents(): Flow<List<CalendarEventWithPolicy>> {
        return combine(
            calendarEventDao.getAllEvents(),
            businessDao.getAllBusinesses(),
        ) { events, businesses ->
            val businessMap = businesses.associateBy { it.id }
            events.map { event ->
                CalendarEventWithPolicy(
                    event = event,
                    policyNumber = event.businessRecordId?.let { businessMap[it]?.policyNumber },
                )
            }
        }
    }

    override fun getEventsForDate(dateMillis: Long): Flow<List<CalendarEventWithPolicy>> {
        val targetDayStart = DateUtils.startOfDay(dateMillis)
        return getAllEvents().map { list ->
            list.filter { DateUtils.startOfDay(it.event.eventDate) == targetDayStart }
        }
    }

    override fun getUpcomingEvents(): Flow<List<CalendarEventWithPolicy>> {
        return getAllEvents().map { list ->
            val todayStart = DateUtils.startOfDay(DateUtils.todayMillis())
            list.filter { !it.event.isCompleted && it.event.eventDate >= todayStart }
                .sortedWith(compareBy<CalendarEventWithPolicy> { it.event.eventDate }.thenBy { it.event.id })
        }
    }

    override fun getOverdueEvents(): Flow<List<CalendarEventWithPolicy>> {
        return getAllEvents().map { list ->
            val todayStart = DateUtils.startOfDay(DateUtils.todayMillis())
            list.filter { !it.event.isCompleted && it.event.eventDate < todayStart }
                .sortedWith(compareBy<CalendarEventWithPolicy> { it.event.eventDate }.thenBy { it.event.id })
        }
    }

    override fun getCalendarDashboardSummary(): Flow<CalendarDashboardSummary> {
        return getAllEvents().map { list ->
            val todayStart = DateUtils.startOfDay(DateUtils.todayMillis())
            val todayEvents = list.filter { DateUtils.startOfDay(it.event.eventDate) == todayStart }
            val upcomingRenewals = list.filter {
                !it.event.isCompleted &&
                    it.event.eventType == CalendarEventType.RENEWAL &&
                    it.event.eventDate >= todayStart
            }
            val upcomingFollowUps = list.filter {
                !it.event.isCompleted &&
                    it.event.eventType == CalendarEventType.FOLLOW_UP &&
                    it.event.eventDate >= todayStart
            }
            val overdueEvents = list.filter {
                !it.event.isCompleted && it.event.eventDate < todayStart
            }.sortedWith(compareBy<CalendarEventWithPolicy> { it.event.eventDate }.thenBy { it.event.id })

            CalendarDashboardSummary(
                todayEventsCount = todayEvents.size,
                upcomingRenewalsCount = upcomingRenewals.size,
                upcomingFollowUpsCount = upcomingFollowUps.size,
                overdueCount = overdueEvents.size,
                todayEvents = todayEvents,
                overdueEvents = overdueEvents,
            )
        }
    }

    override suspend fun getEventById(id: Long): CalendarEventEntity? {
        return calendarEventDao.getEventById(id)
    }

    override suspend fun insertEvent(event: CalendarEventEntity): Result<Long> {
        val validation = validateEvent(title = event.title, eventDate = event.eventDate)
        if (!validation.isSuccess) {
            return Result.failure(
                IllegalArgumentException(validation.errorMessage ?: "Invalid event details"),
            )
        }
        return try {
            val normalized = event.copy(
                title = event.title.trim(),
                description = event.description?.trim()?.ifEmpty { null },
                eventDate = DateUtils.startOfDay(event.eventDate),
                eventTime = event.eventTime?.trim()?.ifEmpty { null },
            )
            val id = calendarEventDao.insertEvent(normalized)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateEvent(event: CalendarEventEntity): Result<Unit> {
        if (event.id <= 0L) {
            return Result.failure(IllegalArgumentException("Cannot update event with invalid ID"))
        }
        val validation = validateEvent(title = event.title, eventDate = event.eventDate)
        if (!validation.isSuccess) {
            return Result.failure(
                IllegalArgumentException(validation.errorMessage ?: "Invalid event details"),
            )
        }
        return try {
            val normalized = event.copy(
                title = event.title.trim(),
                description = event.description?.trim()?.ifEmpty { null },
                eventDate = DateUtils.startOfDay(event.eventDate),
                eventTime = event.eventTime?.trim()?.ifEmpty { null },
            )
            calendarEventDao.updateEvent(normalized)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteEvent(event: CalendarEventEntity): Result<Unit> {
        return try {
            calendarEventDao.deleteEvent(event)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteEventById(id: Long): Result<Unit> {
        return try {
            calendarEventDao.deleteEventById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setEventCompleted(id: Long, isCompleted: Boolean): Result<Unit> {
        return try {
            calendarEventDao.updateEventCompletion(id, isCompleted)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun validateEvent(
        title: String,
        eventDate: Long,
    ): ValidationResult {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) {
            return ValidationResult.Error("Event title cannot be empty.")
        }
        if (eventDate <= 0L) {
            return ValidationResult.Error("Event date must be valid.")
        }
        return ValidationResult.Success
    }
}
