package com.dravid.businesstracker.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a scheduled calendar event (Policy Renewal or Follow-up).
 */
@Entity(
    tableName = "calendar_events",
    indices = [
        Index(value = ["event_date"]),
        Index(value = ["business_record_id"]),
    ],
)
data class CalendarEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "event_type")
    val eventType: CalendarEventType,

    @ColumnInfo(name = "event_date")
    val eventDate: Long,

    @ColumnInfo(name = "event_time")
    val eventTime: String? = null,

    @ColumnInfo(name = "business_record_id")
    val businessRecordId: Long? = null,

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)
