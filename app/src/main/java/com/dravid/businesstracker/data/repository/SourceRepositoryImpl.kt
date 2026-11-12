package com.dravid.businesstracker.data.repository

import com.dravid.businesstracker.data.calculation.BusinessCalculator
import com.dravid.businesstracker.data.local.BusinessDao
import com.dravid.businesstracker.data.local.SourceDao
import com.dravid.businesstracker.data.model.SourceEntity
import com.dravid.businesstracker.data.model.SourceWithAnalytics
import com.dravid.businesstracker.data.model.UnassignedSourceAnalytics
import com.dravid.businesstracker.util.ValidationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Concrete implementation of SourceRepository using SourceDao and BusinessDao.
 */
class SourceRepositoryImpl(
    private val sourceDao: SourceDao,
    private val businessDao: BusinessDao,
) : SourceRepository {

    override fun getAllSources(): Flow<List<SourceEntity>> {
        return sourceDao.getAllSources()
    }

    override fun getSourcesWithAnalytics(): Flow<List<SourceWithAnalytics>> {
        return combine(
            sourceDao.getAllSources(),
            businessDao.getAllBusinesses(),
        ) { sources, businesses ->
            val businessesBySource = businesses.groupBy { it.sourceId }
            sources.map { source ->
                val sourceBusinesses = businessesBySource[source.id] ?: emptyList()
                val summary = BusinessCalculator.calculateSummary(sourceBusinesses)
                SourceWithAnalytics(
                    source = source,
                    summary = summary,
                )
            }
        }
    }

    override fun getUnassignedAnalytics(): Flow<UnassignedSourceAnalytics> {
        return businessDao.getAllBusinesses().map { businesses ->
            val unassignedBusinesses = businesses.filter { it.sourceId == null }
            val summary = BusinessCalculator.calculateSummary(unassignedBusinesses)
            UnassignedSourceAnalytics(summary = summary)
        }
    }

    override suspend fun getSourceById(id: Long): SourceEntity? {
        return sourceDao.getSourceById(id)
    }

    override suspend fun insertSource(source: SourceEntity): Result<Long> {
        val trimmedName = source.name.trim()
        val validation = validateSource(name = trimmedName, currentId = null)
        if (!validation.isSuccess) {
            return Result.failure(
                IllegalArgumentException(validation.errorMessage ?: "Invalid source name"),
            )
        }
        return try {
            val id = sourceDao.insertSource(
                source.copy(
                    name = trimmedName,
                    notes = source.notes?.trim()?.ifEmpty { null },
                ),
            )
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateSource(source: SourceEntity): Result<Unit> {
        if (source.id <= 0L) {
            return Result.failure(IllegalArgumentException("Cannot update source with invalid ID"))
        }
        val trimmedName = source.name.trim()
        val validation = validateSource(name = trimmedName, currentId = source.id)
        if (!validation.isSuccess) {
            return Result.failure(
                IllegalArgumentException(validation.errorMessage ?: "Invalid source name"),
            )
        }
        return try {
            sourceDao.updateSource(
                source.copy(
                    name = trimmedName,
                    notes = source.notes?.trim()?.ifEmpty { null },
                ),
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteSource(sourceId: Long): Result<Unit> {
        return try {
            sourceDao.deleteSourceWithUnlinking(sourceId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun validateSource(name: String, currentId: Long?): ValidationResult {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return ValidationResult.Error("Source name cannot be empty.")
        }
        val existing = sourceDao.getSourceByName(trimmed)
        if (existing != null && (currentId == null || existing.id != currentId)) {
            return ValidationResult.Error("A source with this name already exists.")
        }
        return ValidationResult.Success
    }
}
