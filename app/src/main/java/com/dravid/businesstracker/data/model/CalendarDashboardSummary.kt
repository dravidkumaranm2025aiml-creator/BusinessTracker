package com.dravid.businesstracker.data.model

/**
 * Summary metrics of scheduled calendar events for dashboard integration.
 */
data class CalendarDashboardSummary(
    val todayEventsCount: Int = 0,
    val upcomingRenewalsCount: Int = 0,
    val upcomingFollowUpsCount: Int = 0,
    val overdueCount: Int = 0,
    val todayEvents: List<CalendarEventWithPolicy> = emptyList(),
    val overdueEvents: List<CalendarEventWithPolicy> = emptyList(),
)
