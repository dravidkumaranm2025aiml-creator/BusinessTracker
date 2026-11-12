package com.dravid.businesstracker.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import com.dravid.businesstracker.data.model.BusinessStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

class BusinessDatabaseTest {

    private lateinit var converters: Converters

    @Before
    fun setup() {
        converters = Converters()
    }

    @Test
    fun converters_fromBusinessStatus_convertsToStringCorrectly() {
        assertEquals("ISSUED", converters.fromBusinessStatus(BusinessStatus.ISSUED))
        assertEquals("PENDING", converters.fromBusinessStatus(BusinessStatus.PENDING))
        assertEquals("CANCELLED", converters.fromBusinessStatus(BusinessStatus.CANCELLED))
        assertNull(converters.fromBusinessStatus(null))
    }

    @Test
    fun converters_toBusinessStatus_convertsFromStringCorrectly() {
        assertEquals(BusinessStatus.ISSUED, converters.toBusinessStatus("ISSUED"))
        assertEquals(BusinessStatus.PENDING, converters.toBusinessStatus("PENDING"))
        assertEquals(BusinessStatus.CANCELLED, converters.toBusinessStatus("CANCELLED"))
        assertNull(converters.toBusinessStatus(null))
    }

    @Test
    fun converters_toBusinessStatus_unknownString_throwsExceptionSafely() {
        // Unknown or corrupted database status values must throw an error rather than silently defaulting to PENDING
        assertThrows(IllegalArgumentException::class.java) {
            converters.toBusinessStatus("CORRUPTED_UNKNOWN_STATUS")
        }
    }

    @Test
    fun migration_2_to_3_executesExpectedDdlStatements() {
        val executedStatements = mutableListOf<String>()
        val dbProxy = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedStatements.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        BusinessDatabase.MIGRATION_2_3.migrate(dbProxy)

        assertEquals(2, BusinessDatabase.MIGRATION_2_3.startVersion)
        assertEquals(3, BusinessDatabase.MIGRATION_2_3.endVersion)
        assertEquals(2, executedStatements.size)
        assertTrue(executedStatements[0].contains("CREATE TABLE IF NOT EXISTS sources"))
        assertTrue(executedStatements[0].contains("name TEXT NOT NULL COLLATE NOCASE"))
        assertTrue(executedStatements[1].contains("CREATE UNIQUE INDEX IF NOT EXISTS index_sources_name ON sources (name)"))
    }

    @Test
    fun converters_fromCalendarEventType_convertsToStringCorrectly() {
        assertEquals("RENEWAL", converters.fromCalendarEventType(com.dravid.businesstracker.data.model.CalendarEventType.RENEWAL))
        assertEquals("FOLLOW_UP", converters.fromCalendarEventType(com.dravid.businesstracker.data.model.CalendarEventType.FOLLOW_UP))
        assertNull(converters.fromCalendarEventType(null))
    }

    @Test
    fun converters_toCalendarEventType_convertsFromStringCorrectly() {
        assertEquals(com.dravid.businesstracker.data.model.CalendarEventType.RENEWAL, converters.toCalendarEventType("RENEWAL"))
        assertEquals(com.dravid.businesstracker.data.model.CalendarEventType.FOLLOW_UP, converters.toCalendarEventType("FOLLOW_UP"))
        assertNull(converters.toCalendarEventType(null))
    }

    @Test
    fun converters_toCalendarEventType_unknownString_throwsExceptionSafely() {
        assertThrows(IllegalArgumentException::class.java) {
            converters.toCalendarEventType("INVALID_EVENT_TYPE")
        }
    }

    @Test
    fun migration_3_to_4_executesExpectedDdlStatements() {
        val executedStatements = mutableListOf<String>()
        val dbProxy = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedStatements.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        BusinessDatabase.MIGRATION_3_4.migrate(dbProxy)

        assertEquals(3, BusinessDatabase.MIGRATION_3_4.startVersion)
        assertEquals(4, BusinessDatabase.MIGRATION_3_4.endVersion)
        assertEquals(3, executedStatements.size)
        assertTrue(executedStatements[0].contains("CREATE TABLE IF NOT EXISTS `calendar_events`"))
        assertTrue(executedStatements[0].contains("`event_type` TEXT NOT NULL"))
        assertTrue(executedStatements[1].contains("CREATE INDEX IF NOT EXISTS `index_calendar_events_event_date`"))
        assertTrue(executedStatements[2].contains("CREATE INDEX IF NOT EXISTS `index_calendar_events_business_record_id`"))
    }
}
