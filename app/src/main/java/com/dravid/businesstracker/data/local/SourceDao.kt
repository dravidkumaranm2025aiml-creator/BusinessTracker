package com.dravid.businesstracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.dravid.businesstracker.data.model.SourceEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for lead and referral sources.
 */
@Dao
interface SourceDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSource(source: SourceEntity): Long

    @Update
    suspend fun updateSource(source: SourceEntity)

    @Delete
    suspend fun deleteSource(source: SourceEntity)

    @Query("DELETE FROM sources WHERE id = :id")
    suspend fun deleteSourceById(id: Long)

    @Query("SELECT * FROM sources WHERE id = :id")
    suspend fun getSourceById(id: Long): SourceEntity?

    @Query("SELECT * FROM sources WHERE id = :id")
    fun observeSourceById(id: Long): Flow<SourceEntity?>

    @Query("SELECT * FROM sources WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun getSourceByName(name: String): SourceEntity?

    @Query("SELECT * FROM sources ORDER BY name COLLATE NOCASE ASC")
    fun getAllSources(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM sources ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllSourcesList(): List<SourceEntity>

    @Query("SELECT COUNT(*) FROM sources")
    suspend fun getSourceCount(): Int

    @Query("UPDATE business_records SET source_id = NULL WHERE source_id = :sourceId")
    suspend fun clearSourceIdFromBusinesses(sourceId: Long)

    @Transaction
    suspend fun deleteSourceWithUnlinking(id: Long) {
        clearSourceIdFromBusinesses(id)
        deleteSourceById(id)
    }
}
