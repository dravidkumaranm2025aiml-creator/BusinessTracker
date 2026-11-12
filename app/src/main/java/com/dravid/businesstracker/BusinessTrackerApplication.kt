package com.dravid.businesstracker

import android.app.Application
import com.dravid.businesstracker.data.local.BusinessDatabase
import com.dravid.businesstracker.data.repository.BusinessRepository
import com.dravid.businesstracker.data.repository.BusinessRepositoryImpl
import com.dravid.businesstracker.data.repository.CalendarRepository
import com.dravid.businesstracker.data.repository.CalendarRepositoryImpl
import com.dravid.businesstracker.data.repository.SourceRepository
import com.dravid.businesstracker.data.repository.SourceRepositoryImpl
import com.dravid.businesstracker.data.repository.ThemePreferencesRepository
import com.dravid.businesstracker.data.repository.ThemePreferencesRepositoryImpl

/**
 * Custom Application class providing dependency access for Room database and repositories.
 */
class BusinessTrackerApplication : Application() {

    val database: BusinessDatabase by lazy {
        BusinessDatabase.getDatabase(this)
    }

    val businessRepository: BusinessRepository by lazy {
        BusinessRepositoryImpl(database.businessDao())
    }

    val sourceRepository: SourceRepository by lazy {
        SourceRepositoryImpl(
            sourceDao = database.sourceDao(),
            businessDao = database.businessDao(),
        )
    }

    val calendarRepository: CalendarRepository by lazy {
        CalendarRepositoryImpl(
            calendarEventDao = database.calendarEventDao(),
            businessDao = database.businessDao(),
        )
    }

    val themeRepository: ThemePreferencesRepository by lazy {
        ThemePreferencesRepositoryImpl(this)
    }
}
