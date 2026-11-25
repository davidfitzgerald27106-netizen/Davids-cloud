package app.wakephrase

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioRecordingConfiguration
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.StorageService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

/**
 * Keeps the microphone open in a foreground service and listens, on the
 * device only, for the wake phrase. When it hears it, it opens the target app.
 *
 * Audio is only passed to the speech recognizer while there is sound above
 * the room's background level. In silence the loop just measures volume.
 */
class ListenService : Service() {

    private val main = Handler(Looper.getMainLooper())
    private lateinit var prefs: SharedPreferences
    private lateinit var audio: AudioManager

    private var model: Model? = null
    private var worker: Thread? = null

    @Volatile
    private var capturing = false

    @Volatile
    private var sessionId = -1

    private var lastFire = 0L

    // Keeps the listening loop running while the screen is off.
    private var cpuLock: PowerManager.WakeLock? = null

    private val recordingWatcher = object : AudioManager.AudioRecordingCallback() {
        override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>?) {
            val mine = configs?.firstOrNull { it.clientAudioSessionId == sessionId }
            val nowSilenced = mine?.isClientSilenced == true
            if (nowSilenced != silenced) {
                silenced = nowSilenced
                if (capturing) {
                    setStatus(if (nowSilenced) PAUSED_TEXT else LISTENING_TEXT)
                }
            }
        }
    }

    private val resumeCheck = object : Runnable {
        override fun run() {
            if (!running) return
            // Our own recording is stopped here, so anything active is another app.
            val othersRecording = try {
                audio.activeRecordingConfigurations.isNotEmpty()
            } catch (e: Exception) {
                false
            }
            if (othersRecording || inCall()) {
                setStatus(PAUSED_TEXT)
                main.postDelayed(this, RECHECK_MS)
            } else {
                startCapture()
            }
        }
    }

    /** True during a phone call, a ringing phone, or another app's voice session. */
    private fun inCall(): Boolean = audio.mode != AudioManager.MODE_NORMAL

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE)
        audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createChannel()
        try {
            startForeground(
                NOTIFICATION_ID,
                buildNotification("Starting"),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } catch (e: Exception) {
            // Android refuses to start the microphone from the background.
            status = "Open the app and turn listening on."
            stopSelf()
            return
        }
        running = true
        status = "Loading speech model"
        cpuLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wakephrase:listen")
            .also { it.acquire() }
        audio.registerAudioRecordingCallback(recordingWatcher, main)
        StorageService.unpack(
            this, "model-en-us", "model",
            { loaded ->
                model = loaded
                if (running) startCapture()
            },
            { error ->
                setStatus("Could not load the speech model: ${error.message}")
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            prefs.edit().putBoolean(Prefs.WANTED, false).apply()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        running = false
        main.removeCallbacksAndMessages(null)
        try {
            audio.unregisterAudioRecordingCallback(recordingWatcher)
        } catch (e: Exception) {
            // never registered
        }
        stopCapture()
        try {
            cpuLock?.let { if (it.isHeld) it.release() }
        } catch (e: Exception) {
            // already released
        }
        cpuLock = null
        model?.close()
        model = null
        silenced = false
        level = 0
        status = "Stopped"
        super.onDestroy()
    }

    private fun startCapture() {
        if (capturing || model == null || !running) return
        capturing = true
        worker = Thread({ captureLoop() }, "listen").also { it.start() }
    }

    private fun stopCapture() {
        capturing = false
        try {
            worker?.join(2000)
        } catch (e: InterruptedException) {
            // shutting down anyway
        }
        worker = null
    }

    private fun captureLoop() {
        val loaded = model ?: return
        var record: AudioRecord? = null
        var recognizer: Recognizer? = null
        try {
            val minBuffer = AudioRecord.getMinBufferSize(
                RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            record = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBuffer, RATE * 4)
            )
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                throw IllegalStateException("microphone not available")
            }
            recognizer = Recognizer(loaded, RATE.toFloat(), GRAMMAR)
            recognizer.setWords(true)

            sessionId = record.audioSessionId
            record.startRecording()
            setStatus(LISTENING_TEXT)

            val buffer = ShortArray(FRAME)
            val preRoll = ArrayDeque<ShortArray>()
            var floor = 150.0
            var gateOpen = false
            var quietFrames = 0
            var openFrames = 0
            var frames = 0

            while (capturing) {
                val n = record.read(buffer, 0, FRAME)
                if (n < 0) throw IllegalStateException("microphone read failed ($n)")
                if (n == 0) continue

                frames += 1
                if (frames % 10 == 0 && inCall()) {
                    // A call or voice session started: release the microphone entirely.
                    capturing = false
                    setStatus(PAUSED_TEXT)
                    main.postDelayed(resumeCheck, RECHECK_MS)
                    break
                }

                val rms = rms(buffer, n)
                level = rms.toInt()
                val loud = rms > maxOf(MIN_GATE, floor * 2.5)

                if (!gateOpen) {
                    if (loud) {
                        gateOpen = true
                        quietFrames = 0
                        openFrames = 0
                        // Replay the half second before the sound started,
                        // so the first word is not clipped.
                        for (chunk in preRoll) feed(recognizer, chunk, chunk.size)
                        preRoll.clear()
                        feed(recognizer, buffer, n)
                    } else {
                        floor = floor * 0.95 + rms * 0.05
                        preRoll.addLast(buffer.copyOf(n))
                        if (preRoll.size > PRE_ROLL_FRAMES) preRoll.removeFirst()
                    }
                } else {
                    feed(recognizer, buffer, n)
                    openFrames += 1
                    quietFrames = if (loud) 0 else quietFrames + 1
                    // Slowly accept steady noise (music, a fan) as the new background.
                    floor = floor * 0.995 + rms * 0.005
                    if (quietFrames >= QUIET_FRAMES || openFrames >= MAX_OPEN_FRAMES) {
                        handle(recognizer.finalResult)
                        gateOpen = false
                    }
                }
            }
        } catch (e: Exception) {
            if (capturing) {
                capturing = false
                setStatus("Microphone problem: ${e.message}. Trying again shortly.")
                main.postDelayed(resumeCheck, RETRY_MS)
            }
        } finally {
            sessionId = -1
            level = 0
            try {
                record?.stop()
            } catch (e: Exception) {
                // was never started
            }
            record?.release()
            recognizer?.close()
        }
    }

    private fun feed(recognizer: Recognizer, data: ShortArray, length: Int) {
        if (recognizer.acceptWaveForm(data, length)) handle(recognizer.result)
    }

    private fun rms(data: ShortArray, length: Int): Double {
        var sum = 0.0
        for (i in 0 until length) {
            val s = data[i].toDouble()
            sum += s * s
        }
        return sqrt(sum / length)
    }

    /** Looks at one finished utterance and decides whether it was the wake phrase. */
    private fun handle(json: String?) {
        if (json.isNullOrEmpty()) return
        val parsed = try {
            JSONObject(json)
        } catch (e: Exception) {
            return
        }
        val text = parsed.optString("text").trim()
        if (text.replace("[unk]", "").isBlank()) return

        var best = -1.0
        val words = parsed.optJSONArray("result")
        if (words != null) {
            for (i in 0 until words.length() - 1) {
                val first = words.optJSONObject(i) ?: continue
                val second = words.optJSONObject(i + 1) ?: continue
                val gap = second.optDouble("start", 0.0) - first.optDouble("end", 0.0)
                if (first.optString("word") == "hey" &&
                    second.optString("word") in NAMES &&
                    gap <= MAX_GAP_SECONDS
                ) {
                    val sure = minOf(first.optDouble("conf", 0.0), second.optDouble("conf", 0.0))
                    best = maxOf(best, sure)
                }
            }
        }

        val needed = Prefs.threshold(prefs) / 100.0
        val percent = (best * 100).toInt()
        when {
            best < 0 -> note("\"$text\"  not the phrase")
            best >= needed -> {
                note("\"$text\"  $percent% sure  opened Claude")
                main.post { fire() }
            }
            else -> note("\"$text\"  $percent% sure  below your setting")
        }
    }

    private fun fire() {
        if (!running) return
        val now = System.currentTimeMillis()
        if (now - lastFire < COOLDOWN_MS) return
        lastFire = now
        heardCount += 1

        // Let go of the microphone so the app being opened can use it.
        stopCapture()
        chirp()
        wakeScreen()
        val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (keyguard.isKeyguardLocked) {
            // Locked: light the screen and ask for a normal unlock first.
            val opened = try {
                startActivity(
                    Intent(this, WakeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                true
            } catch (e: Exception) {
                false
            }
            setStatus(
                if (opened) "Heard it. Waiting for the phone to unlock."
                else "Heard it, but could not wake the phone."
            )
        } else {
            val opened = Launcher.open(this)
            setStatus(
                if (opened) "Heard it. Opened Claude." else "Heard it, but could not open Claude."
            )
            if (opened && Prefs.autoVoice(prefs) && TapService.connected) TapService.request()
        }
        main.removeCallbacks(resumeCheck)
        main.postDelayed(resumeCheck, RESUME_MS)
    }

    /** A short tone, so you know it heard you without looking at the phone. */
    private fun chirp() {
        try {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70)
            tone.startTone(ToneGenerator.TONE_PROP_ACK, 150)
            main.postDelayed({ tone.release() }, 600)
        } catch (e: Exception) {
            // no tone available; not important
        }
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

    private fun setStatus(text: String) {
        status = text
        main.post {
            if (running) {
                getSystemService(NotificationManager::class.java)
                    .notify(NOTIFICATION_ID, buildNotification(text))
            }
        }
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

    companion object {
        const val ACTION_STOP = "app.wakephrase.STOP"
        private const val CHANNEL_ID = "listening"
        private const val NOTIFICATION_ID = 1

        private const val RATE = 16000
        private const val FRAME = 1600            // 100 ms of audio
        private const val PRE_ROLL_FRAMES = 5     // 500 ms kept from before the sound
        private const val QUIET_FRAMES = 6        // 600 ms of quiet ends an utterance
        private const val MAX_OPEN_FRAMES = 80    // never analyse more than 8 s in one go
        private const val MIN_GATE = 100.0        // lowest volume that counts as sound
        private const val MAX_GAP_SECONDS = 0.45  // longest pause allowed inside the phrase

        private const val COOLDOWN_MS = 5000L
        private const val RESUME_MS = 6000L
        private const val RECHECK_MS = 3000L
        private const val RETRY_MS = 10000L

        private const val LISTENING_TEXT = "Listening for \"Hey Claude\""
        private const val PAUSED_TEXT = "Paused while another app uses the microphone"

        private val NAMES = setOf("claude", "cloud")

        // The recognizer only knows these phrases; everything else is "[unk]".
        // "hey cloud" is there because it sounds the same as the name. The rest
        // are decoys: they give look-alike speech somewhere else to land.
        private val PHRASES = listOf(
            "hey claude", "hey cloud",
            "hey", "hay", "they", "hi", "hello", "okay",
            "hey there", "hey you", "hey google", "hey guys",
            "claude", "cloud", "clouds", "the cloud", "loud", "allowed", "crowd",
            "[unk]"
        )
        private val GRAMMAR = PHRASES.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }

        private val clock = SimpleDateFormat("h:mm:ss a", Locale.US)
        private val events = ArrayDeque<String>()

        @Volatile
        var running = false

        @Volatile
        var status = "Stopped"

        @Volatile
        var heardCount = 0

        @Volatile
        var level = 0

        @Volatile
        var silenced = false

        fun note(line: String) {
            synchronized(events) {
                events.addFirst("${clock.format(Date())}  $line")
                while (events.size > 12) events.removeLast()
            }
        }

        fun recent(): String = synchronized(events) {
            if (events.isEmpty()) "Nothing yet." else events.joinToString("\n")
        }
    }
}
