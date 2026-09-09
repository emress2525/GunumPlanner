package com.emre.gunumplanner

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

data class DeviceCalendarEvent(
    val id: Long,
    val title: String,
    val start: Long,
    val end: Long,
    val allDay: Boolean,
    val calendarName: String
)

object CalendarBridge {
    fun eventsForDay(context: Context, day: LocalDate): List<DeviceCalendarEvent> {
        if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) return emptyList()
        val zone = ZoneId.systemDefault()
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, start)
        ContentUris.appendId(builder, end)
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME
        )
        val out = mutableListOf<DeviceCalendarEvent>()
        try {
            context.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                CalendarContract.Instances.BEGIN + " ASC"
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    out += DeviceCalendarEvent(
                        id = cursor.getLong(0),
                        title = cursor.getString(1) ?: "",
                        start = cursor.getLong(2),
                        end = cursor.getLong(3),
                        allDay = cursor.getInt(4) == 1,
                        calendarName = cursor.getString(5) ?: "Takvim"
                    )
                }
            }
        } catch (_: Exception) {
            return emptyList()
        }
        return out
    }

    fun addTask(context: Context, item: Db.Item): Boolean {
        if (item.dueAt <= 0) return false
        if (context.checkSelfPermission(Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) return false
        if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) return false
        val calendarId = preferredWritableCalendar(context) ?: return false
        val duration = (if (item.durationMinutes > 0) item.durationMinutes else 30) * 60_000L
        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, item.dueAt)
            put(CalendarContract.Events.DTEND, item.dueAt + duration)
            put(CalendarContract.Events.TITLE, item.title)
            put(CalendarContract.Events.DESCRIPTION, item.body)
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }
        return try {
            context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values) != null
        } catch (_: Exception) {
            false
        }
    }

    private fun preferredWritableCalendar(context: Context): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.VISIBLE,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )
        val candidates = mutableListOf<Pair<Long, Boolean>>()
        return try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                CalendarContract.Calendars.VISIBLE + "=1 AND " + CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + ">=?",
                arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val accountType = cursor.getString(1) ?: ""
                    candidates += id to (accountType == "com.google")
                }
            }
            candidates.firstOrNull { it.second }?.first ?: candidates.firstOrNull()?.first
        } catch (_: Exception) {
            null
        }
    }
}
