package com.emre.gunumplanner

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class GunumWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TaskFactory(applicationContext, intent)
    }

    private class TaskFactory(
        private val context: Context,
        intent: Intent
    ) : RemoteViewsFactory {
        private val db = Db(context)
        private val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        private var tasks: List<Db.Item> = emptyList()

        override fun onCreate() {
            reload()
        }

        override fun onDataSetChanged() {
            reload()
        }

        override fun onDestroy() {
            tasks = emptyList()
        }

        override fun getCount(): Int = tasks.size

        override fun getViewAt(position: Int): RemoteViews? {
            if (position !in tasks.indices) return null
            val item = tasks[position]
            val done = item.status == Db.STATUS_DONE
            val rv = RemoteViews(context.packageName, R.layout.widget_task_row)

            rv.setTextViewText(R.id.widget_task_check, if (done) "✓" else "○")
            rv.setTextViewText(R.id.widget_task_title, item.title)
            rv.setTextColor(R.id.widget_task_title, if (done) 0xFF8A8F99.toInt() else 0xFF171923.toInt())
            rv.setTextColor(R.id.widget_task_check, if (done) 0xFF22A06B.toInt() else 0xFF5B5FEF.toInt())

            val meta = buildList {
                if (item.dueAt > 0L) {
                    val time = Instant.ofEpochMilli(item.dueAt)
                        .atZone(ZoneId.systemDefault())
                        .toLocalTime()
                        .format(DateTimeFormatter.ofPattern("HH:mm"))
                    add(time)
                } else {
                    add("Saati yok")
                }
                if (item.durationMinutes > 0) add("${item.durationMinutes} dk")
                if (item.priority == 0) add("Acil")
                else if (item.priority == 1) add("Önemli")
            }.joinToString("  •  ")

            rv.setTextViewText(R.id.widget_task_meta, meta)
            rv.setViewVisibility(R.id.widget_task_meta, if (meta.isBlank()) View.GONE else View.VISIBLE)

            val open = Intent().apply {
                action = WidgetItemActionReceiver.ACTION_OPEN
                putExtra("item_id", item.id)
                putExtra("widget_id", widgetId)
            }
            rv.setOnClickFillInIntent(R.id.widget_task_row, open)

            val toggle = Intent().apply {
                action = WidgetItemActionReceiver.ACTION_TOGGLE
                putExtra("item_id", item.id)
                putExtra("widget_id", widgetId)
            }
            rv.setOnClickFillInIntent(R.id.widget_task_check, toggle)

            return rv
        }

        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount(): Int = 1
        override fun getItemId(position: Int): Long = tasks.getOrNull(position)?.id ?: position.toLong()
        override fun hasStableIds(): Boolean = true

        private fun reload() {
            val today = LocalDate.now().toString()
            tasks = db.getItemsForDay(today)
                .filter { it.type == Db.TYPE_TASK }
                .sortedWith(
                    compareBy<Db.Item> { if (it.status == Db.STATUS_DONE) 1 else 0 }
                        .thenBy { if (it.dueAt > 0L) 0 else 1 }
                        .thenBy { if (it.dueAt > 0L) it.dueAt else Long.MAX_VALUE }
                        .thenBy { it.priority }
                        .thenBy { it.createdAt }
                )
        }
    }
}
