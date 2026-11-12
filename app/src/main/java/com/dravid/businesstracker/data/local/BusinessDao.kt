package com.dravid.businesstracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.dravid.businesstracker.data.model.BusinessEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for business records.
 */
@Dao
interface BusinessDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBusiness(business: BusinessEntity): Long

    @Update
    suspend fun updateBusiness(business: BusinessEntity)

    @Delete
    suspend fun deleteBusiness(business: BusinessEntity)

    @Query("DELETE FROM business_records WHERE id = :id")
    suspend fun deleteBusinessById(id: Long)

    @Query("SELECT * FROM business_records WHERE id = :id")
    suspend fun getBusinessById(id: Long): BusinessEntity?

    @Query("SELECT * FROM business_records WHERE id = :id")
    fun observeBusinessById(id: Long): Flow<BusinessEntity?>

    @Query("SELECT * FROM business_records ORDER BY business_date DESC, id DESC")
    fun getAllBusinesses(): Flow<List<BusinessEntity>>

    @Query("SELECT * FROM business_records ORDER BY business_date ASC, id ASC")
    fun getAllBusinessesAsc(): Flow<List<BusinessEntity>>

    @Query("SELECT COUNT(*) FROM business_records")
    suspend fun getRecordCount(): Int

    @Query("UPDATE business_records SET source_id = NULL WHERE source_id = :sourceId")
    suspend fun clearSourceIdFromBusinesses(sourceId: Long)

    @Query("UPDATE calendar_events SET business_record_id = NULL WHERE business_record_id = :businessId")
    suspend fun clearBusinessRecordIdFromCalendarEvents(businessId: Long)

    @Transaction
    suspend fun deleteBusinessWithUnlinking(id: Long) {
        clearBusinessRecordIdFromCalendarEvents(id)
        deleteBusinessById(id)
    }
}
