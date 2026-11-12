package com.dravid.businesstracker.util

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Date formatting and parsing utility using java.time (API 26+).
 */
object DateUtils {

    private val displayFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    private val shortMonthYearFormatter = DateTimeFormatter.ofPattern("MMM yyyy")
    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun todayMillis(): Long {
        return System.currentTimeMillis()
    }

    fun todayLocalDate(): LocalDate {
        return LocalDate.now(ZoneId.systemDefault())
    }

    fun formatDate(millis: Long): String {
        return try {
            val localDate = Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            displayFormatter.format(localDate)
        } catch (_: Exception) {
            ""
        }
    }

    fun formatDate(date: LocalDate): String {
        return try {
            displayFormatter.format(date)
        } catch (_: Exception) {
            ""
        }
    }

    fun formatYearMonth(yearMonth: YearMonth): String {
        return try {
            monthYearFormatter.format(yearMonth)
        } catch (_: Exception) {
            ""
        }
    }

    fun formatShortMonthYear(yearMonth: YearMonth): String {
        return try {
            shortMonthYearFormatter.format(yearMonth)
        } catch (_: Exception) {
            ""
        }
    }

    fun formatDateIso(millis: Long): String {
        return try {
            val localDate = Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            isoFormatter.format(localDate)
        } catch (_: Exception) {
            ""
        }
    }

    fun parseIsoDate(dateString: String): Long? {
        return try {
            val localDate = LocalDate.parse(dateString.trim(), isoFormatter)
            localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (_: DateTimeParseException) {
            null
        }
    }

    fun toLocalDate(millis: Long): LocalDate {
        return Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    fun toStartOfDayMillis(date: LocalDate): Long {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun startOfDay(millis: Long): Long {
        return try {
            val localDate = Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (_: Exception) {
            millis
        }
    }

    fun endOfDay(millis: Long): Long {
        return try {
            val localDate = Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            localDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
        } catch (_: Exception) {
            millis
        }
    }

    fun isSameDay(millis1: Long, millis2: Long): Boolean {
        return startOfDay(millis1) == startOfDay(millis2)
    }

    fun isToday(millis: Long): Boolean {
        return isSameDay(millis, todayMillis())
    }

    fun toYearMonth(millis: Long): YearMonth {
        return YearMonth.from(toLocalDate(millis))
    }

    fun startOfMonth(yearMonth: YearMonth): Long {
        return toStartOfDayMillis(yearMonth.atDay(1))
    }

    fun endOfMonth(yearMonth: YearMonth): Long {
        return endOfDay(toStartOfDayMillis(yearMonth.atEndOfMonth()))
    }

    fun startOfYear(year: Int): Long {
        return toStartOfDayMillis(LocalDate.of(year, 1, 1))
    }

    fun endOfYear(year: Int): Long {
        return endOfDay(toStartOfDayMillis(LocalDate.of(year, 12, 31)))
    }
}
