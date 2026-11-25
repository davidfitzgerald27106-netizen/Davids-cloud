package app.wakephrase

import android.content.SharedPreferences

/** The few values the app remembers between runs. */
object Prefs {
    const val FILE = "settings"

    /** True while the user wants listening on; used for the reminder after a restart. */
    const val WANTED = "wanted"

    private const val THRESHOLD = "threshold"
    const val THRESHOLD_MIN = 50
    const val THRESHOLD_MAX = 95
    private const val THRESHOLD_DEFAULT = 80

    private const val AUTO_VOICE = "auto_voice"

    /** Whether to tap Claude's voice mode button after opening it. */
    fun autoVoice(prefs: SharedPreferences): Boolean = prefs.getBoolean(AUTO_VOICE, true)

    fun setAutoVoice(prefs: SharedPreferences, on: Boolean) {
        prefs.edit().putBoolean(AUTO_VOICE, on).apply()
    }

    /** How sure, in percent, the recognizer must be before the app acts. */
    fun threshold(prefs: SharedPreferences): Int =
        prefs.getInt(THRESHOLD, THRESHOLD_DEFAULT).coerceIn(THRESHOLD_MIN, THRESHOLD_MAX)

    fun setThreshold(prefs: SharedPreferences, value: Int) {
        prefs.edit().putInt(THRESHOLD, value.coerceIn(THRESHOLD_MIN, THRESHOLD_MAX)).apply()
    }
}
