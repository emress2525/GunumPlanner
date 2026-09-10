package com.emre.gunumplanner

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

object LocationReminderManager {
    private const val PREFIX = "gunum_item_"

    fun hasForegroundPermission(context: Context): Boolean =
        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun hasBackgroundPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 29) return hasForegroundPermission(context)
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun canRunInBackground(context: Context): Boolean = hasForegroundPermission(context) && hasBackgroundPermission(context)

    fun schedule(context: Context, rule: ExtrasRepository.LocationRule): Boolean {
        if (!rule.enabled || !canRunInBackground(context)) return false
        val transitionType = if (rule.transition == "EXIT") Geofence.GEOFENCE_TRANSITION_EXIT else Geofence.GEOFENCE_TRANSITION_ENTER
        val geofence = Geofence.Builder()
            .setRequestId(PREFIX + rule.itemId)
            .setCircularRegion(rule.lat, rule.lng, rule.radius.coerceAtLeast(100f))
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(transitionType)
            .build()
        val request = GeofencingRequest.Builder()
            .setInitialTrigger(0)
            .addGeofence(geofence)
            .build()
        return try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return false
            LocationServices.getGeofencingClient(context).addGeofences(request, pendingIntent(context))
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun cancel(context: Context, itemId: Long) {
        runCatching { LocationServices.getGeofencingClient(context).removeGeofences(listOf(PREFIX + itemId)) }
    }

    fun rescheduleAll(context: Context) {
        if (!canRunInBackground(context)) return
        val db = Db(context)
        ExtrasRepository.ensureSchema(db)
        ExtrasRepository.getEnabledLocations(db).forEach { rule ->
            val item = db.getItem(rule.itemId)
            if (item != null && item.status == Db.STATUS_OPEN) schedule(context, rule)
        }
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
        return PendingIntent.getBroadcast(context, 7119, intent, flags)
    }
}
