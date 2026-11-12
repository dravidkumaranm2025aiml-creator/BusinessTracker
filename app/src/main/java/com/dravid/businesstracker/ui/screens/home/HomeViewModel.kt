package com.dravid.businesstracker.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dravid.businesstracker.data.model.BusinessSummary
import com.dravid.businesstracker.data.model.CalendarDashboardSummary
import com.dravid.businesstracker.data.repository.BusinessRepository
import com.dravid.businesstracker.data.repository.CalendarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val summary: BusinessSummary = BusinessSummary(),
    val calendarSummary: CalendarDashboardSummary = CalendarDashboardSummary(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class HomeViewModel(
    private val businessRepository: BusinessRepository,
    private val calendarRepository: CalendarRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            businessRepository.getBusinessSummary().collect { summary ->
                _uiState.update { current ->
                    current.copy(
                        summary = summary,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            }
        }
        viewModelScope.launch {
            calendarRepository.getCalendarDashboardSummary().collect { calendarSummary ->
                _uiState.update { current ->
                    current.copy(calendarSummary = calendarSummary)
                }
            }
        }
    }
}
