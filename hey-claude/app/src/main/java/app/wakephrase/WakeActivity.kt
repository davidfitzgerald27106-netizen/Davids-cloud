package app.wakephrase

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.Bundle

/**
 * Used when the wake phrase is heard while the phone is locked. It lights the
 * screen and asks Android to unlock in the normal way (face, fingerprint, PIN,
 * or no prompt at all if the phone is currently in an Extend Unlock state).
 * It never bypasses the lock. Once the phone is unlocked it opens Claude.
 */
class WakeActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!keyguard.isKeyguardLocked) {
            proceed()
            return
        }
        keyguard.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() = proceed()

            override fun onDismissCancelled() {
                ListenService.note("Phone stayed locked, so Claude was not opened")
                finish()
            }

            override fun onDismissError() {
                ListenService.note("Could not ask the phone to unlock")
                finish()
            }
        })
    }

    private fun proceed() {
        val prefs = getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE)
        val opened = Launcher.open(this)
        if (opened && Prefs.autoVoice(prefs) && TapService.connected) TapService.request()
        if (!opened) ListenService.note("Unlocked, but could not open Claude")
        finish()
    }
}
