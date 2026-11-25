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

    /** How sure, in percent, the recognizer must be before the app acts. */
    fun threshold(prefs: SharedPreferences): Int =
        prefs.getInt(THRESHOLD, THRESHOLD_DEFAULT).coerceIn(THRESHOLD_MIN, THRESHOLD_MAX)

    fun setThreshold(prefs: SharedPreferences, value: Int) {
        prefs.edit().putInt(THRESHOLD, value.coerceIn(THRESHOLD_MIN, THRESHOLD_MAX)).apply()
    }
}
