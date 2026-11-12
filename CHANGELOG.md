# Changelog

All notable changes to the BusinessTracker project are documented in this file.

## [1.0.0] - 2026-11-12

### Initial Production Release (v1.0)

#### Core Architecture & Platform
- **Offline-First & Local Storage:** Fully local SQLite database powered by Room 2.8 (Schema Version 4). Zero network, telemetry, or external cloud dependencies.
- **Precision Monetary Arithmetic:** All insurance premium amounts are stored and calculated as exact integer `Long` paise (1 Rupee = 100 paise), eliminating floating-point rounding errors.
- **Modern Android Architecture:** MVVM architecture using Jetpack Compose, Material 3, Kotlin Coroutines, and reactive Flows.
- **Responsive Layout:** Adaptive UI supporting smartphones (bottom `NavigationBar`) and foldables/tablets (side `NavigationRail`).

#### Main Features
- **Performance Dashboard (Home):**
  - Real-time issued and pending premium summaries.
  - Policy count metrics and action-item schedules.
  - Overdue alerts and today's schedule summaries.
- **Business Management:**
  - Full CRUD operations for policy records.
  - Support for `ISSUED`, `PENDING`, and `CANCELLED` status workflows.
  - Strict validation ensuring issue date is present for issued policies.
  - Case-insensitive search, status chip filtering, and chronological sorting.
- **Lead & Referral Sources:**
  - Acquisition channel attribution with issued and pending volume analytics.
  - Case-insensitive uniqueness enforcement for source names.
  - Safe source deletion with automatic unlinking of associated policies.
- **Calendar & Scheduling:**
  - Month navigation with calendar grid dot indicators.
  - Policy renewal and client follow-up scheduling with optional policy links.
  - Automatic overdue event calculation based on system date boundaries.
  - Safe policy deletion with automatic unlinking of linked calendar events.
- **Reports & Analytics:**
  - Multi-period filtering (All Time, This Month, Last Month, This Year, Custom Date Ranges).
  - Executive KPI summary cards with average premium calculation.
  - Monthly performance trends with native Compose bar visualizations.
  - Status breakdown distribution and channel performance rankings.
- **Settings & Theming:**
  - System Default, Clean Light, and Midnight Dark themes.
  - Persistent theme selection via Preferences DataStore.
  - System diagnostics and verified privacy guarantees.

#### Reliability & QA
- **Database Atomicity:** Room `@Transaction` operations for safe policy and source deletions with unlinking.
- **Non-Destructive Room Migrations:** Fully tested sequential migrations (1 → 2, 2 → 3, 3 → 4).
- **Test Suite:** 75 automated unit and integration tests passing cleanly.
