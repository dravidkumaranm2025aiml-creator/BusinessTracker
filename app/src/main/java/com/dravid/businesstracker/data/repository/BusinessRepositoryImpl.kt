package com.dravid.businesstracker.data.repository

import com.dravid.businesstracker.data.calculation.BusinessCalculator
import com.dravid.businesstracker.data.local.BusinessDao
import com.dravid.businesstracker.data.model.BusinessEntity
import com.dravid.businesstracker.data.model.BusinessStatus
import com.dravid.businesstracker.data.model.BusinessSummary
import com.dravid.businesstracker.util.DateUtils
import com.dravid.businesstracker.util.ValidationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Concrete implementation of BusinessRepository using Room BusinessDao.
 */
class BusinessRepositoryImpl(
    private val businessDao: BusinessDao,
) : BusinessRepository {

    override fun getAllBusinesses(): Flow<List<BusinessEntity>> {
        return businessDao.getAllBusinesses()
    }

    override fun getBusinessSummary(): Flow<BusinessSummary> {
        return businessDao.getAllBusinesses().map { records ->
            BusinessCalculator.calculateSummary(records)
        }
    }

    override suspend fun getBusinessById(id: Long): BusinessEntity? {
        return businessDao.getBusinessById(id)
    }

    override fun observeBusinessById(id: Long): Flow<BusinessEntity?> {
        return businessDao.observeBusinessById(id)
    }

    override suspend fun insertBusiness(business: BusinessEntity): Result<Long> {
        val validation = validateBusiness(
            policyNumber = business.policyNumber,
            premiumAmountPaise = business.premiumAmountPaise,
            status = business.status,
            businessDate = business.businessDate,
            issueDate = business.issueDate,
        )
        if (!validation.isSuccess) {
            return Result.failure(
                IllegalArgumentException(validation.errorMessage ?: "Invalid business record"),
            )
        }
        return try {
            val id = businessDao.insertBusiness(business)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateBusiness(business: BusinessEntity): Result<Unit> {
        if (business.id <= 0) {
            return Result.failure(IllegalArgumentException("Cannot update record with invalid ID"))
        }
        val validation = validateBusiness(
            policyNumber = business.policyNumber,
            premiumAmountPaise = business.premiumAmountPaise,
            status = business.status,
            businessDate = business.businessDate,
            issueDate = business.issueDate,
        )
        if (!validation.isSuccess) {
            return Result.failure(
                IllegalArgumentException(validation.errorMessage ?: "Invalid business record"),
            )
        }
        return try {
            businessDao.updateBusiness(business)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteBusiness(business: BusinessEntity): Result<Unit> {
        return deleteBusinessById(business.id)
    }

    override suspend fun deleteBusinessById(id: Long): Result<Unit> {
        return try {
            businessDao.deleteBusinessWithUnlinking(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun validateBusiness(
        policyNumber: String,
        premiumAmountPaise: Long,
        status: BusinessStatus,
        businessDate: Long,
        issueDate: Long?,
    ): ValidationResult {
        val trimmedPolicy = policyNumber.trim()
        if (trimmedPolicy.isEmpty()) {
            return ValidationResult.Error("Policy / Reference ID cannot be empty.")
        }
        if (premiumAmountPaise <= 0L) {
            return ValidationResult.Error("Premium amount must be greater than zero.")
        }
        if (businessDate <= 0L) {
            return ValidationResult.Error("Business date must be valid.")
        }
        if (status == BusinessStatus.ISSUED && issueDate == null) {
            return ValidationResult.Error("Issue date is required when policy status is ISSUED.")
        }
        if (issueDate != null) {
            val businessDayStart = DateUtils.startOfDay(businessDate)
            val issueDayStart = DateUtils.startOfDay(issueDate)
            if (issueDayStart < businessDayStart) {
                return ValidationResult.Error("Issue date cannot be earlier than business date.")
            }
        }
        return ValidationResult.Success
    }
}
