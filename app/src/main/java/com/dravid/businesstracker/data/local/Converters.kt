package com.dravid.businesstracker.data.local

import androidx.room.TypeConverter
import com.dravid.businesstracker.data.model.BusinessStatus

/**
 * Room type converters for custom data types.
 * Rejects unknown or corrupted status values with an explicit exception
 * rather than silently converting corrupted data to PENDING.
 */
class Converters {

    @TypeConverter
    fun fromBusinessStatus(status: BusinessStatus?): String? {
        return status?.name
    }

    @TypeConverter
    fun toBusinessStatus(value: String?): BusinessStatus? {
        if (value == null) return null
        return try {
            BusinessStatus.valueOf(value)
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("Unknown or corrupted business status in database: '$value'")
        }
    }

    @TypeConverter
    fun fromCalendarEventType(type: com.dravid.businesstracker.data.model.CalendarEventType?): String? {
        return type?.name
    }

    @TypeConverter
    fun toCalendarEventType(value: String?): com.dravid.businesstracker.data.model.CalendarEventType? {
        if (value == null) return null
        return try {
            com.dravid.businesstracker.data.model.CalendarEventType.valueOf(value)
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("Unknown or corrupted calendar event type in database: '$value'")
        }
    }
}
