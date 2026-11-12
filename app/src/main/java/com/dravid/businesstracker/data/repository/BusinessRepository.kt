package com.dravid.businesstracker.data.repository

import com.dravid.businesstracker.data.model.BusinessEntity
import com.dravid.businesstracker.data.model.BusinessStatus
import com.dravid.businesstracker.data.model.BusinessSummary
import com.dravid.businesstracker.util.ValidationResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface defining business data access, validation, and calculations.
 */
interface BusinessRepository {

    /**
     * Observes all business records ordered by business date descending.
     */
    fun getAllBusinesses(): Flow<List<BusinessEntity>>

    /**
     * Observes real-time calculated business summary metrics in exact paise.
     */
    fun getBusinessSummary(): Flow<BusinessSummary>

    /**
     * Retrieves a single business record by ID.
     */
    suspend fun getBusinessById(id: Long): BusinessEntity?

    /**
     * Observes a single business record by ID.
     */
    fun observeBusinessById(id: Long): Flow<BusinessEntity?>

    /**
     * Inserts a business record after validation.
     */
    suspend fun insertBusiness(business: BusinessEntity): Result<Long>

    /**
     * Updates an existing business record after validation.
     */
    suspend fun updateBusiness(business: BusinessEntity): Result<Unit>

    /**
     * Deletes a business record.
     */
    suspend fun deleteBusiness(business: BusinessEntity): Result<Unit>

    /**
     * Deletes a business record by ID.
     */
    suspend fun deleteBusinessById(id: Long): Result<Unit>

    /**
     * Validates business fields before persistence.
     */
    fun validateBusiness(
        policyNumber: String,
        premiumAmountPaise: Long,
        status: BusinessStatus,
        businessDate: Long,
        issueDate: Long?,
    ): ValidationResult
}
