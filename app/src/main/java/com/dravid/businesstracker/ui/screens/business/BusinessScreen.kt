package com.dravid.businesstracker.ui.screens.business

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dravid.businesstracker.data.model.BusinessEntity
import com.dravid.businesstracker.data.model.BusinessStatus
import com.dravid.businesstracker.data.model.SourceEntity
import com.dravid.businesstracker.ui.AppViewModelProvider
import com.dravid.businesstracker.util.DateUtils
import com.dravid.businesstracker.util.FormatUtils

@Composable
fun BusinessScreen(
    modifier: Modifier = Modifier,
    viewModel: BusinessViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessages()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddForm() },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Business Record",
                )
            }
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                label = { Text("Search by Policy / Reference ID") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear Search",
                            )
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status Filter Chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = uiState.statusFilter == null,
                    onClick = { viewModel.setStatusFilter(null) },
                    label = { Text("All (${uiState.records.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )

                BusinessStatus.entries.forEach { status ->
                    val count = uiState.records.count { it.status == status }
                    FilterChip(
                        selected = uiState.statusFilter == status,
                        onClick = { viewModel.setStatusFilter(status) },
                        label = { Text("${status.name} ($count)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sort & Date Filter Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = { viewModel.toggleSortOrder() },
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Sort,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.sortOrder == SortOrder.NEWEST_FIRST) "Newest First" else "Oldest First",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val hasDateFilter = (uiState.startDateFilter != null) || (uiState.endDateFilter != null)
                    OutlinedButton(
                        onClick = { viewModel.openDateFilter() },
                        colors = if (hasDateFilter) {
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        } else {
                            ButtonDefaults.outlinedButtonColors()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FilterAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (hasDateFilter) "Dates Active" else "Date Filter",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }

                    if (hasDateFilter) {
                        IconButton(onClick = { viewModel.clearDateFilter() }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear Date Filter",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Content: Loading / Empty / List
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (uiState.records.isEmpty()) {
                EmptyRecordsView(
                    onAddClicked = { viewModel.openAddForm() },
                )
            } else if (uiState.filteredRecords.isEmpty()) {
                NoMatchingRecordsView(
                    onClearFilters = {
                        viewModel.setSearchQuery("")
                        viewModel.setStatusFilter(null)
                        viewModel.clearDateFilter()
                    },
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(
                        items = uiState.filteredRecords,
                        key = { it.id },
                    ) { record ->
                        BusinessRecordCard(
                            record = record,
                            availableSources = uiState.availableSources,
                            onEdit = { viewModel.openEditForm(record) },
                            onDelete = { viewModel.requestDelete(record) },
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Record Dialog
    if (uiState.isFormOpen) {
        BusinessFormDialog(
            isEdit = uiState.editingRecordId != null,
            policyNumber = uiState.formPolicyNumber,
            premiumAmount = uiState.formPremiumAmount,
            status = uiState.formStatus,
            businessDate = uiState.formBusinessDate,
            issueDate = uiState.formIssueDate,
            selectedSourceId = uiState.formSourceId,
            availableSources = uiState.availableSources,
            validationMessage = uiState.formValidationMessage,
            onPolicyNumberChange = { viewModel.updateFormPolicyNumber(it) },
            onPremiumAmountChange = { viewModel.updateFormPremiumAmount(it) },
            onStatusChange = { viewModel.updateFormStatus(it) },
            onBusinessDateChange = { viewModel.updateFormBusinessDate(it) },
            onIssueDateChange = { viewModel.updateFormIssueDate(it) },
            onSourceChange = { viewModel.updateFormSourceId(it) },
            onSave = { viewModel.saveRecord() },
            onDismiss = { viewModel.closeForm() },
        )
    }

    // Delete Confirmation Dialog
    uiState.recordToDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = {
                Text(
                    text = "Delete Business Record",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete policy '${record.policyNumber}'? This action cannot be undone and will automatically recalculate all business metrics.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDelete() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Date Range Filter Dialog
    if (uiState.isDateFilterOpen) {
        DateRangeFilterDialog(
            startDate = uiState.startDateFilter,
            endDate = uiState.endDateFilter,
            onApply = { start, end -> viewModel.setDateRangeFilter(start, end) },
            onDismiss = { viewModel.closeDateFilter() },
        )
    }
}

@Composable
private fun BusinessRecordCard(
    record: BusinessEntity,
    availableSources: List<SourceEntity>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val sourceName = availableSources.firstOrNull { it.id == record.sourceId }?.name

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = record.policyNumber,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (sourceName != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AccountTree,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = sourceName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = "Recorded: ${DateUtils.formatDate(record.businessDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                StatusBadge(status = record.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Premium",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Text(
                        text = FormatUtils.formatPaiseToCurrency(record.premiumAmountPaise),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = when (record.status) {
                            BusinessStatus.ISSUED -> MaterialTheme.colorScheme.primary
                            BusinessStatus.CANCELLED -> MaterialTheme.colorScheme.outline
                            BusinessStatus.PENDING -> MaterialTheme.colorScheme.secondary
                        },
                    )
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit Policy",
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete Policy",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            if (record.status == BusinessStatus.ISSUED && record.issueDate != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Event,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Issued on: ${DateUtils.formatDate(record.issueDate)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: BusinessStatus) {
    val (bgColor, textColor) = when (status) {
        BusinessStatus.ISSUED -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        BusinessStatus.PENDING -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        BusinessStatus.CANCELLED -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
    ) {
        Text(
            text = status.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun BusinessFormDialog(
    isEdit: Boolean,
    policyNumber: String,
    premiumAmount: String,
    status: BusinessStatus,
    businessDate: Long,
    issueDate: Long?,
    selectedSourceId: Long?,
    availableSources: List<SourceEntity>,
    validationMessage: String?,
    onPolicyNumberChange: (String) -> Unit,
    onPremiumAmountChange: (String) -> Unit,
    onStatusChange: (BusinessStatus) -> Unit,
    onBusinessDateChange: (Long) -> Unit,
    onIssueDateChange: (Long?) -> Unit,
    onSourceChange: (Long?) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showBusinessDatePicker by remember { mutableStateOf(value = false) }
    var showIssueDatePicker by remember { mutableStateOf(value = false) }
    var sourceDropdownExpanded by remember { mutableStateOf(value = false) }

    val selectedSourceName = availableSources.firstOrNull { it.id == selectedSourceId }?.name ?: "None / Unassigned"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "Edit Business Record" else "New Business Record",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (validationMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = validationMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }

                // Policy / Reference ID
                OutlinedTextField(
                    value = policyNumber,
                    onValueChange = onPolicyNumberChange,
                    label = { Text("Policy / Reference ID *") },
                    placeholder = { Text("e.g. POL-88231") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Premium Amount
                OutlinedTextField(
                    value = premiumAmount,
                    onValueChange = onPremiumAmountChange,
                    label = { Text("Premium Amount (₹) *") },
                    placeholder = { Text("1250.50") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Lead / Referral Source Dropdown
                ExposedDropdownMenuBox(
                    expanded = sourceDropdownExpanded,
                    onExpandedChange = { sourceDropdownExpanded = !sourceDropdownExpanded },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = selectedSourceName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Lead / Referral Source") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = sourceDropdownExpanded,
                        onDismissRequest = { sourceDropdownExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("None / Unassigned") },
                            onClick = {
                                onSourceChange(null)
                                sourceDropdownExpanded = false
                            },
                        )
                        availableSources.forEach { source ->
                            DropdownMenuItem(
                                text = { Text(source.name) },
                                onClick = {
                                    onSourceChange(source.id)
                                    sourceDropdownExpanded = false
                                },
                            )
                        }
                    }
                }

                // Status Selector
                Text(
                    text = "Policy Status *",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BusinessStatus.entries.forEach { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { onStatusChange(s) },
                            label = { Text(s.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }

                // Business Date
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showBusinessDatePicker = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "Business Date *",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        Text(
                            text = DateUtils.formatDate(businessDate),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    IconButton(onClick = { showBusinessDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Filled.CalendarToday,
                            contentDescription = "Pick Business Date",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                // Issue Date (only when status is ISSUED)
                if (status == BusinessStatus.ISSUED) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showIssueDatePicker = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Issue Date *",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                            Text(
                                text = issueDate?.let { DateUtils.formatDate(it) } ?: "Select Date",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        IconButton(onClick = { showIssueDatePicker = true }) {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = "Pick Issue Date",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
                Text(if (isEdit) "Update" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )

    if (showBusinessDatePicker) {
        SingleDatePickerModal(
            initialDate = businessDate,
            onDateSelected = {
                onBusinessDateChange(it)
                showBusinessDatePicker = false
            },
            onDismiss = { showBusinessDatePicker = false },
        )
    }

    if (showIssueDatePicker) {
        SingleDatePickerModal(
            initialDate = issueDate ?: businessDate,
            onDateSelected = {
                onIssueDateChange(it)
                showIssueDatePicker = false
            },
            onDismiss = { showIssueDatePicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleDatePickerModal(
    initialDate: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                },
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun DateRangeFilterDialog(
    startDate: Long?,
    endDate: Long?,
    onApply: (Long?, Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    var startText by remember { mutableStateOf(startDate?.let { DateUtils.formatDateIso(it) } ?: "") }
    var endText by remember { mutableStateOf(endDate?.let { DateUtils.formatDateIso(it) } ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Filter by Business Date",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                OutlinedTextField(
                    value = startText,
                    onValueChange = {
                        startText = it
                        errorMessage = null
                    },
                    label = { Text("From Date (YYYY-MM-DD)") },
                    placeholder = { Text("2026-01-01") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = endText,
                    onValueChange = {
                        endText = it
                        errorMessage = null
                    },
                    label = { Text("To Date (YYYY-MM-DD)") },
                    placeholder = { Text("2026-12-31") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = if (startText.isNotBlank()) DateUtils.parseIsoDate(startText) else null
                    val e = if (endText.isNotBlank()) DateUtils.parseIsoDate(endText) else null

                    if (startText.isNotBlank() && s == null) {
                        errorMessage = "Invalid start date format. Please use YYYY-MM-DD."
                        return@Button
                    }
                    if (endText.isNotBlank() && e == null) {
                        errorMessage = "Invalid end date format. Please use YYYY-MM-DD."
                        return@Button
                    }
                    if (s != null && e != null && s > e) {
                        errorMessage = "Start date cannot be after end date."
                        return@Button
                    }
                    onApply(s, e)
                },
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onApply(null, null)
                },
            ) {
                Text("Clear")
            }
        },
    )
}

@Composable
private fun EmptyRecordsView(
    onAddClicked: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.BusinessCenter,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Business Records",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Add insurance policies to track premiums, manage statuses, and view live performance analytics.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onAddClicked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Record First Business")
            }
        }
    }
}

@Composable
private fun NoMatchingRecordsView(
    onClearFilters: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.outline,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "No Matching Records",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "No records matched your search query or selected filters.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(onClick = onClearFilters) {
                Text("Reset Filters")
            }
        }
    }
}
