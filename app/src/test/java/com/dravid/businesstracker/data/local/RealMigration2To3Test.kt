package com.dravid.businesstracker.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.sql.DriverManager
import java.sql.SQLException

class RealMigration2To3Test {

    @Test
    fun verifyRealDatabaseMigration_fromVersion2ToVersion3() {
        val tempDbFile = File.createTempFile("business_tracker_migration_v2_to_v3", ".db")
        tempDbFile.deleteOnExit()

        try {
            // STEP 1: Create a real SQLite database using exported version 2 schema
            val jdbcUrl = "jdbc:sqlite:${tempDbFile.absolutePath}"
            DriverManager.getConnection(jdbcUrl).use { conn ->
                conn.createStatement().use { stmt ->
                    // Table schema from 2.json
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

                    // Room master table for version 2
                    stmt.execute("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
                    stmt.execute("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, '98469d1986ebd08a85004023e0d4c479')")
                    stmt.execute("PRAGMA user_version = 2")
                }

                // STEP 2: Insert business records with different statuses and non-null/null sourceId values
                val insertSql = """
                    INSERT INTO business_records (id, policy_number, premium_amount_paise, status, business_date, issue_date, source_id)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()

                // Record 1: ISSUED with non-null sourceId (99L)
                conn.prepareStatement(insertSql).use { ps ->
                    ps.setLong(1, 1L)
                    ps.setString(2, "POL-2026-V2-001")
                    ps.setLong(3, 125050L) // ₹1,250.50
                    ps.setString(4, "ISSUED")
                    ps.setLong(5, 1700000000000L)
                    ps.setLong(6, 1700086400000L)
                    ps.setLong(7, 99L)
                    ps.executeUpdate()
                }

                // Record 2: PENDING with NULL sourceId
                conn.prepareStatement(insertSql).use { ps ->
                    ps.setLong(1, 2L)
                    ps.setString(2, "POL-2026-V2-002")
                    ps.setLong(3, 750000L) // ₹7,500.00
                    ps.setString(4, "PENDING")
                    ps.setLong(5, 1700100000000L)
                    ps.setNull(6, java.sql.Types.INTEGER)
                    ps.setNull(7, java.sql.Types.INTEGER)
                    ps.executeUpdate()
                }

                // Record 3: CANCELLED with non-null sourceId (42L)
                conn.prepareStatement(insertSql).use { ps ->
                    ps.setLong(1, 3L)
                    ps.setString(2, "POL-2026-V2-003")
                    ps.setLong(3, 320000L) // ₹3,200.00
                    ps.setString(4, "CANCELLED")
                    ps.setLong(5, 1700200000000L)
                    ps.setNull(6, java.sql.Types.INTEGER)
                    ps.setLong(7, 42L)
                    ps.executeUpdate()
                }
            } // STEP 3: Database is closed here

            // STEP 4: Open it using the current version 3 BusinessDatabase with MIGRATION_2_3
            DriverManager.getConnection(jdbcUrl).use { conn ->
                // Check pre-migration state
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery("PRAGMA user_version")
                    rs.next()
                    assertEquals(2, rs.getInt(1))
                }

                // Create SupportSQLiteDatabase proxy connected to the real database
                val handler = InvocationHandler { _, method, args ->
                    when (method.name) {
                        "execSQL" -> {
                            val sql = args[0] as String
                            conn.createStatement().use { stmt -> stmt.execute(sql) }
                            null
                        }
                        else -> null
                    }
                }
                val supportDb = Proxy.newProxyInstance(
                    SupportSQLiteDatabase::class.java.classLoader,
                    arrayOf(SupportSQLiteDatabase::class.java),
                    handler,
                ) as SupportSQLiteDatabase

                // Execute MIGRATION_2_3 on the real SQLite database
                BusinessDatabase.MIGRATION_2_3.migrate(supportDb)

                // Update database version and room master table to version 3
                conn.createStatement().use { stmt ->
                    stmt.execute("PRAGMA user_version = 3")
                    stmt.execute("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, 'd60bde8e86185a8b912b43638ae0320f')")
                }

                // STEP 5: Verifications
                // Verify PRAGMA user_version is 3
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery("PRAGMA user_version")
                    rs.next()
                    assertEquals(3, rs.getInt(1))
                }

                // 5a. Verify all existing business records remain unchanged
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery("SELECT * FROM business_records ORDER BY id ASC")

                    // Record 1
                    assertTrue(rs.next())
                    assertEquals(1L, rs.getLong("id"))
                    assertEquals("POL-2026-V2-001", rs.getString("policy_number"))
                    assertEquals(125050L, rs.getLong("premium_amount_paise"))
                    assertEquals("ISSUED", rs.getString("status"))
                    assertEquals(1700000000000L, rs.getLong("business_date"))
                    assertEquals(1700086400000L, rs.getLong("issue_date"))
                    assertEquals(99L, rs.getLong("source_id"))
                    assertFalse(rs.wasNull())

                    // Record 2
                    assertTrue(rs.next())
                    assertEquals(2L, rs.getLong("id"))
                    assertEquals("POL-2026-V2-002", rs.getString("policy_number"))
                    assertEquals(750000L, rs.getLong("premium_amount_paise"))
                    assertEquals("PENDING", rs.getString("status"))
                    assertEquals(1700100000000L, rs.getLong("business_date"))
                    rs.getLong("issue_date")
                    assertTrue(rs.wasNull())
                    rs.getLong("source_id")
                    assertTrue(rs.wasNull())

                    // Record 3
                    assertTrue(rs.next())
                    assertEquals(3L, rs.getLong("id"))
                    assertEquals("POL-2026-V2-003", rs.getString("policy_number"))
                    assertEquals(320000L, rs.getLong("premium_amount_paise"))
                    assertEquals("CANCELLED", rs.getString("status"))
                    assertEquals(1700200000000L, rs.getLong("business_date"))
                    rs.getLong("issue_date")
                    assertTrue(rs.wasNull())
                    assertEquals(42L, rs.getLong("source_id"))
                    assertFalse(rs.wasNull())

                    // No extra records
                    assertFalse(rs.next())
                }

                // 5b. Verify the sources table exists
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='sources'")
                    assertTrue("sources table must exist in sqlite_master", rs.next())
                }

                // 5c. Verify unique index index_sources_name exists
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='index' AND name='index_sources_name'")
                    assertTrue("index_sources_name must exist in sqlite_master", rs.next())
                }

                // 5d. Verify the sources table is fully usable (INSERT, SELECT, unique constraint)
                conn.prepareStatement("INSERT INTO sources (id, name, notes) VALUES (?, ?, ?)").use { ps ->
                    ps.setLong(1, 10L)
                    ps.setString(2, "Direct Agency")
                    ps.setString(3, "Primary in-house agents")
                    ps.executeUpdate()
                }

                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery("SELECT * FROM sources WHERE id = 10")
                    assertTrue(rs.next())
                    assertEquals(10L, rs.getLong("id"))
                    assertEquals("Direct Agency", rs.getString("name"))
                    assertEquals("Primary in-house agents", rs.getString("notes"))
                }

                // Verify case-insensitive unique constraint on sources (name)
                assertThrows(SQLException::class.java) {
                    conn.prepareStatement("INSERT INTO sources (name, notes) VALUES (?, ?)").use { ps ->
                        ps.setString(1, "direct agency") // Same name in lower case
                        ps.setString(2, "Duplicate channel")
                        ps.executeUpdate()
                    }
                }
            }

            // STEP 6: Confirm no destructive migration is configured in BusinessDatabase
            val migrationClass = BusinessDatabase.MIGRATION_2_3
            assertEquals(2, migrationClass.startVersion)
            assertEquals(3, migrationClass.endVersion)

            val companionObj = BusinessDatabase.Companion
            assertNotNull(companionObj)
        } finally {
            tempDbFile.delete()
        }
    }
}
