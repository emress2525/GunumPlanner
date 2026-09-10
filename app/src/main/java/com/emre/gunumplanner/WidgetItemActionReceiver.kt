package com.emre.gunumplanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WidgetItemActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getLongExtra("item_id", -1L)
        if (itemId <= 0L) return

        when (intent.action) {
            ACTION_TOGGLE -> {
                val db = Db(context)
                val item = db.getItem(itemId) ?: return
                if (item.status == Db.STATUS_DONE) {
                    db.reopenItem(item.id)
                    val reopened = db.getItem(item.id)
                    if (reopened != null && reopened.dueAt > System.currentTimeMillis()) {
                        ReminderScheduler.schedule(context, reopened.id, reopened.dueAt)
                    }
                } else {
                    ReminderScheduler.cancel(context, item.id)
                    val nextId = db.completeItem(item.id)
                    if (nextId > 0L) {
                        FullRepository.propagateSeries(db, item.id, nextId)
                        val next = db.getItem(nextId)
                        if (next != null && next.dueAt > System.currentTimeMillis()) {
                            ReminderScheduler.schedule(context, next.id, next.dueAt)
                        }
                    }
                }
                GunumWidgetProvider.refreshAll(context)
            }

            ACTION_OPEN -> {
                val open = Intent(context, PremiumV2Activity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra("open_item_id", itemId)
                context.startActivity(open)
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.emre.gunumplanner.WIDGET_TOGGLE_TASK"
        const val ACTION_OPEN = "com.emre.gunumplanner.WIDGET_OPEN_TASK"
    }
}
