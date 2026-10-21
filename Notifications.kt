package com.athaniqraq.app.notifications

import android.app.*
import android.content.*
import android.os.Build
import androidx.core.app.NotificationCompat
import com.athaniqraq.app.R

object Notifications {
    const val CHANNEL_ID = "athan_prayers"
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "تنبيهات الصلاة", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "تنبيه عند دخول وقت الصلاة"
            })
        }
    }
    fun schedule(context: Context, atMillis: Long, prayerName: String, requestCode: Int) {
        val intent = Intent(context, AlarmReceiver::class.java).putExtra("prayer", prayerName)
        val pi = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        }
    }
}
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Notifications.createChannel(context)
        val name = intent.getStringExtra("prayer") ?: "الصلاة"
        val n = NotificationCompat.Builder(context, Notifications.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("حان وقت الصلاة")
            .setContentText("حان الآن وقت $name")
            .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build()
        context.getSystemService(NotificationManager::class.java).notify(name.hashCode(), n)
    }
}
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // The app should recalculate today's times and schedule alarms after boot.
        // MainActivity refreshes schedules when opened; persist alarm schedule for unattended rescheduling in production.
    }
}
