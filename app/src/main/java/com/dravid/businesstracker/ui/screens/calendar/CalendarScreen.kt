package com.dravid.businesstracker.ui.screens.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Schedule
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dravid.businesstracker.data.model.BusinessEntity
import com.dravid.businesstracker.data.model.CalendarEventType
import com.dravid.businesstracker.data.model.CalendarEventWithPolicy
import com.dravid.businesstracker.ui.AppViewModelProvider
import com.dravid.businesstracker.util.DateUtils
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = viewModel(factory = AppViewModelProvider.Factory),
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
                    contentDescription = "Schedule Event",
                )
            }
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 1. Monthly Calendar Card
                item(key = "calendar_month_card") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Month Header with Prev / Today / Next controls
                            MonthHeaderRow(
                                currentMonth = uiState.currentMonth,
                                onPrev = { viewModel.previousMonth() },
                                onNext = { viewModel.nextMonth() },
                                onToday = { viewModel.goToToday() },
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Day of week headers (Sun - Sat)
                            DaysOfWeekHeader()

                            Spacer(modifier = Modifier.height(6.dp))

                            // Month Days Grid
                            MonthCalendarGrid(
                                currentMonth = uiState.currentMonth,
                                selectedDate = uiState.selectedDate,
                                monthEventDates = uiState.monthEventDates,
                                onSelectDate = { viewModel.selectDate(it) },
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Legend for dots
                            CalendarLegendRow()
                        }
                    }
                }

                // 2. Selected Date Events Section
                item(key = "selected_date_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Events for ${DateUtils.formatDate(uiState.selectedDate)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${uiState.eventsForSelectedDate.size} scheduled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        OutlinedButton(
                            onClick = { viewModel.openAddForm(uiState.selectedDate) },
                            contentPadding = ButtonDefaults.TextButtonContentPadding,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add on Date", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                if (uiState.eventsForSelectedDate.isEmpty()) {
                    item(key = "empty_selected_date") {
                        EmptyDayEventsView(onAdd = { viewModel.openAddForm(uiState.selectedDate) })
                    }
                } else {
                    items(
                        items = uiState.eventsForSelectedDate,
                        key = { "selected_${it.event.id}" },
                    ) { eventItem ->
                        CalendarEventCard(
                            item = eventItem,
                            onToggleComplete = { viewModel.toggleEventCompletion(eventItem) },
                            onEdit = { viewModel.openEditForm(eventItem) },
                            onDelete = { viewModel.requestDelete(eventItem) },
                        )
                    }
                }

                // 3. Upcoming Events Section
                item(key = "upcoming_events_header") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Upcoming Schedules",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                if (uiState.upcomingEvents.isEmpty()) {
                    item(key = "empty_upcoming_events") {
                        EmptyUpcomingEventsView()
                    }
                } else {
                    items(
                        items = uiState.upcomingEvents,
                        key = { "upcoming_${it.event.id}" },
                    ) { eventItem ->
                        CalendarEventCard(
                            item = eventItem,
                            showDate = true,
                            onToggleComplete = { viewModel.toggleEventCompletion(eventItem) },
                            onEdit = { viewModel.openEditForm(eventItem) },
                            onDelete = { viewModel.requestDelete(eventItem) },
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (uiState.isFormOpen) {
        CalendarEventFormDialog(
            isEdit = uiState.editingEventId != null,
            title = uiState.formTitle,
            description = uiState.formDescription,
            eventType = uiState.formEventType,
            eventDate = uiState.formEventDate,
            eventTime = uiState.formEventTime,
            selectedBusinessRecordId = uiState.formBusinessRecordId,
            availableBusinesses = uiState.availableBusinesses,
            validationMessage = uiState.formValidationMessage,
            onTitleChange = { viewModel.updateFormTitle(it) },
            onDescriptionChange = { viewModel.updateFormDescription(it) },
            onEventTypeChange = { viewModel.updateFormEventType(it) },
            onEventDateChange = { viewModel.updateFormEventDate(it) },
            onEventTimeChange = { viewModel.updateFormEventTime(it) },
            onBusinessRecordChange = { viewModel.updateFormBusinessRecordId(it) },
            onSave = { viewModel.saveEvent() },
            onDismiss = { viewModel.closeForm() },
        )
    }

    // Delete Confirmation Dialog
    uiState.eventToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = {
                Text(
                    text = "Delete Event",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${item.event.title}'? This action cannot be undone.",
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
}

@Composable
private fun MonthHeaderRow(
    currentMonth: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            AnimatedContent(
                targetState = currentMonth,
                transitionSpec = {
                    if (targetState.isAfter(initialState)) {
                        (fadeIn(animationSpec = tween(200)) + slideInHorizontally(animationSpec = tween(200)) { it / 3 })
                            .togetherWith(fadeOut(animationSpec = tween(150)) + slideOutHorizontally(animationSpec = tween(150)) { -it / 3 })
                    } else {
                        (fadeIn(animationSpec = tween(200)) + slideInHorizontally(animationSpec = tween(200)) { -it / 3 })
                            .togetherWith(fadeOut(animationSpec = tween(150)) + slideOutHorizontally(animationSpec = tween(150)) { it / 3 })
                    }
                },
                label = "calendar_month_text_anim",
            ) { month ->
                Text(
                    text = DateUtils.formatYearMonth(month),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onToday) {
                Text("Today", style = MaterialTheme.typography.labelMedium)
            }
            IconButton(onClick = onPrev) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Month",
                )
            }
            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Month",
                )
            }
        }
    }
}

@Composable
private fun DaysOfWeekHeader() {
    val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    Row(modifier = Modifier.fillMaxWidth()) {
        days.forEach { dayName ->
            Text(
                text = dayName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MonthCalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    monthEventDates: Map<LocalDate, List<CalendarEventType>>,
    onSelectDate: (LocalDate) -> Unit,
) {
    val firstDayOfMonth = currentMonth.atDay(1)
    val daysInMonth = currentMonth.lengthOfMonth()

    // Sunday = 7 in DayOfWeek.SUNDAY, so mod 7 gives Sunday = 0, Monday = 1...
    val leadingEmptyDays = firstDayOfMonth.dayOfWeek.value % 7
    val totalCells = leadingEmptyDays + daysInMonth
    val rows = (totalCells + 6) / 7

    val today = LocalDate.now()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayOfMonth = cellIndex - leadingEmptyDays + 1

                    if (dayOfMonth in 1..daysInMonth) {
                        val cellDate = currentMonth.atDay(dayOfMonth)
                        val isSelected = cellDate == selectedDate
                        val isToday = cellDate == today
                        val eventTypes = monthEventDates[cellDate].orEmpty()

                        DayCell(
                            date = cellDate,
                            isSelected = isSelected,
                            isToday = isToday,
                            eventTypes = eventTypes,
                            onClick = { onSelectDate(cellDate) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        // Empty cell outside current month
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    eventTypes: List<CalendarEventType>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasRenewal = eventTypes.contains(CalendarEventType.RENEWAL)
    val hasFollowUp = eventTypes.contains(CalendarEventType.FOLLOW_UP)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                    isToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.surface
                },
            )
            .then(
                if (isToday && !isSelected) {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp),
                    )
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )

            // Event indicators: Dots for Renewal (emerald) and Follow-up (secondary)
            if (hasRenewal || hasFollowUp) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    if (hasRenewal) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                    if (hasFollowUp) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarLegendRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Renewal",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.width(16.dp))

        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Follow-up",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CalendarEventCard(
    item: CalendarEventWithPolicy,
    showDate: Boolean = false,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val event = item.event
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (event.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (event.isCompleted) 0.5.dp else 1.5.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Status Toggle, Title, Type Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    IconButton(
                        onClick = onToggleComplete,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = if (event.isCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = if (event.isCompleted) "Reopen" else "Complete",
                            tint = if (event.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (event.isCompleted) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            textDecoration = if (event.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        )

                        if (showDate) {
                            Text(
                                text = DateUtils.formatDate(event.eventDate),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }

                EventTypeBadge(type = event.eventType)
            }

            // Description if any
            if (!event.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 36.dp),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer Details: Linked Policy, Time, Edit/Delete
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 36.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.policyNumber != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Policy,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = item.policyNumber,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (!event.eventTime.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.outline,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = event.eventTime,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit Event",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete Event",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventTypeBadge(type: CalendarEventType) {
    val (label, containerColor, contentColor) = when (type) {
        CalendarEventType.RENEWAL -> Triple(
            "Renewal",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        CalendarEventType.FOLLOW_UP -> Triple(
            "Follow-up",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = containerColor,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarEventFormDialog(
    isEdit: Boolean,
    title: String,
    description: String,
    eventType: CalendarEventType,
    eventDate: Long,
    eventTime: String,
    selectedBusinessRecordId: Long?,
    availableBusinesses: List<BusinessEntity>,
    validationMessage: String?,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onEventTypeChange: (CalendarEventType) -> Unit,
    onEventDateChange: (Long) -> Unit,
    onEventTimeChange: (String) -> Unit,
    onBusinessRecordChange: (Long?) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var policyDropdownExpanded by remember { mutableStateOf(false) }

    val selectedPolicyNumber = availableBusinesses.firstOrNull { it.id == selectedBusinessRecordId }?.policyNumber
        ?: "None / Standalone Event"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "Edit Scheduled Event" else "Schedule New Event",
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

                // Event Title
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("Event Title *") },
                    placeholder = { Text("e.g. Policy Renewal Notice, Call Client") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Event Type Selector
                Text(
                    text = "Event Type *",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = eventType == CalendarEventType.RENEWAL,
                        onClick = { onEventTypeChange(CalendarEventType.RENEWAL) },
                        label = { Text("Policy Renewal") },
                        leadingIcon = {
                            if (eventType == CalendarEventType.RENEWAL) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )

                    FilterChip(
                        selected = eventType == CalendarEventType.FOLLOW_UP,
                        onClick = { onEventTypeChange(CalendarEventType.FOLLOW_UP) },
                        label = { Text("Follow-up") },
                        leadingIcon = {
                            if (eventType == CalendarEventType.FOLLOW_UP) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    )
                }

                // Event Date Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showDatePicker = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "Event Date *",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        Text(
                            text = DateUtils.formatDate(eventDate),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Filled.CalendarToday,
                            contentDescription = "Pick Event Date",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                // Optional Time
                OutlinedTextField(
                    value = eventTime,
                    onValueChange = onEventTimeChange,
                    label = { Text("Time (Optional)") },
                    placeholder = { Text("e.g. 10:30 AM") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Linked Policy / Reference Selector (Searchable Dropdown)
                ExposedDropdownMenuBox(
                    expanded = policyDropdownExpanded,
                    onExpandedChange = { policyDropdownExpanded = !policyDropdownExpanded },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = selectedPolicyNumber,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Linked Policy / Reference") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = policyDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = policyDropdownExpanded,
                        onDismissRequest = { policyDropdownExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("None / Standalone Event") },
                            onClick = {
                                onBusinessRecordChange(null)
                                policyDropdownExpanded = false
                            },
                        )
                        availableBusinesses.forEach { business ->
                            DropdownMenuItem(
                                text = { Text(business.policyNumber) },
                                onClick = {
                                    onBusinessRecordChange(business.id)
                                    policyDropdownExpanded = false
                                },
                            )
                        }
                    }
                }

                // Optional Description
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Enter additional notes or context") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
                Text(if (isEdit) "Update" else "Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )

    if (showDatePicker) {
        SingleDatePickerModal(
            initialDate = eventDate,
            onDateSelected = {
                onEventDateChange(it)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

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
private fun EmptyDayEventsView(onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Outlined.Event,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No events scheduled for this date",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onAdd) {
                Text("Schedule an Event")
            }
        }
    }
}

@Composable
private fun EmptyUpcomingEventsView() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "No upcoming policy renewals or client follow-ups.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun CalendarScreenPreview() {
    com.dravid.businesstracker.ui.theme.BusinessTrackerTheme {
        CalendarScreen()
    }
}
