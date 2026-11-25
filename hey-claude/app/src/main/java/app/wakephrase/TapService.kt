package app.wakephrase

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Optional. Taps the voice mode button in the Claude app, and only in the
 * few seconds after the wake phrase was heard. It is limited to the Claude
 * app and never reads message text.
 */
class TapService : AccessibilityService() {

    private val main = Handler(Looper.getMainLooper())
    private var targetPackage: String? = null
    private var seen: String = ""

    private val poll = object : Runnable {
        override fun run() {
            val deadline = pendingUntil
            if (deadline == 0L) return
            if (System.currentTimeMillis() > deadline) {
                pendingUntil = 0L
                ListenService.note(
                    "Voice button not found. Buttons on screen: " +
                        seen.ifEmpty { "none seen" }
                )
                return
            }
            attempt()
            if (pendingUntil != 0L) main.postDelayed(this, POLL_MS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        targetPackage = Launcher.packageName(this)
        targetPackage?.let { name ->
            // Only ever receive events from the Claude app.
            serviceInfo = serviceInfo.apply { packageNames = arrayOf(name) }
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        main.removeCallbacksAndMessages(null)
        if (instance === this) instance = null
        return super.onUnbind(intent)
    }

    override fun onInterrupt() = Unit

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (pendingUntil != 0L) attempt()
    }

    private fun begin() {
        seen = ""
        main.removeCallbacks(poll)
        main.postDelayed(poll, FIRST_TRY_MS)
    }

    private fun attempt() {
        val root = rootInActiveWindow ?: return
        val wanted = targetPackage
        if (wanted != null && root.packageName?.toString() != wanted) return

        var best: AccessibilityNodeInfo? = null
        var bestScore = 0
        var bestLabel = ""
        val labels = ArrayList<String>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.addLast(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val node = queue.removeFirst()
            visited += 1
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.addLast(it) }
            }
            if (!node.isVisibleToUser) continue
            // Button labels only. Message text is never looked at.
            val label = node.contentDescription?.toString()?.trim().orEmpty()
            if (label.isEmpty() || label.length > 40) continue
            if (clickable(node) == null) continue
            if (labels.size < 15) labels.add(label)
            val score = score(label.lowercase())
            if (score > bestScore) {
                bestScore = score
                best = node
                bestLabel = label
            }
        }
        seen = labels.joinToString(", ")

        val target = best?.let { clickable(it) } ?: return
        if (target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            pendingUntil = 0L
            ListenService.note("Started voice mode (tapped \"$bestLabel\")")
        }
    }

    private fun clickable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        var hops = 0
        while (current != null && hops < 4) {
            if (current.isClickable) return current
            current = current.parent
            hops += 1
        }
        return null
    }

    private fun score(label: String): Int {
        if (AVOID.any { label.contains(it) }) return 0
        return when {
            label.contains("voice mode") -> 3
            label.contains("start voice") ||
                label.contains("voice chat") ||
                label.contains("voice conversation") -> 2
            label.contains("voice") -> 1
            else -> 0
        }
    }

    companion object {
        private const val WINDOW_MS = 15000L
        private const val FIRST_TRY_MS = 700L
        private const val POLL_MS = 400L
        private const val MAX_NODES = 800

        // Never tap something that would end, mute, type or open settings.
        private val AVOID = listOf(
            "stop", "end", "exit", "close", "leave", "mute", "cancel",
            "input", "dictat", "message", "setting", "record"
        )

        @Volatile
        private var instance: TapService? = null

        @Volatile
        private var pendingUntil = 0L

        val connected: Boolean
            get() = instance != null

        /** Called right after the wake phrase opened the Claude app. */
        fun request() {
            val service = instance ?: return
            pendingUntil = System.currentTimeMillis() + WINDOW_MS
            service.main.post { service.begin() }
        }
    }
}
