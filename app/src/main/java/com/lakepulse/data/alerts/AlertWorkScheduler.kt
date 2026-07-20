package com.lakepulse.data.alerts

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlertCheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AlertChecker.run(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }
}

class AlertBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        AlertWorkScheduler.schedule(context.applicationContext)
    }
}

object AlertWorkScheduler {
    private const val REQUEST_CODE = 4401
    private const val INTERVAL_MS = 2L * 60L * 60L * 1000L

    fun schedule(context: Context) {
        LakePulseNotifier.ensureChannel(context)
        val app = context.applicationContext
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = pendingIntent(app)
        am.cancel(pending)
        am.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + INTERVAL_MS,
            INTERVAL_MS,
            pending,
        )
    }

    fun runNow(context: Context) {
        LakePulseNotifier.ensureChannel(context)
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { AlertChecker.run(context.applicationContext) }
        }
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlertCheckReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
