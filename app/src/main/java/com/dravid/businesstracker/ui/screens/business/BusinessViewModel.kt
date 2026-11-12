package com.dravid.businesstracker.ui.screens.business

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dravid.businesstracker.data.model.BusinessEntity
import com.dravid.businesstracker.data.model.BusinessStatus
import com.dravid.businesstracker.data.model.SourceEntity
import com.dravid.businesstracker.data.repository.BusinessRepository
import com.dravid.businesstracker.data.repository.SourceRepository
import com.dravid.businesstracker.util.DateUtils
import com.dravid.businesstracker.util.FormatUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST,
}

data class BusinessUiState(
    val records: List<BusinessEntity> = emptyList(),
    val filteredRecords: List<BusinessEntity> = emptyList(),
    val availableSources: List<SourceEntity> = emptyList(),
    val searchQuery: String = "",
    val statusFilter: BusinessStatus? = null,
    val startDateFilter: Long? = null,
    val endDateFilter: Long? = null,
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    // Add / Edit form dialog
    val isFormOpen: Boolean = false,
    val editingRecordId: Long? = null,
    val formPolicyNumber: String = "",
    val formPremiumAmount: String = "",
    val formStatus: BusinessStatus = BusinessStatus.PENDING,
    val formBusinessDate: Long = DateUtils.todayMillis(),
    val formIssueDate: Long? = null,
    val formSourceId: Long? = null,
    val formValidationMessage: String? = null,
    // Delete dialog
    val recordToDelete: BusinessEntity? = null,
    // Date filter dialog
    val isDateFilterOpen: Boolean = false,
)

class BusinessViewModel(
    private val repository: BusinessRepository,
    private val sourceRepository: SourceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusinessUiState(isLoading = true))
    val uiState: StateFlow<BusinessUiState> = _uiState.asStateFlow()

    init {
        loadRecords()
    }

    private fun loadRecords() {
        viewModelScope.launch {
            repository.getAllBusinesses().collect { records ->
                _uiState.update { current ->
                    current.copy(
                        records = records,
                        filteredRecords = applyFilters(
                            records = records,
                            query = current.searchQuery,
                            status = current.statusFilter,
                            startDate = current.startDateFilter,
                            endDate = current.endDateFilter,
                            sortOrder = current.sortOrder,
                        ),
                        isLoading = false,
                    )
                }
            }
        }
        viewModelScope.launch {
            sourceRepository.getAllSources().collect { sources ->
                _uiState.update { it.copy(availableSources = sources) }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { current ->
            current.copy(
                searchQuery = query,
                filteredRecords = applyFilters(
                    records = current.records,
                    query = query,
                    status = current.statusFilter,
                    startDate = current.startDateFilter,
                    endDate = current.endDateFilter,
                    sortOrder = current.sortOrder,
                ),
            )
        }
    }

    fun setStatusFilter(status: BusinessStatus?) {
        _uiState.update { current ->
            current.copy(
                statusFilter = status,
                filteredRecords = applyFilters(
                    records = current.records,
                    query = current.searchQuery,
                    status = status,
                    startDate = current.startDateFilter,
                    endDate = current.endDateFilter,
                    sortOrder = current.sortOrder,
                ),
            )
        }
    }

    fun setDateRangeFilter(start: Long?, end: Long?) {
        _uiState.update { current ->
            current.copy(
                startDateFilter = start,
                endDateFilter = end,
                isDateFilterOpen = false,
                filteredRecords = applyFilters(
                    records = current.records,
                    query = current.searchQuery,
                    status = current.statusFilter,
                    startDate = start,
                    endDate = end,
                    sortOrder = current.sortOrder,
                ),
            )
        }
    }

    fun clearDateFilter() {
        setDateRangeFilter(null, null)
    }

    fun openDateFilter() {
        _uiState.update { it.copy(isDateFilterOpen = true) }
    }

    fun closeDateFilter() {
        _uiState.update { it.copy(isDateFilterOpen = false) }
    }

    fun toggleSortOrder() {
        val newOrder = if (_uiState.value.sortOrder == SortOrder.NEWEST_FIRST) {
            SortOrder.OLDEST_FIRST
        } else {
            SortOrder.NEWEST_FIRST
        }
        setSortOrder(newOrder)
    }

    fun setSortOrder(order: SortOrder) {
        _uiState.update { current ->
            current.copy(
                sortOrder = order,
                filteredRecords = applyFilters(
                    records = current.records,
                    query = current.searchQuery,
                    status = current.statusFilter,
                    startDate = current.startDateFilter,
                    endDate = current.endDateFilter,
                    sortOrder = order,
                ),
            )
        }
    }

    fun openAddForm() {
        _uiState.update {
            it.copy(
                isFormOpen = true,
                editingRecordId = null,
                formPolicyNumber = "",
                formPremiumAmount = "",
                formStatus = BusinessStatus.PENDING,
                formBusinessDate = DateUtils.todayMillis(),
                formIssueDate = null,
                formSourceId = null,
                formValidationMessage = null,
            )
        }
    }

    fun openEditForm(record: BusinessEntity) {
        _uiState.update {
            it.copy(
                isFormOpen = true,
                editingRecordId = record.id,
                formPolicyNumber = record.policyNumber,
                formPremiumAmount = FormatUtils.formatPaiseToDecimalString(record.premiumAmountPaise),
                formStatus = record.status,
                formBusinessDate = record.businessDate,
                formIssueDate = record.issueDate,
                formSourceId = record.sourceId,
                formValidationMessage = null,
            )
        }
    }

    fun closeForm() {
        _uiState.update {
            it.copy(
                isFormOpen = false,
                formValidationMessage = null,
            )
        }
    }

    fun updateFormPolicyNumber(value: String) {
        _uiState.update { it.copy(formPolicyNumber = value, formValidationMessage = null) }
    }

    fun updateFormPremiumAmount(value: String) {
        _uiState.update { it.copy(formPremiumAmount = value, formValidationMessage = null) }
    }

    fun updateFormStatus(status: BusinessStatus) {
        _uiState.update { current ->
            val issueDate = when (status) {
                BusinessStatus.ISSUED -> current.formIssueDate ?: current.formBusinessDate
                else -> null
            }
            current.copy(
                formStatus = status,
                formIssueDate = issueDate,
                formValidationMessage = null,
            )
        }
    }

    fun updateFormBusinessDate(millis: Long) {
        _uiState.update { current ->
            val updatedIssueDate = if (current.formStatus == BusinessStatus.ISSUED &&
                current.formIssueDate != null &&
                current.formIssueDate < millis
            ) {
                millis
            } else {
                current.formIssueDate
            }
            current.copy(
                formBusinessDate = millis,
                formIssueDate = updatedIssueDate,
                formValidationMessage = null,
            )
        }
    }

    fun updateFormIssueDate(millis: Long?) {
        _uiState.update { it.copy(formIssueDate = millis, formValidationMessage = null) }
    }

    fun updateFormSourceId(sourceId: Long?) {
        _uiState.update { it.copy(formSourceId = sourceId, formValidationMessage = null) }
    }

    fun saveRecord() {
        val current = _uiState.value
        val trimmedPolicy = current.formPolicyNumber.trim()
        if (trimmedPolicy.isEmpty()) {
            _uiState.update { it.copy(formValidationMessage = "Policy / Reference ID cannot be empty.") }
            return
        }

        val premiumPaise = FormatUtils.parseRupeesToPaise(current.formPremiumAmount)
        if (premiumPaise == null || premiumPaise <= 0L) {
            _uiState.update {
                it.copy(formValidationMessage = "Premium amount must be a positive number greater than 0 (e.g. 1250.50).")
            }
            return
        }

        val businessDate = current.formBusinessDate
        val issueDate = if (current.formStatus == BusinessStatus.ISSUED) {
            current.formIssueDate ?: businessDate
        } else {
            null
        }

        val validation = repository.validateBusiness(
            policyNumber = trimmedPolicy,
            premiumAmountPaise = premiumPaise,
            status = current.formStatus,
            businessDate = businessDate,
            issueDate = issueDate,
        )

        if (!validation.isSuccess) {
            _uiState.update { it.copy(formValidationMessage = validation.errorMessage) }
            return
        }

        viewModelScope.launch {
            val result = if (current.editingRecordId == null) {
                repository.insertBusiness(
                    BusinessEntity(
                        policyNumber = trimmedPolicy,
                        premiumAmountPaise = premiumPaise,
                        status = current.formStatus,
                        businessDate = businessDate,
                        issueDate = issueDate,
                        sourceId = current.formSourceId,
                    ),
                )
            } else {
                repository.updateBusiness(
                    BusinessEntity(
                        id = current.editingRecordId,
                        policyNumber = trimmedPolicy,
                        premiumAmountPaise = premiumPaise,
                        status = current.formStatus,
                        businessDate = businessDate,
                        issueDate = issueDate,
                        sourceId = current.formSourceId,
                    ),
                )
            }

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isFormOpen = false,
                        formValidationMessage = null,
                        successMessage = if (current.editingRecordId == null) {
                            "Business record added successfully."
                        } else {
                            "Business record updated successfully."
                        },
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        formValidationMessage = result.exceptionOrNull()?.message
                            ?: "Failed to save business record.",
                    )
                }
            }
        }
    }

    fun requestDelete(record: BusinessEntity) {
        _uiState.update { it.copy(recordToDelete = record) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(recordToDelete = null) }
    }

    fun confirmDelete() {
        val record = _uiState.value.recordToDelete ?: return
        viewModelScope.launch {
            val result = repository.deleteBusiness(record)
            _uiState.update { current ->
                current.copy(
                    recordToDelete = null,
                    successMessage = if (result.isSuccess) "Record deleted." else null,
                    errorMessage = if (result.isFailure) "Failed to delete record." else null,
                )
            }
        }
    }

    fun dismissMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    private fun applyFilters(
        records: List<BusinessEntity>,
        query: String,
        status: BusinessStatus?,
        startDate: Long?,
        endDate: Long?,
        sortOrder: SortOrder,
    ): List<BusinessEntity> {
        var list = records

        if (query.isNotBlank()) {
            val q = query.trim()
            list = list.filter { it.policyNumber.contains(q, ignoreCase = true) }
        }

        if (status != null) {
            list = list.filter { it.status == status }
        }

        if (startDate != null) {
            val start = DateUtils.startOfDay(startDate)
            list = list.filter { it.businessDate >= start }
        }

        if (endDate != null) {
            val end = DateUtils.endOfDay(endDate)
            list = list.filter { it.businessDate <= end }
        }

        return when (sortOrder) {
            SortOrder.NEWEST_FIRST -> list.sortedWith(
                compareByDescending<BusinessEntity> { it.businessDate }.thenByDescending { it.id },
            )
            SortOrder.OLDEST_FIRST -> list.sortedWith(
                compareBy<BusinessEntity> { it.businessDate }.thenBy { it.id },
            )
        }
    }
}
