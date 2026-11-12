package com.dravid.businesstracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dravid.businesstracker.data.model.CalendarEventEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for calendar and scheduling events.
 */
@Dao
interface CalendarEventDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEvent(event: CalendarEventEntity): Long

    @Update
    suspend fun updateEvent(event: CalendarEventEntity)

    @Delete
    suspend fun deleteEvent(event: CalendarEventEntity)

    @Query("DELETE FROM calendar_events WHERE id = :id")
    suspend fun deleteEventById(id: Long)

    @Query("SELECT * FROM calendar_events WHERE id = :id")
    suspend fun getEventById(id: Long): CalendarEventEntity?

    @Query("SELECT * FROM calendar_events WHERE id = :id")
    fun observeEventById(id: Long): Flow<CalendarEventEntity?>

    @Query("SELECT * FROM calendar_events ORDER BY event_date ASC, id ASC")
    fun getAllEvents(): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE event_date = :eventDate ORDER BY id ASC")
    fun getEventsForDate(eventDate: Long): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE event_date >= :startDate AND event_date <= :endDate ORDER BY event_date ASC, id ASC")
    fun getEventsForDateRange(startDate: Long, endDate: Long): Flow<List<CalendarEventEntity>>

    @Query("UPDATE calendar_events SET is_completed = :isCompleted WHERE id = :id")
    suspend fun updateEventCompletion(id: Long, isCompleted: Boolean)

    @Query("UPDATE calendar_events SET business_record_id = NULL WHERE business_record_id = :businessRecordId")
    suspend fun clearBusinessRecordId(businessRecordId: Long)

    @Query("SELECT COUNT(*) FROM calendar_events")
    suspend fun getEventCount(): Int
}
