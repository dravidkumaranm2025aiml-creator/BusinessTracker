package com.dravid.businesstracker.ui.screens.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dravid.businesstracker.data.model.BusinessReport
import com.dravid.businesstracker.data.model.BusinessStatus
import com.dravid.businesstracker.data.model.MonthlyPerformance
import com.dravid.businesstracker.data.model.ReportPeriodFilter
import com.dravid.businesstracker.data.model.SourcePerformance
import com.dravid.businesstracker.data.model.StatusBreakdownItem
import com.dravid.businesstracker.ui.AppViewModelProvider
import com.dravid.businesstracker.util.DateUtils
import com.dravid.businesstracker.util.FormatUtils

@Composable
fun ReportsScreen(
    modifier: Modifier = Modifier,
    viewModel: ReportsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ReportsScreenContent(
        uiState = uiState,
        onFilterSelected = { viewModel.setPeriodFilter(it) },
        onOpenCustomPicker = { viewModel.openCustomRangeDialog() },
        onResetToAllTime = { viewModel.resetFilterToAllTime() },
        onApplyCustomRange = { start, end -> viewModel.applyCustomDateRange(start, end) },
        onCloseCustomRange = { viewModel.closeCustomRangeDialog() },
        modifier = modifier,
    )
}

@Composable
fun ReportsScreenContent(
    uiState: ReportsUiState,
    onFilterSelected: (ReportPeriodFilter) -> Unit,
    onOpenCustomPicker: () -> Unit,
    onResetToAllTime: () -> Unit,
    onApplyCustomRange: (Long, Long) -> Unit,
    onCloseCustomRange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Screen Header
        ReportsHeaderSection(
            selectedFilter = uiState.selectedFilter,
            startDateMillis = uiState.startDateMillis,
            endDateMillis = uiState.endDateMillis,
            filteredCount = uiState.report.filteredRecordCount,
            totalCount = uiState.report.totalDatabaseRecordCount,
        )

        // Date Range Filters
        ReportFiltersRow(
            selectedFilter = uiState.selectedFilter,
            onFilterSelected = onFilterSelected,
            onOpenCustomPicker = onOpenCustomPicker,
        )

        when {
            uiState.report.totalDatabaseRecordCount == 0 -> {
                // Case 1: Empty database
                EmptyDatabaseReportsCard()
            }
            uiState.report.filteredRecordCount == 0 -> {
                // Case 2: Filtered range has no records
                EmptyFilteredReportsCard(
                    onResetToAllTime = onResetToAllTime,
                )
            }
            else -> {
                // Case 3: Display full report
                // 1. Executive Summary Metric Cards (6 KPI Cards)
                ExecutiveKpiSection(report = uiState.report)

                // 2. Status Breakdown
                StatusBreakdownSection(breakdown = uiState.report.statusBreakdown)

                // 3. Monthly Business Performance (with Native Compose Bar Chart)
                MonthlyPerformanceSection(monthlyPerformances = uiState.report.monthlyPerformances)

                // 4. Source-wise Performance Breakdown
                SourcePerformanceSection(
                    sourcePerformances = uiState.report.sourcePerformances,
                    unassignedPerformance = uiState.report.unassignedPerformance,
                )
            }
        }
    }

    if (uiState.isCustomDatePickerOpen) {
        CustomDateRangeDialog(
            initialStart = uiState.customStartCandidate,
            initialEnd = uiState.customEndCandidate,
            onConfirm = onApplyCustomRange,
            onDismiss = onCloseCustomRange,
        )
    }
}

@Composable
private fun ReportsHeaderSection(
    selectedFilter: ReportPeriodFilter,
    startDateMillis: Long?,
    endDateMillis: Long?,
    filteredCount: Int,
    totalCount: Int,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Business Reports & Analytics",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(4.dp))

        val periodDescription = when (selectedFilter) {
            ReportPeriodFilter.ALL_TIME -> "Showing all-time records ($filteredCount of $totalCount policies)"
            ReportPeriodFilter.THIS_MONTH -> {
                val startStr = startDateMillis?.let { DateUtils.formatDate(it) } ?: ""
                val endStr = endDateMillis?.let { DateUtils.formatDate(it) } ?: ""
                "This Month: $startStr – $endStr ($filteredCount records)"
            }
            ReportPeriodFilter.LAST_MONTH -> {
                val startStr = startDateMillis?.let { DateUtils.formatDate(it) } ?: ""
                val endStr = endDateMillis?.let { DateUtils.formatDate(it) } ?: ""
                "Last Month: $startStr – $endStr ($filteredCount records)"
            }
            ReportPeriodFilter.THIS_YEAR -> {
                val startStr = startDateMillis?.let { DateUtils.formatDate(it) } ?: ""
                val endStr = endDateMillis?.let { DateUtils.formatDate(it) } ?: ""
                "This Year: $startStr – $endStr ($filteredCount records)"
            }
            ReportPeriodFilter.CUSTOM -> {
                val startStr = startDateMillis?.let { DateUtils.formatDate(it) } ?: ""
                val endStr = endDateMillis?.let { DateUtils.formatDate(it) } ?: ""
                "Custom Period: $startStr – $endStr ($filteredCount records)"
            }
        }

        Text(
            text = periodDescription,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReportFiltersRow(
    selectedFilter: ReportPeriodFilter,
    onFilterSelected: (ReportPeriodFilter) -> Unit,
    onOpenCustomPicker: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReportPeriodFilter.entries.forEach { filter ->
            val isSelected = filter == selectedFilter
            FilterChip(
                selected = isSelected,
                onClick = {
                    if (filter == ReportPeriodFilter.CUSTOM) {
                        onOpenCustomPicker()
                    } else {
                        onFilterSelected(filter)
                    }
                },
                label = { Text(filter.displayName) },
                leadingIcon = {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    } else if (filter == ReportPeriodFilter.CUSTOM) {
                        Icon(
                            imageVector = Icons.Outlined.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    }
}

@Composable
private fun ExecutiveKpiSection(report: BusinessReport) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Key Performance Indicators",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        // Row 1: Total Issued Premium & Total Issued Policies
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ReportKpiCard(
                title = "Total Issued Premium",
                value = FormatUtils.formatPaiseToCurrency(report.summary.totalIssuedPremiumPaise),
                subtitle = "${report.summary.totalIssuedPolicies} active policies issued",
                icon = Icons.Outlined.Payments,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            ReportKpiCard(
                title = "Avg. Premium / Policy",
                value = FormatUtils.formatPaiseToCurrency(report.averageIssuedPremiumPaise),
                subtitle = "Per issued policy",
                icon = Icons.AutoMirrored.Outlined.TrendingUp,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        // Row 2: Pending Pipeline & Pending Count
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ReportKpiCard(
                title = "Total Pending Premium",
                value = FormatUtils.formatPaiseToCurrency(report.summary.pendingPremiumPaise),
                subtitle = "${report.summary.pendingPolicies} proposals pending",
                icon = Icons.Outlined.HourglassEmpty,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )

            ReportKpiCard(
                title = "Cancelled Policies",
                value = report.summary.cancelledPolicies.toString(),
                subtitle = "Excluded from revenue",
                icon = Icons.Outlined.Cancel,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReportKpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color,
    contentColor: Color,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StatusBreakdownSection(breakdown: List<StatusBreakdownItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.PieChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Policy Status Distribution",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            breakdown.forEach { item ->
                val (statusColor, statusLabel) = when (item.status) {
                    BusinessStatus.ISSUED -> MaterialTheme.colorScheme.primary to "Issued"
                    BusinessStatus.PENDING -> MaterialTheme.colorScheme.secondary to "Pending"
                    BusinessStatus.CANCELLED -> MaterialTheme.colorScheme.outline to "Cancelled"
                }

                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(statusColor),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$statusLabel (${item.policyCount})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        val premiumText = if (item.status == BusinessStatus.CANCELLED) {
                            "Non-revenue"
                        } else {
                            FormatUtils.formatPaiseToCurrency(item.premiumPaise)
                        }
                        Text(
                            text = "$premiumText • ${String.format(java.util.Locale.US, "%.1f", item.percentageOfPolicies)}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { (item.percentageOfPolicies / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = statusColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlyPerformanceSection(monthlyPerformances: List<MonthlyPerformance>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Monthly Business Trend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (monthlyPerformances.isEmpty()) {
                Text(
                    text = "No monthly records available for the selected period.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                // Visual Bar Chart showing recent months (up to 6 months chronological)
                val chartData = monthlyPerformances.take(6).reversed()
                val maxPremium = chartData.maxOfOrNull {
                    maxOf(it.summary.totalIssuedPremiumPaise, it.summary.pendingPremiumPaise)
                } ?: 1L
                val safeMax = if (maxPremium <= 0L) 1L else maxPremium

                // Chart Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Issued", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Pending", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bar columns
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    chartData.forEach { monthData ->
                        val issuedRatio = (monthData.summary.totalIssuedPremiumPaise.toFloat() / safeMax).coerceIn(0.04f, 1f)
                        val pendingRatio = (monthData.summary.pendingPremiumPaise.toFloat() / safeMax).coerceIn(0.04f, 1f)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                // Issued Bar
                                Box(
                                    modifier = Modifier
                                        .width(14.dp)
                                        .height((100 * issuedRatio).dp)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(MaterialTheme.colorScheme.primary),
                                )
                                // Pending Bar
                                Box(
                                    modifier = Modifier
                                        .width(14.dp)
                                        .height((100 * pendingRatio).dp)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(MaterialTheme.colorScheme.secondary),
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = DateUtils.formatShortMonthYear(monthData.yearMonth),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))

                // Monthly Table List
                monthlyPerformances.forEach { monthItem ->
                    MonthlyRowItem(monthData = monthItem)
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlyRowItem(monthData: MonthlyPerformance) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1.2f)) {
            Text(
                text = DateUtils.formatYearMonth(monthData.yearMonth),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${monthData.summary.totalRecords} total records",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(modifier = Modifier.weight(1.3f), horizontalAlignment = Alignment.End) {
            Text(
                text = FormatUtils.formatPaiseToCurrency(monthData.summary.totalIssuedPremiumPaise),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "${monthData.summary.totalIssuedPolicies} Issued",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Column(modifier = Modifier.weight(1.3f), horizontalAlignment = Alignment.End) {
            Text(
                text = FormatUtils.formatPaiseToCurrency(monthData.summary.pendingPremiumPaise),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = "${monthData.summary.pendingPolicies} Pending",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun SourcePerformanceSection(
    sourcePerformances: List<SourcePerformance>,
    unassignedPerformance: SourcePerformance,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Hub,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Source Performance & Attribution",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Combine all named sources + unassigned
            val allSourceItems = sourcePerformances + unassignedPerformance

            if (allSourceItems.all { it.summary.totalRecords == 0 }) {
                Text(
                    text = "No source attribution records found in this period.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                allSourceItems.forEach { item ->
                    SourcePerformanceRow(item = item)
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SourcePerformanceRow(item: SourcePerformance) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1.4f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.sourceName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.sourceId == null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = "Unassigned",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Text(
                text = "${item.summary.totalRecords} total (${item.summary.cancelledPolicies} cancelled)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(modifier = Modifier.weight(1.3f), horizontalAlignment = Alignment.End) {
            Text(
                text = FormatUtils.formatPaiseToCurrency(item.summary.totalIssuedPremiumPaise),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "${item.summary.totalIssuedPolicies} Issued",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Column(modifier = Modifier.weight(1.3f), horizontalAlignment = Alignment.End) {
            Text(
                text = FormatUtils.formatPaiseToCurrency(item.summary.pendingPremiumPaise),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = "${item.summary.pendingPolicies} Pending",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun EmptyDatabaseReportsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Assessment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Business Records Recorded",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Recorded policies will automatically generate premium analytics, monthly trends, status breakdowns, and source attributions here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun EmptyFilteredReportsCard(onResetToAllTime: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(36.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "No Records in Selected Period",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "No business records match this date filter. Select a broader date range or return to all-time view.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onResetToAllTime) {
                Text("Show All Time")
            }
        }
    }
}

@Composable
private fun CustomDateRangeDialog(
    initialStart: Long,
    initialEnd: Long,
    onConfirm: (Long, Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var startDate by remember { mutableStateOf(initialStart) }
    var endDate by remember { mutableStateOf(initialEnd) }
    var isPickingStart by remember { mutableStateOf(false) }
    var isPickingEnd by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Custom Date Range",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Filter business analytics between start and end transaction dates.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Start Date Picker Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .clickable { isPickingStart = true }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(text = "Start Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(text = DateUtils.formatDate(startDate), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    }
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }

                // End Date Picker Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .clickable { isPickingEnd = true }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(text = "End Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(text = DateUtils.formatDate(endDate), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    }
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(startDate, endDate) },
            ) {
                Text("Apply Filter")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )

    if (isPickingStart) {
        ReportDatePickerModal(
            initialDate = startDate,
            onDateSelected = {
                startDate = it
                isPickingStart = false
            },
            onDismiss = { isPickingStart = false },
        )
    }

    if (isPickingEnd) {
        ReportDatePickerModal(
            initialDate = endDate,
            onDateSelected = {
                endDate = it
                isPickingEnd = false
            },
            onDismiss = { isPickingEnd = false },
        )
    }
}

@Composable
private fun ReportDatePickerModal(
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

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun ReportsScreenPreview() {
    com.dravid.businesstracker.ui.theme.BusinessTrackerTheme {
        ReportsScreen()
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun ReportsScreenPopulatedPreview() {
    val sampleReport = BusinessReport(
        summary = com.dravid.businesstracker.data.model.BusinessSummary(
            totalIssuedPremiumPaise = 4500000L,
            totalIssuedPolicies = 12,
            pendingPremiumPaise = 1250000L,
            pendingPolicies = 4,
            cancelledPolicies = 2,
            totalRecords = 18,
        ),
        averageIssuedPremiumPaise = 375000L,
        cancelledPremiumPaise = 650000L,
        monthlyPerformances = listOf(
            MonthlyPerformance(
                yearMonth = java.time.YearMonth.of(2026, 11),
                summary = com.dravid.businesstracker.data.model.BusinessSummary(
                    totalIssuedPremiumPaise = 2500000L,
                    totalIssuedPolicies = 7,
                    pendingPremiumPaise = 750000L,
                    pendingPolicies = 2,
                    totalRecords = 10,
                ),
            ),
            MonthlyPerformance(
                yearMonth = java.time.YearMonth.of(2026, 10),
                summary = com.dravid.businesstracker.data.model.BusinessSummary(
                    totalIssuedPremiumPaise = 2000000L,
                    totalIssuedPolicies = 5,
                    pendingPremiumPaise = 500000L,
                    pendingPolicies = 2,
                    totalRecords = 8,
                ),
            ),
        ),
        sourcePerformances = listOf(
            SourcePerformance(
                sourceId = 1L,
                sourceName = "Direct Sales",
                summary = com.dravid.businesstracker.data.model.BusinessSummary(
                    totalIssuedPremiumPaise = 3000000L,
                    totalIssuedPolicies = 8,
                    pendingPremiumPaise = 750000L,
                    pendingPolicies = 2,
                    totalRecords = 11,
                ),
            ),
            SourcePerformance(
                sourceId = 2L,
                sourceName = "Agent Network",
                summary = com.dravid.businesstracker.data.model.BusinessSummary(
                    totalIssuedPremiumPaise = 1000000L,
                    totalIssuedPolicies = 3,
                    pendingPremiumPaise = 500000L,
                    pendingPolicies = 2,
                    totalRecords = 5,
                ),
            ),
        ),
        unassignedPerformance = SourcePerformance(
            sourceId = null,
            sourceName = "Unassigned",
            summary = com.dravid.businesstracker.data.model.BusinessSummary(
                totalIssuedPremiumPaise = 500000L,
                totalIssuedPolicies = 1,
                totalRecords = 2,
            ),
        ),
        statusBreakdown = listOf(
            StatusBreakdownItem(BusinessStatus.ISSUED, 12, 4500000L, 66.7f),
            StatusBreakdownItem(BusinessStatus.PENDING, 4, 1250000L, 22.2f),
            StatusBreakdownItem(BusinessStatus.CANCELLED, 2, 650000L, 11.1f),
        ),
        filteredRecordCount = 18,
        totalDatabaseRecordCount = 18,
    )

    com.dravid.businesstracker.ui.theme.BusinessTrackerTheme {
        ReportsScreenContent(
            uiState = ReportsUiState(
                selectedFilter = ReportPeriodFilter.THIS_MONTH,
                startDateMillis = 1794000000000L,
                endDateMillis = 1796592000000L,
                report = sampleReport,
                isLoading = false,
            ),
            onFilterSelected = {},
            onOpenCustomPicker = {},
            onResetToAllTime = {},
            onApplyCustomRange = { _, _ -> },
            onCloseCustomRange = {},
        )
    }
}
