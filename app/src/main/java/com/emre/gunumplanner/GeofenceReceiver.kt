package com.emre.gunumplanner

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        val transition = event.geofenceTransition
        if (transition != Geofence.GEOFENCE_TRANSITION_ENTER && transition != Geofence.GEOFENCE_TRANSITION_EXIT) return

        val db = Db(context)
        ExtrasRepository.ensureSchema(db)
        event.triggeringGeofences.orEmpty().forEach { fence ->
            val itemId = fence.requestId.removePrefix("gunum_item_").toLongOrNull() ?: return@forEach
            val item = db.getItem(itemId) ?: return@forEach
            if (item.status != Db.STATUS_OPEN) return@forEach
            val rule = ExtrasRepository.getLocation(db, itemId) ?: return@forEach
            notify(context, item, rule, transition)
        }
    }

    private fun notify(context: Context, item: Db.Item, rule: ExtrasRepository.LocationRule, transition: Int) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "gunum_location"
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Konum hatırlatmaları", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Bir konuma gelince veya konumdan çıkınca görev hatırlatmaları"
                }
            )
        }

        val openIntent = Intent(context, PremiumV2Activity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPi = PendingIntent.getActivity(
            context,
            (item.id and 0x7fffffff).toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "COMPLETE"
            putExtra("itemId", item.id)
        }
        val donePi = PendingIntent.getBroadcast(
            context,
            ((item.id + 310000) and 0x7fffffff).toInt(),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val place = rule.label.ifBlank { rule.address.ifBlank { "belirlediğin konum" } }
        val verb = if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) "ayrıldın" else "geldin"
        val notification = Notification.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle("$place konumuna $verb")
            .setContentText(item.title)
            .setStyle(Notification.BigTextStyle().bigText("$place konumuna $verb. ${item.title}"))
            .setAutoCancel(true)
            .setContentIntent(openPi)
            .addAction(Notification.Action.Builder(android.R.drawable.checkbox_on_background, "Tamamla", donePi).build())
            .build()

        manager.notify((item.id and 0x7fffffff).toInt(), notification)
    }
}
