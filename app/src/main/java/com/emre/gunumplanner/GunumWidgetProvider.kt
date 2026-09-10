package com.emre.gunumplanner

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import java.time.LocalDate

class GunumWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        super.onUpdate(context, manager, ids)
        ids.forEach { update(context, manager, it) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        update(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, GunumWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
                ids.forEach { update(context, manager, it) }
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.emre.gunumplanner.WIDGET_REFRESH"

        fun refreshAll(context: Context) {
            context.sendBroadcast(
                Intent(context, GunumWidgetProvider::class.java).setAction(ACTION_REFRESH)
            )
        }

        private fun update(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val db = Db(context)
            val today = LocalDate.now().toString()
            val tasks = db.getItemsForDay(today).filter { it.type == Db.TYPE_TASK }
            val openCount = tasks.count { it.status == Db.STATUS_OPEN }
            val doneCount = tasks.count { it.status == Db.STATUS_DONE }
            val total = tasks.size

            val views = RemoteViews(context.packageName, R.layout.widget_gunum)
            views.setTextViewText(
                R.id.widget_count,
                when {
                    total == 0 -> "Bugün görev yok"
                    doneCount == 0 -> "$openCount açık görev"
                    else -> "$openCount açık  •  $doneCount tamamlandı"
                }
            )

            views.setProgressBar(R.id.widget_progress, total.coerceAtLeast(1), doneCount, false)
            views.setViewVisibility(R.id.widget_progress, if (total > 0) View.VISIBLE else View.INVISIBLE)

            val serviceIntent = Intent(context, GunumWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_list, serviceIntent)
            views.setEmptyView(R.id.widget_list, R.id.widget_empty)

            val itemTemplate = PendingIntent.getBroadcast(
                context,
                43000 + widgetId,
                Intent(context, WidgetItemActionReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_list, itemTemplate)

            val openApp = PendingIntent.getActivity(
                context,
                41000 + widgetId,
                Intent(context, PremiumV2Activity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_header, openApp)
            views.setOnClickPendingIntent(R.id.widget_open_all, openApp)

            val addIntent = Intent(context, PremiumV2Activity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra("widget_quick_add", true)
            val add = PendingIntent.getActivity(
                context,
                42000 + widgetId,
                addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_add, add)
            views.setOnClickPendingIntent(R.id.widget_add_bottom, add)

            manager.updateAppWidget(widgetId, views)
            manager.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_list)
        }
    }
}
