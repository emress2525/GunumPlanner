package app.namaz.tr.v8.alarm

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.namaz.tr.v8.NamazApplication

class PrayerRescheduleWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        val graph = (applicationContext as NamazApplication).graph
        PrayerAlarmCoordinator(applicationContext, graph).rescheduleUpcoming()
    }.fold(
        onSuccess = { Result.success() },
        onFailure = { Result.retry() },
    )
}
