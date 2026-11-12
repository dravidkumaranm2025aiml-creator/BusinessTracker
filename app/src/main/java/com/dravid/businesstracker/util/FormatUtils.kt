package com.dravid.businesstracker.util

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Currency and monetary formatting utilities ensuring exact precision using paise (1 Rupee = 100 paise).
 */
object FormatUtils {

    /**
     * Formats paise (Long) to exact currency display string: ₹1,250.50
     */
    fun formatPaiseToCurrency(paise: Long): String {
        val isNegative = paise < 0
        val absPaise = if (isNegative) -paise else paise
        val rupees = absPaise / 100
        val cents = absPaise % 100

        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
        }
        val df = DecimalFormat("#,##0", symbols)
        val formattedRupees = df.format(rupees)
        val formattedCents = String.format(Locale.US, "%02d", cents)

        val prefix = if (isNegative) "-₹" else "₹"
        return "$prefix$formattedRupees.$formattedCents"
    }

    /**
     * Formats paise to plain decimal text (e.g. 1250.50) for editing in text input fields.
     */
    fun formatPaiseToDecimalString(paise: Long): String {
        return BigDecimal.valueOf(paise)
            .movePointLeft(2)
            .setScale(2)
            .toPlainString()
    }

    /**
     * Parses user rupee string (e.g. "1250.50" or "₹1,250.50" or "100") to exact paise (Long).
     * Returns null if string is not a valid number or has more than 2 decimal places.
     */
    fun parseRupeesToPaise(input: String): Long? {
        val clean = input.replace("₹", "").replace(",", "").trim()
        if (clean.isEmpty()) return null
        return try {
            val bd = BigDecimal(clean)
            if (bd.scale() > 2) return null
            val paiseBd = bd.movePointRight(2)
            val maxBd = BigDecimal.valueOf(Long.MAX_VALUE)
            val minBd = BigDecimal.valueOf(Long.MIN_VALUE)
            if ((paiseBd > maxBd) || (paiseBd < minBd)) {
                return null
            }
            paiseBd.longValueExact()
        } catch (_: Exception) {
            null
        }
    }
}
