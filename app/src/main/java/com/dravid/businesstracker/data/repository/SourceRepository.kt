package com.dravid.businesstracker.data.repository

import com.dravid.businesstracker.data.model.SourceEntity
import com.dravid.businesstracker.data.model.SourceWithAnalytics
import com.dravid.businesstracker.data.model.UnassignedSourceAnalytics
import com.dravid.businesstracker.util.ValidationResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for lead/referral sources and attribution analytics.
 */
interface SourceRepository {

    /**
     * Observes all sources ordered alphabetically.
     */
    fun getAllSources(): Flow<List<SourceEntity>>

    /**
     * Observes sources paired with their real-time calculated business analytics.
     */
    fun getSourcesWithAnalytics(): Flow<List<SourceWithAnalytics>>

    /**
     * Observes real-time calculated business analytics for unassigned business records.
     */
    fun getUnassignedAnalytics(): Flow<UnassignedSourceAnalytics>

    /**
     * Retrieves a source by ID.
     */
    suspend fun getSourceById(id: Long): SourceEntity?

    /**
     * Inserts a new source after validating uniqueness and non-emptiness.
     */
    suspend fun insertSource(source: SourceEntity): Result<Long>

    /**
     * Updates an existing source after validation.
     */
    suspend fun updateSource(source: SourceEntity): Result<Unit>

    /**
     * Deletes a source by ID, resetting associated business records' source_id to null.
     */
    suspend fun deleteSource(sourceId: Long): Result<Unit>

    /**
     * Validates source name ensuring it is non-empty and unique (case-insensitive, trimmed).
     */
    suspend fun validateSource(name: String, currentId: Long?): ValidationResult
}
