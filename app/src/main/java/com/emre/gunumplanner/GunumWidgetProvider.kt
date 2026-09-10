package com.emre.gunumplanner

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.time.LocalDate

class GunumWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        super.onUpdate(context, manager, ids)
        ids.forEach { update(context, manager, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            val component = android.content.ComponentName(context, GunumWidgetProvider::class.java)
            manager.getAppWidgetIds(component).forEach { update(context, manager, it) }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.emre.gunumplanner.WIDGET_REFRESH"
        fun refreshAll(context: Context) {
            context.sendBroadcast(Intent(context, GunumWidgetProvider::class.java).setAction(ACTION_REFRESH))
        }

        private fun update(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val db = Db(context)
            val today = LocalDate.now().toString()
            val tasks = db.getItemsForDay(today).filter { it.type == Db.TYPE_TASK && it.status == Db.STATUS_OPEN }
            val next = tasks.minByOrNull { if (it.dueAt > 0) it.dueAt else Long.MAX_VALUE }
            val views = RemoteViews(context.packageName, R.layout.widget_gunum)
            views.setTextViewText(R.id.widget_count, if (tasks.isEmpty()) "Bugün açık görev yok" else "${tasks.size} açık görev")
            views.setTextViewText(R.id.widget_title, next?.title ?: "Günün hazır")
            views.setTextViewText(R.id.widget_sub, next?.let { if (it.durationMinutes > 0) "${it.durationMinutes} dk • dokun ve aç" else "Dokun ve aç" } ?: "Yeni bir görev ekleyebilirsin")

            val open = PendingIntent.getActivity(
                context, 41000 + widgetId,
                Intent(context, PremiumV2Activity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, open)
            views.setOnClickPendingIntent(R.id.widget_add, open)

            if (next != null) {
                val done = PendingIntent.getBroadcast(
                    context, 42000 + widgetId,
                    Intent(context, NotificationActionReceiver::class.java).setAction("COMPLETE").putExtra("itemId", next.id),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_done, done)
                views.setViewVisibility(R.id.widget_done, android.view.View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_done, android.view.View.GONE)
            }
            manager.updateAppWidget(widgetId, views)
        }
    }
}
