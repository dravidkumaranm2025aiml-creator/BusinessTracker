package com.dravid.businesstracker.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.sql.DriverManager

class RealMigration3To4Test {

    @Test
    fun verifyRealDatabaseMigration_fromVersion3ToVersion4() {
        val tempDbFile = File.createTempFile("business_tracker_migration_v3_to_v4", ".db")
        tempDbFile.deleteOnExit()

        try {
            val jdbcUrl = "jdbc:sqlite:${tempDbFile.absolutePath}"

            // STEP 1: Create real SQLite database using exported version 3 schema (3.json)
            DriverManager.getConnection(jdbcUrl).use { conn ->
                conn.createStatement().use { stmt ->
                    // business_records table from 3.json
                    stmt.execute(
                        """
                        CREATE TABLE IF NOT EXISTS `business_records` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `policy_number` TEXT NOT NULL,
                            `premium_amount_paise` INTEGER NOT NULL,
                            `status` TEXT NOT NULL,
                            `business_date` INTEGER NOT NULL,
                            `issue_date` INTEGER,
                            `source_id` INTEGER
                        )
                        """.trimIndent(),
                    )

                    // sources table from 3.json
                    stmt.execute(
                        """
                        CREATE TABLE IF NOT EXISTS `sources` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL COLLATE NOCASE,
                            `notes` TEXT
                        )
                        """.trimIndent(),
                    )
                    stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS `index_sources_name` ON `sources` (`name`)")

                    // Room master table for version 3
                    stmt.execute("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
                    stmt.execute("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, 'd60bde8e86185a8b912b43638ae0320f')")
                    stmt.execute("PRAGMA user_version = 3")
                }

                // STEP 2: Insert initial sources
                val insertSourceSql = "INSERT INTO sources (id, name, notes) VALUES (?, ?, ?)"
                conn.prepareStatement(insertSourceSql).use { ps ->
                    ps.setLong(1, 10L)
                    ps.setString(2, "Direct Sales")
                    ps.setString(3, "In-house team")
                    ps.executeUpdate()
                }
                conn.prepareStatement(insertSourceSql).use { ps ->
                    ps.setLong(1, 20L)
                    ps.setString(2, "Broker Referral")
                    ps.setNull(3, java.sql.Types.VARCHAR)
                    ps.executeUpdate()
                }

                // STEP 3: Insert initial business records
                val insertBusinessSql = """
                    INSERT INTO business_records (id, policy_number, premium_amount_paise, status, business_date, issue_date, source_id)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()

                // Record 1: ISSUED with sourceId = 10
                conn.prepareStatement(insertBusinessSql).use { ps ->
                    ps.setLong(1, 101L)
                    ps.setString(2, "POL-V3-101")
                    ps.setLong(3, 250000L) // ₹2,500.00
                    ps.setString(4, "ISSUED")
                    ps.setLong(5, 1700000000000L)
                    ps.setLong(6, 1700086400000L)
                    ps.setLong(7, 10L)
                    ps.executeUpdate()
                }

                // Record 2: PENDING with sourceId = null
                conn.prepareStatement(insertBusinessSql).use { ps ->
                    ps.setLong(1, 102L)
                    ps.setString(2, "POL-V3-102")
                    ps.setLong(3, 980050L) // ₹9,800.50
                    ps.setString(4, "PENDING")
                    ps.setLong(5, 1700100000000L)
                    ps.setNull(6, java.sql.Types.INTEGER)
                    ps.setNull(7, java.sql.Types.INTEGER)
                    ps.executeUpdate()
                }

                // Record 3: CANCELLED with sourceId = 20
                conn.prepareStatement(insertBusinessSql).use { ps ->
                    ps.setLong(1, 103L)
                    ps.setString(2, "POL-V3-103")
                    ps.setLong(3, 150000L) // ₹1,500.00
                    ps.setString(4, "CANCELLED")
                    ps.setLong(5, 1700200000000L)
                    ps.setNull(6, java.sql.Types.INTEGER)
                    ps.setLong(7, 20L)
                    ps.executeUpdate()
                }
            } // Closed connection

            // STEP 4: Apply MIGRATION_3_4 using SupportSQLiteDatabase Proxy over real JDBC connection
            DriverManager.getConnection(jdbcUrl).use { conn ->
                // Verify pre-migration state is v3
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery("PRAGMA user_version")
                    rs.next()
                    assertEquals(3, rs.getInt(1))
                }

                val handler = InvocationHandler { _, method, args ->
                    if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                        val sql = args[0] as String
                        conn.createStatement().use { stmt ->
                            stmt.execute(sql)
                        }
                    }
                    null
                }

                val dbProxy = Proxy.newProxyInstance(
                    SupportSQLiteDatabase::class.java.classLoader,
                    arrayOf(SupportSQLiteDatabase::class.java),
                    handler,
                ) as SupportSQLiteDatabase

                // Execute MIGRATION_3_4
                BusinessDatabase.MIGRATION_3_4.migrate(dbProxy)

                // Update version pragma to 4
                conn.createStatement().use { stmt ->
                    stmt.execute("PRAGMA user_version = 4")
                }
            } // Closed connection

            // STEP 5: Verify migration results on re-opened database
            DriverManager.getConnection(jdbcUrl).use { conn ->
                // 1. Verify schema version is now 4
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery("PRAGMA user_version")
                    rs.next()
                    assertEquals(4, rs.getInt(1))
                }

                // 2. Verify all business records remain completely intact
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery("SELECT * FROM business_records ORDER BY id ASC")

                    // Record 1
                    assertTrue(rs.next())
                    assertEquals(101L, rs.getLong("id"))
                    assertEquals("POL-V3-101", rs.getString("policy_number"))
                    assertEquals(250000L, rs.getLong("premium_amount_paise"))
                    assertEquals("ISSUED", rs.getString("status"))
                    assertEquals(1700000000000L, rs.getLong("business_date"))
                    assertEquals(1700086400000L, rs.getLong("issue_date"))
                    assertEquals(10L, rs.getLong("source_id"))

                    // Record 2
                    assertTrue(rs.next())
                    assertEquals(102L, rs.getLong("id"))
                    assertEquals("POL-V3-102", rs.getString("policy_number"))
                    assertEquals(980050L, rs.getLong("premium_amount_paise"))
                    assertEquals("PENDING", rs.getString("status"))
                    assertEquals(1700100000000L, rs.getLong("business_date"))
                    assertEquals(0L, rs.getLong("issue_date"))
                    assertTrue(rs.wasNull())
                    assertEquals(0L, rs.getLong("source_id"))
                    assertTrue(rs.wasNull())

                    // Record 3
                    assertTrue(rs.next())
                    assertEquals(103L, rs.getLong("id"))
                    assertEquals("POL-V3-103", rs.getString("policy_number"))
                    assertEquals(150000L, rs.getLong("premium_amount_paise"))
                    assertEquals("CANCELLED", rs.getString("status"))
                    assertEquals(1700200000000L, rs.getLong("business_date"))
                    assertEquals(0L, rs.getLong("issue_date"))
                    assertTrue(rs.wasNull())
                    assertEquals(20L, rs.getLong("source_id"))

                    assertFalse(rs.next())
                }

                // 3. Verify sources table remains intact and usable
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery("SELECT * FROM sources ORDER BY id ASC")

                    assertTrue(rs.next())
                    assertEquals(10L, rs.getLong("id"))
                    assertEquals("Direct Sales", rs.getString("name"))
                    assertEquals("In-house team", rs.getString("notes"))

                    assertTrue(rs.next())
                    assertEquals(20L, rs.getLong("id"))
                    assertEquals("Broker Referral", rs.getString("name"))
                    assertNull(rs.getString("notes"))

                    assertFalse(rs.next())
                }

                // 4. Verify calendar_events table exists with required schema
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery("PRAGMA table_info(calendar_events)")
                    val columns = mutableMapOf<String, String>()
                    while (rs.next()) {
                        columns[rs.getString("name")] = rs.getString("type")
                    }

                    assertTrue(columns.containsKey("id"))
                    assertTrue(columns.containsKey("title"))
                    assertTrue(columns.containsKey("description"))
                    assertTrue(columns.containsKey("event_type"))
                    assertTrue(columns.containsKey("event_date"))
                    assertTrue(columns.containsKey("event_time"))
                    assertTrue(columns.containsKey("business_record_id"))
                    assertTrue(columns.containsKey("is_completed"))
                    assertTrue(columns.containsKey("created_at"))
                }

                // 5. Verify index creation on calendar_events
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery("PRAGMA index_list(calendar_events)")
                    val indices = mutableListOf<String>()
                    while (rs.next()) {
                        indices.add(rs.getString("name"))
                    }

                    assertTrue(indices.contains("index_calendar_events_event_date"))
                    assertTrue(indices.contains("index_calendar_events_business_record_id"))
                }

                // 6. Verify calendar_events CRUD operations and unlinking
                val insertEventSql = """
                    INSERT INTO calendar_events (title, description, event_type, event_date, event_time, business_record_id, is_completed, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()

                conn.prepareStatement(insertEventSql).use { ps ->
                    ps.setString(1, "Policy 101 Renewal")
                    ps.setString(2, "Follow-up for premium cheque")
                    ps.setString(3, "RENEWAL")
                    ps.setLong(4, 1710000000000L)
                    ps.setString(5, "11:00 AM")
                    ps.setLong(6, 101L) // Linked to POL-V3-101
                    ps.setInt(7, 0)
                    ps.setLong(8, System.currentTimeMillis())
                    ps.executeUpdate()
                }

                // Verify event retrieval
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery("SELECT * FROM calendar_events WHERE title = 'Policy 101 Renewal'")
                    assertTrue(rs.next())
                    assertEquals("RENEWAL", rs.getString("event_type"))
                    assertEquals(101L, rs.getLong("business_record_id"))
                    assertEquals(0, rs.getInt("is_completed"))
                }

                // Verify unlinking behavior: clear business_record_id when business record is removed
                conn.createStatement().use { stmt ->
                    stmt.executeUpdate("UPDATE calendar_events SET business_record_id = NULL WHERE business_record_id = 101")
                }

                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery("SELECT * FROM calendar_events WHERE title = 'Policy 101 Renewal'")
                    assertTrue(rs.next())
                    assertEquals(0L, rs.getLong("business_record_id"))
                    assertTrue(rs.wasNull()) // Safely unlinked, event retained!
                }
            }
        } finally {
            tempDbFile.delete()
        }
    }
}
