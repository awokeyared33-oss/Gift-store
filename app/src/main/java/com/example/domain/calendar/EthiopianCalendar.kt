package com.example.domain.calendar

import java.time.LocalDate

/**
 * Authentic Ethiopian (Ge'ez) Calendar Engine.
 * 
 * Ethiopian Calendar properties:
 * - 13 months: 12 months of 30 days, plus 13th month 'Pagume' (5 days, or 6 days in leap years).
 * - Leap year occurs every 4 years (when year % 4 == 3).
 * - Ethiopian year is 7 or 8 years behind the Gregorian year.
 * - Meskerem 1 (Ethiopian New Year) falls on September 11 (or September 12 in the Gregorian year preceding an Ethiopian leap year).
 */
data class EthiopianDate(
    val year: Int,
    val month: Int, // 1 to 13
    val day: Int    // 1 to 30 (or 1 to 5/6 for month 13)
) {
    init {
        require(month in 1..13) { "Ethiopian month must be between 1 and 13, got $month" }
        val maxDays = if (month == 13) {
            if (isEthiopianLeapYear(year)) 6 else 5
        } else {
            30
        }
        require(day in 1..maxDays) { "Day must be between 1 and $maxDays for month $month in year $year, got $day" }
    }

    fun isLeapYear(): Boolean = isEthiopianLeapYear(year)

    fun getMonthNameEnglish(): String = ETHIOPIAN_MONTHS_EN[month - 1]
    fun getMonthNameAmharic(): String = ETHIOPIAN_MONTHS_AM[month - 1]

    fun format(isAmharic: Boolean = false): String {
        val mName = if (isAmharic) getMonthNameAmharic() else getMonthNameEnglish()
        return "$mName $day, $year"
    }

    companion object {
        fun isEthiopianLeapYear(year: Int): Boolean = (year % 4 == 3)

        val ETHIOPIAN_MONTHS_EN = listOf(
            "Meskerem",  // 1
            "Tikimt",    // 2
            "Hidar",     // 3
            "Tahsas",    // 4
            "Tir",       // 5
            "Yakatit",   // 6
            "Magabit",   // 7
            "Miazia",    // 8
            "Ginbot",    // 9
            "Sene",      // 10
            "Hamle",     // 11
            "Nehase",    // 12
            "Pagume"     // 13
        )

        val ETHIOPIAN_MONTHS_AM = listOf(
            "መስከረም", // 1
            "ጥቅምት",  // 2
            "ኅዳር",   // 3
            "ታኅሣሥ",  // 4
            "ጥር",    // 5
            "የካቲት",  // 6
            "መጋቢት",  // 7
            "ሚያዝያ",  // 8
            "ግንቦት",  // 9
            "ሰኔ",    // 10
            "ሐምሌ",   // 11
            "ነሐሴ",   // 12
            "ጳጉሜ"   // 13
        )
    }
}

object EthiopianCalendarConverter {

    // On Gregorian 2024-09-11 (Epoch Day 19977), Ethiopian date is Meskerem 1, 2017 EC.
    // 2016 full Ethiopian years = 504 4-year cycles of 1461 days = 736344 days.
    // Epoch offset = 19977 - 736344 = -716367L
    private const val ETHIOPIAN_EPOCH_DIFF = -716367L

    /**
     * Converts Gregorian LocalDate to EthiopianDate.
     */
    fun toEthiopian(gregorianDate: LocalDate): EthiopianDate {
        val daysSinceEthEpoch = gregorianDate.toEpochDay() - ETHIOPIAN_EPOCH_DIFF
        val cycle = Math.floorDiv(daysSinceEthEpoch, 1461L)
        val dayInCycle = Math.floorMod(daysSinceEthEpoch, 1461L).toInt()

        val (yearOffset, dayOfYear) = when {
            dayInCycle < 365 -> 0 to dayInCycle
            dayInCycle < 730 -> 1 to (dayInCycle - 365)
            dayInCycle < 1096 -> 2 to (dayInCycle - 730)
            else -> 3 to (dayInCycle - 1096)
        }

        val year = (cycle * 4 + yearOffset + 1).toInt()
        val month = (dayOfYear / 30) + 1
        val day = (dayOfYear % 30) + 1

        return EthiopianDate(year, month, day)
    }

    /**
     * Converts EthiopianDate to Gregorian LocalDate.
     */
    fun toGregorian(ethiopianDate: EthiopianDate): LocalDate {
        val y = ethiopianDate.year - 1
        val cycle = Math.floorDiv(y.toLong(), 4L)
        val yearOffset = Math.floorMod(y.toLong(), 4L).toInt()

        val daysInCycleBeforeYear = when (yearOffset) {
            0 -> 0
            1 -> 365
            2 -> 730
            3 -> 1096
            else -> 0
        }

        val dayOfYear = (ethiopianDate.month - 1) * 30 + (ethiopianDate.day - 1)
        val daysSinceEthEpoch = cycle * 1461L + daysInCycleBeforeYear + dayOfYear
        val epochDay = daysSinceEthEpoch + ETHIOPIAN_EPOCH_DIFF
        return LocalDate.ofEpochDay(epochDay)
    }

    /**
     * Calculates the next upcoming occurrence of a celebration in Gregorian LocalDate,
     * given its Ethiopian month, day, and recurrence cycle.
     * 
     * @param fromDate current Gregorian date
     * @param ethMonth celebration Ethiopian month (1-13)
     * @param ethDay celebration Ethiopian day
     * @param recurrenceCycle "YEARLY", "MONTHLY", "EVERY_TWO_YEARS", "NONE"
     * @return next occurrence in LocalDate
     */
    fun getNextCelebrationDate(
        fromDate: LocalDate,
        ethMonth: Int,
        ethDay: Int,
        recurrenceCycle: String = "YEARLY"
    ): LocalDate {
        val currentEth = toEthiopian(fromDate)
        
        fun safeDate(y: Int, m: Int): EthiopianDate {
            val maxDay = if (m == 13) {
                if (EthiopianDate.isEthiopianLeapYear(y)) 6 else 5
            } else 30
            val validDay = ethDay.coerceAtMost(maxDay).coerceAtLeast(1)
            return EthiopianDate(y, m, validDay)
        }

        return when (recurrenceCycle.uppercase()) {
            "MONTHLY" -> {
                // Occurs every month on ethDay
                val thisMonthDate = safeDate(currentEth.year, currentEth.month)
                val thisMonthGreg = toGregorian(thisMonthDate)
                if (!thisMonthGreg.isBefore(fromDate)) {
                    thisMonthGreg
                } else {
                    var nextMonth = currentEth.month + 1
                    var nextYear = currentEth.year
                    if (nextMonth > 13) {
                        nextMonth = 1
                        nextYear += 1
                    }
                    toGregorian(safeDate(nextYear, nextMonth))
                }
            }
            "EVERY_TWO_YEARS" -> {
                val thisYearTargetEth = safeDate(currentEth.year, ethMonth)
                val thisYearGreg = toGregorian(thisYearTargetEth)
                if (!thisYearGreg.isBefore(fromDate)) {
                    thisYearGreg
                } else {
                    toGregorian(safeDate(currentEth.year + 2, ethMonth))
                }
            }
            "NONE" -> {
                val thisYearTargetEth = safeDate(currentEth.year, ethMonth)
                toGregorian(thisYearTargetEth)
            }
            else -> { // "YEARLY"
                val thisYearTargetEth = safeDate(currentEth.year, ethMonth)
                val thisYearGreg = toGregorian(thisYearTargetEth)
                if (!thisYearGreg.isBefore(fromDate)) {
                    thisYearGreg
                } else {
                    toGregorian(safeDate(currentEth.year + 1, ethMonth))
                }
            }
        }
    }
}
