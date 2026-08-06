package com.sentinelle.app.ui.viewmodel.dashboard

import com.sentinelle.app.data.DayCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// padDailyTrend is deliberately pure (no Android, no Room) so the chart's
// data shaping is testable without a device — same approach as
// HeuristicSpamDetector.scoreFromHistory.
class PadDailyTrendTest {
    private val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // Fixed reference point so these don't drift with the wall clock.
    private val today =
        Calendar
            .getInstance()
            .apply {
                set(2026, Calendar.AUGUST, 6, 12, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

    private fun dayKey(daysAgo: Int): String =
        Calendar
            .getInstance()
            .apply {
                timeInMillis = today
                add(Calendar.DAY_OF_YEAR, -daysAgo)
            }.let { formatter.format(it.time) }

    @Test
    fun emptyTrendStaysEmptySoTheUiCanShowItsOwnMessage() {
        assertEquals(emptyList<DayCount>(), padDailyTrend(emptyList(), TimeRange.WEEK, today))
    }

    /**
     * The reported bug: a single blocked call produced a one-entry list, so
     * the chart drew one bar at full width and full height — a solid block
     * rather than a trend.
     */
    @Test
    fun singleEventIsPaddedToAFullWeekRatherThanOneFullWidthBar() {
        val trend = listOf(DayCount(day = dayKey(1), count = 1))
        val padded = padDailyTrend(trend, TimeRange.WEEK, today)

        assertEquals(7, padded.size)
        assertEquals(1, padded.count { it.count > 0 })
        assertEquals(6, padded.count { it.count == 0 })
    }

    @Test
    fun paddedDaysAreContiguousAndEndToday() {
        val padded = padDailyTrend(listOf(DayCount(dayKey(2), 3)), TimeRange.WEEK, today)

        assertEquals(dayKey(6), padded.first().day)
        assertEquals(dayKey(0), padded.last().day)
        assertEquals((6 downTo 0).map { dayKey(it) }, padded.map { it.day })
    }

    @Test
    fun existingCountsSurvivePaddingOnTheRightDays() {
        val trend =
            listOf(
                DayCount(dayKey(4), 2),
                DayCount(dayKey(1), 5),
            )
        val padded = padDailyTrend(trend, TimeRange.WEEK, today).associate { it.day to it.count }

        assertEquals(2, padded[dayKey(4)])
        assertEquals(5, padded[dayKey(1)])
        assertEquals(0, padded[dayKey(3)])
    }

    @Test
    fun monthRangePadsToThirtyDays() {
        val padded = padDailyTrend(listOf(DayCount(dayKey(3), 1)), TimeRange.MONTH, today)
        assertEquals(30, padded.size)
    }

    @Test
    fun allRangeSpansFromTheFirstRecordedDayToToday() {
        val padded = padDailyTrend(listOf(DayCount(dayKey(9), 1)), TimeRange.ALL, today)

        assertEquals(10, padded.size)
        assertEquals(dayKey(9), padded.first().day)
        assertEquals(dayKey(0), padded.last().day)
    }

    /**
     * Without a cap, a year-old first event would draw 365 bars into the
     * same width — each under a pixel.
     */
    @Test
    fun allRangeIsCappedSoBarsStayReadable() {
        val padded = padDailyTrend(listOf(DayCount(dayKey(400), 1)), TimeRange.ALL, today)

        assertEquals(90, padded.size)
        assertEquals(dayKey(0), padded.last().day)
    }

    @Test
    fun allRangeWithOnlyTodaysEventsIsASingleDay() {
        val padded = padDailyTrend(listOf(DayCount(dayKey(0), 4)), TimeRange.ALL, today)

        assertEquals(1, padded.size)
        assertEquals(4, padded.single().count)
    }

    @Test
    fun eventsOlderThanTheWindowDoNotShiftTheAxis() {
        // A stale row (shouldn't normally reach here, since the DAO filters
        // by range) must not push the axis back past today.
        val padded = padDailyTrend(listOf(DayCount(dayKey(40), 1)), TimeRange.WEEK, today)

        assertEquals(7, padded.size)
        assertEquals(dayKey(0), padded.last().day)
        assertTrue(padded.all { it.count == 0 })
    }
}
