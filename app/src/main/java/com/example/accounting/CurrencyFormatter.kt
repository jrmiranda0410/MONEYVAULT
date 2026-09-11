package com.example.accounting

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

object CurrencyFormatter {
    private val decimalFormat = DecimalFormat("#,##,##0.00", DecimalFormatSymbols(Locale.ENGLISH))

    fun formatPaise(paise: Long, symbol: String = "₹"): String {
        val isNegative = paise < 0
        val positivePaise = abs(paise)
        val rupees = positivePaise / 100
        val minor = positivePaise % 100
        val value = rupees + (minor / 100.0)
        val formatted = decimalFormat.format(value)
        return if (isNegative) "-$symbol$formatted" else "$symbol$formatted"
    }

    /**
     * Parses a user input string into minor units (paise).
     * Accepts "100", "100.5", "100.50", "1,000", etc.
     */
    fun parseInputToPaise(input: String): Long? {
        val cleaned = input.trim().replace(",", "").replace("₹", "").replace("$", "").replace("€", "").replace("£", "")
        if (cleaned.isEmpty()) return null
        return try {
            val parts = cleaned.split(".")
            if (parts.size > 2) return null
            val wholePart = parts[0].toLongOrNull() ?: 0L
            val fractionPart = if (parts.size == 2) {
                val dec = parts[1]
                when {
                    dec.isEmpty() -> 0L
                    dec.length == 1 -> (dec + "0").toLong()
                    else -> dec.substring(0, 2).toLong()
                }
            } else 0L

            if (wholePart < 0) {
                -1L * (abs(wholePart) * 100L + fractionPart)
            } else {
                wholePart * 100L + fractionPart
            }
        } catch (e: Exception) {
            null
        }
    }

    fun paiseToInputString(paise: Long): String {
        val absPaise = abs(paise)
        val whole = absPaise / 100
        val frac = absPaise % 100
        return if (frac == 0L) {
            "$whole"
        } else {
            String.format(Locale.US, "%d.%02d", whole, frac)
        }
    }
}
