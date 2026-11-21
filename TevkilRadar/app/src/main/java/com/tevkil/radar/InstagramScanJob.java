package com.tevkil.radar;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;

/** Best-effort ~15min Android periodic task; Android may defer execution. */
public final class InstagramScanJob extends JobService {
    private static final int JOB_ID = 751091;
    private Thread worker;

    public static void schedule(Context context) {
        JobScheduler scheduler = context.getSystemService(JobScheduler.class);
        if (scheduler == null) return;
        scheduler.cancel(JOB_ID);
        if (!Prefs.raw(context).getBoolean("meta_enabled", false) || !InstagramSecureConfig.configured(context)) return;
        JobInfo info = new JobInfo.Builder(JOB_ID, new ComponentName(context, InstagramScanJob.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(15 * 60 * 1000L)
                .setPersisted(true).build();
        scheduler.schedule(info);
    }

    @Override public boolean onStartJob(JobParameters params) {
        worker = new Thread(() -> {
            try {
                if (Prefs.raw(this).getBoolean("meta_enabled", false)) InstagramRemoteScanner.scan(this);
            } finally { jobFinished(params, false); }
        }, "instagram-api-scan");
        worker.start();
        return true;
    }
    @Override public boolean onStopJob(JobParameters params) {
        if (worker != null) worker.interrupt();
        return true;
    }
}
