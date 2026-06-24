package com.rinx.artRINXapp.feature.notifications.presentation.messages

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Date/time formatting for the chat thread: a per-day separator header ("Today" / "Jun 22") and the
 * short time-in-bubble label ("2:30 pm"). All in the device's local timezone, mirroring the
 * SimpleDateFormat approach used by EventFormatting (the app doesn't use java.time).
 */
object ChatDateTime {

    /** Local-midnight epoch for [epochMs] — equal for two messages on the same calendar day. */
    fun dayKey(epochMs: Long): Long {
        if (epochMs <= 0L) return 0L
        return Calendar.getInstance().apply {
            timeInMillis = epochMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /** Centered separator label: "Today" / "Yesterday" / "Jun 22" / "Jun 22, 2023" (other years). */
    fun dayHeader(epochMs: Long, nowMs: Long = System.currentTimeMillis()): String {
        if (epochMs <= 0L) return ""
        val day = dayKey(epochMs)
        val today = dayKey(nowMs)
        val oneDayMs = 24L * 60 * 60 * 1000
        return when (day) {
            today -> "Today"
            today - oneDayMs -> "Yesterday"
            else -> {
                val sameYear = Calendar.getInstance().apply { timeInMillis = epochMs }.get(Calendar.YEAR) ==
                    Calendar.getInstance().apply { timeInMillis = nowMs }.get(Calendar.YEAR)
                val pattern = if (sameYear) "MMM d" else "MMM d, yyyy"
                SimpleDateFormat(pattern, Locale.US).format(Date(epochMs))
            }
        }
    }

    /** Short time-in-bubble label, e.g. "2:30 pm". Empty when [epochMs] is unset. */
    fun shortTime(epochMs: Long): String {
        if (epochMs <= 0L) return ""
        return SimpleDateFormat("h:mm a", Locale.US).format(Date(epochMs)).lowercase(Locale.US)
    }
}
