package com.dravid.businesstracker.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dravid.businesstracker.BusinessTrackerApplication
import com.dravid.businesstracker.data.model.BusinessEntity
import com.dravid.businesstracker.data.model.BusinessStatus
import com.dravid.businesstracker.data.model.BusinessSummary
import com.dravid.businesstracker.data.model.CalendarDashboardSummary
import com.dravid.businesstracker.data.model.CalendarEventEntity
import com.dravid.businesstracker.data.model.CalendarEventWithPolicy
import com.dravid.businesstracker.data.model.SourceEntity
import com.dravid.businesstracker.data.model.SourceWithAnalytics
import com.dravid.businesstracker.data.model.UnassignedSourceAnalytics
import com.dravid.businesstracker.data.repository.BusinessRepository
import com.dravid.businesstracker.data.repository.CalendarRepository
import com.dravid.businesstracker.data.repository.SourceRepository
import com.dravid.businesstracker.ui.screens.business.BusinessViewModel
import com.dravid.businesstracker.ui.screens.calendar.CalendarViewModel
import com.dravid.businesstracker.ui.screens.home.HomeViewModel
import com.dravid.businesstracker.ui.screens.reports.ReportsViewModel
import com.dravid.businesstracker.ui.screens.sources.SourcesViewModel
import com.dravid.businesstracker.util.ValidationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Factory providing dependencies to ViewModels throughout the app.
 * Gracefully provides an in-memory preview repository when rendered inside Compose Previews.
 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            val app = businessTrackerApplication()
            val businessRepo = app?.businessRepository ?: PreviewBusinessRepository
            val sourceRepo = app?.sourceRepository ?: PreviewSourceRepository
            BusinessViewModel(businessRepo, sourceRepo)
        }
        initializer {
            val app = businessTrackerApplication()
            val businessRepo = app?.businessRepository ?: PreviewBusinessRepository
            val calendarRepo = app?.calendarRepository ?: PreviewCalendarRepository
            HomeViewModel(businessRepo, calendarRepo)
        }
        initializer {
            val sourceRepo = businessTrackerApplication()?.sourceRepository ?: PreviewSourceRepository
            SourcesViewModel(sourceRepo)
        }
        initializer {
            val app = businessTrackerApplication()
            val calendarRepo = app?.calendarRepository ?: PreviewCalendarRepository
            val businessRepo = app?.businessRepository ?: PreviewBusinessRepository
            CalendarViewModel(calendarRepo, businessRepo)
        }
        initializer {
            val app = businessTrackerApplication()
            val businessRepo = app?.businessRepository ?: PreviewBusinessRepository
            val sourceRepo = app?.sourceRepository ?: PreviewSourceRepository
            ReportsViewModel(businessRepo, sourceRepo)
        }
        initializer {
            val app = businessTrackerApplication()
            val themeRepo = app?.themeRepository ?: PreviewThemePreferencesRepository
            com.dravid.businesstracker.ui.screens.settings.SettingsViewModel(themeRepo)
        }
    }
}

fun CreationExtras.businessTrackerApplication(): BusinessTrackerApplication? =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as? BusinessTrackerApplication)

private object PreviewBusinessRepository : BusinessRepository {
    private val recordsFlow = MutableStateFlow<List<BusinessEntity>>(emptyList())
    private val summaryFlow = MutableStateFlow(BusinessSummary())

    override fun getAllBusinesses(): Flow<List<BusinessEntity>> = recordsFlow
    override fun getBusinessSummary(): Flow<BusinessSummary> = summaryFlow
    override suspend fun getBusinessById(id: Long): BusinessEntity? = null
    override fun observeBusinessById(id: Long): Flow<BusinessEntity?> = MutableStateFlow(null)
    override suspend fun insertBusiness(business: BusinessEntity): Result<Long> = Result.success(1L)
    override suspend fun updateBusiness(business: BusinessEntity): Result<Unit> = Result.success(Unit)
    override suspend fun deleteBusiness(business: BusinessEntity): Result<Unit> = Result.success(Unit)
    override suspend fun deleteBusinessById(id: Long): Result<Unit> = Result.success(Unit)
    override fun validateBusiness(
        policyNumber: String,
        premiumAmountPaise: Long,
        status: BusinessStatus,
        businessDate: Long,
        issueDate: Long?,
    ): ValidationResult = ValidationResult.Success
}

private object PreviewSourceRepository : SourceRepository {
    private val sourcesFlow = MutableStateFlow<List<SourceEntity>>(emptyList())
    private val analyticsFlow = MutableStateFlow<List<SourceWithAnalytics>>(emptyList())
    private val unassignedFlow = MutableStateFlow(UnassignedSourceAnalytics(summary = BusinessSummary()))

    override fun getAllSources(): Flow<List<SourceEntity>> = sourcesFlow
    override fun getSourcesWithAnalytics(): Flow<List<SourceWithAnalytics>> = analyticsFlow
    override fun getUnassignedAnalytics(): Flow<UnassignedSourceAnalytics> = unassignedFlow
    override suspend fun getSourceById(id: Long): SourceEntity? = null
    override suspend fun insertSource(source: SourceEntity): Result<Long> = Result.success(1L)
    override suspend fun updateSource(source: SourceEntity): Result<Unit> = Result.success(Unit)
    override suspend fun deleteSource(sourceId: Long): Result<Unit> = Result.success(Unit)
    override suspend fun validateSource(name: String, currentId: Long?): ValidationResult = ValidationResult.Success
}

private object PreviewCalendarRepository : CalendarRepository {
    private val eventsFlow = MutableStateFlow<List<CalendarEventWithPolicy>>(emptyList())
    private val summaryFlow = MutableStateFlow(CalendarDashboardSummary())

    override fun getAllEvents(): Flow<List<CalendarEventWithPolicy>> = eventsFlow
    override fun getEventsForDate(dateMillis: Long): Flow<List<CalendarEventWithPolicy>> = eventsFlow
    override fun getUpcomingEvents(): Flow<List<CalendarEventWithPolicy>> = eventsFlow
    override fun getOverdueEvents(): Flow<List<CalendarEventWithPolicy>> = eventsFlow
    override fun getCalendarDashboardSummary(): Flow<CalendarDashboardSummary> = summaryFlow
    override suspend fun getEventById(id: Long): CalendarEventEntity? = null
    override suspend fun insertEvent(event: CalendarEventEntity): Result<Long> = Result.success(1L)
    override suspend fun updateEvent(event: CalendarEventEntity): Result<Unit> = Result.success(Unit)
    override suspend fun deleteEvent(event: CalendarEventEntity): Result<Unit> = Result.success(Unit)
    override suspend fun deleteEventById(id: Long): Result<Unit> = Result.success(Unit)
    override suspend fun setEventCompleted(id: Long, isCompleted: Boolean): Result<Unit> = Result.success(Unit)
    override fun validateEvent(title: String, eventDate: Long): ValidationResult = ValidationResult.Success
}

private object PreviewThemePreferencesRepository : com.dravid.businesstracker.data.repository.ThemePreferencesRepository {
    private val modeFlow = MutableStateFlow(com.dravid.businesstracker.ui.theme.AppThemeMode.SYSTEM)
    override val themeMode: kotlinx.coroutines.flow.StateFlow<com.dravid.businesstracker.ui.theme.AppThemeMode> = modeFlow
    override suspend fun setThemeMode(mode: com.dravid.businesstracker.ui.theme.AppThemeMode) {
        modeFlow.value = mode
    }
}
