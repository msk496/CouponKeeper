package kr.co.couponkeeper

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import java.util.*

object CalendarHelper {
    fun addIfPermitted(ctx:Context,title:String,body:String,expiry:Long) {
        if(ctx.checkSelfPermission(Manifest.permission.WRITE_CALENDAR)!=PackageManager.PERMISSION_GRANTED) return
        try {
            val calendars=ctx.contentResolver.query(CalendarContract.Calendars.CONTENT_URI,
                arrayOf(CalendarContract.Calendars._ID), "${CalendarContract.Calendars.VISIBLE}=1",null,null)
            val id=calendars?.use { if(it.moveToFirst()) it.getLong(0) else -1L } ?: -1L
            if(id<0) return
            val start=expiry-15*60*1000L
            val values=ContentValues().apply {
                put(CalendarContract.Events.DTSTART,start); put(CalendarContract.Events.DTEND,expiry)
                put(CalendarContract.Events.TITLE,"쿠폰 만료: $title")
                put(CalendarContract.Events.DESCRIPTION,body.take(500))
                put(CalendarContract.Events.CALENDAR_ID,id); put(CalendarContract.Events.EVENT_TIMEZONE,TimeZone.getDefault().id)
                put(CalendarContract.Events.HAS_ALARM,1)
            }
            val uri=ctx.contentResolver.insert(CalendarContract.Events.CONTENT_URI,values) ?: return
            val eventId=uri.lastPathSegment?.toLongOrNull() ?: return
            ctx.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI,ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID,eventId); put(CalendarContract.Reminders.MINUTES,1440)
                put(CalendarContract.Reminders.METHOD,CalendarContract.Reminders.METHOD_ALERT)
            })
        } catch (_:Exception) { }
    }
}
