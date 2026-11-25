package app.wakephrase

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Android does not let an app switch the microphone on by itself after a
 * restart. This posts a reminder instead; one tap turns listening back on.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = context.getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(Prefs.WANTED, false)) return

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val open = PendingIntent.getActivity(
            context, 2,
            Intent(context, MainActivity::class.java).setAction(MainActivity.ACTION_START),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Hey Claude is off")
            .setContentText("Tap to turn listening back on.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            // notifications not allowed; nothing else to do
        }
    }

    companion object {
        private const val CHANNEL_ID = "reminder"
        private const val NOTIFICATION_ID = 2
    }
}
