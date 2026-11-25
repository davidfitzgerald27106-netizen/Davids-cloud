package app.wakephrase

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView

/** One screen: the switch, what is still needed, and what the app has been hearing. */
class MainActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var prefs: SharedPreferences
    private lateinit var statusView: TextView
    private lateinit var checklistView: TextView
    private lateinit var thresholdLabel: TextView
    private lateinit var heardView: TextView
    private lateinit var toggle: Button
    private var startRequested = false

    private val refresher = object : Runnable {
        override fun run() {
            refresh()
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE)
        startRequested = intent?.action == ACTION_START
        val pad = (20 * resources.displayMetrics.density).toInt()

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        column.addView(TextView(this).apply {
            text = "Hey Claude"
            textSize = 26f
        })
        column.addView(TextView(this).apply {
            text = "Say \"Hey Claude\" and the Claude app opens. " +
                "Listening happens on this phone only. Nothing is recorded or sent anywhere."
            textSize = 15f
            setPadding(0, pad / 2, 0, pad)
        })

        statusView = TextView(this).apply {
            textSize = 17f
            setPadding(0, 0, 0, pad / 2)
        }
        column.addView(statusView)

        toggle = Button(this).apply {
            setOnClickListener { onToggle() }
        }
        column.addView(toggle)

        column.addView(Button(this).apply {
            text = "Test: open Claude now"
            setOnClickListener {
                if (!Launcher.open(this@MainActivity)) {
                    statusView.text = "Claude app not found on this phone."
                }
            }
        })

        thresholdLabel = TextView(this).apply {
            textSize = 15f
            setPadding(0, pad, 0, 0)
        }
        column.addView(thresholdLabel)
        column.addView(SeekBar(this).apply {
            max = Prefs.THRESHOLD_MAX - Prefs.THRESHOLD_MIN
            progress = Prefs.threshold(prefs) - Prefs.THRESHOLD_MIN
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {
                    if (fromUser) {
                        Prefs.setThreshold(prefs, value + Prefs.THRESHOLD_MIN)
                        refresh()
                    }
                }

                override fun onStartTrackingTouch(bar: SeekBar?) = Unit
                override fun onStopTrackingTouch(bar: SeekBar?) = Unit
            })
        })
        column.addView(TextView(this).apply {
            text = "Opens when it shouldn't: slide right. Misses you: slide left."
            textSize = 13f
        })

        column.addView(TextView(this).apply {
            text = "What it heard"
            textSize = 17f
            setPadding(0, pad, 0, 0)
        })
        heardView = TextView(this).apply {
            textSize = 13f
            setPadding(0, pad / 4, 0, 0)
        }
        column.addView(heardView)

        checklistView = TextView(this).apply {
            textSize = 15f
            setPadding(0, pad, 0, pad / 2)
        }
        column.addView(checklistView)

        column.addView(Button(this).apply {
            text = "Allow microphone and notifications"
            setOnClickListener { askRuntimePermissions() }
        })
        column.addView(Button(this).apply {
            text = "Allow opening Claude from the background"
            setOnClickListener {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        })
        column.addView(Button(this).apply {
            text = "Battery settings for this app"
            setOnClickListener {
                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        })

        column.addView(TextView(this).apply {
            text = "Good to know:\n" +
                "- After a restart of the phone, tap the reminder to turn listening back on.\n" +
                "- It cannot unlock the phone. From a locked screen it wakes the display.\n" +
                "- It steps aside while another app is using the microphone.\n" +
                "- If Samsung puts this app to sleep, set its battery use to Unrestricted."
            textSize = 14f
            setPadding(0, pad, 0, 0)
        })

        setContentView(ScrollView(this).apply { addView(column) })
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == ACTION_START) startRequested = true
    }

    override fun onResume() {
        super.onResume()
        if (startRequested) {
            startRequested = false
            if (!ListenService.running && hasMic()) turnOn()
        }
        handler.post(refresher)
    }

    override fun onPause() {
        handler.removeCallbacks(refresher)
        super.onPause()
    }

    private fun hasMic() =
        checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun hasNotifications() =
        Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun hasOverlay() = Settings.canDrawOverlays(this)

    private fun askRuntimePermissions() {
        val wanted = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) wanted.add(Manifest.permission.POST_NOTIFICATIONS)
        requestPermissions(wanted.toTypedArray(), 1)
    }

    private fun turnOn() {
        prefs.edit().putBoolean(Prefs.WANTED, true).apply()
        startForegroundService(Intent(this, ListenService::class.java))
    }

    private fun onToggle() {
        if (ListenService.running) {
            prefs.edit().putBoolean(Prefs.WANTED, false).apply()
            stopService(Intent(this, ListenService::class.java))
        } else {
            if (!hasMic()) {
                askRuntimePermissions()
                return
            }
            turnOn()
        }
        handler.postDelayed({ refresh() }, 300)
    }

    private fun mark(ok: Boolean) = if (ok) "[done]" else "[needed]"

    private fun meter(level: Int): String {
        val bars = (level / 150).coerceIn(0, 20)
        return "#".repeat(bars) + ".".repeat(20 - bars)
    }

    private fun refresh() {
        val on = ListenService.running
        toggle.text = if (on) "Turn listening off" else "Turn listening on"
        statusView.text = "Status: ${ListenService.status}\n" +
            "Times opened since start: ${ListenService.heardCount}\n" +
            "Microphone: ${if (on) meter(ListenService.level) else "off"}"
        thresholdLabel.text = "How sure it must be: ${Prefs.threshold(prefs)}%"
        heardView.text = ListenService.recent()
        checklistView.text = "Setup\n" +
            "${mark(hasMic())} Microphone\n" +
            "${mark(hasNotifications())} Notifications\n" +
            "${mark(hasOverlay())} Open Claude from the background\n" +
            "${mark(Launcher.find(this) != null)} Claude app installed"
    }

    companion object {
        const val ACTION_START = "app.wakephrase.START"
    }
}
