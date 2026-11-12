package com.dravid.businesstracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dravid.businesstracker.data.model.BusinessEntity
import com.dravid.businesstracker.data.model.CalendarEventEntity
import com.dravid.businesstracker.data.model.SourceEntity

/**
 * Main Room Database for the BusinessTracker application.
 * Version 4: Added calendar_events table and indices for policy renewal & follow-up scheduling.
 */
@Database(
    entities = [BusinessEntity::class, SourceEntity::class, CalendarEventEntity::class],
    version = BusinessDatabase.DATABASE_VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class BusinessDatabase : RoomDatabase() {

    abstract fun businessDao(): BusinessDao
    abstract fun sourceDao(): SourceDao
    abstract fun calendarEventDao(): CalendarEventDao

    companion object {
        const val DATABASE_NAME = "business_tracker_database"
        const val DATABASE_VERSION = 4

        /**
         * Migration from version 1 to 2:
         * Replaces floating-point premium_amount (REAL) with exact premium_amount_paise (INTEGER).
         * Converts existing records by rounding (premium_amount * 100) to paise.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS business_records_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        policy_number TEXT NOT NULL,
                        premium_amount_paise INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        business_date INTEGER NOT NULL,
                        issue_date INTEGER,
                        source_id INTEGER
                    )
                    """.trimIndent(),
                )

                db.execSQL(
                    """
                    INSERT INTO business_records_new (id, policy_number, premium_amount_paise, status, business_date, issue_date, source_id)
                    SELECT id, policy_number, CAST(ROUND(premium_amount * 100) AS INTEGER), status, business_date, issue_date, source_id
                    FROM business_records
                    """.trimIndent(),
                )

                db.execSQL("DROP TABLE business_records")
                db.execSQL("ALTER TABLE business_records_new RENAME TO business_records")
            }
        }

        /**
         * Migration from version 2 to 3:
         * Creates sources table and case-insensitive unique index on name.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sources (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL COLLATE NOCASE,
                        notes TEXT
                    )
                    """.trimIndent(),
                )

                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_sources_name ON sources (name)
                    """.trimIndent(),
                )
            }
        }

        /**
         * Migration from version 3 to 4:
         * Creates calendar_events table and indices on event_date and business_record_id.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `calendar_events` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT,
                        `event_type` TEXT NOT NULL,
                        `event_date` INTEGER NOT NULL,
                        `event_time` TEXT,
                        `business_record_id` INTEGER,
                        `is_completed` INTEGER NOT NULL,
                        `created_at` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_calendar_events_event_date` ON `calendar_events` (`event_date`)
                    """.trimIndent(),
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_calendar_events_business_record_id` ON `calendar_events` (`business_record_id`)
                    """.trimIndent(),
                )
            }
        }

        @Volatile
        private var INSTANCE: BusinessDatabase? = null

        fun getDatabase(context: Context): BusinessDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BusinessDatabase::class.java,
                    DATABASE_NAME,
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
