package com.dravid.businesstracker.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dravid.businesstracker.data.calculation.BusinessCalculator
import com.dravid.businesstracker.data.model.BusinessReport
import com.dravid.businesstracker.data.model.ReportPeriodFilter
import com.dravid.businesstracker.data.repository.BusinessRepository
import com.dravid.businesstracker.data.repository.SourceRepository
import com.dravid.businesstracker.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class ReportsUiState(
    val selectedFilter: ReportPeriodFilter = ReportPeriodFilter.ALL_TIME,
    val startDateMillis: Long? = null,
    val endDateMillis: Long? = null,
    val report: BusinessReport = BusinessReport(),
    val isCustomDatePickerOpen: Boolean = false,
    val customStartCandidate: Long = DateUtils.todayMillis(),
    val customEndCandidate: Long = DateUtils.todayMillis(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

class ReportsViewModel(
    private val businessRepository: BusinessRepository,
    private val sourceRepository: SourceRepository,
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(ReportPeriodFilter.ALL_TIME)
    private val _customRange = MutableStateFlow<Pair<Long, Long>?>(null)

    private val _uiState = MutableStateFlow(ReportsUiState(isLoading = true))
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        observeReportsData()
    }

    private fun observeReportsData() {
        viewModelScope.launch {
            combine(
                businessRepository.getAllBusinesses(),
                sourceRepository.getAllSources(),
                _selectedFilter,
                _customRange,
            ) { businesses, sources, filter, customRange ->
                val totalDbRecords = businesses.size
                val (startMillis, endMillis) = resolveDateBoundaries(filter, customRange)
                val filteredRecords = BusinessCalculator.filterByDateRange(
                    records = businesses,
                    startDateMillis = startMillis,
                    endDateMillis = endMillis,
                )
                val report = BusinessCalculator.generateReport(
                    filteredRecords = filteredRecords,
                    allSources = sources,
                    totalDatabaseRecordCount = totalDbRecords,
                )

                filter to (Pair(startMillis, endMillis) to report)
            }.collect { (filter, rangeAndReport) ->
                val (range, report) = rangeAndReport
                _uiState.update { current ->
                    current.copy(
                        selectedFilter = filter,
                        startDateMillis = range.first,
                        endDateMillis = range.second,
                        report = report,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            }
        }
    }

    fun setPeriodFilter(filter: ReportPeriodFilter) {
        if (filter == ReportPeriodFilter.CUSTOM) {
            val currentRange = _customRange.value
            _uiState.update { current ->
                current.copy(
                    customStartCandidate = currentRange?.first ?: DateUtils.todayMillis(),
                    customEndCandidate = currentRange?.second ?: DateUtils.todayMillis(),
                    isCustomDatePickerOpen = true,
                )
            }
        } else {
            _selectedFilter.value = filter
        }
    }

    fun openCustomRangeDialog() {
        val currentRange = _customRange.value
        _uiState.update { current ->
            current.copy(
                customStartCandidate = currentRange?.first ?: DateUtils.todayMillis(),
                customEndCandidate = currentRange?.second ?: DateUtils.todayMillis(),
                isCustomDatePickerOpen = true,
            )
        }
    }

    fun closeCustomRangeDialog() {
        _uiState.update { it.copy(isCustomDatePickerOpen = false) }
    }

    fun updateCustomStartCandidate(millis: Long) {
        _uiState.update { it.copy(customStartCandidate = millis) }
    }

    fun updateCustomEndCandidate(millis: Long) {
        _uiState.update { it.copy(customEndCandidate = millis) }
    }

    fun applyCustomDateRange(startMillis: Long, endMillis: Long) {
        val normalizedStart = minOf(startMillis, endMillis)
        val normalizedEnd = maxOf(startMillis, endMillis)
        _customRange.value = normalizedStart to normalizedEnd
        _selectedFilter.value = ReportPeriodFilter.CUSTOM
        _uiState.update {
            it.copy(
                isCustomDatePickerOpen = false,
                customStartCandidate = normalizedStart,
                customEndCandidate = normalizedEnd,
            )
        }
    }

    fun resetFilterToAllTime() {
        _customRange.value = null
        _selectedFilter.value = ReportPeriodFilter.ALL_TIME
    }

    private fun resolveDateBoundaries(
        filter: ReportPeriodFilter,
        customRange: Pair<Long, Long>?,
    ): Pair<Long?, Long?> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)

        return when (filter) {
            ReportPeriodFilter.ALL_TIME -> null to null
            ReportPeriodFilter.THIS_MONTH -> {
                val ym = YearMonth.from(today)
                val start = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val end = ym.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
                start to end
            }
            ReportPeriodFilter.LAST_MONTH -> {
                val ym = YearMonth.from(today).minusMonths(1)
                val start = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val end = ym.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
                start to end
            }
            ReportPeriodFilter.THIS_YEAR -> {
                val start = today.withDayOfYear(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val end = today.withDayOfYear(today.lengthOfYear()).atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
                start to end
            }
            ReportPeriodFilter.CUSTOM -> {
                if (customRange != null) {
                    val start = DateUtils.startOfDay(customRange.first)
                    val end = DateUtils.endOfDay(customRange.second)
                    start to end
                } else {
                    null to null
                }
            }
        }
    }
}
