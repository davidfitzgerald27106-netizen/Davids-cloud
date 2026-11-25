package app.wakephrase

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService

/**
 * Keeps the microphone open in a foreground service and listens, on the
 * device only, for the wake phrase. When it hears it, it opens the target app.
 */
class ListenService : Service(), RecognitionListener {

    private val handler = Handler(Looper.getMainLooper())
    private var model: Model? = null
    private var speech: SpeechService? = null
    private var lastFire = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(
            NOTIFICATION_ID,
            buildNotification("Starting"),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        )
        running = true
        status = "Loading speech model"
        StorageService.unpack(
            this, "model-en-us", "model",
            { loaded ->
                model = loaded
                startListening()
            },
            { error ->
                status = "Could not load the speech model: ${error.message}"
                updateNotification("Model failed to load")
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        stopListening()
        model?.close()
        model = null
        running = false
        status = "Stopped"
        super.onDestroy()
    }

    private fun startListening() {
        val loaded = model ?: return
        stopListening()
        try {
            val recognizer = Recognizer(loaded, SAMPLE_RATE, GRAMMAR)
            speech = SpeechService(recognizer, SAMPLE_RATE).also { it.startListening(this) }
            status = "Listening for \"Hey Claude\""
            updateNotification("Listening for \"Hey Claude\"")
        } catch (e: Exception) {
            status = "Microphone unavailable: ${e.message}"
            updateNotification("Microphone unavailable")
            handler.postDelayed({ startListening() }, RETRY_MS)
        }
    }

    private fun stopListening() {
        speech?.let {
            it.stop()
            it.shutdown()
        }
        speech = null
    }

    override fun onPartialResult(hypothesis: String?) = check(hypothesis, "partial")

    override fun onResult(hypothesis: String?) = check(hypothesis, "text")

    override fun onFinalResult(hypothesis: String?) = Unit

    override fun onTimeout() = Unit

    override fun onError(exception: Exception?) {
        status = "Listening error: ${exception?.message}"
        handler.postDelayed({ startListening() }, RETRY_MS)
    }

    private fun check(hypothesis: String?, field: String) {
        if (hypothesis.isNullOrEmpty()) return
        val heard = try {
            JSONObject(hypothesis).optString(field)
        } catch (e: Exception) {
            ""
        }
        if (TRIGGERS.any { heard.contains(it) }) fire()
    }

    private fun fire() {
        val now = System.currentTimeMillis()
        if (now - lastFire < COOLDOWN_MS) return
        lastFire = now
        heardCount += 1

        // Let go of the microphone so the app being opened can use it.
        stopListening()
        wakeScreen()
        val opened = Launcher.open(this)
        status = if (opened) "Heard it. Opened Claude." else "Heard it, but could not open Claude."
        updateNotification(if (opened) "Opened Claude" else "Could not open Claude")
        handler.postDelayed({ startListening() }, RESUME_MS)
    }

    @Suppress("DEPRECATION")
    private fun wakeScreen() {
        val power = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (power.isInteractive) return
        val lock = power.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "wakephrase:screen"
        )
        lock.acquire(5000)
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Listening", NotificationManager.IMPORTANCE_LOW
        )
        channel.description = "Shown while the wake phrase listener is on"
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, ListenService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Hey Claude")
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Stop", stop).build())
            .build()
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(text))
    }

    companion object {
        const val ACTION_STOP = "app.wakephrase.STOP"
        private const val CHANNEL_ID = "listening"
        private const val NOTIFICATION_ID = 1
        private const val SAMPLE_RATE = 16000.0f
        private const val COOLDOWN_MS = 5000L
        private const val RESUME_MS = 20000L
        private const val RETRY_MS = 10000L

        // The recognizer only knows these phrases; everything else is "[unk]".
        // "cloud" and "clod" are there because they sound the same as the name.
        private const val GRAMMAR =
            "[\"hey claude\", \"hey cloud\", \"hey clod\", \"[unk]\"]"
        private val TRIGGERS = listOf("hey claude", "hey cloud", "hey clod")

        @Volatile
        var running = false

        @Volatile
        var status = "Stopped"

        @Volatile
        var heardCount = 0
    }
}
