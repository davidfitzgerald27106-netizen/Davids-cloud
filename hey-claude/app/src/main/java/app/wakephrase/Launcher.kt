package app.wakephrase

import android.content.Context
import android.content.Intent

/** Finds the Claude app on this phone and opens it. */
object Launcher {

    private const val PACKAGE = "com.anthropic.claude"

    fun find(context: Context): Intent? {
        val pm = context.packageManager
        pm.getLaunchIntentForPackage(PACKAGE)?.let { return it }

        // Fall back to any installed app whose name is exactly "Claude".
        val probe = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val match = pm.queryIntentActivities(probe, 0).firstOrNull {
            it.loadLabel(pm).toString().trim().equals("Claude", ignoreCase = true)
        } ?: return null
        return pm.getLaunchIntentForPackage(match.activityInfo.packageName)
    }

    fun open(context: Context): Boolean {
        val intent = find(context) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
